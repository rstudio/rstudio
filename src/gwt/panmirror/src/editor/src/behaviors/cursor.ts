/*
 * cursor.ts
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

import { PluginKey, Plugin } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { dropCursor } from 'prosemirror-dropcursor';
import { gapCursor, GapCursor } from 'prosemirror-gapcursor';
import 'prosemirror-gapcursor/style/gapcursor.css';

import { findParentNodeOfTypeClosestToPos } from 'prosemirror-utils';

import { Extension } from '../api/extension';

import './cursor.css';

const extension: Extension = {
  plugins: () => {
    return [
      gapCursor(), 
      dropCursor(),
      new Plugin({
        key: new PluginKey('div-gap-cursor'),
        props: {
          handleDOMEvents: {
            click: gapClickHandler,
          },
        },
      })];
  },
};

function gapClickHandler(view: EditorView, event: Event): boolean {

  const schema = view.state.schema;
  const mouseEvent = event as MouseEvent;
  const clickPos = view.posAtCoords({ left: mouseEvent.clientX, top: mouseEvent.clientY } );

  if (clickPos) {

    // resolve click pos
    const $clickPos = view.state.doc.resolve(clickPos.pos);    

    // create a gap cursor at the click position
    const createGapCursor = () => {
      // focus the view
      view.focus();
        
      // create the gap cursor
      const tr = view.state.tr;
      const cursor = new GapCursor($clickPos, $clickPos); 
      tr.setSelection(cursor);
      view.dispatch(tr);
      
      // prevent default event handling
      event.preventDefault();
      event.stopImmediatePropagation();
      return false;
    };
     
    // handle clicks at the top of divs
     if (schema.nodes.div) {
      const div = findParentNodeOfTypeClosestToPos(
        view.state.doc.resolve(clickPos.pos), schema.nodes.div
      );
      if (div && div.pos === clickPos.inside) {
        
        return createGapCursor();
      
      }
    }

    // handle clicks above body
    if ($clickPos.parent.type === schema.nodes.body &&
        $clickPos.start() === $clickPos.pos) {
      
      return createGapCursor();

    }
    
  }

  return false;
}

export default extension;
