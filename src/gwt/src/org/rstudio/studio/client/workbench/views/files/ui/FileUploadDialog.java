/*
 * FileUploadDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.files.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.core.client.jsonrpc.RpcResponse;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.HtmlFormModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;

public class FileUploadDialog extends HtmlFormModalDialog<PendingFileUpload>
{
   public FileUploadDialog(
         String actionURL,
         FileSystemItem targetDirectory,
         FileDialogs fileDialogs,
         RemoteFileSystemContext fileSystemContext,
         Operation beginOperation,
         OperationWithInput<PendingFileUpload> completedOperation,
         Operation failedOperation)
   {
      super("Upload Files",
            Roles.getDialogRole(),
            "Uploading file...",
            actionURL,
            beginOperation,
            completedOperation,
            failedOperation);
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
      targetDirectory_ = targetDirectory;
   }
   
   @Override
   protected void positionAndShowDialog(final Command onCompleted)
   {
      final PopupPanel thisPanel = this;
      setPopupPositionAndShow(new PopupPanel.PositionCallback() {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            int left = (Window.getClientWidth()/2) - (offsetWidth/2);
            int top = (Window.getClientHeight()/2) - (offsetHeight/2);
            // clip the top so the choose file dialog always appears 
            // over the file upload dialog (mostly a problem on osx)
            top = Math.min(top, 200);
            
            thisPanel.setPopupPosition(left, top);
            
            onCompleted.execute();
         }
      });
   }
   
   @Override
   protected void setFormPanelEncodingAndMethod(FormPanel formPanel)
   {
      formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
      formPanel.setMethod(FormPanel.METHOD_POST);
   }

   @Override
   protected PendingFileUpload parseResults(String results) throws Exception
   {
      RpcResponse response = RpcResponse.parse(results);
      if (response == null)
         throw new Exception("Unexpected response from server");
      
      // check for errors
      RpcError error = response.getError();
      if (error != null)
      {
         // special error message if we know the user failed to 
         // select a directory
         if (error.getCode() == RpcError.PARAM_INVALID &&
             fileUpload_.getFilename().length() == 0)
         {
            throw new Exception("You must specify a file to upload.");
         }
         else
         {
            throw new Exception(error.getEndUserMessage());  
         }
      }
      
      // return PendingFileUpload
      PendingFileUpload pendingFileUpload = response.getResult();
      return pendingFileUpload;  
   }
   
   // NOTE: discovered that GWT was always submitting the form whether
   // or not we cancelled the SubmitEvent. Perhaps their bug? Anyway, the 
   // solution was to always return true for validation and then to check
   // for the empty fileUpload filename above in parseResults (knowing that
   // the server would always return an error if no file was specified)
   @Override
   protected boolean validate()
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      panel.setStyleName(ThemeStyles.INSTANCE.fileUploadPanel());
        
      // directory panel
      HorizontalPanel directoryPanel = new HorizontalPanel();
      directoryPanel.setWidth("100%");
      directoryPanel.setStyleName(ThemeStyles.INSTANCE.fileUploadField());
      
      // target directory chooser
      directoryNameWidget_ = new DirectoryChooserTextBox("Target directory:",
         ElementIds.TextBoxButtonId.UPLOAD_TARGET);
      directoryNameWidget_.setText(targetDirectory_.getPath());
      directoryNameWidget_.addValueChangeHandler((valueChangeEvent) ->
      {
         targetDirectory_ = FileSystemItem.createDir(valueChangeEvent.getValue());
         targetDirectoryHidden_.setValue(targetDirectory_.getPath());
      });
      directoryPanel.add(directoryNameWidget_);

      panel.add(directoryPanel);
      
      // filename field
      fileUpload_ = new FileUpload();
      fileUpload_.setStyleName(ThemeStyles.INSTANCE.fileUploadField());
      fileUpload_.setName("file");
      FormLabel uploadLabel = new FormLabel("File to upload:", fileUpload_);
      uploadLabel.addStyleName(ThemeStyles.INSTANCE.fileUploadLabel());
      panel.add(uploadLabel);
      panel.add(fileUpload_);
      
      // zip file tip field
      HTML tip = new HTML("<b>TIP</b>: To upload multiple files or a " +
                          "directory, create a zip file. The zip file will " +
                          "be automatically expanded after upload.");
      tip.addStyleName(ThemeStyles.INSTANCE.fileUploadField());
      tip.addStyleName(ThemeStyles.INSTANCE.fileUploadTipLabel());
      panel.add(tip);
      
      // target directory hidden field
      targetDirectoryHidden_ = new Hidden("targetDirectory", targetDirectory_.getPath());
      panel.add(targetDirectoryHidden_);
            
      return panel;
   }

   private FileUpload fileUpload_;
   private FileSystemItem targetDirectory_;
   private Hidden targetDirectoryHidden_;
   private DirectoryChooserTextBox directoryNameWidget_;
   @SuppressWarnings("unused")
   private final FileDialogs fileDialogs_;
   @SuppressWarnings("unused")
   private RemoteFileSystemContext fileSystemContext_;
}
