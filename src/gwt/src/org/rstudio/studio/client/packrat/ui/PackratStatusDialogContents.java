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

import com.google.gwt.core.client.GWT;
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

   public PackratStatusDialogContents() {
      
      setupTable();
      
      // fill the table with the results
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   private void setupTable() {
      // use status to fill the table
      statusTable.setText(0, 0, "foo");
      statusTable.setText(0, 1, "bar");
      statusTable.insertRow(1);
   }
   
   @UiField (provided = true) FlexTable statusTable;

}
