/*
 * Toolbar.tsx
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

import React from 'react';

import { IconNames } from '@blueprintjs/icons';
import {
  IProps,
  ButtonGroup,
  Button,
  Text,
  IconName,
  Divider,
  Popover,
  Position,
  Menu,
  MaybeElement,
} from '@blueprintjs/core';

import styles from './Toolbar.module.scss';

export const Toolbar: React.FC<IProps> = props => {
  return (
    <ButtonGroup className={[styles.toolbar, props.className].join(' ')} minimal={true}>
      {props.children}
    </ButtonGroup>
  );
};

export interface ToolbarButtonProps extends IProps {
  icon?: IconName | MaybeElement;
  title: string;
  enabled: boolean;
  active: boolean;
  onClick: () => void;
}

export const ToolbarButton: React.FC<ToolbarButtonProps> = props => {
  return (
    <Button
      className={[styles.toolbarButton, props.className].join(' ')}
      title={props.title}
      icon={props.icon}
      disabled={!props.enabled}
      active={props.active}
      onClick={props.onClick}
    />
  );
};

export const ToolbarDivider: React.FC = () => {
  return <Divider className={styles.toolbarDivider} />;
};

export const ToolbarText: React.FC<IProps> = props => {
  return <Text className={styles.toolbarText}>{props.children}</Text>;
};

export interface ToolbarMenuProps extends IProps {
  text?: string;
  icon?: IconName;
  disabled?: boolean;
}

export const ToolbarMenu: React.FC<ToolbarMenuProps> = props => {
  return (
    <Popover
      className={[styles.toolbarMenu, props.className].join(' ')}
      disabled={props.disabled}
      autoFocus={false}
      minimal={true}
      inheritDarkTheme={false}
      position={Position.BOTTOM_LEFT}
    >
      <Button
        className={styles.toolbarMenuButton}
        icon={props.icon}
        disabled={props.disabled}
        rightIcon={IconNames.CARET_DOWN}
      >
        {props.text}
      </Button>
      <Menu>{props.children}</Menu>
    </Popover>
  );
};
