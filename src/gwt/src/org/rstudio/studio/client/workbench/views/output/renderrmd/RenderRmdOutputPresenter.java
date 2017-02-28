/*
 * RenderRmdOutputPresenter.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.output.renderrmd;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderCompletedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderOutputEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderStartedEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleActivateEvent;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneDisplay;
import org.rstudio.studio.client.workbench.views.output.common.CompileOutputPaneFactory;

public class RenderRmdOutputPresenter extends BusyPresenter
   implements RmdRenderStartedEvent.Handler,
              RmdRenderOutputEvent.Handler,
              RmdRenderCompletedEvent.Handler,
              RestartStatusEvent.Handler
{
   @Inject
   public RenderRmdOutputPresenter(CompileOutputPaneFactory outputFactory,
                                   RMarkdownServerOperations server,
                                   GlobalDisplay globalDisplay,
                                   EventBus events)
   {
      super(outputFactory.create("R Markdown", 
                                 "View the R Markdown render log"));
      view_ = (CompileOutputPaneDisplay) getView();
      view_.setHasLogs(false);
      server_ = server;
      events_ = events;

      view_.stopButton().addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            terminateRenderRmd();
         }
      });
      
      view_.errorList().addSelectionCommitHandler(
                              new SelectionCommitHandler<CodeNavigationTarget>() {

         @Override
         public void onSelectionCommit(
                              SelectionCommitEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            FileSystemItem fsi = FileSystemItem.createFile(target.getFile());
            RStudioGinjector.INSTANCE.getFileTypeRegistry()
               .editFile(fsi, target.getPosition());
         }
      });
      globalDisplay_ = globalDisplay;
   }
   
   public void initialize()
   {
   }

   public void confirmClose(final Command onConfirmed)
   {
      // if we're in the middle of rendering, presume that the user might be
      // trying to end the render by closing the tab.
      if (isBusy())
      {
        globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
              "Stop R Markdown Rendering", 
              "The rendering of '" + targetFile_ + "' is in progress. Do you "+
              "want to terminate and close the tab?", false, 
              new Operation()
              {
                 @Override
                 public void execute()
                 {
                    // Close the tab immediately and terminate the knit
                    // asynchronously
                    terminateRenderRmd();
                    onConfirmed.execute();
                 }
              }, null, null, "Stop", "Cancel", true);
      }
      else
      {
        onConfirmed.execute();
      }
   }

   @Override
   public void onRmdRenderStarted(RmdRenderStartedEvent event)
   {
      switchToConsoleAfterRender_ = !view_.isEffectivelyVisible();
      view_.ensureVisible(true);
      view_.compileStarted(event.getTargetFile());
      targetFile_ = event.getTargetFile();
      setIsBusy(true);
   }

   @Override
   public void onRmdRenderOutput(RmdRenderOutputEvent event)
   {
      view_.showOutput(event.getOutput(), true);
   }
   
   @Override
   public void onRmdRenderCompleted(RmdRenderCompletedEvent event)
   {
      view_.compileCompleted();
      setIsBusy(false);
      if (event.getResult().getSucceeded() && switchToConsoleAfterRender_)
      {
         events_.fireEvent(new ConsoleActivateEvent(false)); 
      }
      else if (!event.getResult().getSucceeded())
      {
         view_.ensureVisible(true);
      }
      if (!event.getResult().getSucceeded() && 
          SourceMarker.showErrorList(event.getResult().getKnitrErrors()))
      {
         view_.showErrors(event.getResult().getKnitrErrors());
      }
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // when the restart finishes, clean up the view in case we didn't get a
      // RmdCompletedEvent
      if (event.getStatus() != RestartStatusEvent.RESTART_COMPLETED ||
          !isBusy())
         return;

      view_.compileCompleted();
      setIsBusy(false);
      if (switchToConsoleAfterRender_)
      {
         events_.fireEvent(new ConsoleActivateEvent(false)); 
      }
   }
   
   private void terminateRenderRmd()
   {
      server_.terminateRenderRmd(false, new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            setIsBusy(false);
         }

         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Knit Terminate Failed", 
                  error.getMessage());
         }
      });
   }
   
   private final RMarkdownServerOperations server_;
   private final CompileOutputPaneDisplay view_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   
   private boolean switchToConsoleAfterRender_ = false;
   private String targetFile_;
}