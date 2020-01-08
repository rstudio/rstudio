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
