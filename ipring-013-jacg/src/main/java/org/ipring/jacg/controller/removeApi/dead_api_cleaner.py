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

  # 7. Analyze an unused @Service implementation method. The target class
  #    must be annotated with @Service and its class name must end with Impl.
  python -B scripts/dead_api_cleaner.py --impl-only com.cds.track.service.TrackServiceImpl#deliveryFailNum

  # 8. Scan all @Service *Impl methods and list methods with no real callers.
  python -B scripts/dead_api_cleaner.py --impl-only

  # 9. Delete after reviewing the implementation-method dry-run output.
  python -B scripts/dead_api_cleaner.py delete --impl-only com.cds.track.service.TrackServiceImpl#deliveryFailNum

  # 10. Delete all scanned unused implementation methods after reviewing output.
  python -B scripts/dead_api_cleaner.py delete --impl-only

  # 11. Start the Web management page.
  python -B scripts/dead_api_cleaner_web.py --host 127.0.0.1 --port 8765

Recommended workflow:

  1. Commit or stash unrelated local changes first.
  2. Run dry-run and review "Removable methods" and "Retained boundary methods".
  3. Run the delete command only after the plan is acceptable.
  4. Run project compilation/tests after deletion.
  5. For implementation methods, prefer the Web "分析实现类" preview first,
     then use the separate "删除实现类" button only after reviewing the full
     deletion tree and retained boundary nodes.

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
  - With --impl-only it starts from @Service classes whose names end with Impl,
    and applies the same downward call-chain and external-reference checks.
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
REFERENCE_SUFFIXES = (
    ".java",
    ".xml",
    ".properties",
    ".yml",
    ".yaml",
    ".sql",
)

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
    unresolved_extends: List[str] = field(default_factory=list)
    unresolved_implements: List[str] = field(default_factory=list)


@dataclass
class MyBatisStatementInfo:
    namespace: str
    statement_id: str
    tag: str
    path: Path
    line: int
    start: int
    end: int


@dataclass
class RouteMatch:
    requested_path: str
    method: MethodInfo
    matched_route: str


@dataclass
class ImplMethodMatch:
    requested_target: str
    method: MethodInfo


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
    impl_roots: List[ImplMethodMatch] = field(default_factory=list)
    unresolved_impl_methods: List[str] = field(default_factory=list)
    impl_scan_all: bool = False
    unused_mappers: Set[MethodKey] = field(default_factory=set)
    mapper_retain_reasons: Dict[str, List[str]] = field(default_factory=dict)
    unused_mapper_files: Set[str] = field(default_factory=set)


@dataclass
class CleanupRunOptions:
    root: Path
    include_tests: bool = False
    exact_route_match: bool = False
    max_depth: int = 80
    delete_xml: bool = True
    scan_unused_mappers: bool = True
    mapper_only: bool = False
    impl_only: bool = False
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


