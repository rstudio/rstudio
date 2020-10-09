/*
 * editor-theme.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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
  solarizedMode: boolean;
  cursorColor: string;
  selectionColor: string;
  nodeSelectionColor: string;
  backgroundColor: string;
  metadataBackgroundColor: string;
  chunkBackgroundColor: string;
  spanBackgroundColor: string;
  divBackgroundColor: string;
  commentColor: string;
  commentBackgroundColor: string;
  gutterBackgroundColor: string;
  gutterTextColor: string;
  textColor: string;
  lightTextColor: string;
  placeholderTextColor: string;
  invisibleTextColor: string;
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
    solarizedMode: false,
    cursorColor: 'black',
    selectionColor: '#8cf',
    nodeSelectionColor: 'rgb(102,155,243)',
    backgroundColor: 'white',
    metadataBackgroundColor: 'rgb(255, 255, 255)',
    chunkBackgroundColor: 'rgb(251,251,251)',
    spanBackgroundColor: '#fff8dc',
    divBackgroundColor: 'rgb(243, 243, 243)',
    commentColor: '#3c4c72',
    commentBackgroundColor: '#FFECCB',
    gutterBackgroundColor: '#f0f0f0',
    gutterTextColor: '#333',
    textColor: 'black',
    surfaceWidgetTextColor: 'rgba(0,0,0,0.5)',
    lightTextColor: 'rgb(60, 76, 114)',
    linkTextColor: '#106ba3',
    placeholderTextColor: 'gray',
    invisibleTextColor: 'rgb(191, 191, 191)',
    markupTextColor: 'rgb(185, 6, 144)',
    findTextBackgroundColor: 'rgb(250, 250, 255)',
    findTextBorderColor: 'rgb(200, 200, 250)',
    borderBackgroundColor: '#ddd',
    blockBorderColor: '#ddd',
    focusOutlineColor: '#5d84cd',
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
    .pm-default-theme .pm-background-color {
      background-color: ${defaults.backgroundColor} !important;
    }
    .pm-default-theme .pm-text-color {
      color: ${defaults.textColor} !important;
    }
    .pm-default-theme .pm-border-background-color {
      background-color: ${defaults.borderBackgroundColor} !important;
    }
    .pm-default-theme .pm-input-text {
      border-color: ${defaults.paneBorderColor} !important
    }
    .pm-default-theme .pm-block-border-color {
      border-color: ${defaults.blockBorderColor} !important;
    }
    .pm-default-theme .pm-input-outline-button {
      color: ${defaults.textColor};
      border-color: ${defaults.textColor};
      background-color: ${defaults.backgroundColor};
    }
    .pm-default-theme .pm-selected-navigation-tree-item {
      background-color: ${defaults.findTextBackgroundColor} !important;
      border-color: ${defaults.findTextBorderColor} !important;
    }
    .pm-default-theme .pm-navigation-tree-item {
      border-color: ${defaults.backgroundColor} !important;
    }
    .pm-default-theme .pm-block-border-color {
      border-color: ${defaults.blockBorderColor} !important;
    }
    .pm-default-theme .pm-focus-outline-color {
      border-color: ${defaults.focusOutlineColor} !important;
    }
    .pm-default-theme .pm-input-button {
      border-color: ${defaults.borderBackgroundColor};
      background-color: ${defaults.backgroundColor};
    }
    .pm-default-theme .pm-placeholder-text-color {
      color: ${defaults.placeholderTextColor} !important;
    }
    .pm-default-theme .pm-background-color *::selection {
      background-color: ${defaults.selectionColor} !important;
    }
    .pm-default-theme .pm-find-text,
    .pm-default-theme .pm-list-item-selected,
    .pm-default-theme .pm-grid-item-selected {
      background-color: ${defaults.findTextBackgroundColor} !important;
      box-shadow: 0 0 0 1px ${defaults.findTextBorderColor}; 
      border-radius: 3px;
    }
    .pm-default-theme .pm-rstudio-button {
      border-color: DarkGray !important;
    }
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
    .pm-nbsp-background-color {
      background-color: ${theme.spanBackgroundColor} !important;
    }
    .pm-comment-color {
      color: ${theme.commentColor} !important;
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
    .pm-placeholder-text-color {
      color: ${theme.placeholderTextColor} !important;
    }
    .pm-invisible-text-color {
      color: ${theme.invisibleTextColor} !important;
    }
    .pm-link-text-color {
      color: ${theme.linkTextColor} !important;
    }
    .pm-markup-text-color {
      color: ${theme.markupTextColor} !important;
    }
    .pm-find-text,
    .pm-list-item-selected,
    .pm-grid-item-selected {
      background-color: ${theme.findTextBackgroundColor} !important;
      box-shadow: 0 0 0 1px ${theme.findTextBorderColor}; 
      border-radius: 3px;
    }
    .pm-selected-text {
      background-color: ${theme.selectionColor} !important;
    }
    .pm-selected-list-item {
      background-color: ${theme.findTextBackgroundColor} !important;
      border: 1px solid ${theme.findTextBorderColor} !important;
      border-radius: 3px;
    }
    .pm-dark-mode .pm-selected-list-item {
      background-color: ${theme.chunkBackgroundColor} !important;
      border: 1px solid transparent !important;
    }
    .pm-navigation-tree-item {
      border: 1px solid ${theme.backgroundColor} !important;
      border-radius: 3px;
      margin-left: 2px;
      margin-right: 2px;
    }
    .pm-selected-navigation-tree-item {
      background-color: ${theme.findTextBackgroundColor} !important;
      border: 1px solid ${theme.findTextBorderColor} !important;
      border-radius: 3px;
      margin-left: 2px;
      margin-right: 2px;
    }
    .pm-border-background-color {
      background-color: ${theme.borderBackgroundColor}!important;
    }
    .pm-block-border-color {
      border-color: ${theme.blockBorderColor} !important;
    }
    .pm-focus-outline-color {
      border-color: ${theme.focusOutlineColor} !important;
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
      box-shadow: 0 1px 3px ${theme.blockBorderColor} !important;
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
    .pm-fixedwidth-font {
      font-family: ${theme.fixedWidthFont} !important;
      font-size: ${theme.fixedWidthFontSizePt}pt !important;
    }
    .pm-proportional-font {
      font-family: ${theme.proportionalFont} !important;
      font-size: ${theme.proportionalFontSizePt}pt !important;
    }
    .pm-input-text {
      border-color: ${theme.paneBorderColor};
    }
    .pm-input-button {
      border-color: ${theme.borderBackgroundColor};
      background-color: ${theme.backgroundColor};
    }
    .pm-input-outline-button {
      color: ${theme.textColor};
      border-color: ${theme.textColor};
      background-color: ${theme.backgroundColor};
    }
    .pm-ace-first-line-meta .ace_text-layer .ace_line_group:first-child,
    .pm-ace-first-line-meta .ace_text-layer .ace_line_group:first-child span {
      color: ${theme.lightTextColor} !important;
    }
    .ProseMirror-gapcursor:after {
      border-top: 1px solid ${theme.textColor};
    }
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

  // set css
  styleEl.innerHTML = css;
}
