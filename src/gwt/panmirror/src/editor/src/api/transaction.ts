/*
 * transaction.ts
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

import { Transaction, EditorState, Plugin, PluginKey, Selection } from 'prosemirror-state';
import { Node as ProsemirrorNode, Mark, MarkType, Slice } from 'prosemirror-model';
import { ChangeSet } from 'prosemirror-changeset';
import { ReplaceStep, Step, Transform } from 'prosemirror-transform';

import { sliceContentLength } from './slice';
import { traverseNodes, TraverseResult } from './pm-util';
import { NodeWithPos, Predicate } from 'prosemirror-utils';

export const kSetMarkdownTransaction = 'setMarkdown';
export const kAddToHistoryTransaction = 'addToHistory';
export const kFixupTransaction = 'docFixup';
export const kRestoreLocationTransaction = 'restoreLocation';

export type TransactionsFilter = (transactions: Transaction[], oldState: EditorState, newState: EditorState) => boolean;

export type TransactionNodeFilter = (
  node: ProsemirrorNode<any>,
  pos: number,
  parent: ProsemirrorNode<any>,
  index: number,
) => boolean;

export interface AppendTransactionHandler {
  name: string;
  filter?: TransactionsFilter;
  nodeFilter?: TransactionNodeFilter;
  append: (tr: Transaction) => void;
}

// wrapper for transaction that is guaranteed not to modify the position of any 
// nodes in the document (useful for grouping many disparate handlers that arne't
// aware of each other's actions onto the same trasaction)
export class MarkTransaction {
  
  private tr: Transaction;

  constructor(tr: Transaction) {
    this.tr = tr;
  }
  get doc(): ProsemirrorNode {
    return this.tr.doc;
  } 
  get selection(): Selection {
    return this.tr.selection;
  }
  public addMark(from: number, to: number, mark: Mark): this {
    this.tr.addMark(from, to, mark);
    return this;
  }
  public removeMark(from: number, to: number, mark?: Mark | MarkType): this {
    this.tr.removeMark(from, to, mark);
    return this;
  }
  public removeStoredMark(mark: Mark | MarkType): this {
    this.tr.removeStoredMark(mark);
    return this;
  }
  public insertText(text: string, from: number): this {
    this.tr.insertText(text, from, from + text.length);
    return this;
  }
}

export interface AppendMarkTransactionHandler {
  name: string;
  filter: (node: ProsemirrorNode) => boolean;
  append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => void;
}

export function appendMarkTransactionsPlugin(handlers: AppendMarkTransactionHandler[]): Plugin {
  return new Plugin({
    key: new PluginKey('appendMarkTransactions'),

    appendTransaction: (transactions: Transaction[], oldState: EditorState, newState: EditorState) => {
      // skip for selection-only changes
      if (!transactionsDocChanged(transactions)) {
        return;
      }

      // create transaction
      const tr = newState.tr;

      // create markTransaction wrapper
      const markTr = new MarkTransaction(tr);

      forChangedNodes(
        oldState,
        newState,
        node => true,
        (node: ProsemirrorNode, pos: number) => {
          for (const handler of handlers) {
            // get a fresh view of the node
            node = tr.doc.nodeAt(pos)!;

            // call the handler
            if (handler.filter(node)) {
              handler.append(markTr, node, pos);
            }
          }
        },
      );

      // return transaction
      if (tr.docChanged || tr.selectionSet) {
        return tr;
      }
    },
  });
}

export function appendTransactionsPlugin(handlers: AppendTransactionHandler[]): Plugin {
  return new Plugin({
    key: new PluginKey('appendTransactions'),

    appendTransaction: (transactions: Transaction[], oldState: EditorState, newState: EditorState) => {
      // skip for selection-only changes
      if (!transactionsDocChanged(transactions)) {
        return;
      }

      // create transaction
      const tr = newState.tr;

      // compute the changeSet
      if (transactionsAreTypingChange(transactions)) {
        const changeSet = transactionsChangeSet(transactions, oldState, newState);

        // call each handler
        for (const handler of handlers) {
          // track whether there is a change
          let haveChange = false;

          // call filters if we have them
          if (handler.filter || handler.nodeFilter) {
            // first the low-level transaction filter
            if (handler.filter) {
              haveChange = handler.filter(transactions, oldState, newState);
            }

            // if that doesn't detect a change then try the nodeFilter if we have one
            if (!haveChange && handler.nodeFilter) {
              const checkForChange = (
                node: ProsemirrorNode<any>,
                pos: number,
                parent: ProsemirrorNode<any>,
                index: number,
              ) => {
                if (handler.nodeFilter!(node, pos, parent, index)) {
                  haveChange = true;
                  return false;
                }
              };

              for (const change of changeSet.changes) {
                oldState.doc.nodesBetween(change.fromA, change.toA, checkForChange);
                newState.doc.nodesBetween(change.fromB, change.toB, checkForChange);
              }
            }

            // no filters means we should always run (force haveChange to true)
          } else {
            haveChange = true;
          }

          // run the handler if applicable
          if (haveChange) {
            handler.append(tr);
          }
        }

        // run them all if this is a larger change
      } else {
        handlers.forEach(handler => handler.append(tr));
      }

      // return transaction
      if (tr.docChanged || tr.selectionSet) {
        return tr;
      }
    },
  });
}

export function transactionsDocChanged(transactions: Transaction[]) {
  return transactions.some(transaction => transaction.docChanged);
}

export function transactionsChangeSet(transactions: Transaction[], oldState: EditorState, newState: EditorState) {
  let changeSet = ChangeSet.create(oldState.doc);
  for (const transaction of transactions) {
    changeSet = changeSet.addSteps(newState.doc, transaction.mapping.maps);
  }
  return changeSet;
}

export function trTransform(tr: Transaction, f: (transform: Transform) => void): Transaction {
  // create a new transform so we can do position mapping relative
  // to the actions taken here (b/c the transaction might already
  // have other steps so we can't do tr.mapping.map)
  const newActions = new Transform(tr.doc);

  // call the function (passing it a mapping function that uses our newActions)
  f(newActions);

  // copy the contents of newActions to the actual transaction
  for (const step of newActions.steps) {
    tr.step(step);
  }

  // return the transaction for chaining
  return tr;
}


/**
 * Creates a node iterator on top of a transaction (that may or may not already
 * have some steps in it), to be used in a for-of loop. Before each iteration,
 * the iterator will map its internal cursor to account for new operations
 * performed on the transaction since the last time it had control. In other
 * words, this iterator is designed to let you mutate the document you're
 * iterating over.
 *
 * @param tr The transaction that represents our starting point. As we iterate,
 * we'll be responsive to additional actions that are added to this transaction
 * by our caller.
 * @param predicate Only nodes that match the predicate will be returned.
 * @param descend If false, then after a matching node is found, the iterator
 * jumps to the end of that node. If true, then the iterator jumps to the start
 * of that node's content.
 * @param assoc When mapping our internal cursor to account for additional work
 * being done on tr, this assoc should be used.
 */
