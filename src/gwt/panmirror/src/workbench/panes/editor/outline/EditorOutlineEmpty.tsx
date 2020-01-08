import React from 'react';

import { useTranslation } from 'react-i18next';

import styles from './EditorOutlineSidebar.module.scss';

export const EditorOutlineEmpty: React.FC = () => {
  const { t } = useTranslation();

  return <div className={styles.outlineEmpty}>{t('outline_empty')}</div>;
};
