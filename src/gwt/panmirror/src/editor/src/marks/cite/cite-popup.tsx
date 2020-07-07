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

//import './xref-popup.css';
import { BibliographySource, BibliographyManager } from "../../api/bibliography";
import { kCiteIdPrefixPattern } from "./cite";

const kMaxWidth = 350; // also in xref-popup.css

export function citePopupPlugin(schema: Schema, ui: EditorUI, bibMgr: BibliographyManager) {

  return textPopupDecorationPlugin({
    key: new PluginKey<DecorationSet>('cite-popup'),
    markType: schema.marks.cite_id,
    maxWidth: kMaxWidth,
    dismissOnEdit: true,
    createPopup: async (view: EditorView, target: TextPopupTarget, style: React.CSSProperties) => {
      await bibMgr.loadBibliography(ui, view.state.doc);

      console.log(target.text);
      const citeId = target.text.replace(/^-@|^@/, '');
      console.log(citeId);
      const source = bibMgr.findCiteId(citeId);
      if (source) {
        // click handler
        const onClick = () => {
          const url = bibMgr.urlForSource(source);
          if (url) {
            ui.display.openURL(url);
          }
        };

        return (<CitePopup source={source} onClick={onClick} style={style} />);

      }
      return null;
    },
    specKey: (target: TextPopupTarget) => {
      return `xref:${target.text}`;
    }
  });

}

interface CitePopupProps extends WidgetProps {
  source: BibliographySource;
  onClick: VoidFunction;
}

const CitePopup: React.FC<CitePopupProps> = props => {
  return (
    <Popup classes={['pm-xref-popup']} style={props.style}>
      <div>
        <LinkButton
          text={props.source.DOI || props.source.id}
          onClick={props.onClick}
          maxWidth={kMaxWidth - 20}
          classes={['pm-xref-popup-key pm-fixedwidth-font']}
        />
      </div>
      <div className="pm-xref-popup-file">
        {props.source.title}
      </div>

    </Popup>
  );
};