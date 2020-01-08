import React from 'react';

import { IProps, Popover, Position, Button, Menu } from '@blueprintjs/core';
import { IconNames, IconName } from '@blueprintjs/icons';

import styles from './Menu.module.scss';

export interface MainMenuProps extends IProps {
  text: string;
  menu: JSX.Element;
}

export const MainMenu: React.FC<MainMenuProps> = props => {
  return (
    <Popover
      autoFocus={false}
      minimal={true}
      inheritDarkTheme={false}
      content={props.menu}
      position={Position.BOTTOM_LEFT}
    >
      <Button className={styles.button}>{props.text}</Button>
    </Popover>
  );
};

export const MenubarMenu: React.FC<IProps> = props => {
  return <Menu className={styles.menubarMenu}>{props.children}</Menu>;
};

export interface SubMenuProps extends IProps {
  text: string;
  icon?: IconName;
}

export const SubMenu: React.FC<SubMenuProps> = props => {
  return (
    <Menu.Item text={props.text} icon={props.icon || IconNames.BLANK}>
      {props.children}
    </Menu.Item>
  );
};
