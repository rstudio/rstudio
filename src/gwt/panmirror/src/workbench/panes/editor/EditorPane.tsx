/*
 * EditorPane.tsx
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

import React from 'react';

import { connect } from 'react-redux';

import { Editor, EditorEvents } from 'editor/src/editor';
import { EditorOutline } from 'editor/src/api/outline';
import { EditorDialogs as IEditorDialogs, EditorUIContext } from 'editor/src/api/ui';

import { CommandManager, withCommandManager } from 'workbench/commands/CommandManager';
import { WorkbenchState } from 'workbench/store/store';
import {
  setEditorMarkdown,
  setEditorSelection,
  setEditorOutline,
  setEditorTitle,
} from 'workbench/store/editor/editor-actions';
import { Pane } from 'workbench/widgets/Pane';

import PandocEngine from './editor-pandoc';
import { editorProsemirrorCommands, editorExternalCommands, editorDebugCommands } from './editor-commands';
import { EditorActionsContext } from './EditorActionsContext';
import EditorToolbar from './EditorToolbar';

import EditorDialogs from './dialogs/EditorDialogs';
import EditorOutlineSidebar from './outline/EditorOutlineSidebar';

import styles from './EditorPane.module.scss';

interface EditorPaneProps {
  title: string;
  markdown: string;
  showMarkdown: boolean;
  setTitle: (title: string) => void;
  setMarkdown: (markdown: string) => void;
  setOutline: (outline: EditorOutline) => void;
  setSelection: (selection: {}) => void;
  commandManager: CommandManager;
}

class EditorPane extends React.Component<EditorPaneProps> {
  // container for the editor
  private parent: HTMLDivElement | null;

  // editor instance
  private editor: Editor | null;

  // events we need to unsubscibe from when we are unmounted
  private editorEvents: VoidFunction[];

  // we track the markdown last sent to / observed within the editor
  // so that we can mask out updates that don't change the content
  // (this is neccessary to prevent update loops)
  private editorMarkdown: string | null;

  // services that we provide to the core editor
  private editorDialogsRef: React.RefObject<EditorDialogs>;
  private pandocEngine: PandocEngine;

  constructor(props: Readonly<EditorPaneProps>) {
    super(props);
    this.parent = null;
    this.editor = null;
    this.editorEvents = [];
    this.editorMarkdown = null;
    this.editorDialogsRef = React.createRef<EditorDialogs>();
    this.pandocEngine = new PandocEngine();
  }

  public render() {
    return (
      <Pane className={'editor-pane'}>
        <EditorActionsContext.Provider value={this}>
          <EditorToolbar />
          <div id="editor" className={styles.editorParent} ref={el => (this.parent = el)}>
            <EditorOutlineSidebar />
          </div>
          <EditorDialogs ref={this.editorDialogsRef} />
        </EditorActionsContext.Provider>
      </Pane>
    );
  }

  public async componentDidMount() {
    // create editor
    this.editor = await Editor.create(this.parent!, {
      pandoc: this.pandocEngine,
      format: 'markdown',
      ui: {
        dialogs: this.editorDialogs,
        context: this.editorUIContext,
      },
      options: {
        autoFocus: true,
        spellCheck: false,
        autoLink: true,
        rmdCodeChunks: true,
        codemirror: true,
      },
    });

    // show any warnings
    this.showPandocWarnings();

    // subscribe to events
    this.onEditorEvent(EditorEvents.Update, this.onEditorDocChanged);
    this.onEditorEvent(EditorEvents.OutlineChange, this.onEditorOutlineChanged);
    this.onEditorEvent(EditorEvents.SelectionChange, this.onEditorSelectionChanged);

    // add commands
    this.props.commandManager.addCommands([
      ...editorProsemirrorCommands(this.editor.commands()),
      ...editorExternalCommands(this.editor!),
      ...editorDebugCommands(this.editor!),
    ]);

    // update editor
    this.updateEditor();
  }

  public componentWillUnmount() {
    this.editorEvents.forEach(unregister => unregister());
  }

  public componentDidUpdate(prevProps: EditorPaneProps) {
    // ignore if the editor is not yet loaded
    if (!this.editor) {
      return;
    }

    // if showMarkdown changed to true then save markdown
    if (this.props.showMarkdown && !prevProps.showMarkdown) {
      this.saveMarkdown();
    }

    // update editor
    this.updateEditor();
  }

  // implement EditorActions interface by proxing to this.editor --
  // we need to do this rather than just passing the this.editor! as
  // the value b/c we wait until after rendering to actually create
  // the editor (because it needs a live DOM element as it's parent)
  public focus() {
    if (this.editor) {
      this.editor.focus();
    }
  }
  public navigate(id: string) {
    if (this.editor) {
      this.editor.navigate(id);
    }
  }

  private updateEditor() {
    // set content (will no-op if prop change was from ourselves)
    this.setEditorContent(this.props.markdown);

    // if title changed then set it
    if (this.props.title !== this.editor!.getTitle()) {
      this.editor!.setTitle(this.props.title);
    }
  }

  private get editorDialogs(): IEditorDialogs {
    const dialogs = this.editorDialogsRef.current!;
    return {
      alert: dialogs.alert.bind(dialogs),
      editLink: dialogs.editLink.bind(dialogs),
      editImage: dialogs.editImage.bind(dialogs),
      editOrderedList: dialogs.editOrderedList.bind(dialogs),
      editAttr: dialogs.editAttr.bind(dialogs),
      editSpan: dialogs.editSpan.bind(dialogs),
      editDiv: dialogs.editDiv.bind(dialogs),
      editRawInline: dialogs.editRawInline.bind(dialogs),
      editRawBlock: dialogs.editRawBlock.bind(dialogs),
      insertTable: dialogs.insertTable.bind(dialogs),
      insertCitation: dialogs.insertCitation.bind(dialogs),
    };
  }

  private get editorUIContext(): EditorUIContext {
    return {
      translateResourcePath: (href: string) => href,
    };
  }

  private async setEditorContent(markdown: string) {
    if (markdown !== this.editorMarkdown) {
      this.editorMarkdown = markdown;
      try {
        await this.editor!.setMarkdown(markdown);
      } catch (error) {
        this.editorDialogs.alert(error.message);
      }
    }
  }

  private async onEditorDocChanged() {
    if (this.props.showMarkdown) {
      this.saveMarkdown();
    }

    // set title into reduce
    const title = this.editor!.getTitle();
    this.props.setTitle(title);
  }

  private onEditorOutlineChanged() {
    // set outline into redux
    const outline = this.editor!.getOutline();
    if (outline) {
      this.props.setOutline(outline);
    }
  }

  private async saveMarkdown() {
    try {
      // generate markdown (save a copy so we can ignore resulting update)
      const markdown = await this.editor!.getMarkdown({});
      this.editorMarkdown = markdown;

      // set markdown into redux
      this.props.setMarkdown(markdown);
    } catch (error) {
      this.editorDialogs.alert(error.message);
    }
  }

  private onEditorSelectionChanged() {
    this.props.setSelection(this.editor!.getSelection());
  }

  private onEditorEvent(event: string, handler: () => void) {
    this.editorEvents.push(this.editor!.subscribe(event, handler.bind(this)));
  }

  private showPandocWarnings() {
    const pandocFormat = this.editor!.getPandocFormat();
    const warnings = pandocFormat.warnings;
    if (warnings.invalidFormat) {
      console.log('WARNING: invalid pandoc format ' + warnings.invalidFormat);
    }
    if (warnings.invalidOptions.length) {
      console.log(`WARNING: ${pandocFormat.baseName} does not support options: ${warnings.invalidOptions.join(', ')}`);
    }
  }
}

const mapStateToProps = (state: WorkbenchState) => {
  return {
    title: state.editor.title,
    markdown: state.editor.markdown,
    showMarkdown: state.prefs.showMarkdown,
  };
};

const mapDispatchToProps = (dispatch: any) => {
  return {
    setMarkdown: (markdown: string) => dispatch(setEditorMarkdown(markdown)),
    setTitle: (title: string) => dispatch(setEditorTitle(title)),
    setOutline: (outline: EditorOutline) => dispatch(setEditorOutline(outline)),
    setSelection: (selection: {}) => dispatch(setEditorSelection(selection)),
  };
};

// @ts-ignore
export default withCommandManager(connect(mapStateToProps, mapDispatchToProps)(EditorPane));
