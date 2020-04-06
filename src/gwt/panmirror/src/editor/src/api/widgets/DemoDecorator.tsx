

import * as React from 'react';

import { Panel, PanelCell } from './Panel';

import { WidgetProps, reactNodeDecorator } from './react';
import { EditorView } from 'prosemirror-view';


export interface DemoDecoratorProps extends WidgetProps {
  text: string;
}

const DemoDecorator: React.FC<DemoDecoratorProps> = props => {
  return (
    <Panel>
      <PanelCell>
        {props.text}
      </PanelCell>
    </Panel>
  );
};

export function demoDecorator(view: EditorView, text: string) {
  return reactNodeDecorator(view, DemoDecorator({ text })!);
}
