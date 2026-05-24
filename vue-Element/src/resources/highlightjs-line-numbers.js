// jshint multistr:true
function lineNumberInit(hljs, w, d) {
  'use strict';

  if (!w.document) {
    w.console.error('DOM not found!');
    return;
  }

  w = w || window;
  d = d || document;

  function addStyles() {
    var css = d.createElement('style');
    css.type = 'text/css';
    css.innerHTML = '.hljs-ln { display: block; width: 100%; overflow: auto; } .hljs-ln td { padding: 0; } .hljs-ln-code { padding-left: 10px !important; } .hljs-ln-numbers { -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; text-align: right; color: #999; border-right: 1px solid #ccc; vertical-align: top; padding-right: 10px !important; } .hljs-ln-numbers br { display: none; }';
    (d.head || d.body).appendChild(css);
  }

  function lineNumbersBlock(element) {
    if (typeof element !== 'object') return false;

    var parent = element.parentNode;
    var lines = getLinesCount(element.innerHTML);
    var lineNumbersWrapper;

    parent.classList.add('hljs-ln');
    parent.style.counterReset = 'linenumber ' + (parseInt(getStartLineNumber(element)) - 1);

    lineNumbersWrapper = d.createElement('span');
    lineNumbersWrapper.className = 'hljs-ln-numbers';

    lineNumbersWrapper.innerHTML = generateLineNumbers(lines);

    element.insertBefore(lineNumbersWrapper, element.firstChild);
  }

  function lineNumbersValue(value, startFrom) {
    var element = d.createElement('div');

    element.innerHTML = value;

    var blocks = element.querySelectorAll('pre code');
    var i;

    startFrom = startFrom || 1;

    for (i = 0; i < blocks.length; i++) {
      blocks[i].innerHTML = addLineNumbers(blocks[i].innerHTML, startFrom);
    }

    return element.innerHTML;
  }

  function initLineNumbersOnLoad() {
    var blocks = d.querySelectorAll('pre code');
    var i;

    for (i = 0; i < blocks.length; i++) {
      lineNumbersBlock(blocks[i].parentNode);
    }
  }

  function getLinesCount(code) {
    var matches = code.match(/\n/g);
    return matches ? matches.length + 1 : 1;
  }

  function getStartLineNumber(element) {
    var lineNumber = element.getAttribute('data-line');
    return lineNumber ? parseInt(lineNumber, 10) : 1;
  }

  function generateLineNumbers(count) {
    var lines = '';
    var i;

    for (i = 1; i <= count; i++) {
      lines += '<br>';
    }

    return lines;
  }

  function addLineNumbers(code, startFrom) {
    var lines = code.split(/\r?\n/);
    var i;

    for (i = 0; i < lines.length; i++) {
      lines[i] = '<span class="hljs-ln-line">' + lines[i] + '</span>';
    }

    return lines.join('\n');
  }

  if (hljs) {
    hljs.initLineNumbersOnLoad = initLineNumbersOnLoad;
    hljs.lineNumbersBlock = lineNumbersBlock;
    hljs.lineNumbersValue = lineNumbersValue;

    addStyles();
  } else {
    w.console.error('highlight.js not detected!');
  }

  return {
    initLineNumbersOnLoad: initLineNumbersOnLoad,
    lineNumbersBlock: lineNumbersBlock,
    lineNumbersValue: lineNumbersValue
  };
}

export { lineNumberInit }