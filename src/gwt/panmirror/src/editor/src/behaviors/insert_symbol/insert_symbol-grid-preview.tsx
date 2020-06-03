/*
 * insert_symbol-grid-preview.tsx
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

import { WidgetProps } from '../../api/widgets/react';
import React from 'react';
import { SymbolCharacter } from './insert_symbol-dataprovider';

interface SymbolPreviewProps extends WidgetProps {
  top: number;
  left: number;
  height: number;
  width: number;
  symbolCharacter: SymbolCharacter;
}

export const SymbolPreview = React.forwardRef<any, SymbolPreviewProps>((props, ref) => {
  return (
    (
      <div
        className="pm-symbol-grid-symbol-preview pm-background-color pm-pane-border-color"
        style={{
          position: 'fixed',
          left: props.left,
          top: props.top,
          height: props.height + 'px',
          width: props.width + 'px',
        }}
        ref={ref}
      >
        <div className="pm-symbol-grid-symbol-preview-symbol">
          {props.symbolCharacter.value}
        </div>
        <div className="pm-symbol-grid-symbol-preview-name">
          {props.symbolCharacter.name}
        </div>
        <div className="pm-symbol-grid-symbol-preview-codepoint">
          {props.symbolCharacter.codepoint ? 'U+' + props.symbolCharacter.codepoint.toString(16) : ''}
        </div>
      </div>
    )
  );
});
