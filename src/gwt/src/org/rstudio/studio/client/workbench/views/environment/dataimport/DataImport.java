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
import org.rstudio.core.client.widget.FileOrUrlChooserTextBox;
import org.rstudio.core.client.widget.GridViewerFrame;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressIndicatorDelay;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataImport extends Composite
{
   private static DataImportUiBinder uiBinder = GWT
         .create(DataImportUiBinder.class);
   
   private SourceServerOperations server_;
   private final int maxRows_ = 200;
   private ProgressIndicatorDelay progressIndicator_;
   boolean loadingData_;
   
   interface DataImportUiBinder extends UiBinder<Widget, DataImport>
   {
   }

   public DataImport(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = new ProgressIndicatorDelay(progressIndicator);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      initWidget(uiBinder.createAndBindUi(this));
      
      Size size = DomMetrics.adjustedElementSizeToDefaultMax();
      setSize(size.width + "px", size.height + "px");
   }
   
   @Inject
   private void initialize(SourceServerOperations server)
   {
      server_ = server;
   }
   
   @UiFactory
   FileOrUrlChooserTextBox makeFileOrUrlChooserTextBox() {
      FileOrUrlChooserTextBox fileOrUrlChooserTextBox = new FileOrUrlChooserTextBox("File/URL:", null);
      
      fileOrUrlChooserTextBox.addValueChangeHandler(new ValueChangeHandler<String>()
      {
         
         @Override
         public void onValueChange(ValueChangeEvent<String> arg0)
         {
            refreshPreview();
         }
      });
      
      return fileOrUrlChooserTextBox;
   }
   
   @UiField
   FileOrUrlChooserTextBox fileOrUrlChooserTextBox_;
   
   @UiField
   GridViewerFrame gridViewer_;
   
   public DataImportOptions getOptions()
   {
      DataImportOptions options = new DataImportOptions();
      options.setDataName("dataset");
      options.setImportLocation(fileOrUrlChooserTextBox_.getText());
      
      return options;
   }
   
   private void refreshPreview()
   {
      progressIndicator_.onProgress("Retrieving preview data");
      server_.previewDataImport(fileOrUrlChooserTextBox_.getText(), maxRows_, new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            JsObject error = response.getObject("error");
            if (error != null)
            {
               progressIndicator_.onError(getErrorMessage(error));
               return;
            }
            
            gridViewer_.setOption("nullsAsNAs", "true");
            gridViewer_.setOption("status", "Previewing first " + toLocaleString(maxRows_) + " entries");
            gridViewer_.setData(response);
            
            progressIndicator_.onCompleted();
         }
         
         @Override
         public void onError(ServerError error)
         {
            progressIndicator_.onError(error.getMessage());
         }
      });
   }
   
   private final native String toLocaleString(int number) /*-{
      return number.toLocaleString ? number.toLocaleString() : number;
   }-*/;
   
   private final native String getErrorMessage(JsObject data) /*-{
      return data.error.message.join(' ');
   }-*/;
}
