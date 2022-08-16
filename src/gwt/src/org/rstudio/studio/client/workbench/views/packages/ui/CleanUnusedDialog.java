/*
 * CleanUnusedDialog.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import org.rstudio.studio.client.workbench.views.packages.PackagesConstants;

public class CleanUnusedDialog 
   extends PackageActionConfirmationDialog<PackratPackageAction>
{
   public CleanUnusedDialog(
         GlobalDisplay globalDisplay,
         ServerDataSource<JsArray<PackratPackageAction>> cleanDS,
         OperationWithInput<ArrayList<PackratPackageAction>> checkOperation,
         Operation cancelOperation)
   {
      super(constants_.cleanUnusedPackagesCaption(), constants_.removePackagesCaption(), Roles.getDialogRole(), cleanDS, checkOperation,
            cancelOperation);
      globalDisplay_ = globalDisplay;
   }

   @Override
   protected void showNoActionsRequired()
   {
      globalDisplay_.showMessage(GlobalDisplay.MSG_INFO, constants_.packratCleanCaption(),
            constants_.packratCleanMessage());
   }

   @Override
   protected String getExplanatoryText()
   {
      return constants_.explanatoryMessage();
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
      table.addColumn(nameColumn, constants_.packageHeader());
      table.setColumnWidth(nameColumn, 65, Unit.PCT);
      
      TextColumn<PendingAction> installedColumn = new TextColumn<PendingAction>() {
         public String getValue(PendingAction action)
         {
            return action.getActionInfo().getLibraryVersion();
         } 
      };  
      table.addColumn(installedColumn, constants_.versionText());
      table.setColumnWidth(installedColumn, 35, Unit.PCT);
   }

   @Override
   protected String getActionName(PackratPackageAction action)
   {
      return action.getPackage();
   }

   private final GlobalDisplay globalDisplay_;
   private static final PackagesConstants constants_ = com.google.gwt.core.client.GWT.create(PackagesConstants.class);
}
