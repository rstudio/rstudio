/* PackratRestoreDialogContents.java
 *
 * Copyright (C) 2014 by RStudio, Inc.
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
import org.rstudio.studio.client.packrat.model.PackratRestoreActions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class PackratRestoreDialogContents extends Composite {

   private static PackratRestoreDialogContentsUiBinder uiBinder = GWT.create(PackratRestoreDialogContentsUiBinder.class);

   interface PackratRestoreDialogContentsUiBinder extends UiBinder<Widget, PackratRestoreDialogContents> {}

   public PackratRestoreDialogContents(JsArray<PackratRestoreActions> prRestoreActionsArray)
   {
      
      prRestoreActionsList_ = new ArrayList<PackratRestoreActions>();
      JsArrayUtil.fillList(prRestoreActionsArray, prRestoreActionsList_);
      
      table_ = new DataGrid<PackratRestoreActions>();
      table_.setRowData(prRestoreActionsList_);
      
      initTableColumns();
      
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   private void addColumn(DataGrid<PackratRestoreActions> table, SortableColumnWithHeader<PackratRestoreActions> col)
   {
      table.addColumn(col.getColumn(), col.getHeader());
   }
   
   private void initTableColumns()
   {
//      package action packrat.version library.version                    message
//      1  digest    add         0.6.4.1            <NA> Install 'digest' (0.6.4.1)
      
      addColumn(table_, new SortableColumnWithHeader<PackratRestoreActions>(prRestoreActionsList_, "package", "Package"));
//      addColumn(table_, new SortableColumnWithHeader<PackratRestoreActions>(prRestoreActionsList_, "action", "Action"));
      addColumn(table_, new SortableColumnWithHeader<PackratRestoreActions>(prRestoreActionsList_, "packrat.version", "Lockfile Version"));
      addColumn(table_, new SortableColumnWithHeader<PackratRestoreActions>(prRestoreActionsList_, "library.version", "Library Version"));
      addColumn(table_, new SortableColumnWithHeader<PackratRestoreActions>(prRestoreActionsList_, "message", "Action to Perform"));
      
      table_.setColumnWidth(0, "15%");
      table_.setColumnWidth(1, "15%");
      table_.setColumnWidth(2, "15%");
      table_.setColumnWidth(3, "55%");
      
   }
   
   private ArrayList<PackratRestoreActions> prRestoreActionsList_;
   @UiField (provided = true) DataGrid<PackratRestoreActions> table_;
   

}
