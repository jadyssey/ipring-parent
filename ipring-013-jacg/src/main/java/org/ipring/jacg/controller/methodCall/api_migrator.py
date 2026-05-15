#!/usr/bin/env python3
"""
Spring API migration helper for Java/Maven projects.

The script locates Spring MVC endpoints in a source project, recursively
collects the project-local code reached by the request flow, and writes
method-sliced Java files to a target project. It reuses the parser and call
graph from dead_api_cleaner.py.

Examples:

  # Preview only.
  python -B scripts/api_migrator.py /app/transport/serviceInfo ^
    --source-root D:/git/usCode/dbu-mod-delivery ^
    --target-root D:/git/target-project

  # Migrate files into the target project's src/main/java and src/main/resources.
  python -B scripts/api_migrator.py migrate /app/transport/serviceInfo ^
    --source-root D:/git/usCode/dbu-mod-delivery ^
    --target-root D:/git/target-project

  # Migrate to a specific Java source root. Original package directories are kept.
  python -B scripts/api_migrator.py migrate --paths /foo/bar ^
    --source-root A --target-dir B/src/main/java

  # Load options from JSON.
  python -B scripts/api_migrator.py migrate --config api-migration.json

Config JSON keys use camelCase names matching the CLI options, for example:

{
  "paths": ["/foo/bar"],
  "sourceRoot": "D:/git/project-a",
  "targetRoot": "D:/git/project-b",
  "overwrite": false
}

Safety model:

  - The default action is plan and does not write files.
  - The migrate action writes copied files but refuses to overwrite different
    existing files unless --overwrite is passed.
  - Java files are sliced at method/member level. Only methods in the endpoint
    call graph are retained; unused fields and imports are removed.
  - Target Java files are always written under the original package directory.
    Package rewrites are rejected to preserve compile-time dependencies.
  - MyBatis XML files are pruned to the selected Mapper statement ids.
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


SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from dead_api_cleaner import (  # noqa: E402
    ClassInfo,
    JavaProjectAnalyzer,
    MethodKey,
    RouteMatch,
    clean_type,
    find_matching,
    iter_mybatis_statements,
    leading_annotation_block,
    looks_like_method_signature,
    mask_java,
    normalize_path,
    read_text,
    relpath,
    remove_mybatis_statement,
    split_top_level,
    STATEMENT_TAGS,
    write_text,
)


TARGET_REF_SUFFIXES = (".java", ".xml", ".yml", ".yaml", ".properties")


@dataclass
class MigrationOptions:
    source_root: Path
    target_root: Optional[Path] = None
    target_java_dir: Optional[Path] = None
    target_resource_dir: Optional[Path] = None
    include_tests: bool = False
    exact_route_match: bool = False
    max_depth: int = 80
    include_resources: bool = True
    whole_file_closure: bool = False
    source_base_package: Optional[str] = None
    target_base_package: Optional[str] = None
    package_map: Dict[str, str] = field(default_factory=dict)
    overwrite: bool = False
    skip_conflicts: bool = False
    update_target_refs: bool = False
    report: str = "api_migration_report.json"
    no_report: bool = False
    verbose: bool = False


@dataclass
class PlannedFile:
    kind: str
    source: Path
    target: Path
    source_rel: str
    target_rel: str
    rewritten: bool = False
    status: str = "planned"
    selected_methods: List[str] = field(default_factory=list)


@dataclass
class MigrationPlan:
    routes: List[RouteMatch]
    unresolved_paths: List[str]
    unresolved_suggestions: Dict[str, List[RouteMatch]]
    method_closure: Set[MethodKey]
    class_fqns: Set[str]
    methods_by_class: Dict[str, Set[MethodKey]]
    resource_statements: Dict[str, Set[str]]
    resource_paths: Set[Path]
    package_map: Dict[str, str]
    class_fqn_map: Dict[str, str]
    files: List[PlannedFile]
    warnings: List[str] = field(default_factory=list)
    all_classes_by_path: Dict[Path, List[ClassInfo]] = field(default_factory=dict)
    xml_namespaces: Dict[str, List[Path]] = field(default_factory=dict)


@dataclass
class MigrationResult:
    written: List[str] = field(default_factory=list)
    skipped_same: List[str] = field(default_factory=list)
    skipped_conflicts: List[str] = field(default_factory=list)
    skipped_existing_models: List[str] = field(default_factory=list)
    conflicts: List[str] = field(default_factory=list)
    updated_refs: List[str] = field(default_factory=list)
    report_path: Optional[Path] = None


@dataclass
class JavaMemberBlock:
    kind: str
    name: str
    text: str
    field_names: Set[str] = field(default_factory=set)
    param_count: int = 0


@dataclass
class JavaMergePlan:
    text: str
    changed: bool = False
    conflicts: List[str] = field(default_factory=list)
    skipped_conflicts: List[str] = field(default_factory=list)
    skipped_existing_models: List[str] = field(default_factory=list)


@dataclass
class ResourceMergePlan:
    text: str
    changed: bool = False
    conflicts: List[str] = field(default_factory=list)
    skipped_conflicts: List[str] = field(default_factory=list)


MODEL_CLASS_SUFFIXES = (
    "DO",
    "PO",
    "DTO",
    "VO",
    "BO",
    "AO",
    "QO",
    "Entity",
    "Model",
    "Domain",
    "Request",
    "Response",
    "Req",
    "Resp",
    "Enum",
)
NON_MODEL_CLASS_SUFFIXES = (
    "Controller",
    "Service",
    "ServiceImpl",
    "Mapper",
    "Dao",
    "Repository",
    "Client",
    "Feign",
    "Util",
    "Utils",
    "Config",
    "Configuration",
    "Constants",
)
MODEL_PACKAGE_PARTS = {
    "domain",
    "dto",
    "vo",
    "bo",
    "ao",
    "po",
    "entity",
    "model",
    "request",
    "response",
}


def normalize_package(pkg: Optional[str]) -> Optional[str]:
    if pkg is None:
        return None
    pkg = pkg.strip().strip(".")
    return pkg or None


def parse_package_map(values: Sequence[str]) -> Dict[str, str]:
    mapping: Dict[str, str] = {}
    for raw in values:
        if not raw:
            continue
        if "=" not in raw:
            raise ValueError(f"invalid package map '{raw}', expected old.package=new.package")
        left, right = raw.split("=", 1)
        left_pkg = normalize_package(left)
        right_pkg = normalize_package(right)
        if not left_pkg or not right_pkg:
            raise ValueError(f"invalid package map '{raw}', package names cannot be empty")
        mapping[left_pkg] = right_pkg
    return mapping


def longest_common_package(packages: Iterable[str]) -> Optional[str]:
    parts_list = [pkg.split(".") for pkg in packages if pkg]
    if not parts_list:
        return None
    common: List[str] = []
    for columns in zip(*parts_list):
        first = columns[0]
        if all(item == first for item in columns):
            common.append(first)
        else:
            break
    return ".".join(common) if common else None


def rewrite_package_name(package_name: str, package_map: Dict[str, str]) -> str:
    for old, new in sorted(package_map.items(), key=lambda item: len(item[0]), reverse=True):
        if package_name == old:
            return new
        if package_name.startswith(old + "."):
            return new + package_name[len(old) :]
    return package_name


def build_class_fqn_map(classes: Iterable[ClassInfo], package_map: Dict[str, str]) -> Dict[str, str]:
    result: Dict[str, str] = {}
    for cls in classes:
        new_package = rewrite_package_name(cls.package, package_map)
        new_fqn = f"{new_package}.{cls.name}" if new_package else cls.name
        if new_fqn != cls.fqn:
            result[cls.fqn] = new_fqn
    return result


def replace_fqns(text: str, class_fqn_map: Dict[str, str]) -> str:
    for old, new in sorted(class_fqn_map.items(), key=lambda item: len(item[0]), reverse=True):
        text = re.sub(rf"(?<![\w.]){re.escape(old)}(?![\w.])", new, text)
    return text


def rewrite_import_line(line: str, package_map: Dict[str, str]) -> str:
    match = re.match(r"(\s*import\s+(?:static\s+)?)([\w.]+)(\s*;\s*)$", line)
    if not match:
        return line
    prefix, target, suffix = match.groups()
    if target.endswith(".*"):
        package_name = target[:-2]
        rewritten = rewrite_package_name(package_name, package_map)
        return f"{prefix}{rewritten}.*{suffix}"
    owner = ".".join(target.split(".")[:-1])
    member = target.split(".")[-1]
    rewritten_owner = rewrite_package_name(owner, package_map)
    return f"{prefix}{rewritten_owner}.{member}{suffix}"


def rewrite_java_text(
    text: str,
    package_map: Dict[str, str],
    class_fqn_map: Dict[str, str],
) -> str:
    if not package_map and not class_fqn_map:
        return text

    def package_repl(match: re.Match[str]) -> str:
        old_pkg = match.group(1)
        new_pkg = rewrite_package_name(old_pkg, package_map)
        return f"package {new_pkg};"

    text = re.sub(r"\bpackage\s+([\w.]+)\s*;", package_repl, text, count=1)
    lines = [rewrite_import_line(line, package_map) for line in text.splitlines(True)]
    text = "".join(lines)
    return replace_fqns(text, class_fqn_map)


def rewrite_resource_text(text: str, class_fqn_map: Dict[str, str]) -> str:
    if not class_fqn_map:
        return text
    return replace_fqns(text, class_fqn_map)


def method_id(method_name: str, param_count: int) -> str:
    return f"{method_name}/{param_count}"


def method_id_from_method(method: Any) -> str:
    return method_id(method.name, len(method.params))


def import_simple_name(target: str) -> str:
    if target.endswith(".*"):
        return "*"
    return target.split(".")[-1]


def prune_imports(text: str, imported_classes: Set[str]) -> str:
    """Remove imports for project-local classes that are not referenced anymore."""
    if not imported_classes:
        return text
    lines = text.splitlines(True)
    body_text = re.sub(r"^\s*import\s+(?:static\s+)?[\w.*]+\s*;\s*$", "", text, flags=re.M)
    kept: List[str] = []
    for line in lines:
        match = re.match(r"\s*import\s+(static\s+)?([\w.*]+)\s*;", line)
        if not match:
            kept.append(line)
            continue
        target = match.group(2)
        owner = target[:-2] if target.endswith(".*") else target
        if owner in imported_classes or target in imported_classes:
            simple = import_simple_name(target)
            if simple != "*" and re.search(rf"\b{re.escape(simple)}\b", body_text):
                kept.append(line)
            elif match.group(1) and re.search(rf"\b{re.escape(simple)}\b", body_text):
                kept.append(line)
            continue
        kept.append(line)
    return "".join(kept)


def strip_empty_import_gap(text: str) -> str:
    return re.sub(r"(\bpackage\s+[\w.]+\s*;\s*)\n{3,}", r"\1\n\n", text, count=1)


def should_keep_field_or_initializer(segment: str, selected_source: str, cls: ClassInfo) -> bool:
    masked_segment = segment.strip()
    if not masked_segment:
        return False
    if not selected_source.strip():
        return True
    if masked_segment.startswith("static") or masked_segment.startswith("{"):
        return True
    if " enum " in f" {masked_segment} ":
        return True
    const_match = re.search(r"\b([A-Z][A-Z0-9_]*)\b\s*=", masked_segment)
    if const_match and re.search(rf"\b{re.escape(const_match.group(1))}\b", selected_source):
        return True
    for field_name in cls.fields:
        if re.search(rf"\b{re.escape(field_name)}\b", masked_segment) and re.search(
            rf"\b{re.escape(field_name)}\b", selected_source
        ):
            return True
    if re.search(r"@\s*(Autowired|Resource|Value|Qualifier)\b", segment):
        return any(
            re.search(rf"\b{re.escape(field_name)}\b", selected_source)
            for field_name in cls.fields
            if re.search(rf"\b{re.escape(field_name)}\b", masked_segment)
        )
    return False


def selected_method_source(cls: ClassInfo, selected: Set[MethodKey]) -> str:
    chunks: List[str] = []
    for method in cls.methods:
        if method.key in selected:
            chunks.append(cls.source[method.full_start : method.full_end])
    return "\n".join(chunks)


def selected_member_dependency_source(cls: ClassInfo, selected: Set[MethodKey]) -> str:
    selected_source = selected_method_source(cls, selected)
    chunks = [cls.annotations_text, selected_source]
    masked = cls.masked
    i = cls.body_start + 1
    member_start = i
    depth = 0
    while i < cls.body_end:
        c = masked[i]
        if c == "{" and depth == 0:
            segment = masked[member_start:i]
            parsed = looks_like_method_signature(segment, cls.name)
            close = find_matching(masked, i, "{", "}")
            if close > i:
                raw = cls.source[member_start : close + 1]
                if (parsed and parsed[0] == cls.name) or (
                    not parsed and should_keep_field_or_initializer(raw, selected_source, cls)
                ):
                    chunks.append(raw)
                i = close + 1
                member_start = i
                continue
            depth += 1
        elif c == "{":
            depth += 1
        elif c == "}":
            depth = max(0, depth - 1)
        elif c == ";" and depth == 0:
            raw = cls.source[member_start : i + 1]
            parsed = looks_like_method_signature(masked[member_start : i + 1], cls.name)
            if (parsed and parsed[0] == cls.name) or (
                not parsed and should_keep_field_or_initializer(raw, selected_source, cls)
            ):
                chunks.append(raw)
            member_start = i + 1
        i += 1
    return "\n".join(chunks)


def parse_java_classes_from_text(text: str, path: Path) -> List[ClassInfo]:
    parser = JavaProjectAnalyzer(path.parent if path.parent else Path("."))
    masked = mask_java(text)
    package_match = re.search(r"\bpackage\s+([\w.]+)\s*;", masked)
    package = package_match.group(1) if package_match else ""
    imports, static_imports, static_wildcards = parser.parse_imports(masked)
    classes: List[ClassInfo] = []
    for match in re.finditer(
        r"((?:public|protected|private|abstract|final|static)\s+)*"
        r"\b(class|interface|enum)\s+([A-Za-z_]\w*)\b",
        masked,
        flags=re.S,
    ):
        if parser.brace_depth(masked, match.start()) != 0:
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
        extends, implements = parser.parse_type_relations(decl)
        cls = ClassInfo(
            path=path,
            package=package,
            name=name,
            fqn=fqn,
            kind=kind,
            source=text,
            masked=masked,
            class_start=match.start(),
            body_start=open_brace,
            body_end=close_brace,
            annotations_text=leading_annotation_block(text, match.start()),
            imports=imports,
            static_imports=static_imports,
            static_wildcards=static_wildcards,
            extends=extends,
            implements=implements,
        )
        parser.parse_members(cls)
        classes.append(cls)
    return classes


def primary_class(classes: Sequence[ClassInfo], fallback_name: Optional[str] = None) -> Optional[ClassInfo]:
    if not classes:
        return None
    if fallback_name:
        for cls in classes:
            if cls.name == fallback_name:
                return cls
    for cls in classes:
        if re.search(rf"\bpublic\s+(?:class|interface|enum)\s+{re.escape(cls.name)}\b", cls.masked):
            return cls
    return classes[0]


def is_model_or_enum(cls: ClassInfo) -> bool:
    if cls.kind == "enum":
        return True
    if cls.name.endswith(NON_MODEL_CLASS_SUFFIXES):
        return False
    if cls.name.endswith(MODEL_CLASS_SUFFIXES):
        return True
    package_parts = {part.lower() for part in cls.package.split(".") if part}
    if package_parts & MODEL_PACKAGE_PARTS:
        return True
    normalized_path = str(cls.path).replace("\\", "/").lower()
    return any(f"/{part}/" in normalized_path for part in MODEL_PACKAGE_PARTS)


def parse_params_from_signature(signature: str) -> List[str]:
    masked = mask_java(signature)
    open_paren = masked.find("(")
    if open_paren < 0:
        return []
    close_paren = find_matching(masked, open_paren, "(", ")")
    if close_paren < 0:
        return []
    params_text = signature[open_paren + 1 : close_paren].strip()
    if not params_text:
        return []
    return [part for part in split_top_level(params_text, ",") if part.strip()]


def field_names_from_segment(segment: str) -> Set[str]:
    masked = mask_java(segment)
    names: Set[str] = set()
    if re.search(r"\b(class|interface|enum)\b", masked):
        return names
    if "(" in masked and ")" in masked:
        return names
    for part in split_top_level(masked.rstrip(";"), ","):
        part = part.strip()
        if not part:
            continue
        before_equals = part.split("=", 1)[0].strip()
        match = re.search(r"\b([A-Za-z_]\w*)\s*(?:\[\s*\])?\s*$", before_equals)
        if match:
            names.add(match.group(1))
    return names


def extract_top_level_members(cls: ClassInfo) -> List[JavaMemberBlock]:
    members: List[JavaMemberBlock] = []
    source = cls.source
    masked = cls.masked
    i = cls.body_start + 1
    member_start = i
    depth = 0

    while i < cls.body_end:
        c = masked[i]
        if c == "{" and depth == 0:
            segment = masked[member_start:i]
            close = find_matching(masked, i, "{", "}")
            parsed = looks_like_method_signature(segment, cls.name)
            if close > i:
                text = source[member_start : close + 1]
                if parsed:
                    name = parsed[0]
                    kind = "constructor" if name == cls.name else "method"
                    members.append(
                        JavaMemberBlock(
                            kind=kind,
                            name=name,
                            text=text,
                            param_count=len(parse_params_from_signature(source[member_start:i])),
                        )
                    )
                else:
                    kind = "initializer" if segment.strip().startswith(("static", "{")) else "block"
                    members.append(JavaMemberBlock(kind=kind, name="", text=text))
                i = close + 1
                member_start = i
                continue
            depth += 1
        elif c == "{":
            depth += 1
        elif c == "}":
            depth = max(0, depth - 1)
        elif c == ";" and depth == 0:
            text = source[member_start : i + 1]
            parsed = looks_like_method_signature(masked[member_start : i + 1], cls.name)
            if parsed:
                name = parsed[0]
                kind = "constructor" if name == cls.name else "method"
                members.append(
                    JavaMemberBlock(
                        kind=kind,
                        name=name,
                        text=text,
                        param_count=len(parse_params_from_signature(text)),
                    )
                )
            else:
                field_names = field_names_from_segment(text)
                kind = "field" if field_names else "statement"
                members.append(JavaMemberBlock(kind=kind, name="", text=text, field_names=field_names))
            member_start = i + 1
        i += 1

    return members


def import_lines(text: str) -> List[str]:
    return [
        line.strip()
        for line in text.splitlines()
        if re.match(r"\s*import\s+(?:static\s+)?[\w.*]+\s*;\s*$", line)
    ]


def merge_imports(target_text: str, source_text: str) -> str:
    target_imports = import_lines(target_text)
    source_imports = import_lines(source_text)
    missing = [line for line in source_imports if line not in set(target_imports)]
    if not missing:
        return target_text

    lines = target_text.splitlines(True)
    import_indices = [idx for idx, line in enumerate(lines) if re.match(r"\s*import\s+(?:static\s+)?[\w.*]+\s*;", line)]
    insert_lines = [line + "\n" for line in missing]
    insert_idx: int
    if import_indices:
        insert_idx = import_indices[-1] + 1
        if insert_idx < len(lines) and lines[insert_idx].strip():
            insert_lines.append("\n")
    else:
        package_idx = next((idx for idx, line in enumerate(lines) if re.match(r"\s*package\s+[\w.]+\s*;", line)), None)
        insert_idx = package_idx + 1 if package_idx is not None else 0
        if insert_idx > 0 and (insert_idx >= len(lines) or lines[insert_idx].strip()):
            insert_lines.insert(0, "\n")
        if insert_idx < len(lines) and lines[insert_idx].strip():
            insert_lines.append("\n")
    lines[insert_idx:insert_idx] = insert_lines
    return "".join(lines)


def member_text_key(text: str) -> str:
    return re.sub(r"\s+", "", mask_java(text))


def append_members_to_class(target_text: str, target_cls: ClassInfo, members: Sequence[JavaMemberBlock]) -> str:
    if not members:
        return target_text
    insert_at = target_cls.body_end
    indent_match = re.search(r"\n([ \t]*)\}", target_text[max(0, insert_at - 120) : insert_at + 1])
    class_indent = indent_match.group(1) if indent_match else ""
    member_text = "\n".join(block.text.strip("\n") for block in members if block.text.strip())
    if not member_text:
        return target_text
    prefix = "" if target_text[:insert_at].endswith("\n\n") else "\n"
    suffix = "\n" if member_text.endswith("\n") else "\n"
    return target_text[:insert_at] + prefix + member_text + suffix + class_indent + target_text[insert_at:]


def selected_classes_for_source(item: PlannedFile, plan: MigrationPlan) -> List[ClassInfo]:
    classes = getattr(plan, "all_classes_by_path", {}).get(item.source.resolve(), [])
    return [cls for cls in classes if cls.fqn in plan.methods_by_class]


def should_copy_whole_java_file(item: PlannedFile, plan: MigrationPlan) -> bool:
    selected_classes = selected_classes_for_source(item, plan)
    return bool(selected_classes) and all(is_model_or_enum(cls) for cls in selected_classes)


def should_prune_java_imports(classes: Sequence[ClassInfo], selected_by_class: Dict[str, Set[MethodKey]]) -> bool:
    return any(
        cls.fqn in selected_by_class and not is_model_or_enum(cls)
        for cls in classes
    )


def slice_class_body(cls: ClassInfo, selected: Set[MethodKey]) -> Tuple[str, List[str]]:
    """Return a class body containing only selected methods and used members."""
    source = cls.source
    masked = cls.masked
    selected_ids = [method_id_from_method(method) for method in cls.methods if method.key in selected]
    if cls.kind == "interface":
        selected_names = {method.name for method in cls.methods if method.key in selected}
        selected = {
            method.key
            for method in cls.methods
            if method.key in selected or method.name in selected_names
        }
        selected_ids = [method_id_from_method(method) for method in cls.methods if method.key in selected]

    selected_source = selected_method_source(cls, selected)
    out: List[str] = [source[cls.class_start : cls.body_start + 1]]
    member_start = cls.body_start + 1
    i = member_start
    depth = 0

    while i < cls.body_end:
        c = masked[i]
        if c == "{" and depth == 0:
            segment = masked[member_start:i]
            parsed = looks_like_method_signature(segment, cls.name)
            close = find_matching(masked, i, "{", "}")
            if parsed and close > i:
                method = next(
                    (
                        item
                        for item in cls.methods
                        if item.full_start == member_start and item.full_end == close + 1
                    ),
                    None,
                )
                if method is not None and method.key in selected:
                    out.append(source[member_start : close + 1])
                elif method is None and parsed[0] == cls.name:
                    out.append(source[member_start : close + 1])
                elif method is None and should_keep_field_or_initializer(
                    source[member_start : close + 1], selected_source, cls
                ):
                    out.append(source[member_start : close + 1])
                i = close + 1
                member_start = i
                continue
            close = find_matching(masked, i, "{", "}")
            if close > i:
                if should_keep_field_or_initializer(source[member_start : close + 1], selected_source, cls):
                    out.append(source[member_start : close + 1])
                i = close + 1
                member_start = i
                continue
            depth += 1
        elif c == "{":
            depth += 1
        elif c == "}":
            depth = max(0, depth - 1)
        elif c == ";" and depth == 0:
            segment = source[member_start : i + 1]
            masked_segment = masked[member_start : i + 1]
            parsed = looks_like_method_signature(masked_segment, cls.name)
            if parsed:
                method = next(
                    (
                        item
                        for item in cls.methods
                        if item.full_start == member_start and item.full_end == i + 1
                    ),
                    None,
                )
                if method is not None and method.key in selected:
                    out.append(segment)
                elif method is None and parsed[0] == cls.name:
                    out.append(segment)
            elif should_keep_field_or_initializer(segment, selected_source, cls):
                out.append(segment)
            member_start = i + 1
        i += 1

    out.append(source[cls.body_end : cls.body_end + 1])
    return "".join(out), selected_ids


def slice_java_text(
    text: str,
    classes: Sequence[ClassInfo],
    selected_by_class: Dict[str, Set[MethodKey]],
    imported_classes: Set[str],
) -> Tuple[str, List[str]]:
    if not classes:
        return text, []
    new_text = text
    all_selected_ids: List[str] = []
    for cls in sorted(classes, key=lambda item: item.class_start, reverse=True):
        is_selected_class = cls.fqn in selected_by_class
        selected = selected_by_class.get(cls.fqn, set())
        if not is_selected_class:
            new_text = new_text[: cls.class_start] + new_text[cls.body_end + 1 :]
            continue
        if is_model_or_enum(cls):
            all_selected_ids.extend(
                f"{cls.fqn}#{method_id_from_method(method)}"
                for method in cls.methods
                if method.key in selected
            )
            continue
        if not selected:
            new_text = new_text[: cls.class_start] + new_text[cls.body_end + 1 :]
            continue
        sliced, selected_ids = slice_class_body(cls, selected)
        new_text = new_text[: cls.class_start] + sliced + new_text[cls.body_end + 1 :]
        all_selected_ids.extend(f"{cls.fqn}#{item}" for item in selected_ids)
    if should_prune_java_imports(classes, selected_by_class):
        new_text = prune_imports(new_text, imported_classes)
    new_text = strip_empty_import_gap(new_text)
    return new_text, sorted(all_selected_ids)


def prune_mybatis_xml(text: str, namespace: str, keep_ids: Set[str], path: Path) -> str:
    if not keep_ids:
        return text
    statement_ids = {
        statement.statement_id
        for statement in iter_mybatis_statements(text, namespace, path)
    }
    remove_ids = statement_ids - keep_ids
    new_text = text
    for statement_id in sorted(remove_ids):
        new_text = remove_mybatis_statement(new_text, statement_id)
    return new_text


def mybatis_statement_text(text: str, statement_id: str) -> Optional[str]:
    name = re.escape(statement_id)
    for tag in STATEMENT_TAGS:
        full = re.search(
            rf"[ \t]*<{tag}\b(?=[^>]*\bid\s*=\s*['\"]{name}['\"])[^>]*>"
            rf".*?</{tag}>[ \t]*(?:\r?\n)?",
            text,
            flags=re.S,
        )
        if full:
            return full.group(0).strip("\n")
        self_closing = re.search(
            rf"[ \t]*<{tag}\b(?=[^>]*\bid\s*=\s*['\"]{name}['\"])[^>]*/>[ \t]*(?:\r?\n)?",
            text,
            flags=re.S,
        )
        if self_closing:
            return self_closing.group(0).strip("\n")
    return None


def append_mybatis_statements(target_text: str, statement_texts: Sequence[str]) -> str:
    statements = [text.strip("\n") for text in statement_texts if text.strip()]
    if not statements:
        return target_text
    close = re.search(r"</mapper\s*>", target_text)
    if not close:
        return target_text
    insert_at = close.start()
    prefix = "" if target_text[:insert_at].endswith("\n\n") else "\n"
    body = "\n\n".join(statements)
    suffix = "\n" if body.endswith("\n") else "\n"
    return target_text[:insert_at] + prefix + body + suffix + target_text[insert_at:]


def safe_relative(path: Path, root: Path) -> Path:
    try:
        return path.resolve().relative_to(root.resolve())
    except ValueError:
        return path.name


def under_dir(path: Path, directory: Path) -> bool:
    try:
        path.resolve().relative_to(directory.resolve())
        return True
    except ValueError:
        return False


def find_marker_root(path: Path, marker_parts: Sequence[str]) -> Optional[Path]:
    parts = list(path.resolve().parts)
    marker = list(marker_parts)
    for idx in range(0, len(parts) - len(marker) + 1):
        if [part.lower() for part in parts[idx : idx + len(marker)]] == [
            part.lower() for part in marker
        ]:
            return Path(*parts[: idx + len(marker)])
    return None


def default_target_java_dir(options: MigrationOptions) -> Path:
    if options.target_java_dir:
        return options.target_java_dir.resolve()
    if options.target_root:
        candidate = options.target_root / "src" / "main" / "java"
        if candidate.exists() or (options.target_root / "pom.xml").exists():
            return candidate.resolve()
        return options.target_root.resolve()
    raise ValueError("target root or target Java directory is required")


def default_target_resource_dir(options: MigrationOptions) -> Path:
    if options.target_resource_dir:
        return options.target_resource_dir.resolve()
    if options.target_root:
        return (options.target_root / "src" / "main" / "resources").resolve()
    java_dir = default_target_java_dir(options)
    return java_dir.resolve()


class ApiMigrationPlanner:
    def __init__(self, options: MigrationOptions) -> None:
        self.options = options
        self.source_root = options.source_root.resolve()
        self.analyzer = JavaProjectAnalyzer(
            root=self.source_root,
            include_tests=options.include_tests,
            exact_route_match=options.exact_route_match,
            max_depth=options.max_depth,
            delete_xml=False,
            scan_unused_mappers=False,
            verbose=options.verbose,
        )
        self.classes_by_path: Dict[Path, List[ClassInfo]] = defaultdict(list)

    def build(self, paths: Sequence[str]) -> MigrationPlan:
        if not self.source_root.exists():
            raise FileNotFoundError(f"source root does not exist: {self.source_root}")
        clean_paths = self.normalize_paths(paths)
        if not clean_paths:
            raise ValueError("provide at least one API path")

        self.analyzer.index()
        for cls in self.analyzer.classes.values():
            self.classes_by_path[cls.path.resolve()].append(cls)

        routes, unresolved = self.analyzer.locate_routes(clean_paths)
        unresolved_suggestions = {
            path: self.analyzer.suggest_routes(path)
            for path in unresolved
        }
        root_keys = {match.method.key for match in routes}
        method_closure = self.analyzer.reachable_closure(root_keys)
        methods_by_class = self.collect_dependency_methods(method_closure)
        class_fqns = set(methods_by_class)
        package_map = self.resolve_package_map(class_fqns)
        class_fqn_map = build_class_fqn_map(
            (self.analyzer.classes[fqn] for fqn in sorted(class_fqns)),
            package_map,
        )
        resource_statements = (
            self.collect_resource_statements(methods_by_class)
            if self.options.include_resources
            else {}
        )
        resource_paths = {Path(key).resolve() for key in resource_statements}
        files = self.plan_files(class_fqns, methods_by_class, resource_paths, package_map, class_fqn_map)
        warnings = self.build_warnings(method_closure, class_fqns, unresolved)
        return MigrationPlan(
            routes=routes,
            unresolved_paths=unresolved,
            unresolved_suggestions=unresolved_suggestions,
            method_closure=method_closure,
            class_fqns=class_fqns,
            methods_by_class=methods_by_class,
            resource_statements=resource_statements,
            resource_paths=resource_paths,
            package_map=package_map,
            class_fqn_map=class_fqn_map,
            files=files,
            warnings=warnings,
            all_classes_by_path=dict(self.classes_by_path),
            xml_namespaces=dict(self.analyzer.xml_namespaces),
        )

    @staticmethod
    def normalize_paths(paths: Sequence[str]) -> List[str]:
        deduped: List[str] = []
        seen: Set[str] = set()
        for path in paths:
            norm = normalize_path(str(path))
            if norm not in seen:
                deduped.append(norm)
                seen.add(norm)
        return deduped

    def collect_dependency_methods(self, method_closure: Set[MethodKey]) -> Dict[str, Set[MethodKey]]:
        selected_methods: Set[MethodKey] = {
            key for key in method_closure if key in self.analyzer.methods
        }
        selected_classes: Set[str] = {
            key.class_fqn for key in selected_methods if key.class_fqn in self.analyzer.classes
        }
        queue: deque[str] = deque(sorted(selected_classes))

        def add_class_with_members(fqn: Optional[str]) -> None:
            if not fqn or fqn not in self.analyzer.classes:
                return
            cls = self.analyzer.classes[fqn]
            if fqn not in selected_classes:
                selected_classes.add(fqn)
                queue.append(fqn)
            for method in self.default_methods_for_dependency(cls):
                selected_methods.add(method.key)

        def add_method(key: MethodKey) -> None:
            if key not in self.analyzer.methods:
                return
            if key not in selected_methods:
                selected_methods.add(key)
            add_class_with_members(key.class_fqn)

        for fqn in list(selected_classes):
            cls = self.analyzer.classes.get(fqn)
            if cls:
                for method in self.default_methods_for_dependency(cls):
                    selected_methods.add(method.key)

        # Interface contracts are needed to keep injection and implementation
        # wiring compilable even when only one side appears in the call graph.
        for key in list(selected_methods):
            method = self.analyzer.methods[key]
            cls = self.analyzer.classes.get(method.class_fqn)
            if cls and cls.kind == "interface":
                for impl in self.analyzer.interface_impls.get(cls.fqn, set()):
                    for impl_method in self.analyzer.methods_by_class.get(impl, {}).get(method.name, []):
                        if len(impl_method.params) == len(method.params):
                            add_method(impl_method.key)
            elif cls:
                for interface_fqn, impls in self.analyzer.interface_impls.items():
                    if cls.fqn not in impls:
                        continue
                    for interface_method in self.analyzer.methods_by_class.get(interface_fqn, {}).get(method.name, []):
                        if len(interface_method.params) == len(method.params):
                            add_method(interface_method.key)

        while queue:
            fqn = queue.popleft()
            cls = self.analyzer.classes.get(fqn)
            if not cls:
                continue
            if cls.kind == "interface":
                for impl_fqn in self.analyzer.interface_impls.get(cls.fqn, set()):
                    add_class_with_members(impl_fqn)
            for dep in self.selected_class_dependencies(cls, selected_methods):
                add_class_with_members(dep)

        grouped: Dict[str, Set[MethodKey]] = defaultdict(set)
        for fqn in selected_classes:
            grouped.setdefault(fqn, set())
        for key in selected_methods:
            if key.class_fqn in selected_classes:
                grouped[key.class_fqn].add(key)
        return dict(grouped)

    def default_methods_for_dependency(self, cls: ClassInfo) -> List[Any]:
        # Domain/DTO/enums commonly rely on Lombok or bean accessors at runtime.
        # Keep accessors/constructors/object methods so method-sliced files still
        # compile and data binding remains usable.
        keep: List[Any] = []
        for method in cls.methods:
            if cls.kind == "enum":
                keep.append(method)
                continue
            if self.analyzer.is_java_bean_accessor(method):
                keep.append(method)
                continue
            if method.name in {"equals", "hashCode", "toString"}:
                keep.append(method)
                continue
            if method.name == cls.name:
                keep.append(method)
        return keep

    def selected_class_dependencies(self, cls: ClassInfo, selected_methods: Set[MethodKey]) -> Set[str]:
        deps: Set[str] = set()
        selected = {key for key in selected_methods if key.class_fqn == cls.fqn}
        source_chunks = [cls.annotations_text]
        for method in cls.methods:
            if method.key in selected:
                source_chunks.append(method.signature)
                source_chunks.append(method.annotations_text)
                if method.body_start is not None and method.body_end is not None:
                    source_chunks.append(cls.source[method.body_start : method.body_end + 1])
                for type_text in method.params.values():
                    deps.add(self.analyzer.resolve_type(type_text, cls) or "")
        selected_source = "\n".join(source_chunks)
        selected_source = selected_source + "\n" + selected_member_dependency_source(cls, selected)

        for type_text in list(cls.extends) + list(cls.implements):
            deps.add(self.analyzer.resolve_type(type_text, cls) or "")
        for field_name, type_text in cls.fields.items():
            if re.search(rf"\b{re.escape(field_name)}\b", selected_source):
                deps.add(self.analyzer.resolve_type(type_text, cls) or "")
        for type_name in self.extract_type_names_from_text(selected_source):
            deps.add(self.analyzer.resolve_type(type_name, cls) or "")

        for imported in cls.imports.values():
            simple = imported.split(".")[-1]
            if simple != "*" and re.search(rf"\b{re.escape(simple)}\b", selected_source):
                deps.add(imported if imported in self.analyzer.classes else "")
        for imported in cls.static_imports.values():
            member = imported.split(".")[-1]
            owner = ".".join(imported.split(".")[:-1])
            if re.search(rf"\b{re.escape(member)}\b", selected_source):
                deps.add(owner if owner in self.analyzer.classes else "")
        for owner in cls.static_wildcards:
            if owner in self.analyzer.classes:
                deps.add(owner)

        deps.discard("")
        deps.discard(cls.fqn)
        return {dep for dep in deps if dep in self.analyzer.classes}

    @staticmethod
    def extract_type_names_from_text(text: str) -> Set[str]:
        names: Set[str] = set()
        for match in re.finditer(r"\b[A-Z][A-Za-z0-9_]*\b", text):
            names.add(match.group(0))
        for match in re.finditer(r"\b([a-z_]\w*(?:\.[A-Za-z_]\w*){2,})\b", text):
            names.add(clean_type(match.group(1)))
        return names

    def collect_dependency_classes(self, method_closure: Set[MethodKey]) -> Set[str]:
        selected: Set[str] = {
            key.class_fqn for key in method_closure if key.class_fqn in self.analyzer.classes
        }
        queue: deque[str] = deque(sorted(selected))

        def add_class(fqn: Optional[str]) -> None:
            if fqn and fqn in self.analyzer.classes and fqn not in selected:
                selected.add(fqn)
                queue.append(fqn)

        while queue:
            fqn = queue.popleft()
            cls = self.analyzer.classes.get(fqn)
            if not cls:
                continue
            for sibling in self.classes_by_path.get(cls.path.resolve(), []):
                add_class(sibling.fqn)
            for dep in self.class_dependencies(cls):
                add_class(dep)
            if cls.kind == "interface":
                for impl_fqn in self.analyzer.interface_impls.get(cls.fqn, set()):
                    add_class(impl_fqn)
            for method in cls.methods:
                for callee in self.analyzer.graph.get(method.key, set()):
                    add_class(callee.class_fqn)

        return selected

    def class_dependencies(self, cls: ClassInfo) -> Set[str]:
        deps: Set[str] = set()

        for imported in cls.imports.values():
            if imported.endswith(".*"):
                deps.update(self.classes_in_package(imported[:-2]))
            elif imported in self.analyzer.classes:
                deps.add(imported)
        for imported in cls.static_imports.values():
            owner = ".".join(imported.split(".")[:-1])
            if owner in self.analyzer.classes:
                deps.add(owner)
        for owner in cls.static_wildcards:
            if owner in self.analyzer.classes:
                deps.add(owner)

        for type_text in list(cls.extends) + list(cls.implements):
            deps.add(self.analyzer.resolve_type(type_text, cls) or "")
        for type_text in cls.fields.values():
            deps.add(self.analyzer.resolve_type(type_text, cls) or "")
        for method in cls.methods:
            for type_text in method.params.values():
                deps.add(self.analyzer.resolve_type(type_text, cls) or "")

        for type_name in self.extract_type_names(cls):
            deps.add(self.analyzer.resolve_type(type_name, cls) or "")

        deps.discard("")
        deps.discard(cls.fqn)
        return {dep for dep in deps if dep in self.analyzer.classes}

    def classes_in_package(self, package_name: str) -> Set[str]:
        return {
            fqn
            for fqn, cls in self.analyzer.classes.items()
            if cls.package == package_name
        }

    @staticmethod
    def extract_type_names(cls: ClassInfo) -> Set[str]:
        names: Set[str] = set()
        masked = cls.masked[cls.class_start : cls.body_end + 1]
        for match in re.finditer(r"\b[A-Z][A-Za-z0-9_]*\b", masked):
            names.add(match.group(0))
        for match in re.finditer(r"\b([a-z_]\w*(?:\.[A-Za-z_]\w*){2,})\b", masked):
            candidate = match.group(1)
            if "." in candidate:
                names.add(clean_type(candidate))
        return names

    def resolve_package_map(self, class_fqns: Set[str]) -> Dict[str, str]:
        if self.options.package_map or self.options.target_base_package:
            raise ValueError(
                "package rewrite is disabled: migrated files must keep original package names and directories"
            )
        return {}

    def collect_resources(self, class_fqns: Set[str]) -> Set[Path]:
        resources: Set[Path] = set()
        for fqn in class_fqns:
            for path in self.analyzer.xml_namespaces.get(fqn, []):
                resources.add(path.resolve())
        return resources

    def collect_resource_statements(
        self,
        methods_by_class: Dict[str, Set[MethodKey]],
    ) -> Dict[str, Set[str]]:
        resources: Dict[str, Set[str]] = defaultdict(set)
        for class_fqn, keys in methods_by_class.items():
            xml_paths = self.analyzer.xml_namespaces.get(class_fqn, [])
            if not xml_paths:
                continue
            keep_names = {
                self.analyzer.methods[key].name
                for key in keys
                if key in self.analyzer.methods
            }
            for path in xml_paths:
                resources[str(path.resolve())].update(keep_names)
        return dict(resources)

    def plan_files(
        self,
        class_fqns: Set[str],
        methods_by_class: Dict[str, Set[MethodKey]],
        resource_paths: Set[Path],
        package_map: Dict[str, str],
        class_fqn_map: Dict[str, str],
    ) -> List[PlannedFile]:
        target_java_dir = default_target_java_dir(self.options)
        target_resource_dir = default_target_resource_dir(self.options)
        files: List[PlannedFile] = []
        planned_sources: Set[Path] = set()

        for source in sorted({self.analyzer.classes[fqn].path.resolve() for fqn in class_fqns}):
            classes = self.classes_by_path.get(source, [])
            if not classes:
                continue
            primary = classes[0]
            target_package = rewrite_package_name(primary.package, package_map)
            target = target_java_dir / Path(*target_package.split(".")) / source.name
            if not target_package:
                target = target_java_dir / source.name
            source_rel = str(relpath(source, self.source_root))
            target_rel = self.target_rel(target)
            files.append(
                PlannedFile(
                    kind="java",
                    source=source,
                    target=target,
                    source_rel=source_rel,
                    target_rel=target_rel,
                    rewritten=True,
                    selected_methods=sorted(
                        method_id_from_method(self.analyzer.methods[key])
                        for cls in classes
                        for key in methods_by_class.get(cls.fqn, set())
                        if key in self.analyzer.methods
                    ),
                )
            )
            planned_sources.add(source)

        for source in sorted(resource_paths):
            if source in planned_sources:
                continue
            resource_root = find_marker_root(source, ("src", "main", "resources"))
            rel = safe_relative(source, resource_root) if resource_root else safe_relative(source, self.source_root)
            target = target_resource_dir / rel
            files.append(
                PlannedFile(
                    kind="resource",
                    source=source,
                    target=target,
                    source_rel=str(relpath(source, self.source_root)),
                    target_rel=self.target_rel(target),
                    rewritten=True,
                )
            )

        return files

    def target_rel(self, path: Path) -> str:
        if self.options.target_root:
            return str(safe_relative(path, self.options.target_root))
        return str(path)

    def build_warnings(
        self,
        method_closure: Set[MethodKey],
        class_fqns: Set[str],
        unresolved: Sequence[str],
    ) -> List[str]:
        warnings: List[str] = []
        if unresolved:
            warnings.append("some requested API paths were not resolved")
        for key in sorted(method_closure, key=lambda item: item.text()):
            method = self.analyzer.methods.get(key)
            if not method:
                warnings.append(f"method not found in index: {key.text()}")
                continue
            if method.class_fqn not in class_fqns:
                warnings.append(f"method class not selected: {method.label}")
        warnings.append(
            "using method-level endpoint call graph; Java files are sliced to selected methods and required members"
        )
        if not self.options.include_resources:
            warnings.append("resource copying is disabled; MyBatis XML files may be missing")
        return warnings


class ApiMigrator:
    def __init__(self, options: MigrationOptions) -> None:
        self.options = options

    def migrate(self, plan: MigrationPlan) -> MigrationResult:
        result = MigrationResult()
        planned_targets = {item.target.resolve() for item in plan.files}

        for item in plan.files:
            content = self.render_file(item, plan)
            if item.kind == "java" and item.target.exists() and not self.options.overwrite:
                merge = self.merge_java_target(item, plan, content)
                if merge.conflicts:
                    item.status = "conflict"
                    result.conflicts.extend(merge.conflicts)
                    continue
                result.skipped_conflicts.extend(merge.skipped_conflicts)
                result.skipped_existing_models.extend(merge.skipped_existing_models)
                if merge.skipped_conflicts and not merge.changed:
                    item.status = "skipped-conflict"
                    continue
                if merge.skipped_existing_models and not merge.changed:
                    item.status = "skipped-existing-model"
                    continue
                if not merge.changed:
                    item.status = "skipped-same"
                    result.skipped_same.append(item.target_rel)
                    continue
                item.target.parent.mkdir(parents=True, exist_ok=True)
                write_text(item.target, merge.text)
                item.status = "written"
                result.written.append(item.target_rel)
                continue
            if item.kind == "resource" and item.target.exists() and not self.options.overwrite:
                merge = self.merge_resource_target(item, plan, content)
                if merge.conflicts:
                    item.status = "conflict"
                    result.conflicts.extend(merge.conflicts)
                    continue
                result.skipped_conflicts.extend(merge.skipped_conflicts)
                if merge.skipped_conflicts and not merge.changed:
                    item.status = "skipped-conflict"
                    continue
                if not merge.changed:
                    item.status = "skipped-same"
                    result.skipped_same.append(item.target_rel)
                    continue
                item.target.parent.mkdir(parents=True, exist_ok=True)
                write_text(item.target, merge.text)
                item.status = "written"
                result.written.append(item.target_rel)
                continue
            if item.target.exists():
                existing = read_text(item.target)
                if existing == content:
                    item.status = "skipped-same"
                    result.skipped_same.append(item.target_rel)
                    continue
                if not self.options.overwrite:
                    if self.options.skip_conflicts:
                        item.status = "skipped-conflict"
                        result.skipped_conflicts.append(item.target_rel)
                        continue
                    item.status = "conflict"
                    result.conflicts.append(item.target_rel)
                    continue
            item.target.parent.mkdir(parents=True, exist_ok=True)
            write_text(item.target, content)
            item.status = "written"
            result.written.append(item.target_rel)

        if self.options.update_target_refs and plan.class_fqn_map:
            result.updated_refs = self.update_existing_target_refs(plan, planned_targets)

        return result

    def merge_resource_target(self, item: PlannedFile, plan: MigrationPlan, source_text: str) -> ResourceMergePlan:
        existing = read_text(item.target)
        if existing == source_text:
            return ResourceMergePlan(text=existing)
        if item.source.suffix.lower() != ".xml":
            if self.options.skip_conflicts:
                return ResourceMergePlan(text=existing, skipped_conflicts=[item.target_rel])
            return ResourceMergePlan(text=existing, conflicts=[item.target_rel])

        keep_ids = plan.resource_statements.get(str(item.source.resolve()), set())
        if not keep_ids:
            if self.options.skip_conflicts:
                return ResourceMergePlan(text=existing, skipped_conflicts=[item.target_rel])
            return ResourceMergePlan(text=existing, conflicts=[item.target_rel])

        target_ids = {
            statement.statement_id
            for statement in iter_mybatis_statements(existing, "", item.target)
        }
        skipped = [
            f"{item.target_rel}#{statement_id}"
            for statement_id in sorted(keep_ids & target_ids)
        ]
        missing_ids = sorted(keep_ids - target_ids)
        statement_texts = [
            mybatis_statement_text(source_text, statement_id)
            for statement_id in missing_ids
        ]
        statement_texts = [text for text in statement_texts if text]
        if not statement_texts:
            return ResourceMergePlan(text=existing, skipped_conflicts=skipped)
        merged = append_mybatis_statements(existing, statement_texts)
        if merged == existing:
            if self.options.skip_conflicts:
                return ResourceMergePlan(text=existing, skipped_conflicts=[item.target_rel])
            return ResourceMergePlan(text=existing, conflicts=[item.target_rel])
        return ResourceMergePlan(
            text=merged,
            changed=True,
            skipped_conflicts=skipped,
        )

    def merge_java_target(self, item: PlannedFile, plan: MigrationPlan, source_text: str) -> JavaMergePlan:
        existing = read_text(item.target)
        if existing == source_text:
            return JavaMergePlan(text=existing)

        source_classes = parse_java_classes_from_text(source_text, item.source)
        target_classes = parse_java_classes_from_text(existing, item.target)
        original_classes = selected_classes_for_source(item, plan)
        original_by_name = {cls.name: cls for cls in original_classes}
        target_by_name = {cls.name: cls for cls in target_classes}
        source_by_name = {cls.name: cls for cls in source_classes}
        incoming_names = {cls.name for cls in source_classes} or {cls.name for cls in original_classes}

        merged_text = existing
        changed = False
        skipped_conflicts: List[str] = []
        skipped_models: List[str] = []

        for class_name in sorted(incoming_names):
            source_cls = source_by_name.get(class_name)
            original_cls = original_by_name.get(class_name)
            target_cls = target_by_name.get(class_name)
            classification_cls = original_cls or source_cls
            if not source_cls or not classification_cls:
                continue

            if is_model_or_enum(classification_cls):
                if target_cls:
                    skipped_models.append(f"{item.target_rel}#{class_name}")
                    continue
                conflict_label = f"{item.target_rel}#{class_name}"
                if self.options.skip_conflicts:
                    skipped_conflicts.append(conflict_label)
                    continue
                return JavaMergePlan(text=merged_text, conflicts=[conflict_label])

            if not target_cls:
                conflict_label = f"{item.target_rel}#{class_name}"
                if self.options.skip_conflicts:
                    skipped_conflicts.append(conflict_label)
                    continue
                return JavaMergePlan(text=merged_text, conflicts=[conflict_label])

            selected_names = {
                self.method_name_for_key(key)
                for key in plan.methods_by_class.get(classification_cls.fqn, set())
            }
            selected_names.discard("")
            source_members = extract_top_level_members(source_cls)
            incoming_methods = [
                block
                for block in source_members
                if block.kind == "method" and (not selected_names or block.name in selected_names)
            ]
            if not incoming_methods:
                continue

            target_method_names = {
                method.name
                for method in target_cls.methods
            }
            conflicting_names = sorted({block.name for block in incoming_methods if block.name in target_method_names})
            if conflicting_names:
                skipped_conflicts.extend(
                    f"{item.target_rel}#{class_name}.{name}"
                    for name in conflicting_names
                )
            migratable_methods = [
                block
                for block in incoming_methods
                if block.name not in target_method_names
            ]
            if not migratable_methods:
                continue
            migratable_method_source = "\n".join(block.text for block in migratable_methods)

            target_field_names = set(target_cls.fields)
            target_members = extract_top_level_members(target_cls)
            target_member_keys = {member_text_key(block.text) for block in target_members}
            target_constructors = {
                (block.name, block.param_count)
                for block in target_members
                if block.kind == "constructor"
            }
            incoming_field_names = set()
            members_to_append: List[JavaMemberBlock] = []
            for block in source_members:
                if block.kind == "field":
                    new_fields = block.field_names - target_field_names - incoming_field_names
                    if (
                        new_fields
                        and member_text_key(block.text) not in target_member_keys
                        and should_keep_field_or_initializer(block.text, migratable_method_source, source_cls)
                    ):
                        members_to_append.append(block)
                        incoming_field_names.update(new_fields)
                    continue
                if block.kind in {"initializer", "block", "statement"}:
                    if (
                        member_text_key(block.text) not in target_member_keys
                        and should_keep_field_or_initializer(block.text, migratable_method_source, source_cls)
                    ):
                        members_to_append.append(block)
                    continue
                if block.kind == "constructor":
                    same_ctor = (block.name, block.param_count) in target_constructors
                    if not same_ctor and member_text_key(block.text) not in target_member_keys:
                        members_to_append.append(block)
                    continue
                if block.kind == "method" and block in migratable_methods:
                    members_to_append.append(block)

            if not members_to_append:
                continue

            merged_text = merge_imports(merged_text, source_text)
            refreshed_classes = parse_java_classes_from_text(merged_text, item.target)
            refreshed_target = next((cls for cls in refreshed_classes if cls.name == class_name), None)
            if not refreshed_target:
                conflict_label = f"{item.target_rel}#{class_name}"
                if self.options.skip_conflicts:
                    skipped_conflicts.append(conflict_label)
                    continue
                return JavaMergePlan(text=merged_text, conflicts=[conflict_label])
            merged_text = append_members_to_class(merged_text, refreshed_target, members_to_append)
            changed = True
            target_classes = parse_java_classes_from_text(merged_text, item.target)
            target_by_name = {cls.name: cls for cls in target_classes}

        return JavaMergePlan(
            text=merged_text,
            changed=changed,
            skipped_conflicts=skipped_conflicts,
            skipped_existing_models=skipped_models,
        )

    @staticmethod
    def method_name_for_key(key: MethodKey) -> str:
        return key.name

    def render_file(self, item: PlannedFile, plan: MigrationPlan) -> str:
        text = read_text(item.source)
        if item.kind == "java":
            if should_copy_whole_java_file(item, plan):
                return text
            classes = getattr(plan, "all_classes_by_path", {}).get(item.source.resolve(), [])
            sliced, selected = slice_java_text(
                text,
                classes,
                plan.methods_by_class,
                set(plan.class_fqns),
            )
            item.selected_methods = selected
            return sliced if sliced.strip() else text
        keep_ids = plan.resource_statements.get(str(item.source.resolve()), set())
        namespace = ""
        for class_fqn, paths in getattr(plan, "xml_namespaces", {}).items():
            if item.source in paths or item.source.resolve() in {p.resolve() for p in paths}:
                namespace = class_fqn
                break
        if not namespace:
            match = re.search(r'\bnamespace\s*=\s*["\']([^"\']+)["\']', text)
            namespace = match.group(1) if match else ""
        return prune_mybatis_xml(text, namespace, keep_ids, item.source)

    def update_existing_target_refs(
        self,
        plan: MigrationPlan,
        planned_targets: Set[Path],
    ) -> List[str]:
        if not self.options.target_root:
            return []
        touched: List[str] = []
        target_root = self.options.target_root.resolve()
        for dirpath, dirnames, filenames in os.walk(target_root):
            current = Path(dirpath)
            dirnames[:] = [
                d
                for d in dirnames
                if d not in {".git", ".idea", ".vscode", "target", "build", "out"}
            ]
            for filename in filenames:
                path = current / filename
                if path.suffix not in TARGET_REF_SUFFIXES:
                    continue
                if path.resolve() in planned_targets:
                    continue
                try:
                    text = read_text(path)
                except Exception:
                    continue
                rewritten = replace_fqns(text, plan.class_fqn_map)
                if rewritten != text:
                    write_text(path, rewritten)
                    touched.append(str(safe_relative(path, target_root)))
        return sorted(touched)


def report_dict(plan: MigrationPlan, result: Optional[MigrationResult], source_root: Path) -> Dict[str, Any]:
    return {
        "sourceRoot": str(source_root),
        "routes": [
            {
                "requestedPath": match.requested_path,
                "matchedRoute": normalize_path(match.matched_route),
                "method": match.method.label,
                "file": str(relpath(match.method.path, source_root)),
                "line": match.method.line,
            }
            for match in plan.routes
        ],
        "unresolvedPaths": list(plan.unresolved_paths),
        "unresolvedSuggestions": {
            path: [
                {
                    "matchedRoute": normalize_path(match.matched_route),
                    "method": match.method.label,
                    "file": str(relpath(match.method.path, source_root)),
                    "line": match.method.line,
                }
                for match in suggestions
            ]
            for path, suggestions in plan.unresolved_suggestions.items()
        },
        "summary": {
            "reachableMethods": len(plan.method_closure),
            "javaClasses": len(plan.class_fqns),
            "resourceFiles": len(plan.resource_paths),
            "plannedFiles": len(plan.files),
            "rewrittenFiles": sum(1 for item in plan.files if item.rewritten),
        },
        "packageMap": plan.package_map,
        "classFqnMap": plan.class_fqn_map,
        "javaClasses": sorted(plan.class_fqns),
        "files": [
            {
                "kind": item.kind,
                "source": item.source_rel,
                "target": item.target_rel,
                "rewritten": item.rewritten,
                "status": item.status,
                "selectedMethods": item.selected_methods,
            }
            for item in plan.files
        ],
        "warnings": plan.warnings,
        "result": None
        if result is None
        else {
            "written": result.written,
            "skippedSame": result.skipped_same,
            "skippedConflicts": result.skipped_conflicts,
            "skippedExistingModels": result.skipped_existing_models,
            "conflicts": result.conflicts,
            "updatedRefs": result.updated_refs,
        },
    }


def write_report_file(
    options: MigrationOptions,
    plan: MigrationPlan,
    result: Optional[MigrationResult],
) -> Optional[Path]:
    if options.no_report or not options.report:
        return None
    base = options.target_root or options.source_root
    report_path = Path(options.report)
    if not report_path.is_absolute():
        report_path = base / report_path
    report_path.parent.mkdir(parents=True, exist_ok=True)
    write_text(report_path, json.dumps(report_dict(plan, result, options.source_root), ensure_ascii=False, indent=2))
    return report_path


def print_summary(
    action: str,
    options: MigrationOptions,
    plan: MigrationPlan,
    result: Optional[MigrationResult],
    report_path: Optional[Path],
    limit: int,
) -> None:
    print(f"[{action.upper()}] API migration")
    print(f"Source root: {options.source_root.resolve()}")
    if options.target_root:
        print(f"Target root: {options.target_root.resolve()}")
    print()

    if plan.routes:
        print("Matched routes:")
        for match in plan.routes:
            print(
                f"  - {match.requested_path} -> {normalize_path(match.matched_route)} "
                f"=> {match.method.label} ({relpath(match.method.path, options.source_root)}:{match.method.line})"
            )
    if plan.unresolved_paths:
        print("Unresolved paths:")
        for path in plan.unresolved_paths:
            print(f"  - {path}")
            suggestions = plan.unresolved_suggestions.get(path, [])
            if suggestions:
                print("    Similar routes:")
                for match in suggestions[:5]:
                    print(
                        f"      - {normalize_path(match.matched_route)} "
                        f"=> {match.method.label} ({relpath(match.method.path, options.source_root)}:{match.method.line})"
                    )
    print()
    print(
        "Summary: "
        f"reachableMethods={len(plan.method_closure)}, "
        f"javaClasses={len(plan.class_fqns)}, "
        f"resources={len(plan.resource_paths)}, "
        f"plannedFiles={len(plan.files)}"
    )
    if plan.warnings:
        print("Warnings:")
        for warning in plan.warnings[:limit]:
            print(f"  - {warning}")
    print()

    print("Planned files:")
    for idx, item in enumerate(plan.files):
        if idx >= limit:
            print(f"  ... {len(plan.files) - limit} more")
            break
        rewrite = " rewritten" if item.rewritten else ""
        status = f" [{item.status}]" if result is not None else ""
        print(f"  - {item.kind}: {item.source_rel} -> {item.target_rel}{rewrite}{status}")

    if result is not None:
        print()
        print(
            "Migration result: "
            f"written={len(result.written)}, "
            f"skippedSame={len(result.skipped_same)}, "
            f"skippedConflicts={len(result.skipped_conflicts)}, "
            f"skippedExistingModels={len(result.skipped_existing_models)}, "
            f"conflicts={len(result.conflicts)}, "
            f"updatedRefs={len(result.updated_refs)}"
        )
        if result.conflicts:
            print("Conflicts:")
            for path in result.conflicts[:limit]:
                print(f"  - {path}")
            print("Re-run with --skip-conflicts or --overwrite only after reviewing these target files.")
    else:
        print()
        print("No files changed. Re-run with the migrate action to copy files.")
    if report_path:
        print(f"Report: {report_path}")


def load_config(path: Optional[str]) -> Dict[str, Any]:
    if not path:
        return {}
    return json.loads(read_text(Path(path)))


def config_get(config: Dict[str, Any], key: str, default: Any = None) -> Any:
    return config.get(key, default)


def as_path(value: Optional[str], base: Optional[Path] = None) -> Optional[Path]:
    if not value:
        return None
    path = Path(value)
    if not path.is_absolute() and base is not None:
        path = base / path
    return path


def merge_paths(args: argparse.Namespace, config: Dict[str, Any]) -> List[str]:
    paths: List[str] = []
    paths.extend(config_get(config, "paths", []) or [])
    paths.extend(args.paths or [])
    paths.extend(args.positional_paths or [])
    paths_file = args.paths_file or config_get(config, "pathsFile")
    if paths_file:
        for line in read_text(Path(paths_file)).splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                paths.append(line)
    return paths


def build_options(args: argparse.Namespace, config: Dict[str, Any]) -> MigrationOptions:
    source_root = Path(args.source_root or config_get(config, "sourceRoot", ".")).resolve()
    target_root = as_path(args.target_root or config_get(config, "targetRoot"))
    if target_root:
        target_root = target_root.resolve()

    target_dir = args.target_dir or config_get(config, "targetDir")
    target_java_dir = args.target_java_dir or config_get(config, "targetJavaDir") or target_dir
    target_resource_dir = args.target_resource_dir or config_get(config, "targetResourceDir")
    target_java_path = as_path(target_java_dir, target_root)
    target_resource_path = as_path(target_resource_dir, target_root)

    explicit_maps = []
    explicit_maps.extend(config_get(config, "packageMap", []) or [])
    explicit_maps.extend(args.package_map or [])
    if isinstance(explicit_maps, dict):
        package_map = {
            normalize_package(k) or "": normalize_package(v) or ""
            for k, v in explicit_maps.items()
        }
        package_map = {k: v for k, v in package_map.items() if k and v}
    else:
        package_map = parse_package_map(explicit_maps)

    include_resources = bool(config_get(config, "includeResources", True))
    if args.no_resources:
        include_resources = False

    whole_file_closure = bool(
        config_get(config, "wholeFileClosure", False)
        or config_get(config, "includeImportClosure", False)
        or args.whole_file_closure
    )
    if args.method_closure_only:
        whole_file_closure = False

    overwrite = bool(config_get(config, "overwrite", False) or args.overwrite)
    skip_conflicts = bool(config_get(config, "skipConflicts", False) or args.skip_conflicts)
    update_target_refs = bool(config_get(config, "updateTargetRefs", False) or args.update_target_refs)

    report = args.report if args.report is not None else config_get(config, "report", "api_migration_report.json")
    no_report = bool(config_get(config, "noReport", False) or args.no_report)

    return MigrationOptions(
        source_root=source_root,
        target_root=target_root,
        target_java_dir=target_java_path,
        target_resource_dir=target_resource_path,
        include_tests=bool(config_get(config, "includeTests", False) or args.include_tests),
        exact_route_match=bool(config_get(config, "exactRouteMatch", False) or args.exact_route_match),
        max_depth=int(args.max_depth or config_get(config, "maxDepth", 80)),
        include_resources=include_resources,
        whole_file_closure=whole_file_closure,
        source_base_package=args.source_base_package or config_get(config, "sourceBasePackage"),
        target_base_package=args.target_base_package or config_get(config, "targetBasePackage"),
        package_map=package_map,
        overwrite=overwrite,
        skip_conflicts=skip_conflicts,
        update_target_refs=update_target_refs,
        report=report or "",
        no_report=no_report,
        verbose=bool(config_get(config, "verbose", False) or args.verbose),
    )


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Migrate a Spring MVC API endpoint and its project-local Java dependencies."
    )
    parser.add_argument(
        "action",
        nargs="?",
        default="plan",
        help="Action to run: plan or migrate. If omitted, plan is used.",
    )
    parser.add_argument("positional_paths", nargs="*", help="API paths to migrate")
    parser.add_argument("--paths", nargs="+", help="API paths to migrate")
    parser.add_argument("--paths-file", help="Text file containing one API path per line")
    parser.add_argument("--config", help="JSON config file")
    parser.add_argument("--source-root", help="Source project root. Defaults to current directory")
    parser.add_argument("--target-root", help="Target project root")
    parser.add_argument(
        "--target-dir",
        help="Target Java migration root. Usually target-project/src/main/java",
    )
    parser.add_argument("--target-java-dir", help="Explicit target src/main/java directory")
    parser.add_argument("--target-resource-dir", help="Explicit target src/main/resources directory")
    parser.add_argument("--include-tests", action="store_true", help="Include src/test/java in analysis")
    parser.add_argument(
        "--exact-route-match",
        action="store_true",
        help="Disable matching /foo/{id} against concrete paths such as /foo/1",
    )
    parser.add_argument("--max-depth", type=int, help="Maximum method-call depth to traverse")
    parser.add_argument("--no-resources", action="store_true", help="Do not copy matched resource/XML files")
    parser.add_argument(
        "--method-closure-only",
        action="store_true",
        help="Only collect classes directly reached by the endpoint call graph. This is the default.",
    )
    parser.add_argument(
        "--whole-file-closure",
        action="store_true",
        help="Deprecated compatibility flag. Method-level slicing is always used.",
    )
    parser.add_argument("--source-base-package", help="Deprecated. Package rewrites are disabled.")
    parser.add_argument("--target-base-package", help="Deprecated. Package rewrites are disabled.")
    parser.add_argument(
        "--package-map",
        action="append",
        default=[],
        help="Deprecated. Package rewrites are disabled.",
    )
    parser.add_argument("--overwrite", action="store_true", help="Overwrite different target files")
    parser.add_argument(
        "--skip-conflicts",
        action="store_true",
        help="Skip target classes/files with migration conflicts instead of stopping on them",
    )
    parser.add_argument(
        "--update-target-refs",
        action="store_true",
        help="Rewrite old fully-qualified class names in existing target project files",
    )
    parser.add_argument(
        "--report",
        default=None,
        help="JSON report path. Defaults to api_migration_report.json",
    )
    parser.add_argument("--no-report", action="store_true", help="Do not write a JSON report")
    parser.add_argument("--limit", type=int, default=80, help="Console item limit")
    parser.add_argument("--verbose", action="store_true", help="Verbose diagnostics")
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(argv)
    if args.action not in {"plan", "migrate"}:
        args.positional_paths.insert(0, args.action)
        args.action = "plan"

    config = load_config(args.config)
    paths = merge_paths(args, config)
    options = build_options(args, config)
    if not paths:
        parser.error("provide API paths via positional args, --paths, --paths-file, or config.paths")
    if args.action == "migrate" and not (options.target_root or options.target_java_dir):
        parser.error("migrate requires --target-root, --target-dir, or --target-java-dir")

    planner = ApiMigrationPlanner(options)
    plan = planner.build(paths)

    result: Optional[MigrationResult] = None
    if args.action == "migrate":
        result = ApiMigrator(options).migrate(plan)
    report_path = write_report_file(options, plan, result)
    if result:
        result.report_path = report_path
    print_summary(args.action, options, plan, result, report_path, args.limit)

    if plan.unresolved_paths:
        return 2
    if result and result.conflicts:
        return 3
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
