/*
 * SourceCppOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.sourcecpp;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.compile.CompileError;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppCompletedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppStartedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.model.SourceCppState;


public class SourceCppOutputPresenter extends BasePresenter
   implements SourceCppStartedEvent.Handler,
              SourceCppCompletedEvent.Handler
{
   public interface Display extends WorkbenchView, HasEnsureHiddenHandlers
   {
      void ensureVisible(boolean activate);
      void clearAll();
      void scrollToBottom();
      void showResults(SourceCppState state);
      HasSelectionCommitHandlers<CodeNavigationTarget> errorList();
   }

   @Inject
   public SourceCppOutputPresenter(Display view,
                                   FileTypeRegistry fileTypeRegistry,
                                   UIPrefs uiPrefs)
   {
      super(view);
      view_ = view;
      fileTypeRegistry_ = fileTypeRegistry;
      uiPrefs_ = uiPrefs;
    
      view_.errorList().addSelectionCommitHandler(
                         new SelectionCommitHandler<CodeNavigationTarget>() {

         @Override
         public void onSelectionCommit(
                              SelectionCommitEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            FileSystemItem fsi = FileSystemItem.createFile(target.getFile());
            fileTypeRegistry_.editFile(fsi, target.getPosition());
         }
      });
   }
 
   @Override 
   public void onSourceCppStarted(SourceCppStartedEvent event)
   {
      view_.clearAll();
   }
   
   @Override 
   public void onSourceCppCompleted(SourceCppCompletedEvent event)
   {
      updateView(event.getState(), true);
   }
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      Scheduler.get().scheduleDeferred(new Command()
      {
         @Override
         public void execute()
         {
            view_.scrollToBottom();
         }
      });
   }

   private void updateView(SourceCppState state, boolean activate)
   {
      if (state.getErrors().length() > 0)
         view_.ensureVisible(activate);
      
      // show results   
      view_.showResults(state);
      
      // navigate to the first error
      if (uiPrefs_.navigateToBuildError().getValue())
      {
         CompileError error = CompileError.getFirstError(state.getErrors());
         if (error != null)
         {
            fileTypeRegistry_.editFile(
              FileSystemItem.createFile(error.getPath()),
              FilePosition.create(error.getLine(), error.getColumn()),
              true);
         }
      }
      
   }
   
   private final Display view_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final UIPrefs uiPrefs_;
}
