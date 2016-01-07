/*
 * DataImport.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.core.client.widget.FileChooserTextBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class DataImport extends Composite
{
   private static DataImportUiBinder uiBinder = GWT
         .create(DataImportUiBinder.class);
   
   interface DataImportUiBinder extends UiBinder<Widget, DataImport>
   {
   }

   public DataImport()
   {
      initWidget(uiBinder.createAndBindUi(this));
      setWidth("400px");
   }
   
   @UiFactory
   FileChooserTextBox makeSomeWidget() {
      FileChooserTextBox fileChooserTextBox = new FileChooserTextBox("File/URL:", null);
      fileChooserTextBox.setReadOnly(false);
      return fileChooserTextBox;
   }
   
   @UiField
   FileChooserTextBox fileChooserTextBox_;
   
   public DataImportOptions getOptions()
   {
      DataImportOptions options = new DataImportOptions();
      options.setDataName("dataset");
      options.setImportLocation(fileChooserTextBox_.getText());
      
      return options;
   }
}
