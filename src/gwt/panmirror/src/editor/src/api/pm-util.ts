/*
 * pm-util.ts
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

import { Node } from "prosemirror-model";
import { Predicate, NodeWithPos } from "prosemirror-utils";

export enum TraverseResult {
  // Descend into children, if any
  Descend,
  // Continue to next sibling, skipping children
  Next,
  // Skip remaining siblings, continue at parent's sibling
  Up,
  // Stop traversal entirely
  End
}
export type TraverseFn = (node: Node, pos: number, parent: Node, index: number) => (TraverseResult | undefined);

// Traverse a node/doc within a span of positions. The given span need only
// overlap (not contain) a node for it to be included in the iteration.
//
// Versus other forms of iteration in prosemirror(-utils), traverseNodes is
// lazy and also provides a greater degree of flow control (see TraverseResult).
//
// It's OK for `from` to be negative and `to` to be larger than node.nodeSize.
//
// See also: Node.nodesBetween, prosemirror-utils' findChildren. 
export function traverseNodes(node: Node, from: number, to: number, fn: TraverseFn, debug = false) {
  if (debug) {
    // tslint:disable-next-line: no-console
    console.log(`Traversing ${node.type.name} in the range [${from}-${to}]`);
  }
  traverseNodesImpl(node, from, to, fn, 0, null, debug);
}

function traverseNodesImpl(node: Node, from: number, to: number, fn: TraverseFn,
  offset: number, parent: Node | null, debug: boolean) : boolean {
  let pos = 0;

  for (let i = 0; i < node.content.childCount; i++) {
    const child = node.content.child(i);  
    const endPos = pos + child.nodeSize;
    
    if (debug) {
      // tslint:disable-next-line: no-console
      console.log(`${child.type.name} [${offset+pos}-${offset+endPos}]`);
      // tslint:disable-next-line: no-console
      console.log(`offset: ${offset}, condition: ${pos} < ${to} && ${endPos} > ${from}`);
    }

    // Don't look at nodes that fall outside of the desired range
    if (pos < to && endPos > from) {
      // Invoke the callback; the default action is Descend.
      const result = fn(child, offset + pos, node, i) || TraverseResult.Descend;

      if (result === TraverseResult.Descend) {
        // The from, to, and offset all have to be modified to be relative
        // to the child.
        if (child.content.childCount) {
          const childContentPos = pos + 1;
          if (!traverseNodesImpl(child, from - childContentPos, to - childContentPos, fn, offset + childContentPos, node, debug)) {
            // Stop all traversal
            return false;
          }
        }
      } else if (result === TraverseResult.Next) {
        // Do nothing
      } else if (result === TraverseResult.Up) {
        // Stop iterating at this level, but let parent keep going
        return true;
      } else if (result === TraverseResult.End) {
        // Stop all traversal
        return false;
      } else {
        throw new Error("Unexpected TraverseResult value");
      }
    }

    pos = endPos;
  }

  return true;
}

export function findOneNode(node: Node, from: number, to: number, predicate: Predicate) {
  let result: Node | null = null;
  traverseNodes(node, from, to, x => {
    if (predicate(x)) {
      result = x;
      return TraverseResult.End;
    }
  });
  return result;
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
export function* trFindNodes(tr: Transaction, predicate: Predicate, descend = false, assoc: -1 | 1 = 1) {
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
