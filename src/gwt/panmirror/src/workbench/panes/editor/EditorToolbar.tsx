import React from 'react';

import { IconNames } from '@blueprintjs/icons';

import { EditorCommandId } from 'editor/src/api/command';

import { Toolbar, ToolbarDivider, ToolbarMenu } from 'workbench/widgets/Toolbar';
import { CommandToolbarButton } from 'workbench/widgets/command/CommandToolbarButton';
import { CommandToolbarMenu } from 'workbench/widgets/command/CommandToolbarMenu';
import { WithCommand } from 'workbench/widgets/command/WithCommand';
import { WorkbenchCommandId } from 'workbench/commands/commands';

import EditorTableMenuItems from './EditorTableMenuItems';

import styles from './EditorPane.module.scss';

const CommandId = { ...EditorCommandId, ...WorkbenchCommandId };

const EditorToolbar: React.FC = () => {
  return (
    <Toolbar className={styles.toolbar}>
      <CommandToolbarButton command={CommandId.Undo} />
      <CommandToolbarButton command={CommandId.Redo} />
      <ToolbarDivider />
      <CommandToolbarButton command={CommandId.Print} />
      <ToolbarDivider />
      <CommandToolbarMenu
        className={styles.toolbarBlockFormatMenu}
        commands={[
          CommandId.Paragraph,
          '---',
          CommandId.Heading1,
          CommandId.Heading2,
          CommandId.Heading3,
          CommandId.Heading4,
          CommandId.Heading5,
          CommandId.Heading6,
          '---',
          CommandId.CodeBlock,
        ]}
      />
      <ToolbarDivider />
      <CommandToolbarButton command={CommandId.Strong} />
      <CommandToolbarButton command={CommandId.Em} />
      <CommandToolbarButton command={CommandId.Code} />
      <ToolbarDivider />
      <CommandToolbarButton command={CommandId.BulletList} />
      <CommandToolbarButton command={CommandId.OrderedList} />
      <CommandToolbarButton command={CommandId.Blockquote} />
      <ToolbarDivider />
      <WithCommand id={CommandId.TableInsertTable}>
        <ToolbarMenu icon={IconNames.TH}>
          <EditorTableMenuItems />
        </ToolbarMenu>
        <ToolbarDivider />
      </WithCommand>
      <CommandToolbarButton command={CommandId.Link} />
      <CommandToolbarButton command={CommandId.Image} />
      <ToolbarDivider />
      <CommandToolbarButton command={CommandId.RmdChunk} />
    </Toolbar>
  );
};

export default EditorToolbar;
