import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

// 创建vue自定义指令
export default {
  install: (app) => {
    app.directive('highlight', {
      mounted(el) {
        // 先处理pre标签，确保每个pre标签都有对应的code标签
        const preBlocks = el.querySelectorAll('pre')
        preBlocks.forEach(pre => {
          // 检查pre标签是否已经包含code标签
          if (!pre.querySelector('code')) {
            // 如果没有code标签，创建一个并将pre的内容移到code标签中
            const code = document.createElement('code')
            code.textContent = pre.textContent
            pre.textContent = ''
            pre.appendChild(code)
          }
        })
        
        // 然后应用highlight.js
        const blocks = el.querySelectorAll('pre code')
        blocks.forEach(block => {
          hljs.highlightElement(block)
        })
      },
      updated(el) {
        // 先处理pre标签，确保每个pre标签都有对应的code标签
        const preBlocks = el.querySelectorAll('pre')
        preBlocks.forEach(pre => {
          // 检查pre标签是否已经包含code标签
          if (!pre.querySelector('code')) {
            // 如果没有code标签，创建一个并将pre的内容移到code标签中
            const code = document.createElement('code')
            code.textContent = pre.textContent
            pre.textContent = ''
            pre.appendChild(code)
          }
        })
        
        // 然后应用highlight.js
        const blocks = el.querySelectorAll('pre code')
        blocks.forEach(block => {
          hljs.highlightElement(block)
        })
      }
    })
  }
}
