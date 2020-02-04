/*
 * SourceBuildHelper.java
 *
 * Copyright (C) 2009-14 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog.Result;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SourceBuildHelper
{
   @Inject
   public SourceBuildHelper(UserPrefs uiPrefs,
                            SourceShim sourceShim)
   {
      userPrefs_ = uiPrefs;
      sourceShim_ = sourceShim;
   }
   

   public void withSaveFilesBeforeCommand(final Command command, 
                                          String commandSource)
   {
      withSaveFilesBeforeCommand(
         command,
         new Command() { 
            public void execute() 
            {
            }
         },
         commandSource);
   }

   private void withSaveFilesBeforeCommand(final Command command, 
                                           final Command cancelCommand,
                                           String commandSource)
   {     
      if (userPrefs_.saveFilesBeforeBuild().getValue())
      {
         sourceShim_.saveUnsavedDocuments(command);
      }
      else
      {
         String alwaysSaveOption = !userPrefs_.saveFilesBeforeBuild().getValue() ?
                                    "Always save files before build" : null;
         
         ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
               sourceShim_.getUnsavedChanges(Source.TYPE_FILE_BACKED);

         if (unsavedSourceDocs.size() > 0)
         {
            new UnsavedChangesDialog(
                  commandSource,
                  alwaysSaveOption,
                  unsavedSourceDocs,
                  new OperationWithInput<UnsavedChangesDialog.Result>() {
                     @Override
                     public void execute(Result result)
                     {
                        if (result.getAlwaysSave())
                        {
                           userPrefs_.saveFilesBeforeBuild().setGlobalValue(true);
                           userPrefs_.writeUserPrefs();
                        }
                        
                        sourceShim_.handleUnsavedChangesBeforeExit(
                                                      result.getSaveTargets(),
                                                      command);
                        
                        
                     }
                   },
                   cancelCommand
            ).showModal(); 
         }
         else
         {
            command.execute();
         }
      }
   }
   
   private final SourceShim sourceShim_;
   private final UserPrefs userPrefs_;
}