export function* trFindNodes(tr: Transaction, predicate: Predicate, descend = true, assoc: -1 | 1 = 1) {
  // Tracks the document pos at which our next search should start
  let cursor = 0;
  // Tracks which tr steps we've seen
  let trDepth = tr.steps.length;

  while (true) {
    // Since the last time we yielded (if ever), it's possible the transaction
    // picked up additional steps. If that's so, we need to map our cursor
    // through those new steps.
    for (; trDepth < tr.steps.length; trDepth++) {
      cursor = tr.steps[trDepth].getMap().map(cursor, assoc);
    }

    let result: NodeWithPos | undefined;
    const p1 = cursor;
    traverseNodes(tr.doc, cursor, Infinity, (node, pos) => {
      // Must match predicate, and not be earlier than our last match (note that
      // traverseNodes visits nodes that *partially overlap* with the given
      // from/to positions, not just nodes that are entirely contained within)
      if (predicate(node) && pos >= p1) {
        result = {pos, node};
        return TraverseResult.End;
      }
    });

    if (!result) {
      // No more results found, iteration complete
      break;
    }

    // Before we yield, move the starting position to either the content of
    // this node, or past the end.
    cursor = result.pos + (!result.node.isLeaf && descend ? 1 : result.node.nodeSize);

    // Give control back to the caller
    yield result;
  }
}

