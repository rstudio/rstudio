
import React from "react";

import { WidgetProps } from "../../api/widgets/react";

import './tag_input.css';
import { textRangePopupDecorationPosition } from "../../api/widgets/decoration";


interface TagInputProps extends WidgetProps {
  tags: string[];
  deleteTag: (tag: string) => void;
}

export const TagInput: React.FC<TagInputProps> = props => {


  return (<div style={props.style}>
    {props.tags.map(tag => (<Tag key={tag} text={tag} deleteTag={props.deleteTag} />))
    }
  </div >);

};

interface TagProps extends WidgetProps {
  text: string;
  deleteTag: (tag: string) => void;
}


const Tag: React.FC<TagProps> = props => {

  const onClick = (e: React.MouseEvent) => {
    props.deleteTag(props.text);
  };
  return <span key={props.text} onClick={onClick}>{props.text}&nbsp;</span>;
};