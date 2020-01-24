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

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorConfig;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

// TODO: propogate edits as they happen (w/ 1 second delay)
// TODO: shortcut overlap / routing / remapping
// TOOD: command / keyboard shortcut for entering visual mode
// TODO: panmirror outline visibility and width
// TODO: introduce global pref to toggle availabilty of visual mode
// TODO: disable additional source-editing commands in visual mode
// TODO: wire up find and replace actions to panmirror stubs
// TODO: bump toolbar icons down 1 pixel
// TODO: standard editor dialog boxes

public class TextEditingTargetVisualMode 
{
   public TextEditingTargetVisualMode(TextEditingTarget.Display display,
                                      DocUpdateSentinel docUpdateSentinel)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      display_ = display;
      docUpdateSentinel_ = docUpdateSentinel;
      manageUI();
      docUpdateSentinel_.addPropertyValueChangeHandler(PROPERTY_RMD_VISUAL_MODE, (value) -> {
         manageUI();
      });
   } 
   
   @Inject
   public void initialize(Commands commands)
   {
      commands_ = commands;
   }
 
   private void manageUI()
   {
      // always manage commands
      manageCommands();
      
      // get references to the editing container and it's source editor
      TextEditorContainer editorContainer = display_.editorContainer();
      TextEditorContainer.Editor editor = editorContainer.getEditor();
      boolean visualEditorActive = editorContainer.isWidgetActive(panmirror_);
      
      withPanmirror(() -> {
         // activate visual mode
         if (isActive())
         {
            if (!visualEditorActive)
               panmirror_.setMarkdown(editor.getCode(), true, (completed) -> {});
            editorContainer.activateWidget(panmirror_);
         }
         // activate source mode
         else 
         {
            if (visualEditorActive)
               panmirror_.getMarkdown(markdown -> { editor.setCode(markdown); });
            editorContainer.activateEditor(); 
         }
      });
   }
   
   private boolean isActive()
   {
      return docUpdateSentinel_.getBoolProperty(PROPERTY_RMD_VISUAL_MODE, false);
   }
   
   private void withPanmirror(Command ready)
   {
      if (panmirror_ == null)
      {
         PanmirrorConfig config = new PanmirrorConfig();
         config.options.rmdCodeChunks = true;
         PanmirrorWidget.Options options = new PanmirrorWidget.Options();
         
         PanmirrorWidget.create(config, options, (panmirror) -> {
            panmirror_ = panmirror;
            ready.execute();
         });
      }
      else
      {
         ready.execute();
      }
   }
   
   private void manageCommands()
   {
      boolean visualMode = isActive();
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
   private final TextEditingTarget.Display display_;
   private DocUpdateSentinel docUpdateSentinel_;
   
   private PanmirrorWidget panmirror_;
   
   private static final String PROPERTY_RMD_VISUAL_MODE = "rmdVisualMode";
}