export function transactionsHaveChange(
  transactions: Transaction[],
  oldState: EditorState,
  newState: EditorState,
  predicate: (node: ProsemirrorNode<any>, pos: number, parent: ProsemirrorNode<any>, index: number) => boolean,
) {
  // screen out transactions with no doc changes
  if (!transactionsDocChanged(transactions)) {
    return false;
  }

  // function to check for whether we have a change and set a flag if we do
  let haveChange = false;
  const checkForChange = (node: ProsemirrorNode<any>, pos: number, parent: ProsemirrorNode<any>, index: number) => {
    if (predicate(node, pos, parent, index)) {
      haveChange = true;
      return false;
    }
  };

  // for each change in each transaction, check for a node that matches the predicate in either the old or new doc
  const changeSet = transactionsChangeSet(transactions, oldState, newState);

  for (const change of changeSet.changes) {
    oldState.doc.nodesBetween(change.fromA, change.toA, checkForChange);
    newState.doc.nodesBetween(change.fromB, change.toB, checkForChange);
    if (haveChange) {
      break;
    }
  }

  return haveChange;
}

export function forChangedNodes(
  oldState: EditorState | null,
  newState: EditorState,
  predicate: (node: ProsemirrorNode) => boolean,
  f: (node: ProsemirrorNode, pos: number) => boolean | void,
) {
  let complete = false;
  const handler = (node: ProsemirrorNode, pos: number) => {
    if (complete) {
      return;
    }

    if (!predicate || predicate(node)) {
      if (f(node, pos) === false) {
        complete = true;
      }
    }
  };

  if (!oldState) {
    newState.doc.descendants(handler);
  } else if (oldState.doc !== newState.doc) {
    changedDescendants(oldState.doc, newState.doc, 0, handler);
  }
}

// Helper for iterating through the nodes in a document that changed
// compared to the given previous document. Useful for avoiding
// duplicate work on each transaction.
// from: https://github.com/ProseMirror/prosemirror-tables/blob/master/src/fixtables.js
function changedDescendants(
  old: ProsemirrorNode,
  cur: ProsemirrorNode,
  offset: number,
  f: (node: ProsemirrorNode, pos: number) => void,
) {
  const oldSize = old.childCount;
  const curSize = cur.childCount;
  outer: for (let i = 0, j = 0; i < curSize; i++) {
    const child = cur.child(i);
    for (let scan = j, e = Math.min(oldSize, i + 3); scan < e; scan++) {
      if (old.child(scan) === child) {
        j = scan + 1;
        offset += child.nodeSize;
        continue outer;
      }
    }
    f(child, offset);
    if (j < oldSize && old.child(j).sameMarkup(child)) {
      changedDescendants(old.child(j), child, offset + 1, f);
    } else {
      child.nodesBetween(0, child.content.size, f, offset + 1);
    }
    offset += child.nodeSize;
  }
}

export function transactionsAreTypingChange(transactions: Transaction[]) {
  if (
    transactions.length === 1 &&
    transactions[0].steps.length === 1 &&
    transactions[0].steps[0] instanceof ReplaceStep
  ) {
    // step to examine
    const step: any = transactions[0].steps[0];

    // insert single chraracter or new empty slice (e.g. from enter after a paragraph)
    if (step.from === step.to && sliceContentLength(step.slice) <= 1) {
      return true;
    }

    // remove single character
    if (Math.abs(step.from - step.to) === 1 && step.slice.content.size === 0) {
      return true;
    }
  }

  return false;
}

export interface RangeStep extends Step {
  from: number;
  to: number;
}

export interface SliceStep extends RangeStep {
  slice: Slice;
}
