/*
 * VisualModeChunk.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkCallbacks;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkEditor;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPrefsHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextPanmirrorUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode.SyncType;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;

import jsinterop.base.Js;

/**
 * Represents an R Markdown chunk in the visual editor, including its embedded
 * text editor, output, and execution tools. 
 */
public class VisualModeChunk
{
   public VisualModeChunk(int index,
                          PanmirrorUIChunkCallbacks chunkCallbacks,
                          DocUpdateSentinel sentinel,
                          TextEditingTarget target,
                          VisualModeEditorSync sync)
   {
      chunkCallbacks_ = chunkCallbacks;
      sync_ = sync;
      codeExecution_ = target.getCodeExecutor();
      parent_ = target.getDocDisplay();
      target_ = target;
      active_ = false;
      markdownIndex_ = index;

      // Create an element to host all of the chunk output.
      outputHost_ = Document.get().createDivElement();
      outputHost_.getStyle().setPosition(com.google.gwt.dom.client.Style.Position.RELATIVE);

      ChunkOutputUi output = null; 
      if (index > 0)
      {
         Position pos = parent_.positionFromIndex(index);
         scope_ = parent_.getScopeAtPosition(pos);
         if (scope_ != null)
         {
            // Move any pre-existing output from code view into visual mode
            output = target.getNotebook().migrateOutput(scope_, this);
         }
      }
      else
      {
         // No position supplied; no scope is available
         scope_ = null;
      }

      PanmirrorUIChunkEditor chunk = new PanmirrorUIChunkEditor();
      
      releaseOnDismiss_ = new ArrayList<HandlerRegistration>();
      destroyHandlers_ = new ArrayList<Command>();
      rowState_ = new HashMap<Integer,VisualModeChunkRowState>();

      // Create a new AceEditor instance and allow access to the underlying
      // native JavaScript object it represents (AceEditorNative)
      editor_ = new AceEditor();
      final AceEditorNative chunkEditor = editor_.getWidget().getEditor();
      chunk.editor = Js.uncheckedCast(chunkEditor);

      // Forward the R completion context from the parent editing session
      editor_.setRCompletionContext(target_.getRCompletionContext());
      
      // Ensure word wrap mode is on (avoid horizontal scrollbars in embedded
      // editors)
      editor_.setUseWrapMode(true);
      
      // Track activation state and notify visual mode
      editor_.addFocusHandler((evt) -> 
      { 
         active_ = true; 
         target_.getVisualMode().setActiveEditor(editor_);
      });
      editor_.addBlurHandler((evt) ->
      {
         active_ = false;
         target_.getVisualMode().setActiveEditor(null);
      });
       

      // Provide the editor's container element
      host_ = Document.get().createDivElement();
      host_.appendChild(chunkEditor.getContainer());
      host_.getStyle().setPosition(com.google.gwt.dom.client.Style.Position.RELATIVE);

      // Create an element to host all of the execution status widgets
      // (VisualModeChunkRowState).
      execHost_ = Document.get().createDivElement();
      execHost_.getStyle().setProperty("position", "absolute");
      execHost_.getStyle().setProperty("top", "3px");
      host_.appendChild(execHost_);
      
      if (output != null)
      {
         setDefinition(output.getDefinition());
         if (widget_ == null)
         {
            setOutputWidget(output.getOutputWidget());
         }
      }
      host_.appendChild(outputHost_);
      
      // Create the chunk toolbar
      if (scope_ != null)
      {
         createToolbar();
      }

      chunk.element = host_;
      
      // Provide a callback to set the file's mode; this needs to happen in
      // GWT land since the editor accepts GWT-flavored Filetype objects
      chunk.setMode = (String mode) ->
      {
         setMode(editor_, mode);
      };
      
      // Provide a callback to have the code at the cursor executed
      chunk.executeSelection = () ->
      {
         // Ensure visual mode is sync'ed to the editor (we always execute code
         // from the editor)
         sync_.syncToEditor(SyncType.SyncTypeExecution, () ->
         {
            executeSelection();
         });
      };
      
      // Register pref handlers, so that the new editor instance responds to
      // changes in preference values
      TextEditingTargetPrefsHelper.registerPrefs(
            releaseOnDismiss_, 
            RStudioGinjector.INSTANCE.getUserPrefs(), 
            null,  // Project context
            editor_, 
            new TextEditingTargetPrefsHelper.PrefsContext() 
            {
                @Override
                public FileSystemItem getActiveFile()
                {
                   String path = sentinel.getPath();
                   if (path != null)
                      return FileSystemItem.createFile(path);
                   else
                      return null;
                }
            },
            TextEditingTargetPrefsHelper.PrefsSet.Embedded);
      
      // Register callback to be invoked when the editor instance is
      // destroyed; we use this opportunity to clean up pref handlers so they
      // aren't attached to a dead editor instance
      chunk.destroy = () ->
      {
         for (HandlerRegistration reg: releaseOnDismiss_)
         {
            reg.removeHandler();
         }
         
         for (Command cmd: destroyHandlers_)
         {
            cmd.execute();
         }
      };
      
      // Prevent tab from advancing into editor
      chunkEditor.getTextInputElement().setTabIndex(-1);

      // Allow the editor's size to be determined by its content (these
      // settings trigger an auto-growing behavior), up to a max of 1000
      // lines.
      chunkEditor.setMaxLines(1000);
      chunkEditor.setMinLines(1);

      // Turn off line numbers as they're not helpful in chunks
      chunkEditor.getRenderer().setShowGutter(false);
      
      chunk_ = chunk;
   }
   
