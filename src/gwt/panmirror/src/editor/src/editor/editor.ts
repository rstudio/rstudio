/*
 * editor.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import { Node as ProsemirrorNode, Schema, DOMParser, ParseOptions } from 'prosemirror-model';
import { EditorState, Plugin, PluginKey, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import 'prosemirror-view/style/prosemirror.css';

import { setTextSelection, findChildrenByType } from 'prosemirror-utils';

import { EditorOptions } from '../api/options';
import { ProsemirrorCommand, CommandFn, EditorCommand } from '../api/command';
import { findTopLevelBodyNodes } from '../api/node';
import { EditorUI, attrPropsToInput, attrInputToProps, AttrProps, AttrEditInput } from '../api/ui';
import { Extension } from '../api/extension';
import { PandocEngine, PandocWriterOptions } from '../api/pandoc';
import { PandocCapabilities, getPandocCapabilities } from '../api/pandoc_capabilities';
import { fragmentToHTML } from '../api/html';
import { DOMEditorEvents, EventType, EventHandler } from '../api/events';
import {
  ScrollEvent,
  UpdateEvent,
  OutlineChangeEvent,
  StateChangeEvent,
  ResizeEvent,
  LayoutEvent,
  FocusEvent,
  DispatchEvent,
} from '../api/event-types';
import {
  PandocFormat,
  resolvePandocFormat,
  PandocFormatConfig,
  pandocFormatConfigFromCode,
  pandocFormatConfigFromDoc,
} from '../api/pandoc_format';
import { baseKeysPlugin } from '../api/basekeys';
import {
  appendTransactionsPlugin,
  appendMarkTransactionsPlugin,
  kFixupTransaction,
  kAddToHistoryTransaction,
  kSetMarkdownTransaction,
} from '../api/transaction';
import { EditorOutline, outlineNodes } from '../api/outline';
import { EditingLocation, getEditingLocation, EditingOutlineLocation, setEditingLocation } from '../api/location';
import { navigateTo } from '../api/navigation';
import { FixupContext } from '../api/fixup';
import { unitToPixels, pixelsToUnit, roundUnit, kValidUnits } from '../api/image';
import { kPercentUnit } from '../api/css';
import { EditorFormat } from '../api/format';
import { diffChars, EditorChange } from '../api/change';
import { markInputRuleFilter } from '../api/input_rule';
import { EditorEvents } from '../api/events';
import { insertRmdChunk } from '../api/rmd';

import { getTitle, setTitle } from '../nodes/yaml_metadata/yaml_metadata-title';

import { getOutline } from '../behaviors/outline';
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
} from '../behaviors/find';

import { omniInsertExtension } from '../behaviors/omni_insert/omni_insert';
import { completionExtension } from '../behaviors/completion/completion';

import { PandocConverter } from '../pandoc/pandoc_converter';

import { ExtensionManager, initExtensions } from './editor-extensions';
import { defaultTheme, EditorTheme, applyTheme, applyPadding } from './editor-theme';
import { defaultEditorUIImages } from './editor-images';
import { editorMenus, EditorMenus } from './editor-menus';
import { editorSchema } from './editor-schema';

// import styles before extensions so they can be overriden by extensions
import './styles/frame.css';
import './styles/styles.css';


export interface EditorCode {
  code: string;
  cursor?: { row: number; column: number };
}

export interface EditorSetMarkdownResult {
  // editor view of markdown (as it will be persisted)
  canonical: string;

  // unrecoginized pandoc tokens
  unrecognized: string[];
}

export interface EditorContext {
  readonly pandoc: PandocEngine;
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
  navigation_id: string | null;
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

export { EditorCommandId as EditorCommands } from '../api/command';

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

export interface UIToolsFormat {
  parseFormatConfig(markdown: string, isRmd: boolean): PandocFormatConfig;
}

export interface UIToolsSource {
  diffChars(from: string, to: string, timeout: number): EditorChange[];
}

export class UITools {
  public readonly attr: UIToolsAttr;
  public readonly image: UIToolsImage;
  public readonly format: UIToolsFormat;
  public readonly source: UIToolsSource;

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

    this.format = {
      parseFormatConfig: pandocFormatConfigFromCode,
    };

    this.source = {
      diffChars,
    };
  }
}

const keybindingsPlugin = new PluginKey('keybindings');

export class Editor {
  // core context passed from client
  private readonly parent: HTMLElement;
  private readonly context: EditorContext;
  private readonly events: EditorEvents;

  // options (derived from defaults + config)
  private readonly options: EditorOptions;

  // format (pandocFormat includes additional diagnostics based on the validity of
  // provided mode + extensions)
  private readonly format: EditorFormat;
  private readonly pandocFormat: PandocFormat;

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

  // content width constraints (if unset uses default editor CSS)
  private maxContentWidth = 0;
  private minContentPadding = 0;

  // keep track of whether the last transaction was selection-only
  // (indicates that we should use a cursorSentinel when going to source mode)
  private lastTrSelectionOnly = false;

  // create the editor -- note that the markdown argument does not substitute for calling
  // setMarkdown, rather it's used to read the format comment to determine how to
  // initialize the various editor features
  public static async create(
    parent: HTMLElement,
    context: EditorContext,
    format: EditorFormat,
    options: EditorOptions,
  ): Promise<Editor> {
    // provide option defaults
    options = {
      autoFocus: false,
      spellCheck: false,
      codemirror: false,
      rmdImagePreview: false,
      hideFormatComment: false,
      className: '',
      ...options,
    };

    // provide format defaults
    format = {
      pandocMode: 'markdown',
      pandocExtensions: '',
      rmdExtensions: {
        codeChunks: false,
        bookdownXRef: false,
        bookdownXRefUI: false,
        bookdownPart: false,
        blogdownMathInCode: false,
        ...format.rmdExtensions,
      },
      hugoExtensions: {
        shortcodes: false,
        ...format.hugoExtensions,
      },
      docTypes: [],
      ...format,
    };

    // provide context defaults
    const defaultImages = defaultEditorUIImages();
    context = {
      ...context,
      ui: {
        ...context.ui,
        images: {
          ...defaultImages,
          ...context.ui.images,
          omni_insert: {
            ...defaultImages.omni_insert,
            ...context.ui.images,
          },
        },
      },
    };

    // resolve the format
    const pandocFmt = await resolvePandocFormat(context.pandoc, format);

    // get pandoc capabilities
    const pandocCapabilities = await getPandocCapabilities(context.pandoc);

    // create editor
    const editor = new Editor(parent, context, options, format, pandocFmt, pandocCapabilities);

    // return editor
    return Promise.resolve(editor);
  }

  private constructor(
    parent: HTMLElement,
    context: EditorContext,
    options: EditorOptions,
    format: EditorFormat,
    pandocFormat: PandocFormat,
    pandocCapabilities: PandocCapabilities,
  ) {
    // initialize references
    this.parent = parent;
    this.events = new DOMEditorEvents(parent);
    this.context = context;
    this.options = options;
    this.format = format;
    this.keybindings = {};
    this.pandocFormat = pandocFormat;
    this.pandocCapabilities = pandocCapabilities;

    // create core extensions
    this.extensions = this.initExtensions();

    // create schema
    this.schema = editorSchema(this.extensions);

    // register completion handlers (done in a separate step b/c omni insert
    // completion handlers require access to the initializezd commands that
    // carry omni insert info)
    this.registerCompletionExtension();

    // create state
    this.state = EditorState.create({
      schema: this.schema,
      doc: this.initialDoc(),
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

    // add custom restoreFocus handler to the view -- this provides a custom
    // handler for RStudio's FocusContext, necessary because the default
    // ProseMirror dom mutation handler picks up the focus and changes the
    // selection.
    Object.defineProperty(this.view.dom, 'restoreFocus', {
      value: () => {
        this.focus();
      },
    });

    // add proportinal font class to parent
    this.parent.classList.add('pm-proportional-font');

    // apply default theme
    this.applyTheme(defaultTheme());

    // create pandoc translator
    this.pandocConverter = new PandocConverter(this.schema, this.extensions, context.pandoc, this.pandocCapabilities);

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

    {
      // scroll event optimization, as recommended by
      // https://developer.mozilla.org/en-US/docs/Web/API/Document/scroll_event
      let ticking = false;
      this.parent.addEventListener(
        'scroll',
        () => {
          if (!ticking) {
            window.requestAnimationFrame(() => {
              this.emitEvent(ScrollEvent);
              ticking = false;
            });
            ticking = true;
          }
        },
        { capture: true },
      );
    }
  }

  public destroy() {
    this.view.destroy();
  }

  public subscribe<TDetail>(event: EventType<TDetail> | string, handler: EventHandler<TDetail>): VoidFunction {
    if (typeof event === 'string') {
      return this.events.subscribe({ eventName: event }, handler);
    } else {
      return this.events.subscribe(event, handler);
    }
  }

  public setTitle(title: string) {
    const tr = setTitle(this.state, title);
    if (tr) {
      this.view.dispatch(tr);
    }
  }

  public async setMarkdown(
    markdown: string,
    options: PandocWriterOptions,
    emitUpdate: boolean,
  ): Promise<EditorSetMarkdownResult> {
    // get the result
    const result = await this.pandocConverter.toProsemirror(markdown, this.pandocFormat.fullName);
    const { doc, unrecognized } = result;

    // if we are preserving history but the existing doc is empty then create a new state
    // (resets the undo stack so that the intial setting of the document can't be undone)
    if (this.isInitialDoc()) {
      this.state = EditorState.create({
        schema: this.state.schema,
        doc,
        plugins: this.state.plugins,
      });
      this.view.updateState(this.state);
    } else {
      // replace the top level nodes in the doc
      const tr = this.state.tr;
      tr.setMeta(kSetMarkdownTransaction, true);
      let i = 0;
      tr.doc.descendants((node, pos) => {
        const mappedPos = tr.mapping.map(pos);
        tr.replaceRangeWith(mappedPos, mappedPos + node.nodeSize, doc.child(i));
        i++;
        return false;
      });
      // set selection to the beginning of the doc
      const bodyNode = findChildrenByType(tr.doc, this.schema.nodes.body)[0];
      setTextSelection(bodyNode.pos)(tr);
      // dispatch
      this.view.dispatch(tr);
    }

    // apply fixups
    this.applyFixups(FixupContext.Load);

    // notify listeners if requested
    if (emitUpdate) {
      this.emitEvent(UpdateEvent);
      this.emitEvent(OutlineChangeEvent);
      this.emitEvent(StateChangeEvent);
    }

    // return our current markdown representation (so the caller know what our
    // current 'view' of the doc as markdown looks like
    const canonical = await this.getMarkdownCode(this.state.doc, options);

    // return
    return {
      canonical,
      unrecognized,
    };
  }

  // flag indicating whether we've ever had setMarkdown (currently we need this
  // because getMarkdown can only be called after setMarkdown b/c it needs
  // the API version retreived in setMarkdown -- we should remedy this)
  public isInitialDoc() {
    return this.state.doc.attrs.initial;
  }

  public async getMarkdown(options: PandocWriterOptions): Promise<EditorCode> {
    // do we need the cursor sentinel?
    const useCursorSentinel = this.lastTrSelectionOnly;

    // insert cursor sentinel if appropriate
    const target = useCursorSentinel ? docWithCursorSentinel(this.state) : { doc: this.state.doc, sentinel: null };

    // get the code
    const code = await this.getMarkdownCode(target.doc, options);

    // return
    return codeWithCursor(code, target.sentinel);
  }

  public getHTML(): string {
    return fragmentToHTML(this.state.schema, this.state.doc.content);
  }

  public getTitle() {
    return getTitle(this.state);
  }

  public getSelection(): EditorSelection {
    const { from, to } = this.state.selection;
    return {
      from,
      to,
      navigation_id: navigationIdForSelection(this.state),
    };
  }

  public getEditingLocation(): EditingLocation {
    return getEditingLocation(this.view);
  }

  public setEditingLocation(outlineLocation?: EditingOutlineLocation, previousLocation?: EditingLocation) {
    setEditingLocation(this.view, outlineLocation, previousLocation);
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

  // get a canonical version of the passed markdown. this method doesn't mutate the
  // visual editor's state/view (it's provided as a performance optimiation for when
  // source mode is configured to save a canonical version of markdown)
  public async getCanonical(markdown: string, options: PandocWriterOptions): Promise<string> {
    // convert to prosemirror doc
    const result = await this.pandocConverter.toProsemirror(markdown, this.pandocFormat.fullName);

    // create a state for this doc
    const state = EditorState.create({
      schema: this.schema,
      doc: result.doc,
      plugins: this.state.plugins,
    });

    // apply load fixups (eumlating what a full round trip will do)
    const tr = state.tr;
    this.extensionFixups(tr, FixupContext.Load);

    // return markdown (will apply save fixups)
    return this.getMarkdownCode(tr.doc, options);
  }

  public focus() {
    this.view.focus();
  }

  public blur() {
    (this.view.dom as HTMLElement).blur();
  }

  public insertChunk(chunkPlaceholder: string, rowOffset: number, colOffset: number) {
    const insertCmd = insertRmdChunk(chunkPlaceholder, rowOffset, colOffset);
    insertCmd(this.view.state, this.view.dispatch, this.view);
    this.focus();
  }

  public navigate(id: string) {
    navigateTo(this.view, node => id === node.attrs.navigation_id, false);
  }

  public resize() {
    this.syncContentWidth();
    this.applyFixupsOnResize();
    this.emitEvent(ResizeEvent);
  }

  public enableDevTools(initFn: (view: EditorView, stateClass: any) => void) {
    initFn(this.view, { EditorState });
  }

  public getMenus(): EditorMenus {
    return editorMenus(this.context.ui, this.commands());
  }

  public commands(): EditorCommand[] {
    // get keybindings (merge user + default)
    const commandKeys = this.commandKeys();

    return this.extensions.commands(this.schema, this.context.ui).map((command: ProsemirrorCommand) => {
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

  public setMaxContentWidth(maxWidth: number, minPadding = 10) {
    this.maxContentWidth = maxWidth;
    this.minContentPadding = minPadding;
    this.syncContentWidth();
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

  public getPandocFormatConfig(isRmd: boolean): PandocFormatConfig {
    return pandocFormatConfigFromDoc(this.state.doc, isRmd);
  }

  private dispatchTransaction(tr: Transaction) {
    // track previous outline
    const previousOutline = getOutline(this.state);

    // track whether this was a selection-only transaction
    this.lastTrSelectionOnly = tr.selectionSet && !tr.docChanged;

    // apply the transaction
    this.state = this.state.apply(tr);
    this.view.updateState(this.state);

    // notify listeners of state change
    this.emitEvent(StateChangeEvent);

    // notify listeners of updates
    if (tr.docChanged || tr.storedMarksSet) {
      // fire updated (unless this was a fixup)
      if (!tr.getMeta(kFixupTransaction)) {
        this.emitEvent(UpdateEvent);
      }

      // fire outline changed if necessary
      if (getOutline(this.state) !== previousOutline) {
        this.emitEvent(OutlineChangeEvent);
      }
    }

    this.emitEvent(DispatchEvent, tr);

    this.emitEvent(LayoutEvent);
  }

  private emitEvent<TDetail>(eventType: EventType<TDetail>, detail?: TDetail) {
    this.events.emit(eventType, detail);
  }

  private initExtensions() {
    return initExtensions(
      this.format,
      this.options,
      this.context.ui,
      { subscribe: this.subscribe.bind(this), emit: this.emitEvent.bind(this) },
      this.context.extensions,
      this.pandocFormat.extensions,
      this.pandocCapabilities,
    );
  }

  private registerCompletionExtension() {
    // register omni insert extension
    const markFilter = markInputRuleFilter(this.schema, this.extensions.pandocMarks());
    this.extensions.register([
      omniInsertExtension(this.extensions.omniInserters(this.schema, this.context.ui), markFilter, this.context.ui),
    ]);

    // register completion extension
    this.extensions.register([completionExtension(this.extensions.completionHandlers(), this.context.ui, this.events)]);
  }

  private createPlugins(): Plugin[] {
    return [
      baseKeysPlugin(this.extensions.baseKeys(this.schema)),
      this.keybindingsPlugin(),
      appendTransactionsPlugin(this.extensions.appendTransactions(this.schema)),
      appendMarkTransactionsPlugin(this.extensions.appendMarkTransactions(this.schema)),
      ...this.extensions.plugins(this.schema, this.context.ui),
      this.inputRulesPlugin(),
      this.editablePlugin(),
      this.domEventsPlugin(),
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
    // filter for disabling input rules for selected marks
    const markFilter = markInputRuleFilter(this.schema, this.extensions.pandocMarks());

    // create the defautl inputRules plugin
    const plugin = inputRules({ rules: this.extensions.inputRules(this.schema) });
    const handleTextInput = plugin.props.handleTextInput!;

    // override to disable input rules as requested
    // https://github.com/ProseMirror/prosemirror-inputrules/commit/b4bf67623aa4c4c1e096c20aa649c0e63751f337
    plugin.props.handleTextInput = (view: EditorView, from: number, to: number, text: string) => {
      if (!markFilter(view.state)) {
        return false;
      }
      return handleTextInput(view, from, to, text);
    };
    return plugin;
  }

  private domEventsPlugin(): Plugin {
    return new Plugin({
      key: new PluginKey('domevents'),
      props: {
        handleDOMEvents: {
          focus: (view: EditorView, event: Event) => {
            this.emitEvent(FocusEvent);
            return false;
          },
        },
      },
    });
  }

  private keybindingsPlugin(): Plugin {
    // get keybindings (merge user + default)
    const commandKeys = this.commandKeys();

    // command keys from extensions
    const pluginKeys: { [key: string]: CommandFn } = {};
    const commands = this.extensions.commands(this.schema, this.context.ui);
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
    const commands = this.extensions.commands(this.schema, this.context.ui);
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

  // update parent padding based on content width settings (if specified)
  private syncContentWidth() {
    if (this.maxContentWidth && this.parent.clientWidth) {
      const minContentPadding = this.minContentPadding || 10;
      const parentWidth = this.parent.clientWidth;
      if (parentWidth > this.maxContentWidth + 2 * minContentPadding) {
        applyPadding(`calc((100% - ${this.maxContentWidth}px)/2)`);
      } else {
        applyPadding(this.minContentPadding + 'px');
      }
    }
  }

  private applyFixupsOnResize() {
    const docChanged = this.applyFixups(FixupContext.Resize);
    if (!docChanged) {
      // If applyFixupsOnResize returns true, then layout has already
      // been fired; if it hasn't, we must do so now
      this.emitEvent(LayoutEvent);
    }
  }

  private applyFixups(context: FixupContext) {
    let tr = this.state.tr;
    tr = this.extensionFixups(tr, context);
    if (tr.docChanged) {
      tr.setMeta(kAddToHistoryTransaction, false);
      tr.setMeta(kFixupTransaction, true);
      this.view.dispatch(tr);
      return true;
    }
    return false;
  }

  private extensionFixups(tr: Transaction, context: FixupContext) {
    this.extensions.fixups(this.schema, this.view).forEach(fixup => {
      tr = fixup(tr, context);
    });
    return tr;
  }

  private initialDoc(): ProsemirrorNode {
    return this.schema.nodeFromJSON({
      type: 'doc',
      attrs: {
        initial: true,
      },
      content: [
        { type: 'body', content: [{ type: 'paragraph' }] },
        { type: 'notes', content: [] },
      ],
    });
  }

  private async getMarkdownCode(doc: ProsemirrorNode, options: PandocWriterOptions) {
    // apply save fixups to a new transaction that we won't commit (b/c we don't
    // want the fixups to affect the loaded editor state)
    const tr = this.state.tr;
    this.extensionFixups(tr, FixupContext.Save);

    // get code
    return this.pandocConverter.fromProsemirror(tr.doc, this.pandocFormat, options);
  }
}

function navigationIdForSelection(state: EditorState): string | null {
  const outline = outlineNodes(state.doc);
  const outlineNode = outline.reverse().find(node => node.pos < state.selection.from);
  if (outlineNode) {
    return outlineNode.node.attrs.navigation_id;
  } else {
    return null;
  }
}

function docWithCursorSentinel(state: EditorState) {
  // transaction for inserting the sentinel (won't actually commit since it will
  // have the sentinel in it but rather will use the computed tr.doc)
  const tr = state.tr;

  // cursorSentinel to return
  let sentinel: string | null = null;

  // find the anchor of the current selection
  const { anchor } = tr.selection;

  // find the closest top-level text block that isn't an Rmd chunk (their
  // first line gets special processing so we can't put the sentinel there)
  const topLevelTextBlocks = findTopLevelBodyNodes(tr.doc, node => {
    return node.isTextblock && node.type !== state.doc.type.schema.nodes.rmd_chunk;
  });
  const textBlock = topLevelTextBlocks.reverse().find(block => block.pos < anchor);
  if (textBlock) {
    sentinel = 'CursorSentinel-CAFB04C4-080D-4074-898C-F670CAACB8AF';
    let pos = textBlock.pos;
    if (textBlock.pos + textBlock.node.nodeSize < anchor) {
      pos = textBlock.pos + textBlock.node.nodeSize - 1;
    }
    setTextSelection(pos)(tr);
    tr.insertText(sentinel);
  }

  // return the doc and sentinel (if any)
  return {
    doc: tr.doc,
    sentinel,
  };
}

// get editor code + cursor location and jsdiff changes from previousCode
function codeWithCursor(code: string, cursorSentinel: string | null) {
  // determine the cursor row and column using the sentinel (remove the sentinel from the code)
  let newCode = code;
  let cursor: { row: number; column: number } | undefined;
  if (cursorSentinel) {
    newCode = code
      .split(/\r?\n/)
      .map((line, index) => {
        if (!cursor) {
          const sentinelLoc = line.indexOf(cursorSentinel);
          if (sentinelLoc !== -1) {
            line = line.replace(cursorSentinel, '');
            cursor = {
              row: index,
              column: sentinelLoc,
            };
          }
        }
        return line;
      })
      .join('\n');
  }

  return {
    code: newCode,
    cursor,
  };
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
