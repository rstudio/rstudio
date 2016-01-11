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

import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.GridViewer;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class DataImport extends Composite
{
   private static DataImportUiBinder uiBinder = GWT
         .create(DataImportUiBinder.class);
   
   private final SourceServerOperations server_;
   
   interface DataImportUiBinder extends UiBinder<Widget, DataImport>
   {
   }

   public DataImport(SourceServerOperations server)
   {
      initWidget(uiBinder.createAndBindUi(this));
      
      server_ = server;
      
      Size size = DomMetrics.adjustedElementSizeToDefaultMax();
      setSize(size.width + "px", size.height + "px");
      
      int previewHeight = size.height - 60;
      gridViewer_.setHeight(previewHeight + "px");
   }
   
   @UiFactory
   FileChooserTextBox makeFileChooserTextBoxWidget() {
      FileChooserTextBox fileChooserTextBox = new FileChooserTextBox("File/URL:", null);
      fileChooserTextBox.setReadOnly(false);
      
      fileChooserTextBox.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> arg0)
         {
            refreshPreview();
         }
      });
      
      fileChooserTextBox.addTextBoxValueChangeHandler(new ChangeHandler()
      {
         @Override
         public void onChange(ChangeEvent arg0)
         {
            refreshPreview();
         }
      });
      
      return fileChooserTextBox;
   }
   
   @UiField
   FileChooserTextBox fileChooserTextBox_;
   
   @UiField
   GridViewer gridViewer_;
   
   public DataImportOptions getOptions()
   {
      DataImportOptions options = new DataImportOptions();
      options.setDataName("dataset");
      options.setImportLocation(fileChooserTextBox_.getTextBoxText());
      
      return options;
   }
   
   private void refreshPreview()
   {
      server_.previewDataImport(fileChooserTextBox_.getTextBoxText(), new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            gridViewer_.setData(response);
         }
         
         @Override
         public void onError(ServerError error)
         {
         }
      });
   }
}
