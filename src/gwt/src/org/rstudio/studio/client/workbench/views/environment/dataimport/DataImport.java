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
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressIndicatorDelay;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportServerOperations;
import org.rstudio.studio.client.workbench.views.environment.dataimport.res.DataImportResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataImport extends Composite
{
   private static DataImportUiBinder uiBinder = GWT
         .create(DataImportUiBinder.class);
   
   private DataImportServerOperations server_;
   private final int maxRows_ = 200;
   private ProgressIndicatorDelay progressIndicator_;
   private DataImportOptionsUi dataImportOptionsUi_;
   private DataImportResources dataImportResources_;
   private String codePreview_;
   
   private final int minWidth = 350;
   private final int minHeight = 400;
   
   interface DataImportUiBinder extends UiBinder<Widget, DataImport>
   {
   }

   public DataImport(DataImportOptionsUi dataImportOptionsUi,
                     ProgressIndicator progressIndicator)
   {
      dataImportResources_ = GWT.create(DataImportResources.class);
      
      progressIndicator_ = new ProgressIndicatorDelay(progressIndicator);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      initWidget(uiBinder.createAndBindUi(this));
      
      Size size = DomMetrics.adjustedElementSizeToDefaultMax();
      setSize(Math.max(minWidth, size.width) + "px", Math.max(minHeight, size.height) + "px");
      
      dataImportOptionsUi_ = dataImportOptionsUi;
      optionsHost_.add(dataImportOptionsUi_);
      
      dataImportOptionsUi_.addValueChangeHandler(new ValueChangeHandler<DataImportOptions>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<DataImportOptions> dataImportOptions)
         {
            updateCodePreview();
         }
      });
      
      updateCodePreview();
   }
   
   @Inject
   private void initialize(WorkbenchServerOperations server)
   {
      server_ = server;
   }
   
   @UiFactory
   FileOrUrlChooserTextBox makeFileOrUrlChooserTextBox() {
      FileOrUrlChooserTextBox fileOrUrlChooserTextBox = new FileOrUrlChooserTextBox("File/URL:", new Operation()
      {
         @Override
         public void execute()
         {
            refreshPreview();
         }
      }, null);
      
      return fileOrUrlChooserTextBox;
   }
   
   @UiFactory
   PushButton makeCopyButton()
   {
      return new PushButton(new Image(dataImportResources_.copyImage()), new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            copyCodeToClipboard(codePreview_);
         }
      });
   }
   
   @UiField
   FileOrUrlChooserTextBox fileOrUrlChooserTextBox_;
   
   @UiField
   GridViewerFrame gridViewer_;
   
   @UiField
   HTMLPanel optionsHost_;
   
   @UiField
   AceEditorWidget codeArea_;
   
   @UiField
   PushButton copyButton_;
   
   public DataImportOptions getOptions()
   {
      DataImportOptions options = dataImportOptionsUi_.getOptions();
      options.setImportLocation(fileOrUrlChooserTextBox_.getText());
      
      return options;
   }
   
   private void refreshPreview()
   {
      updateCodePreview();
      
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
   
   private String getCodePreview()
   {
      return codePreview_;
   }
   
   private void updateCodePreview()
   {
      codePreview_ = dataImportOptionsUi_.getCodePreview(getOptions());
      codeArea_.setCode(codePreview_);
   }
   
   private final native String toLocaleString(int number) /*-{
      return number.toLocaleString ? number.toLocaleString() : number;
   }-*/;
   
   private final native String getErrorMessage(JsObject data) /*-{
      return data.message.join(' ');
   }-*/;
   
   private final native void copyCodeToClipboard(String text) /*-{
      var copyDiv = document.createElement('div');
      copyDiv.contentEditable = true;
      document.body.appendChild(copyDiv);
      copyDiv.innerHTML = text;
      copyDiv.unselectable = "off";
      copyDiv.focus();
      document.execCommand('SelectAll');
      document.execCommand("Copy", false, null);
      document.body.removeChild(copyDiv);
   }-*/;
}
