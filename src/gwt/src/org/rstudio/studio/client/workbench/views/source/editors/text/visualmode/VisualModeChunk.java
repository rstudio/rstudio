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
import java.util.List;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkEditor;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunks;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPrefsHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;

import jsinterop.base.Js;

public class VisualModeChunk
{
   public VisualModeChunk(int index,
                          PanmirrorUIChunks.GetVisualPosition getPos,
                          DocUpdateSentinel sentinel,
                          DocDisplay parent,
                          TextEditingTargetNotebook notebook, 
                          CompletionContext rCompletionContext)
   {
      getPos_ = getPos;

      ChunkOutputUi output = null; 
      if (index >= 0)
      {
         Position pos = parent.positionFromIndex(index);
         scope_ = parent.getScopeAtPosition(pos);

         // Migrate output UI from this scope
         output = notebook.migrateOutput(scope_, this);
      }
      else
      {
         // No position supplied; no scope is available
         scope_ = null;
      }

      PanmirrorUIChunkEditor chunk = new PanmirrorUIChunkEditor();
      
      releaseOnDismiss_ = new ArrayList<HandlerRegistration>();
      destroyHandlers_ = new ArrayList<Command>();

      // Create a new AceEditor instance and allow access to the underlying
      // native JavaScript object it represents (AceEditorNative)
      final AceEditor editor = new AceEditor();
      final AceEditorNative chunkEditor = editor.getWidget().getEditor();
      chunk.editor = Js.uncheckedCast(chunkEditor);

      // Forward the R completion context from the parent editing session
      editor.setRCompletionContext(rCompletionContext);
      
      // Ensure word wrap mode is on (avoid horizontal scrollbars in embedded
      // editors)
      editor.setUseWrapMode(true);

      // Provide the editor's container element
      DivElement ele = Document.get().createDivElement();
      ele.appendChild(chunkEditor.getContainer());
      
      outputHost_ = Document.get().createDivElement();
      if (output != null && widget_ == null)
      {
         widget_ = output.getOutputWidget();
         setOutputWidget(output.getOutputWidget());
      }
      ele.appendChild(outputHost_);

      chunk.element = ele;
      
      // Provide a callback to set the file's mode; this needs to happen in
      // GWT land since the editor accepts GWT-flavored Filetype objects
      chunk.setMode = (String mode) ->
      {
         setMode(editor, mode);
      };
      
      // Provide a callback to have the code at the cursor executed
      final EditingTargetCodeExecution executor = new EditingTargetCodeExecution(
            editor, sentinel.getId());
      executor.setAppendLinesAtEnd(false);
      chunk.executeSelection = () ->
      {
         if (parent.showChunkOutputInline() && scope_ != null)
         {
            notebook.executeChunk(scope_);
         }
         else
         {
            executor.executeSelection(false);
         }
      };
      
      // Register pref handlers, so that the new editor instance responds to
      // changes in preference values
      TextEditingTargetPrefsHelper.registerPrefs(
            releaseOnDismiss_, 
            RStudioGinjector.INSTANCE.getUserPrefs(), 
            null,  // Project context
            editor, 
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
      
      editor_ = chunk;
   }
   
   public PanmirrorUIChunkEditor getEditor()
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
   
   public int getVisualPosition()
   {
      return getPos_.getVisualPosition();
   }
   
   public void reloadWidget()
   {
      setOutputWidget(widget_);
   }
   
   public void removeWidget()
   {
      outputHost_.setInnerHTML("");
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
   
   private ChunkDefinition def_;
   private ChunkOutputWidget widget_;
   private Scope scope_;
   
   private final PanmirrorUIChunks.GetVisualPosition getPos_;
   private final DivElement outputHost_;
   private final PanmirrorUIChunkEditor editor_;
   private final List<Command> destroyHandlers_;
   private final ArrayList<HandlerRegistration> releaseOnDismiss_;
}
