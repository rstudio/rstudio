
// JJA: add header comment (check for all files in directory)

import { WidgetProps } from '../../api/widgets/react';
import React from 'react';
import { Symbol } from './insert_symbol-data';

interface SymbolPreviewProps extends WidgetProps {
  top: number;
  left: number;
  height: number;
  width: number;
  symbol: Symbol;
}

// JJA: why do we need the ? on accesses of props.symbol? (seems like esp. for the 'U+' 
// case that if it were null/undefined that we'd have busted output)

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
          {props.symbol?.value}
        </div>
        <div className="pm-symbol-grid-symbol-preview-name">
          {props.symbol?.name}
        </div>
        <div className="pm-symbol-grid-symbol-preview-codepoint">
          {'U+' + props.symbol?.codepoint.toString(16)}
        </div>
      </div>
    )
  );
});
