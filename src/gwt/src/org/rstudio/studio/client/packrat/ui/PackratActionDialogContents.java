/* PackratRestoreDialogContents.java
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

package org.rstudio.studio.client.packrat.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.widget.RStudioDataGrid;
import org.rstudio.studio.client.packrat.PackratConstants;
import org.rstudio.studio.client.packrat.model.PackratPackageAction;
import org.rstudio.studio.client.workbench.views.packages.ui.PackagesDataGridCommon;

import java.util.ArrayList;

public class PackratActionDialogContents extends Composite {

   private static PackratRestoreDialogContentsUiBinder uiBinder = 
         GWT.create(PackratRestoreDialogContentsUiBinder.class);

   interface PackratRestoreDialogContentsUiBinder 
         extends UiBinder<Widget, PackratActionDialogContents> {}

   public PackratActionDialogContents(
         String packratAction, 
         JsArray<PackratPackageAction> prRestoreActionsArray)
   {
      
      prRestoreActionsList_ = new ArrayList<>();
      JsArrayUtil.fillList(prRestoreActionsArray, prRestoreActionsList_);
      
      table_ = new RStudioDataGrid<>(prRestoreActionsList_.size(),
            (PackagesDataGridCommon) GWT.create(PackagesDataGridCommon.class));
      table_.setRowData(prRestoreActionsList_);
      
      initTableColumns();
      
      initWidget(uiBinder.createAndBindUi(this));

      if (packratAction == "Snapshot")
      {
         summaryLabel_.setText(constants_.snapshotSummaryLabel());
      }
      else if (packratAction == "Restore")
      {
         summaryLabel_.setText(constants_.restoreSummaryLabel());
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
      addColumn(table_, new SortableColumnWithHeader<>(prRestoreActionsList_, "package", constants_.packageColumnHeaderLabel()));
      addColumn(table_, new SortableColumnWithHeader<>(prRestoreActionsList_, "packrat.version", constants_.packratColumnHeaderLabel()));
      addColumn(table_, new SortableColumnWithHeader<>(prRestoreActionsList_, "library.version", constants_.libraryColumnHeaderLabel()));
      addColumn(table_, new SortableColumnWithHeader<>(prRestoreActionsList_, "message", constants_.actionColumnHeaderLabel()));
      
      table_.setColumnWidth(0, "15%");
      table_.setColumnWidth(1, "15%");
      table_.setColumnWidth(2, "15%");
      table_.setColumnWidth(3, "55%");
      
   }
   
   private ArrayList<PackratPackageAction> prRestoreActionsList_;
   @UiField (provided = true) DataGrid<PackratPackageAction> table_;
   @UiField Label summaryLabel_;

   private static final PackratConstants constants_ = GWT.create(PackratConstants.class);
}
