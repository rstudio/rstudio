
/*
 * widgets.ts
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

import './widgets.css';
 
export function createHorizontalPanel() {
  const div = window.document.createElement('div');
  div.classList.add('pm-horizontal-panel');
  return div;
}

export function addHorizontalPanelCell(panel: HTMLDivElement, el: HTMLElement) {
  el.classList.add('pm-horizontal-panel-cell');
  panel.append(el);
}

export function createPopup(classes: string[], style?: { [key: string]: string }) {
  const popup = window.document.createElement("div");
  popup.classList.add(
    "pm-popup",
    "pm-pane-border-color",
    "pm-background-color",
    "pm-text-color",
    ...classes
  );
  popup.style.position = "absolute";
  applyStyle(popup, style);
  return popup;
}

export function createInlineTextPopup(classes: string[], style?: { [key: string]: string }) {
  const popup = createPopup([ ...classes, "pm-popup-inline-text"], style);
  popup.style.display = "inline-block";
  return popup;
}

export function createLink(href: string, maxWidth?: number, style?: { [key: string]: string }) {
  const link = window.document.createElement("a");
  link.classList.add(
    "pm-link",
    "pm-link-text-color",
  );
  link.href = href;
  link.innerText = href;
  if (maxWidth) {
    link.style.maxWidth = maxWidth + 'px';
  }
  applyStyle(link, style);
  return link;
}

export function createImageButton(classes: string[], style?: { [key: string]: string }) {
  const button = window.document.createElement("button");
  button.classList.add(
    "pm-image-button",
    ...classes
  );
  applyStyle(button, style);
  return button;
}

function applyStyle(el: HTMLElement, style?: { [key: string]: string }) {
  if (style) {
    Object.keys(style).forEach(name => {
      el.style.setProperty(name, style[name]);
    });
  }
}
