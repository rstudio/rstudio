// Type definitions for prosemirror-changeset 2.0
// Project: https://github.com/ProseMirror/prosemirror-changeset
// Definitions by: Bradley Ayers <https://github.com/bradleyayers>
//                 David Hahn <https://github.com/davidka>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped
// TypeScript Version: 2.1

declare module 'prosemirror-changeset' {
  import { Node } from 'prosemirror-model';
  import { StepMap } from 'prosemirror-transform';

  export class Span {
    length: number;
    data: any;
  }
  export class Change {
    fromA: number;
    toA: number;
    fromB: number;
    toB: number;
    deleted: Span[];
    inserted: Span[];
  }
  export class ChangeSet {
    changes: Change[];
    addSteps(newDoc: Node, maps: StepMap[], data?: any[] | any): ChangeSet;
    startDoc: Node;
    map(f: (range: Change) => any): ChangeSet;
    changedRange(b: ChangeSet, maps?: StepMap[]): { from: number; to: number } | null | undefined;
    static create(doc: Node, combine?: (a: any, b: any) => any): ChangeSet;
  }
  export function simplifyChanges(changes: Change[], doc: Node): Change[];
}
