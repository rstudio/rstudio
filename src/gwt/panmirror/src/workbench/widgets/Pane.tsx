/*
 * Pane.tsx
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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
