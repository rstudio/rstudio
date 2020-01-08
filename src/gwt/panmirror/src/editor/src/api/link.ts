import { Node as ProsemirrorNode } from 'prosemirror-model';
import { findChildren, findChildrenByType } from 'prosemirror-utils';

export enum LinkType {
  URL,
  Heading,
  ID,
}

export interface LinkCapabilities {
  headings: boolean;
  attributes: boolean;
}

export interface LinkTargets {
  readonly ids: string[];
  readonly headings: LinkHeadingTarget[];
}

export interface LinkHeadingTarget {
  readonly level: number;
  readonly text: string;
}

export async function linkTargets(doc: ProsemirrorNode) {
  const ids = findChildren(doc, node => !!node.attrs.id).map(value => value.node.attrs.id);

  const headings = findChildrenByType(doc, doc.type.schema.nodes.heading).map(heading => ({
    level: heading.node.attrs.level,
    text: heading.node.textContent,
  }));

  return {
    ids,
    headings,
  };
}
