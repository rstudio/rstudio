/*
 * CheckForUpdatesDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.packages.PackagesConstants;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.inject.Inject;

public class CheckForUpdatesDialog extends PackageActionConfirmationDialog<PackageUpdate>
{
   public CheckForUpdatesDialog(ServerDataSource<JsArray<PackageUpdate>> updatesDS,
                                OperationWithInput<ArrayList<PackageUpdate>> checkOperation,
                                Operation cancelOperation)
   {
      super(constants_.updatePackagesCaption(), constants_.installUpdatesCaption(), Roles.getDialogRole(), updatesDS, checkOperation, cancelOperation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      indicator_ = addProgressIndicator();
   }

   @Inject
   private void initialize(GlobalDisplay globalDisplay,
                           PackagesServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
   }

   @Override
   protected void showNoActionsRequired()
   {
      globalDisplay_.showMessage(
            MessageDialog.INFO, 
            constants_.checkForUpdatesCaption(),
            constants_.checkForUpdatesMessage());
   }

   @Override
   protected void addTableColumns(CellTable<PendingAction> table)
   {
      TextColumn<PendingAction> nameColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getPackageName();
         } 
      };  
      table.addColumn(nameColumn, constants_.packageHeader());
      table.setColumnWidth(nameColumn, 28, Unit.PCT);

      TextColumn<PendingAction> installedColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getInstalled();
         } 
      };  
      table.addColumn(installedColumn, constants_.installedHeader());
      table.setColumnWidth(installedColumn, 28, Unit.PCT);

      TextColumn<PendingAction> availableColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getAvailable();
         } 
      };  
      table.addColumn(availableColumn, constants_.availableHeader());
      table.setColumnWidth(availableColumn, 28, Unit.PCT);

      ImageButtonColumn<PendingAction> newsColumn =
            new ImageButtonColumn<>(
                  new ImageResource2x(ThemeResources.INSTANCE.newsButton2x()),
                  new OperationWithInput<PendingAction>() {
                     
                     public void execute(PendingAction action)
                     {
                        indicator_.onProgress(constants_.openingNewsProgressMessage());
                        server_.getPackageNewsUrl(
                              action.getActionInfo().getPackageName(),
                              action.getActionInfo().getLibPath(),
                              new ServerRequestCallback<String>()
                              {
                                 @Override
                                 public void onResponseReceived(String response)
                                 {
                                    indicator_.clearProgress();
                                    navigateToUrl(response);
                                 }

                                 @Override
                                 public void onError(ServerError error)
                                 {
                                    indicator_.clearProgress();
                                    Debug.logError(error);
                                 }
                              });

                     }
                  },
                  constants_.showPackageNewsTitle());
      table.addColumn(newsColumn, constants_.newsHeader());
      table.setColumnWidth(newsColumn, 16, Unit.PCT);
   }

   @Override
   protected String getActionName(PackageUpdate action)
   {
      return action.getPackageName();
   }

   private void navigateToUrl(String url)
   {
      if (url == null || url.length() == 0)
      {
         globalDisplay_.showErrorMessage(
               constants_.errorOpeningNewsCaption(),
               constants_.errorOpeningNewsMessage());
         return;
      }
      
      GlobalDisplay.NewWindowOptions options = new GlobalDisplay.NewWindowOptions();
      options.setName("_rstudio_package_news");
      globalDisplay_.openWindow(url, options);
   }
   
   private final ProgressIndicator indicator_;

   // Injected ----
   private GlobalDisplay globalDisplay_;
   private PackagesServerOperations server_;
   private static final PackagesConstants constants_ = com.google.gwt.core.client.GWT.create(PackagesConstants.class);
}
