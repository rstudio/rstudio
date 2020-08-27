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
import { EditorUI } from "../ui";
import { realpathSync } from "fs";
import { Rect } from "prosemirror-tables";


export interface TagItem {
  key: string;
  displayText: string;
  displayPrefix: string;
  isEditable?: boolean;
}

interface TagInputProps extends WidgetProps {
  tags: TagItem[];
  ui: EditorUI;
  deleteTag: (tag: TagItem) => void;
  tagEdited: (key: string, text: string) => void;
}

export const TagInput = React.forwardRef<HTMLDivElement, TagInputProps>((props, ref) => {
  return (<div style={props.style} className='pm-tag-input-container' ref={ref}>
    {props.tags.map(tag => (<Tag key={tag.key} tag={tag} deleteTag={props.deleteTag} tagEdited={props.tagEdited} ui={props.ui} />))}
  </div >);
});

interface TagProps extends WidgetProps {
  tag: TagItem;
  ui: EditorUI;
  deleteTag: (tag: TagItem) => void;
  tagEdited: (key: string, text: string) => void;
}

const Tag: React.FC<TagProps> = props => {

  const [editing, setEditing] = React.useState<boolean>(false);
  const [editingText, setEditingText] = React.useState<string>(props.tag.displayText);
  const [displayText, setDisplayText] = React.useState<string>(props.tag.displayText);

  const editTextInput = React.useRef<HTMLInputElement>(null);
  React.useLayoutEffect(() => {
    if (editing) {
      editTextInput.current?.focus();
    }
  }, [editing]);

  React.useEffect(() => {
    props.tagEdited(props.tag.key, displayText);
  }, [editing]);


  const onDeleteClick = (e: React.MouseEvent) => {
    props.deleteTag(props.tag);
  };

  const onDeleteKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
      case 'Space':
        props.deleteTag(props.tag);
        break;
    }
  };

  const onEditClick = (e: React.MouseEvent) => {
    setEditing(true);
  };

  const onEditKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
      case 'Space':
        setEditing(true);
        break;
    }
  };

  const handleEditKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
        e.preventDefault();
        e.stopPropagation();
        setDisplayText(editingText);
        setEditing(false);
        break;
    }
  };

  const handleEditBlur = () => {
    setDisplayText(editingText);
    setEditing(false);
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const edittedText = e.target.value;
    setEditingText(edittedText);
  };

  return <div key={props.tag.displayText} className='pm-tag-input-tag pm-block-border-color'>
    <img src={props.ui.images.widgets?.tag_delete} onClick={onDeleteClick} onKeyPress={onDeleteKeyPress} className='pm-tag-input-delete-image' tabIndex={0} />
    <div className='pm-tag-input-text'>
      {!editing ?
        <div onClick={onEditClick} className='pm-tag-input-text-raw'>{props.tag.displayPrefix}{displayText}</div> :
        <input type='text'
          size={editingText.length}
          ref={editTextInput}
          className='pm-tag-input-text-edit'
          value={editingText}
          onChange={handleEditChange}
          onKeyPress={handleEditKeyPress}
          onBlur={handleEditBlur} />
      }</div>
    {props.tag.isEditable ? <img src={props.ui.images.widgets?.tag_edit} className='pm-tag-input-edit-image' onClick={onEditClick} onKeyPress={onEditKeyPress} tabIndex={0} /> : undefined}
  </div>;
};