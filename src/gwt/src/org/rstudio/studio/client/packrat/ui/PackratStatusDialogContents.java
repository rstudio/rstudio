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

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
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
      List<PackratStatus> prStatusList = new ArrayList<PackratStatus>();
      JsArrayUtil.fillList(prStatusArray, prStatusList);
      
      // create the status table
      statusTable_ = new DataGrid<PackratStatus>();
      statusTable_.setRowData(prStatusList);
      
      initTableColumns();
      
      // create it
      initWidget(uiBinder.createAndBindUi(this));
      
   }
   
   private void initTableColumns() {
      
      // package name column
      Column<PackratStatus, String> packageNameColumn = new Column<PackratStatus, String>(new TextCell()) {
         @Override
         public String getValue(PackratStatus obj) {
            return obj.getPackageName();
         }
      };
      
      statusTable_.addColumn(packageNameColumn, "Package");
      
      Column<PackratStatus, String> packratVersionColumn = new Column<PackratStatus, String>(new TextCell()) {
         @Override
         public String getValue(PackratStatus obj) {
            return obj.getPackageVersion();
         }
      };
      
      statusTable_.addColumn(packratVersionColumn, "Packrat Version");
      
      Column<PackratStatus, String> packageSourceColumn = new Column<PackratStatus, String>(new TextCell()) {
         @Override
         public String getValue(PackratStatus obj) {
            return obj.getPackageSource();
         }
      };
      
      statusTable_.addColumn(packageSourceColumn, "Source");
      
      // package name column
      Column<PackratStatus, String> packratLibraryVersionColumn = new Column<PackratStatus, String>(new TextCell()) {
         @Override
         public String getValue(PackratStatus obj) {
            return obj.getLibraryVersion();
         }
      };
      
      statusTable_.addColumn(packratLibraryVersionColumn, "Library Version");
      
      // package name column
      Column<PackratStatus, String> currentlyUsedColumn = new Column<PackratStatus, String>(new TextCell()) {
         @Override
         public String getValue(PackratStatus obj) {
            return obj.getCurrentlyUsed();
         }
      };
      
      statusTable_.addColumn(currentlyUsedColumn, "Currently Used");
      
   }
   
   @UiField (provided = true) DataGrid<PackratStatus> statusTable_;

}
