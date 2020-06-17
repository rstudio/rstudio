/*
 * TestsOutputPresenter.java
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

package org.rstudio.studio.client.workbench.views.output.tests;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.PaneManager;
import org.rstudio.studio.client.workbench.ui.PaneManager.Tab;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildCompletedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildErrorsEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStartedEvent;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneDisplay;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneFactory;

public class TestsOutputPresenter extends BusyPresenter
   implements BuildStartedEvent.Handler,
              BuildOutputEvent.Handler,
              BuildCompletedEvent.Handler,
              BuildErrorsEvent.Handler,
              RestartStatusEvent.Handler
{
   @Inject
   public TestsOutputPresenter(CompileOutputPaneFactory outputFactory,
                               BuildServerOperations server,
                               GlobalDisplay globalDisplay,
                               PaneManager paneManager,
                               Commands commands,
                               EventBus events)
   {
      super(outputFactory.create("Tests",
                                 "View test results"));
      view_ = (CompileOutputPaneDisplay) getView();
      view_.setHasLogs(false);
      server_ = server;
      paneManager_ = paneManager;

      view_.stopButton().addClickHandler(event ->
      {
         terminateTests();
      });

      view_.errorList().addSelectionCommitHandler((
            SelectionCommitEvent<CodeNavigationTarget> event) ->
      {
         CodeNavigationTarget target = event.getSelectedItem();
         FileSystemItem fsi = FileSystemItem.createFile(target.getFile());
         RStudioGinjector.INSTANCE.getFileTypeRegistry()
            .editFile(fsi, target.getPosition());
      });
      globalDisplay_ = globalDisplay;
   }

   public void initialize()
   {
   }

   public void confirmClose(final Command onConfirmed)
   {
      if (isBusy())
      {
         terminateTests();
      }

      onConfirmed.execute();
   }

   private boolean isEnabled()
   {
      return paneManager_.getTab(Tab.Build).isSuppressed();
   }

   @Override
   public void onBuildStarted(BuildStartedEvent event)
   {
      if (!isEnabled()) return;

      view_.ensureVisible(true);
      view_.compileStarted(event.getSubType());
      setIsBusy(true);
   }

   @Override
   public void onBuildOutput(BuildOutputEvent event)
   {
      if (!isEnabled()) return;

      view_.showOutput(event.getOutput(), true);
   }

   @Override
   public void onBuildCompleted(BuildCompletedEvent event)
   {
      if (!isEnabled()) return;

      view_.compileCompleted();
      setIsBusy(false);
      view_.ensureVisible(true);
   }

   @Override
   public void onBuildErrors(BuildErrorsEvent event)
   {
      if (!isEnabled()) return;

      view_.showErrors(event.getErrors());
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
   }

   private void terminateTests()
   {
      server_.terminateBuild(new DelayedProgressRequestCallback<Boolean>(
                                                       "Terminating Tests..."){
         @Override
         protected void onSuccess(Boolean response)
         {
            if (!response)
            {
               globalDisplay_.showErrorMessage(
                  "Error Terminating Tests",
                  "Unable to terminate tests. Please try again.");
            }
         }
      });
   }

   private final BuildServerOperations server_;
   private final CompileOutputPaneDisplay view_;
   private final GlobalDisplay globalDisplay_;
   private final PaneManager paneManager_;
}
