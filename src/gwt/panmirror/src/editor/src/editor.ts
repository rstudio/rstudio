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
import { EditorUI } from './api/ui';
import { Extension } from './api/extension';
import { ExtensionManager, initExtensions } from './extensions';
import { PandocEngine, pandocFormat, PandocFormat } from './api/pandoc';
import { baseKeysPlugin } from './api/basekeys';
import { appendTransactionsPlugin, appendMarkTransactionsPlugin } from './api/transaction';
import { EditorOutline } from './api/outline';

import { getTitle, setTitle } from './nodes/yaml_metadata/yaml_metadata-title';

import { getOutline } from './behaviors/outline';

import { PandocConverter } from './pandoc/converter';

import './styles/frame.css';
import './styles/styles.css';
import './styles/theme.css';

const kMac = typeof navigator !== 'undefined' ? /Mac/.test(navigator.platform) : false;


export interface EditorConfig {
  readonly pandoc: PandocEngine;
  readonly format: string;
  readonly ui: EditorUI;
  readonly options: EditorOptions;
  readonly hooks?: EditorHooks;
  readonly keybindings?: EditorKeybindings;
  readonly extensions?: readonly Extension[];
}

export interface EditorHooks {
  isEditable?: () => boolean;
}

export interface EditorKeybindings {
  [id: string]: string[];
}

export enum Events  {
  Update = 'update',
  SelectionChange = 'selectionChange'
}


export { EditorCommandId as Commands } from './api/command';


export class Editor {
  private static readonly keybindingsPlugin = new PluginKey('keybindings');

  private readonly parent: HTMLElement;
  private readonly ui: EditorUI;
  private readonly options: EditorOptions;
  private readonly hooks: EditorHooks;
  private readonly schema: Schema;
  private readonly view: EditorView;
  private readonly extensions: ExtensionManager;
  private readonly pandocConverter: PandocConverter;
  private readonly pandocFormat: PandocFormat;

  private state: EditorState;
  private events: ReadonlyMap<string, Event>;
  private keybindings: EditorKeybindings;

  public static async create(parent: HTMLElement, config: EditorConfig): Promise<Editor> {
    const formatInfo = await pandocFormat(config.pandoc, config.format);
    return Promise.resolve(new Editor(parent, config, formatInfo));
  }

  private constructor(parent: HTMLElement, config: EditorConfig, pandocFormat: PandocFormat) {
    // initialize references
    this.parent = parent;
    this.ui = config.ui;
    this.keybindings = config.keybindings || {};
    this.hooks = config.hooks || {};
    this.pandocFormat = pandocFormat;

    // initialize options
    this.options = {
      autoFocus: false,
      spellCheck: true,
      codemirror: true,
      autoLink: false,
      braceMatching: true,
      rmdCodeChunks: false,
      atxHeaders: true,
      ...config.options,
    };

    // initialize custom events
    this.events = this.initEvents();

    // create extensions
    this.extensions = initExtensions(this.options, config.extensions, pandocFormat.extensions);

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

    // apply fixups when the window size changes
    this.applyLayoutFixups = this.applyLayoutFixups.bind(this);
    window.addEventListener('resize', this.applyLayoutFixups);

    // create pandoc translator
    this.pandocConverter = new PandocConverter(
      this.schema,
      this.extensions,
      config.pandoc,
      this.pandocFormat.fullName,
      {
        reader: {},
        writer: {
          atxHeaders: this.options.atxHeaders,
          wrapColumn: this.options.wrapColumn,
        },
      },
    );

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
    // get the doc
    const doc = await this.pandocConverter.toProsemirror(markdown);

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
      this.emitEvent(Events.Update);
      this.emitEvent(Events.SelectionChange);
    }

    return true;
  }

  public getMarkdown(): Promise<string> {
    return this.pandocConverter.fromProsemirror(this.state.doc);
  }

  public getHTML(): string {
    const div = document.createElement('div');
    const fragment = DOMSerializer.fromSchema(this.state.schema).serializeFragment(this.state.doc.content);
    div.appendChild(fragment);
    return div.innerHTML;
  }

  public getSelection(): {} {
    return this.state.selection;
  }

  public getTitle() {
    return getTitle(this.state);
  }

  public getOutline(): EditorOutline {
    return getOutline(this.state);
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

    return this.extensions.commands(this.schema, this.ui, kMac).map((command: ProsemirrorCommand) => {
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

  private dispatchTransaction(tr: Transaction) {
    // apply the transaction
    this.state = this.state.apply(tr);
    this.view.updateState(this.state);

    // notify listeners of selection change
    this.emitEvent(Events.SelectionChange);

    // notify listeners of updates
    if (tr.docChanged) {
      // notify listeners
      this.emitEvent(Events.Update);
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
    events.set(Events.Update, new Event(Events.Update));
    events.set(Events.SelectionChange, new Event(Events.SelectionChange));
    return events;
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
          return ['div', { class: 'body pm-background-color' }, 0];
        },
      },

      notes: {
        content: 'note*',
        parseDOM: [{ tag: 'div[class*="notes"]' }],
        toDOM() {
          return ['div', { class: 'notes pm-background-color' }, 0];
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
      ...this.extensions.plugins(this.schema, this.ui, kMac),
      this.inputRulesPlugin(),
      this.editablePlugin(),
    ];
  }

  private editablePlugin() {
    return new Plugin({
      key: new PluginKey('editable'),
      props: {
        editable: this.hooks.isEditable || (() => true),
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
    const commands = this.extensions.commands(this.schema, this.ui, kMac);
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
      key: Editor.keybindingsPlugin,
      props: {
        handleKeyDown: keydownHandler(pluginKeys),
      },
    });
  }

  private commandKeys(): { [key: string]: readonly string[] } {
    // start with keys provided within command definitions
    const commands = this.extensions.commands(this.schema, this.ui, kMac);
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
