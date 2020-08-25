/*
 * select_tree.tsx
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

import React, { CSSProperties } from "react";

import { WidgetProps } from "../../api/widgets/react";

import './select_tree.css';

// Individual nodes and children of the Select Tree
export interface SelectTreeNode {
  key: string;
  name: string;
  image?: string;
  type: string;
  expanded?: boolean;
  children: SelectTreeNode[];
}

interface SelectTreeProps extends WidgetProps {
  height: number;
  nodes: SelectTreeNode[];
  selectedNode?: SelectTreeNode;
  nodeSelected: (node: SelectTreeNode) => void;
}


export const SelectTree: React.FC<SelectTreeProps> = props => {

  const style: CSSProperties = {
    overflowY: 'scroll',
    height: props.height + 'px',
    ...props.style
  };

  const processKey = (e: React.KeyboardEvent) => {
    const selectedNode = props.selectedNode;
    switch (e.key) {
      case 'ArrowDown':
        if (selectedNode) {
          const next = nextNode(selectedNode, props.nodes);
          props.nodeSelected(next);
        }
        break;

      case 'ArrowUp':
        if (selectedNode) {
          const previous = previousNode(selectedNode, props.nodes);
          props.nodeSelected(previous);
        }
        break;
    }
  };


  return (
    <div style={style} tabIndex={0} onKeyDown={processKey} >
      {props.nodes.map(treeNode => <SelectTreeItem key={treeNode.key} node={treeNode} onSelected={props.nodeSelected} selectedNode={props.selectedNode} />)}
    </div>
  );
};


interface SelectTreeItemProps extends WidgetProps {
  node: SelectTreeNode;
  onSelected: (node: SelectTreeNode) => void;
  indentLevel?: number;
  selectedNode?: SelectTreeNode;
}

export const SelectTreeItem: React.FC<SelectTreeItemProps> = props => {

  const kIndent = 16;

  const style = {
    ...props.style
  };

  const onClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    props.onSelected(props.node);
  };

  React.useEffect(() => {
    if (props.node === props.selectedNode) {
      props.node.expanded = true;
      setExpanded(props.node.expanded);
    }
  }, [props.selectedNode]);

  const [expanded, setExpanded] = React.useState<boolean>(props.node.expanded || false);

  const selected = props.selectedNode && props.selectedNode === props.node;
  const indentLevel = props.indentLevel || 0;

  const indentStyle = {
    paddingLeft: indentLevel * kIndent + 'px'
  };

  const selectedClassName = `${selected ? 'pm-selected-select-tree-item' : 'pm-select-tree-item'} pm-select-tree-node`;
  return (
    <div key={props.node.key} onClick={onClick} style={style}>
      <div className={selectedClassName} style={indentLevel > 0 ? indentStyle : undefined}>
        {props.node.image ? <div className='pm-select-tree-node-image-div'><img src={props.node.image} alt={props.node.name} className='pm-select-tree-node-image' /></div> : null}
        <div className='pm-select-tree-node-label-div'>{props.node.name}</div>
      </div>
      {expanded ? props.node.children?.map(childNode => <SelectTreeItem key={childNode.key} node={childNode} onSelected={props.onSelected} indentLevel={indentLevel + 1} selectedNode={props.selectedNode} />) : undefined}
    </div >
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

// Creates an ordered flattened list of visible nodes in the
// tree. Useful for incrementing through visible nodes :)
function visibleNodes(nodes: SelectTreeNode[]) {

  const nodeList: SelectTreeNode[][] = nodes.map(node => {
    if (node.expanded) {
      return [node].concat(visibleNodes(node.children));
    } else {
      return [node];
    }
  });
  return ([] as SelectTreeNode[]).concat(...nodeList);

}

// Get the next node for the current node
function nextNode(node: SelectTreeNode, allNodes: SelectTreeNode[]): SelectTreeNode {
  const nodes = visibleNodes(allNodes);
  const currentIndex = nodes.indexOf(node);
  if (currentIndex < nodes.length - 1) {
    return nodes[currentIndex + 1];
  } else {
    return nodes[0];
  }
}

// Get the previous node for the current node
function previousNode(node: SelectTreeNode, allNodes: SelectTreeNode[]): SelectTreeNode {
  const nodes = visibleNodes(allNodes);
  const currentIndex = nodes.indexOf(node);
  if (currentIndex > 0) {
    return nodes[currentIndex - 1];
  } else {
    return nodes[nodes.length - 1];
  }

}







