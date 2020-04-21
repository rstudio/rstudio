/*
 * TextEditingTargetVisualMode.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.HasFindReplace;
import org.rstudio.core.client.widget.ProgressPanel;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.panmirror.PanmirrorContext;
import org.rstudio.studio.client.panmirror.PanmirrorEditingLocation;
import org.rstudio.studio.client.panmirror.PanmirrorKeybindings;
import org.rstudio.studio.client.panmirror.PanmirrorOptions;
import org.rstudio.studio.client.panmirror.PanmirrorUIContext;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.PanmirrorWidget.FormatSource;
import org.rstudio.studio.client.panmirror.PanmirrorWriterOptions;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommands;
import org.rstudio.studio.client.panmirror.format.PanmirrorExtendedDocType;
import org.rstudio.studio.client.panmirror.format.PanmirrorFormat;
import org.rstudio.studio.client.panmirror.format.PanmirrorHugoExtensions;
import org.rstudio.studio.client.panmirror.format.PanmirrorRmdExtensions;
import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocFormat;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorFormatComment;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;


public class TextEditingTargetVisualMode
{
   public TextEditingTargetVisualMode(TextEditingTarget target,
                                      TextEditingTarget.Display view,
                                      DocDisplay docDisplay,
                                      DirtyState dirtyState,
                                      DocUpdateSentinel docUpdateSentinel,
                                      EventBus eventBus,
                                      final ArrayList<HandlerRegistration> releaseOnDismiss)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      target_ = target;
      view_ = view;
      docDisplay_ = docDisplay;
      dirtyState_ = dirtyState;
      docUpdateSentinel_ = docUpdateSentinel;
      progress_ = new ProgressPanel(ProgressImages.createSmall(), 200);
      
      // if visual mode isn't enabled then reflect that (if it is enabled we'll
      // defer initialization work until after the tab is actually activated)
      if (!isActivated())
         manageUI(false, false);
      
      // track changes over time
      releaseOnDismiss.add(onDocPropChanged(TextEditingTarget.RMD_VISUAL_MODE, (value) -> {
         manageUI(isActivated(), true);
      }));
      
      // sync to outline visible prop
      releaseOnDismiss.add(onDocPropChanged(TextEditingTarget.DOC_OUTLINE_VISIBLE, (value) -> {
         withPanmirror(() -> {
            panmirror_.showOutline(getOutlineVisible(), getOutlineWidth(), true);
         });
      }));
      
      // sync to user pref changed
      releaseOnDismiss.add(prefs_.enableVisualMarkdownEditingMode().addValueChangeHandler((value) -> {
         view_.manageCommandUI();
      }));
      
      // changes to line wrapping prefs make us dirty
      releaseOnDismiss.add(prefs_.visualMarkdownEditingWrapAuto().addValueChangeHandler((value) -> {
         isDirty_ = true;
      }));
      releaseOnDismiss.add(prefs_.visualMarkdownEditingWrapColumn().addValueChangeHandler((value) -> {
         isDirty_ = true;
      }));
   } 
   
   @Inject
   public void initialize(Commands commands, 
                          UserPrefs prefs, 
                          SourceServerOperations source, 
                          WorkbenchContext context,
                          Session session)
   {
      commands_ = commands;
      prefs_ = prefs;
      source_ = source;
      context_ = context;
      sessionInfo_ = session.getSessionInfo();
   }
   
   public boolean isActivated()
   {
      return docUpdateSentinel_.getBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
   }
   
  
   public void deactivate(ScheduledCommand completed)
   {
      if (isActivated())
      {
         docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
         manageUI(false, true, completed);
      }
      else
      {
         completed.execute();
      }
   }
   
   public void syncToEditor(boolean activatingEditor)
   {
      syncToEditor(activatingEditor, null);
   }
   
   public void syncToEditor(boolean activatingEditor, Command ready)
   {
      // if panmirror is active then generate markdown & sync it to the editor
      if (isPanmirrorActive() && (activatingEditor || isDirty_))
      {
         withPanmirror(() -> {
            PanmirrorWriterOptions options = new PanmirrorWriterOptions();
            options.atxHeaders = true;
            if (prefs_.visualMarkdownEditingWrapAuto().getValue())
               options.wrapColumn = prefs_.visualMarkdownEditingWrapColumn().getValue();
            panmirror_.getMarkdown(options, activatingEditor, markdown -> { 
               TextEditorContainer.EditorCode editorCode = new TextEditorContainer.EditorCode(markdown);
               getSourceEditor().setCode(editorCode, true, activatingEditor); 
               isDirty_ = false;
               if (ready != null)
                  ready.execute();
            });
         });
      }
      // otherwise just return (no-op)
      else
      {
         if (ready != null)
            ready.execute();
      }
   }
   
   
   public void syncFromEditor(Command ready, boolean focus)
   {      
      // flag to prevent the document being set to dirty when loading
      // from source mode
      loadingFromSource_ = true;
      
      // if there is a previous format comment and it's changed then
      // we need to tear down the editor instance and create a new one
      if (panmirrorFormatComment_ != null && panmirrorFormatComment_.hasChanged()) 
      {
         panmirrorFormatComment_ = null;
         view_.editorContainer().removeWidget(panmirror_);
         panmirror_ = null;
      }
      
      withPanmirror(() -> {
         
         panmirror_.setMarkdown(getEditorCode(), true, (done) -> {  
               
            // activate editor
            if (ready != null)
               ready.execute();
            
            isDirty_ = false;
            loadingFromSource_ = false;
          
            // restore selection if we have one
            Scheduler.get().scheduleDeferred(() -> {
               
               // restore saved location
               PanmirrorEditingLocation location = savedEditingLocation();
               if (location != null)
                  panmirror_.restoreEditingLocation(location);
               
               if (focus)
                 panmirror_.focus();
               
               // show any format or extension warnings
               PanmirrorPandocFormat format = panmirror_.getPandocFormat();
               if (format.warnings.invalidFormat.length() > 0)
               {
                  view_.showWarningBar("Invalid Pandoc format: " + format.warnings.invalidFormat);
               }
               else if (format.warnings.invalidOptions.length > 0)
               {
                  view_.showWarningBar("Pandoc format " + format.baseName + " does not support options: " + 
                                          String.join(", ", format.warnings.invalidOptions));
               }
            });                
         });
      });
   }
   
   public void syncFromEditorIfActivated()
   {
      if (isActivated()) 
      {
         // get reference to the editing container 
         TextEditorContainer editorContainer = view_.editorContainer();
         
         // show progress
         progress_.beginProgressOperation(400);
         editorContainer.activateWidget(progress_);
         
         syncFromEditor(() -> {
            // clear progress
            progress_.endProgressOperation();
            
            // re-activate panmirror widget
            editorContainer.activateWidget(panmirror_, false);
            
         }, false);
      }
   }
   
 
   public void manageCommands()
   {
      if (isActivated())
      {
         // if this is the first time we've switched to the doc
         // while in visual mode then complete initialization
         if (!haveEditedInVisualMode_)
         {
            haveEditedInVisualMode_ = true;
            manageUI(true, true);
         }
         else
         {
            syncDevTools();
         }
      }
      
      disableForVisualMode(
        commands_.insertChunk(),
        commands_.jumpTo(),
        commands_.jumpToMatching(),
        commands_.showDiagnosticsActiveDocument(),
        commands_.goToHelp(),
        commands_.goToDefinition(),
        commands_.extractFunction(),
        commands_.extractLocalVariable(),
        commands_.renameInScope(),
        commands_.reflowComment(),
        commands_.commentUncomment(),
        commands_.insertRoxygenSkeleton(),
        commands_.reindent(),
        commands_.reformatCode(),
        commands_.findSelectAll(),
        commands_.findFromSelection(),
        commands_.executeSetupChunk(),
        commands_.executeAllCode(),
        commands_.executeCode(),
        commands_.executeCodeWithoutFocus(),
        commands_.executeCodeWithoutMovingCursor(),
        commands_.executeCurrentChunk(),
        commands_.executeCurrentFunction(),
        commands_.executeCurrentLine(),
        commands_.executeCurrentParagraph(),
        commands_.executeCurrentSection(),
        commands_.executeCurrentStatement(),
        commands_.executeFromCurrentLine(),
        commands_.executeLastCode(),
        commands_.executeNextChunk(),
        commands_.executePreviousChunks(),
        commands_.executeSubsequentChunks(),
        commands_.executeToCurrentLine(),
        commands_.sendToTerminal(),
        commands_.runSelectionAsJob(),
        commands_.runSelectionAsLauncherJob(),
        commands_.sourceActiveDocument(),
        commands_.sourceActiveDocumentWithEcho(),
        commands_.pasteWithIndentDummy(),
        commands_.fold(),
        commands_.foldAll(),
        commands_.unfold(),
        commands_.unfoldAll(),
        commands_.yankAfterCursor(),
        commands_.notebookExpandAllOutput(),
        commands_.notebookCollapseAllOutput(),
        commands_.notebookClearAllOutput(),
        commands_.notebookClearOutput(),
        commands_.goToLine(),
        commands_.wordCount(),
        commands_.restartRClearOutput(),
        commands_.restartRRunAllChunks(),
        commands_.profileCode()
      );
   }
   
   public HasFindReplace getFindReplace()
   {
      if (panmirror_ != null) {
         return panmirror_.getFindReplace();
      } else {
         return new HasFindReplace() {
            public boolean isFindReplaceShowing() { return false; }
            public void showFindReplace(boolean defaultForward) {}
            public void hideFindReplace() {}
            public void findNext() {}
            public void findPrevious() {}
            public void replaceAndFind() {}
            
         };
      }  
   }
   
   public void activateDevTools()
   {
      withPanmirror(() -> {
         panmirror_.activateDevTools();
      });
   }
   
   public void onClosing()
   {
      if (syncOnIdle_ != null)
         syncOnIdle_.suspend();
      if (saveLocationOnIdle_ != null)
         saveLocationOnIdle_.suspend();
   }
  
  
   private void manageUI(boolean activate, boolean focus)
   {
      manageUI(activate, focus, () -> {});
   }
   
   private void manageUI(boolean activate, boolean focus, ScheduledCommand completed)
   {
      // manage commands
      manageCommands();
      
      // manage toolbar buttons / menus in display
      view_.manageCommandUI();
      
      // get references to the editing container and it's source editor
      TextEditorContainer editorContainer = view_.editorContainer();
        
      // visual mode enabled (panmirror editor)
      if (activate)
      {
         // show progress (as this may well require either loading the 
         // panmirror library for the first time or a reload of visual mode,
         // which is normally instant but for very, very large documents
         // can take a couple of seconds)
         progress_.beginProgressOperation(400);
         editorContainer.activateWidget(progress_);
         
         Command activator = () -> {
            
            // clear progress
            progress_.endProgressOperation();
            
            // sync to editor outline prefs
            panmirror_.showOutline(getOutlineVisible(), getOutlineWidth());
            
            // activate widget
            editorContainer.activateWidget(panmirror_, focus);
            
            // begin save-on-idle behavior
            syncOnIdle_.resume();
            saveLocationOnIdle_.resume();
            
            // activate devtools if they are loaded
            syncDevTools();
               
            // execute completed hook
            Scheduler.get().scheduleDeferred(completed);    
         };
         
         withPanmirror(() -> {
            // if we aren't currently active then set our markdown based
            // on what's currently in the source ditor
            if (!isPanmirrorActive()) 
            {
               syncFromEditor(activator, focus);
            }
            else
            {
               activator.execute();
            }  
         });
      }
      
      // visual mode not enabled (source editor)
      else 
      {
         // sync any pending edits, then activate the editor
         syncToEditor(true, () -> {
            
            editorContainer.activateEditor(focus); 
            
            if (syncOnIdle_ != null)
               syncOnIdle_.suspend();
            
            if (saveLocationOnIdle_ != null)
               saveLocationOnIdle_.suspend();
            
            // execute completed hook
            Scheduler.get().scheduleDeferred(completed);
         });  
      }
   }
   
   private void syncDevTools()
   {
      if (panmirror_ != null && panmirror_.devToolsLoaded()) 
         panmirror_.activateDevTools();
   }
   
   
   private void withPanmirror(Command ready)
   {
      if (panmirror_ == null)
      {
         // create panmirror
         PanmirrorContext context = new PanmirrorContext(uiContext());
         PanmirrorOptions options = panmirrorOptions();   
         PanmirrorWidget.Options widgetOptions = new PanmirrorWidget.Options();
         PanmirrorWidget.create(context, panmirrorFormat(), options, widgetOptions, (panmirror) -> {
         
            // save reference to panmirror
            panmirror_ = panmirror;
            
            // track format comment (used to detect when we need to reload for a new format)
            panmirrorFormatComment_ = new FormatComment(new PanmirrorUITools().format);
            
            // remove some keybindings that conflict with the ide
            disableKeys(
               PanmirrorCommands.Paragraph, 
               PanmirrorCommands.Heading1, PanmirrorCommands.Heading2, PanmirrorCommands.Heading3,
               PanmirrorCommands.Heading4, PanmirrorCommands.Heading5, PanmirrorCommands.Heading6,
               PanmirrorCommands.TightList
            );
           
            // periodically sync edits back to main editor
            syncOnIdle_ = new DebouncedCommand(1000)
            {
               @Override
               protected void execute()
               {
                  if (isDirty_)
                     syncToEditor(false);
               }
            };
            
            // periodically save selection
            saveLocationOnIdle_ = new DebouncedCommand(1000)
            {
               @Override
               protected void execute()
               {
                  PanmirrorEditingLocation location = panmirror_.getEditingLocation();
                  String locationProp = + location.pos + ":" + location.scrollTop; 
                  docUpdateSentinel_.setProperty(RMD_VISUAL_MODE_LOCATION, locationProp);
               }
            };
            
            // set dirty flag + nudge idle sync on change
            panmirror_.addChangeHandler(new ChangeHandler() 
            {
               @Override
               public void onChange(ChangeEvent event)
               {
                  // set flag and nudge sync on idle
                  isDirty_ = true;
                  syncOnIdle_.nudge();
                  
                  // update editor dirty state if necessary
                  if (!loadingFromSource_ && !dirtyState_.getValue())
                  {
                     dirtyState_.markDirty(false);
                     source_.setSourceDocumentDirty(
                           docUpdateSentinel_.getId(), true, 
                           new VoidServerRequestCallback());
                  }
               }  
            });
            
            // save selection
            panmirror_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
            {
               @Override
               public void onSelectionChange(SelectionChangeEvent event)
               {
                  saveLocationOnIdle_.nudge();
               }
            });
             
            // track changes in outline sidebar and save as prefs
            panmirror_.addPanmirrorOutlineVisibleHandler((event) -> {
               setOutlineVisible(event.getVisible());
            });
            panmirror_.addPanmirrorOutlineWidthHandler((event) -> {
               setOutlineWidth(event.getWidth());
            });
           
            // good to go!
            ready.execute();
         });
      }
      else
      {
         // panmirror already created
         ready.execute();
      }
   } 
   
   // bizzarly, removing this method triggers a gwt compiler issue that
   // results in the Panmirror interop breaking! we need to investigate
   // this, but in the meantime the method remains. note that this method
   // *should not* be called as it doesn't handle re-entrancy correctly
   // (the restoring of the getActiveWidget can result in forever progress
   // when 2 calls to withProgress are in the promise chain)
   @SuppressWarnings("unused")
   private void withProgress(int delayMs, CommandWithArg<Command> command)
   {
     
      TextEditorContainer editorContainer = view_.editorContainer();
      /*
      IsHideableWidget prevWidget = editorContainer.getActiveWidget();
      progress_.beginProgressOperation(delayMs);
      editorContainer.activateWidget(progress_);
      command.execute(() -> {
         progress_.endProgressOperation();
         editorContainer.activateWidget(prevWidget);
      });
      */
   }
   
   private String getEditorCode()
   {
      TextEditorContainer editorContainer = view_.editorContainer();
      TextEditorContainer.Editor editor = editorContainer.getEditor();
      return editor.getCode();
   }   
   
   // is our widget active in the editor container
   private boolean isPanmirrorActive()
   {
      return view_.editorContainer().isWidgetActive(panmirror_);
   }
   
   private TextEditorContainer.Editor getSourceEditor()
   {
      return view_.editorContainer().getEditor();
   }
  
   private boolean getOutlineVisible()
   {
      return target_.getPreferredOutlineWidgetVisibility();
   }
   
   private void setOutlineVisible(boolean visible)
   {
      target_.setPreferredOutlineWidgetVisibility(visible);
   }
   
   private double getOutlineWidth()
   {
      return target_.getPreferredOutlineWidgetSize();
   }
   
   private void setOutlineWidth(double width)
   {
      target_.setPreferredOutlineWidgetSize(width);
   }
   
   
   private PanmirrorEditingLocation savedEditingLocation()
   {
      String location = docUpdateSentinel_.getProperty(RMD_VISUAL_MODE_LOCATION, null);
      if (StringUtil.isNullOrEmpty(location))
         return null;
      
      String[] parts = location.split(":");
      if (parts.length != 2)
         return null;
      
      try
      {
         PanmirrorEditingLocation editingLocation = new PanmirrorEditingLocation();
         editingLocation.pos = Integer.parseInt(parts[0]);
         editingLocation.scrollTop = Integer.parseInt(parts[1]);
         return editingLocation;
      }
      catch(Exception ex)
      {
         Debug.logException(ex);
         return null;
      }
      
   }
   
   private void disableKeys(String... commands)
   {
      PanmirrorKeybindings keybindings = disabledKeybindings(commands);
      panmirror_.setKeybindings(keybindings);
   }
   
   private PanmirrorKeybindings disabledKeybindings(String... commands)
   {
      PanmirrorKeybindings keybindings = new PanmirrorKeybindings();
      for (String command : commands)
         keybindings.add(command,  new String[0]);
      
      return keybindings;
   }
   
   private void disableForVisualMode(AppCommand... commands)
   {
      for (AppCommand command : commands)
      {
         if (command.isVisible())
            command.setEnabled(!isActivated());
      }
   }
   
   private HandlerRegistration onDocPropChanged(String prop, ValueChangeHandler<String> handler)
   {
      return docUpdateSentinel_.addPropertyValueChangeHandler(prop, handler);
   }
   
   private PanmirrorUIContext uiContext()
   {
      PanmirrorUIContext uiContext = new PanmirrorUIContext();
      uiContext.mapResourcePath = path -> {
         return ImagePreviewer.imgSrcPathFromHref(uiContext.getResourceDir.getResourceDir(), path);
      };
      uiContext.getResourceDir = () -> {  
         if (docUpdateSentinel_.getPath() != null)
            return FileSystemItem.createDir(docUpdateSentinel_.getPath()).getParentPathString();
         else
            return context_.getCurrentWorkingDir().getPath();
      };
      uiContext.translateText = text -> {
         return text;
      };
      return uiContext;
   }
   
   private PanmirrorOptions panmirrorOptions()
   {
      // create options
      PanmirrorOptions options = new PanmirrorOptions();
      
      // use embedded codemirror for code blocks
      options.codemirror = true;
      
      // enable rmdImagePreview if we are an executable rmd
      options.rmdImagePreview = target_.canExecuteChunks();
      
      // hide the format comment so that users must go into
      // source mode to change formats
      options.hideFormatComment = true;
      
      // add focus-visible class to prevent interaction with focus-visible.js
      // (it ends up attempting to apply the "focus-visible" class b/c ProseMirror
      // is contentEditable, and that triggers a dom mutation event for ProseMirror,
      // which in turn causes us to lose table selections)
      options.className = "focus-visible";
      
      return options;
   }
   
   private FormatSource panmirrorFormat()
   {
      return new PanmirrorWidget.FormatSource()
      {
         @Override
         public PanmirrorFormat getFormat(PanmirrorUIToolsFormat formatTools)
         {
            // create format
            PanmirrorFormat format = new PanmirrorFormat();
            
            // see if we have a format comment
            PanmirrorFormatComment formatComment = formatTools.parseFormatComment(getEditorCode());
            
            // pandocMode
            final String kBlackfriday = "blackfriday";
            String alternateMode = null;
            if (formatComment.mode != null)
               alternateMode = formatComment.mode;
            else if (enableBlogdownBlackfriday())
               alternateMode = kBlackfriday;
            
            // set alternate mode if we have one
            if (alternateMode != null)
            {
               format.pandocMode = alternateMode;
               format.pandocExtensions = "";
            }
            // otherwise we're using the default base for R Markdown
            else
            {
               format.pandocMode = "markdown";
               format.pandocExtensions = "+autolink_bare_uris+tex_math_single_backslash";
            }
               
            // custom pandocExtensions
            if (formatComment.extensions != null)
               format.pandocExtensions = formatComment.extensions;
           
            
            // rmdExtensions
            format.rmdExtensions = new PanmirrorRmdExtensions();
            format.rmdExtensions.codeChunks = target_.canExecuteChunks();
            
            // support for bookdown cross-references is always enabled b/c they would not 
            // serialize correctly in markdown modes that don't escape @ if not enabled,
            // and the odds that someone wants to literally write @ref(foo) w/o the leading
            // \ are vanishingly small)
            format.rmdExtensions.bookdownXRef = true;
            
            // support for bookdown part headers is always enabled b/c typing 
            // (PART\*) in the visual editor would result in an escaped \, which
            // wouldn't parse as a port. the odds of (PART\*) occurring naturally
            // in an H1 are also vanishingly small
            format.rmdExtensions.bookdownPart = true;
            
            // enable blogdown math in code (e.g. `$math$`) if we have a blogdown
            // doctype along with the blackfriday markdown engine
            format.rmdExtensions.blogdownMathInCode = 
               isBlogdownDocument() && format.pandocMode.equals(kBlackfriday);
            
            // hugoExtensions
            format.hugoExtensions = new PanmirrorHugoExtensions();
            
            // always enable hugo shortcodes (w/o this we can end up destroying
            // shortcodes during round-tripping, and we don't want to require that 
            // blogdown files be opened within projects). this idiom is obscure 
            // enough that it's vanishingly unlikely to affect non-blogdown docs
            format.hugoExtensions.shortcodes = true;
            
            // fillColumn
            format.wrapColumn = formatComment.fillColumn;
            
            // doctypes
            ArrayList<String> docTypes = new ArrayList<String>();
            if (formatComment.doctypes == null || formatComment.doctypes.length == 0)
            {
               if (isXRefDocument())
                  docTypes.add(PanmirrorExtendedDocType.xref);
               if (isBookdownDocument())
                  docTypes.add(PanmirrorExtendedDocType.bookdown);
               if (isBlogdownDocument()) 
               {
                  docTypes.add(PanmirrorExtendedDocType.blogdown);
                  docTypes.add(PanmirrorExtendedDocType.hugo);
               }  
               format.docTypes = docTypes.toArray(new String[] {});
            }
            
            // return format
            return format;
         }
      };
   }
   
   private boolean isXRefDocument()
   {
      return isBookdownDocument() || isBlogdownDocument() || isDistillDocument();
   }
   
   private boolean isBookdownDocument() 
   {
      return sessionInfo_.getBuildToolsBookdownWebsite();
   }
   
   private boolean isBlogdownDocument() 
   {
      return sessionInfo_.getIsBlogdownProject();
   }
   
   private boolean isDistillDocument()
   {
      return sessionInfo_.getIsDistillProject() ||
             getOutputFormats().contains("distill::distill_article");
   }
 
   // automatically enable blackfriday markdown engine if this is a blogdown
   // file that isn't an Rmd (e.g. .md or .Rmarkdown). 
   private boolean enableBlogdownBlackfriday()
   {
      // get current docPath
      String docPath = docUpdateSentinel_.getPath();
      
      // if we have a doc
      if (docPath != null)
      {
         // if we are in a blogdown project
         if (sessionInfo_.getIsBlogdownProject())
         {
            // if the doc is in the project directory
            FileSystemItem docFile = FileSystemItem.createFile(docPath);
            FileSystemItem projectDir = context_.getActiveProjectDir();
            if (docFile.getPathRelativeTo(projectDir) != null)
            {
               // if it has an extension indicating hugo will render markdown
               String extension = FileSystemItem.getExtensionFromPath(docPath);
               return extension.compareToIgnoreCase(".md") == 0 ||
                      extension.compareToIgnoreCase("Rmarkdown") == 0;
            }
         }
      }
      
      // default to false
      return false;
   }
   
  
   private List<String> getOutputFormats()
   {
      String yaml = YamlFrontMatter.getFrontMatter(docDisplay_);
      if (yaml == null)
      {
         return new ArrayList<String>();
      }
      else
      {
         List<String> formats = TextEditingTargetRMarkdownHelper.getOutputFormats(yaml);
         if (formats == null)
            return new ArrayList<String>();
         else
            return formats;   
      }
   }
   
   private class FormatComment
   {
      public FormatComment(PanmirrorUIToolsFormat formatTools)
      {
         formatTools_ = formatTools;
         comment_ = formatTools_.parseFormatComment(getEditorCode());
      }
      
      public boolean hasChanged()
      {
         PanmirrorFormatComment comment = formatTools_.parseFormatComment(getEditorCode());
         return !PanmirrorFormatComment.areEqual(comment,  comment_);   
      }
      
      private final PanmirrorUIToolsFormat formatTools_;
      private final PanmirrorFormatComment comment_;
   }
   
   
   private Commands commands_;
   private UserPrefs prefs_;
   private WorkbenchContext context_;
   private SessionInfo sessionInfo_;
   private SourceServerOperations source_;
   
   private final TextEditingTarget target_;
   private final TextEditingTarget.Display view_;
   private final DocDisplay docDisplay_;
   private final DirtyState dirtyState_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private DebouncedCommand syncOnIdle_; 
   private boolean isDirty_ = false;
   private boolean loadingFromSource_ = false;
   private boolean haveEditedInVisualMode_ = false; 
   
   private DebouncedCommand saveLocationOnIdle_;
   
   private PanmirrorWidget panmirror_;
   private FormatComment panmirrorFormatComment_;
   
   private final ProgressPanel progress_;
   
   private static final String RMD_VISUAL_MODE_LOCATION = "rmdVisualModeLocation";   
}



