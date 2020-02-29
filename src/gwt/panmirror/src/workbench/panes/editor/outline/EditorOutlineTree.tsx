/*
 * EditorOutlineTree.tsx
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

import React, { useContext } from 'react';

import { IProps, ITreeNode, Tree } from '@blueprintjs/core';

import { EditorOutline, EditorOutlineItem } from 'editor/src/api/outline';

import { EditorActionsContext } from '../EditorActionsContext';

import styles from './EditorOutlineSidebar.module.scss';
import { useTranslation } from 'react-i18next';

export interface EditorOutlineTreeProps extends IProps {
  outline: EditorOutline;
}

export const EditorOutlineTree: React.FC<EditorOutlineTreeProps> = props => {
  // use translation
  const { t } = useTranslation();

  // editor actions context
  const editorActions = useContext(EditorActionsContext);

  // get label for node
  const label = (outlineNode: EditorOutlineItem) => {
    switch (outlineNode.type) {
      case 'heading':
        return outlineNode.title;
      case 'rmd_chunk':
        return t('outline_code_chunk_text');
      case 'yaml_metadata':
        return t('outline_metadata_text');
    }
  };

  // get tree nodes from outline
  const asTreeNode = (outlineNode: EditorOutlineItem): ITreeNode<number> => {
    return {
      id: outlineNode.navigation_id,
      label: label(outlineNode),
      hasCaret: false,
      isExpanded: true,

      childNodes: outlineNode.children.map(asTreeNode),
    };
  };
  const contents = props.outline.map(asTreeNode);

  // drive editor selection from outline
  // const dispatch = useDispatch();
  const onNodeClick = (treeNode: ITreeNode<number>) => {
    editorActions.navigate(treeNode.id as string);
    editorActions.focus();
  };

  // render truee
  return (
    <div className={styles.outlineTreeContainer}>
      <Tree className={styles.outlineTree} contents={contents} onNodeClick={onNodeClick} />
    </div>
  );
};
