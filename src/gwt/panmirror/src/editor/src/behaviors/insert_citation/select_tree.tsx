import React from "react";

import { WidgetProps } from "../../api/widgets/react";


import './select_tree.css';

export interface SelectTreeNode {
  key: string;
  name: string;
  type: string;
  image?: string;
  children: SelectTreeNode[];
  expanded?: boolean;
}

interface SelectTreeProps extends WidgetProps {
  nodes: SelectTreeNode[];
  selectedNode?: SelectTreeNode;
  nodeSelected: (node: SelectTreeNode) => void;
}

export const SelectTree: React.FC<SelectTreeProps> = props => {
  return (
    <div>
      {props.nodes.map(treeNode => <SelectTreeItem key={treeNode.key} node={treeNode} alwaysOpen={true} onSelected={props.nodeSelected} selectedNode={props.selectedNode} />)}
    </div>
  );
};


interface SelectTreeItemProps extends WidgetProps {
  node: SelectTreeNode;
  onSelected: (node: SelectTreeNode) => void;
  indentLevel?: number;
  selectedNode?: SelectTreeNode;
  alwaysOpen?: boolean;

}

export const SelectTreeItem: React.FC<SelectTreeItemProps> = props => {

  const kIndent = 10;

  const style = {
    ...props.style
  };

  const onClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    props.onSelected(props.node);
  };

  const expanded = props.selectedNode && containsChild(props.selectedNode.key, props.node);
  const selected = props.selectedNode && props.selectedNode.key === props.node.key;
  const indentLevel = props.indentLevel || 0;

  const indentStyle = {
    paddingLeft: indentLevel * 10 + 'px'
  };

  const className = `pm-select-tree-node`;
  const selectedClassName = `${selected ? 'pm-selected-select-tree-item' : ''}`;
  return (
    <div key={props.node.key} className={className} onClick={onClick} style={style}>
      <div className={selectedClassName} style={indentStyle}>{props.node.name}</div>
      {props.alwaysOpen || expanded ? props.node.children?.map(childNode => <SelectTreeItem key={childNode.key} node={childNode} onSelected={props.onSelected} indentLevel={indentLevel + 1} selectedNode={props.selectedNode} />) : ''}
    </div>
  );
};

export function containsChild(key: string, node: SelectTreeNode): boolean {
  if (node.key === key) {
    return true;
  }

  for (const childNode of node.children) {
    const hasChild = containsChild(key, childNode);
    if (hasChild) {
      return true;
    }
  }
  return false;
}