def iter_quoted_values(text: str) -> Iterable[str]:
    for match in re.finditer(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', text):
        raw = match.group(0)
        body = raw[1:-1]
        yield (
            body.replace(r"\\", "\\")
            .replace(r"\"", '"')
            .replace(r"\'", "'")
        )


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
        scan_unused_mappers: bool = True,
        verbose: bool = False,
    ) -> None:
        self.root = root.resolve()
        self.include_tests = include_tests
        self.exact_route_match = exact_route_match
        self.max_depth = max_depth
        self.delete_xml = delete_xml
        self.scan_unused_mappers = scan_unused_mappers
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
        self.xml_statements: Dict[Tuple[str, str], List[MyBatisStatementInfo]] = defaultdict(list)
        self.reference_texts: Optional[List[Tuple[Path, str]]] = None
        self.mapper_reference_index: Optional[Dict[str, Set[Path]]] = None
        self.dynamic_mapper_reference_index: Optional[Dict[str, Set[Path]]] = None

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
                else:
                    cls.unresolved_implements.append(clean_type(item))
            for item in cls.extends:
                if not self.resolve_type(item, cls):
                    cls.unresolved_extends.append(clean_type(item))
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
        base_mapper_fqn = self.mybatis_plus_base_mapper_fqn(cls)
        if base_mapper_fqn:
            type_scope.setdefault("baseMapper", base_mapper_fqn)
        type_scope.update(method.params)
        type_scope.update(self.parse_local_vars(body))
        call_events: List[Tuple[int, MethodKey]] = []

        def add_call(offset: int, keys: Iterable[MethodKey]) -> None:
            for key in sort_method_keys(keys):
                if key != method.key:
                    call_events.append((offset, key))

        dotted_spans: List[Tuple[int, int]] = []
        if base_mapper_fqn:
            for m in re.finditer(
                r"\b(?:this\s*\.\s*)?getBaseMapper\s*\(\s*\)\s*\.\s*([A-Za-z_]\w*)\s*\(",
                body,
            ):
                call_name = m.group(1)
                if call_name in CALL_KEYWORDS:
                    continue
                dotted_spans.append((m.start(1), m.end(1)))
                arg_types = self.infer_call_arg_types(original_body, m.end() - 1, type_scope)
                add_call(m.start(), self.resolve_methods_with_impls(base_mapper_fqn, call_name, arg_types))

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

    def mybatis_plus_base_mapper_fqn(self, cls: ClassInfo) -> Optional[str]:
        for parent in cls.extends:
            match = re.match(r"\s*([A-Za-z_][\w.]*)\s*<(.+)>\s*$", parent, flags=re.S)
            if not match:
                continue
            parent_name = match.group(1)
            if parent_name.split(".")[-1] != "ServiceImpl":
                continue
            args = split_top_level(match.group(2), ",")
            if not args:
                continue
            mapper_type = clean_type(args[0])
            mapper_fqn = self.resolve_type(mapper_type, cls)
            if mapper_fqn:
                return mapper_fqn
        return None

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
            namespace_match = re.search(r'\bnamespace\s*=\s*["\']([^"\']+)["\']', text)
            if not namespace_match:
                continue
            namespace = namespace_match.group(1)
            for statement in iter_mybatis_statements(text, namespace, path):
                self.xml_statements[(namespace, statement.statement_id)].append(statement)

    def build_unused_mapper_plan(self) -> Tuple[Set[MethodKey], Dict[str, List[str]]]:
        unused: Set[MethodKey] = set()
        retained: Dict[str, List[str]] = {}
        for method in self.iter_mapper_methods():
            reasons = self.mapper_retain_reasons(method)
            if reasons:
                retained[method.key.text()] = reasons
            else:
                unused.add(method.key)
        return unused, retained

    def iter_mapper_methods(self) -> Iterable[MethodInfo]:
        for cls in self.classes.values():
            if not self.is_mapper_class(cls):
                continue
            for method in cls.methods:
                if method.is_constructor:
                    continue
                if self.deletion_guard_reason(method):
                    continue
                yield method

    def is_mapper_class(self, cls: ClassInfo) -> bool:
        if cls.kind != "interface":
            return False
        if cls.fqn in self.xml_namespaces:
            return True
        lowered_path = str(cls.path).replace("\\", "/").lower()
        if "/mapper/" in lowered_path and cls.name.endswith("Mapper"):
            return True
        annotation_names = {
            name for name, _ in extract_annotation_invocations(cls.annotations_text)
        }
        if "Mapper" in annotation_names:
            return "mapstruct" not in cls.source.lower()
        return False

    def mapper_retain_reasons(self, method: MethodInfo) -> List[str]:
        reasons: List[str] = []
        if self.incoming.get(method.key):
            reasons.extend(self.format_reasons(self.incoming[method.key], []))
        anchors = self.external_anchors.get(method.key, [])
        if anchors:
            reasons.extend(f"anchor: {anchor}" for anchor in anchors)

        references = self.find_external_mapper_references(method)
        reasons.extend(references)

        if self.has_dynamic_mapper_risk(method):
            reasons.append("risk: possible dynamic or reflective mapper invocation")

        return sorted(dict.fromkeys(reasons))

    def find_external_mapper_references(self, method: MethodInfo) -> List[str]:
        refs: List[str] = []
        cls = self.classes.get(method.class_fqn)
        simple_class = cls.name if cls else method.class_fqn.split(".")[-1]
        keys = [
            f"{method.class_fqn}.{method.name}",
            f"{simple_class}.{method.name}",
        ]
        reference_index, _dynamic_index = self.get_mapper_reference_indexes()
        paths: Set[Path] = set()
        for key in keys:
            paths.update(reference_index.get(key, set()))
        for path in self.external_reference_paths(method, paths):
            refs.append(f"reference: {relpath(path, self.root)}")
        return refs[:8]

    def has_dynamic_mapper_risk(self, method: MethodInfo) -> bool:
        cls = self.classes.get(method.class_fqn)
        simple_class = cls.name if cls else method.class_fqn.split(".")[-1]
        risk_tokens = (
            "selectOne(",
            "selectList(",
            "selectMap(",
            "selectCursor(",
            "insert(",
            "update(",
            "delete(",
            "SqlSession",
            "getMapper(",
            "Class.forName(",
            "ReflectionUtils",
            "Method.invoke(",
            "invoke(",
        )
        keys = (
            f"{method.class_fqn}.{method.name}",
            f"{simple_class}.{method.name}",
            method.name,
        )
        _reference_index, dynamic_index = self.get_mapper_reference_indexes()
        paths: Set[Path] = set()
        for key in keys:
            paths.update(dynamic_index.get(key, set()))
        return bool(self.external_reference_paths(method, paths))

    def external_reference_paths(
        self, method: MethodInfo, paths: Iterable[Path]
    ) -> List[Path]:
        own_xml_paths = set(self.xml_namespaces.get(method.class_fqn, []))
        return sorted(
            {
                path
                for path in paths
                if path != method.path and path not in own_xml_paths
            },
            key=lambda path: str(relpath(path, self.root)),
        )

    def get_mapper_reference_indexes(self) -> Tuple[Dict[str, Set[Path]], Dict[str, Set[Path]]]:
        if self.mapper_reference_index is not None and self.dynamic_mapper_reference_index is not None:
            return self.mapper_reference_index, self.dynamic_mapper_reference_index

        reference_index: Dict[str, Set[Path]] = defaultdict(set)
        dynamic_index: Dict[str, Set[Path]] = defaultdict(set)
        risk_tokens = (
            "selectOne(",
            "selectList(",
            "selectMap(",
            "selectCursor(",
            "insert(",
            "update(",
            "delete(",
            "SqlSession",
            "getMapper(",
            "Class.forName(",
            "ReflectionUtils",
            "Method.invoke(",
            "invoke(",
        )
        for path, text in self.get_reference_texts():
            dynamic = any(token in text for token in risk_tokens)
            for key in self.extract_mapper_reference_keys(text):
                reference_index[key].add(path)
                if dynamic:
                    dynamic_index[key].add(path)
            if dynamic:
                for value in iter_quoted_values(text):
                    if re.fullmatch(r"[A-Za-z_]\w*", value):
                        dynamic_index[value].add(path)

        self.mapper_reference_index = reference_index
        self.dynamic_mapper_reference_index = dynamic_index
        return reference_index, dynamic_index

    @staticmethod
    def extract_mapper_reference_keys(text: str) -> Set[str]:
        keys: Set[str] = set()
        for match in re.finditer(
            r"\b([A-Za-z_][\w.]*Mapper)\s*\.\s*([A-Za-z_]\w*)\b",
            text,
        ):
            owner = match.group(1)
            name = match.group(2)
            keys.add(f"{owner}.{name}")
            keys.add(f"{owner.split('.')[-1]}.{name}")
        for value in iter_quoted_values(text):
            if re.fullmatch(r"[A-Za-z_][\w.]*Mapper\.[A-Za-z_]\w*", value):
                owner, name = value.rsplit(".", 1)
                keys.add(value)
                keys.add(f"{owner.split('.')[-1]}.{name}")
        return keys

    def get_reference_texts(self) -> List[Tuple[Path, str]]:
        if self.reference_texts is None:
            items: List[Tuple[Path, str]] = []
            for suffix in REFERENCE_SUFFIXES:
                for path in self.iter_files(suffix):
                    try:
                        items.append((path, read_text(path)))
                    except UnicodeDecodeError:
                        continue
            self.reference_texts = items
        return self.reference_texts

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

        unused_mappers: Set[MethodKey] = set()
        mapper_retain_reasons: Dict[str, List[str]] = {}
        unused_mapper_files: Set[str] = set()
        if self.scan_unused_mappers:
            unused_mappers, mapper_retain_reasons = self.build_unused_mapper_plan()
            unused_mapper_files = self.build_unused_mapper_file_plan(unused_mappers)

        return CleanupPlan(
            roots=roots,
            closure=closure,
            removable=candidates,
            retained=retained,
            root_warnings=root_warnings,
            retain_reasons=retain_reasons,
            unresolved_paths=unresolved,
            unresolved_suggestions=unresolved_suggestions,
            unused_mappers=unused_mappers,
            mapper_retain_reasons=mapper_retain_reasons,
            unused_mapper_files=unused_mapper_files,
        )

    def build_impl_method_plan(self, requested_targets: Sequence[str]) -> CleanupPlan:
        scan_all = not any(str(target).strip() for target in requested_targets)
        if scan_all:
            impl_roots = self.locate_unreferenced_impl_methods()
            unresolved: List[str] = []
        else:
            impl_roots, unresolved = self.locate_impl_methods(requested_targets)
        root_keys = {match.method.key for match in impl_roots}
        closure = self.reachable_closure(root_keys)
        candidates = set(closure)
        changed = True
        while changed:
            changed = False
            for key in list(candidates):
                outside_callers = self.incoming.get(key, set()) - candidates
                anchored = bool(self.external_anchors.get(key))
                if outside_callers or anchored:
                    candidates.remove(key)
                    changed = True

        retained = closure - candidates
        root_warnings: Dict[str, List[str]] = {}
        for match in impl_roots:
            key = match.method.key
            external = self.incoming.get(key, set()) - candidates
            anchors = self.external_anchors.get(key, [])
            if key not in candidates or external or anchors:
                root_warnings[key.text()] = self.format_reasons(external, anchors)

        retain_reasons: Dict[str, List[str]] = {}
        for key in retained:
            external = self.incoming.get(key, set()) - candidates
            anchors = self.external_anchors.get(key, [])
            retain_reasons[key.text()] = self.format_reasons(external, anchors)

        return CleanupPlan(
            roots=[],
            closure=closure,
            removable=candidates,
            retained=retained,
            root_warnings=root_warnings,
            retain_reasons=retain_reasons,
            unresolved_paths=[],
            unresolved_suggestions={},
            impl_roots=impl_roots,
            unresolved_impl_methods=unresolved,
            impl_scan_all=scan_all,
            unused_mappers=set(),
            mapper_retain_reasons={},
            unused_mapper_files=set(),
        )

    def locate_unreferenced_impl_methods(self) -> List[ImplMethodMatch]:
        matches: List[ImplMethodMatch] = []
        for cls in sorted(
            self.classes.values(),
            key=lambda item: (str(relpath(item.path, self.root)), item.fqn),
        ):
            if not self.is_service_impl_class(cls):
                continue
            for method in sorted(cls.methods, key=lambda item: (item.line, item.name, item.full_start)):
                if not self.is_impl_scan_candidate(method):
                    continue
                if self.has_effective_impl_incoming_call(method.key):
                    continue
                matches.append(ImplMethodMatch(method.label, method))
        return matches

    def is_impl_scan_candidate(self, method: MethodInfo) -> bool:
        if method.is_constructor:
            return False
        if method.body_start is None or method.body_end is None:
            return False
        if method.route_paths:
            return False
        if self.has_external_override_risk(method):
            return False
        if self.deletion_guard_reason(method):
            return False
        if method.annotation_names & ENTRYPOINT_ANNOTATIONS:
            return False
        if method.name in {"equals", "hashCode", "toString"}:
            return False
        return True

    def has_external_override_risk(self, method: MethodInfo) -> bool:
        if "Override" not in method.annotation_names:
            return False
        cls = self.classes.get(method.class_fqn)
        if not cls:
            return True
        unresolved_types = cls.unresolved_extends + cls.unresolved_implements
        if not unresolved_types:
            return False
        return any(self.looks_external_type(type_name, cls) for type_name in unresolved_types)

    @staticmethod
    def looks_external_type(type_name: str, cls: ClassInfo) -> bool:
        simple = clean_type(type_name).split(".")[-1]
        if not simple:
            return False
        if simple in {"Serializable", "Cloneable", "AutoCloseable", "Closeable", "Comparable", "Runnable"}:
            return True
        imported = cls.imports.get(simple)
        if imported:
            return not imported.startswith(cls.package.rsplit(".", 1)[0] + ".")
        if "." in type_name:
            return not type_name.startswith(cls.package.rsplit(".", 1)[0] + ".")
        return False

    def has_effective_impl_incoming_call(self, key: MethodKey) -> bool:
        if self.has_real_incoming_call(key):
            return True
        for caller in self.incoming.get(key, set()):
            if not self.is_interface_contract_pair(caller, key):
                continue
            if self.has_real_incoming_call(caller):
                return True
            if self.external_anchors.get(caller):
                return True
        return False

    def has_real_incoming_call(self, key: MethodKey) -> bool:
        return any(
            not self.is_interface_contract_pair(caller, key)
            for caller in self.incoming.get(key, set())
        )

    def is_interface_contract_pair(self, left: MethodKey, right: MethodKey) -> bool:
        left_method = self.methods.get(left)
        right_method = self.methods.get(right)
        if not left_method or not right_method:
            return False
        left_cls = self.classes.get(left.class_fqn)
        right_cls = self.classes.get(right.class_fqn)
        if not left_cls or not right_cls:
            return False
        if left_method.name != right_method.name:
            return False
        if len(left_method.params) != len(right_method.params):
            return False
        if left_cls.kind == "interface" and right_cls.kind == "class":
            return right.class_fqn in self.interface_impls.get(left.class_fqn, set())
        if left_cls.kind == "class" and right_cls.kind == "interface":
            return left.class_fqn in self.interface_impls.get(right.class_fqn, set())
        return False

    def locate_impl_methods(self, requested_targets: Sequence[str]) -> Tuple[List[ImplMethodMatch], List[str]]:
        matches: List[ImplMethodMatch] = []
        unresolved: List[str] = []
        seen: Set[MethodKey] = set()
        for raw_target in requested_targets:
            target = raw_target.strip()
            if not target:
                continue
            parsed = self.parse_impl_method_target(target)
            if not parsed:
                unresolved.append(target)
                continue
            class_name, method_name = parsed
            class_fqn = self.resolve_class_name(class_name)
            if not class_fqn:
                unresolved.append(target)
                continue
            cls = self.classes.get(class_fqn)
            if not cls or not self.is_service_impl_class(cls):
                unresolved.append(target)
                continue
            methods = [
                method
                for method in self.methods_by_class.get(class_fqn, {}).get(method_name, [])
                if not method.is_constructor and method.body_start is not None
            ]
            if not methods:
                unresolved.append(target)
                continue
            for method in methods:
                if method.key not in seen:
                    matches.append(ImplMethodMatch(target, method))
                    seen.add(method.key)
        return matches, unresolved

    @staticmethod
    def parse_impl_method_target(target: str) -> Optional[Tuple[str, str]]:
        normalized = target.strip()
        if "#" in normalized:
            owner, method = normalized.rsplit("#", 1)
        elif "." in normalized:
            owner, method = normalized.rsplit(".", 1)
        else:
            return None
        owner = owner.strip()
        method = method.strip()
        method = re.sub(r"\s*\(.*\)\s*$", "", method)
        if not owner or not re.fullmatch(r"[A-Za-z_]\w*", method):
            return None
        return owner, method

    def resolve_class_name(self, class_name: str) -> Optional[str]:
        if class_name in self.classes:
            return class_name
        simple = class_name.split(".")[-1]
        fqns = self.simple_to_fqns.get(simple, set())
        if "." in class_name:
            candidates = [fqn for fqn in fqns if fqn.endswith("." + simple) and fqn == class_name]
            if len(candidates) == 1:
                return candidates[0]
        if len(fqns) == 1:
            return next(iter(fqns))
        return None

    def is_service_impl_class(self, cls: ClassInfo) -> bool:
        annotation_names = {
            name for name, _ in extract_annotation_invocations(cls.annotations_text)
        }
        return cls.kind == "class" and cls.name.lower().endswith("impl") and "Service" in annotation_names

    def build_unused_mapper_file_plan(self, unused_mappers: Set[MethodKey]) -> Set[str]:
        files: Set[str] = set()
        unused_by_class: Dict[str, Set[MethodKey]] = defaultdict(set)
        for key in unused_mappers:
            unused_by_class[key.class_fqn].add(key)

        for cls in self.classes.values():
            if not self.is_mapper_class(cls):
                continue
            methods = [method for method in cls.methods if not self.deletion_guard_reason(method)]
            if not methods:
                continue
            if any(method.key not in unused_by_class.get(cls.fqn, set()) for method in methods):
                continue
            if self.class_has_external_reference(cls):
                continue
            files.add(str(relpath(cls.path, self.root)))
            for xml_path in self.xml_namespaces.get(cls.fqn, []):
                files.add(str(relpath(xml_path, self.root)))
        return files

    def class_has_external_reference(self, cls: ClassInfo) -> bool:
        simple_patterns = [
            cls.fqn,
            cls.name,
            f'"{cls.fqn}"',
            f"'{cls.fqn}'",
            f'"{cls.name}"',
            f"'{cls.name}'",
        ]
        for path, text in self.get_reference_texts():
            if path == cls.path or path in self.xml_namespaces.get(cls.fqn, []):
                continue
            if any(pattern in text for pattern in simple_patterns):
                return True
        return False

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
        file_delete_paths = {
            (self.root / path).resolve()
            for path in plan.unused_mapper_files
        }
        for key in set(plan.removable) | set(plan.unused_mappers):
            method = self.methods.get(key)
            if not method:
                continue
            if self.deletion_guard_reason(method):
                continue
            if method.path.resolve() in file_delete_paths:
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
            touched_xml = self.delete_xml_removals(plan, file_delete_paths)

        deleted_files: List[str] = []
        for path in sorted(file_delete_paths, key=lambda p: str(relpath(p, self.root))):
            if not path.exists() or not self.is_safe_delete_file(path):
                continue
            path.unlink()
            deleted_files.append(str(relpath(path, self.root)))

        return {
            "java_files": sorted(touched_java),
            "xml_files": sorted(touched_xml),
            "deleted_files": deleted_files,
        }

    def is_safe_delete_file(self, path: Path) -> bool:
        try:
            resolved = path.resolve()
            resolved.relative_to(self.root)
        except ValueError:
            return False
        return resolved.suffix in (JAVA_SUFFIX, XML_SUFFIX)

    def delete_xml_removals(self, plan: CleanupPlan, file_delete_paths: Set[Path]) -> List[str]:
        removals_by_xml: Dict[Path, Set[str]] = defaultdict(set)
        for key in set(plan.removable) | set(plan.unused_mappers):
            method = self.methods.get(key)
            if not method:
                continue
            if self.deletion_guard_reason(method):
                continue
            for xml_path in self.xml_namespaces.get(method.class_fqn, []):
                if xml_path.resolve() in file_delete_paths:
                    continue
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
        retained_mappers = []
        for text, reasons in sorted(plan.mapper_retain_reasons.items()):
            key = self.method_key_from_text(text)
            if key in self.methods:
                retained_mappers.append(
                    {
                        **self.mapper_report(key),
                        "reasons": reasons,
                    }
                )
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
            "implMethods": [
                {
                    "requestedTarget": match.requested_target,
                    "method": match.method.label,
                    "file": str(relpath(match.method.path, self.root)),
                    "line": match.method.line,
                }
                for match in plan.impl_roots
            ],
            "implScanAll": plan.impl_scan_all,
            "implTrees": self.impl_tree_reports(plan),
            "unresolvedPaths": plan.unresolved_paths,
            "unresolvedImplMethods": plan.unresolved_impl_methods,
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
                "implMethods": len(plan.impl_roots),
                "implScanAll": plan.impl_scan_all,
                "unresolvedImplMethods": len(plan.unresolved_impl_methods),
                "unusedMapperMethods": len(plan.unused_mappers),
                "retainedMapperMethods": len(plan.mapper_retain_reasons),
                "unusedMapperFiles": len(plan.unused_mapper_files),
            },
            "removable": [self.method_report(key) for key in sorted(plan.removable, key=lambda k: k.text())],
            "retained": [
                {
                    **self.method_report(key),
                    "reasons": plan.retain_reasons.get(key.text(), []),
                }
                for key in sorted(plan.retained, key=lambda k: k.text())
            ],
            "unusedMappers": [
                self.mapper_report(key)
                for key in sorted(plan.unused_mappers, key=lambda k: k.text())
            ],
            "retainedMappers": retained_mappers,
            "unusedMapperFiles": sorted(plan.unused_mapper_files),
            "rootWarnings": plan.root_warnings,
            "deletedFiles": deleted or {},
        }

    def method_key_from_text(self, text: str) -> MethodKey:
        class_and_name, start_text = text.rsplit("@", 1)
        class_fqn, name = class_and_name.rsplit("#", 1)
        return MethodKey(class_fqn, name, int(start_text))

    def mapper_report(self, key: MethodKey) -> Dict[str, object]:
        data = self.method_report(key)
        method = self.methods[key]
        statements = self.xml_statements.get((method.class_fqn, method.name), [])
        data["xmlStatements"] = [
            {
                "tag": statement.tag,
                "id": statement.statement_id,
                "file": str(relpath(statement.path, self.root)),
                "line": statement.line,
            }
            for statement in statements
        ]
        data["mapperClass"] = method.class_fqn
        return data

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

    def impl_tree_reports(self, plan: CleanupPlan) -> List[Dict[str, object]]:
        root_keys = {match.method.key for match in plan.impl_roots}
        return [
            {
                "requestedTarget": match.requested_target,
                "root": self.method_report(match.method.key, include_code=True),
                "tree": self.call_tree_node(
                    match.method.key,
                    plan,
                    root_keys=root_keys,
                    stack=[],
                    depth=0,
                ),
            }
            for match in plan.impl_roots
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


def iter_mybatis_statements(
    text: str, namespace: str, path: Path
) -> Iterable[MyBatisStatementInfo]:
    for tag in STATEMENT_TAGS:
        full_pattern = re.compile(
            rf"<{tag}\b(?=[^>]*\bid\s*=\s*['\"]([^'\"]+)['\"])[^>]*>"
            rf".*?</{tag}>",
            flags=re.S,
        )
        for match in full_pattern.finditer(text):
            statement_id = match.group(1)
            yield MyBatisStatementInfo(
                namespace=namespace,
                statement_id=statement_id,
                tag=tag,
                path=path,
                line=line_of(text, match.start()),
                start=match.start(),
                end=match.end(),
            )
        self_closing = re.compile(
            rf"<{tag}\b(?=[^>]*\bid\s*=\s*['\"]([^'\"]+)['\"])[^>]*/>",
            flags=re.S,
        )
        for match in self_closing.finditer(text):
            statement_id = match.group(1)
            yield MyBatisStatementInfo(
                namespace=namespace,
                statement_id=statement_id,
                tag=tag,
                path=path,
                line=line_of(text, match.start()),
                start=match.start(),
                end=match.end(),
            )


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
        norm = str(path).strip() if getattr(args, "impl_only", False) else normalize_path(path)
        if norm not in seen:
            deduped.append(norm)
            seen.add(norm)
    return deduped


class DeadApiCleanupService:
    """Reusable analysis/deletion service used by both CLI and Web UI."""

    def __init__(self, options: CleanupRunOptions) -> None:
        self.options = options

    def run(self, paths: Sequence[str]) -> CleanupRunResult:
        clean_paths = (
            self.normalize_impl_targets(paths)
            if self.options.impl_only
            else self.normalize_paths(paths)
        )
        root = self.options.root.resolve()
        if not root.exists():
            raise FileNotFoundError(f"root does not exist: {root}")
        if not clean_paths and not self.options.mapper_only and not self.options.impl_only:
            raise ValueError("provide at least one API path or implementation method")

        analyzer = JavaProjectAnalyzer(
            root=root,
            include_tests=self.options.include_tests,
            exact_route_match=self.options.exact_route_match,
            max_depth=self.options.max_depth,
            delete_xml=self.options.delete_xml,
            scan_unused_mappers=self.options.scan_unused_mappers,
            verbose=self.options.verbose,
        )
        analyzer.index()
        if self.options.impl_only:
            plan = analyzer.build_impl_method_plan(clean_paths)
        else:
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

    @staticmethod
    def normalize_impl_targets(paths: Sequence[str]) -> List[str]:
        deduped: List[str] = []
        seen: Set[str] = set()
        for path in paths:
            target = str(path).strip()
            if target and target not in seen:
                deduped.append(target)
                seen.add(target)
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
    if plan.impl_roots or plan.unresolved_impl_methods:
        plan_label = "implementation-method dead-code cleanup plan"
    elif plan.roots or plan.unresolved_paths:
        plan_label = "API dead-code cleanup plan"
    else:
        plan_label = "mapper dead-code cleanup plan"
    print(f"[{mode}] {plan_label}")
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
    if plan.impl_roots:
        heading = (
            "Unreferenced implementation methods:"
            if plan.impl_scan_all
            else "Matched implementation methods:"
        )
        print(heading)
        for match in plan.impl_roots:
            method = match.method
            print(
                f"  - {match.requested_target} => "
                f"{method.label} ({relpath(method.path, analyzer.root)}:{method.line})"
            )
    if plan.unresolved_impl_methods:
        print("Unresolved implementation methods:")
        for target in plan.unresolved_impl_methods:
            print(f"  - {target}")
    print()
    print(
        "Summary: "
        f"reachable={len(plan.closure)}, "
        f"removable={len(plan.removable)}, "
        f"retainedBoundary={len(plan.retained)}, "
        f"implRoots={len(plan.impl_roots)}, "
        f"unusedMappers={len(plan.unused_mappers)}, "
        f"retainedMappers={len(plan.mapper_retain_reasons)}"
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
    if plan.unused_mappers:
        print()
        print("Unused mapper methods:")
        for idx, key in enumerate(sorted(plan.unused_mappers, key=lambda k: k.text())):
            if idx >= limit:
                print(f"  ... {len(plan.unused_mappers) - limit} more")
                break
            method = analyzer.methods[key]
            statements = analyzer.xml_statements.get((method.class_fqn, method.name), [])
            xml_info = ""
            if statements:
                first = statements[0]
                xml_info = f" xml={relpath(first.path, analyzer.root)}:{first.line}"
            print(f"  - {method.label} ({relpath(method.path, analyzer.root)}:{method.line}){xml_info}")
    if plan.unused_mapper_files:
        print()
        print("Unused mapper file candidates:")
        for idx, path in enumerate(sorted(plan.unused_mapper_files)):
            if idx >= limit:
                print(f"  ... {len(plan.unused_mapper_files) - limit} more")
                break
            print(f"  - {path}")
    if plan.mapper_retain_reasons:
        print()
        print("Retained mapper methods:")
        for idx, (key_text, reasons) in enumerate(sorted(plan.mapper_retain_reasons.items())):
            if idx >= limit:
                print(f"  ... {len(plan.mapper_retain_reasons) - limit} more")
                break
            key = analyzer.method_key_from_text(key_text)
            method = analyzer.methods.get(key)
            if not method:
                continue
            first_reason = f" [{reasons[0]}]" if reasons else ""
            print(
                f"  - {method.label} ({relpath(method.path, analyzer.root)}:{method.line})"
                f"{first_reason}"
            )
    if deleted is None:
        print()
        print("No files changed. Re-run with the delete command to delete removable methods and unused mapper methods.")
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
        "--mapper-only",
        action="store_true",
        help="Analyze/delete unused mapper methods without requiring API paths",
    )
    parser.add_argument(
        "--impl-only",
        action="store_true",
        help="Analyze/delete unused methods rooted at @Service implementation methods",
    )
    parser.add_argument(
        "--no-unused-mapper-scan",
        action="store_true",
        help="Disable unused mapper analysis",
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
    if not paths and not args.mapper_only and not args.impl_only:
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
        scan_unused_mappers=not args.no_unused_mapper_scan,
        mapper_only=args.mapper_only,
        impl_only=args.impl_only,
        delete=args.action == "delete",
        report=args.report,
        no_report=args.no_report,
        verbose=args.verbose,
    )
    result = DeadApiCleanupService(options).run(paths)

    print_console_summary(result.analyzer, result.plan, result.deleted, args.limit)
    if result.plan.unresolved_paths or result.plan.unresolved_impl_methods:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
