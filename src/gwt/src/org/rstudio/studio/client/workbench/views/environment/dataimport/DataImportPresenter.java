/*
 * DataImportPresenter.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportDialog;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportModes;

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

   public void openImportDatasetFromXML(String path)
   {
      dependencyManager_.withDataImportXML(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.XML,
                  "Import XML Data",
                  path)
      );
   }

   public void openImportDatasetFromJSON(String path)
   {
      dependencyManager_.withDataImportJSON(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.JSON,
                  "Import JSON Data",
                  path)
      );
   }

   public void openImportDatasetFromJDBC(String path)
   {
      dependencyManager_.withDataImportJDBC(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.JDBC,
                  "Import from JDBC",
                  path)
      );
   }

   public void openImportDatasetFromODBC(String path)
   {
      dependencyManager_.withDataImportODBC(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.ODBC,
                  "Import from ODBC",
                  path)
      );
   }

   public void openImportDatasetFromMongo(String path)
   {
      dependencyManager_.withDataImportMongo(
            dataImportDependecyUserAction_, 
            getImportDatasetCommandFromMode(
                  DataImportModes.Mongo,
                  "Import from Mongo DB",
                  path)
      );
   }
   
   private EventBus eventBus_;
   private DependencyManager dependencyManager_;
   
   final String dataImportDependecyUserAction_ = "Preparing data import";
}
