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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;



@Singleton
public class Packrat {
   public interface Binder extends CommandBinder<Commands, Packrat> {
   }

   @Inject
   public Packrat(
         Binder binder,
         Commands commands,
         EventBus eventBus,
         GlobalDisplay display,
         FileDialogs fileDialogs,
         WorkbenchContext workbenchContext,
         RemoteFileSystemContext fsContext,
         DependencyManager dependencyManager) {
      
      eventBus_ = eventBus;
      display_ = display;
      fileDialogs_ = fileDialogs;
      workbenchContext_ = workbenchContext;
      fsContext_ = fsContext;
      dependencyManager_ = dependencyManager;
      binder.bind(commands, this);
   }

   @Handler
   public void onPackratHelp() {
      display_.openRStudioLink("packrat");
   }
   
   // packrat::bundle
   private void bundlePackratProject() {
      fileDialogs_.saveFile(
         "Save Bundled Packrat Project...",
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

   // Fire a console event with packrat, while checking that packrat exists
   private void fireConsoleEventWithPackrat(final String userAction) {

      dependencyManager_.withDependencies(

         "Packrat",

         userAction,

         new Dependency[] {
            Dependency.embeddedPackage("packrat")
         },

         new Command() {

            @Override
            public void execute() {
               eventBus_.fireEvent(
                  new SendToConsoleEvent(userAction, true, false)
               );
            }
         }
      );

   }

   @Handler
   public void onPackratSnapshot() {
      // When Packrat commands are invoked, use DependencyManager to check
      // for packrat installation (see withRMarkdownPackage for an example)
      fireConsoleEventWithPackrat("packrat::snapshot()");
   }

   @Handler
   public void onPackratRestore() {
      fireConsoleEventWithPackrat("packrat::restore()");
   }

   @Handler
   public void onPackratClean() {
      fireConsoleEventWithPackrat("packrat::clean()");
   }

   @Handler
   public void onPackratBundle() {
      dependencyManager_.withDependencies(
         
         "Packrat",
         
         "packrat::bundle()",
         
         new Dependency[] {
            Dependency.embeddedPackage("packrat")
         },
         
         new Command() {
            
            @Override
            public void execute() {
               bundlePackratProject();
            }

         });

   }

   @Handler
   public void onPackratStatus() {
      fireConsoleEventWithPackrat("packrat::status()");
   }

   private DependencyManager dependencyManager_;
   private final GlobalDisplay display_;
   private final EventBus eventBus_;
   private final RemoteFileSystemContext fsContext_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;

}
