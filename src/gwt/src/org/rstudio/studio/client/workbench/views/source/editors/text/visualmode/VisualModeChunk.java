/*
 * VisualModeChunk.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.aria.client.ExpandedValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkCallbacks;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunkEditor;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.output.lint.LintManager;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetCodeExecution;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkOutputWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ChunkRowExecState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.FoldStyle;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetCompilePdfHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPrefsHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetQuartoHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.EditorBehavior;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkContextPanmirrorUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkOutputUi;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.VisualMode.SyncType;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.ui.VisualModeCollapseToggle;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions;
import org.rstudio.studio.client.workbench.views.source.model.RnwCompletionContext;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;

import jsinterop.base.Js;

/**
 * Represents an R Markdown chunk in the visual editor, including its embedded
 * text editor, output, and execution tools. 
 */
public class VisualModeChunk
{
   public interface ChunkStyle extends ClientBundle
   {
      @Source("VisualModeChunk.css")
      VisualModeChunk.Styles style();
   }

   public interface Styles extends CssResource
   {
      String host();
      String gutter();
      String gutterIcon();
      String summary();
      String editor();
      String editorHost();
      String chunkHost();
      String toolbar();
   }

   public VisualModeChunk(Element element,
                          int index,
                          boolean isExpanded,
                          JsArrayString classes,
                          PanmirrorUIChunkCallbacks chunkCallbacks,
                          DocUpdateSentinel sentinel,
                          TextEditingTarget target,
                          VisualModeEditorSync sync)
   {
      element_ = element;
      chunkCallbacks_ = chunkCallbacks;
      sync_ = sync;
      codeExecution_ = target.getCodeExecutor();
      parent_ = target.getDocDisplay();
      target_ = target;
      active_ = false;
      markdownIndex_ = index;
      releaseOnDismiss_ = new ArrayList<>();
      destroyHandlers_ = new ArrayList<>();
      lint_ = JsArray.createArray().cast();
      classes_ = classes;

      // Instantiate CSS style
      ChunkStyle style = GWT.create(ChunkStyle.class);
      style_ = style.style();
      style_.ensureInjected();

      // Create an element to host all of the chunk output.
      outputHost_ = Document.get().createDivElement();
      outputHost_.getStyle().setPosition(com.google.gwt.dom.client.Style.Position.RELATIVE);

      ChunkOutputUi output = null;
      if (index >= 0)
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
      
      rowState_ = new HashMap<>();

      // Create a new AceEditor instance and allow access to the underlying
      // native JavaScript object it represents (AceEditorNative)
      editor_ = new AceEditor();
      editor_.setEditorBehavior(EditorBehavior.AceBehaviorEmbedded);
      final AceEditorNative chunkEditor = editor_.getWidget().getEditor();
      chunk.editor = Js.uncheckedCast(chunkEditor);

      // Forward the R and C++ completion contexts from the parent editing session
      editor_.setRCompletionContext(target_.getRCompletionContext());
      editor_.setCppCompletionContext(target_.getCppCompletionContext());
      
      // Forward the Rnw completion context with a wrapper to adjust for the
      // position in our display; this is what allows the completion engine to
      // work on R Markdown chunk options
      editor_.setRnwCompletionContext(wrapRnwCompletionContext(
            target_.getRnwCompletionContext()));
      
      // Ensure word wrap mode is on (avoid horizontal scrollbars in embedded
      // editors)
      editor_.setUseWrapMode(true);
      
      // Special comment continuation
      releaseOnDismiss_.add(editor_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            TextEditingTargetQuartoHelper.continueSpecialCommentOnNewline(editor_, ne);
         }
      }
      ));
      
      // Track activation state and notify visual mode
      releaseOnDismiss_.add(editor_.addFocusHandler((evt) ->
      { 
         active_ = true; 
         target_.getVisualMode().setActiveEditor(editor_);
      }));
      releaseOnDismiss_.add(editor_.addBlurHandler((evt) ->
      {
         active_ = false;
         target_.getVisualMode().setActiveEditor(null);
      }));

      // Track UI pref for tab behavior. Note that this can't be a lambda because Ace has trouble with lambda bindings.
      releaseOnDismiss_.add(RStudioGinjector.INSTANCE.getUserPrefs().tabKeyMoveFocus().bind(
         new CommandWithArg<Boolean>()
         {
            @Override
            public void execute(Boolean movesFocus)
            {
               chunkEditor.setTabMovesFocus(movesFocus);
            }
         }));

      // Track UI pref for line numbers. We need to redraw lint when this changes since showing line numbers causes
      // Ace to draw lint markers inside the editor gutter.
      releaseOnDismiss_.add(RStudioGinjector.INSTANCE.getUserPrefs().visualMarkdownCodeEditorLineNumbers().bind(
         new CommandWithArg<Boolean>()
         {
            @Override
            public void execute(Boolean showLineNumbers)
            {
               showLint(lint_, false);
            }
         }
      ));

      // Provide the editor's container element
      host_ = Document.get().createDivElement();
      host_.setClassName(style_.host());
      host_.setTabIndex(0);

      // add the collapse toggle
      collapse_ = new VisualModeCollapseToggle(isExpanded);
      host_.appendChild(collapse_.getElement());

      // add the summary label
      summary_ = Document.get().createDivElement();
      summary_.setClassName(ThemeFonts.getFixedWidthClass() + " " + style_.summary());
      host_.appendChild(summary_);

      // add the chunk (contains the editor and the output, if any)
      chunkHost_ = Document.get().createDivElement();
      chunkHost_.setClassName(style_.chunkHost());
      host_.appendChild(chunkHost_);

      // add the editor
      editorHost_ = Document.get().createDivElement();
      editorHost_.setClassName(style_.editorHost());
      editorContainer_ = chunkEditor.getContainer();
      editorContainer_.addClassName(style_.editor());
      editorHost_.appendChild(editorContainer_);
      chunkHost_.appendChild(editorHost_);

      // Create an element to host all of the execution status widgets
      // (VisualModeChunkRowState).
      gutterHost_ = Document.get().createDivElement();
      gutterHost_.setClassName(style_.gutter());
      host_.appendChild(gutterHost_);
      
      if (output != null)
      {
         setDefinition(output.getDefinition());
         if (widget_ == null)
         {
            setOutputWidget(output.getOutputWidget());
         }
         Scheduler.get().scheduleDeferred(() ->
         {
            // Perform the initial sync of the output class
            // (deferred so DOM attach happens first)
            syncOutputClass();
         });
      }
      chunkHost_.appendChild(outputHost_);
      
      // Create the chunk toolbar
      if (scope_ != null && isRunnableChunk(scope_))
      {
         createToolbar();
      }

      chunk.element = host_;
      
      // Provide a callback to set the file's mode; this needs to happen in
      // GWT land since the editor accepts GWT-flavored Filetype objects
      chunk.setMode = (String mode) ->
      {
         setMode(editor_, mode);

         // Disable code folding, since we don't have a gutter (must be done
         // after setting the mode)
         editor_.setFoldStyle(FoldStyle.FOLD_MARK_MANUAL);
      
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

      DOM.sinkEvents(host_, Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK | Event.ONFOCUS);
      DOM.setEventListener(host_, evt ->
      {
         switch(evt.getTypeInt())
         {
            case Event.ONMOUSEOVER:
               // Show toggle on mouse over
               collapse_.setShowToggle(true);
               break;

            case Event.ONMOUSEOUT:
               // Hide toggle on mouse out (if editor isn't focused)
               if (!editor_.isFocused())
                  collapse_.setShowToggle(false);
               break;

            case Event.ONFOCUS:
            case Event.ONCLICK:
               // Activate editor if focused/clicked while collapsed
               if (!getExpanded())
               {
                  editor_.focus();
               }
               break;
         }
      });
      
      // Ensure that the editor expands when it gets focus
      releaseOnDismiss_.add(editor_.addFocusHandler((evt) ->
      {
         if (element_ != null)
         {
            element_.addClassName("pm-ace-focused");
         }
         collapse_.setShowToggle(true);
      }));
      releaseOnDismiss_.add(editor_.addBlurHandler((evt) ->
      {
         if (element_ != null)
         {
            element_.removeClassName("pm-ace-focused");
         }
         collapse_.setShowToggle(false);
      }));

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

      // Register callback to be invoked by the editor when it needs to trigger
      // its own expansion state (e.g. when it needs to reveal find results)
      chunk.setExpanded = (boolean expanded) ->
      {
         if (collapse_.expanded.getValue() != expanded)
         {
            collapse_.expanded.setValue(expanded, true);
         }
      };
      
      chunk.getExpanded = () ->
      {
         return collapse_.expanded.getValue();
      };

      // Hook up event handler for expand/collapse
      setChunkExpanded(isExpanded);
      releaseOnDismiss_.add(collapse_.expanded.addValueChangeHandler(evt ->
      {
         setChunkExpanded(evt.getValue());
      }));

      // Prevent tab from advancing into editor
      chunkEditor.getTextInputElement().setTabIndex(-1);

      // Force the use of browser APIs to set focus to the input element
      chunkEditor.useBrowserInputFocus();

      // Allow the editor's size to be determined by its content (these
      // settings trigger an auto-growing behavior), up to a max of 1000
      // lines.
      chunkEditor.setMaxLines(1000);
      chunkEditor.setMinLines(1);

      // Begin linting the chunk
      lintManager_ = new LintManager(new VisualModeLintSource(this), releaseOnDismiss_);

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
      
      if (isRunnableChunk(scope))
      {
         if (toolbar_ == null)
         {
            createToolbar();
         }
         else
         {
            toolbar_.setScope(scope);
         }
      }
      else if (toolbar_ != null)
      {
         host_.removeChild(toolbar_.getToolbar().getElement());
         toolbar_ = null;
      }
     
      // Update the chunk definition location
      if (def_ != null)
      {
         def_.setRow(scope.getEnd().getRow());
      }
   }
   
  
   private boolean isRunnableChunk(Scope scope)
   {
      return TextEditingTargetScopeHelper.isRunnableChunk(
         target_.getDocDisplay(),
         scope.getPreamble().getRow()
      );
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

         // Ensure that when the widget's visibility changes we set the appropriate
         // CSS class on the host
         releaseOnDismiss_.add(widget.addVisibleChangeHandler((evt) ->
         {
            syncOutputClass();
         }));

         outputHost_.appendChild(widget.getElement());

         // Add output decoration to host if necessary
         syncOutputClass();
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

   /**
    * Removes the output widget, if any, from the chunk.
    */
   public void removeWidget()
   {
      outputHost_.setInnerHTML("");
      widget_ = null;
      syncOutputClass();
   }
   
   /**
    * Sets the row state of a range of lines in the chunk.
    * 
    * @param start The first line of the range, counting from the first line in
    *   the chunk
    * @param end The last line of the range
    * @param state The state to apply
    * @param clazz The CSS class to use to display the state, if any
    *
    * @return A set of the row states that were created or modified
    */
   public List<VisualModeChunkRowState> setRowState(int start, int end, int state, String clazz)
   {
      ArrayList<VisualModeChunkRowState> states = new ArrayList<>();
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
               states.add(rowState_.get(i));
            }
            else if (state != ChunkRowExecState.LINE_RESTING)
            {
               // Create a new state widget if we have a non-resting state to
               // draw
               VisualModeChunkRowState row = 
                     new VisualModeChunkRowState(state, editor_, i, clazz);
               row.attach(gutterHost_);
               states.add(row);
               rowState_.put(i, row);
            }
         }
      }

      return states;
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

   /**
    * Sets the expansion state of the chunk. When collapsed, a chunk, and any output
    * it contains, is reduced to a one-line summary that can be expanded to reveal
    * the full chunk.
    *
    * @param expanded
    */
   public void setExpanded(boolean expanded)
   {
      if (expanded != collapse_.expanded.getValue())
      {
         collapse_.expanded.setValue(expanded, true);
      }
   }

   /**
    * Gets the current expansion state of the chunk.
    *
    * @return The current expansion state; true if expanded, false otherwise
    */
   public boolean getExpanded()
   {
      return collapse_.expanded.getValue();
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
      case "ojs":
         editor.setFileType(FileTypeRegistry.JS);
         break;
      case "mermaid":
         editor.setFileType(FileTypeRegistry.MERMAID);
         break;
      case "dot":
         editor.setFileType(FileTypeRegistry.GRAPHVIZ);
         break;
      case "tex":
      case "latex":
         editor.setFileType(FileTypeRegistry.TEX);
         break;
      case "c":
         editor.setFileType(FileTypeRegistry.C);
         break;
      case "cpp":
      case "rcpp":
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
      performWithSelection((pos) ->
      {
         codeExecution_.executeSelection(false);
      });
   }

   /**
    * Performs an arbitrary command after synchronizing the selection state of
    * the child editor to the parent.
    * 
    * @param command The command to perform. The new position of the cursor in
    *    source mode is passed as an argument.
    */
   public void performWithSelection(CommandWithArg<Position> command)
   {
      sync_.syncToEditor(SyncType.SyncTypeExecution, () ->
      {
         performWithSyncedSelection(command);
      });
   }

   /**
    * Returns the parent editor.
    *
    * @return The parent text editing target.
    */
   public TextEditingTarget getParentEditingTarget()
   {
      return target_;
   }

   /**
    * Shows lint results in the chunk.
    *
    * @param lint The lint results to show.
    * @param cancelPending Whether to cancel pending lint requests after showing this lint.
    */
   public void showLint(JsArray<LintItem> lint, boolean cancelPending)
   {
      // Save lint so we can redraw it when necessary
      lint_ = lint;

      // Show damage in the editor itself
      editor_.showLint(lint);

      // If there is any non-lint status, don't draw any lint markers. The row state also shows execution status
      // and we want to avoid showing both at once.
      for (ChunkRowExecState state: rowState_.values())
      {
         if (state.getState() != ChunkRowExecState.LINE_LINT &&
             state.getState() != ChunkRowExecState.LINE_NONE)
         {
            return;
         }
      }

      // Clean out the row state in preparation for showing lint.
      for (ChunkRowExecState state: rowState_.values())
      {
         state.detach();
      }
      rowState_.clear();

      // When line numbers are enabled, gutter symbols show up in the editor itself representing linting issues.
      // When they aren't, we need to show those symbols outside the chunk.
      if (!RStudioGinjector.INSTANCE.getUserPrefs().visualMarkdownCodeEditorLineNumbers().getValue())
      {
         for (int i = 0; i < lint.length(); i++)
         {
            LintItem item = lint.get(i);
            String clazz = style_.gutterIcon() + " ";
            if (StringUtil.equals(item.getType(), "error"))
               clazz += ThemeStyles.INSTANCE.gutterError();
            else if (StringUtil.equals(item.getType(), "info") || StringUtil.equals(item.getType(), "style"))
               clazz += ThemeStyles.INSTANCE.gutterInfo();
            else if (StringUtil.equals(item.getType(), "warning"))
               clazz += ThemeStyles.INSTANCE.gutterWarning();
            List<VisualModeChunkRowState> states = setRowState(
               item.getStartRow() + 1,
               item.getStartRow() + 1,
               ChunkRowExecState.LINE_LINT,
               clazz);

            // Apply title to elements so lint text appears when hovered
            for (VisualModeChunkRowState state: states)
            {
               if (state.getTitle().isEmpty())
                  state.setTitle(item.getText());
               else
                  state.appendToTitle(item.getText());
            }
         }
      }

      // Cancel any pending lint operation if requested. This ensures that lint arriving from the
      // outer editor doesn't get immediately overwritten by pending lint requests from this inner editor.
      if (cancelPending)
      {
         lintManager_.cancelPending();
      }
   }

   private void performWithSyncedSelection(CommandWithArg<Position> command)
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
      // Compute the row offset from the position of the chunk in the editor.
      int offsetRow = scope_.getPreamble().getRow();

      selectionRange.getStart().setRow(
         selectionRange.getStart().getRow() + offsetRow);
      selectionRange.getEnd().setRow(
         selectionRange.getEnd().getRow() + offsetRow);

      // Compute the column offset by examining the size of the leading whitespace
      // (if any) present in the chunk preamble. This handles a case wherein the chunk
      // is indented in the parent editor, such as inside a list.
      String chunkPreamble = parent_.getTextForRange(Range.create(offsetRow, 0, offsetRow + 1, 0));
      int offsetCol = chunkPreamble.length() - StringUtil.trimLeft(chunkPreamble).length();
      selectionRange.getStart().setColumn(
         selectionRange.getStart().getColumn() + offsetCol);
      selectionRange.getEnd().setColumn(
         selectionRange.getEnd().getColumn() + offsetCol);

      // Execute selection in the parent
      parent_.setSelectionRange(selectionRange);

      command.execute(selectionRange.getStart());
      
      // After the event loop, forward the parent selection back to the child if
      // it's changed (this allows us to advance the cursor after running a line)
      Scheduler.get().scheduleDeferred(() ->
      {
         Range postExecution = parent_.getSelectionRange();
         
         // Ignore if range hasn't changed
         if (postExecution.isEqualTo(selectionRange))
            return;

         // Reverse the offset adjustment and apply selection to the nested
         // editor.
         postExecution.getStart().setRow(
               postExecution.getStart().getRow() - offsetRow);
         postExecution.getEnd().setRow(
               postExecution.getEnd().getRow() - offsetRow);
         postExecution.getStart().setColumn(
            postExecution.getStart().getColumn() - offsetCol);
         postExecution.getEnd().setColumn(
            postExecution.getEnd().getColumn() - offsetCol);
         editor_.setSelectionRange(postExecution);
      });
   }

   /**
    * Create the chunk toolbar, which hosts the execution controls (play chunk, etc.)
    */
   private void createToolbar()
   {
      toolbar_ = new ChunkContextPanmirrorUi(target_, 
            scope_, editor_, false, sync_);
      if (toolbar_.getElement() != null)
      {
         toolbar_.getElement().addClassName(style_.toolbar());
      }
      host_.appendChild(toolbar_.getToolbar().getElement());
   }
   
   /**
    * Creates a wrapped version of the given completion context which adjusts
    * chunk options completion for the embedded editor.
    * 
    * @param inner The completion context to wrap
    * @return The wrapped completion context
    */
   private RnwCompletionContext wrapRnwCompletionContext(RnwCompletionContext inner)
   {
      return new RnwCompletionContext()
      {
         @Override
         public int getRnwOptionsStart(String line, int cursorPos)
         {
            // Only the first row can have chunk options in embedded editors
            int row = editor_.getSelectionStart().getRow();
            if (row > 1)
            {
               return -1;
            }
            
            return TextEditingTargetCompilePdfHelper.getRnwOptionsStart(
                  line, cursorPos, 
                  Pattern.create("^\\s*\\{r"), null);
         }
         
         @Override
         public void getChunkOptions(ServerRequestCallback<RnwChunkOptions> requestCallback)
         {
            inner.getChunkOptions(requestCallback);
         }
         
         @Override
         public RnwWeave getActiveRnwWeave()
         {
            return inner.getActiveRnwWeave();
         }
      };
   }

   /**
    * Sets the expansion state of the code chunk
    *
    * @param expanded Whether the chunk is to be expanded.
    */
   private void setChunkExpanded(boolean expanded)
   {
      if (expanded)
      {
         if (element_ != null)
         {
            element_.removeClassName("pm-ace-collapsed");
         }

         // Clear summary and hide it (will be repopulated on collapse)
         summary_.setInnerHTML("");

         // set to writeable
         editor_.setReadOnly(false);

         Roles.getRegionRole().setAriaExpandedState(host_, ExpandedValue.TRUE);
      }
      else
      {
         if (element_ != null)
         {
            element_.addClassName("pm-ace-collapsed");
         }

         // Create and show the summary text
         summary_.appendChild(createSummary());

         // Set to readonly
         editor_.setReadOnly(true);

         A11y.setARIANotExpanded(host_);
      }

      // Nudge the collapse state timer so that this change is saved
      target_.getVisualMode().nudgeSaveCollapseState();

      // Adjust padding to compensate for collapse state
      syncOutputClass();
   }

   /**
    * Creates a one-line summary of a chunk's contents, to be displayed when the chunk is collapsed.
    *
    * @return An HTML element summarizing the chunk.
    */
   private Element createSummary()
   {
      DivElement wrapper = Document.get().createDivElement();

      // Get the entire contents of the chunk in a single string, including the header
      String contents = chunkCallbacks_.getTextContent.getTextContent();

      // Line counter
      int lines = 0;

      // We're mostly interested in the language and label; establish defaults
      String engine = "R";
      String label = "";

      // By convention, the first class in a non-executable chunk is its language.
      // Use that as the "engine" unless another is explicitly specified.
      if (classes_ != null &&
         classes_.length() > 0 &&
         !StringUtil.isNullOrEmpty(classes_.get(0)))
      {
         engine = StringUtil.capitalize(classes_.get(0));
      }

      // Quarto chunks use this syntax, which must be parsed separately
      String quartoLabel = "#| label:";

      // Iterate over each line in the chunk
      for (String line: StringUtil.getLineIterator(contents))
      {
         if (lines == 0)
         {
            if (StringUtil.equals(line.trim(), "---"))
            {
               // Special case for the YAML header
               engine = "YAML";
               label = "Metadata";
            }
            else
            {
               // This is the first line in the chunk (its header). Parse it, reintroducing
               // the backticks since they aren't present in the embedded editor.
               Map<String, String> options = new HashMap<>();
               RChunkHeaderParser.parse("```" + line, engine, options);

               // Check for the "engine" (language) option; extract it if specified
               String optionEngine = options.get("engine");
               if (!StringUtil.isNullOrEmpty(optionEngine))
               {
                  engine = StringUtil.capitalize(StringUtil.stringValue(optionEngine));
               }

               // Check for the "label" option; the parser is smart enough to synthesize
               // this from the various ways of specifying Knitr labels
               String labelEngine = options.get("label");
               if (!StringUtil.isNullOrEmpty(labelEngine))
               {
                  label = StringUtil.stringValue(labelEngine);
               }
            }
         }
         else
         {
            // If this is the magic comment indicating a Quarto label, extract the label
            if (line.startsWith(quartoLabel))
            {
               label = line.substring(quartoLabel.length()).trim();
            }
         }
         lines++;
      }

      String summary = "";

      // Start with the label (if we found one); this is rendered at full opacity for emphasis
      if (!StringUtil.isNullOrEmpty(label))
      {
         SpanElement spanLabel = Document.get().createSpanElement();
         spanLabel.setInnerText(label + "");
         spanLabel.getStyle().setOpacity(1);
         wrapper.appendChild(spanLabel);
         summary += ": ";
      }

      // Summarize engine and line count
      summary += (lines > 1 ? constants_.visualModeChunkSummaryPlural(engine, lines) :
              constants_.visualModeChunkSummary(engine, lines));

      SpanElement spanSummary = Document.get().createSpanElement();
      spanSummary.setInnerText(summary);
      spanSummary.getStyle().setOpacity(0.6);
      wrapper.appendChild(spanSummary);

      return wrapper;
   }

   /**
    * Synchronize the CSS class indicating whether we have output with the output element.
    * When there's visible output the host needs to use less padding, so this must be
    * synchronized whenever visibility of the output element changes.
    */
   private void syncOutputClass()
   {
      // Skip if we aren't yet fully instantiated
      if (host_ == null || element_ == null)
      {
         return;
      }

      String outputClass = "pm-ace-has-output";
      if (widget_ != null && widget_.isVisible())
      {
         // We have output; add the CSS decoration
         element_.addClassName(outputClass);
      }
      else
      {
         // We don't have output; remove it
         element_.removeClassName(outputClass);
      }
   }

   private ChunkDefinition def_;
   private ChunkOutputWidget widget_;
   private Scope scope_;
   private ChunkContextPanmirrorUi toolbar_;
   private boolean active_;
   private JsArrayString classes_;
   private PanmirrorUIChunkCallbacks chunkCallbacks_;
   private Styles style_;
   private JsArray<LintItem> lint_;

   private final Element element_;
   private final DivElement outputHost_;
   private final DivElement host_;
   private final DivElement chunkHost_;
   private final DivElement gutterHost_;
   private final DivElement editorHost_;
   private final PanmirrorUIChunkEditor chunk_;
   private final AceEditor editor_;
   private final Element editorContainer_;
   private final DocDisplay parent_;
   private final List<Command> destroyHandlers_;
   private final ArrayList<HandlerRegistration> releaseOnDismiss_;
   private final VisualModeEditorSync sync_;
   private final EditingTargetCodeExecution codeExecution_;
   private final VisualModeCollapseToggle collapse_;
   @SuppressWarnings("unused")
   private final LintManager lintManager_;
   private final DivElement summary_;
   private final Map<Integer,VisualModeChunkRowState> rowState_;
   private final TextEditingTarget target_;
   private final int markdownIndex_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
