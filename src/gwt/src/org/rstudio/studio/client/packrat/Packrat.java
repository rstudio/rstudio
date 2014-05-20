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
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

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
         RemoteFileSystemContext fsContext) {
      
      eventBus_ = eventBus;
      display_ = display;
      fileDialogs_ = fileDialogs;
      workbenchContext_ = workbenchContext;
      fsContext_ = fsContext;
      binder.bind(commands, this);
   }

   @Handler
   public void onPackratHelp() {
      display_.openRStudioLink("packrat");
   }

   @Handler
   public void onPackratSnapshot() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::snapshot()", true, false)
      );
   }

   @Handler
   public void onPackratRestore() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::restore()", true, false)
      );
   }

   @Handler
   public void onPackratClean() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::clean()", true, false)
      );
   }

   @Handler
   public void onPackratBundle() {
      fileDialogs_.saveFile("Save Bundled Packrat Project...", fsContext_,
            workbenchContext_.getCurrentWorkingDir(), "zip", false,
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
                  cmd.append("packrat::bundle(file = '").append(bundleFile)
                        .append("', overwrite = TRUE)");

                  eventBus_.fireEvent(new SendToConsoleEvent(cmd.toString(),
                        true, false));

               }
            });
      
   }
   
   @Handler
   public void onPackratStatus() {
      eventBus_.fireEvent(
            new SendToConsoleEvent("packrat::status()", true, false)
      );
   }


   private final GlobalDisplay display_;
   private final EventBus eventBus_;
   private final RemoteFileSystemContext fsContext_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;

}
