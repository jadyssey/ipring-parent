#!/usr/bin/env python3
"""
Web UI for api_migrator.py.

Start:

  python -B scripts/api_migrator_web.py --host 127.0.0.1 --port 8766

Open:

  http://127.0.0.1:8766

The UI is dependency-free and runs as a local management page. It reuses
ApiMigrationPlanner and ApiMigrator from api_migrator.py.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import traceback
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse


SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from api_migrator import (  # noqa: E402
    ApiMigrationPlanner,
    ApiMigrator,
    MigrationOptions,
    MigrationPlan,
    MigrationResult,
    PlannedFile,
    normalize_package,
    read_text,
    relpath,
    report_dict,
)


DEFAULT_ROOT = SCRIPT_DIR.parent.resolve()
PLAN_CACHE_MAX = 12


@dataclass
class CachedPlan:
    plan_id: str
    options: MigrationOptions
    plan: MigrationPlan
    created_at: float
    env_vars: Dict[str, str]
    extra_params: Dict[str, str]
    result: Optional[MigrationResult] = None


PLAN_CACHE: Dict[str, CachedPlan] = {}


INDEX_HTML = r"""<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>API Migration Console</title>
  <style>
    :root {
      color-scheme: light;
      --paper: #f4efe4;
      --paper-2: #fffaf0;
      --ink: #1b2a2a;
      --muted: #68706b;
      --line: #d8cdbb;
      --panel: rgba(255, 250, 240, 0.92);
      --teal: #006d6f;
      --teal-dark: #02484a;
      --rust: #bf5b22;
      --rust-dark: #823a18;
      --gold: #dcae48;
      --code-bg: #101818;
      --code-fg: #d6e7df;
      --ok: #147a4b;
      --warn: #a05a00;
      --bad: #a03224;
      --shadow: 0 18px 60px rgba(26, 35, 32, 0.14);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      color: var(--ink);
      font-family: Aptos, Candara, "Trebuchet MS", sans-serif;
      background:
        radial-gradient(circle at 8% 0%, rgba(220, 174, 72, 0.28), transparent 26rem),
        radial-gradient(circle at 88% 18%, rgba(0, 109, 111, 0.22), transparent 30rem),
        linear-gradient(135deg, #f4efe4 0%, #f8f0de 46%, #e9f0ea 100%);
    }
    button, input, textarea, select { font: inherit; }
    button {
      border: 0;
      border-radius: 12px;
      padding: 10px 14px;
      cursor: pointer;
      color: #fff;
      background: var(--teal);
      box-shadow: 0 10px 24px rgba(0, 109, 111, 0.18);
      transition: transform 140ms ease, box-shadow 140ms ease, background 140ms ease;
    }
    button:hover { transform: translateY(-1px); background: var(--teal-dark); box-shadow: 0 14px 30px rgba(0, 109, 111, 0.22); }
    button.secondary { background: #efe3cd; color: var(--ink); box-shadow: none; border: 1px solid var(--line); }
    button.secondary:hover { background: #e4d3b8; }
    button.danger { background: var(--rust); }
    button.danger:hover { background: var(--rust-dark); }
    button.ghost { background: transparent; color: var(--teal); border: 1px solid rgba(0, 109, 111, 0.28); box-shadow: none; }
    button.small { padding: 7px 10px; border-radius: 9px; font-size: 12px; }
    header {
      padding: 24px 28px 10px;
    }
    .hero {
      position: relative;
      overflow: hidden;
      border: 1px solid rgba(27, 42, 42, 0.12);
      border-radius: 28px;
      padding: 24px;
      min-height: 132px;
      color: #fff;
      background:
        linear-gradient(135deg, rgba(2, 72, 74, 0.98), rgba(0, 109, 111, 0.88)),
        repeating-linear-gradient(45deg, transparent 0 12px, rgba(255,255,255,.04) 12px 13px);
      box-shadow: var(--shadow);
    }
    .hero:after {
      content: "";
      position: absolute;
      right: -80px;
      top: -80px;
      width: 260px;
      height: 260px;
      border-radius: 42%;
      background: rgba(220, 174, 72, 0.28);
      transform: rotate(18deg);
    }
    .hero h1 {
      position: relative;
      z-index: 1;
      margin: 0;
      font-family: Georgia, "Times New Roman", serif;
      font-size: clamp(30px, 4vw, 54px);
      line-height: 0.94;
      letter-spacing: -0.04em;
    }
    .hero p {
      position: relative;
      z-index: 1;
      max-width: 880px;
      margin: 12px 0 0;
      color: rgba(255, 255, 255, 0.82);
      line-height: 1.5;
    }
    .app {
      display: grid;
      grid-template-columns: minmax(360px, 440px) minmax(0, 1fr);
      gap: 18px;
      padding: 18px 28px 28px;
    }
    .panel {
      background: var(--panel);
      border: 1px solid rgba(27, 42, 42, 0.12);
      border-radius: 22px;
      box-shadow: var(--shadow);
      backdrop-filter: blur(10px);
      min-width: 0;
    }
    .config {
      align-self: start;
      position: sticky;
      top: 16px;
      max-height: calc(100vh - 32px);
      overflow: auto;
      padding: 18px;
    }
    .workspace {
      min-height: 78vh;
      padding: 12px;
      display: grid;
      gap: 10px;
    }
    h2, h3 {
      margin: 0;
      letter-spacing: -0.02em;
    }
    h2 { font-size: 19px; }
    h3 { font-size: 14px; color: var(--teal-dark); text-transform: uppercase; letter-spacing: 0.08em; }
    label {
      display: block;
      margin: 12px 0 6px;
      color: var(--muted);
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    input[type="text"], input[type="number"], textarea, select {
      width: 100%;
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 10px 11px;
      color: var(--ink);
      background: rgba(255, 255, 255, 0.74);
      outline: none;
    }
    input:focus, textarea:focus {
      border-color: rgba(0, 109, 111, 0.6);
      box-shadow: 0 0 0 4px rgba(0, 109, 111, 0.1);
    }
    textarea {
      min-height: 120px;
      resize: vertical;
      font-family: "Cascadia Mono", "JetBrains Mono", Consolas, monospace;
      font-size: 12px;
    }
    .section {
      border: 1px solid rgba(216, 205, 187, 0.8);
      border-radius: 18px;
      padding: 14px;
      margin-top: 12px;
      background: rgba(255, 255, 255, 0.44);
    }
    .grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
    }
    .row {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      gap: 8px;
      align-items: end;
      margin-top: 8px;
    }
    .row.map, .row.env {
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
    }
    .checks {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px 10px;
      margin-top: 8px;
    }
    .check {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--ink);
      font-size: 13px;
      min-height: 28px;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 14px;
    }
    .status {
      min-height: 22px;
      color: var(--muted);
      font-size: 13px;
      line-height: 1.5;
      margin-top: 12px;
    }
    .status.bad { color: var(--bad); }
    .status.ok { color: var(--ok); }
    .summary {
      display: grid;
      grid-template-columns: repeat(6, minmax(66px, 1fr));
      gap: 6px;
      align-items: stretch;
    }
    .metric {
      border-radius: 12px;
      padding: 8px 10px;
      color: #fff;
      background: linear-gradient(135deg, #1b2a2a, #006d6f);
      min-height: 48px;
      position: relative;
      overflow: hidden;
      display: grid;
      grid-template-columns: auto 1fr;
      align-items: baseline;
      gap: 8px;
    }
    .metric:nth-child(2) { background: linear-gradient(135deg, #3a2a1f, #bf5b22); }
    .metric:nth-child(3) { background: linear-gradient(135deg, #3b351d, #dcae48); color: #1b2a2a; }
    .metric:nth-child(4) { background: linear-gradient(135deg, #243331, #147a4b); }
    .metric:nth-child(5) { background: linear-gradient(135deg, #321f1b, #a03224); }
    .metric:nth-child(6) { background: linear-gradient(135deg, #263444, #5e6f7d); }
    .metric .num {
      font-family: Georgia, "Times New Roman", serif;
      font-size: 22px;
      line-height: 1;
      letter-spacing: -0.04em;
    }
    .metric .label {
      margin-top: 0;
      font-size: 11px;
      opacity: 0.82;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .project-strip {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      max-height: 30px;
      overflow: hidden;
    }
    .pill {
      border: 1px solid rgba(0, 109, 111, 0.18);
      background: rgba(255, 255, 255, 0.5);
      border-radius: 999px;
      padding: 4px 8px;
      font-size: 11px;
      color: var(--teal-dark);
      line-height: 1.2;
    }
    .main-grid {
      display: grid;
      grid-template-columns: minmax(220px, 300px) minmax(0, 1fr);
      gap: 10px;
      min-height: calc(100vh - 260px);
    }
    .tree-panel, .detail-panel {
      border: 1px solid rgba(216, 205, 187, 0.82);
      border-radius: 20px;
      background: rgba(255, 255, 255, 0.56);
      overflow: hidden;
      min-width: 0;
    }
    .panel-head {
      padding: 9px 12px;
      border-bottom: 1px solid rgba(216, 205, 187, 0.82);
      background: rgba(255, 250, 240, 0.82);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 10px;
    }
    .tree {
      padding: 8px;
      overflow: auto;
      height: calc(100vh - 330px);
      min-height: 560px;
      font-size: 12px;
    }
    details { margin-left: 10px; }
    summary {
      cursor: pointer;
      padding: 5px 4px;
      color: var(--teal-dark);
      user-select: none;
    }
    .file-btn {
      width: 100%;
      text-align: left;
      padding: 7px 8px;
      margin: 2px 0;
      border-radius: 10px;
      border: 0;
      background: transparent;
      color: var(--ink);
      box-shadow: none;
      display: flex;
      justify-content: space-between;
      gap: 8px;
      align-items: center;
    }
    .file-btn:hover, .file-btn.active {
      background: rgba(0, 109, 111, 0.1);
      color: var(--teal-dark);
      transform: none;
      box-shadow: none;
    }
    .badge {
      border-radius: 999px;
      padding: 3px 7px;
      font-size: 11px;
      white-space: nowrap;
      color: #fff;
      background: var(--teal);
    }
    .badge.resource { background: var(--rust); }
    .badge.conflict { background: var(--bad); }
    .badge.skipped-conflict { background: var(--warn); }
    .badge.skipped-existing-model { background: #5e6f7d; }
    .badge.append { background: var(--gold); color: var(--ink); }
    .badge.overwrite { background: var(--rust); }
    .badge.same { background: var(--ok); }
    .badge.new { background: var(--teal); }
    .file-meta {
      padding: 8px 12px;
      color: var(--muted);
      font-size: 11px;
      line-height: 1.45;
      border-bottom: 1px solid rgba(216, 205, 187, 0.82);
    }
    .code-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      min-height: calc(100vh - 390px);
    }
    .code-pane {
      min-width: 0;
      border-right: 1px solid rgba(216, 205, 187, 0.82);
      display: grid;
      grid-template-rows: auto minmax(0, 1fr);
    }
    .code-pane:last-child { border-right: 0; }
    .code-title {
      padding: 8px 10px;
      color: var(--code-fg);
      background: #182323;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      font-size: 12px;
      display: flex;
      justify-content: space-between;
      gap: 8px;
    }
    pre.code {
      margin: 0;
      padding: 12px;
      overflow: auto;
      color: var(--code-fg);
      background:
        linear-gradient(90deg, rgba(220,174,72,.08) 0 1px, transparent 1px 100%),
        var(--code-bg);
      background-size: 44px 100%;
      font-family: "Cascadia Mono", "JetBrains Mono", Consolas, monospace;
      font-size: 12.5px;
      line-height: 1.62;
      white-space: pre;
      tab-size: 2;
      height: calc(100vh - 430px);
      min-height: 620px;
      max-height: none;
    }
    .tok-key { color: #ffd166; }
    .tok-str { color: #95d5b2; }
    .tok-num { color: #90dbf4; }
    .tok-ann { color: #ffb703; }
    .tok-com { color: #7a8a85; font-style: italic; }
    .tok-tag { color: #8ecae6; }
    .tok-attr { color: #f4a261; }
    .empty {
      padding: 36px;
      border: 1px dashed var(--line);
      border-radius: 18px;
      color: var(--muted);
      background: rgba(255, 255, 255, 0.42);
      line-height: 1.7;
    }
    .config-json {
      margin-top: 10px;
      display: none;
    }
    .config-json.open { display: block; }
    @media (max-width: 1180px) {
      .summary { grid-template-columns: repeat(6, minmax(70px, 1fr)); }
      .main-grid { grid-template-columns: 1fr; }
      .tree { height: 260px; min-height: 260px; }
    }
    @media (max-width: 940px) {
      header { padding: 14px; }
      .app { grid-template-columns: 1fr; padding: 14px; }
      .config { position: static; max-height: none; }
      .code-grid { grid-template-columns: 1fr; }
      .code-pane { border-right: 0; border-bottom: 1px solid rgba(216, 205, 187, 0.82); }
    }
    @media (max-width: 620px) {
      .summary { grid-template-columns: 1fr 1fr 1fr; }
      .grid-2, .checks { grid-template-columns: 1fr; }
      .row.map, .row.env { grid-template-columns: 1fr; }
      .row { grid-template-columns: 1fr; }
      .hero { border-radius: 20px; }
    }
  </style>
</head>
<body>
  <header>
    <div class="hero">
      <h1>API Migration Console</h1>
      <p>集中配置迁移参数，预览接口依赖闭包、目标目录结构和代码变更。默认只生成计划，执行迁移需要单独点击。</p>
    </div>
  </header>

  <main class="app">
    <aside class="panel config">
      <h2>配置中心</h2>

      <div class="section">
        <h3>路径</h3>
        <label>源项目路径</label>
        <input id="sourceRoot" type="text" value="." placeholder="D:\git\project-a">
        <label>目标项目路径</label>
        <input id="targetRoot" type="text" placeholder="D:\git\project-b">
        <div class="grid-2">
          <div>
            <label>目标 Java 目录</label>
            <input id="targetJavaDir" type="text" placeholder="可选，默认 target/src/main/java">
          </div>
          <div>
            <label>目标资源目录</label>
            <input id="targetResourceDir" type="text" placeholder="可选，默认 target/src/main/resources">
          </div>
        </div>
      </div>

      <div class="section">
        <h3>接口路径</h3>
        <div id="pathsList"></div>
        <div class="actions"><button class="secondary small" type="button" onclick="addPathRow('')">新增接口</button></div>
      </div>

      <div class="section">
        <h3>包名与目录</h3>
        <div class="status">迁移结果严格保留原始 package 与目录层级；包名映射已禁用，避免破坏依赖关系。</div>
      </div>

      <div class="section">
        <h3>环境变量</h3>
        <div id="envList"></div>
        <div class="actions"><button class="secondary small" type="button" onclick="addEnvRow('', '')">新增变量</button></div>
      </div>

      <div class="section">
        <h3>运行参数</h3>
        <div class="grid-2">
          <div>
            <label>最大调用深度</label>
            <input id="maxDepth" type="number" min="1" value="80">
          </div>
          <div>
            <label>报告路径</label>
            <input id="report" type="text" value="api_migration_report.json">
          </div>
        </div>
        <div class="checks">
          <label class="check"><input id="includeResources" type="checkbox" checked> 复制资源/XML</label>
          <label class="check"><input id="methodSlice" type="checkbox" checked disabled> 方法级最小集</label>
          <label class="check"><input id="includeTests" type="checkbox"> 包含测试代码</label>
          <label class="check"><input id="exactRouteMatch" type="checkbox"> 精确路由匹配</label>
          <label class="check"><input id="updateTargetRefs" type="checkbox"> 更新目标引用</label>
          <label class="check"><input id="skipConflicts" type="checkbox" checked> 冲突时跳过类</label>
          <label class="check"><input id="overwrite" type="checkbox"> 允许覆盖冲突</label>
          <label class="check"><input id="writeReport" type="checkbox"> 写入报告文件</label>
        </div>
        <label>扩展运行参数</label>
        <div id="extraParamList"></div>
        <div class="actions"><button class="secondary small" type="button" onclick="addExtraParamRow('', '')">新增参数</button></div>
      </div>

      <div class="actions">
        <button type="button" onclick="runPlan()">预览计划</button>
        <button class="danger" type="button" onclick="runMigrate()">执行迁移</button>
        <button class="ghost" type="button" onclick="toggleConfigJson()">JSON</button>
      </div>
      <div id="status" class="status">等待配置。</div>
      <div id="configJsonBox" class="config-json">
        <label>配置 JSON</label>
        <textarea id="configJson" spellcheck="false"></textarea>
        <div class="actions">
          <button class="secondary small" type="button" onclick="exportConfig()">导出当前配置</button>
          <button class="secondary small" type="button" onclick="importConfig()">应用 JSON 配置</button>
        </div>
      </div>
    </aside>

    <section class="panel workspace">
      <div id="emptyState" class="empty">
        先填写源路径、目标路径和接口路径，然后点击“预览计划”。计划会展示待迁移项目分布、目录树、每个文件的源代码和目标重写预览。
      </div>
      <div id="resultView" style="display:none;">
        <div class="summary" id="summary"></div>
        <div class="project-strip" id="projects"></div>
        <div class="main-grid">
          <div class="tree-panel">
            <div class="panel-head">
              <h2>目录结构</h2>
              <select id="treeMode" onchange="renderTree()">
                <option value="target">目标路径</option>
                <option value="source">源路径</option>
              </select>
            </div>
            <div id="fileTree" class="tree"></div>
          </div>
          <div class="detail-panel">
            <div class="panel-head">
              <h2>代码预览</h2>
              <button class="secondary small" type="button" onclick="copySelectedPath()">复制路径</button>
            </div>
            <div id="fileMeta" class="file-meta">选择目录树中的文件。</div>
            <div class="code-grid">
              <div class="code-pane">
                <div class="code-title"><span>源代码</span><span id="sourceInfo"></span></div>
                <pre class="code"><code id="sourceCode"></code></pre>
              </div>
              <div class="code-pane">
                <div class="code-title"><span>目标预览</span><span id="targetInfo"></span></div>
                <pre class="code"><code id="targetCode"></code></pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>

  <script>
    const state = { plan: null, selectedIndex: null };

    function init() {
      addPathRow('/app/transport/serviceInfo');
      addEnvRow('', '');
      addExtraParamRow('', '');
      exportConfig();
    }

    function rowButton(label, onclick) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'secondary small';
      btn.textContent = label;
      btn.onclick = onclick;
      return btn;
    }

    function addPathRow(value) {
      const wrap = document.createElement('div');
      wrap.className = 'row';
      wrap.innerHTML = '<input type="text" class="api-path" placeholder="/api/path">';
      wrap.querySelector('input').value = value || '';
      wrap.appendChild(rowButton('删除', () => wrap.remove()));
      document.getElementById('pathsList').appendChild(wrap);
    }

    function addEnvRow(key, value) {
      const wrap = document.createElement('div');
      wrap.className = 'row env';
      wrap.innerHTML = '<input type="text" class="env-key" placeholder="变量名"><input type="text" class="env-value" placeholder="变量值">';
      wrap.querySelector('.env-key').value = key || '';
      wrap.querySelector('.env-value').value = value || '';
      wrap.appendChild(rowButton('删除', () => wrap.remove()));
      document.getElementById('envList').appendChild(wrap);
    }

    function addExtraParamRow(key, value) {
      const wrap = document.createElement('div');
      wrap.className = 'row env';
      wrap.innerHTML = '<input type="text" class="extra-key" placeholder="参数名"><input type="text" class="extra-value" placeholder="参数值">';
      wrap.querySelector('.extra-key').value = key || '';
      wrap.querySelector('.extra-value').value = value || '';
      wrap.appendChild(rowButton('删除', () => wrap.remove()));
      document.getElementById('extraParamList').appendChild(wrap);
    }

    function values(selector) {
      return Array.from(document.querySelectorAll(selector)).map(el => el.value.trim()).filter(Boolean);
    }

    function keyValueRows(keySelector, valueSelector) {
      const keys = Array.from(document.querySelectorAll(keySelector));
      const vals = Array.from(document.querySelectorAll(valueSelector));
      const out = [];
      keys.forEach((keyEl, idx) => {
        const key = keyEl.value.trim();
        const value = (vals[idx] && vals[idx].value || '').trim();
        if (key || value) out.push({ key, value });
      });
      return out;
    }

    function collectConfig() {
      return {
        paths: values('.api-path'),
        sourceRoot: document.getElementById('sourceRoot').value.trim() || '.',
        targetRoot: document.getElementById('targetRoot').value.trim(),
        targetJavaDir: document.getElementById('targetJavaDir').value.trim(),
        targetResourceDir: document.getElementById('targetResourceDir').value.trim(),
        sourceBasePackage: '',
        targetBasePackage: '',
        packageMap: [],
        envVars: keyValueRows('.env-key', '.env-value'),
        extraParams: keyValueRows('.extra-key', '.extra-value'),
        runParams: {
          maxDepth: Number(document.getElementById('maxDepth').value || 80),
          includeResources: document.getElementById('includeResources').checked,
          wholeFileClosure: false,
          includeTests: document.getElementById('includeTests').checked,
          exactRouteMatch: document.getElementById('exactRouteMatch').checked,
          updateTargetRefs: document.getElementById('updateTargetRefs').checked,
          skipConflicts: document.getElementById('skipConflicts').checked,
          overwrite: document.getElementById('overwrite').checked,
          writeReport: document.getElementById('writeReport').checked,
          report: document.getElementById('report').value.trim()
        }
      };
    }

    function applyConfig(cfg) {
      document.getElementById('sourceRoot').value = cfg.sourceRoot || '.';
      document.getElementById('targetRoot').value = cfg.targetRoot || '';
      document.getElementById('targetJavaDir').value = cfg.targetJavaDir || '';
      document.getElementById('targetResourceDir').value = cfg.targetResourceDir || '';
      document.getElementById('pathsList').innerHTML = '';
      (cfg.paths || ['']).forEach(addPathRow);
      document.getElementById('envList').innerHTML = '';
      (cfg.envVars || [{key:'', value:''}]).forEach(row => addEnvRow(row.key || '', row.value || ''));
      document.getElementById('extraParamList').innerHTML = '';
      (cfg.extraParams || [{key:'', value:''}]).forEach(row => addExtraParamRow(row.key || '', row.value || ''));
      const run = cfg.runParams || {};
      document.getElementById('maxDepth').value = run.maxDepth || 80;
      document.getElementById('includeResources').checked = run.includeResources !== false;
      document.getElementById('includeTests').checked = !!run.includeTests;
      document.getElementById('exactRouteMatch').checked = !!run.exactRouteMatch;
      document.getElementById('updateTargetRefs').checked = !!run.updateTargetRefs;
      document.getElementById('skipConflicts').checked = run.skipConflicts !== false;
      document.getElementById('overwrite').checked = !!run.overwrite;
      document.getElementById('writeReport').checked = !!run.writeReport;
      document.getElementById('report').value = run.report || 'api_migration_report.json';
      exportConfig();
    }

    function setStatus(text, cls) {
      const el = document.getElementById('status');
      el.textContent = text;
      el.className = 'status ' + (cls || '');
    }

    async function postJson(url, payload) {
      const resp = await fetch(url, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
      });
      const data = await resp.json();
      if (!resp.ok || data.ok === false) {
        throw new Error(data.error || ('HTTP ' + resp.status));
      }
      return data;
    }

    async function runPlan() {
      const cfg = collectConfig();
      if (!cfg.paths.length) {
        setStatus('至少需要一个接口路径。', 'bad');
        return;
      }
      setStatus('正在分析接口依赖闭包...', '');
      try {
        const data = await postJson('/api/plan', cfg);
        state.plan = data;
        state.selectedIndex = null;
        renderPlan(data);
        exportConfig();
        setStatus('计划生成完成。', 'ok');
      } catch (err) {
        setStatus(err.message, 'bad');
      }
    }

    async function runMigrate() {
      if (!state.plan) {
        await runPlan();
      }
      if (!state.plan) return;
      if (!confirm('确认执行迁移？默认不会覆盖冲突文件；勾选“冲突时跳过类”后会跳过冲突类并继续。')) return;
      const cfg = collectConfig();
      setStatus('正在执行迁移...', '');
      try {
        const data = await postJson('/api/migrate', {
          planId: state.plan.planId,
          overwrite: cfg.runParams.overwrite,
          skipConflicts: cfg.runParams.skipConflicts,
          updateTargetRefs: cfg.runParams.updateTargetRefs,
          writeReport: cfg.runParams.writeReport,
          report: cfg.runParams.report
        });
        state.plan = data;
        renderPlan(data);
        const skipped = data.result && data.result.skippedConflicts ? data.result.skippedConflicts.length : 0;
        const conflicts = data.result && data.result.conflicts ? data.result.conflicts.length : 0;
        setStatus(skipped ? `迁移执行完成，已跳过 ${skipped} 个冲突项。` : '迁移执行完成。', conflicts ? 'bad' : 'ok');
      } catch (err) {
        setStatus(err.message, 'bad');
      }
    }

    function renderPlan(data) {
      document.getElementById('emptyState').style.display = 'none';
      document.getElementById('resultView').style.display = 'grid';
      const s = data.summary || {};
      const result = data.result || {};
      const conflicts = result.conflicts ? result.conflicts.length : s.conflictFiles || 0;
      const skipped = result.skippedConflicts ? result.skippedConflicts.length : s.skippedConflictFiles || 0;
      const metrics = [
        ['接口', s.routes || 0],
        ['方法', s.reachableMethods || 0],
        ['类', s.javaClasses || 0],
        ['资源', s.resourceFiles || 0],
        ['冲突', conflicts],
        ['跳过', skipped]
      ];
      document.getElementById('summary').innerHTML = metrics.map(([label, num]) => `
        <div class="metric"><div class="num">${num}</div><div class="label">${label}</div></div>
      `).join('');
      const projects = data.projects || [];
      const visibleProjects = projects.slice(0, 4);
      const hiddenProjects = projects.slice(4);
      const hiddenFiles = hiddenProjects.reduce((sum, p) => sum + (p.files || 0), 0);
      let projectHtml = visibleProjects.map(p =>
        `<span class="pill">${escapeHtml(p.name)} ${p.files}f/${p.classes}c</span>`
      ).join('');
      if (hiddenProjects.length) {
        projectHtml += `<span class="pill">+${hiddenProjects.length} more ${hiddenFiles}f</span>`;
      }
      document.getElementById('projects').innerHTML = projectHtml;
      renderTree();
      clearPreview();
    }

    function buildTree(files, mode) {
      const root = {};
      files.forEach(file => {
        const path = (mode === 'source' ? file.source : file.target).replaceAll('\\', '/');
        const parts = path.split('/').filter(Boolean);
        let node = root;
        parts.forEach((part, idx) => {
          node.children = node.children || {};
          node.children[part] = node.children[part] || { name: part, children: {}, file: null };
          node = node.children[part];
          if (idx === parts.length - 1) node.file = file;
        });
      });
      return root;
    }

    function renderTreeNode(node) {
      const names = Object.keys(node.children || {}).sort((a, b) => a.localeCompare(b));
      return names.map(name => {
        const child = node.children[name];
        if (child.file) {
          const f = child.file;
          const active = state.selectedIndex === f.index ? ' active' : '';
          return `<button class="file-btn${active}" onclick="loadFile(${f.index})"><span>${escapeHtml(name)}</span><span class="badge ${f.status} ${f.kind}">${escapeHtml(f.status)}</span></button>`;
        }
        return `<details open><summary>${escapeHtml(name)}</summary>${renderTreeNode(child)}</details>`;
      }).join('');
    }

    function renderTree() {
      if (!state.plan) return;
      const mode = document.getElementById('treeMode').value;
      const tree = buildTree(state.plan.files || [], mode);
      document.getElementById('fileTree').innerHTML = renderTreeNode(tree) || '<div class="empty">暂无文件。</div>';
    }

    function clearPreview() {
      document.getElementById('fileMeta').textContent = '选择目录树中的文件。';
      document.getElementById('sourceCode').textContent = '';
      document.getElementById('targetCode').textContent = '';
      document.getElementById('sourceInfo').textContent = '';
      document.getElementById('targetInfo').textContent = '';
    }

    async function loadFile(index) {
      if (!state.plan) return;
      state.selectedIndex = index;
      renderTree();
      setStatus('正在加载文件预览...', '');
      try {
        const data = await postJson('/api/file-preview', { planId: state.plan.planId, index });
        const f = data.file;
        const lang = f.kind === 'java' ? 'java' : 'xml';
        document.getElementById('fileMeta').innerHTML = `
          <strong>${escapeHtml(f.kind)}</strong> | 状态: ${escapeHtml(f.status)} | 源: ${escapeHtml(f.source)}<br>
          目标: ${escapeHtml(f.target)}
        `;
        document.getElementById('sourceInfo').textContent = `${data.sourceLines} 行`;
        document.getElementById('targetInfo').textContent = `${data.targetLines} 行`;
        document.getElementById('sourceCode').innerHTML = highlight(data.sourceCode || '', lang);
        document.getElementById('targetCode').innerHTML = highlight(data.targetCode || '', lang);
        setStatus('文件预览已加载。', 'ok');
      } catch (err) {
        setStatus(err.message, 'bad');
      }
    }

    function escapeHtml(text) {
      return String(text || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }

    function highlight(code, lang) {
      let html = escapeHtml(code);
      if (lang === 'xml') {
        html = html.replace(/(&lt;\/?[\w:.-]+)/g, '<span class="tok-tag">$1</span>');
        html = html.replace(/\s([\w:.-]+)=(&quot;.*?&quot;|&#039;.*?&#039;)/g, ' <span class="tok-attr">$1</span>=<span class="tok-str">$2</span>');
        return html;
      }
      html = html.replace(/(&quot;(?:\\.|[^&])*?&quot;|&#039;(?:\\.|[^&])*?&#039;)/g, '<span class="tok-str">$1</span>');
      html = html.replace(/(\/\/.*?$|\/\*[\s\S]*?\*\/)/gm, '<span class="tok-com">$1</span>');
      html = html.replace(/\b(public|private|protected|class|interface|enum|extends|implements|return|new|if|else|for|while|switch|case|break|continue|try|catch|finally|throw|throws|static|final|void|int|long|double|float|boolean|char|byte|short|import|package|this|super|null|true|false)\b/g, '<span class="tok-key">$1</span>');
      html = html.replace(/(@[A-Za-z_]\w*)/g, '<span class="tok-ann">$1</span>');
      html = html.replace(/\b(\d+(?:\.\d+)?)\b/g, '<span class="tok-num">$1</span>');
      return html;
    }

    function toggleConfigJson() {
      const box = document.getElementById('configJsonBox');
      box.classList.toggle('open');
      exportConfig();
    }

    function exportConfig() {
      document.getElementById('configJson').value = JSON.stringify(collectConfig(), null, 2);
    }

    function importConfig() {
      try {
        applyConfig(JSON.parse(document.getElementById('configJson').value));
        setStatus('JSON 配置已应用。', 'ok');
      } catch (err) {
        setStatus('JSON 解析失败: ' + err.message, 'bad');
      }
    }

    async function copySelectedPath() {
      if (!state.plan || state.selectedIndex === null) return;
      const file = (state.plan.files || []).find(f => f.index === state.selectedIndex);
      if (!file) return;
      await navigator.clipboard.writeText(file.target);
      setStatus('目标路径已复制。', 'ok');
    }

    init();
  </script>
</body>
</html>
"""


def json_response(handler: BaseHTTPRequestHandler, status: int, payload: Dict[str, Any]) -> None:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(data)))
    handler.end_headers()
    handler.wfile.write(data)


def text_response(handler: BaseHTTPRequestHandler, status: int, text: str, content_type: str = "text/html") -> None:
    data = text.encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", f"{content_type}; charset=utf-8")
    handler.send_header("Content-Length", str(len(data)))
    handler.end_headers()
    handler.wfile.write(data)


def read_json(handler: BaseHTTPRequestHandler) -> Dict[str, Any]:
    length = int(handler.headers.get("Content-Length", "0") or "0")
    if length <= 0:
        return {}
    return json.loads(handler.rfile.read(length).decode("utf-8"))


def resolve_path(value: Optional[str], base: Optional[Path] = DEFAULT_ROOT) -> Optional[Path]:
    if not value:
        return None
    path = Path(str(value))
    if not path.is_absolute() and base is not None:
        path = base / path
    return path.resolve()


def kv_rows_to_dict(rows: Any, key_name: str = "key", value_name: str = "value") -> Dict[str, str]:
    result: Dict[str, str] = {}
    if isinstance(rows, dict):
        return {str(k): str(v) for k, v in rows.items() if str(k).strip()}
    for row in rows or []:
        if not isinstance(row, dict):
            continue
        key = str(row.get(key_name, "")).strip()
        value = str(row.get(value_name, "")).strip()
        if key:
            result[key] = value
    return result


def package_rows_to_dict(rows: Any) -> Dict[str, str]:
    result: Dict[str, str] = {}
    if isinstance(rows, dict):
        rows = [{"from": k, "to": v} for k, v in rows.items()]
    for row in rows or []:
        if not isinstance(row, dict):
            continue
        old = normalize_package(row.get("from"))
        new = normalize_package(row.get("to"))
        if old and new:
            result[old] = new
    return result


def build_options(payload: Dict[str, Any]) -> Tuple[MigrationOptions, List[str], Dict[str, str], Dict[str, str]]:
    run = payload.get("runParams") or {}
    source_root = resolve_path(payload.get("sourceRoot") or ".") or DEFAULT_ROOT
    target_root = resolve_path(payload.get("targetRoot"), DEFAULT_ROOT)
    target_java_dir = resolve_path(payload.get("targetJavaDir"), target_root or DEFAULT_ROOT)
    target_resource_dir = resolve_path(payload.get("targetResourceDir"), target_root or DEFAULT_ROOT)
    report = str(run.get("report") or payload.get("report") or "api_migration_report.json").strip()
    write_report = bool(run.get("writeReport") or payload.get("writeReport"))
    options = MigrationOptions(
        source_root=source_root,
        target_root=target_root,
        target_java_dir=target_java_dir,
        target_resource_dir=target_resource_dir,
        include_tests=bool(run.get("includeTests") or payload.get("includeTests")),
        exact_route_match=bool(run.get("exactRouteMatch") or payload.get("exactRouteMatch")),
        max_depth=int(run.get("maxDepth") or payload.get("maxDepth") or 80),
        include_resources=run.get("includeResources", payload.get("includeResources", True)) is not False,
        whole_file_closure=False,
        source_base_package=None,
        target_base_package=None,
        package_map={},
        overwrite=bool(run.get("overwrite") or payload.get("overwrite")),
        skip_conflicts=run.get("skipConflicts", payload.get("skipConflicts", True)) is not False,
        update_target_refs=bool(run.get("updateTargetRefs") or payload.get("updateTargetRefs")),
        report=report,
        no_report=not write_report,
        verbose=bool(run.get("verbose") or payload.get("verbose")),
    )
    paths = [str(item).strip() for item in payload.get("paths", []) if str(item).strip()]
    env_vars = kv_rows_to_dict(payload.get("envVars"))
    extra_params = kv_rows_to_dict(payload.get("extraParams"))
    return options, paths, env_vars, extra_params


def file_status(item: PlannedFile, plan: MigrationPlan, options: MigrationOptions) -> str:
    if not item.target.exists():
        return "new"
    try:
        rendered = ApiMigrator(options).render_file(item, plan)
        existing = read_text(item.target)
    except Exception:
        return "conflict"
    if rendered == existing:
        return "same"
    if item.kind != "java" or options.overwrite:
        if options.overwrite:
            return "overwrite"
        if item.kind == "resource":
            merge = ApiMigrator(options).merge_resource_target(item, plan, rendered)
            if merge.conflicts:
                return "conflict"
            if merge.skipped_conflicts and not merge.changed:
                return "skipped-conflict"
            if merge.changed:
                return "append"
            return "same"
        return "conflict"
    merge = ApiMigrator(options).merge_java_target(item, plan, rendered)
    if merge.conflicts:
        return "conflict"
    if merge.skipped_conflicts and not merge.changed:
        return "skipped-conflict"
    if merge.skipped_existing_models and not merge.changed:
        return "skipped-existing-model"
    if merge.changed:
        return "append"
    return "same"


def file_record(index: int, item: PlannedFile, plan: MigrationPlan, options: MigrationOptions) -> Dict[str, Any]:
    try:
        size = item.source.stat().st_size
    except OSError:
        size = 0
    status = item.status
    if status == "planned":
        status = file_status(item, plan, options)
    return {
        "index": index,
        "kind": item.kind,
        "source": item.source_rel,
        "target": item.target_rel,
        "rewritten": item.rewritten,
        "status": status,
        "bytes": size,
    }


def project_summary(plan: MigrationPlan) -> List[Dict[str, Any]]:
    projects: Dict[str, Dict[str, Any]] = {}
    class_projects: Dict[str, set] = {}
    for fqn in plan.class_fqns:
        cls = plan.class_fqns and fqn
        if not cls:
            continue
    for item in plan.files:
        parts = Path(item.source_rel).parts
        name = parts[0] if parts else "root"
        projects.setdefault(name, {"name": name, "files": 0, "classes": 0, "resources": 0})
        projects[name]["files"] += 1
        if item.kind == "resource":
            projects[name]["resources"] += 1
    for fqn in plan.class_fqns:
        # The matching source file is enough to assign a class to a Maven module.
        class_info = None
        for cls in getattr(plan, "_classes", []):
            if getattr(cls, "fqn", None) == fqn:
                class_info = cls
                break
        _ = class_info
    # Avoid depending on private planner state; approximate class count by Java files.
    for item in plan.files:
        if item.kind == "java":
            parts = Path(item.source_rel).parts
            name = parts[0] if parts else "root"
            projects.setdefault(name, {"name": name, "files": 0, "classes": 0, "resources": 0})
            projects[name]["classes"] += 1
    return sorted(projects.values(), key=lambda item: item["name"])


def plan_payload(cached: CachedPlan) -> Dict[str, Any]:
    plan = cached.plan
    options = cached.options
    base = report_dict(plan, cached.result, options.source_root)
    files = [
        file_record(idx, item, plan, options)
        for idx, item in enumerate(plan.files)
    ]
    summary = dict(base.get("summary") or {})
    summary.update(
        {
            "routes": len(plan.routes),
            "plannedFiles": len(files),
            "resourceFiles": len(plan.resource_paths),
            "conflictFiles": sum(1 for item in files if item["status"] == "conflict"),
            "skippedConflictFiles": sum(1 for item in files if item["status"] == "skipped-conflict"),
            "skippedExistingModelFiles": sum(1 for item in files if item["status"] == "skipped-existing-model"),
            "appendFiles": sum(1 for item in files if item["status"] == "append"),
            "sameFiles": sum(1 for item in files if item["status"] == "same"),
            "newFiles": sum(1 for item in files if item["status"] == "new"),
        }
    )
    result = None
    if cached.result:
        result = {
            "written": cached.result.written,
            "skippedSame": cached.result.skipped_same,
            "skippedConflicts": cached.result.skipped_conflicts,
            "skippedExistingModels": cached.result.skipped_existing_models,
            "conflicts": cached.result.conflicts,
            "updatedRefs": cached.result.updated_refs,
            "reportPath": str(cached.result.report_path) if cached.result.report_path else None,
        }
    return {
        "ok": True,
        "planId": cached.plan_id,
        "createdAt": cached.created_at,
        "summary": summary,
        "routes": base.get("routes", []),
        "unresolvedPaths": base.get("unresolvedPaths", []),
        "unresolvedSuggestions": base.get("unresolvedSuggestions", {}),
        "warnings": base.get("warnings", []),
        "packageMap": base.get("packageMap", {}),
        "classFqnMap": base.get("classFqnMap", {}),
        "projects": project_summary(plan),
        "files": files,
        "envVars": cached.env_vars,
        "extraParams": cached.extra_params,
        "result": result,
    }


def cache_plan(cached: CachedPlan) -> None:
    PLAN_CACHE[cached.plan_id] = cached
    if len(PLAN_CACHE) <= PLAN_CACHE_MAX:
        return
    oldest = sorted(PLAN_CACHE.values(), key=lambda item: item.created_at)[0]
    PLAN_CACHE.pop(oldest.plan_id, None)


def next_plan_id() -> str:
    return f"plan-{int(time.time() * 1000)}"


def update_cached_options_for_migrate(cached: CachedPlan, payload: Dict[str, Any]) -> None:
    if "overwrite" in payload:
        cached.options.overwrite = bool(payload.get("overwrite"))
    if "skipConflicts" in payload:
        cached.options.skip_conflicts = bool(payload.get("skipConflicts"))
    if "updateTargetRefs" in payload:
        cached.options.update_target_refs = bool(payload.get("updateTargetRefs"))
    if "writeReport" in payload:
        cached.options.no_report = not bool(payload.get("writeReport"))
    if payload.get("report"):
        cached.options.report = str(payload.get("report"))


def count_lines(text: str) -> int:
    if not text:
        return 0
    return text.count("\n") + (0 if text.endswith("\n") else 1)


class ApiMigratorWebHandler(BaseHTTPRequestHandler):
    server_version = "ApiMigratorWeb/1.0"

    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        if parsed.path in {"/", "/index.html"}:
            text_response(self, 200, INDEX_HTML)
            return
        json_response(self, 404, {"ok": False, "error": "not found"})

    def do_POST(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        try:
            payload = read_json(self)
            if parsed.path == "/api/plan":
                self.handle_plan(payload)
                return
            if parsed.path == "/api/file-preview":
                self.handle_file_preview(payload)
                return
            if parsed.path == "/api/migrate":
                self.handle_migrate(payload)
                return
            json_response(self, 404, {"ok": False, "error": "not found"})
        except Exception as exc:
            json_response(
                self,
                500,
                {
                    "ok": False,
                    "error": str(exc),
                    "trace": traceback.format_exc(limit=8),
                },
            )

    def handle_plan(self, payload: Dict[str, Any]) -> None:
        options, paths, env_vars, extra_params = build_options(payload)
        if not paths:
            json_response(self, 400, {"ok": False, "error": "至少需要一个接口路径"})
            return
        planner = ApiMigrationPlanner(options)
        plan = planner.build(paths)
        cached = CachedPlan(
            plan_id=next_plan_id(),
            options=options,
            plan=plan,
            created_at=time.time(),
            env_vars=env_vars,
            extra_params=extra_params,
        )
        cache_plan(cached)
        status = 200 if not plan.unresolved_paths else 422
        json_response(self, status, plan_payload(cached))

    def handle_file_preview(self, payload: Dict[str, Any]) -> None:
        plan_id = str(payload.get("planId") or "")
        cached = PLAN_CACHE.get(plan_id)
        if not cached:
            json_response(self, 404, {"ok": False, "error": "计划已过期，请重新预览"})
            return
        index = int(payload.get("index"))
        if index < 0 or index >= len(cached.plan.files):
            json_response(self, 400, {"ok": False, "error": "文件索引无效"})
            return
        item = cached.plan.files[index]
        source_code = read_text(item.source)
        target_code = ApiMigrator(cached.options).render_file(item, cached.plan)
        file_data = file_record(index, item, cached.plan, cached.options)
        json_response(
            self,
            200,
            {
                "ok": True,
                "file": file_data,
                "sourceCode": source_code,
                "targetCode": target_code,
                "sourceLines": count_lines(source_code),
                "targetLines": count_lines(target_code),
            },
        )

    def handle_migrate(self, payload: Dict[str, Any]) -> None:
        plan_id = str(payload.get("planId") or "")
        cached = PLAN_CACHE.get(plan_id)
        if not cached:
            json_response(self, 404, {"ok": False, "error": "计划已过期，请重新预览"})
            return
        if not (cached.options.target_root or cached.options.target_java_dir):
            json_response(self, 400, {"ok": False, "error": "执行迁移需要目标项目路径或目标 Java 目录"})
            return
        update_cached_options_for_migrate(cached, payload)
        cached.result = ApiMigrator(cached.options).migrate(cached.plan)
        if not cached.options.no_report:
            report_path = Path(cached.options.report)
            base = cached.options.target_root or cached.options.source_root
            if not report_path.is_absolute():
                report_path = base / report_path
            report_path.parent.mkdir(parents=True, exist_ok=True)
            report_path.write_text(
                json.dumps(report_dict(cached.plan, cached.result, cached.options.source_root), ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            cached.result.report_path = report_path
        json_response(self, 200, plan_payload(cached))

    def log_message(self, fmt: str, *args: Any) -> None:
        sys.stderr.write("%s - - [%s] %s\n" % (self.client_address[0], self.log_date_time_string(), fmt % args))


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Start the API migration Web UI.")
    parser.add_argument("--host", default="127.0.0.1", help="Host to bind")
    parser.add_argument("--port", type=int, default=8766, help="Port to bind")
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    args = build_arg_parser().parse_args(argv)
    server = ThreadingHTTPServer((args.host, args.port), ApiMigratorWebHandler)
    print(f"API migration Web UI: http://{args.host}:{args.port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