   public PanmirrorUIChunkEditor getEditor()
   {
      return chunk_;
   }
   
   public AceEditor getAceInstance()
   {
      return editor_;
   }
   
   /**
    * Add a callback to be invoked when the chunk editor is destroyed.
    * 
    * @param handler A callback to invoke on destruction
    * @return A registration object that can be used to unregister the callback
    */
   public HandlerRegistration addDestroyHandler(Command handler)
   {
      destroyHandlers_.add(handler);
      return () ->
      {
         destroyHandlers_.remove(handler);
      };
   }
   
   /**
    * Gets the scope of the code chunk (in the parent editor)
    * 
    * @return The scope, or null if unknown.
    */
   public Scope getScope()
   {
      return scope_;
   }
   
   /**
    * Updates the scope of the code chunk in the parent editor
    * 
    * @param scope
    */
   public void setScope(Scope scope)
   {
      scope_ = scope;

      // Update the toolbar's location, or create one if we don't have one yet
      if (toolbar_ == null)
      {
         createToolbar();
      }
      else
      {
         toolbar_.setScope(scope);
      }
      
      // Update the chunk definition location
      if (def_ != null)
      {
         def_.setRow(scope.getEnd().getRow());
      }
   }
   
   /**
    * Loads a chunk output widget into the chunk.
    * 
    * @param widget The chunk output widget
    */
   public void setOutputWidget(ChunkOutputWidget widget)
   {
      if (outputHost_ != null)
      {
         removeWidget();

         // Append the given output widget
         widget_ = widget;
         outputHost_.appendChild(widget.getElement());
      }
   }
   
   /**
    * Gets the chunk's raw definition.
    * 
    * @return The chunk definition
    */
   public ChunkDefinition getDefinition()
   {
      return def_;
   }
   
   /**
    * Sets the raw definition of the chunk.
    * 
    * @param def The chunk definition
    */
   public void setDefinition(ChunkDefinition def)
   {
      def_ = def;
   }
   
   /**
    * Gets the visual position of the chunk (from Prosemirror)
    * 
    * @return The chunk's visual position
    */
   public int getVisualPosition()
   {
      return chunkCallbacks_.getPos.getVisualPosition();
   }
   

