/*
 * DataImportPresenter.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

@Singleton
public class DataImportPresenter
{
   @Inject
   public DataImportPresenter(EventBus eventBus,
                              DependencyManager dependencyManager)
   {
      eventBus_ = eventBus;
      dependencyManager_ = dependencyManager;
   }
   
   public Command getImportDatasetCommandFromMode(
      final DataImportModes dataImportMode,
      final String dialogTitle,
      final String path)
   {
       return 
          new Command() {
             @Override
             public void execute()
             {
                DataImportDialog dataImportDialog = new DataImportDialog(
                      dataImportMode,
                      dialogTitle,
                      path,
                      new OperationWithInput<String>()
                {
                   @Override
                   public void execute(final String importCode)
                   {
                      eventBus_.fireEvent(new SendToConsoleEvent(importCode, true, true)); 
                   }
                });
                
                dataImportDialog.showModal();
             }
          };
   }
   
   public void openImportDatasetFromCSV(String path)
   {
      dependencyManager_.withDataImportCSV(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.Text,
                  "Import Text Data",
                  path)
      );
   }
   
   public void openImportDatasetFromSAV(String path)
   {
      dependencyManager_.withDataImportSAV(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.SAV,
                  "Import Statistical Data",
                  path)
      );
   }

   public void openImportDatasetFromSAS(String path)
   {
      dependencyManager_.withDataImportSAV(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.SAS,
                  "Import Statistical Data",
                  path)
      );
   }

   public void openImportDatasetFromStata(String path)
   {
      dependencyManager_.withDataImportSAV(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.Stata,
                  "Import Statistical Data",
                  path)
      );
   }

   public void openImportDatasetFromXLS(String path)
   {
      dependencyManager_.withDataImportXLS(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.XLS,
                  "Import Excel Data",
                  path)
      );
   }

   private EventBus eventBus_;
   private DependencyManager dependencyManager_;
   
   final String dataImportDependecyUserAction_ = "Preparing data import";
}
