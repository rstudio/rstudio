/*
 * BuildPresenter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildOutputEvent;
import org.rstudio.studio.client.workbench.views.buildtools.events.BuildStatusEvent;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildServerOperations;
import org.rstudio.studio.client.workbench.views.buildtools.model.BuildState;
import org.rstudio.studio.client.workbench.views.source.SourceShim;

public class BuildPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
      void buildStarted();
      
      void showOutput(String output);
      void scrollToBottom();
      
      void buildCompleted();
      
      HasClickHandlers stopButton();
   }
   
   @Inject
   public BuildPresenter(Display display, 
                         GlobalDisplay globalDisplay,
                         SourceShim sourceShim,
                         UIPrefs uiPrefs,
                         BuildServerOperations server,
                         EventBus eventBus)
   {
      super(display);
      view_ = display;
      server_ = server;
      globalDisplay_ = globalDisplay;
      sourceShim_ = sourceShim;
      uiPrefs_ = uiPrefs;
      
      eventBus.addHandler(BuildStatusEvent.TYPE, 
                          new BuildStatusEvent.Handler()
      {  
         @Override
         public void onBuildStatus(BuildStatusEvent event)
         {
            String status = event.getStatus();
            if (status.equals(BuildStatusEvent.STATUS_STARTED))
            {
               view_.buildStarted();
            }
            else if (status.equals(BuildStatusEvent.STATUS_COMPLETED))
            {
               view_.buildCompleted();
            }
         }
      });
      
      eventBus.addHandler(BuildOutputEvent.TYPE, 
                          new BuildOutputEvent.Handler()
      {
         @Override
         public void onBuildOutput(BuildOutputEvent event)
         {
            view_.showOutput(event.getOutput());
         }
      });
      
      
      view_.stopButton().addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            terminateBuild();
         }
      });
   }
   
   public void initialize(BuildState buildState)
   {
      view_.buildStarted();
      view_.showOutput(buildState.getOutput());
      if (!buildState.isRunning())
         view_.buildCompleted();
   }
   
  
   void onBuildAll()
   {
      startBuild("build-all");
   }
   
   void onCheckPackage()
   {
      startBuild("check-package");
   }
   
  
   void onRebuildAll()
   {
      startBuild("rebuild-all");
   }
   
   void onCleanAll()
   {
      startBuild("clean-all");
   }
   
   private void startBuild(final String type)
   {
      // attempt to start a build (this will be a silent no-op if there
      // is already a build running)
      Command buildCommand = new Command() {
         @Override
         public void execute()
         {
            server_.startBuild(type,
                  new DelayedProgressRequestCallback<Boolean>("Starting Build...") {
               @Override
               protected void onSuccess(Boolean response)
               {
                 
               }
            });
         }
      };
      
      if (uiPrefs_.saveAllBeforeBuild().getValue())
      {
         sourceShim_.saveAllUnsaved(buildCommand);
      }
      else
      {
         buildCommand.execute();
      }
   }
   
   private void terminateBuild()
   {
       server_.terminateBuild(new DelayedProgressRequestCallback<Boolean>(
                                                       "Terminating Build..."){
         @Override
         protected void onSuccess(Boolean response)
         {
            if (!response)
            {
               globalDisplay_.showErrorMessage(
                     "Error Terminating Build",
                     "Unable to terminate build. Please try again.");
            }
         }
       });  
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
   
   private final GlobalDisplay globalDisplay_;
   private final SourceShim sourceShim_;
   private final UIPrefs uiPrefs_;
   private final BuildServerOperations server_;
   private final Display view_ ; 
}
