/*
 * SourceCppOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.sourcecpp;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
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

   public interface Binder extends CommandBinder<Commands, SourceCppOutputPresenter> {}

   @Inject
   public SourceCppOutputPresenter(Display view,
                                   FileTypeRegistry fileTypeRegistry,
                                   Commands commands,
                                   Binder binder,
                                   UserPrefs uiPrefs)
   {
      super(view);
      view_ = view;
      fileTypeRegistry_ = fileTypeRegistry;
      uiPrefs_ = uiPrefs;
      commands_ = commands;
      binder.bind(commands, this);

      view_.errorList().addSelectionCommitHandler(
         (SelectionCommitEvent<CodeNavigationTarget> event) ->
         {
            CodeNavigationTarget target = event.getSelectedItem();
            FileSystemItem fsi = FileSystemItem.createFile(target.getFile());
            fileTypeRegistry_.editFile(fsi, target.getPosition());
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

   @Handler
   public void onActivateSourceCpp()
   {
      // Ensure that console pane is not minimized
      commands_.activateConsolePane().execute();
      view_.bringToFront();
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
         SourceMarker error = SourceMarker.getFirstError(state.getErrors());
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
   private final UserPrefs uiPrefs_;
   private final Commands commands_;
}
