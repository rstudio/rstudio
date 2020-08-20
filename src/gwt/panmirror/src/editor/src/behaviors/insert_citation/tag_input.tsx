
import React from "react";

import { WidgetProps } from "../../api/widgets/react";

import './tag_input.css';


interface TagInputProps extends WidgetProps {
  tags: string[];
  deleteTag: (tag: string) => void;
}

export const TagInput: React.FC<TagInputProps> = props => {
  return (<div style={props.style}>
    {props.tags.map(tag => (<span key={tag}>{tag}</span>))}
  </div>);

};