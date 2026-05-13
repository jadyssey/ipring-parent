#!/usr/bin/env python3
"""
Usage:

  # 1. Dry-run only. This is the default and does not modify any file.
  python -B scripts/dead_api_cleaner.py /waybill/addressError/v2/query

  # 2. Analyze multiple API paths.
  python -B scripts/dead_api_cleaner.py --paths /api/a /api/b

  # 3. Read API paths from a text file, one path per line. Blank lines and
  #    lines starting with "#" are ignored.
  python -B scripts/dead_api_cleaner.py --paths-file api-paths.txt

  # 4. Write the JSON report to a custom location.
  python -B scripts/dead_api_cleaner.py /api/a --report reports/dead-api.json

  # 5. Suppress JSON report output.
  python -B scripts/dead_api_cleaner.py /api/a --no-report

  # 6. Delete after reviewing dry-run output and the JSON report.
  python -B scripts/dead_api_cleaner.py delete /api/a

  # 7. Start the Web management page.
  python -B scripts/dead_api_cleaner_web.py --host 127.0.0.1 --port 8765

Recommended workflow:

  1. Commit or stash unrelated local changes first.
  2. Run dry-run and review "Removable methods" and "Retained boundary methods".
  3. Run the delete command only after the plan is acceptable.
  4. Run project compilation/tests after deletion.

Safety model:

  - The script locates Spring MVC routes from class-level and method-level
    @RequestMapping/@GetMapping/@PostMapping/etc. annotations.
  - It supports literal route strings, simple string constants, concatenated
    constants, injected service calls, same-class calls, static utility calls,
    and interface-to-implementation service contracts.
  - It recursively follows internal calls until a method has an external
    caller, framework entrypoint annotation, or shared dependency boundary.
  - It deletes only methods whose incoming references are fully contained in
    the selected endpoint closure.
  - It never deletes enum methods/constants or JavaBean getter/setter methods.
  - It can also remove matching MyBatis XML statements for deleted mapper
    methods unless --no-delete-xml is used.
  - Path variables are matched by default, e.g. /foo/{id} matches /foo/123.
    Use --exact-route-match to disable this.

Exit codes:

  0: analysis completed and all requested paths were resolved.
  2: analysis completed, but at least one requested path was not resolved.

Static dead-code cleaner for Spring MVC API endpoints in this repository.

The script takes one or more API paths, locates their Controller methods,
builds a conservative Java method call graph, and removes only methods whose
incoming references are fully contained in the endpoint deletion closure.

Default action is dry-run analysis. Use the delete command to modify files.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Set, Tuple


JAVA_SUFFIX = ".java"
XML_SUFFIX = ".xml"

SKIP_DIRS = {
    ".git",
    ".idea",
    ".vscode",
    "target",
    "build",
    "out",
    ".gradle",
    "node_modules",
}

MAPPING_ANNOTATIONS = {
    "RequestMapping",
    "GetMapping",
    "PostMapping",
    "PutMapping",
    "DeleteMapping",
    "PatchMapping",
}

ENTRYPOINT_ANNOTATIONS = {
    "Scheduled",
    "KafkaListener",
    "RabbitListener",
    "JmsListener",
    "EventListener",
    "PostConstruct",
    "PreDestroy",
    "Bean",
}

CALL_KEYWORDS = {
    "if",
    "for",
    "while",
    "switch",
    "catch",
    "return",
    "throw",
    "new",
    "super",
    "this",
    "try",
    "else",
    "do",
    "case",
    "assert",
    "synchronized",
}

STATEMENT_TAGS = ("select", "insert", "update", "delete")


@dataclass(frozen=True)
class MethodKey:
    class_fqn: str
    name: str
    start: int

    def text(self) -> str:
        return f"{self.class_fqn}#{self.name}@{self.start}"


@dataclass
class MethodInfo:
    key: MethodKey
    class_fqn: str
    class_name: str
    name: str
    path: Path
    line: int
    full_start: int
    full_end: int
    sig_start: int
    sig_end: int
    body_start: Optional[int]
    body_end: Optional[int]
    signature: str
    annotations_text: str
    params: Dict[str, str]
    is_abstract: bool = False
    is_constructor: bool = False
    route_paths: List[str] = field(default_factory=list)
    annotation_names: Set[str] = field(default_factory=set)

    @property
    def label(self) -> str:
        return f"{self.class_fqn}.{self.name}({', '.join(self.param_types)})"

    @property
    def param_types(self) -> List[str]:
        return list(self.params.values())


@dataclass
class ClassInfo:
    path: Path
    package: str
    name: str
    fqn: str
    kind: str
    source: str
    masked: str
    class_start: int
    body_start: int
    body_end: int
    annotations_text: str
    imports: Dict[str, str] = field(default_factory=dict)
    static_imports: Dict[str, str] = field(default_factory=dict)
    static_wildcards: List[str] = field(default_factory=list)
    extends: List[str] = field(default_factory=list)
    implements: List[str] = field(default_factory=list)
    fields: Dict[str, str] = field(default_factory=dict)
    raw_constants: Dict[str, str] = field(default_factory=dict)
    constants: Dict[str, str] = field(default_factory=dict)
    methods: List[MethodInfo] = field(default_factory=list)
    route_paths: List[str] = field(default_factory=list)


@dataclass
class RouteMatch:
    requested_path: str
    method: MethodInfo
    matched_route: str


@dataclass
class CleanupPlan:
    roots: List[RouteMatch]
    closure: Set[MethodKey]
    removable: Set[MethodKey]
    retained: Set[MethodKey]
    root_warnings: Dict[str, List[str]]
    retain_reasons: Dict[str, List[str]]
    unresolved_paths: List[str]
    unresolved_suggestions: Dict[str, List[RouteMatch]] = field(default_factory=dict)


@dataclass
class CleanupRunOptions:
    root: Path
    include_tests: bool = False
    exact_route_match: bool = False
    max_depth: int = 80
    delete_xml: bool = True
    delete: bool = False
    report: str = "dead_api_cleanup_report.json"
    no_report: bool = False
    verbose: bool = False


@dataclass
class CleanupRunResult:
    analyzer: "JavaProjectAnalyzer"
    plan: CleanupPlan
    deleted: Optional[Dict[str, object]]
    report: Dict[str, Any]
    report_path: Optional[Path]


def read_text(path: Path) -> str:
    for encoding in ("utf-8-sig", "utf-8", "gb18030", "latin1"):
        try:
            return path.read_text(encoding=encoding)
        except UnicodeDecodeError:
            continue
    return path.read_text(errors="replace")


def write_text(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def mask_java(source: str) -> str:
    """Replace comments and string/char literals with spaces, preserving length."""
    out = list(source)
    i = 0
    n = len(source)
    state = "normal"
    while i < n:
        c = source[i]
        nxt = source[i + 1] if i + 1 < n else ""
        if state == "normal":
            if c == "/" and nxt == "/":
                out[i] = out[i + 1] = " "
                i += 2
                state = "line_comment"
                continue
            if c == "/" and nxt == "*":
                out[i] = out[i + 1] = " "
                i += 2
                state = "block_comment"
                continue
            if c == '"':
                out[i] = " "
                i += 1
                state = "string"
                continue
            if c == "'":
                out[i] = " "
                i += 1
                state = "char"
                continue
            i += 1
            continue
        if state == "line_comment":
            if c == "\n":
                state = "normal"
            else:
                out[i] = " "
            i += 1
            continue
        if state == "block_comment":
            if c == "*" and nxt == "/":
                out[i] = out[i + 1] = " "
                i += 2
                state = "normal"
            else:
                if c != "\n":
                    out[i] = " "
                i += 1
            continue
        if state == "string":
            if c == "\\":
                out[i] = " "
                if i + 1 < n:
                    out[i + 1] = " "
                i += 2
                continue
            if c == '"':
                out[i] = " "
                i += 1
                state = "normal"
                continue
            if c != "\n":
                out[i] = " "
            i += 1
            continue
        if state == "char":
            if c == "\\":
                out[i] = " "
                if i + 1 < n:
                    out[i + 1] = " "
                i += 2
                continue
            if c == "'":
                out[i] = " "
                i += 1
                state = "normal"
                continue
            if c != "\n":
                out[i] = " "
            i += 1
            continue
    return "".join(out)


def line_of(source: str, offset: int) -> int:
    return source.count("\n", 0, max(0, offset)) + 1


def normalize_path(path: str) -> str:
    path = (path or "").strip()
    if not path:
        return "/"
    path = path.split("?", 1)[0].split("#", 1)[0].strip()
    path = path.replace("\\", "/")
    while "//" in path:
        path = path.replace("//", "/")
    if not path.startswith("/"):
        path = "/" + path
    if len(path) > 1 and path.endswith("/"):
        path = path[:-1]
    return path


def join_paths(left: str, right: str) -> str:
    left = "" if left in ("", "/") else left
    right = "" if right in ("", "/") else right
    if not left and not right:
        return "/"
    return normalize_path(f"{left}/{right}")


def path_matches(route: str, requested: str, exact_only: bool = False) -> bool:
    route = normalize_path(route)
    requested = normalize_path(requested)
    if route == requested:
        return True
    if exact_only:
        return False
    pattern = re.escape(route)
    pattern = re.sub(r"\\\{[^/{}]+\\\}", r"[^/]+", pattern)
    return re.fullmatch(pattern, requested) is not None


def split_top_level(text: str, delimiter: str = ",") -> List[str]:
    parts: List[str] = []
    start = 0
    depth_round = depth_curly = depth_angle = 0
    in_string = False
    in_char = False
    escape = False
    for i, c in enumerate(text):
        if in_string:
            if escape:
                escape = False
            elif c == "\\":
                escape = True
            elif c == '"':
                in_string = False
            continue
        if in_char:
            if escape:
                escape = False
            elif c == "\\":
                escape = True
            elif c == "'":
                in_char = False
            continue
        if c == '"':
            in_string = True
            continue
        if c == "'":
            in_char = True
            continue
        if c == "(":
            depth_round += 1
        elif c == ")":
            depth_round = max(0, depth_round - 1)
        elif c == "{":
            depth_curly += 1
        elif c == "}":
            depth_curly = max(0, depth_curly - 1)
        elif c == "<":
            depth_angle += 1
        elif c == ">":
            depth_angle = max(0, depth_angle - 1)
        elif (
            c == delimiter
            and depth_round == 0
            and depth_curly == 0
            and depth_angle == 0
        ):
            parts.append(text[start:i].strip())
            start = i + 1
    tail = text[start:].strip()
    if tail:
        parts.append(tail)
    return parts


def split_concat(text: str) -> List[str]:
    return split_top_level(text, "+")


def remove_generics(type_text: str) -> str:
    out = []
    depth = 0
    for c in type_text:
        if c == "<":
            depth += 1
            continue
        if c == ">":
            depth = max(0, depth - 1)
            continue
        if depth == 0:
            out.append(c)
    return "".join(out)


def clean_type(type_text: str) -> str:
    type_text = re.sub(r"@\w+(?:\([^)]*\))?", " ", type_text)
    type_text = re.sub(r"\b(final|volatile|transient)\b", " ", type_text)
    type_text = remove_generics(type_text)
    type_text = type_text.replace("[]", " ")
    type_text = type_text.replace("...", " ")
    type_text = " ".join(type_text.strip().split())
    if not type_text:
        return ""
    return type_text.split()[-1]


def parse_string_literal(expr: str) -> Optional[str]:
    expr = expr.strip()
    if len(expr) < 2 or not (expr.startswith('"') and expr.endswith('"')):
        return None
    body = expr[1:-1]
    try:
        return bytes(body, "utf-8").decode("unicode_escape")
    except Exception:
        return body


def find_matching(source: str, start: int, open_char: str, close_char: str) -> int:
    depth = 1
    i = start + 1
    n = len(source)
    while i < n:
        c = source[i]
        if c == open_char:
            depth += 1
        elif c == close_char:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def extract_annotation_invocations(text: str) -> List[Tuple[str, str]]:
    annotations: List[Tuple[str, str]] = []
    i = 0
    n = len(text)
    while i < n:
        at = text.find("@", i)
        if at < 0:
            break
        m = re.match(r"@([A-Za-z_][\w.]*)(\s*)", text[at:])
        if not m:
            i = at + 1
            continue
        name = m.group(1).split(".")[-1]
        pos = at + m.end()
        args = ""
        while pos < n and text[pos].isspace():
            pos += 1
        if pos < n and text[pos] == "(":
            end = find_annotation_paren(text, pos)
            if end >= 0:
                args = text[pos + 1 : end]
                pos = end + 1
        annotations.append((name, args))
        i = pos
    return annotations


def find_annotation_paren(text: str, start: int) -> int:
    depth = 1
    i = start + 1
    in_string = False
    in_char = False
    escape = False
    while i < len(text):
        c = text[i]
        if in_string:
            if escape:
                escape = False
            elif c == "\\":
                escape = True
            elif c == '"':
                in_string = False
            i += 1
            continue
        if in_char:
            if escape:
                escape = False
            elif c == "\\":
                escape = True
            elif c == "'":
                in_char = False
            i += 1
            continue
        if c == '"':
            in_string = True
        elif c == "'":
            in_char = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def parse_annotation_args(args: str) -> List[str]:
    if not args.strip():
        return [""]
    values: List[str] = []
    unnamed: List[str] = []
    for part in split_top_level(args, ","):
        if not part:
            continue
        key = None
        value = part
        pieces = split_top_level(part, "=")
        if len(pieces) == 2 and re.fullmatch(r"[A-Za-z_]\w*", pieces[0].strip()):
            key = pieces[0].strip()
            value = pieces[1].strip()
        if key in ("value", "path"):
            values.append(value)
        elif key is None:
            unnamed.append(value)
    return values or unnamed or [""]


def looks_like_method_signature(masked_segment: str, class_name: str) -> Optional[Tuple[str, int, int]]:
    leading = len(masked_segment) - len(masked_segment.lstrip())
    segment = masked_segment[leading:].rstrip()
    if "(" not in segment or ")" not in segment:
        return None
    if re.search(r"=\s*(new\s+)?[A-Za-z_][\w.]*\s*\([^;]*$", segment):
        return None
    for call in re.finditer(r"([A-Za-z_]\w*)\s*\(", segment):
        name = call.group(1)
        if name in CALL_KEYWORDS:
            continue
        if call.start(1) > 0 and segment[call.start(1) - 1] in ".@":
            continue
        close = find_matching(segment, call.end() - 1, "(", ")")
        if close < 0:
            continue
        tail = segment[close + 1 :].strip()
        tail = tail[:-1].strip() if tail.endswith(";") else tail
        if tail and not re.fullmatch(r"throws\s+[\w.$<>, ?\[\]\n\r\t]+", tail):
            continue
        before = segment[: call.start(1)].strip()
        if not before:
            continue
        if before.endswith("."):
            continue
        if re.search(r"\b(return|throw|new|case)\s+$", before):
            continue
        return name, leading + call.start(1), leading + call.end(1)
    return None


def leading_annotation_block(source: str, decl_start: int) -> str:
    """Return annotations immediately preceding a class or member declaration."""
    prefix = source[:decl_start]
    lines = prefix.splitlines(True)
    selected: List[str] = []
    collecting = False
    paren_balance = 0
    brace_balance = 0
    for line in reversed(lines):
        stripped = line.strip()
        if not stripped:
            if collecting and paren_balance <= 0 and brace_balance <= 0:
                break
            continue
        opens = stripped.count("(")
        closes = stripped.count(")")
        brace_opens = stripped.count("{")
        brace_closes = stripped.count("}")
        if stripped.startswith("@") or collecting:
            selected.append(line)
            paren_balance += closes - opens
            brace_balance += brace_closes - brace_opens
            collecting = True
            if stripped.startswith("@") and paren_balance >= 0 and brace_balance >= 0:
                paren_balance = 0
                brace_balance = 0
            continue
        break
    return "".join(reversed(selected))


def parse_params(signature: str) -> Dict[str, str]:
    start = signature.find("(")
    end = signature.rfind(")")
    if start < 0 or end < start:
        return {}
    params_text = signature[start + 1 : end].strip()
    if not params_text:
        return {}
    params: Dict[str, str] = {}
    for raw in split_top_level(params_text, ","):
        raw = re.sub(r"@\w+(?:\([^)]*\))?", " ", raw)
        raw = " ".join(raw.replace("\n", " ").split())
        if not raw:
            continue
        is_varargs = "..." in raw
        tokens = raw.split()
        if len(tokens) < 2:
            continue
        name = tokens[-1].replace("...", "").replace("[]", "")
        name = re.sub(r"[^A-Za-z0-9_].*$", "", name)
        type_text = " ".join(tokens[:-1])
        ctype = clean_type(type_text)
        if ctype and is_varargs:
            ctype = ctype + "..."
        if name and ctype:
            params[name] = ctype
    return params


class JavaProjectAnalyzer:
    def __init__(
        self,
        root: Path,
        include_tests: bool = False,
        exact_route_match: bool = False,
        max_depth: int = 80,
        delete_xml: bool = True,
        verbose: bool = False,
    ) -> None:
        self.root = root.resolve()
        self.include_tests = include_tests
        self.exact_route_match = exact_route_match
        self.max_depth = max_depth
        self.delete_xml = delete_xml
        self.verbose = verbose

        self.classes: Dict[str, ClassInfo] = {}
        self.simple_to_fqns: Dict[str, Set[str]] = defaultdict(set)
        self.methods: Dict[MethodKey, MethodInfo] = {}
        self.methods_by_class: Dict[str, Dict[str, List[MethodInfo]]] = defaultdict(
            lambda: defaultdict(list)
        )
        self.constants_raw: Dict[Tuple[str, str], str] = {}
        self.constants: Dict[Tuple[str, str], str] = {}
        self.interface_impls: Dict[str, Set[str]] = defaultdict(set)
        self.graph: Dict[MethodKey, Set[MethodKey]] = defaultdict(set)
        self.ordered_graph: Dict[MethodKey, List[MethodKey]] = defaultdict(list)
        self.incoming: Dict[MethodKey, Set[MethodKey]] = defaultdict(set)
        self.external_anchors: Dict[MethodKey, List[str]] = defaultdict(list)
        self.xml_namespaces: Dict[str, List[Path]] = defaultdict(list)

    def index(self) -> None:
        java_files = list(self.iter_files(JAVA_SUFFIX))
        for path in java_files:
            for cls in self.parse_java_file(path):
                self.classes[cls.fqn] = cls
                self.simple_to_fqns[cls.name].add(cls.fqn)

        for cls in self.classes.values():
            for name, expr in cls.raw_constants.items():
                self.constants_raw[(cls.fqn, name)] = expr

        for key in list(self.constants_raw):
            value = self.resolve_constant(key[0], key[1], set())
            if value is not None:
                self.constants[key] = value

        for cls in self.classes.values():
            cls.constants = {
                name: self.constants[(cls.fqn, name)]
                for name in cls.raw_constants
                if (cls.fqn, name) in self.constants
            }
            cls.route_paths = self.extract_mapping_paths(cls.annotations_text, cls)
            for method in cls.methods:
                method.route_paths = self.extract_mapping_paths(
                    method.annotations_text, cls
                )
                method.annotation_names = {
                    name for name, _ in extract_annotation_invocations(method.annotations_text)
                }
                self.methods[method.key] = method
                self.methods_by_class[method.class_fqn][method.name].append(method)
                if method.annotation_names & ENTRYPOINT_ANNOTATIONS:
                    self.external_anchors[method.key].append(
                        "framework entrypoint annotation"
                    )
                guard_reason = self.deletion_guard_reason(method)
                if guard_reason:
                    self.external_anchors[method.key].append(guard_reason)

        self.build_implementation_index()
        self.build_call_graph()
        self.add_controller_anchors()
        self.add_interface_contract_edges()
        self.build_incoming()
        self.index_xml_namespaces()

    def deletion_guard_reason(self, method: MethodInfo) -> Optional[str]:
        cls = self.classes.get(method.class_fqn)
        if cls and cls.kind == "enum":
            return "guard: enum class code is never deleted"
        if self.is_java_bean_accessor(method):
            return "guard: JavaBean getter/setter is never deleted"
        return None

    @staticmethod
    def is_java_bean_accessor(method: MethodInfo) -> bool:
        if method.route_paths:
            return False
        if method.is_constructor:
            return False
        param_count = len(method.params)
        if param_count == 0 and (
            re.fullmatch(r"get[A-Z_].*", method.name)
            or re.fullmatch(r"is[A-Z_].*", method.name)
        ):
            return True
        if param_count == 1 and re.fullmatch(r"set[A-Z_].*", method.name):
            return True
        return False

    def iter_files(self, suffix: str) -> Iterable[Path]:
        for dirpath, dirnames, filenames in os.walk(self.root):
            current = Path(dirpath)
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            if not self.include_tests:
                lowered = str(current).replace("\\", "/").lower()
                if "/src/test/" in lowered or lowered.endswith("/src/test"):
                    dirnames[:] = []
                    continue
            for filename in filenames:
                if filename.endswith(suffix):
                    yield current / filename

    def parse_java_file(self, path: Path) -> List[ClassInfo]:
        source = read_text(path)
        masked = mask_java(source)
        package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", masked)
        package = package_match.group(1) if package_match else ""
        imports, static_imports, static_wildcards = self.parse_imports(masked)
        classes: List[ClassInfo] = []
        for match in re.finditer(
            r"((?:public|protected|private|abstract|final|static)\s+)*"
            r"\b(class|interface|enum)\s+([A-Za-z_]\w*)\b",
            masked,
            flags=re.S,
        ):
            if self.brace_depth(masked, match.start()) != 0:
                continue
            open_brace = masked.find("{", match.end())
            if open_brace < 0:
                continue
            close_brace = find_matching(masked, open_brace, "{", "}")
            if close_brace < 0:
                continue
            kind = match.group(2)
            name = match.group(3)
            fqn = f"{package}.{name}" if package else name
            decl = masked[match.end() : open_brace]
            extends, implements = self.parse_type_relations(decl)
            annotations_text = leading_annotation_block(source, match.start())
            cls = ClassInfo(
                path=path,
                package=package,
                name=name,
                fqn=fqn,
                kind=kind,
                source=source,
                masked=masked,
                class_start=match.start(),
                body_start=open_brace,
                body_end=close_brace,
                annotations_text=annotations_text,
                imports=imports,
                static_imports=static_imports,
                static_wildcards=static_wildcards,
                extends=extends,
                implements=implements,
            )
            self.parse_members(cls)
            classes.append(cls)
        return classes

    @staticmethod
    def parse_imports(masked: str) -> Tuple[Dict[str, str], Dict[str, str], List[str]]:
        imports: Dict[str, str] = {}
        static_imports: Dict[str, str] = {}
        static_wildcards: List[str] = []
        for m in re.finditer(r"\bimport\s+(static\s+)?([\w.*]+)\s*;", masked):
            is_static = bool(m.group(1))
            target = m.group(2)
            if is_static:
                if target.endswith(".*"):
                    static_wildcards.append(target[:-2])
                else:
                    static_imports[target.split(".")[-1]] = target
            else:
                imports[target.split(".")[-1]] = target
        return imports, static_imports, static_wildcards

    @staticmethod
    def parse_type_relations(decl: str) -> Tuple[List[str], List[str]]:
        extends: List[str] = []
        implements: List[str] = []
        ext = re.search(r"\bextends\s+(.+?)(?=\bimplements\b|$)", decl, flags=re.S)
        if ext:
            extends = [p.strip() for p in split_top_level(ext.group(1), ",") if p.strip()]
        impl = re.search(r"\bimplements\s+(.+)$", decl, flags=re.S)
        if impl:
            implements = [
                p.strip() for p in split_top_level(impl.group(1), ",") if p.strip()
            ]
        return extends, implements

    @staticmethod
    def brace_depth(masked: str, offset: int) -> int:
        depth = 0
        for c in masked[:offset]:
            if c == "{":
                depth += 1
            elif c == "}":
                depth = max(0, depth - 1)
        return depth

    def parse_members(self, cls: ClassInfo) -> None:
        masked = cls.masked
        source = cls.source
        i = cls.body_start + 1
        member_start = i
        depth = 0
        while i < cls.body_end:
            c = masked[i]
            if c == "{" and depth == 0:
                segment = masked[member_start:i]
                parsed = looks_like_method_signature(segment, cls.name)
                close = find_matching(masked, i, "{", "}")
                if parsed and close > i:
                    self.add_method(cls, member_start, i, close + 1, body_start=i, body_end=close)
                    i = close + 1
                    member_start = i
                    continue
                depth += 1
            elif c == "{":
                depth += 1
            elif c == "}":
                depth = max(0, depth - 1)
            elif c == ";" and depth == 0:
                segment = masked[member_start : i + 1]
                if looks_like_method_signature(segment, cls.name):
                    self.add_method(
                        cls,
                        member_start,
                        i + 1,
                        i + 1,
                        body_start=None,
                        body_end=None,
                        is_abstract=True,
                    )
                else:
                    self.parse_field_or_constant(cls, member_start, i + 1)
                member_start = i + 1
            i += 1

    def add_method(
        self,
        cls: ClassInfo,
        member_start: int,
        sig_end: int,
        full_end: int,
        body_start: Optional[int],
        body_end: Optional[int],
        is_abstract: bool = False,
    ) -> None:
        source = cls.source
        masked = cls.masked
        raw_segment = source[member_start:sig_end]
        masked_segment = masked[member_start:sig_end]
        parsed = looks_like_method_signature(masked_segment, cls.name)
        if not parsed:
            return
        name = parsed[0]
        if name == cls.name:
            return
        rel_name_start = parsed[1]
        sig_start = member_start + self.signature_start_offset(masked_segment, rel_name_start)
        signature = source[sig_start:sig_end].strip()
        annotations_text = leading_annotation_block(source, sig_start)
        key = MethodKey(cls.fqn, name, sig_start)
        method = MethodInfo(
            key=key,
            class_fqn=cls.fqn,
            class_name=cls.name,
            name=name,
            path=cls.path,
            line=line_of(source, sig_start),
            full_start=member_start,
            full_end=full_end,
            sig_start=sig_start,
            sig_end=sig_end,
            body_start=body_start,
            body_end=body_end,
            signature=signature,
            annotations_text=annotations_text,
            params=parse_params(signature),
            is_abstract=is_abstract,
        )
        cls.methods.append(method)

    @staticmethod
    def signature_start_offset(masked_segment: str, rel_name_start: int) -> int:
        prefix = masked_segment[:rel_name_start]
        lines = prefix.splitlines(True)
        offset = 0
        for line in lines:
            stripped = line.strip()
            if not stripped:
                offset += len(line)
                continue
            if stripped.startswith("@"):
                offset += len(line)
                continue
            if stripped.startswith("*") or stripped.startswith("/"):
                offset += len(line)
                continue
            break
        return offset

    def parse_field_or_constant(self, cls: ClassInfo, start: int, end: int) -> None:
        source_segment = cls.source[start:end]
        masked_segment = cls.masked[start:end]
        const_match = re.search(
            r"\bString\s+([A-Z][A-Z0-9_]*)\s*=\s*(.+?)\s*;",
            source_segment,
            flags=re.S,
        )
        if const_match:
            cls.raw_constants[const_match.group(1)] = const_match.group(2).strip()
        field_match = re.search(
            r"(?:^|\n)\s*(?:@[\w.]+(?:\([^;]*?\))?\s*)*"
            r"(?:public|protected|private|static|final|volatile|transient|\s)+"
            r"([A-Za-z_][\w.$<>, ?\[\]]+?)\s+([A-Za-z_]\w*)\s*(?:=|;)",
            masked_segment,
            flags=re.S,
        )
        if field_match:
            field_type = clean_type(field_match.group(1))
            field_name = field_match.group(2)
            if field_type and field_name:
                cls.fields[field_name] = field_type

    def resolve_constant(
        self, class_fqn: str, name: str, stack: Set[Tuple[str, str]]
    ) -> Optional[str]:
        key = (class_fqn, name)
        if key in self.constants:
            return self.constants[key]
        if key in stack:
            return None
        expr = self.constants_raw.get(key)
        if expr is None:
            return None
        cls = self.classes.get(class_fqn)
        if not cls:
            return None
        stack.add(key)
        value = self.eval_string_expr(expr, cls, stack)
        stack.discard(key)
        if value is not None:
            self.constants[key] = value
        return value

    def eval_string_expr(
        self, expr: str, cls: ClassInfo, stack: Optional[Set[Tuple[str, str]]] = None
    ) -> Optional[str]:
        expr = expr.strip()
        if not expr:
            return ""
        if expr.startswith("{") and expr.endswith("}"):
            return None
        literal = parse_string_literal(expr)
        if literal is not None:
            return literal
        parts = split_concat(expr)
        if len(parts) > 1:
            resolved: List[str] = []
            for part in parts:
                val = self.eval_string_expr(part, cls, stack)
                if val is None:
                    return None
                resolved.append(val)
            return "".join(resolved)
        return self.resolve_constant_expr(expr, cls, stack or set())

    def resolve_constant_expr(
        self, expr: str, cls: ClassInfo, stack: Set[Tuple[str, str]]
    ) -> Optional[str]:
        expr = expr.strip()
        if not re.fullmatch(r"[A-Za-z_][\w.]*", expr):
            return None
        if "." not in expr:
            if (cls.fqn, expr) in self.constants_raw:
                return self.resolve_constant(cls.fqn, expr, stack)
            static_target = cls.static_imports.get(expr)
            if static_target:
                owner = ".".join(static_target.split(".")[:-1])
                member = static_target.split(".")[-1]
                return self.resolve_constant(owner, member, stack)
            for owner in cls.static_wildcards:
                if (owner, expr) in self.constants_raw:
                    return self.resolve_constant(owner, expr, stack)
            return None
        owner_expr, member = expr.rsplit(".", 1)
        owner = self.resolve_type(owner_expr, cls)
        if owner and (owner, member) in self.constants_raw:
            return self.resolve_constant(owner, member, stack)
        return None

    def extract_mapping_paths(self, annotations_text: str, cls: ClassInfo) -> List[str]:
        paths: List[str] = []
        for name, args in extract_annotation_invocations(annotations_text):
            if name not in MAPPING_ANNOTATIONS:
                continue
            for expr in parse_annotation_args(args):
                values = self.eval_path_values(expr, cls)
                paths.extend(values)
        return [normalize_path(p) for p in paths] if paths else []

    def eval_path_values(self, expr: str, cls: ClassInfo) -> List[str]:
        expr = expr.strip()
        if not expr:
            return [""]
        if expr.startswith("{") and expr.endswith("}"):
            values: List[str] = []
            for item in split_top_level(expr[1:-1], ","):
                values.extend(self.eval_path_values(item, cls))
            return values
        value = self.eval_string_expr(expr, cls, set())
        return [value] if value is not None else []

    def resolve_type(self, type_expr: str, cls: ClassInfo) -> Optional[str]:
        type_expr = clean_type(type_expr)
        if not type_expr:
            return None
        if type_expr in self.classes:
            return type_expr
        simple = type_expr.split(".")[-1]
        if "." in type_expr and type_expr in self.classes:
            return type_expr
        imported = cls.imports.get(simple)
        if imported in self.classes:
            return imported
        same_package = f"{cls.package}.{simple}" if cls.package else simple
        if same_package in self.classes:
            return same_package
        fqns = self.simple_to_fqns.get(simple, set())
        if len(fqns) == 1:
            return next(iter(fqns))
        return None

    def build_implementation_index(self) -> None:
        for cls in self.classes.values():
            if cls.kind == "interface":
                continue
            for item in cls.implements:
                resolved = self.resolve_type(item, cls)
                if resolved:
                    self.interface_impls[resolved].add(cls.fqn)
            if cls.name.endswith("Impl"):
                base = cls.name[:-4]
                for candidate in (base, f"I{base}"):
                    for fqn in self.simple_to_fqns.get(candidate, set()):
                        if self.classes[fqn].kind == "interface":
                            self.interface_impls[fqn].add(cls.fqn)

    def build_call_graph(self) -> None:
        for cls in self.classes.values():
            for method in cls.methods:
                if method.body_start is None or method.body_end is None:
                    continue
                callees = self.extract_calls(cls, method)
                self.ordered_graph[method.key].extend(callees)
                self.graph[method.key].update(callees)

    def extract_calls(self, cls: ClassInfo, method: MethodInfo) -> List[MethodKey]:
        assert method.body_start is not None and method.body_end is not None
        body = cls.masked[method.body_start + 1 : method.body_end]
        original_body = cls.source[method.body_start + 1 : method.body_end]
        type_scope = dict(cls.fields)
        type_scope.update(method.params)
        type_scope.update(self.parse_local_vars(body))
        call_events: List[Tuple[int, MethodKey]] = []

        def add_call(offset: int, keys: Iterable[MethodKey]) -> None:
            for key in sort_method_keys(keys):
                if key != method.key:
                    call_events.append((offset, key))

        dotted_spans: List[Tuple[int, int]] = []
        for m in re.finditer(r"\b([A-Za-z_]\w*)\s*\.\s*([A-Za-z_]\w*)\s*\(", body):
            qualifier, call_name = m.group(1), m.group(2)
            dotted_spans.append((m.start(2), m.end(2)))
            if call_name in CALL_KEYWORDS:
                continue
            arg_types = self.infer_call_arg_types(original_body, m.end() - 1, type_scope)
            if qualifier == "this":
                add_call(m.start(), self.resolve_methods(cls.fqn, call_name, arg_types))
            elif qualifier == "super":
                for parent in cls.extends:
                    parent_fqn = self.resolve_type(parent, cls)
                    if parent_fqn:
                        add_call(m.start(), self.resolve_methods(parent_fqn, call_name, arg_types))
            elif qualifier in type_scope:
                target_fqn = self.resolve_type(type_scope[qualifier], cls)
                if target_fqn:
                    add_call(m.start(), self.resolve_methods_with_impls(target_fqn, call_name, arg_types))
            elif qualifier and qualifier[0].isupper():
                target_fqn = self.resolve_type(qualifier, cls)
                if target_fqn:
                    add_call(m.start(), self.resolve_methods_with_impls(target_fqn, call_name, arg_types))

        for m in re.finditer(r"(?<![.\w])([A-Za-z_]\w*)\s*\(", body):
            name = m.group(1)
            if name in CALL_KEYWORDS:
                continue
            if any(start <= m.start(1) < end for start, end in dotted_spans):
                continue
            arg_types = self.infer_call_arg_types(original_body, m.end() - 1, type_scope)
            add_call(m.start(), self.resolve_methods(cls.fqn, name, arg_types))
            static_target = cls.static_imports.get(name)
            if static_target:
                owner = ".".join(static_target.split(".")[:-1])
                add_call(m.start(), self.resolve_methods_with_impls(owner, name, arg_types))

        for m in re.finditer(r"\b([A-Za-z_]\w*)\s*::\s*([A-Za-z_]\w*)", body):
            qualifier, name = m.group(1), m.group(2)
            if qualifier == "this":
                add_call(m.start(), self.resolve_methods(cls.fqn, name, None))
            else:
                target_fqn = self.resolve_type(qualifier, cls)
                if target_fqn:
                    add_call(m.start(), self.resolve_methods_with_impls(target_fqn, name, None))

        ordered: List[MethodKey] = []
        seen: Set[MethodKey] = set()
        for _offset, key in sorted(call_events, key=lambda item: (item[0], item[1].text())):
            if key not in seen:
                ordered.append(key)
                seen.add(key)
        return ordered

    def infer_call_arg_types(
        self,
        original_body: str,
        paren_start: int,
        type_scope: Dict[str, str],
    ) -> Optional[List[str]]:
        close = find_annotation_paren(original_body, paren_start)
        if close < 0:
            return None
        args_text = original_body[paren_start + 1 : close].strip()
        if not args_text:
            return []
        return [self.infer_expr_type(arg, type_scope) for arg in split_top_level(args_text, ",")]

    def infer_expr_type(self, expr: str, type_scope: Dict[str, str]) -> str:
        expr = expr.strip()
        if not expr:
            return "Unknown"
        expr = strip_wrapping_parentheses(expr)
        if re.fullmatch(r'"(?:\\.|[^"\\])*"', expr):
            return "String"
        if re.fullmatch(r"'(?:\\.|[^'\\])'", expr):
            return "char"
        if re.fullmatch(r"true|false", expr):
            return "boolean"
        if re.fullmatch(r"-?\d+[lL]", expr):
            return "long"
        if re.fullmatch(r"-?\d+", expr):
            return "int"
        if re.fullmatch(r"-?\d+\.\d+[fF]", expr):
            return "float"
        if re.fullmatch(r"-?\d+\.\d+([dD])?", expr):
            return "double"
        if expr == "null":
            return "null"
        cast_match = re.match(r"^\(\s*([A-Za-z_][\w.$<>?,\s\[\]]+)\s*\)\s*.+$", expr)
        if cast_match:
            return clean_type(cast_match.group(1)) or "Unknown"
        new_match = re.match(r"^new\s+([A-Za-z_][\w.$<>?,\s\[\]]+)", expr)
        if new_match:
            return clean_type(new_match.group(1)) or "Unknown"
        if re.fullmatch(r"[A-Za-z_]\w*", expr) and expr in type_scope:
            return type_scope[expr]
        if "." in expr:
            tail = expr.rsplit(".", 1)[-1]
            if re.fullmatch(r"[A-Z][A-Z0-9_]*", tail):
                return "Unknown"
        return "Unknown"

    @staticmethod
    def parse_local_vars(masked_body: str) -> Dict[str, str]:
        found: Dict[str, str] = {}
        decl_pattern = re.compile(
            r"(?:^|[;{}\n]\s*)"
            r"(?:final\s+)?"
            r"([A-Z][\w.$]*(?:\s*<[^;{}()=]*>)?(?:\s*\[\])?)"
            r"\s+([a-zA-Z_]\w*)\s*(?==|;|,)",
            flags=re.S,
        )
        for m in decl_pattern.finditer(masked_body):
            ctype = clean_type(m.group(1))
            name = m.group(2)
            if ctype and name:
                found[name] = ctype
        for m in re.finditer(
            r"for\s*\(\s*(?:final\s+)?([A-Z][\w.$<>?,\s\[\]]+)\s+([a-zA-Z_]\w*)\s*:",
            masked_body,
            flags=re.S,
        ):
            ctype = clean_type(m.group(1))
            name = m.group(2)
            if ctype and name:
                found[name] = ctype
        return found

    def resolve_methods(
        self,
        class_fqn: str,
        method_name: str,
        arg_types: Optional[List[str]] = None,
    ) -> Set[MethodKey]:
        methods = list(self.methods_by_class.get(class_fqn, {}).get(method_name, []))
        methods = self.filter_overloads(methods, arg_types)
        return {method.key for method in methods}

    def resolve_methods_with_impls(
        self,
        class_fqn: str,
        method_name: str,
        arg_types: Optional[List[str]] = None,
    ) -> Set[MethodKey]:
        keys = set(self.resolve_methods(class_fqn, method_name, arg_types))
        cls = self.classes.get(class_fqn)
        if cls and cls.kind == "interface":
            for impl in self.interface_impls.get(class_fqn, set()):
                keys.update(self.resolve_methods(impl, method_name, arg_types))
        return keys

    def filter_overloads(
        self,
        methods: List[MethodInfo],
        arg_types: Optional[List[str]],
    ) -> List[MethodInfo]:
        if arg_types is None or len(methods) <= 1:
            return methods
        arity_matches = [
            method for method in methods
            if self.method_accepts_arg_count(method, len(arg_types))
        ]
        if not arity_matches:
            return methods
        exact_matches = [
            method for method in arity_matches
            if self.method_accepts_arg_types(method, arg_types)
        ]
        return exact_matches or arity_matches

    @staticmethod
    def method_accepts_arg_count(method: MethodInfo, count: int) -> bool:
        param_types = method.param_types
        if param_types and param_types[-1].endswith("..."):
            return count >= len(param_types) - 1
        return len(param_types) == count

    def method_accepts_arg_types(self, method: MethodInfo, arg_types: List[str]) -> bool:
        param_types = method.param_types
        fixed_count = len(param_types)
        vararg_type: Optional[str] = None
        if param_types and param_types[-1].endswith("..."):
            fixed_count -= 1
            vararg_type = param_types[-1][:-3]
        for idx, arg_type in enumerate(arg_types):
            param_type = param_types[idx] if idx < fixed_count else vararg_type
            if param_type is None:
                return False
            if not self.type_compatible(arg_type, param_type):
                return False
        return True

    @staticmethod
    def type_compatible(arg_type: str, param_type: str) -> bool:
        arg_type = normalize_type_name(arg_type)
        param_type = normalize_type_name(param_type)
        if arg_type in ("unknown", "null"):
            return True
        if arg_type == param_type:
            return True
        primitive_wrappers = {
            "int": "integer",
            "long": "long",
            "double": "double",
            "float": "float",
            "boolean": "boolean",
            "char": "character",
            "byte": "byte",
            "short": "short",
        }
        if primitive_wrappers.get(arg_type) == param_type:
            return True
        if primitive_wrappers.get(param_type) == arg_type:
            return True
        if param_type == "object":
            return True
        numeric = {"byte", "short", "int", "integer", "long", "float", "double"}
        if arg_type in numeric and param_type in numeric:
            return True
        return False

    def add_controller_anchors(self) -> None:
        for method in self.methods.values():
            if method.route_paths:
                self.external_anchors[method.key].append("Spring MVC route")

    def add_interface_contract_edges(self) -> None:
        for interface_fqn, impls in self.interface_impls.items():
            interface_methods = self.methods_by_class.get(interface_fqn, {})
            for name, methods in interface_methods.items():
                for interface_method in methods:
                    for impl in impls:
                        for impl_method in self.methods_by_class.get(impl, {}).get(name, []):
                            self.graph[interface_method.key].add(impl_method.key)
                            self.graph[impl_method.key].add(interface_method.key)
                            append_unique(self.ordered_graph[interface_method.key], impl_method.key)
                            append_unique(self.ordered_graph[impl_method.key], interface_method.key)

    def build_incoming(self) -> None:
        self.incoming.clear()
        for caller, callees in self.graph.items():
            for callee in callees:
                self.incoming[callee].add(caller)

    def index_xml_namespaces(self) -> None:
        for path in self.iter_files(XML_SUFFIX):
            text = read_text(path)
            for m in re.finditer(r'\bnamespace\s*=\s*["\']([^"\']+)["\']', text):
                self.xml_namespaces[m.group(1)].append(path)

    def iter_routes(self) -> Iterable[RouteMatch]:
        for cls in self.classes.values():
            class_paths = cls.route_paths or [""]
            for method in cls.methods:
                if not method.route_paths:
                    continue
                for cp in class_paths:
                    for mp in method.route_paths:
                        route = join_paths(cp, mp)
                        yield RouteMatch(route, method, route)

    def locate_routes(self, requested_paths: Sequence[str]) -> Tuple[List[RouteMatch], List[str]]:
        matches: List[RouteMatch] = []
        unresolved: List[str] = []
        seen_roots: Set[MethodKey] = set()
        for raw_path in requested_paths:
            requested = normalize_path(raw_path)
            current_matches: List[RouteMatch] = []
            for route_match in self.iter_routes():
                route = route_match.matched_route
                if path_matches(route, requested, self.exact_route_match):
                    current_matches.append(RouteMatch(requested, route_match.method, route))
            if not current_matches:
                unresolved.append(requested)
                continue
            for match in current_matches:
                if match.method.key not in seen_roots:
                    matches.append(match)
                    seen_roots.add(match.method.key)
        return matches, unresolved

    def suggest_routes(self, requested: str, limit: int = 8) -> List[RouteMatch]:
        requested = normalize_path(requested)
        scored: List[Tuple[float, int, str, RouteMatch]] = []
        request_tokens = route_tokens(requested)
        for route_match in self.iter_routes():
            route = normalize_path(route_match.matched_route)
            route_tokens_set = route_tokens(route)
            common = len(request_tokens & route_tokens_set)
            union = len(request_tokens | route_tokens_set) or 1
            token_score = common / union
            suffix_score = common_suffix_score(requested, route)
            substring_score = 0.0
            req_low = requested.lower()
            route_low = route.lower()
            if req_low in route_low or route_low in req_low:
                substring_score = 1.0
            elif request_tokens and request_tokens <= route_tokens_set:
                substring_score = 0.7
            score = token_score * 3.0 + suffix_score * 2.0 + substring_score
            if score <= 0:
                continue
            scored.append((score, common, route, route_match))
        scored.sort(key=lambda item: (-item[0], -item[1], item[2]))
        return [item[3] for item in scored[:limit]]

    def build_plan(self, requested_paths: Sequence[str]) -> CleanupPlan:
        roots, unresolved = self.locate_routes(requested_paths)
        unresolved_suggestions = {
            path: self.suggest_routes(path)
            for path in unresolved
        }
        root_keys = {match.method.key for match in roots}
        closure = self.reachable_closure(root_keys)
        candidates = set(closure)
        changed = True
        while changed:
            changed = False
            for key in list(candidates):
                if key in root_keys:
                    continue
                outside_callers = self.incoming.get(key, set()) - candidates
                anchored = bool(self.external_anchors.get(key))
                if outside_callers or anchored:
                    candidates.remove(key)
                    changed = True

        retained = closure - candidates
        root_warnings: Dict[str, List[str]] = {}
        for key in root_keys:
            external = self.incoming.get(key, set()) - root_keys
            anchors = [a for a in self.external_anchors.get(key, []) if a != "Spring MVC route"]
            if external or anchors:
                root_warnings[key.text()] = self.format_reasons(external, anchors)

        retain_reasons: Dict[str, List[str]] = {}
        for key in retained:
            external = self.incoming.get(key, set()) - candidates
            anchors = self.external_anchors.get(key, [])
            retain_reasons[key.text()] = self.format_reasons(external, anchors)

        return CleanupPlan(
            roots=roots,
            closure=closure,
            removable=candidates,
            retained=retained,
            root_warnings=root_warnings,
            retain_reasons=retain_reasons,
            unresolved_paths=unresolved,
            unresolved_suggestions=unresolved_suggestions,
        )

    def reachable_closure(self, roots: Set[MethodKey]) -> Set[MethodKey]:
        closure: Set[MethodKey] = set(roots)
        queue: deque[Tuple[MethodKey, int]] = deque((root, 0) for root in roots)
        while queue:
            key, depth = queue.popleft()
            if depth >= self.max_depth:
                continue
            for callee in self.graph.get(key, set()):
                if callee not in closure:
                    closure.add(callee)
                    queue.append((callee, depth + 1))
        return closure

    def format_reasons(
        self, callers: Iterable[MethodKey], anchors: Iterable[str]
    ) -> List[str]:
        reasons = [f"anchor: {anchor}" for anchor in anchors]
        for caller in sorted(callers, key=lambda k: k.text()):
            method = self.methods.get(caller)
            if method:
                reasons.append(f"caller: {method.label} ({relpath(method.path, self.root)}:{method.line})")
            else:
                reasons.append(f"caller: {caller.text()}")
        return reasons or ["no explicit reason recorded"]

    def delete_plan(self, plan: CleanupPlan) -> Dict[str, object]:
        java_edits: Dict[Path, List[Tuple[int, int, str]]] = defaultdict(list)
        for key in plan.removable:
            method = self.methods.get(key)
            if not method:
                continue
            if self.deletion_guard_reason(method):
                continue
            java_edits[method.path].append(
                (method.full_start, method.full_end, method.label)
            )

        touched_java: List[str] = []
        for path, edits in java_edits.items():
            text = read_text(path)
            for start, end, _label in sorted(edits, key=lambda item: item[0], reverse=True):
                text = text[:start] + text[end:]
            write_text(path, text)
            touched_java.append(str(relpath(path, self.root)))

        touched_xml: List[str] = []
        if self.delete_xml:
            touched_xml = self.delete_xml_removals(plan)

        return {
            "java_files": sorted(touched_java),
            "xml_files": sorted(touched_xml),
        }

    def delete_xml_removals(self, plan: CleanupPlan) -> List[str]:
        removals_by_xml: Dict[Path, Set[str]] = defaultdict(set)
        for key in plan.removable:
            method = self.methods.get(key)
            if not method:
                continue
            if self.deletion_guard_reason(method):
                continue
            for xml_path in self.xml_namespaces.get(method.class_fqn, []):
                removals_by_xml[xml_path].add(method.name)

        touched: List[str] = []
        for path, method_names in removals_by_xml.items():
            text = read_text(path)
            new_text = text
            for name in method_names:
                new_text = remove_mybatis_statement(new_text, name)
            if new_text != text:
                write_text(path, new_text)
                touched.append(str(relpath(path, self.root)))
        return touched

    def report_dict(self, plan: CleanupPlan, deleted: Optional[Dict[str, object]] = None) -> Dict[str, object]:
        return {
            "root": str(self.root),
            "delete": deleted is not None,
            "routes": [
                {
                    "requestedPath": match.requested_path,
                    "matchedRoute": normalize_path(match.matched_route),
                    "method": match.method.label,
                    "file": str(relpath(match.method.path, self.root)),
                    "line": match.method.line,
                }
                for match in plan.roots
            ],
            "apiTrees": self.api_tree_reports(plan),
            "unresolvedPaths": plan.unresolved_paths,
            "unresolvedSuggestions": {
                path: [
                    {
                        "matchedRoute": normalize_path(match.matched_route),
                        "method": match.method.label,
                        "file": str(relpath(match.method.path, self.root)),
                        "line": match.method.line,
                    }
                    for match in suggestions
                ]
                for path, suggestions in plan.unresolved_suggestions.items()
            },
            "summary": {
                "reachableMethods": len(plan.closure),
                "removableMethods": len(plan.removable),
                "retainedBoundaryMethods": len(plan.retained),
            },
            "removable": [self.method_report(key) for key in sorted(plan.removable, key=lambda k: k.text())],
            "retained": [
                {
                    **self.method_report(key),
                    "reasons": plan.retain_reasons.get(key.text(), []),
                }
                for key in sorted(plan.retained, key=lambda k: k.text())
            ],
            "rootWarnings": plan.root_warnings,
            "deletedFiles": deleted or {},
        }

    def api_tree_reports(self, plan: CleanupPlan) -> List[Dict[str, object]]:
        root_keys = {match.method.key for match in plan.roots}
        return [
            {
                "requestedPath": match.requested_path,
                "matchedRoute": normalize_path(match.matched_route),
                "root": self.method_report(match.method.key, include_code=True),
                "tree": self.call_tree_node(
                    match.method.key,
                    plan,
                    root_keys=root_keys,
                    stack=[],
                    depth=0,
                ),
            }
            for match in plan.roots
        ]

    def call_tree_node(
        self,
        key: MethodKey,
        plan: CleanupPlan,
        root_keys: Set[MethodKey],
        stack: List[MethodKey],
        depth: int,
    ) -> Dict[str, object]:
        method = self.methods[key]
        status = "delete" if key in plan.removable else "retain"
        reasons: List[str] = []
        if key in plan.retained:
            reasons = plan.retain_reasons.get(key.text(), [])
        elif key in root_keys:
            reasons = plan.root_warnings.get(key.text(), [])
        node: Dict[str, object] = {
            **self.method_report(key, include_code=True),
            "status": status,
            "depth": depth,
            "reasons": reasons,
            "children": [],
        }
        if key in stack:
            node["cycle"] = True
            node["children"] = []
            return node
        # A retained boundary means this branch is shared externally; show the
        # boundary node but do not expand deeper in the deletion preview.
        if key in plan.retained:
            return node
        children: List[Dict[str, object]] = []
        for child_key in self.ordered_children(key):
            if child_key not in plan.closure:
                continue
            children.append(
                self.call_tree_node(
                    child_key,
                    plan,
                    root_keys=root_keys,
                    stack=stack + [key],
                    depth=depth + 1,
                )
            )
        node["children"] = children
        return node

    def ordered_children(self, key: MethodKey) -> List[MethodKey]:
        ordered: List[MethodKey] = []
        seen: Set[MethodKey] = set()
        for child_key in self.ordered_graph.get(key, []):
            if child_key in self.graph.get(key, set()) and child_key not in seen:
                ordered.append(child_key)
                seen.add(child_key)
        for child_key in sort_method_keys(self.graph.get(key, set())):
            if child_key not in seen:
                ordered.append(child_key)
                seen.add(child_key)
        return ordered

    def method_report(self, key: MethodKey, include_code: bool = False) -> Dict[str, object]:
        method = self.methods[key]
        data: Dict[str, object] = {
            "method": method.label,
            "file": str(relpath(method.path, self.root)),
            "line": method.line,
            "abstract": method.is_abstract,
            "routes": method.route_paths,
        }
        if include_code:
            source = read_text(method.path)
            data["code"] = source[method.full_start : method.full_end].strip()
            data["startLine"] = line_of(source, method.full_start)
            data["endLine"] = line_of(source, method.full_end)
        return data


def remove_mybatis_statement(text: str, method_name: str) -> str:
    name = re.escape(method_name)
    for tag in STATEMENT_TAGS:
        pattern = re.compile(
            rf"\n?[ \t]*<{tag}\b(?=[^>]*\bid\s*=\s*['\"]{name}['\"])[^>]*>"
            rf".*?</{tag}>[ \t]*(?:\r?\n)?",
            flags=re.S,
        )
        text = pattern.sub("\n", text)
        self_closing = re.compile(
            rf"\n?[ \t]*<{tag}\b(?=[^>]*\bid\s*=\s*['\"]{name}['\"])[^>]*/>"
            rf"[ \t]*(?:\r?\n)?",
            flags=re.S,
        )
        text = self_closing.sub("\n", text)
    return text


def route_tokens(path: str) -> Set[str]:
    path = normalize_path(path).lower()
    pieces = [piece for piece in re.split(r"[/_.-]+", path) if piece]
    tokens: Set[str] = set()
    for piece in pieces:
        tokens.add(piece)
        for camel in re.findall(r"[a-z]+|[0-9]+", piece):
            if camel:
                tokens.add(camel)
    return tokens


def common_suffix_score(left: str, right: str) -> float:
    left_parts = [p.lower() for p in normalize_path(left).split("/") if p]
    right_parts = [p.lower() for p in normalize_path(right).split("/") if p]
    if not left_parts or not right_parts:
        return 0.0
    count = 0
    for a, b in zip(reversed(left_parts), reversed(right_parts)):
        if a != b:
            break
        count += 1
    return count / max(len(left_parts), len(right_parts))


def sort_method_keys(keys: Iterable[MethodKey]) -> List[MethodKey]:
    return sorted(keys, key=lambda key: key.text())


def append_unique(items: List[MethodKey], key: MethodKey) -> None:
    if key not in items:
        items.append(key)


def normalize_type_name(type_name: str) -> str:
    type_name = (type_name or "Unknown").strip()
    type_name = type_name[:-3] if type_name.endswith("...") else type_name
    type_name = type_name.replace("[]", "")
    type_name = type_name.split(".")[-1]
    return type_name.lower()


def strip_wrapping_parentheses(expr: str) -> str:
    expr = expr.strip()
    changed = True
    while changed and expr.startswith("(") and expr.endswith(")"):
        changed = False
        end = find_annotation_paren(expr, 0)
        if end == len(expr) - 1:
            expr = expr[1:-1].strip()
            changed = True
    return expr


def relpath(path: Path, root: Path) -> Path:
    try:
        return path.resolve().relative_to(root.resolve())
    except Exception:
        return path


def load_paths(args: argparse.Namespace) -> List[str]:
    paths: List[str] = []
    paths.extend(args.paths or [])
    paths.extend(args.positional_paths or [])
    if args.paths_file:
        for line in read_text(Path(args.paths_file)).splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                paths.append(line)
    deduped: List[str] = []
    seen: Set[str] = set()
    for path in paths:
        norm = normalize_path(path)
        if norm not in seen:
            deduped.append(norm)
            seen.add(norm)
    return deduped


class DeadApiCleanupService:
    """Reusable analysis/deletion service used by both CLI and Web UI."""

    def __init__(self, options: CleanupRunOptions) -> None:
        self.options = options

    def run(self, paths: Sequence[str]) -> CleanupRunResult:
        clean_paths = self.normalize_paths(paths)
        root = self.options.root.resolve()
        if not root.exists():
            raise FileNotFoundError(f"root does not exist: {root}")
        if not clean_paths:
            raise ValueError("provide at least one API path")

        analyzer = JavaProjectAnalyzer(
            root=root,
            include_tests=self.options.include_tests,
            exact_route_match=self.options.exact_route_match,
            max_depth=self.options.max_depth,
            delete_xml=self.options.delete_xml,
            verbose=self.options.verbose,
        )
        analyzer.index()
        plan = analyzer.build_plan(clean_paths)
        deleted = analyzer.delete_plan(plan) if self.options.delete else None
        report = analyzer.report_dict(plan, deleted)
        report_path = self.write_report(root, report)
        return CleanupRunResult(
            analyzer=analyzer,
            plan=plan,
            deleted=deleted,
            report=report,
            report_path=report_path,
        )

    @staticmethod
    def normalize_paths(paths: Sequence[str]) -> List[str]:
        deduped: List[str] = []
        seen: Set[str] = set()
        for path in paths:
            norm = normalize_path(path)
            if norm not in seen:
                deduped.append(norm)
                seen.add(norm)
        return deduped

    def write_report(self, root: Path, report: Dict[str, Any]) -> Optional[Path]:
        if self.options.no_report or not self.options.report:
            return None
        report_path = Path(self.options.report)
        if not report_path.is_absolute():
            report_path = root / report_path
        write_text(report_path, json.dumps(report, ensure_ascii=False, indent=2))
        return report_path


def print_console_summary(
    analyzer: JavaProjectAnalyzer,
    plan: CleanupPlan,
    deleted: Optional[Dict[str, object]],
    limit: int,
) -> None:
    mode = "DELETE" if deleted is not None else "DRY-RUN"
    print(f"[{mode}] API dead-code cleanup plan")
    print(f"Root: {analyzer.root}")
    print()
    if plan.roots:
        print("Matched routes:")
        for match in plan.roots:
            method = match.method
            print(
                f"  - {match.requested_path} -> {normalize_path(match.matched_route)} "
                f"=> {method.label} ({relpath(method.path, analyzer.root)}:{method.line})"
            )
    if plan.unresolved_paths:
        print("Unresolved paths:")
        for path in plan.unresolved_paths:
            print(f"  - {path}")
            suggestions = plan.unresolved_suggestions.get(path, [])
            if suggestions:
                print("    Similar routes:")
                for match in suggestions[:5]:
                    method = match.method
                    print(
                        f"      - {normalize_path(match.matched_route)} "
                        f"=> {method.label} ({relpath(method.path, analyzer.root)}:{method.line})"
                    )
    print()
    print(
        "Summary: "
        f"reachable={len(plan.closure)}, "
        f"removable={len(plan.removable)}, "
        f"retainedBoundary={len(plan.retained)}"
    )
    if plan.root_warnings:
        print()
        print("Root warnings:")
        for key, reasons in plan.root_warnings.items():
            print(f"  - {key}")
            for reason in reasons[:5]:
                print(f"      {reason}")
    print()
    print("Removable methods:")
    for idx, key in enumerate(sorted(plan.removable, key=lambda k: k.text())):
        if idx >= limit:
            print(f"  ... {len(plan.removable) - limit} more")
            break
        method = analyzer.methods[key]
        print(f"  - {method.label} ({relpath(method.path, analyzer.root)}:{method.line})")
    if plan.retained:
        print()
        print("Retained boundary methods:")
        for idx, key in enumerate(sorted(plan.retained, key=lambda k: k.text())):
            if idx >= limit:
                print(f"  ... {len(plan.retained) - limit} more")
                break
            method = analyzer.methods[key]
            reasons = plan.retain_reasons.get(key.text(), [])
            first_reason = f" [{reasons[0]}]" if reasons else ""
            print(
                f"  - {method.label} ({relpath(method.path, analyzer.root)}:{method.line})"
                f"{first_reason}"
            )
    if deleted is None:
        print()
        print("No files changed. Re-run with the delete command to delete the removable methods.")
    else:
        print()
        print("Modified files:")
        for group in ("java_files", "xml_files"):
            for path in deleted.get(group, []):
                print(f"  - {path}")


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Safely remove Java code reachable only from deprecated Spring API paths."
    )
    parser.add_argument(
        "action",
        nargs="?",
        default="analyze",
        help="Action to run: analyze or delete. If omitted, analyze is used.",
    )
    parser.add_argument("positional_paths", nargs="*", help="API paths to remove")
    parser.add_argument("--paths", nargs="+", help="API paths to remove")
    parser.add_argument("--paths-file", help="Text file containing one API path per line")
    parser.add_argument("--root", default=".", help="Project root, defaults to current directory")
    parser.add_argument(
        "--include-tests",
        action="store_true",
        help="Include src/test/java in reference analysis",
    )
    parser.add_argument(
        "--exact-route-match",
        action="store_true",
        help="Disable matching /foo/{id} against concrete paths such as /foo/1",
    )
    parser.add_argument(
        "--max-depth",
        type=int,
        default=80,
        help="Maximum method-call depth to traverse",
    )
    parser.add_argument(
        "--no-delete-xml",
        action="store_true",
        help="Do not remove MyBatis XML statements for deleted mapper methods",
    )
    parser.add_argument(
        "--report",
        default="dead_api_cleanup_report.json",
        help="JSON report path. Use empty string to disable",
    )
    parser.add_argument(
        "--no-report",
        action="store_true",
        help="Do not write a JSON report",
    )
    parser.add_argument("--limit", type=int, default=80, help="Console item limit")
    parser.add_argument("--verbose", action="store_true", help="Verbose diagnostics")
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(argv)
    if args.action not in ("analyze", "delete"):
        args.positional_paths.insert(0, args.action)
        args.action = "analyze"
    paths = load_paths(args)
    if not paths:
        parser.error("provide API paths via positional args, --paths, or --paths-file")

    root = Path(args.root).resolve()
    if not root.exists():
        parser.error(f"root does not exist: {root}")

    if args.no_report:
        args.report = ""

    options = CleanupRunOptions(
        root=root,
        include_tests=args.include_tests,
        exact_route_match=args.exact_route_match,
        max_depth=args.max_depth,
        delete_xml=not args.no_delete_xml,
        delete=args.action == "delete",
        report=args.report,
        no_report=args.no_report,
        verbose=args.verbose,
    )
    result = DeadApiCleanupService(options).run(paths)

    print_console_summary(result.analyzer, result.plan, result.deleted, args.limit)
    if result.plan.unresolved_paths:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
