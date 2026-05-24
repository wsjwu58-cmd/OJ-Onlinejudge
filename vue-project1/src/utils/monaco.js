// Monaco Editor worker + theme config for Vite
// 说明：Monaco 在 Vite 环境下要显式配置 worker，否则会出现“无高亮/无智能提示/控制台 worker 报错”等问题。

import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker'
import cssWorker from 'monaco-editor/esm/vs/language/css/css.worker?worker'
import htmlWorker from 'monaco-editor/esm/vs/language/html/html.worker?worker'
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker'

// 语言服务（contribution）
import 'monaco-editor/esm/vs/language/json/monaco.contribution'
import 'monaco-editor/esm/vs/language/css/monaco.contribution'
import 'monaco-editor/esm/vs/language/html/monaco.contribution'
import 'monaco-editor/esm/vs/language/typescript/monaco.contribution'

import * as monaco from 'monaco-editor/esm/vs/editor/editor.api'

// Vite 下通过全局 MonacoEnvironment.getWorker 指定不同语言使用的 worker
self.MonacoEnvironment = {
  getWorker(_, label) {
    if (label === 'json') return new jsonWorker()
    if (label === 'css' || label === 'scss' || label === 'less') return new cssWorker()
    if (label === 'html' || label === 'handlebars' || label === 'razor') return new htmlWorker()
    if (label === 'typescript' || label === 'javascript') return new tsWorker()
    return new editorWorker()
  }
}

// 自定义主题（可按需调整）
const customTheme = {
  base: 'vs-dark',
  inherit: true,
  rules: [
    { token: 'comment', foreground: '617b91', fontStyle: 'italic' },
    { token: 'keyword', foreground: 'c5cceb', fontStyle: 'bold' },
    { token: 'string', foreground: 'a9b1d6' },
    { token: 'number', foreground: 'c5cceb' },
    { token: 'operator', foreground: 'c7cacf' },
    { token: 'delimiter', foreground: 'c7cacf' }
  ],
  colors: {
    'editor.background': '#252837',
    'editor.foreground': '#c5cceb',
    'editor.lineHighlightBackground': '#29344c',
    'editorCursor.foreground': '#c5cceb',
    'editorLineNumber.foreground': '#617b91',
    'editorLineNumber.activeForeground': '#c5cceb'
  }
}

monaco.editor.defineTheme('custom-dark', customTheme)
