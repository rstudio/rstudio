/*
 * PackratStatus.java
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
import java.util.List;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.studio.client.packrat.model.PackratStatus;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

// This widget represents the body of the Packrat install dialog.
public class PackratStatusDialogContents extends Composite
{
   
   private static PackratStatusUiBinder uiBinder = GWT
         .create(PackratStatusUiBinder.class);

   interface PackratStatusUiBinder extends UiBinder<Widget, PackratStatusDialogContents> {}

   public PackratStatusDialogContents(JsArray<PackratStatus> prStatusArray) {
      
      // jsArray -> List
      prStatusList_ = new ArrayList<PackratStatus>();
      JsArrayUtil.fillList(prStatusArray, prStatusList_);
      
      // create the status table
      statusTable_ = new DataGrid<PackratStatus>();
      statusTable_.setRowData(prStatusList_);
      
      // init
      initTableColumns();
      
      // create it
      initWidget(uiBinder.createAndBindUi(this));
      
   }
   
   private void addColumn(DataGrid<PackratStatus> statusTable, SortableColumnWithHeader<PackratStatus> col) {
      statusTable.addColumn(col.getColumn(), col.getHeader());
   }
   
   private void initTableColumns() {
      
      addColumn(statusTable_, new SortableColumnWithHeader<PackratStatus>(prStatusList_, "package", "Package"));
      addColumn(statusTable_, new SortableColumnWithHeader<PackratStatus>(prStatusList_, "packrat.source", "Source"));
      addColumn(statusTable_, new SortableColumnWithHeader<PackratStatus>(prStatusList_, "packrat.version", "Lockfile Version"));
      addColumn(statusTable_, new SortableColumnWithHeader<PackratStatus>(prStatusList_, "library.version", "Library Version"));
      addColumn(statusTable_, new SortableColumnWithHeader<PackratStatus>(prStatusList_, "currently.used", "Currently Used?"));

   }
   
   private final List<PackratStatus> prStatusList_;
   @UiField (provided = true) DataGrid<PackratStatus> statusTable_;

}
