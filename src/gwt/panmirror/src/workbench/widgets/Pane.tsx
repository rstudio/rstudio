import React from 'react';

import { Card, IProps, Elevation } from '@blueprintjs/core';

import styles from './Pane.module.scss';

export interface PaneProps extends IProps {
  className: string;
  elevation?: Elevation;
}

export const Pane: React.FC<PaneProps> = props => {
  const { className, elevation = Elevation.ZERO } = props;

  return (
    <Card className={`${styles.pane} ${className}`} elevation={elevation}>
      {props.children}
    </Card>
  );
};
