declare module 'prosemirror-utils' {
  import { Node as ProsemirrorNode, Schema, NodeType, Mark, MarkType, ResolvedPos, Fragment } from 'prosemirror-model';
  import { Selection, Transaction } from 'prosemirror-state';

  export type Predicate = (node: ProsemirrorNode) => boolean;

  export type DomAtPos = (pos: number) => { node: Node; offset: number };

  export type ContentNodeWithPos = { pos: number; start: number; depth: number; node: ProsemirrorNode };

  export type NodeWithPos = { pos: number; node: ProsemirrorNode };

  export type CellTransform = (cell: ContentNodeWithPos, tr: Transaction) => Transaction;

  export type MovementOptions = { tryToFit: boolean; direction?: -1 | 0 | 1 };

  // Selection
  export function findParentNode(predicate: Predicate): (selection: Selection) => ContentNodeWithPos | undefined;

  export function findParentNodeClosestToPos($pos: ResolvedPos, predicate: Predicate): ContentNodeWithPos | undefined;

  export function findParentDomRef(
    predicate: Predicate,
    domAtPos: DomAtPos,
  ): (selection: Selection) => Node | undefined;

  export function hasParentNode(predicate: Predicate): (selection: Selection) => boolean;

  export function findParentNodeOfType(
    nodeType: NodeType | NodeType[],
  ): (selection: Selection) => ContentNodeWithPos | undefined;

  export function findParentNodeOfTypeClosestToPos(
    $pos: ResolvedPos,
    nodeType: NodeType | NodeType[],
  ): ContentNodeWithPos | undefined;

  export function hasParentNodeOfType(nodeType: NodeType | NodeType[]): (selection: Selection) => boolean;

  export function findParentDomRefOfType(
    nodeType: NodeType | NodeType[],
    domAtPos: DomAtPos,
  ): (selection: Selection) => Node | undefined;

  export function findSelectedNodeOfType(
    nodeType: NodeType | NodeType[],
  ): (selection: Selection) => ContentNodeWithPos | undefined;

  export function isNodeSelection(selection: Selection): boolean;

  export function findPositionOfNodeBefore(selection: Selection): number | undefined;

  export function findDomRefAtPos(position: number, domAtPos: DomAtPos): Node;

  // Node
  export function flatten(node: ProsemirrorNode, descend?: boolean): NodeWithPos[];

  export function findChildren(node: ProsemirrorNode, predicate: Predicate, descend?: boolean): NodeWithPos[];

  export function findTextNodes(node: ProsemirrorNode, descend?: boolean): NodeWithPos[];

  export function findInlineNodes(node: ProsemirrorNode, descend?: boolean): NodeWithPos[];

  export function findBlockNodes(node: ProsemirrorNode, descend?: boolean): NodeWithPos[];

  export function findChildrenByAttr(node: ProsemirrorNode, predicate: Predicate, descend?: boolean): NodeWithPos[];

  export function findChildrenByType(node: ProsemirrorNode, nodeType: NodeType, descend?: boolean): NodeWithPos[];

  export function findChildrenByMark(node: ProsemirrorNode, markType: MarkType, descend?: boolean): NodeWithPos[];

  export function contains(node: ProsemirrorNode, nodeType: NodeType): boolean;

  // Table
  export function findTable(selection: Selection): ContentNodeWithPos | undefined;

  export function isCellSelection(selection: Selection): boolean;

  export function isColumnSelected(columnIndex: number): (selection: Selection) => boolean;

  export function isRowSelected(rowIndex: number): (selection: Selection) => boolean;

  export function isTableSelected(selection: Selection): boolean;

  export function getCellsInColumn(
    columnIndex: number | number[],
  ): (selection: Selection) => ContentNodeWithPos[] | undefined;

  export function getCellsInRow(
    rowIndex: number | number[],
  ): (selection: Selection) => ContentNodeWithPos[] | undefined;

  export function getCellsInTable(selection: Selection): ContentNodeWithPos[] | undefined;

  export function selectColumn(columnIndex: number, expand?: boolean): (tr: Transaction) => Transaction;

  export function selectRow(rowIndex: number, expand?: boolean): (tr: Transaction) => Transaction;

  export function selectTable(tr: Transaction): Transaction;

