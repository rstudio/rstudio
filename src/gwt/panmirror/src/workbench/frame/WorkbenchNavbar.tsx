/*
 * WorkbenchNavbar.tsx
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

import { IProps, Navbar } from '@blueprintjs/core';

import WorkbenchMenubar from './WorkbenchMenubar';
import WorkbenchTitle from './WorkbenchTitle';

import styles from './WorkbenchNavbar.module.scss';

const WorkbenchNavbar: React.FC<IProps> = () => {
  return (
    <Navbar className={`navbar ${styles.navbar}`} fixedToTop={true}>
      <div className={styles.logo}>
        <img src="/images/logo.png" alt="" />
      </div>
      <div className={styles.controls}>
        <div>
          <WorkbenchTitle />
        </div>
        <WorkbenchMenubar />
      </div>
    </Navbar>
  );
};

export default WorkbenchNavbar;
