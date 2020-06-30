/*
 * xref-popup.tsx
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


import { Schema } from "prosemirror-model";
import { PluginKey } from "prosemirror-state";
import { DecorationSet, EditorView } from "prosemirror-view";

import React from "react";

import { EditorUI } from "../../api/ui";
import { EditorNavigation } from "../../api/navigation";
import { textPopupDecorationPlugin, TextPopupTarget } from "../../api/text-popup";
import { WidgetProps } from "../../api/widgets/react";
import { Popup } from "../../api/widgets/popup";
import { EditorServer } from "../../editor/editor";
import { XRef, xrefKey } from "../../api/xref";

import './xref-popup.css';

export function xrefPopupPlugin(schema: Schema, ui: EditorUI, nav: EditorNavigation, server: EditorServer) {

  return textPopupDecorationPlugin({
    key: new PluginKey<DecorationSet>('xref-popup'),
    markType: schema.marks.xref,
    maxWidth: 370,
    dismissOnEdit: true,
    createPopup: async (view: EditorView, target: TextPopupTarget, style: React.CSSProperties) => {

      const kXRefRegEx = /^@ref\(([A-Za-z0-9:-]*)\)$/;

      // lookup xref on server
      const docPath = ui.context.getDocumentPath();
      if (docPath) {
        const match = target.text.match(kXRefRegEx);
        if (match && match[1].length) {
          const xrefs = await server.xref.xrefForId(docPath, match[1]);
          if (xrefs.refs.length) {
            return (<XRefPopup xref={xrefs.refs[0]} ui={ui} nav={nav} style={style} />);
          }
        }
      }
      return null;
    },
    specKey: (target: TextPopupTarget) => {
      return `xref:${target.text}`;
    }
  });

}

interface XRefPopupProps extends WidgetProps {
  xref: XRef;
  ui: EditorUI;
  nav: EditorNavigation;
  style: React.CSSProperties;
}

const XRefPopup: React.FC<XRefPopupProps> = props => {
  return (
    <Popup classes={['pm-xref-popup']} style={props.style}>
      <div>
        {xrefKey(props.xref)} &mdash; {props.xref.file}
      </div>
      <div>
        {props.xref.title}
      </div>
    </Popup>
  );
};