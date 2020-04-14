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
import { MarkSpec, Node as ProsemirrorNode, NodeSpec, Schema, DOMParser, ParseOptions } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { setTextSelection, findParentNodeOfTypeClosestToPos, findParentNode } from 'prosemirror-utils';
import 'prosemirror-view/style/prosemirror.css';

import polyfill from './polyfill/index';

import { EditorOptions } from './api/options';
import { ProsemirrorCommand, CommandFn, EditorCommand } from './api/command';
import { PandocMark, markIsActive } from './api/mark';
import { PandocNode } from './api/node';
import { EditorUI, attrPropsToInput, attrInputToProps, AttrProps, AttrEditInput } from './api/ui';
import { Extension } from './api/extension';
import { ExtensionManager, initExtensions } from './extensions';
import { PandocEngine } from './api/pandoc';
import { PandocCapabilities, getPandocCapabilities } from './api/pandoc_capabilities';
import { fragmentToHTML } from './api/html';
import { EditorEvent } from './api/events';
import {
  PandocFormat,
  resolvePandocFormat,
  splitPandocFormatString,
  PandocFormatComment,
  resolvePandocFormatComment,
  pandocFormatCommentFromCode,
  pandocFormatCommentFromState,
} from './api/pandoc_format';
import { baseKeysPlugin } from './api/basekeys';
import {
  appendTransactionsPlugin,
  appendMarkTransactionsPlugin,
  kFixupTransaction,
  kAddToHistoryTransaction,
} from './api/transaction';
import { EditorOutline } from './api/outline';
import { EditingLocation, getEditingLocation, restoreEditingLocation } from './api/location';
import { navigateTo } from './api/navigation';
import { FixupContext } from './api/fixup';
import { unitToPixels, pixelsToUnit, roundUnit, kValidUnits } from './api/image';
import { kPercentUnit } from './api/css';

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

import { PandocConverter, PandocWriterOptions } from './pandoc/converter';

import { applyTheme, defaultTheme, EditorTheme } from './theme';

import './styles/frame.css';
import './styles/styles.css';

// apply polyfills
polyfill();

const kMac = typeof navigator !== 'undefined' ? /Mac/.test(navigator.platform) : false;

export interface EditorCode {
  markdown: string;
  cursorSentinel?: string;
}

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

export interface UIToolsAttr {
  propsToInput(attr: AttrProps): AttrEditInput;
  inputToProps(input: AttrEditInput): AttrProps;
}

export interface UIToolsImage {
  validUnits(): string[];
  percentUnit(): string;
  unitToPixels(value: number, unit: string, containerWidth: number): number;
  pixelsToUnit(pixels: number, unit: string, containerWidth: number): number;
  roundUnit(value: number, unit: string): string;
}

export class UITools {
  public readonly attr: UIToolsAttr;
  public readonly image: UIToolsImage;