   /**
    * Scrolls the cursor into view.
    */
   public void scrollCursorIntoView()
   {
      chunkCallbacks_.scrollCursorIntoView.scroll();
   }
   
   /**
    * Scroll the chunk's notebook output into view. 
    */
   public void scrollOutputIntoView()
   {
      // Defer this so the layout pass completes and the element has height.
      Scheduler.get().scheduleDeferred(() ->
      {
         if (widget_ != null && widget_.isVisible())
         {
            chunkCallbacks_.scrollIntoView.scrollIntoView(outputHost_);
         }
      });
   }
   
   public void reloadWidget()
   {
      setOutputWidget(widget_);
   }
   
   public void removeWidget()
   {
      outputHost_.setInnerHTML("");
   }
   
   /**
    * Sets the execution state of a range of lines in the chunk.
    * 
    * @param start The first line of the range, counting from the first line in
    *   the chunk
    * @param end The last line of the range
    * @param state the execution state to apply
    */
   public void setLineExecState(int start, int end, int state)
   {
      for (int i = start; i <= end; i++)
      {
         if (state == ChunkRowExecState.LINE_NONE)
         {
            // Removing state; clear the execution display
            if (rowState_.containsKey(i))
            {
               rowState_.get(i).detach();
               rowState_.remove(i);
            }
         }
         else
         {
            if (rowState_.containsKey(i) && rowState_.get(i).attached())
            {
               // Adding state to a widget we already track
               rowState_.get(i).setState(state);
            }
            else if (state != ChunkRowExecState.LINE_RESTING)
            {
               // Create a new state widget if we have a non-resting state to
               // draw
               VisualModeChunkRowState row = 
                     new VisualModeChunkRowState(state, editor_, i);
               row.attach(execHost_);
               rowState_.put(i, row);
            }
         }
      }
   }
   
   /**
    * Sets the execution state of this chunk.
    * 
    * @param state The new execution state.
    */
   public void setState(int state)
   {
      if (toolbar_ != null)
      {
         toolbar_.setState(state);
      }
   }
   
   /**
    * Is the editor currently active?
    * 
    * @return Whether the editor has focus.
    */
   public boolean isActive()
   {
      return active_;
   }
   
   /**
    * Sets focus to the editor instance inside the chunk.
    */
   public void focus()
   {
      editor_.focus();
   }
   
   /**
    * Returns the position/index of the chunk in the original Markdown document,
    * if known. Note that this value may not be correct if the Markdown document
    * has been mutated since the chunk was created.
    * 
    * @return The index of the chunk in Markdown
    */
   public int getMarkdownIndex()
   {
      return markdownIndex_;
   }
   
   private void setMode(AceEditor editor, String mode)
   {
      switch(mode)
      {
      case "r":
         editor.setFileType(FileTypeRegistry.R);
         break;
      case "python":
         editor.setFileType(FileTypeRegistry.PYTHON);
         break;
      case "js":
      case "javascript":
         editor.setFileType(FileTypeRegistry.JS);
         break;
      case "tex":
      case "latex":
         editor.setFileType(FileTypeRegistry.TEX);
         break;
      case "c":
         editor.setFileType(FileTypeRegistry.C);
         break;
      case "cpp":
         editor.setFileType(FileTypeRegistry.CPP);
         break;
      case "sql":
         editor.setFileType(FileTypeRegistry.SQL);
         break;
      case "yaml-frontmatter":
         editor.setFileType(FileTypeRegistry.YAML);
         // Turn off all of Ace's built-in YAML completion as it's not helpful
         // for embedded YAML front matter
         editor.getWidget().getEditor().setCompletionOptions(false, false, false, 0, 0);
         break;
      case "yaml":
         editor.setFileType(FileTypeRegistry.YAML);
         break;
      case "java":
         editor.setFileType(FileTypeRegistry.JAVA);
         break;
      case "html":
         editor.setFileType(FileTypeRegistry.HTML);
         break;
      case "shell":
      case "bash":
         editor.setFileType(FileTypeRegistry.SH);
         break;
      case "theorem":
      case "lemma":
      case "corollary":
      case "proposition":
      case "conjecture":
      case "definition":
      case "example":
      case "exercise":
         // These are Bookdown theorem types
         editor.setFileType(FileTypeRegistry.TEX);
      default:
         editor.setFileType(FileTypeRegistry.TEXT);
         break;
      }
   }
   
