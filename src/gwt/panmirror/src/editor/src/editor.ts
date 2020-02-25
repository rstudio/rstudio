/*
 * editor.ts
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

import { inputRules } from 'prosemirror-inputrules';
import { keydownHandler } from 'prosemirror-keymap';
import {
  MarkSpec,
  Node as ProsemirrorNode,
  NodeSpec,
  Schema,
  DOMSerializer,
  DOMParser,
  ParseOptions,
} from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { findChildren, setTextSelection } from 'prosemirror-utils';
import 'prosemirror-view/style/prosemirror.css';

import { EditorOptions } from './api/options';
import { ProsemirrorCommand, CommandFn, EditorCommand } from './api/command';
import { PandocMark, markIsActive } from './api/mark';
import { PandocNode } from './api/node';
import { EditorUI, attrPropsToInput, attrInputToProps, AttrProps, AttrEditInput } from './api/ui';
import { Extension } from './api/extension';
import { ExtensionManager, initExtensions } from './extensions';
import { PandocEngine } from './api/pandoc';
import {
  PandocFormat,
  resolvePandocFormat,
  splitPandocFormatString,
  PandocFormatComment,
  resolvePandocFormatComment,
  pandocFormatCommentFromCode,
} from './api/pandoc_format';
import { baseKeysPlugin } from './api/basekeys';
import { appendTransactionsPlugin, appendMarkTransactionsPlugin } from './api/transaction';
import { EditorOutline } from './api/outline';
import { EditingLocation, getEditingLocation, restoreEditingLocation } from './api/location';

import { getTitle, setTitle } from './nodes/yaml_metadata/yaml_metadata-title';

import { getOutline } from './behaviors/outline';
import {
  FindOptions,
  find,
  matchCount,
  selectFirst,
  selectNext,
  selectPrevious,
  replace,
  replaceAll,
  clear,
  selectCurrent,
} from './behaviors/find';

import { getFormatComment } from './behaviors/format_comment';

import { PandocConverter, PandocWriterOptions } from './pandoc/converter';

import { applyTheme, defaultTheme, EditorTheme } from './theme';

import './styles/frame.css';
import './styles/styles.css';

const kMac = typeof navigator !== 'undefined' ? /Mac/.test(navigator.platform) : false;

export interface EditorContext {
  readonly pandoc: PandocEngine;
  readonly format: string;
  readonly ui: EditorUI;
  readonly hooks?: EditorHooks;
  readonly extensions?: readonly Extension[];
}

export interface EditorHooks {
  isEditable?: () => boolean;
}

export interface EditorKeybindings {
  [id: string]: string[];
}

export enum EditorEvents {
  Update = 'update',
  OutlineChange = 'outlineChange',
  SelectionChange = 'selectionChange',
}

export interface EditorSelection {
  from: number;
  to: number;
}

export interface EditorFindReplace {
  find: (term: string, options: FindOptions) => boolean;
  matches: () => number;
  selectFirst: () => boolean;
  selectCurrent: () => boolean;
  selectNext: () => boolean;
  selectPrevious: () => boolean;
  replace: (text: string) => boolean;
  replaceAll: (text: string) => boolean;
  clear: () => boolean;
}

export { EditorCommandId as EditorCommands } from './api/command';

export class UITools {
  public attrPropsToInput(attr: AttrProps) {
    return attrPropsToInput(attr);
  }
  public attrInputToProps(input: AttrEditInput) {
    return attrInputToProps(input);
  }
}

const keybindingsPlugin = new PluginKey('keybindings');

export class Editor {
  // core context passed from client
  private readonly parent: HTMLElement;
  private readonly context: EditorContext;

  // options (derived from defaults + config)
  private readonly options: EditorOptions;

  // pandoc format used for reading and writing markdown
  // note that this can change from what is specified at
  // construction time based on magic comments being
  // provided within the document
  private pandocFormat: PandocFormat;

  // core prosemirror state/behaviors
  private readonly extensions: ExtensionManager;
  private readonly schema: Schema;
  private state: EditorState;
  private readonly view: EditorView;
  private readonly pandocConverter: PandocConverter;

  // setting via setKeybindings forces reconfiguration of EditorState
  // with plugins recreated
  private keybindings: EditorKeybindings;

  // event sinks
  private readonly events: ReadonlyMap<string, Event>;

  public static async create(
    parent: HTMLElement,
    context: EditorContext,
    options: EditorOptions,
    markdown?: string,
  ): Promise<Editor> {
    // provide default options
    options = {
      autoFocus: false,
      spellCheck: true,
      codemirror: true,
      autoLink: false,
      braceMatching: true,
      rmdCodeChunks: false,
      formatComment: true,
      ...options,
    };

    // default format to what is specified in the config
    let format = context.format;

    // if markdown was specified then try to read the format from it
    if (markdown && options.formatComment) {
      format = resolvePandocFormatComment(pandocFormatCommentFromCode(markdown), format);
    }

    // resolve the format
    const pandocFmt = await resolvePandocFormat(context.pandoc, format);

    // create editor
    const editor = new Editor(parent, context, options, pandocFmt);

    // set initial markdown if specified
    if (markdown) {
      await editor.setMarkdown(markdown, false);
    }

    // return editor
    return Promise.resolve(editor);
  }

  private constructor(parent: HTMLElement, context: EditorContext, options: EditorOptions, pandocFormat: PandocFormat) {
    // initialize references
    this.parent = parent;
    this.context = context;
    this.options = options;
    this.keybindings = {};
    this.pandocFormat = pandocFormat;

    // initialize custom events
    this.events = this.initEvents();

    // create extensions
    this.extensions = this.initExtensions();

    // create schema
    this.schema = this.initSchema();

    // create state
    this.state = EditorState.create({
      schema: this.schema,
      doc: this.emptyDoc(),
      plugins: this.createPlugins(),
    });

    // create view
    this.view = new EditorView(this.parent, {
      state: this.state,
      dispatchTransaction: this.dispatchTransaction.bind(this),
      domParser: new EditorDOMParser(this.schema),
    });

    // add proportinal font class to parent
    this.parent.classList.add('pm-proportional-font');

    // apply default theme
    applyTheme(defaultTheme());

    // apply fixups when the window size changes
    this.applyLayoutFixups = this.applyLayoutFixups.bind(this);
    window.addEventListener('resize', this.applyLayoutFixups);

    // create pandoc translator
    this.pandocConverter = new PandocConverter(this.schema, this.extensions, context.pandoc);

    // focus editor immediately if requested
    if (this.options.autoFocus) {
      setTimeout(() => {
        this.focus();
      }, 10);
    }

    // disale spellcheck if requested
    if (!this.options.spellCheck) {
      this.parent.setAttribute('spellcheck', 'false');
    }
  }

  public destroy() {
    document.removeEventListener('resize', this.applyLayoutFixups);
    this.view.destroy();
  }

  public subscribe(event: string, handler: VoidFunction): VoidFunction {
    if (!this.events.has(event)) {
      const valid = Array.from(this.events.keys()).join(', ');
      throw new Error(`Unknown event ${event}. Valid events are ${valid}`);
    }
    this.parent.addEventListener(event, handler);
    return () => {
      this.parent.removeEventListener(event, handler);
    };
  }

  public setTitle(title: string) {
    const tr = setTitle(this.state, title);
    if (tr) {
      this.view.dispatch(tr);
    }
  }

  public async setMarkdown(markdown: string, emitUpdate = true): Promise<boolean> {
    // update format from source code magic comments
    await this.updatePandocFormat(pandocFormatCommentFromCode(markdown));

    // get the doc
    const doc = await this.pandocConverter.toProsemirror(markdown, this.pandocFormat.fullName);

    // re-initialize editor state
    this.state = EditorState.create({
      schema: this.state.schema,
      doc,
      plugins: this.state.plugins,
    });
    this.view.updateState(this.state);

    // apply layout fixups
    this.applyLayoutFixups();

    // notify listeners if requested
    if (emitUpdate) {
      this.emitEvent(EditorEvents.Update);
      this.emitEvent(EditorEvents.OutlineChange);
      this.emitEvent(EditorEvents.SelectionChange);
    }

    return true;
  }

  public async getMarkdown(options: PandocWriterOptions): Promise<string> {
    // get current format comment
    const formatComment = getFormatComment(this.state);

    // update format from source code magic comments
    await this.updatePandocFormat(formatComment);

    // override wrapColumn option if it was specified
    if (this.options.formatComment) {
      options.wrapColumn = formatComment.fillColumn || options.wrapColumn;
    }

    // apply layout fixups
    this.applyLayoutFixups();

    // do the conversion
    return this.pandocConverter.fromProsemirror(this.state.doc, this.pandocFormat.fullName, options);
  }

  public getHTML(): string {
    const div = document.createElement('div');
    const fragment = DOMSerializer.fromSchema(this.state.schema).serializeFragment(this.state.doc.content);
    div.appendChild(fragment);
    return div.innerHTML;
  }

  public getTitle() {
    return getTitle(this.state);
  }

  public getSelection(): EditorSelection {
    const { from, to } = this.state.selection;
    return { from, to };
  }

  public getEditingLocation(): EditingLocation {
    return getEditingLocation(this.view);
  }

  public restoreEditingLocation(location: EditingLocation) {
    restoreEditingLocation(this.view, location);
  }

  public getOutline(): EditorOutline {
    return getOutline(this.state);
  }

  public getFindReplace(): EditorFindReplace {
    return {
      find: (term: string, options: FindOptions) => find(this.view, term, options),
      matches: () => matchCount(this.view),
      selectCurrent: () => selectCurrent(this.view),
      selectFirst: () => selectFirst(this.view),
      selectNext: () => selectNext(this.view),
      selectPrevious: () => selectPrevious(this.view),
      replace: (text: string) => replace(this.view, text),
      replaceAll: (text: string) => replaceAll(this.view, text),
      clear: () => clear(this.view),
    };
  }

  public focus() {
    this.view.focus();
  }

  public blur() {
    (this.view.dom as HTMLElement).blur();
  }

  public navigate(id: string) {
    const result = findChildren(this.state.doc, node => id === node.attrs.navigation_id, true);
    if (result.length) {
      const target = result[0];

      // set selection
      this.view.dispatch(setTextSelection(target.pos)(this.state.tr));

      // scroll to selection
      const node = this.view.nodeDOM(target.pos);
      if (node instanceof HTMLElement) {
        node.scrollIntoView({ behavior: 'smooth' });
      }
    }
  }

  public resize() {
    this.applyLayoutFixups();
  }

  public enableDevTools(initFn: (view: EditorView, stateClass: any) => void) {
    initFn(this.view, { EditorState });
  }

  public commands(): EditorCommand[] {
    // get keybindings (merge user + default)
    const commandKeys = this.commandKeys();

    return this.extensions.commands(this.schema, this.context.ui, kMac).map((command: ProsemirrorCommand) => {
      return {
        id: command.id,
        keymap: commandKeys[command.id],
        isActive: () => command.isActive(this.state),
        isEnabled: () => command.isEnabled(this.state),
        execute: () => {
          command.execute(this.state, this.view.dispatch, this.view);
          if (command.keepFocus) {
            this.focus();
          }
        },
      };
    });
  }

  public applyTheme(theme: EditorTheme) {
    applyTheme(theme);
  }

  public setKeybindings(keyBindings: EditorKeybindings) {
    // validate that all of these keys can be rebound

    this.keybindings = keyBindings;
    this.state = this.state.reconfigure({
      schema: this.state.schema,
      plugins: this.createPlugins(),
    });
  }

  public getPandocFormat() {
    return this.pandocFormat;
  }

  private async updatePandocFormat(formatComment: PandocFormatComment) {
    // don't do it if our options tell us not to
    if (!this.options.formatComment) {
      return;
    }

    // start with existing format
    const existingFormat = splitPandocFormatString(this.pandocFormat.fullName);

    // determine the target format (this is either from a format comment
    // or alternatively based on the default format)
    const targetFormat = splitPandocFormatString(resolvePandocFormatComment(formatComment, this.context.format));

    // if this differs from the one in the source code, then update the format
    if (targetFormat.format !== existingFormat.format || targetFormat.options !== existingFormat.options) {
      const format = targetFormat.format + targetFormat.options;
      this.pandocFormat = await resolvePandocFormat(this.context.pandoc, format);
    }
  }

  private dispatchTransaction(tr: Transaction) {
    // track previous outline
    const previousOutline = getOutline(this.state);

    // apply the transaction
    this.state = this.state.apply(tr);
    this.view.updateState(this.state);

    // notify listeners of selection change
    this.emitEvent(EditorEvents.SelectionChange);

    // notify listeners of updates
    if (tr.docChanged) {
      // fire updated
      this.emitEvent(EditorEvents.Update);

      // fire outline changed if necessary
      if (getOutline(this.state) !== previousOutline) {
        this.emitEvent(EditorEvents.OutlineChange);
      }
    }
  }

  private emitEvent(name: string) {
    const event = this.events.get(name);
    if (event) {
      this.parent.dispatchEvent(event);
    }
  }

  private initEvents(): ReadonlyMap<string, Event> {
    const events = new Map<string, Event>();
    events.set(EditorEvents.Update, new Event(EditorEvents.Update));
    events.set(EditorEvents.OutlineChange, new Event(EditorEvents.OutlineChange));
    events.set(EditorEvents.SelectionChange, new Event(EditorEvents.SelectionChange));
    return events;
  }

  private initExtensions() {
    return initExtensions(this.options, this.context.extensions, this.pandocFormat.extensions);
  }

  private initSchema(): Schema {
    // build in doc node + nodes from extensions
    const nodes: { [name: string]: NodeSpec } = {
      doc: {
        content: 'body notes',
      },

      body: {
        content: 'block+',
        isolating: true,
        parseDOM: [{ tag: 'div[class*="body"]' }],
        toDOM() {
          return ['div', { class: 'body pm-cursor-color pm-text-color pm-background-color pm-content' }, 0];
        },
      },

      notes: {
        content: 'note*',
        parseDOM: [{ tag: 'div[class*="notes"]' }],
        toDOM() {
          return ['div', { class: 'notes pm-cursor-color pm-text-color pm-background-color pm-content' }, 0];
        },
      },

      note: {
        content: 'block+',
        attrs: {
          ref: {},
          number: { default: 1 },
        },
        isolating: true,
        parseDOM: [
          {
            tag: 'div[class*="note"]',
            getAttrs(dom: Node | string) {
              const el = dom as Element;
              return {
                ref: el.getAttribute('data-ref'),
              };
            },
          },
        ],
        toDOM(node: ProsemirrorNode) {
          return [
            'div',
            { 'data-ref': node.attrs.ref, class: 'note pm-footnote-body', 'data-number': node.attrs.number },
            0,
          ];
        },
      },
    };
    this.extensions.pandocNodes().forEach((node: PandocNode) => {
      nodes[node.name] = node.spec;
    });

    // marks from extensions
    const marks: { [name: string]: MarkSpec } = {};
    this.extensions.pandocMarks().forEach((mark: PandocMark) => {
      marks[mark.name] = mark.spec;
    });

    // return schema
    return new Schema({
      nodes,
      marks,
    });
  }

  private createPlugins(): Plugin[] {
    return [
      baseKeysPlugin(this.extensions.baseKeys(this.schema)),
      this.keybindingsPlugin(),
      appendTransactionsPlugin(this.extensions.appendTransactions(this.schema)),
      appendMarkTransactionsPlugin(this.extensions.appendMarkTransactions(this.schema)),
      ...this.extensions.plugins(this.schema, this.context.ui, kMac),
      this.inputRulesPlugin(),
      this.editablePlugin(),
    ];
  }

  private editablePlugin() {
    const hooks = this.context.hooks || {};
    return new Plugin({
      key: new PluginKey('editable'),
      props: {
        editable: hooks.isEditable || (() => true),
      },
    });
  }

  private inputRulesPlugin() {
    // see which marks disable input rules
    const disabledMarks: string[] = [];
    this.extensions.pandocMarks().forEach((mark: PandocMark) => {
      if (mark.noInputRules) {
        disabledMarks.push(mark.name);
      }
    });

    // create the defautl inputRules plugin
    const plugin = inputRules({ rules: this.extensions.inputRules(this.schema) });
    const handleTextInput = plugin.props.handleTextInput!;

    // override to disable input rules as requested
    // https://github.com/ProseMirror/prosemirror-inputrules/commit/b4bf67623aa4c4c1e096c20aa649c0e63751f337
    plugin.props.handleTextInput = (view: EditorView<any>, from: number, to: number, text: string) => {
      for (const mark of disabledMarks) {
        if (markIsActive(view.state, this.schema.marks[mark])) {
          return false;
        }
      }
      return handleTextInput(view, from, to, text);
    };
    return plugin;
  }

  private keybindingsPlugin(): Plugin {
    // get keybindings (merge user + default)
    const commandKeys = this.commandKeys();

    // command keys from extensions
    const pluginKeys: { [key: string]: CommandFn } = {};
    const commands = this.extensions.commands(this.schema, this.context.ui, kMac);
    commands.forEach((command: ProsemirrorCommand) => {
      const keys = commandKeys[command.id];
      if (keys) {
        keys.forEach((key: string) => {
          pluginKeys[key] = command.execute;
        });
      }
    });

    // return plugin
    return new Plugin({
      key: keybindingsPlugin,
      props: {
        handleKeyDown: keydownHandler(pluginKeys),
      },
    });
  }

  private commandKeys(): { [key: string]: readonly string[] } {
    // start with keys provided within command definitions
    const commands = this.extensions.commands(this.schema, this.context.ui, kMac);
    const defaultKeys = commands.reduce((keys: { [key: string]: readonly string[] }, command: ProsemirrorCommand) => {
      keys[command.id] = command.keymap;
      return keys;
    }, {});

    // merge with user keybindings
    return {
      ...defaultKeys,
      ...this.keybindings,
    };
  }

  private applyLayoutFixups() {
    let tr = this.state.tr;
    tr = this.extensionLayoutFixups(tr);
    if (tr.docChanged) {
      tr.setMeta('addToHistory', false);
      this.view.dispatch(tr);
    }
  }

  private extensionLayoutFixups(tr: Transaction) {
    this.extensions.layoutFixups(this.schema, this.view).forEach(fixup => {
      tr = fixup(tr);
    });
    return tr;
  }

  private emptyDoc(): ProsemirrorNode {
    return this.schema.nodeFromJSON({
      type: 'doc',
      content: [
        { type: 'body', content: [{ type: 'paragraph' }] },
        { type: 'notes', content: [] },
      ],
    });
  }
}

// custom DOMParser that preserves all whitespace (required by display math marks)
class EditorDOMParser extends DOMParser {
  constructor(schema: Schema) {
    super(schema, DOMParser.fromSchema(schema).rules);
  }
  public parse(dom: Node, options?: ParseOptions) {
    return super.parse(dom, { ...options, preserveWhitespace: 'full' });
  }

  public parseSlice(dom: Node, options?: ParseOptions) {
    return super.parseSlice(dom, { ...options, preserveWhitespace: 'full' });
  }
}