  constructor() {
    this.attr = {
      propsToInput: attrPropsToInput,
      inputToProps: attrInputToProps,
    };

    this.image = {
      validUnits: () => kValidUnits,
      percentUnit: () => kPercentUnit,
      unitToPixels,
      pixelsToUnit,
      roundUnit,
    };
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

  // pandoc capabilities
  private pandocCapabilities: PandocCapabilities;

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

  // create the editor -- note that the markdown argument does not substitute for calling
  // setMarkdown, rather it's used to read the format comment to determine how to 
  // initialize the various editor features
  public static async create(
    parent: HTMLElement,
    context: EditorContext,
    options: EditorOptions,
    markdown: string,
  ): Promise<Editor> {
    // provide default options
    options = {
      autoFocus: false,
      spellCheck: false,
      codemirror: true,
      braceMatching: true,
      rmdCodeChunks: false,
      rmdImagePreview: false,
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

    // get pandoc capabilities
    const pandocCapabilities = await getPandocCapabilities(context.pandoc);

    // create editor
    const editor = new Editor(parent, context, options, pandocFmt, pandocCapabilities);

    // return editor
    return Promise.resolve(editor);
  }

  private constructor(
    parent: HTMLElement, 
    context: EditorContext, 
    options: EditorOptions, 
    pandocFormat: PandocFormat,
    pandocCapabilities: PandocCapabilities) 
  {
    // initialize references
    this.parent = parent;
    this.context = context;
    this.options = options;
    this.keybindings = {};
    this.pandocFormat = pandocFormat;
    this.pandocCapabilities = pandocCapabilities;

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

    // additional dom attributes for editor node
    const attributes: { [name: string]: string } = {};
    if (options.className) {
      attributes.class = options.className;
    }

    // create view
    this.view = new EditorView(this.parent, {
      state: this.state,
      dispatchTransaction: this.dispatchTransaction.bind(this),
      domParser: new EditorDOMParser(this.schema),
      attributes,
    });

    // add proportinal font class to parent
    this.parent.classList.add('pm-proportional-font');

    // apply default theme
    this.applyTheme(defaultTheme());

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
    this.view.destroy();
  }

  public subscribe(event: EditorEvent, handler: VoidFunction): VoidFunction {
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

    // apply fixups
    this.applyFixups(FixupContext.Load);

    // notify listeners if requested
    if (emitUpdate) {
      this.emitEvent(EditorEvent.Update);
      this.emitEvent(EditorEvent.OutlineChange);
      this.emitEvent(EditorEvent.SelectionChange);
    }

    return true;
  }

  public async getMarkdown(options: PandocWriterOptions, cursorSentinel: boolean): Promise<EditorCode> {
    // get current format comment
    const formatComment = pandocFormatCommentFromState(this.state);

    // update format from source code magic comments
    await this.updatePandocFormat(formatComment);

    // override wrapColumn option if it was specified
    if (this.options.formatComment) {
      options.wrapColumn = formatComment.fillColumn || options.wrapColumn;
    }

    // apply layout fixups
    this.applyFixups(FixupContext.Save);

    // convert doc
    const docWithCursor = cursorSentinel
      ? this.docWithCursorSentinel()
      : { doc: this.state.doc, cursorSentinel: undefined };
    const markdown = await this.pandocConverter.fromProsemirror(docWithCursor.doc, this.pandocFormat, options);

    return {
      markdown,
      cursorSentinel: docWithCursor.cursorSentinel,
    };
  }

  public getHTML(): string {
    return fragmentToHTML(this.state.schema, this.state.doc.content);
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
    // delay the restore so all of our code mirror instances
    // can become visible (which allows decorators that reference
    // offsetTop to draw at the proper location)
    setTimeout(() => {
      restoreEditingLocation(this.view, location);
    }, 100);
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
    navigateTo(this.view, node => id === node.attrs.navigation_id);
  }

  public resize() {
    this.applyFixupsOnResize();
    this.emitEvent(EditorEvent.Resize);
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
        plural: () => command.plural(this.state),
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
    // set global dark mode class
    this.parent.classList.toggle('pm-dark-mode', !!theme.darkMode);
    // apply the rest of the theme
    applyTheme(theme);
  }

  public useFixedPadding(fixed: boolean) {
    this.parent.classList.toggle('pm-fixed-padding', fixed);
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
    this.emitEvent(EditorEvent.SelectionChange);

    // notify listeners of updates
    if (tr.docChanged) {
      // fire updated (unless this was a fixup)
      if (!tr.getMeta(kFixupTransaction)) {
        this.emitEvent(EditorEvent.Update);
      }

      // fire outline changed if necessary
      if (getOutline(this.state) !== previousOutline) {
        this.emitEvent(EditorEvent.OutlineChange);
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
    events.set(EditorEvent.Update, new Event(EditorEvent.Update));
    events.set(EditorEvent.OutlineChange, new Event(EditorEvent.OutlineChange));
    events.set(EditorEvent.SelectionChange, new Event(EditorEvent.SelectionChange));
    events.set(EditorEvent.Resize, new Event(EditorEvent.Resize));
    return events;
  }

  private initExtensions() {
    return initExtensions(
      this.options,
      this.context.ui,
      { subscribe: this.subscribe.bind(this) },
      this.context.extensions,
      this.pandocFormat.extensions,
      this.pandocCapabilities
    );
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
          return ['div', { class: 'body pm-cursor-color pm-text-color pm-background-color pm-editing-root-node' }, 
                   ['div', { class: 'pm-content'}, 0]
                 ];
        },
      },

      notes: {
        content: 'note*',
        parseDOM: [{ tag: 'div[class*="notes"]' }],
        toDOM() {
          return ['div', { class: 'notes pm-cursor-color pm-text-color pm-background-color pm-editing-root-node' }, 
                   ['div', { class: 'pm-content'}, 0]
                 ];
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

  private applyFixupsOnResize() {
    this.applyFixups(FixupContext.Resize);
  }

  private applyFixups(context: FixupContext) {
    let tr = this.state.tr;
    tr = this.extensionFixups(tr, context);
    if (tr.docChanged) {
      tr.setMeta(kAddToHistoryTransaction, false);
      tr.setMeta(kFixupTransaction, true);
      this.view.dispatch(tr);
    }
  }

  private extensionFixups(tr: Transaction, context: FixupContext) {
    this.extensions.fixups(this.schema, this.view).forEach(fixup => {
      tr = fixup(tr, context);
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

  private docWithCursorSentinel() {
    // transaction for inserting the sentinel (won't actually commit since it will
    // have the sentinel in it but rather will use the computed tr.doc)
    const tr = this.state.tr;

    // cursorSentinel to return
    let cursorSentinel: string | undefined;

    // find the beginning of the nearest text block
    const textBlock = findParentNode(node => node.isTextblock)(tr.selection);
    if (textBlock) {
      // only proceed if we are not inside a table (as the sentinel will mess up
      // table column formatting)
      const textBlockPos = tr.doc.resolve(textBlock.pos);
      if (!this.schema.nodes.table || !findParentNodeOfTypeClosestToPos(textBlockPos, this.schema.nodes.table)) {
        // space at the end of the sentinel so that it doesn't interere with
        // markdown that is sensitive to contiguous characters (e.g. math)
        cursorSentinel = 'CursorSentinel-CAFB04C4-080D-4074-898C-F670CAACB8AF';
        setTextSelection(textBlock.pos)(tr);
        tr.insertText(cursorSentinel);
      }
    }

    // return the doc and sentinel (if any)
    return {
      doc: tr.doc,
      cursorSentinel,
    };
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
