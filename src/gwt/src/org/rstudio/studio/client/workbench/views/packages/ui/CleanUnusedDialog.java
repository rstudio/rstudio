/*
 * CleanUnusedDialog.java
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
package org.rstudio.studio.client.workbench.views.packages.ui;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.packrat.model.PackratPackageAction;
import org.rstudio.studio.client.server.ServerDataSource;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;

public class CleanUnusedDialog 
   extends PackageActionConfirmationDialog<PackratPackageAction>
{
   public CleanUnusedDialog(
         GlobalDisplay globalDisplay,
         ServerDataSource<JsArray<PackratPackageAction>> cleanDS,
         OperationWithInput<ArrayList<PackratPackageAction>> checkOperation,
         Operation cancelOperation)
   {
      super("Clean Unused Packages", "Remove Packages", Roles.getDialogRole(), cleanDS, checkOperation,
            cancelOperation);
      globalDisplay_ = globalDisplay;
   }

   @Override
   protected void showNoActionsRequired()
   {
      globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, "Packrat Clean", 
            "No unused packages were found in the library.");
   }

   @Override
   protected String getExplanatoryText()
   {
      return "These packages are present in your library, but do not " +
        "appear to be used by code in your project. Select any you'd like to " +
        "clean up.";
   }
   
   @Override
   protected void addTableColumns(CellTable<PendingAction> table)
   {
     TextColumn<PendingAction> nameColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getPackage();
         } 
      };  
      table.addColumn(nameColumn, "Package");
      table.setColumnWidth(nameColumn, 65, Unit.PCT);
      
      TextColumn<PendingAction> installedColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getLibraryVersion();
         } 
      };  
      table.addColumn(installedColumn, "Version");
      table.setColumnWidth(installedColumn, 35, Unit.PCT);
   }

   @Override
   protected String getActionName(PackratPackageAction action)
   {
      return action.getPackage();
   }

   private final GlobalDisplay globalDisplay_;
}
