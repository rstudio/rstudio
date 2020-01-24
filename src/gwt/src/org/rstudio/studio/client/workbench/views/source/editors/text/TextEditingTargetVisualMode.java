/*
 * TextEditingTargetVisualMode.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorConfig;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;


// TODO: should we be tied to the idle pref or use a different value?

// TODO: command enablement: more + move to inside adaptToFileType 
// TODO: shortcut overlap / routing / remapping
// TOOD: command / keyboard shortcut for entering visual mode
// TODO: panmirror outline visibility and width
// TODO: introduce global pref to toggle availabilty of visual mode
// TODO: disable additional source-editing commands in visual mode
// TODO: wire up find and replace actions to panmirror stubs
// TODO: apply themeing
// TODO: accessibility pass

// TODO: move into workbench (has deps there)
// TODO: copyright headers
// TODO: standard editor dialog boxes

public class TextEditingTargetVisualMode 
{
   public TextEditingTargetVisualMode(TextEditingTarget.Display display,
                                      DirtyState dirtyState,
                                      DocUpdateSentinel docUpdateSentinel)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      display_ = display;
      dirtyState_ = dirtyState;
      docUpdateSentinel_ = docUpdateSentinel;
      
      // manage ui based on current pref + changes over time
      manageUI();
      docUpdateSentinel_.addPropertyValueChangeHandler(PROPERTY_RMD_VISUAL_MODE, (value) -> {
         manageUI();
      });
   } 
   
   @Inject
   public void initialize(UserPrefs prefs, Commands commands, SourceServerOperations source)
   {
      commands_ = commands;
      prefs_ = prefs;
      source_ = source;
   }
   
   public void sync()
   {
      sync(null);
   }
   
   public void sync(Command ready)
   {
      // if panmirror is active then generate markdown, sync 
      // it to the editor, then clear the dirty flag
      if (isPanmirrorActive())
      {
         panmirror_.getMarkdown(markdown -> { 
            getSourceEditor().setCode(markdown); 
            isDirty_ = false;
            if (ready != null)
               ready.execute();
         });
      }
      // otherwise just return (no-op)
      else
      {
         if (ready != null)
            ready.execute();
      }
   }
 
   private void manageUI()
   {
      // always manage commands
      manageCommands();
      
      // get references to the editing container and it's source editor
      TextEditorContainer editorContainer = display_.editorContainer();
      TextEditorContainer.Editor editor = editorContainer.getEditor();
      
      withPanmirror(() -> {
         
         // visual mode active
         if (isVisualMode())
         {
            // if we aren't currently active then set our markdown based
            // on what's currently in the source ditor
            if (!isPanmirrorActive()) 
            {
               panmirror_.setMarkdown(editor.getCode(), false, (completed) -> {
                  isDirty_ = false;
               });
            }
            
            // activate panmirror and begin sync-on-idle behavior
            editorContainer.activateWidget(panmirror_);
            syncOnIdle_.resume();
         }
         
         // source mode active
         else 
         {
            // sync any pending edits, then activate the editor
            sync(() -> {
               editorContainer.activateEditor(); 
               syncOnIdle_.suspend();
            });  
         }
      });
   }
  
   
   private void withPanmirror(Command ready)
   {
      if (panmirror_ == null)
      {
         PanmirrorConfig config = new PanmirrorConfig();
         config.options.rmdCodeChunks = true;
         PanmirrorWidget.Options options = new PanmirrorWidget.Options();
         
         PanmirrorWidget.create(config, options, (panmirror) -> {
            
            // save reference to panmirror
            panmirror_ = panmirror;
            
            // periodically sync edits back to main editor
            syncOnIdle_ = new DebouncedCommand(prefs_.autoSaveMs())
            {
               @Override
               protected void execute()
               {
                  if (isDirty_)
                     sync();
               }
            };
            
            // set dirty flag on change
            panmirror_.addChangeHandler(new ChangeHandler() {
               @Override
               public void onChange(ChangeEvent event)
               {
                  // set flag and nudge sync on idle
                  isDirty_ = true;
                  syncOnIdle_.nudge();
                  
                  // update editor dirty state if necessary
                  if (!dirtyState_.getValue())
                  {
                     dirtyState_.markDirty(false);
                     source_.setSourceDocumentDirty(
                           docUpdateSentinel_.getId(), true, 
                           new VoidServerRequestCallback());
                  }
               }  
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
    
   // has the user requested visual mode?
   private boolean isVisualMode()
   {
      return docUpdateSentinel_.getBoolProperty(PROPERTY_RMD_VISUAL_MODE, false);
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
   
   private void manageCommands()
   {
      boolean visualMode = isVisualMode();
      commands_.goToNextChunk().setEnabled(!visualMode);
      commands_.goToPrevChunk().setEnabled(!visualMode);
      commands_.goToNextSection().setEnabled(!visualMode);
      commands_.goToPrevSection().setEnabled(!visualMode);
      commands_.insertChunk().setEnabled(!visualMode);
      commands_.executePreviousChunks().setEnabled(!visualMode);
      commands_.executeSubsequentChunks().setEnabled(!visualMode);
      commands_.executeCurrentChunk().setEnabled(!visualMode);
      commands_.executeNextChunk().setEnabled(!visualMode);
      commands_.checkSpelling().setEnabled(!visualMode);
      display_.manageCommandUI();
   } 
   
   private Commands commands_;
   private UserPrefs prefs_;
   private SourceServerOperations source_;
   
   private final TextEditingTarget.Display display_;
   private final DirtyState dirtyState_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private DebouncedCommand syncOnIdle_; 
   private boolean isDirty_ = false;
   
   private PanmirrorWidget panmirror_;
   
   private static final String PROPERTY_RMD_VISUAL_MODE = "rmdVisualMode";
}
