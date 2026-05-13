#!/usr/bin/env python3
"""
Web management page for dead_api_cleaner.py.

Start:

  python -B scripts/dead_api_cleaner_web.py --host 127.0.0.1 --port 8765

Then open:

  http://127.0.0.1:8765

The Web UI uses the same DeadApiCleanupService as the CLI. Preview is dry-run
by default. Use the separate delete button only after reviewing the plan.
"""

from __future__ import annotations

import argparse
import html
import json
import sys
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Dict, List, Tuple
from urllib.parse import urlparse

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from dead_api_cleaner import CleanupRunOptions, DeadApiCleanupService


DEFAULT_ROOT = SCRIPT_DIR.parent.resolve()


INDEX_HTML = r"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Dead API Cleaner</title>
  <style>
    :root {
      color-scheme: light;
      --bg: #f6f7f9;
      --panel: #ffffff;
      --line: #d9dee7;
      --text: #17202a;
      --muted: #667085;
      --blue: #1f6feb;
      --red: #b42318;
      --green: #067647;
      --amber: #b54708;
      --code: #111827;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
      background: var(--bg);
      color: var(--text);
    }
    header {
      padding: 16px 24px;
      background: #111827;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    header h1 { margin: 0; font-size: 18px; font-weight: 650; }
    header .sub { color: #cbd5e1; font-size: 13px; }
    main {
      display: grid;
      grid-template-columns: minmax(340px, 430px) minmax(0, 1fr);
      gap: 16px;
      padding: 16px;
      min-height: calc(100vh - 58px);
    }
    section {
      background: var(--panel);
      border: 1px solid var(--line);
      border-radius: 8px;
      min-width: 0;
    }
    .config {
      padding: 16px;
      align-self: start;
      position: sticky;
      top: 16px;
      max-height: calc(100vh - 90px);
      overflow: auto;
    }
    .preview {
      padding: 0;
      overflow: hidden;
    }
    h2 {
      font-size: 15px;
      margin: 0 0 12px;
    }
    label {
      display: block;
      font-size: 12px;
      color: var(--muted);
      margin: 12px 0 6px;
    }
    textarea, input[type="text"], input[type="number"] {
      width: 100%;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 9px 10px;
      font: inherit;
      background: #fff;
      color: var(--text);
    }
    textarea {
      min-height: 180px;
      resize: vertical;
      font-family: Consolas, "Courier New", monospace;
      font-size: 13px;
    }
    .row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
    }
    .checks {
      margin-top: 12px;
      display: grid;
      gap: 8px;
    }
    .check {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--text);
    }
    .check input { width: 16px; height: 16px; }
    .actions {
      margin-top: 16px;
      display: flex;
      gap: 10px;
      align-items: center;
    }
    button {
      border: 0;
      border-radius: 6px;
      padding: 9px 13px;
      font-weight: 650;
      cursor: pointer;
    }
    .primary { background: var(--blue); color: #fff; }
    .secondary { background: #e5e7eb; color: #111827; }
    .danger { background: var(--red); color: #fff; }
    button:disabled { opacity: .55; cursor: not-allowed; }
    .status {
      padding: 12px 16px;
      border-bottom: 1px solid var(--line);
      background: #fbfcfe;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      min-height: 54px;
    }
    .summary {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      font-size: 12px;
    }
    .pill {
      border: 1px solid var(--line);
      border-radius: 999px;
      padding: 3px 8px;
      background: #fff;
      color: var(--muted);
    }
    .content {
      padding: 14px;
      overflow: auto;
      max-height: calc(100vh - 128px);
    }
    .empty {
      color: var(--muted);
      padding: 24px;
      text-align: center;
    }
    .api {
      border: 1px solid var(--line);
      border-radius: 8px;
      margin-bottom: 14px;
      overflow: hidden;
      background: #fff;
    }
    .api-head {
      padding: 12px 14px;
      border-bottom: 1px solid var(--line);
      background: #f8fafc;
    }
    .api-title {
      font-weight: 700;
      word-break: break-all;
    }
    .api-meta {
      margin-top: 5px;
      color: var(--muted);
      font-size: 12px;
      word-break: break-all;
    }
    .tree {
      padding: 8px 8px 8px 12px;
    }
    .node {
      border-left: 2px solid #cfd6e4;
      margin-left: 8px;
      padding: 6px 0 6px 12px;
    }
    .node-line {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      min-height: 28px;
    }
    .child-toggle {
      border: 1px solid var(--line);
      background: #fff;
      color: #344054;
      border-radius: 5px;
      padding: 3px 7px;
      font-size: 12px;
      font-weight: 650;
      cursor: pointer;
    }
    .child-toggle:hover {
      background: #f2f4f7;
    }
    .children[hidden] {
      display: none;
    }
    .method {
      font-family: Consolas, "Courier New", monospace;
      font-size: 13px;
      word-break: break-all;
    }
    .badge {
      border-radius: 999px;
      padding: 2px 7px;
      font-size: 12px;
      font-weight: 700;
    }
    .delete { color: var(--green); background: #dcfae6; }
    .retain { color: var(--amber); background: #fef0c7; }
    .file {
      color: var(--muted);
      font-size: 12px;
      word-break: break-all;
      margin: 2px 0 4px;
    }
    details.codebox {
      margin: 6px 0 2px;
      border: 1px solid var(--line);
      border-radius: 6px;
      background: #0b1020;
      overflow: hidden;
    }
    details.codebox summary {
      cursor: pointer;
      padding: 7px 9px;
      color: #dbe4ff;
      background: #111827;
      font-size: 12px;
      user-select: none;
    }
    pre {
      margin: 0;
      padding: 10px;
      overflow: auto;
      color: #e5e7eb;
      font: 12px/1.5 Consolas, "Courier New", monospace;
      white-space: pre;
    }
    .reasons {
      color: var(--muted);
      font-size: 12px;
      margin: 4px 0 0;
    }
    .error {
      margin: 12px 0;
      padding: 10px 12px;
      border: 1px solid #fecdca;
      background: #fef3f2;
      color: var(--red);
      border-radius: 6px;
      white-space: pre-wrap;
    }
    .unresolved {
      margin-bottom: 14px;
      border: 1px solid #fedf89;
      background: #fffaeb;
      border-radius: 8px;
      padding: 12px;
    }
    .unresolved h3 { margin: 0 0 8px; font-size: 14px; }
    .unresolved ul { margin: 6px 0 0; padding-left: 20px; }
    @media (max-width: 980px) {
      main { grid-template-columns: 1fr; }
      .config { position: static; max-height: none; }
      .content { max-height: none; }
    }
  </style>
</head>
<body>
  <header>
    <div>
      <h1>Dead API Cleaner</h1>
      <div class="sub">Spring API 调用链分析与安全删除预览</div>
    </div>
    <div class="sub">CLI 与 Web 共用同一分析服务</div>
  </header>
  <main>
    <section class="config">
      <h2>批量输入区</h2>
      <label for="paths">接口路径，每行一个</label>
      <textarea id="paths" spellcheck="false">/waybill/errorAddress/updateInfo</textarea>

      <h2 style="margin-top:18px">运行参数</h2>
      <label for="root">项目根目录</label>
      <input id="root" type="text" value="__ROOT__">

      <div class="row">
        <div>
          <label for="maxDepth">最大调用深度</label>
          <input id="maxDepth" type="number" min="1" max="500" value="80">
        </div>
        <div>
          <label for="limit">预览节点上限</label>
          <input id="limit" type="number" min="10" max="5000" value="1200">
        </div>
      </div>

      <label for="report">报告路径</label>
      <input id="report" type="text" value="dead_api_cleanup_report.json">

      <div class="checks">
        <label class="check"><input id="includeTests" type="checkbox"> Include src/test in analysis</label>
        <label class="check"><input id="exactRoute" type="checkbox"> Exact route match; do not match path variables</label>
        <label class="check"><input id="deleteXml" type="checkbox" checked> Delete matching MyBatis XML statements</label>
        <label class="check"><input id="writeReport" type="checkbox" checked> Write JSON report</label>
      </div>

      <div class="actions">
        <button class="primary" id="runBtn">Analyze</button>
        <button class="danger" id="deleteBtn">Delete</button>
        <button class="secondary" id="clearBtn">Clear</button>
      </div>
    </section>

    <section class="preview">
      <div class="status">
        <div id="statusText">Waiting</div>
        <div class="summary" id="summary"></div>
      </div>
      <div class="content" id="preview">
        <div class="empty">Enter API paths, then click Analyze.</div>
      </div>
    </section>
  </main>
  <script>
    const $ = (id) => document.getElementById(id);
    const runBtn = $("runBtn");
    const deleteBtn = $("deleteBtn");
    const clearBtn = $("clearBtn");
    const preview = $("preview");
    const statusText = $("statusText");
    const summary = $("summary");

    clearBtn.addEventListener("click", () => {
      preview.innerHTML = '<div class="empty">Enter API paths, then click Analyze.</div>';
      statusText.textContent = "Waiting";
      summary.innerHTML = "";
    });

    runBtn.addEventListener("click", async () => {
      await submit("/api/analyze");
    });

    deleteBtn.addEventListener("click", async () => {
      const ok = window.confirm("Confirm deletion? This will modify local Java/XML files.");
      if (!ok) {
        return;
      }
      await submit("/api/delete");
    });

    async function submit(endpoint) {
      const deleting = endpoint === "/api/delete";
      const paths = $("paths").value.split(/\r?\n/).map(v => v.trim()).filter(v => v && !v.startsWith("#"));
      const payload = {
        paths,
        root: $("root").value.trim(),
        maxDepth: Number($("maxDepth").value || 80),
        limit: Number($("limit").value || 1200),
        report: $("report").value.trim(),
        includeTests: $("includeTests").checked,
        exactRouteMatch: $("exactRoute").checked,
        deleteXml: $("deleteXml").checked,
        writeReport: $("writeReport").checked
      };
      if (!paths.length) {
        preview.innerHTML = '<div class="error">Enter at least one API path.</div>';
        return;
      }
      runBtn.disabled = true;
      deleteBtn.disabled = true;
      statusText.textContent = deleting ? "Deleting..." : "Analyzing...";
      summary.innerHTML = "";
      preview.innerHTML = '<div class="empty">' + (deleting ? "Deleting" : "Analyzing") + '. Large repositories may take tens of seconds.</div>';
      try {
        const resp = await fetch(endpoint, {
          method: "POST",
          headers: {"Content-Type": "application/json"},
          body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (!resp.ok || !data.ok) {
          throw new Error(data.error || ("HTTP " + resp.status));
        }
        render(data.report, payload.limit);
        statusText.textContent = deleting ? "Deletion completed" : "Preview generated";
      } catch (err) {
        statusText.textContent = "Failed";
        preview.innerHTML = '<div class="error">' + escapeHtml(String(err.message || err)) + '</div>';
      } finally {
        runBtn.disabled = false;
        deleteBtn.disabled = false;
      }
    }

    function render(report, limit) {
      const s = report.summary || {};
      summary.innerHTML = [
        pill("接口", (report.routes || []).length),
        pill("可达方法", s.reachableMethods || 0),
        pill("待删除", s.removableMethods || 0),
        pill("保留边界", s.retainedBoundaryMethods || 0)
      ].join("");

      const parts = [];
      if ((report.unresolvedPaths || []).length) {
        parts.push(renderUnresolved(report));
      }
      const trees = report.apiTrees || [];
      if (!trees.length && !parts.length) {
        preview.innerHTML = '<div class="empty">没有匹配到接口。</div>';
        return;
      }
      trees.forEach((api, index) => {
        parts.push(renderApi(api, index, limit));
      });
      preview.innerHTML = parts.join("");
    }

    function renderUnresolved(report) {
      const suggestions = report.unresolvedSuggestions || {};
      return (report.unresolvedPaths || []).map(path => {
        const items = (suggestions[path] || []).map(s =>
          '<li><code>' + escapeHtml(s.matchedRoute) + '</code> - ' +
          escapeHtml(s.method) + ' <span class="file">' + escapeHtml(s.file + ":" + s.line) + '</span></li>'
        ).join("");
        return '<div class="unresolved"><h3>未识别：' + escapeHtml(path) + '</h3>' +
          (items ? '<div>相似路由：</div><ul>' + items + '</ul>' : '<div>没有找到相似路由。</div>') +
          '</div>';
      }).join("");
    }

    function renderApi(api, index, limit) {
      const tree = api.tree;
      return '<article class="api">' +
        '<div class="api-head">' +
        '<div class="api-title">' + escapeHtml(api.requestedPath) + '</div>' +
        '<div class="api-meta">匹配路由：' + escapeHtml(api.matchedRoute) + '</div>' +
        '</div>' +
        '<div class="tree">' + renderNode(tree, "api" + index, 0, {count: 0, limit}) + '</div>' +
        '</article>';
    }

    function renderNode(node, prefix, depth, state) {
      if (!node || state.count >= state.limit) {
        return state.count >= state.limit ? '<div class="node"><span class="file">已达到预览节点上限。</span></div>' : "";
      }
      state.count += 1;
      const statusClass = node.status === "delete" ? "delete" : "retain";
      const statusText = node.status === "delete" ? "待删除" : "保留";
      const codeId = prefix + "-" + state.count;
      const childId = "children-" + codeId;
      const reasons = (node.reasons || []).slice(0, 4).map(r => '<div>' + escapeHtml(r) + '</div>').join("");
      const childNodes = node.children || [];
      const toggle = childNodes.length
        ? '<button type="button" class="child-toggle" data-target="' + childId + '" aria-expanded="true">收起子调用</button>'
        : "";
      const children = childNodes.map(child => renderNode(child, prefix, depth + 1, state)).join("");
      return '<div class="node" style="margin-left:' + Math.min(depth * 10, 80) + 'px">' +
        '<div class="node-line">' +
        '<span class="badge ' + statusClass + '">' + statusText + '</span>' +
        toggle +
        '<span class="method">' + escapeHtml(node.method || "") + '</span>' +
        '</div>' +
        '<div class="file">' + escapeHtml((node.file || "") + ":" + (node.line || "")) + '</div>' +
        (reasons ? '<div class="reasons">' + reasons + '</div>' : '') +
        '<details class="codebox">' +
        '<summary>查看/隐藏完整代码</summary>' +
        '<pre id="' + codeId + '">' + escapeHtml(node.code || "") + '</pre>' +
        '</details>' +
        (children ? '<div class="children" id="' + childId + '">' + children + '</div>' : '') +
        '</div>';
    }

    preview.addEventListener("click", (event) => {
      const button = event.target.closest(".child-toggle");
      if (!button) {
        return;
      }
      const target = document.getElementById(button.dataset.target);
      if (!target) {
        return;
      }
      const collapsed = !target.hidden;
      target.hidden = collapsed;
      button.setAttribute("aria-expanded", String(!collapsed));
      button.textContent = collapsed ? "展开子调用" : "收起子调用";
    });

    function pill(name, value) {
      return '<span class="pill">' + escapeHtml(name) + ': ' + escapeHtml(String(value)) + '</span>';
    }

    function escapeHtml(value) {
      return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
    }
  </script>
</body>
</html>
"""


class DeadApiCleanerWebHandler(BaseHTTPRequestHandler):
    server_version = "DeadApiCleanerWeb/1.0"

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path in ("", "/"):
            self.send_html(INDEX_HTML.replace("__ROOT__", html.escape(str(DEFAULT_ROOT))))
            return
        if parsed.path == "/health":
            self.send_json({"ok": True})
            return
        self.send_error(404, "Not Found")

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path not in ("/api/analyze", "/api/delete"):
            self.send_error(404, "Not Found")
            return
        try:
            payload = self.read_json()
            report = self.run_analysis(payload, delete=parsed.path == "/api/delete")
            self.send_json({"ok": True, "report": report})
        except Exception as exc:
            self.send_json(
                {
                    "ok": False,
                    "error": str(exc),
                    "trace": traceback.format_exc(limit=8),
                },
                status=500,
            )

    def read_json(self) -> Dict[str, Any]:
        length = int(self.headers.get("Content-Length", "0") or "0")
        raw = self.rfile.read(length)
        if not raw:
            return {}
        return json.loads(raw.decode("utf-8"))

    def run_analysis(self, payload: Dict[str, Any], delete: bool = False) -> Dict[str, Any]:
        paths = payload.get("paths") or []
        if not isinstance(paths, list):
            raise ValueError("paths must be a list")
        clean_paths = [str(path).strip() for path in paths if str(path).strip()]
        root = Path(str(payload.get("root") or DEFAULT_ROOT)).resolve()
        max_depth = int(payload.get("maxDepth") or 80)
        report = str(payload.get("report") or "dead_api_cleanup_report.json")
        write_report = bool(payload.get("writeReport", True))
        options = CleanupRunOptions(
            root=root,
            include_tests=bool(payload.get("includeTests", False)),
            exact_route_match=bool(payload.get("exactRouteMatch", False)),
            max_depth=max_depth,
            delete_xml=bool(payload.get("deleteXml", True)),
            delete=delete,
            report=report,
            no_report=not write_report,
        )
        result = DeadApiCleanupService(options).run(clean_paths)
        return result.report

    def send_html(self, body: str, status: int = 200) -> None:
        data = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def send_json(self, body: Dict[str, Any], status: int = 200) -> None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, fmt: str, *args: Tuple[Any, ...]) -> None:
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Start Dead API Cleaner Web UI.")
    parser.add_argument("--host", default="127.0.0.1", help="Bind host")
    parser.add_argument("--port", type=int, default=8765, help="Bind port")
    return parser


def main() -> int:
    args = build_arg_parser().parse_args()
    server = ThreadingHTTPServer((args.host, args.port), DeadApiCleanerWebHandler)
    url = f"http://{args.host}:{args.port}"
    print(f"Dead API Cleaner Web UI: {url}")
    print("Press Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print()
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
