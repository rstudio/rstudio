/* PackratRestoreDialogContents.java
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

package org.rstudio.studio.client.packrat.ui;

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.studio.client.packrat.model.PackratPackageAction;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesDataGridCommon;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class PackratActionDialogContents extends Composite {

   private static PackratRestoreDialogContentsUiBinder uiBinder = 
         GWT.create(PackratRestoreDialogContentsUiBinder.class);

   interface PackratRestoreDialogContentsUiBinder 
         extends UiBinder<Widget, PackratActionDialogContents> {}

   public PackratActionDialogContents(
         String packratAction, 
         JsArray<PackratPackageAction> prRestoreActionsArray)
   {
      
      prRestoreActionsList_ = new ArrayList<PackratPackageAction>();
      JsArrayUtil.fillList(prRestoreActionsArray, prRestoreActionsList_);
      
      table_ = new RStudioDataGrid<PackratPackageAction>(prRestoreActionsList_.size(),
            (PackagesDataGridCommon)GWT.create(PackagesDataGridCommon.class));
      table_.setRowData(prRestoreActionsList_);
      
      initTableColumns();
      
      initWidget(uiBinder.createAndBindUi(this));

      if (packratAction == "Snapshot")
      {
         summaryLabel_.setText("The following packages have changed in " +
               "your project's private library. Select Snapshot to save " + 
               "these changes in Packrat.");
      }
      else if (packratAction == "Restore")
      {
         summaryLabel_.setText("The following packages have changed in " +
               "Packrat. Select Restore to apply these changes to your " +
               "project's private library.");
      }
   }
   
   private void addColumn(
         DataGrid<PackratPackageAction> table, 
         SortableColumnWithHeader<PackratPackageAction> col)
   {
      table.addColumn(col.getColumn(), col.getHeader());
   }
   
   private void initTableColumns()
   {      
      addColumn(table_, new SortableColumnWithHeader<PackratPackageAction>(prRestoreActionsList_, "package", "Package"));
      addColumn(table_, new SortableColumnWithHeader<PackratPackageAction>(prRestoreActionsList_, "packrat.version", "Packrat"));
      addColumn(table_, new SortableColumnWithHeader<PackratPackageAction>(prRestoreActionsList_, "library.version", "Library"));
      addColumn(table_, new SortableColumnWithHeader<PackratPackageAction>(prRestoreActionsList_, "message", "Action"));
      
      table_.setColumnWidth(0, "15%");
      table_.setColumnWidth(1, "15%");
      table_.setColumnWidth(2, "15%");
      table_.setColumnWidth(3, "55%");
      
   }
   
   private ArrayList<PackratPackageAction> prRestoreActionsList_;
   @UiField (provided = true) DataGrid<PackratPackageAction> table_;
   @UiField Label summaryLabel_;
}
