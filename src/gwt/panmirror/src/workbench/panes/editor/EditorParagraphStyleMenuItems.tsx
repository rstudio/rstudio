import React from 'react';

import { Menu } from '@blueprintjs/core';

import { EditorCommandId } from 'editor/src/api/command';

import { CommandMenuItem, CommandMenuItemActive } from 'workbench/widgets/command/CommandMenuItem';

const EditorParagraphStyleMenuItems: React.FC = () => {
  return (
    <>
      <CommandMenuItem id={EditorCommandId.Paragraph} active={CommandMenuItemActive.Check} />
      <Menu.Divider />
      <CommandMenuItem id={EditorCommandId.Heading1} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={EditorCommandId.Heading2} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={EditorCommandId.Heading3} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={EditorCommandId.Heading4} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={EditorCommandId.Heading5} active={CommandMenuItemActive.Check} />
      <CommandMenuItem id={EditorCommandId.Heading6} active={CommandMenuItemActive.Check} />
      <Menu.Divider />
      <CommandMenuItem id={EditorCommandId.CodeBlock} active={CommandMenuItemActive.Check} />
    </>
  );
};

export default EditorParagraphStyleMenuItems;
