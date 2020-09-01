/*
 * tag-input.tsx
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

import { WidgetProps } from "./react";

import { EditorUI } from "../ui";

import './tag-input.css';
import { TextInput } from "./text";

// Item representing a tag entry
// The key remains stable even if the tag is edited
// The displayPrefix will be displayed to the user, but removed when editing
export interface TagItem {
  key: string;
  displayText: string;
  displayPrefix: string;
  isEditable?: boolean;
}

interface TagInputProps extends WidgetProps {
  tags: TagItem[];
  tagDeleted: (tag: TagItem) => void;
  tagChanged: (key: string, text: string) => void;
  ui: EditorUI;
}

export const TagInput = React.forwardRef<HTMLDivElement, TagInputProps>((props, ref) => {
  return (<div style={props.style} className='pm-tag-input-container' ref={ref}>
    {props.tags.map(tag => (<Tag key={tag.key} tag={tag} tagDeleted={props.tagDeleted} tagChanged={props.tagChanged} ui={props.ui} />))}
  </div >);
});

interface TagProps extends WidgetProps {
  tag: TagItem;
  tagDeleted: (tag: TagItem) => void;
  tagChanged: (key: string, text: string) => void;
  ui: EditorUI;
}

const Tag: React.FC<TagProps> = props => {

  const [editing, setEditing] = React.useState<boolean>(false);
  const [editingText, setEditingText] = React.useState<string>(props.tag.displayText);
  const [displayText, setDisplayText] = React.useState<string>(props.tag.displayText);

  // Anytime we begin editing, focus the text input
  const editTextInput = React.useRef<HTMLInputElement>(null);
  React.useLayoutEffect(() => {
    if (editing) {
      editTextInput.current?.focus();
    }
  }, [editing]);

  // Click the delete icon
  const onDeleteClick = (e: React.MouseEvent) => {
    props.tagDeleted(props.tag);
  };

  // Enter or space while delete icon focused
  const onDeleteKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
      case 'Space':
        props.tagDeleted(props.tag);
        break;
    }
  };

  // Click on the edit icon
  const onEditClick = (e: React.MouseEvent) => {
    if (props.tag.isEditable) {
      setEditing(true);
    }
  };

  // Enter or space while edit icon is focused
  const onEditKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
      case 'Space':
        setEditing(true);
        break;
    }
  };

  // Commit edits to the tag
  const commitTagEdit = () => {
    // Update the text
    setDisplayText(editingText);

    // Halt editing
    setEditing(false);

    // Notify of change
    props.tagChanged(props.tag.key, editingText);
  };

  // When editing the tag, allow enter to accept the changes
  const handleEditKeyPress = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
        e.preventDefault();
        e.stopPropagation();
        commitTagEdit();
        break;
    }
  };

  // When editing, clicking away from the tag will accept changes
  const handleEditBlur = () => {
    commitTagEdit();
  };

  const handleEditChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const edittedText = e.target.value;
    setEditingText(edittedText);
  };

  return <div key={props.tag.displayText} className='pm-tag-input-tag pm-block-border-color'>
    <img src={props.ui.images.widgets?.tag_delete} onClick={onDeleteClick} onKeyPress={onDeleteKeyPress} className='pm-tag-input-delete-image' tabIndex={0} />
    <div className={`pm-tag-input-text ${props.tag.isEditable ? 'pm-tag-input-text-edittable' : undefined}`}>
      {!editing ?
        <div onClick={onEditClick} className='pm-tag-input-text-raw pm-text-color'>{props.tag.displayPrefix}{displayText}</div> :
        <TextInput
          width={`${editingText.length} ch`}
          ref={editTextInput}
          className='pm-tag-input-text-edit'
          value={editingText}
          onChange={handleEditChange}
          onKeyPress={handleEditKeyPress}
          onBlur={handleEditBlur} />
      }
    </div>
    {props.tag.isEditable ? <img src={props.ui.images.widgets?.tag_edit} className='pm-tag-input-edit-image' onClick={onEditClick} onKeyPress={onEditKeyPress} tabIndex={0} /> : undefined}
  </div>;
};