  export function emptyCell(cell: ContentNodeWithPos, schema: Schema): (tr: Transaction) => Transaction;

  export function addColumnAt(columnIndex: number): (tr: Transaction) => Transaction;

  export function moveRow(
    originRowIndex: number,
    targetRowIndex: number,
    options?: MovementOptions,
  ): (tr: Transaction) => Transaction;

  export function moveColumn(
    originColumnIndex: number,
    targetColumnIndex: number,
    options?: MovementOptions,
  ): (tr: Transaction) => Transaction;

  export function addRowAt(rowIndex: number, clonePreviousRow?: boolean): (tr: Transaction) => Transaction;

  export function cloneRowAt(cloneRowIndex: number): (tr: Transaction) => Transaction;

  export function removeColumnAt(columnIndex: number): (tr: Transaction) => Transaction;

  export function removeRowAt(rowIndex: number): (tr: Transaction) => Transaction;

  export function removeSelectedColumns(tr: Transaction): Transaction;

  export function removeSelectedRows(tr: Transaction): Transaction;

  export function removeTable(tr: Transaction): Transaction;

  export function removeColumnClosestToPos($pos: ResolvedPos): (tr: Transaction) => Transaction;

  export function removeRowClosestToPos($pos: ResolvedPos): (tr: Transaction) => Transaction;

  export function forEachCellInColumn(
    columnIndex: number,
    cellTransform: CellTransform,
    moveCursorToLastCell?: boolean,
  ): (tr: Transaction) => Transaction;

  export function forEachCellInRow(
    rowIndex: number,
    cellTransform: CellTransform,
    moveCursorToLastCell?: boolean,
  ): (tr: Transaction) => Transaction;

  export function setCellAttrs(cell: ContentNodeWithPos, attrs: Object): (tr: Transaction) => Transaction;

  export function findCellClosestToPos($pos: ResolvedPos): ContentNodeWithPos | undefined;

  export function findCellRectClosestToPos(
    $pos: ResolvedPos,
  ): { top: number; bottom: number; left: number; right: number } | undefined;

  export function createTable(
    schema: Schema,
    rowsCount?: number,
    colsCount?: number,
    withHeaderRow?: boolean,
    cellContent?: Node,
  ): ProsemirrorNode;

  export function getSelectionRect(
    selection: Selection,
  ): { top: number; bottom: number; left: number; right: number } | undefined;

  export function getSelectionRangeInColumn(
    columnIndex: number,
  ): (tr: Transaction) => { $anchor: ResolvedPos; $head: ResolvedPos; indexes: number[] };

  export function getSelectionRangeInRow(
    rowIndex: number,
  ): (tr: Transaction) => { $anchor: ResolvedPos; $head: ResolvedPos; indexes: number[] };

  // Transforms
  export function removeParentNodeOfType(nodeType: NodeType | NodeType[]): (tr: Transaction) => Transaction;

  export function replaceParentNodeOfType(
    nodeType: NodeType | NodeType[],
    node: ProsemirrorNode,
  ): (tr: Transaction) => Transaction;

  export function removeSelectedNode(tr: Transaction): Transaction;

  export function replaceSelectedNode(node: ProsemirrorNode): (tr: Transaction) => Transaction;

  export function convertTableNodeToArrayOfRows(tableNode: ProsemirrorNode): ProsemirrorNode[];

  export function convertArrayOfRowsToTableNode(
    tableNode: ProsemirrorNode,
    tableArray: ProsemirrorNode[],
  ): ProsemirrorNode;

  export function canInsert($pos: ResolvedPos, node: ProsemirrorNode | Fragment): boolean;

  export function safeInsert(
    node: ProsemirrorNode | Fragment,
    position?: number,
    tryToReplace?: boolean,
  ): (tr: Transaction) => Transaction;

  export function setParentNodeMarkup(
    nodeType: NodeType | NodeType[],
    type?: NodeType | null,
    attrs?: { [key: string]: any } | null,
    marks?: Mark[],
  ): (tr: Transaction) => Transaction;

  export function selectParentNodeOfType(nodeType: NodeType | NodeType[]): (tr: Transaction) => Transaction;

  export function removeNodeBefore(tr: Transaction): Transaction;

  export function setTextSelection(position: number, dir?: number): (tr: Transaction) => Transaction;
}
