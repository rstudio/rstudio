import { InputRule } from 'prosemirror-inputrules';
import { Schema } from 'prosemirror-model';
import { Plugin, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { ProsemirrorCommand } from './command';
import { PandocMark } from './mark';
import { PandocNode } from './node';
import { EditorUI } from './ui';
import { BaseKeyBinding } from './basekeys';
import { AppendTransactionHandler, AppendMarkTransactionHandler } from './transaction';
import { EditorOptions } from './options';
import { PandocExtensions } from './pandoc';

export interface Extension {
  marks?: PandocMark[];
  nodes?: PandocNode[];
  baseKeys?: (schema: Schema) => readonly BaseKeyBinding[];
  inputRules?: (schema: Schema) => readonly InputRule[];
  commands?: (schema: Schema, ui: EditorUI, mac: boolean) => readonly ProsemirrorCommand[];
  plugins?: (schema: Schema, ui: EditorUI, mac: boolean) => readonly Plugin[];
  appendTransaction?: (schema: Schema) => readonly AppendTransactionHandler[];
  appendMarkTransaction?: (schema: Schema) => readonly AppendMarkTransactionHandler[];
  layoutFixups?: (schema: Schema, view: EditorView) => Readonly<Array<(tr: Transaction) => Transaction>>;
}

// return an extension conditional on the active EditorOptions
export type ExtensionFn = (pandocExtensions: PandocExtensions, options: EditorOptions) => Extension | null;

// create an ExtensionFn for a given extension and format option that must be enabled
export function extensionIfEnabled(extension: Extension, name: string | string[]) {
  return (pandocExtensions: PandocExtensions) => {
    // match single extension name
    if (typeof name === 'string') {
      if (pandocExtensions[name]) {
        return extension;
      }

      // match any one of several names
    } else if (Array.isArray(name)) {
      for (let i = 0; i < name.length; i++) {
        if (pandocExtensions[name[i]]) {
          return extension;
        }
      }
    }

    // didn't find match
    return null;
  };
}
