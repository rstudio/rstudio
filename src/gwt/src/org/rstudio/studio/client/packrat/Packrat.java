/*
 * Packrat.java
 *
 * Copyright (C) 2014 by RStudio, Inc.
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

package org.rstudio.studio.client.packrat;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.packrat.model.PackratContext;
import org.rstudio.studio.client.packrat.model.PackratRestoreActions;
import org.rstudio.studio.client.packrat.model.PackratStatus;
import org.rstudio.studio.client.packrat.ui.PackratRestoreDialog;
import org.rstudio.studio.client.packrat.ui.PackratStatusDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.remote.RemoteServer;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.packages.Packages;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class Packrat
{
   public interface Binder extends CommandBinder<Commands, Packrat> {}

   public Packrat(Packages.Display display)
   {
      display_ = display;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(
         Binder binder,
         Commands commands,
         EventBus eventBus,
         GlobalDisplay globalDisplay,
         WorkbenchContext workbenchContext,
         RemoteFileSystemContext fsContext,
         DependencyManager dependencyManager,
         PackratStatus prStatus,
         RemoteServer server,
         Provider<FileDialogs> pFileDialogs) {
      
      eventBus_ = eventBus;
      globalDisplay_ = globalDisplay;
      workbenchContext_ = workbenchContext;
      fsContext_ = fsContext;
      dependencyManager_ = dependencyManager;
      server_ = server;
      pFileDialogs_ = pFileDialogs;
      binder.bind(commands, this);
   }

   @Handler
   public void onPackratBootstrap() 
   {
      // get status
      server_.getPackratContext(
         new SimpleRequestCallback<PackratContext>() {
            @Override
            public void onResponseReceived(PackratContext context)
            {
               String message =
                  "Packrat is a dependency management tool that makes your " +
                  "R code more isolated, portable, and reproducible by " +
                  "giving your project its own privately managed package " +
                  "library.\n\n" +
                  "Do you want to use packrat with this project?";
               
               globalDisplay_.showYesNoMessage(
                   MessageDialog.QUESTION, 
                   "Use Packrat",
                   message,
                   new Operation() {

                     @Override
                     public void execute()
                     {
                        bootstrapPackrat();
                     }
                      
                   },
                   true);
            }
         });
   }
   
   private void bootstrapPackrat()
   {
      dependencyManager_.withDependencies(
         "Packrat", 
         new Dependency[] {
            Dependency.embeddedPackage("packrat")
         },
         new Command() {
            @Override
            public void execute()
            {
               server_.packratBootstrap(
                  workbenchContext_.getActiveProjectDir().getPath(), 
                  new VoidServerRequestCallback());
            } 
         });
   }
   
   
   @Handler
   public void onPackratHelp() 
   {
      globalDisplay_.openRStudioLink("packrat");
   }
   
   private void fireConsoleEvent(final String userAction) 
   {
      eventBus_.fireEvent(new SendToConsoleEvent(userAction, true, false));
   }

   @Handler
   public void onPackratSnapshot() 
   {
      fireConsoleEvent("packrat::snapshot()");
   }

   @Handler
   public void onPackratRestore() 
   {
      String projDir = workbenchContext_.getActiveProjectDir().getPath();
      
      // Ask the server for the current project status
      server_.getPackratRestoreActions(projDir, new ServerRequestCallback<JsArray<PackratRestoreActions>>() {
         
         @Override
         public void onResponseReceived(JsArray<PackratRestoreActions> prRestoreActions) {
            if (prRestoreActions == null) {
               globalDisplay_.showMessage(
                  GlobalDisplay.MSG_INFO,
                  "Restore packages...",
                  "All packages are up to date."
               );
            } else {
               new PackratRestoreDialog(prRestoreActions, eventBus_).showModal();
            }
         }

         @Override
         public void onError(ServerError error) {
            Debug.logError(error);
         }
         
      });
      
   }

   @Handler
   public void onPackratClean() 
   {
      fireConsoleEvent("packrat::clean()");
   }

   @Handler
   public void onPackratBundle() 
   {
      pFileDialogs_.get().saveFile(
            "Bundle Packrat Project...",
            fsContext_,
            workbenchContext_.getCurrentWorkingDir(),
            "zip",
            false,
            new ProgressOperationWithInput<FileSystemItem>() {

               @Override
               public void execute(FileSystemItem input,
                                   ProgressIndicator indicator) {

                  if (input == null)
                     return;

                  indicator.onCompleted();

                  String bundleFile = input.getPath();
                  if (bundleFile == null)
                     return;

                  StringBuilder cmd = new StringBuilder();
                  // We use 'overwrite = TRUE' since the UI dialog will prompt
                  // us if we want to overwrite
                  cmd
                  .append("packrat::bundle(file = '")
                  .append(bundleFile)
                  .append("', overwrite = TRUE)")
                  ;

                  eventBus_.fireEvent(
                     new SendToConsoleEvent(
                        cmd.toString(),
                        true,
                        false
                     )
                  );

               }

            });
   }

   @Handler
   public void onPackratStatus() 
   {
      String projDir = workbenchContext_.getActiveProjectDir().getPath();
      
      // Ask the server for the current project status
      server_.getPackratStatus(projDir, new ServerRequestCallback<JsArray<PackratStatus>>() {
         
         @Override
         public void onResponseReceived(JsArray<PackratStatus> prStatus) {
            new PackratStatusDialog(prStatus).showModal();
         }

         @Override
         public void onError(ServerError error) {
            Debug.logError(error);
         }
         
      });
      
   }
  
   @SuppressWarnings("unused")
   private final Packages.Display display_;
   private DependencyManager dependencyManager_;
   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private RemoteFileSystemContext fsContext_;
   private WorkbenchContext workbenchContext_;
   private Provider<FileDialogs> pFileDialogs_;
   private RemoteServer server_;
}
