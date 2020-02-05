/*
 * TextEditingTargetVisualMode.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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

import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.Panmirror;
import org.rstudio.studio.client.panmirror.PanmirrorConfig;
import org.rstudio.studio.client.panmirror.PanmirrorEditingLocation;
import org.rstudio.studio.client.panmirror.PanmirrorKeybindings;
import org.rstudio.studio.client.panmirror.PanmirrorUIContext;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.PanmirrorWriterOptions;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommands;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;


// TODO: make line endings configurable
// TODO: link to docs
// TODO: fix the way list indending is done
// TODO: windows deps/build

// TODO: wire up find and replace actions to panmirror stubs

// TODO: standard editor dialog boxes

public class TextEditingTargetVisualMode 
{
   public TextEditingTargetVisualMode(TextEditingTarget target,
                                      TextEditingTarget.Display display,
                                      DirtyState dirtyState,
                                      DocUpdateSentinel docUpdateSentinel)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      target_ = target;
      display_ = display;
      dirtyState_ = dirtyState;
      docUpdateSentinel_ = docUpdateSentinel;
      
      // manage ui based on current pref + changes over time
      manageUI(isEnabled(), false);
      onDocPropChanged(TextEditingTarget.RMD_VISUAL_MODE, (value) -> {
         manageUI(isEnabled(), true);
      });
      
      // sync to outline visible prop
      onDocPropChanged(TextEditingTarget.DOC_OUTLINE_VISIBLE, (value) -> {
         panmirror_.showOutline(getOutlineVisible(), getOutlineWidth(), true);
      });
      
      // sync to user pref changed
      prefs_.enableVisualMarkdownEditingMode().addValueChangeHandler((value) -> {
         display_.manageCommandUI();
      });
      
      // changes to line wrapping prefs make us dirty
      prefs_.visualMarkdownEditingWrapAuto().addValueChangeHandler((value) -> {
         isDirty_ = true;
      });
      prefs_.visualMarkdownEditingWrapColumn().addValueChangeHandler((value) -> {
         isDirty_ = true;
      });
   } 
   
   @Inject
   public void initialize(Commands commands, UserPrefs prefs, SourceServerOperations source)
   {
      commands_ = commands;
      prefs_ = prefs;
      source_ = source;
   }
   
  
   public void deactivate(ScheduledCommand completed)
   {
      if (isEnabled())
      {
         docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
         manageUI(false, true, completed);
      }
   }
   
   public void sync()
   {
      sync(null);
   }
   
   public void sync(Command ready)
   {
      // if panmirror is active then generate markdown & sync it to the editor
      if (isPanmirrorActive() && isDirty_)
      {
         withPanmirror(() -> {
            PanmirrorWriterOptions options = new PanmirrorWriterOptions();
            if (prefs_.visualMarkdownEditingWrapAuto().getValue())
               options.wrapColumn = prefs_.visualMarkdownEditingWrapColumn().getValue();
            panmirror_.getMarkdown(options, markdown -> { 
               getSourceEditor().setCode(markdown, true); 
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
 
   public void manageCommands()
   {
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
        commands_.notebookExpandAllOutput(),
        commands_.notebookCollapseAllOutput(),
        commands_.notebookClearAllOutput(),
        commands_.notebookClearOutput(),
        commands_.goToLine(),
        commands_.wordCount(),
        commands_.checkSpelling(),
        commands_.restartRClearOutput(),
        commands_.restartRRunAllChunks(),
        commands_.profileCode()
      );
   } 
   
   private boolean isEnabled()
   {
      return docUpdateSentinel_.getBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
   }
   
  
   private void manageUI(boolean enable, boolean focus)
   {
      manageUI(enable, focus, () -> {});
   }
   
   private void manageUI(boolean enable, boolean focus, ScheduledCommand completed)
   {
      // manage commands
      manageCommands();
      
      // manage toolbar buttons / menus in display
      display_.manageCommandUI();
      
      // get references to the editing container and it's source editor
      TextEditorContainer editorContainer = display_.editorContainer();
      TextEditorContainer.Editor editor = editorContainer.getEditor();
        
      // visual mode enabled (panmirror editor)
      if (enable)
      {
         Command activate = () -> {
            // sync to editor outline prefs
            panmirror_.showOutline(getOutlineVisible(), getOutlineWidth());
            
            // activate widget
            editorContainer.activateWidget(panmirror_, focus);
            
            // begin save-on-idle behavior
            syncOnIdle_.resume();
            saveLocationOnIdle_.resume();
         
            // execute completed hook
            Scheduler.get().scheduleDeferred(completed);    
         };
         
         withPanmirror(() -> {
            // if we aren't currently active then set our markdown based
            // on what's currently in the source ditor
            if (!isPanmirrorActive()) 
            {
               loadingFromSource_ = true;
               panmirror_.setMarkdown(editor.getCode(), true, (done) -> {  
                  
                  isDirty_ = false;
                  loadingFromSource_ = false;
                  
                  // activate editor
                  activate.execute();
                  
                  // restore selection if we have one
                  Scheduler.get().scheduleDeferred(() -> {
                     PanmirrorEditingLocation location = savedEditingLocation();
                     if (location != null)
                        panmirror_.restoreEditingLocation(location);
                     if (focus)
                       panmirror_.focus();
                  });                
               });
            }
            else
            {
               activate.execute();
            }  
         });
      }
      
      // visual mode not enabled (source editor)
      else 
      {
         // sync any pending edits, then activate the editor
         sync(() -> {
            
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
   
   
   private void withPanmirror(Command ready)
   {
      if (panmirror_ == null)
      {
         // create panmirror
         PanmirrorConfig config = new PanmirrorConfig(uiContext());
         
         // options
         config.options.rmdCodeChunks = true;
           
         PanmirrorWidget.Options options = new PanmirrorWidget.Options();
         PanmirrorWidget.create(config, options, (panmirror) -> {
            
            // save reference to panmirror
            panmirror_ = panmirror;
            
            // remove some keybindings that conflict with the ide
            PanmirrorCommands commands = Panmirror.EditorCommands;
            disableKeys(
               commands.Paragraph, 
               commands.Heading1, commands.Heading2, commands.Heading3,
               commands.Heading4, commands.Heading5, commands.Heading6,
               commands.BulletList, commands.OrderedList, commands.TightList
            );
           
            // periodically sync edits back to main editor
            syncOnIdle_ = new DebouncedCommand(1000)
            {
               @Override
               protected void execute()
               {
                  if (isDirty_)
                     sync();
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
   
   // is our widget active in the editor container
   private boolean isPanmirrorActive()
   {
      return display_.editorContainer().isWidgetActive(panmirror_);
   }
   
   private TextEditorContainer.Editor getSourceEditor()
   {
      return display_.editorContainer().getEditor();
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
            command.setEnabled(!isEnabled());
      }
   }
   
   private void onDocPropChanged(String prop, ValueChangeHandler<String> handler)
   {
      docUpdateSentinel_.addPropertyValueChangeHandler(prop, handler);
   }
   
   private PanmirrorUIContext uiContext()
   {
      PanmirrorUIContext uiContext = new PanmirrorUIContext();
      uiContext.translateResourcePath = path -> {
         if (docUpdateSentinel_.getPath() != null)
            return ImagePreviewer.imgSrcPathFromHref(docUpdateSentinel_, path);
         else
            return path;
      };
      return uiContext;
   }
  
   
   private Commands commands_;
   private UserPrefs prefs_;
   private SourceServerOperations source_;
   
   private final TextEditingTarget target_;
   private final TextEditingTarget.Display display_;
   private final DirtyState dirtyState_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private DebouncedCommand syncOnIdle_; 
   private boolean isDirty_ = false;
   private boolean loadingFromSource_ = false;
   
   private DebouncedCommand saveLocationOnIdle_;
   
   private PanmirrorWidget panmirror_;
   
   private static final String RMD_VISUAL_MODE_LOCATION = "rmdVisualModeLocation";   
}



