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

import org.rstudio.studio.client.packrat.model.PackratStatus;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

// This widget represents the body of the Packrat install dialog.
public class PackratStatusDialogContents extends Composite
{

   private static PackratStatusUiBinder uiBinder = GWT
         .create(PackratStatusUiBinder.class);

   interface PackratStatusUiBinder extends UiBinder<Widget, PackratStatusDialogContents> {}

   public PackratStatusDialogContents(JsArray<PackratStatus> prStatus) {
      
      statusTable_ = new FlexTable();
      
      // fill the table with the results
      JsArrayString keys = prStatus.get(0).keys();
      int nRow = prStatus.length();
      int nCol = prStatus.get(0).length();
      
      // set the column names
      for (int i = 0; i < nCol; i++) {
         statusTable_.setText(0, i, keys.get(i));
      }
      
      for (int i = 0; i < nRow; i++) {
         PackratStatus thisRow = prStatus.get(i);
         for (int j = 0; j < nCol; j++) {
            statusTable_.setText(i + 1, j, thisRow.getString(keys.get(j)));
         }
      }
      
      // create it
      initWidget(uiBinder.createAndBindUi(this));
      
   }
   
   @UiField (provided = true) FlexTable statusTable_;

}
