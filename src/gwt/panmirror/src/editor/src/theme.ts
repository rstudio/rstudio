/*
 * theme.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

export interface EditorTheme {
  darkMode: boolean;
  cursorColor: string;
  selectionColor: string;
  nodeSelectionColor: string;
  backgroundColor: string;
  metadataBackgroundColor: string;
  chunkBackgroundColor: string;
  spanBackgroundColor: string;
  divBackgroundColor: string;
  commentBackgroundColor: string;
  gutterBackgroundColor: string;
  gutterTextColor: string;
  textColor: string;
  lightTextColor: string;
  linkTextColor: string;
  surfaceWidgetTextColor: string;
  markupTextColor: string;
  findTextBackgroundColor: string;
  findTextBorderColor: string;
  borderBackgroundColor: string;
  blockBorderColor: string;
  focusOutlineColor: string;
  paneBorderColor: string;
  fixedWidthFont: string;
  fixedWidthFontSizePt: number;
  proportionalFont: string;
  proportionalFontSizePt: number;
  code: CodeTheme;
}

export interface CodeTheme {
  keywordColor: string;
  atomColor: string;
  numberColor: string;
  variableColor: string;
  defColor: string;
  operatorColor: string;
  commentColor: string;
  stringColor: string;
  metaColor: string;
  builtinColor: string;
  bracketColor: string;
  tagColor: string;
  attributeColor: string;
  hrColor: string;
  linkColor: string;
  errorColor: string;
}

export function defaultTheme(): EditorTheme {
  return {
    darkMode: false,
    cursorColor: 'black',
    selectionColor: '#8cf',
    nodeSelectionColor: 'rgb(102,155,243)',
    backgroundColor: 'white',
    metadataBackgroundColor: 'rgb(255, 255, 255)',
    chunkBackgroundColor: 'rgb(251,251,251)',
    spanBackgroundColor: '#fff8dc',
    divBackgroundColor: 'rgb(243, 243, 243)',
    commentBackgroundColor: 'rgb(254, 155, 243)',
    gutterBackgroundColor: '#f0f0f0',
    gutterTextColor: '#333',
    textColor: 'black',
    surfaceWidgetTextColor: 'rgba(0,0,0,0.5)',
    lightTextColor: 'rgb(60, 76, 114)',
    linkTextColor: ' #106ba3',
    markupTextColor: 'rgb(185, 6, 144)',
    findTextBackgroundColor: 'rgb(250, 250, 255)',
    findTextBorderColor: 'rgb(200, 200, 250)',
    borderBackgroundColor: '#ddd',
    blockBorderColor: '#ddd',
    focusOutlineColor: '#ddd',
    paneBorderColor: 'silver',
    fixedWidthFont: 'monospace, monospace',
    fixedWidthFontSizePt: 9,
    proportionalFont: '"Lucida Sans", "DejaVu Sans", "Lucida Grande", "Segoe UI", Verdana, Helvetica, sans-serif',
    proportionalFontSizePt: 10,
    code: {
      keywordColor: 'rgb(0, 0, 255)',
      atomColor: 'rgb(88, 92, 246)',
      numberColor: 'rgb(0, 0, 205)',
      variableColor: 'rgb(0, 0, 0)',
      defColor: 'rgb(0, 0, 0)',
      operatorColor: 'rgb(104, 118, 135)',
      commentColor: 'rgb(76, 136, 107)',
      stringColor: 'rgb(3, 106, 7)',
      metaColor: 'rgb(0, 0, 0)',
      builtinColor: 'rgb(0, 0, 0)',
      bracketColor: 'rgb(104, 118, 135)',
      tagColor: 'rgb(0, 22, 142)',
      attributeColor: 'rgb(0, 0, 0)',
      hrColor: 'rgb(0, 0, 0)',
      linkColor: 'rgb(0, 0, 255)',
      errorColor: 'rgb(197, 6, 11)',
    },
  };
}

export function applyTheme(theme: EditorTheme) {
  // merge w/ defaults
  const defaults = defaultTheme();
  theme = {
    ...defaults,
    ...theme,
    code: {
      ...defaults.code,
      ...theme.code,
    },
  };

  // generate theme css
  const themeCss = `
    .pm-cursor-color {
      caret-color: ${theme.cursorColor}
    }
    .pm-background-color {
      background-color: ${theme.backgroundColor} !important;
    }
    .pm-metadata-background-color {
      background-color: ${theme.metadataBackgroundColor} !important;
    }
    .pm-chunk-background-color {
      background-color: ${theme.chunkBackgroundColor} !important;
    }
    .pm-span-background-color {
      background-color: ${theme.spanBackgroundColor} !important;
    }
    .pm-div-background-color {
      background-color: ${theme.divBackgroundColor} !important;
    }
    .pm-comment-background-color {
      background-color: ${theme.commentBackgroundColor} !important;
    }
    .pm-text-color {
      color: ${theme.textColor} !important;
    }
    .pm-surface-widget-text-color {
      color: ${theme.surfaceWidgetTextColor} !important;
    }
    .pm-light-text-color {
      color: ${theme.lightTextColor} !important;
    }
    .pm-link-text-color {
      color: ${theme.linkTextColor} !important;
    }
    .pm-markup-text-color {
      color: ${theme.markupTextColor} !important;
    }
    .pm-find-text {
      background-color: ${theme.findTextBackgroundColor} !important;
      outline: 1px solid ${theme.findTextBorderColor} !important;
    }
    .pm-find-text-selected {
      background-color: ${theme.selectionColor} !important;
    }
    .pm-border-background-color {
      background-color: ${theme.borderBackgroundColor}!important;
    }
    .pm-block-border-color {
      border-color: ${theme.blockBorderColor} !important;
    }
    .pm-focus-outline-color {
      outline-color: ${theme.focusOutlineColor} !important;
    }
    .pm-pane-border-color {
      border-color: ${theme.paneBorderColor} !important;
    }
    .pm-div .pm-div {
      border-color: ${theme.focusOutlineColor} !important;
    }
    .pm-raw-block-border {
      border-color: ${theme.markupTextColor} !important;
    }
    .pm-popup {
      box-shadow: 0 2px 10px ${theme.paneBorderColor} !important;
    }
    .pm-selected-node-outline-color,
    .ProseMirror-selectednode {
      outline-color: ${theme.nodeSelectionColor} !important;
    }
    .pm-selected-node-border-color {
      border-color: ${theme.nodeSelectionColor} !important;
    }
    .pm-dark-mode .pm-selected-node-outline-color,
    .pm-dark-mode .ProseMirror-selectednode {
      outline-color: ${theme.nodeSelectionColor} !important;
    }
    .pm-dark-mode .pm-selected-node-border-color {
      border-color: ${theme.nodeSelectionColor} !important;
    }
    .pm-background-color *::selection {
      background-color: ${theme.selectionColor} !important;
    }
    .pm-background-color *::-moz-selection {
      background-color: ${theme.selectionColor} !important;
    }
    .ProseMirror .CodeMirror .CodeMirror-selectedtext  { 
      background: none !important;
    }
    .pm-fixedwidth-font {
      font-family: ${theme.fixedWidthFont} !important;
      font-size: ${theme.fixedWidthFontSizePt}pt !important;
    }
    .pm-proportional-font {
      font-family: ${theme.proportionalFont} !important;
      font-size: ${theme.proportionalFontSizePt}pt !important;
    }
    .CodeMirror,
    .CodeMirror pre.CodeMirror-line, .CodeMirror pre.CodeMirror-line-like {
      font-family: ${theme.fixedWidthFont};
      font-size: ${theme.fixedWidthFontSizePt}pt !important;
    }
    .CodeMirror {
      background: ${theme.backgroundColor};
    }
    .CodeMirror-selected { background: none  ; }
    .CodeMirror-focused .CodeMirror-selected { background: ${theme.selectionColor}  ; }
    .CodeMirror-line::selection, .CodeMirror-line > span::selection, .CodeMirror-line > span > span::selection { background: ${theme.selectionColor}  ; }
    .CodeMirror-line::-moz-selection, .CodeMirror-line > span::-moz-selection, .CodeMirror-line > span > span::-moz-selection { background: ${theme.selectionColor}  ; }
    .CodeMirror-cursor { border-left-color: ${theme.cursorColor}; }
    .CodeMirror pre.CodeMirror-line, .CodeMirror pre.CodeMirror-line-like  { color: ${theme.textColor}; }
    .CodeMirror-gutters { 
      border-right-color: ${theme.blockBorderColor} !important;
      background-color: ${theme.gutterBackgroundColor} !important; 
    }
    .CodeMirror-linenumber { color: ${theme.gutterTextColor} !important; }
    .cm-s-default .cm-keyword {color: ${theme.code.keywordColor};}
    .cm-s-default .cm-atom {color: ${theme.code.atomColor};}
    .cm-s-default .cm-number {color: ${theme.code.numberColor};}
    .cm-s-default .cm-def {color: ${theme.code.defColor};}
    .cm-s-default .cm-variable { color: ${theme.textColor}; }
    .cm-s-default .cm-punctuation,
    .cm-s-default .cm-property,
    .cm-s-default .cm-operator {color: ${theme.code.operatorColor};}
    .cm-s-default .cm-variable-2 { color: ${theme.textColor}; }
    .cm-s-default .cm-variable-3, .cm-s-default .cm-type { color: ${theme.textColor}; }
    .cm-s-default .cm-comment {color: ${theme.code.commentColor};}
    .cm-s-default .cm-string {color: ${theme.code.stringColor};}
    .cm-s-default .cm-string-2 {color: ${theme.code.stringColor};}
    .cm-s-default .cm-meta {color: ${theme.code.metaColor};}
    .cm-s-default .cm-qualifier {color:${theme.code.metaColor};}
    .cm-s-default .cm-builtin {color: ${theme.code.builtinColor};}
    .cm-s-default .cm-bracket {color: ${theme.code.bracketColor};}
    .cm-s-default .cm-tag {color: ${theme.code.tagColor};}
    .cm-s-default .cm-attribute {color: ${theme.code.attributeColor};}
    .cm-s-default .cm-hr {color: ${theme.code.hrColor};}
    .cm-s-default .cm-link {color:${theme.code.linkColor};}
    .cm-s-default .cm-error {color: ${theme.code.errorColor};}
  `;

  // set style
  setStyleElement('pm-editor-theme-styles-id', themeCss);
}

export function applyPadding(padding: string) {
  const paddingCss = `
    .pm-editing-root-node {
      padding-left: ${padding} !important;
      padding-right: ${padding} !important;
    }
  `;
  setStyleElement('pm-editor-padding-styles-id', paddingCss);
}

function setStyleElement(id: string, css: string) {
  // get access to style element (create if necessary)
  let styleEl = window.document.getElementById(id);
  if (styleEl === null) {
    styleEl = window.document.createElement('style');
    styleEl.setAttribute('id', id);
    styleEl.setAttribute('type', 'text/css');
    window.document.head.appendChild(styleEl);
  }

  // set css if we need to
  if (styleEl.innerHTML !== css) {
    styleEl.innerHTML = css;
  }
}