   /**
    * Executes the chunk via the parent editor
    */
   public void execute()
   {
      sync_.syncToEditor(SyncType.SyncTypeExecution, () ->
      {
         target_.executeChunk(Position.create(scope_.getBodyStart().getRow(), 0));
      });
   }
   
   /**
    * Executes the active selection via the parent editor
    */
   public void executeSelection()
   {
      performWithSelection(() ->
      {
         codeExecution_.executeSelection(false);
      });
   }

   /**
    * Performs an arbitrary command after synchronizing the selection state of
    * the child editor to the parent.
    * 
    * @param command The command to perform.
    */
   public void performWithSelection(Command command)
   {
      sync_.syncToEditor(SyncType.SyncTypeExecution, () ->
      {
         performWithSyncedSelection(command);
      });
   }
   
   private void performWithSyncedSelection(Command command)
   {
      // Ensure we have a scope. This should always exist since we sync the
      // scope outline prior to executing code.
      if (scope_ == null)
      {
         Debug.logWarning("Cannot execute selection; no selection scope available");
         return;
      }
      
      // Extract the selection range from the native chunk editing widget
      Range selectionRange = editor_.getSelectionRange();
      
      // Map the selection range inside the child editor into the parent editor
      // by adjusting the row offset; for example, if this chunk is at row 12 in
      // the parent document, then executing row 2 in the chunk means executing
      // row 14 in the parent.
      //
      // Consider: Do we need to adjust the column, too? (For chunks indented in
      // the parent document, such as inside a list)
      int offset = scope_.getPreamble().getRow();
      selectionRange.getStart().setRow(
            selectionRange.getStart().getRow() + offset);
      selectionRange.getEnd().setRow(
            selectionRange.getEnd().getRow() + offset);
      
      // Execute selection in the parent
      parent_.setSelectionRange(selectionRange);

      command.execute();
      
      // After the event loop, forward the parent selection back to the child if
      // it's changed (this allows us to advance the cursor after running a line)
      Scheduler.get().scheduleDeferred(() ->
      {
         Range postExecution = parent_.getSelectionRange();
         
         // Ignore if range hasn't changed
         if (postExecution.isEqualTo(selectionRange))
            return;

         // Reverse the offset adjustment and apply selection to the nested
         // editor
         postExecution.getStart().setRow(
               postExecution.getStart().getRow() - offset);
         postExecution.getEnd().setRow(
               postExecution.getEnd().getRow() - offset);
         editor_.setSelectionRange(postExecution);
      });
   }
   
   private void createToolbar()
   {
      toolbar_ = new ChunkContextPanmirrorUi(target_, 
            scope_, editor_, false, sync_);
      host_.appendChild(toolbar_.getToolbar().getElement());
   }
   
   private ChunkDefinition def_;
   private ChunkOutputWidget widget_;
   private Scope scope_;
   private ChunkContextPanmirrorUi toolbar_;
   private boolean active_;
   private PanmirrorUIChunkCallbacks chunkCallbacks_;

   private final DivElement outputHost_;
   private final DivElement host_;
   private final DivElement execHost_;
   private final PanmirrorUIChunkEditor chunk_;
   private final AceEditor editor_;
   private final DocDisplay parent_;
   private final List<Command> destroyHandlers_;
   private final ArrayList<HandlerRegistration> releaseOnDismiss_;
   private final VisualModeEditorSync sync_;
   private final EditingTargetCodeExecution codeExecution_;
   private final Map<Integer,VisualModeChunkRowState> rowState_;
   private final TextEditingTarget target_;
   private final int markdownIndex_;
}
