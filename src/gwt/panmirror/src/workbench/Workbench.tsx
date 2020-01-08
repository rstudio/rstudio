import React, { useEffect } from 'react';

import { Store } from 'redux';
import { Provider as StoreProvider } from 'react-redux';

import { FocusStyleManager } from '@blueprintjs/core';

import { WorkbenchState } from './store/store';

import { CommandManagerProvider } from './commands/CommandManager';

import WorkbenchNavbar from './frame/WorkbenchNavbar';
import WorkbenchClipboard from './frame/WorkbenchClipboard';
import WorkbenchHotkeys from './frame/WorkbenchHotkeys';

import EditorPane from './panes/editor/EditorPane';
import MarkdownPane from './panes/markdown/MarkdownPane';

import './Workbench.scss';

interface WorkbenchProps {
  store: Store<WorkbenchState>;
}

const Workbench: React.FC<WorkbenchProps> = props => {
  // only show focus on key navigation
  useEffect(() => {
    FocusStyleManager.onlyShowFocusOnTabs();
  }, []);

  return (
    <div className={'workbench'}>
      <StoreProvider store={props.store}>
        <CommandManagerProvider>
          <WorkbenchNavbar />

          <div className={'workspace'}>
            <EditorPane />
            <MarkdownPane />
          </div>

          <WorkbenchClipboard />
          <WorkbenchHotkeys />
        </CommandManagerProvider>
      </StoreProvider>
    </div>
  );
};

export default Workbench;
