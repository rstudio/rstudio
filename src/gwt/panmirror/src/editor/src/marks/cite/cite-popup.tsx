/*
 * cite-popup.tsx
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
import { textPopupDecorationPlugin, TextPopupTarget } from "../../api/text-popup";
import { WidgetProps } from "../../api/widgets/react";
import { Popup } from "../../api/widgets/popup";
import { LinkButton } from "../../api/widgets/button";
import { join } from "../../api/path";
import { BibliographySource, BibliographyManager, cslFromDoc } from "../../api/bibliography";
import { PandocServer } from "../../api/pandoc";

import './cite-popup.css';


const kMaxWidth = 400; // also in cite-popup.css

export function citePopupPlugin(schema: Schema, ui: EditorUI, bibMgr: BibliographyManager, server: PandocServer) {

  return textPopupDecorationPlugin({
    key: new PluginKey<DecorationSet>('cite-popup'),
    markType: schema.marks.cite_id,
    maxWidth: kMaxWidth,
    dismissOnEdit: true,
    createPopup: async (view: EditorView, target: TextPopupTarget, style: React.CSSProperties) => {
      await bibMgr.loadBibliography(ui, view.state.doc);

      const csl = cslFromDoc(view.state.doc);
      const citeId = target.text.replace(/^-@|^@/, '');
      const source = bibMgr.findCiteId(citeId);
      if (source) {
        const docPath = ui.context.getDocumentPath();
        const previewHtml = await server.citationHTML(docPath, JSON.stringify([source]), csl || null);
        const compressedPreviewHtml = previewHtml.replace(/\r?\n|\r/g, '');

        // TODO: Deal with links in preview + append a link if needed

        // click handler
        const onClick = () => {
          const url = bibMgr.urlForSource(source);
          if (url) {
            ui.display.openURL(url);
          }
        };

        return (
          <CitePopup
            previewHtml={compressedPreviewHtml}
            linkText={ui.context.translateText("[Link]")}
            onClick={onClick}
            style={style} />
        );

      }
      return null;
    },
    specKey: (target: TextPopupTarget) => {
      return `cite:${target.text}`;
    }
  });
}

interface CitePopupProps extends WidgetProps {
  previewHtml: string;
  linkText?: string;
  onClick: VoidFunction;
}

const CitePopup: React.FC<CitePopupProps> = props => {
  return (
    <Popup classes={['pm-cite-popup']} style={props.style}>
      <div className='pm-cite-popup-preview'>
        <div dangerouslySetInnerHTML={{ __html: props.previewHtml || '' }} />
      </div>
    </Popup>
  );
};