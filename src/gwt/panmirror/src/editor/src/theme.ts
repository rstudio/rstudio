/*
 * theme.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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
  cursorColor: string;
  selectionColor: string;
  backgroundColor: string;
  metadataBackgroundColor: string;
  chunkBackgroundColor: string;
  chunkHeaderBackgroundColor: string;
  spanBackgroundColor: string;
  divBackgroundColor: string;
  textColor: string;
  lightTextColor: string;
  linkTextColor: string;
  markupTextColor: string;
  borderBackgroundColor: string;
  blockBorderColor: string;
  focusOutlineColor: string;
  paneBorderColor: string;
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


export function defaultTheme() : EditorTheme {
  return {
    cursorColor: "black",
    selectionColor: "#8cf",
    backgroundColor: "white",
    metadataBackgroundColor: "rgb(251,251,251)", 
    chunkBackgroundColor: "rgb(251,251,251)",
    chunkHeaderBackgroundColor: "rgb(242,242,242)", // TODO
    spanBackgroundColor: "#fff8dc",
    divBackgroundColor: "rgb(236, 249, 250)",
    textColor: "black",
    lightTextColor: "rgb(60, 76, 114)",
    linkTextColor: " #106ba3",
    markupTextColor: "rgb(185, 6, 144)",
    borderBackgroundColor: "#ddd",
    blockBorderColor: "#ddd",
    focusOutlineColor: "#ddd",
    paneBorderColor: "silver",
    code: {
      keywordColor: "black",
      atomColor: "black",
      numberColor: "black",
      variableColor: "black",
      defColor: "black",
      operatorColor: "black",
      commentColor: "black",
      stringColor: "black",
      metaColor: "black",
      builtinColor: "black",
      bracketColor: "black",
      tagColor: "black",
      attributeColor: "black",
      hrColor: "black",
      linkColor: "black",
      errorColor: "black"
    }
  }
}

export function applyTheme(theme: EditorTheme)
{
  // merge w/ defaults
  const defaults = defaultTheme();
  theme = {
    ...defaults,
    ...theme,
    code: {
      ...defaults.code,
      ...theme.code
    }
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
    .pm-chunk-header-background-color {
      background-color: ${theme.chunkHeaderBackgroundColor} !important;
    }
    .pm-span-background-color {
      background-color: ${theme.spanBackgroundColor} !important;
    }
    .pm-div-background-color {
      background-color: ${theme.divBackgroundColor} !important;
    }
    .pm-text-color {
      color: ${theme.textColor} !important;
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
    .pm-selected-node-outline-color,
    .ProseMirror-selectednode {
      outline-color: ${theme.selectionColor} !important;
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
    .CodeMirror-selected { background: none  ; }
    .CodeMirror-focused .CodeMirror-selected { background: ${theme.selectionColor}  ; }
    .CodeMirror-line::selection, .CodeMirror-line > span::selection, .CodeMirror-line > span > span::selection { background: ${theme.selectionColor}  ; }
    .CodeMirror-line::-moz-selection, .CodeMirror-line > span::-moz-selection, .CodeMirror-line > span > span::-moz-selection { background: ${theme.selectionColor}  ; }

    .CodeMirror-cursor { border-left-color: ${theme.cursorColor}; }
    .CodeMirror pre.CodeMirror-line, .CodeMirror pre.CodeMirror-line-like  { color: ${theme.textColor}; }
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
  
  // get access to theme element (create if necessary)
  const themeElementId = 'pm-editor-theme';
  let themeElement = window.document.getElementById(themeElementId);
  if (themeElement === null) {
    themeElement = window.document.createElement("style");
    themeElement.setAttribute("id", themeElementId);
    themeElement.setAttribute("type", "text/css");
    window.document.head.appendChild(themeElement);
  }

  // set theme 
  themeElement.innerHTML = themeCss;
}
