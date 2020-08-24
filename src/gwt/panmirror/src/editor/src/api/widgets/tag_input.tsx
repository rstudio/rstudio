/*
 * tag_input.tsx
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import React from "react";

import { WidgetProps } from "../../api/widgets/react";

import './tag_input.css';


interface TagInputProps extends WidgetProps {
  tags: string[];
  deleteTag: (tag: string) => void;
}

export const TagInput = React.forwardRef<HTMLDivElement, TagInputProps>((props, ref) => {
  return (<div style={props.style} className='pm-tag-input-container' ref={ref}>
    {props.tags.map(tag => (<Tag key={tag} text={tag} deleteTag={props.deleteTag} />))
    }
  </div >);
});

interface TagProps extends WidgetProps {
  text: string;
  deleteTag: (tag: string) => void;
}

const Tag: React.FC<TagProps> = props => {
  const onClick = (e: React.MouseEvent) => {
    props.deleteTag(props.text);
  };
  return <div key={props.text} onClick={onClick} className='pm-tag-input-text pm-selected-select-tree-item'>{props.text}</div>;
};