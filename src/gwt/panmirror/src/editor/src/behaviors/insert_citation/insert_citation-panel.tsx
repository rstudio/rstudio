// TODO: comment


import React from "react";

import { Node as ProsemirrorNode } from 'prosemirror-model';

import { WidgetProps } from "../../api/widgets/react";

import { SelectTree, SelectTreeNode } from "./select_tree";
import { BibliographyManager, BibliographyContainer, BibliographySource } from "../../api/bibliography/bibliography";
import { EditorUI } from "../../api/ui";

import './insert_citation-panel.css';
import { FixedSizeList, ListChildComponentProps } from "react-window";

interface InsertCitationPanelProps extends WidgetProps {
  ui: EditorUI;
  doc: ProsemirrorNode;
  height: number;
  width: number;
  bibliographyManager: BibliographyManager;
  onCitationChanged: (cites: string[]) => void;
}

// Takes a flat data structure of containers and turns it into a hierarchical
// tree structure for display as TreeNodes.
function toTree(type: string, containers: BibliographyContainer[]): SelectTreeNode[] {

  const treeMap: { [id: string]: SelectTreeNode } = {};
  const rootNodes: SelectTreeNode[] = [];

  containers.sort((a, b) => a.name.localeCompare(b.name)).forEach(container => {

    // First see if we have an existing node for this item
    // A node could already be there if we had to insert a 'placeholder' 
    // node to contain the node's children before we encountered the node.
    const currentNode = treeMap[container.key] || { key: container.key, name: container.name, children: [], type };

    // Always set its name to be sure we fill this in when we encounter it
    currentNode.name = container.name;

    if (container.parentKey) {
      let parentNode = treeMap[container.parentKey];
      if (!parentNode) {
        // This is a placeholder node - we haven't yet encountered this child's parent
        // so we insert this to hold the child. Once we encounter the true parent node, 
        // we will fix up the values in this placeholder node.
        parentNode = { key: container.parentKey, name: '', children: [], type };
        treeMap[container.parentKey] = parentNode;
      }
      parentNode.children?.push(currentNode);
    } else {
      rootNodes.push(currentNode);
    }
    treeMap[container.key] = currentNode;
  });
  return rootNodes;
}


export const kAllLocalType = 'All Local Sources';
export const kDOIType = 'DOI Search';

export const InsertCitationPanel: React.FC<InsertCitationPanelProps> = props => {

  const [citeIds, setCiteIds] = React.useState<string[]>([]);
  const [treeSourceData, setTreeSourceData] = React.useState<SelectTreeNode[]>([]);
  const [selectedNode, setSelectedNode] = React.useState<SelectTreeNode>();
  const [itemData, setItemData] = React.useState<BibliographySource[]>([]);

  const bibMgr = props.bibliographyManager;
  React.useEffect(() => {
    async function loadData() {
      await bibMgr.load(props.ui, props.doc);

      const providers = bibMgr.localProviders();
      const localProviderNodes = providers.map(provider => {
        const node: any = {};
        node.key = provider.key;
        node.name = provider.name;
        node.type = provider.name;
        node.children = toTree(provider.name, provider.containers(props.doc, props.ui));
        return node;
      });

      const treeData: SelectTreeNode[] = [
        {
          key: 'My Sources',
          name: 'My Sources',
          type: kAllLocalType,
          children: localProviderNodes,
          expanded: true
        },
        {
          key: 'DOI',
          name: "Find DOI",
          type: kDOIType,
          children: [],
          expanded: true
        },
      ];
      setTreeSourceData(treeData);
      setSelectedNode(treeData[0]);
    }
    loadData();
  }, []);

  React.useEffect(() => {
    async function loadData() {
      if (selectedNode) {
        const provider = bibMgr.localProviders().find(prov => prov.name === selectedNode.type);
        if (provider) {
          setItemData(provider.itemsForCollection(selectedNode.key));
        }
      }
    }
    loadData();

    // load the right panel
  }, [selectedNode]);

  const style: React.CSSProperties = {
    height: props.height + 'px',
    width: props.width + 'px',
    ...props.style,
  };

  const onClick = () => {
    const cites = [Math.random().toString(36).substring(7), ...citeIds];
    setCiteIds(cites);
    props.onCitationChanged(cites);
  };

  const nodeSelected = (node: SelectTreeNode) => {
    setSelectedNode(node);
  };



  return (
    <div className='pm-cite-panel-container' style={style}>

      <div className='pm-cite-panel-cite-selection'>
        <div className='pm-cite-panel-cite-selection-sources'>
          <SelectTree
            nodes={treeSourceData}
            selectedNode={selectedNode}
            nodeSelected={nodeSelected} />
        </div>

        <div className='pm-cite-panel-cite-selection-items'>
          <FixedSizeList
            height={500}
            width='100%'
            itemCount={itemData.length}
            itemSize={50}
            itemData={itemData}
          >
            {CitationListItem}
          </FixedSizeList>
        </div>
      </div>
      <div className='pm-cite-panel-selected-cites' onClick={onClick}>
        {citeIds}
      </div>
    </div >
  );
};

export const CitationListItem = (props: ListChildComponentProps) => {
  return (<div style={props.style}>
    {props.data[props.index].title}
  </div>);
};