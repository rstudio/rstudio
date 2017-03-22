/*
 * CheckForUpdatesDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerDataSource;
import org.rstudio.studio.client.workbench.views.packages.model.PackageUpdate;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;

public class CheckForUpdatesDialog extends PackageActionConfirmationDialog<PackageUpdate>
{
   public CheckForUpdatesDialog(
         GlobalDisplay globalDisplay,
         ServerDataSource<JsArray<PackageUpdate>> updatesDS,
         OperationWithInput<ArrayList<PackageUpdate>> checkOperation,
         Operation cancelOperation)
   {
      super("Update Packages", "Install Updates", updatesDS, checkOperation, cancelOperation);
      globalDisplay_ = globalDisplay;
   }

   protected void showNoActionsRequired()
   {
      globalDisplay_.showMessage(
                    MessageDialog.INFO, 
                    "Check for Updates", 
                    "All packages are up to date.");
    
   }
  
  protected void addTableColumns(CellTable<PendingAction> table)
  {
    TextColumn<PendingAction> nameColumn = new TextColumn<PendingAction>() {
        public String getValue(PendingAction action)
        {
           return action.getActionInfo().getPackageName();
        } 
     };  
     table.addColumn(nameColumn, "Package");
     table.setColumnWidth(nameColumn, 28, Unit.PCT);
     
     TextColumn<PendingAction> installedColumn = new TextColumn<PendingAction>() {
        public String getValue(PendingAction action)
        {
           return action.getActionInfo().getInstalled();
        } 
     };  
     table.addColumn(installedColumn, "Installed");
     table.setColumnWidth(installedColumn, 28, Unit.PCT);
     
     TextColumn<PendingAction> availableColumn = new TextColumn<PendingAction>() {
        public String getValue(PendingAction action)
        {
           return action.getActionInfo().getAvailable();
        } 
     };  
     table.addColumn(availableColumn, "Available");
     table.setColumnWidth(availableColumn, 28, Unit.PCT);
     
     ImageButtonColumn<PendingAction> newsColumn = 
        new ImageButtonColumn<PendingAction>(
          new ImageResource2x(ThemeResources.INSTANCE.newsButton2x()),
          new OperationWithInput<PendingAction>() {
            public void execute(PendingAction action)
            {
               GlobalDisplay.NewWindowOptions options = 
                                 new GlobalDisplay.NewWindowOptions();
               options.setName("_rstudio_package_news");
               globalDisplay_.openWindow(action.getActionInfo().getNewsUrl(),
                                         options);
            }  
          },
          "Show package NEWS") 
          {
              @Override
              protected boolean showButton(PendingAction action)
              {
                 return !StringUtil.isNullOrEmpty(
                                         action.getActionInfo().getNewsUrl());
              }
          };
     table.addColumn(newsColumn, "NEWS");
     table.setColumnWidth(newsColumn, 16, Unit.PCT);
  }
  
   private final GlobalDisplay globalDisplay_;
}
