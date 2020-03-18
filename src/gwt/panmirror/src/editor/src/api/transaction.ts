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
import { ReplaceStep, Step } from 'prosemirror-transform';

import { sliceContentLength } from './slice';

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

export interface MarkTransaction {
  doc: ProsemirrorNode;
  selection: Selection;
  addMark(from: number, to: number, mark: Mark): this;
  removeMark(from: number, to: number, mark?: Mark | MarkType): this;
  removeStoredMark(mark: Mark | MarkType): this;
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
              handler.append(tr, node, pos);
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
