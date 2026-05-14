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
    .action-groups {
      margin-top: 16px;
      display: grid;
      gap: 12px;
    }
    .action-group {
      border: 1px solid var(--line);
      border-radius: 8px;
      padding: 12px;
      background: #fbfcfe;
    }
    .action-title {
      font-size: 14px;
      font-weight: 700;
      margin-bottom: 4px;
    }
    .action-note {
      color: var(--muted);
      font-size: 12px;
      line-height: 1.45;
      margin-bottom: 10px;
    }
    .actions {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      align-items: center;
    }
    .utility-actions {
      display: flex;
      justify-content: flex-end;
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
    .preview-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
      padding: 14px;
      background: #fff;
    }
    .preview-grid.is-expanded {
      grid-template-columns: 1fr;
    }
    .preview-grid.is-expanded .preview-panel:not(.is-expanded) {
      display: none;
    }
    .preview-panel {
      min-width: 0;
      border: 1px solid var(--line);
      border-radius: 8px;
      overflow: hidden;
      background: #fff;
    }
    .preview-panel.is-expanded .content {
      max-height: calc(100vh - 182px);
    }
    .preview-panel-head {
      padding: 12px 14px;
      border-bottom: 1px solid var(--line);
      background: #f8fafc;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 12px;
    }
    .preview-panel-heading {
      min-width: 0;
    }
    .preview-panel-title {
      font-size: 14px;
      font-weight: 700;
    }
    .preview-panel-sub {
      margin-top: 4px;
      color: var(--muted);
      font-size: 12px;
    }
    .preview-panel-tools {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      flex-wrap: wrap;
      justify-content: flex-end;
      min-width: 144px;
    }
    .preview-toggle {
      border: 1px solid var(--line);
      background: #fff;
      color: #344054;
      border-radius: 5px;
      padding: 5px 9px;
      font-size: 12px;
      font-weight: 650;
      cursor: pointer;
      white-space: nowrap;
    }
    .preview-toggle:hover {
      background: #f2f4f7;
    }
    .content {
      padding: 14px;
      overflow: auto;
      max-height: calc(100vh - 216px);
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
    .mapper-list {
      display: grid;
      gap: 10px;
    }
    .mapper-item {
      border: 1px solid var(--line);
      border-radius: 8px;
      background: #fff;
      padding: 12px;
    }
    .mapper-title {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      align-items: center;
      font-family: Consolas, "Courier New", monospace;
      font-size: 13px;
      word-break: break-all;
    }
    .mapper-section {
      margin: 10px 0 6px;
      color: var(--muted);
      font-size: 12px;
      font-weight: 700;
    }
    @media (max-width: 980px) {
      main { grid-template-columns: 1fr; }
      .config { position: static; max-height: none; }
      .content { max-height: none; }
    }
    @media (max-width: 1280px) {
      .preview-grid { grid-template-columns: 1fr; }
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

      <label for="implTargets">实现类方法，每行一个；留空则扫描全部 @Service *Impl 无引用方法</label>
      <textarea id="implTargets" class="compact" spellcheck="false"></textarea>

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

      <div class="action-groups">
        <div class="action-group">
          <div class="action-title">接口管理</div>
          <div class="action-note">按输入的接口路径生成调用链预览，并仅对接口调用链执行删除。</div>
          <div class="actions">
            <button class="primary" id="runBtn">分析接口</button>
            <button class="danger" id="deleteApiBtn">删除接口</button>
          </div>
        </div>
        <div class="action-group">
          <div class="action-title">Mapper管理</div>
          <div class="action-note">扫描无引用 MyBatis Mapper，并仅对 Mapper 候选执行删除。</div>
          <div class="actions">
            <button class="secondary" id="mapperBtn">分析 Mapper</button>
            <button class="danger" id="deleteMapperBtn">删除 Mapper</button>
          </div>
        </div>
        <div class="action-group">
          <div class="action-title">实现类管理</div>
          <div class="action-note">可指定 @Service *Impl 方法；留空时扫描所有没有真实上游调用的实现类方法。</div>
          <div class="actions">
            <button class="secondary" id="implBtn">分析实现类无用方法</button>
            <button class="danger" id="deleteImplBtn">删除实现类无用方法</button>
          </div>
        </div>
        <div class="utility-actions">
          <button class="secondary" id="clearBtn">Clear</button>
        </div>
      </div>
    </section>

    <section class="preview">
      <div class="status">
        <div id="statusText">Waiting</div>
        <div class="summary" id="summary"></div>
      </div>
      <div class="preview-grid" id="previewRoot">
        <article class="preview-panel" data-preview-panel="api">
          <div class="preview-panel-head">
            <div class="preview-panel-heading">
              <div class="preview-panel-title">接口预览</div>
              <div class="preview-panel-sub">对应左侧“接口管理”的分析与删除结果。</div>
            </div>
            <div class="preview-panel-tools">
              <button type="button" class="preview-toggle" data-preview-toggle="api" aria-expanded="false">放大</button>
              <div class="summary" id="apiSummary"></div>
            </div>
          </div>
          <div class="content" id="apiPreview">
            <div class="empty">输入接口路径后点击“分析接口”。</div>
          </div>
        </article>
        <article class="preview-panel" data-preview-panel="mapper">
          <div class="preview-panel-head">
            <div class="preview-panel-heading">
              <div class="preview-panel-title">Mapper预览</div>
              <div class="preview-panel-sub">对应左侧“Mapper管理”的分析与删除结果。</div>
            </div>
            <div class="preview-panel-tools">
              <button type="button" class="preview-toggle" data-preview-toggle="mapper" aria-expanded="false">放大</button>
              <div class="summary" id="mapperSummary"></div>
            </div>
          </div>
          <div class="content" id="mapperPreview">
            <div class="empty">点击“分析 Mapper”查看无引用 Mapper。</div>
          </div>
        </article>
        <article class="preview-panel" data-preview-panel="impl">
          <div class="preview-panel-head">
            <div class="preview-panel-heading">
              <div class="preview-panel-title">实现类预览</div>
              <div class="preview-panel-sub">对应左侧“实现类管理”的分析与删除结果。</div>
            </div>
            <div class="preview-panel-tools">
              <button type="button" class="preview-toggle" data-preview-toggle="impl" aria-expanded="false">放大</button>
              <div class="summary" id="implSummary"></div>
            </div>
          </div>
          <div class="content" id="implPreview">
            <div class="empty">输入实现类方法或留空扫描全部，然后点击“分析实现类无用方法”。</div>
          </div>
        </article>
      </div>
    </section>
  </main>
  <script>
    const $ = (id) => document.getElementById(id);
    const runBtn = $("runBtn");
    const mapperBtn = $("mapperBtn");
    const implBtn = $("implBtn");
    const deleteApiBtn = $("deleteApiBtn");
    const deleteMapperBtn = $("deleteMapperBtn");
    const deleteImplBtn = $("deleteImplBtn");
    const clearBtn = $("clearBtn");
    const previewRoot = $("previewRoot");
    const apiPreview = $("apiPreview");
    const mapperPreview = $("mapperPreview");
    const implPreview = $("implPreview");
    const statusText = $("statusText");
    const summary = $("summary");
    const apiSummary = $("apiSummary");
    const mapperSummary = $("mapperSummary");
    const implSummary = $("implSummary");
    let apiReport = null;
    let mapperReport = null;
    let implReport = null;
    let currentLimit = 1200;
    let expandedPreview = null;

    clearBtn.addEventListener("click", () => {
      apiPreview.innerHTML = '<div class="empty">输入接口路径后点击“分析接口”。</div>';
      mapperPreview.innerHTML = '<div class="empty">点击“分析 Mapper”查看无引用 Mapper。</div>';
      implPreview.innerHTML = '<div class="empty">输入实现类方法或留空扫描全部，然后点击“分析实现类无用方法”。</div>';
      statusText.textContent = "Waiting";
      summary.innerHTML = "";
      apiSummary.innerHTML = "";
      mapperSummary.innerHTML = "";
      implSummary.innerHTML = "";
      apiReport = null;
      mapperReport = null;
      implReport = null;
    });

    runBtn.addEventListener("click", async () => {
      await submit("/api/analyze", "api");
    });

    mapperBtn.addEventListener("click", async () => {
      await submit("/api/analyze", "mapper");
    });

    implBtn.addEventListener("click", async () => {
      await submit("/api/analyze", "impl");
    });

    deleteApiBtn.addEventListener("click", async () => {
      const ok = window.confirm("Confirm deletion of removable API call-chain methods? This will modify local Java/XML files.");
      if (!ok) {
        return;
      }
      await submit("/api/delete", "api");
    });

    deleteMapperBtn.addEventListener("click", async () => {
      const ok = window.confirm("Confirm deletion of unused mapper methods? This will modify local Java/XML files.");
      if (!ok) {
        return;
      }
      await submit("/api/delete", "mapper");
    });

    deleteImplBtn.addEventListener("click", async () => {
      const ok = window.confirm("Confirm deletion of removable implementation-method chain? This will modify local Java/XML files.");
      if (!ok) {
        return;
      }
      await submit("/api/delete", "impl");
    });

    async function submit(endpoint, target) {
      const deleting = endpoint === "/api/delete";
      const paths = $("paths").value.split(/\r?\n/).map(v => v.trim()).filter(v => v && !v.startsWith("#"));
      const implTargets = $("implTargets").value.split(/\r?\n/).map(v => v.trim()).filter(v => v && !v.startsWith("#"));
      const payload = {
        paths: target === "impl" ? implTargets : paths,
        root: $("root").value.trim(),
        maxDepth: Number($("maxDepth").value || 80),
        limit: Number($("limit").value || 1200),
        report: $("report").value.trim(),
        includeTests: $("includeTests").checked,
        exactRouteMatch: $("exactRoute").checked,
        deleteXml: $("deleteXml").checked,
        writeReport: $("writeReport").checked,
        mapperOnly: target === "mapper",
        implOnly: target === "impl"
      };
      if (target === "api" && !paths.length) {
        apiPreview.innerHTML = '<div class="error">Enter at least one API path.</div>';
        return;
      }
      setButtonsDisabled(true);
      statusText.textContent = (deleting ? "Deleting " : "Analyzing ") + targetLabel(target) + "...";
      summary.innerHTML = "";
      setPanelLoading(target, (deleting ? "Deleting" : "Analyzing") + '. Large repositories may take tens of seconds.');
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
        currentLimit = payload.limit;
        if (target === "mapper") {
          mapperReport = data.report;
        } else if (target === "impl") {
          implReport = data.report;
        } else {
          apiReport = data.report;
        }
        render(data.report, currentLimit, target);
        statusText.textContent = targetLabel(target) + (deleting ? " deletion completed" : " preview generated");
      } catch (err) {
        statusText.textContent = "Failed";
        setPanelError(target, String(err.message || err));
      } finally {
        setButtonsDisabled(false);
      }
    }

    function setButtonsDisabled(disabled) {
      runBtn.disabled = disabled;
      mapperBtn.disabled = disabled;
      implBtn.disabled = disabled;
      deleteApiBtn.disabled = disabled;
      deleteMapperBtn.disabled = disabled;
      deleteImplBtn.disabled = disabled;
    }

    function setPanelLoading(target, message) {
      const panel = targetPanel(target);
      panel.innerHTML = '<div class="empty">' + escapeHtml(message) + '</div>';
    }

    function setPanelError(target, message) {
      const panel = targetPanel(target);
      panel.innerHTML = '<div class="error">' + escapeHtml(message) + '</div>';
    }

    function render(report, limit, target) {
      const s = report.summary || {};
      summary.innerHTML = [
        pill("接口", (report.routes || []).length),
        pill("实现类", s.implMethods || 0),
        pill("可达方法", s.reachableMethods || 0),
        pill("待删除", s.removableMethods || 0),
        pill("保留边界", s.retainedBoundaryMethods || 0),
        pill("无引用Mapper", s.unusedMapperMethods || 0),
        pill("风险保留Mapper", s.retainedMapperMethods || 0)
      ].join("");
      if (target === "mapper") {
        mapperSummary.innerHTML = mapperPills(report);
        mapperPreview.innerHTML = renderMapperTab(report, limit);
        return;
      }
      if (target === "impl") {
        implSummary.innerHTML = implPills(report);
        implPreview.innerHTML = renderImplTab(report, limit);
        return;
      }
      apiSummary.innerHTML = apiPills(report);
      apiPreview.innerHTML = renderApiTab(report, limit);
    }

    function targetPanel(target) {
      if (target === "mapper") {
        return mapperPreview;
      }
      if (target === "impl") {
        return implPreview;
      }
      return apiPreview;
    }

    function targetLabel(target) {
      if (target === "mapper") {
        return "Mapper";
      }
      if (target === "impl") {
        return "Implementation";
      }
      return "API";
    }

    function renderApiTab(report, limit) {
      const parts = [];
      if ((report.unresolvedPaths || []).length) {
        parts.push(renderUnresolved(report));
      }
      const trees = report.apiTrees || [];
      if (!trees.length && !parts.length) {
        return '<div class="empty">没有匹配到接口。</div>';
      }
      trees.forEach((api, index) => {
        parts.push(renderApi(api, index, limit));
      });
      return parts.join("");
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

    function renderImplTab(report, limit) {
      const parts = [];
      if ((report.unresolvedImplMethods || []).length) {
        parts.push(renderUnresolvedImplMethods(report));
      }
      const trees = report.implTrees || [];
      if (!trees.length && !parts.length) {
        return report.implScanAll
          ? '<div class="empty">没有识别到无真实上游调用的 @Service *Impl 方法。</div>'
          : '<div class="empty">没有匹配到符合 @Service 且类名以 Impl 结尾的实现类方法。</div>';
      }
      if (report.implScanAll) {
        parts.push('<div class="mapper-section">全量扫描结果：以下实现类方法没有真实上游调用，删除前请重点核对反射、定时任务配置和外部框架调用。</div>');
      }
      trees.forEach((item, index) => {
        parts.push(renderImplTree(item, index, limit));
      });
      return parts.join("");
    }

    function renderUnresolvedImplMethods(report) {
      return (report.unresolvedImplMethods || []).map(target =>
        '<div class="unresolved"><h3>未识别实现类方法：' + escapeHtml(target) + '</h3>' +
        '<div>请使用完整类名加方法名，例如 <code>com.example.FooServiceImpl#bar</code>。目标类必须带有 @Service 注解且类名以 Impl 结尾。</div>' +
        '</div>'
      ).join("");
    }

    function renderImplTree(item, index, limit) {
      const tree = item.tree;
      const title = item.requestedTarget || ((item.root && item.root.method) || "");
      return '<article class="api">' +
        '<div class="api-head">' +
        '<div class="api-title">' + escapeHtml(title) + '</div>' +
        '<div class="api-meta">根方法：' + escapeHtml((item.root && item.root.method) || "") + '</div>' +
        '</div>' +
        '<div class="tree">' + renderNode(tree, "impl" + index, 0, {count: 0, limit}) + '</div>' +
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

    function renderMapperTab(report, limit) {
      const unused = report.unusedMappers || [];
      const retained = report.retainedMappers || [];
      const files = report.unusedMapperFiles || [];
      const parts = [];
      if (!unused.length && !retained.length && !files.length) {
        return '<div class="empty">没有识别到可删除的无引用 Mapper。</div>';
      }
      if (unused.length) {
        parts.push('<div class="mapper-section">待删除 Mapper 方法</div>');
        parts.push('<div class="mapper-list">' + unused.slice(0, limit).map(renderMapperItem).join("") + '</div>');
        if (unused.length > limit) {
          parts.push('<div class="file">已达到预览上限，剩余 ' + escapeHtml(String(unused.length - limit)) + ' 个未展示。</div>');
        }
      }
      if (files.length) {
        parts.push('<div class="mapper-section">整文件候选（仅当全部方法无引用且无类级引用）</div>');
        parts.push('<div class="mapper-list">' + files.slice(0, limit).map(path =>
          '<div class="mapper-item"><span class="badge delete">候选</span> <span class="file">' + escapeHtml(path) + '</span></div>'
        ).join("") + '</div>');
      }
      if (retained.length) {
        parts.push('<div class="mapper-section">风险保留 Mapper 方法</div>');
        parts.push('<div class="mapper-list">' + retained.slice(0, limit).map(renderRetainedMapperItem).join("") + '</div>');
      }
      return parts.join("");
    }

    function renderMapperItem(item) {
      const xml = (item.xmlStatements || []).map(s =>
        '<div class="file">XML: ' + escapeHtml(s.file + ":" + s.line + " #" + s.id) + '</div>'
      ).join("");
      return '<div class="mapper-item">' +
        '<div class="mapper-title"><span class="badge delete">待删除</span><span>' + escapeHtml(item.method || "") + '</span></div>' +
        '<div class="file">' + escapeHtml((item.file || "") + ":" + (item.line || "")) + '</div>' +
        xml +
        '</div>';
    }

    function renderRetainedMapperItem(item) {
      const reasons = (item.reasons || []).slice(0, 6).map(r => '<div>' + escapeHtml(r) + '</div>').join("");
      return '<div class="mapper-item">' +
        '<div class="mapper-title"><span class="badge retain">保留</span><span>' + escapeHtml(item.method || "") + '</span></div>' +
        '<div class="file">' + escapeHtml((item.file || "") + ":" + (item.line || "")) + '</div>' +
        (reasons ? '<div class="reasons">' + reasons + '</div>' : '') +
        '</div>';
    }

    previewRoot.addEventListener("click", (event) => {
      const previewToggle = event.target.closest(".preview-toggle");
      if (previewToggle) {
        togglePreviewPanel(previewToggle.dataset.previewToggle);
        return;
      }
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

    function togglePreviewPanel(target) {
      if (!target) {
        return;
      }
      expandedPreview = expandedPreview === target ? null : target;
      previewRoot.classList.toggle("is-expanded", Boolean(expandedPreview));
      previewRoot.querySelectorAll("[data-preview-panel]").forEach(panel => {
        const active = panel.dataset.previewPanel === expandedPreview;
        panel.classList.toggle("is-expanded", active);
      });
      previewRoot.querySelectorAll("[data-preview-toggle]").forEach(button => {
        const active = button.dataset.previewToggle === expandedPreview;
        button.textContent = active ? "还原" : "放大";
        button.setAttribute("aria-expanded", String(active));
      });
    }

    function pill(name, value) {
      return '<span class="pill">' + escapeHtml(name) + ': ' + escapeHtml(String(value)) + '</span>';
    }

    function apiPills(report) {
      const s = report.summary || {};
      return [
        pill("接口", (report.routes || []).length),
        pill("可达方法", s.reachableMethods || 0),
        pill("待删除", s.removableMethods || 0),
        pill("保留边界", s.retainedBoundaryMethods || 0)
      ].join("");
    }

    function mapperPills(report) {
      const s = report.summary || {};
      return [
        pill("待删除", s.unusedMapperMethods || 0),
        pill("保留", s.retainedMapperMethods || 0),
        pill("整文件候选", s.unusedMapperFiles || 0)
      ].join("");
    }

    function implPills(report) {
      const s = report.summary || {};
      return [
        pill("实现类", s.implMethods || 0),
        pill("可达方法", s.reachableMethods || 0),
        pill("待删除", s.removableMethods || 0),
        pill("保留边界", s.retainedBoundaryMethods || 0),
        pill("未识别", s.unresolvedImplMethods || 0)
      ].join("");
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
        mapper_only = bool(payload.get("mapperOnly", False))
        impl_only = bool(payload.get("implOnly", False))
        options = CleanupRunOptions(
            root=root,
            include_tests=bool(payload.get("includeTests", False)),
            exact_route_match=bool(payload.get("exactRouteMatch", False)),
            max_depth=max_depth,
            delete_xml=bool(payload.get("deleteXml", True)),
            mapper_only=mapper_only,
            impl_only=impl_only,
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
