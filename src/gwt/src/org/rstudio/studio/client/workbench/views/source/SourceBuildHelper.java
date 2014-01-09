/*
 * SourceBuildHelper.java
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
package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog.Result;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SourceBuildHelper
{
   @Inject
   public SourceBuildHelper(UIPrefs uiPrefs,
                            SourceShim sourceShim)
   {
      uiPrefs_ = uiPrefs;
      sourceShim_ = sourceShim;
   }

   public void withSaveFilesBeforeCommand(final Command command, 
                                          String commandSource)
   {     
      if (uiPrefs_.saveAllBeforeBuild().getValue())
      {
         sourceShim_.saveAllUnsaved(command);
      }
      else
      {
         String alwaysSaveOption = !uiPrefs_.saveAllBeforeBuild().getValue() ?
                                    "Always save files before build" : null;
         
         ArrayList<UnsavedChangesTarget> unsavedSourceDocs = 
               sourceShim_.getUnsavedChanges();

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
                           uiPrefs_.saveAllBeforeBuild().setGlobalValue(true);
                           uiPrefs_.writeUIPrefs();
                        }
                        
                        sourceShim_.handleUnsavedChangesBeforeExit(
                                                      result.getSaveTargets(),
                                                      command);
                        
                        
                     }
                   },
                   null
            ).showModal(); 
         }
         else
         {
            command.execute();
         }
      }
   }
   
   private final SourceShim sourceShim_;
   private final UIPrefs uiPrefs_;
}
