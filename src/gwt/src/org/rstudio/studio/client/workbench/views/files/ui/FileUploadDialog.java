/*
 * FileUploadDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.filedialog.FileDialogResources;
import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.core.client.jsonrpc.RpcResponse;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.HtmlFormModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.views.files.model.PendingFileUpload;

public class FileUploadDialog extends HtmlFormModalDialog<PendingFileUpload>
{
   public FileUploadDialog(
         String actionURL,
         FileSystemItem targetDirectory,
         FileDialogs fileDialogs,
         RemoteFileSystemContext fileSystemContext,
         OperationWithInput<PendingFileUpload> completedOperation)
   {
      super("Upload Files", 
            "Uploading file...", 
            actionURL, 
            completedOperation);
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
      
      // directory name (informational field)
      panel.add(new Label("Target directory:"));
      directoryNameWidget_ = new DirectoryNameWidget();
      directoryNameWidget_.setDirectory(targetDirectory_);
      directoryPanel.add(directoryNameWidget_);
      
      // browse directory button
      // JJA: removed browse button (was causing confusion for users who
      // thought it was what they should press to browse local files)
      /*
      Button browseButton = new Button("Browse...", 
                                       new BrowseDirectoryClickHandler());
      browseButton.getElement().getStyle().setMarginRight(5, Unit.PX);
      directoryPanel.add(browseButton);
      directoryPanel.setCellHorizontalAlignment(
                                          browseButton, 
                                          HasHorizontalAlignment.ALIGN_RIGHT);
      */
      panel.add(directoryPanel);
      
      // filename field
      panel.add(new Label("File to upload:"));
      fileUpload_ = new FileUpload();
      fileUpload_.setStyleName(ThemeStyles.INSTANCE.fileUploadField());
      fileUpload_.setName("file");
      panel.add(fileUpload_);
      
      // zip file tip field
      HTML tip = new HTML("<b>TIP</b>: To upload multiple files or a " +
                          "directory, create a zip file. The zip file will " +
                          "be automatically expanded after upload.");
      tip.addStyleName(ThemeStyles.INSTANCE.fileUploadField());
      tip.addStyleName(ThemeStyles.INSTANCE.fileUploadTipLabel());
      panel.add(tip);
      
      // target directory hidden field
      targetDirectoryHidden_ = new Hidden("targetDirectory", 
                                           targetDirectory_.getPath());
      panel.add(targetDirectoryHidden_);
            
      return panel;
   }
   
   // JJA: used by currently commented out browse directory button
   @SuppressWarnings("unused")
   private class BrowseDirectoryClickHandler implements ClickHandler
   {   
      public void onClick(ClickEvent event)
      {
         fileDialogs_.chooseFolder(
             "Choose Target Directory",
             fileSystemContext_,
             targetDirectory_,
             new ProgressOperationWithInput<FileSystemItem>() {

               public void execute(FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                     return;
                  
                  indicator.onCompleted();
                  targetDirectory_ = input;
                  targetDirectoryHidden_.setValue(input.getPath());
                  directoryNameWidget_.setDirectory(input);
               }          
             });
      }   
   }
   
   private class DirectoryNameWidget extends HorizontalPanel
   {
      public DirectoryNameWidget()
      {
         setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
         
         image_ = new Image();
         image_.setStyleName(
                     FileDialogResources.INSTANCE.styles().columnIcon());
         this.add(image_);
         name_ = new HTML();
         this.add(name_);
      }
      
      public void setDirectory(FileSystemItem directoryItem)
      {
         if (directoryItem.equalTo(FileSystemItem.home()))
         {
            image_.setResource(new ImageResource2x(FileDialogResources.INSTANCE.homeImage2x()));
            name_.setHTML("Home");
         }
         else
         {
            image_.setResource(new ImageResource2x(FileIconResources.INSTANCE.iconFolder2x()));
            name_.setHTML("&nbsp;" + directoryItem.getPath());
         }
      }
      
      Image image_ ;
      HTML name_ ;
   }
   
   private FileUpload fileUpload_;
   private FileSystemItem targetDirectory_;
   private Hidden targetDirectoryHidden_;
   private DirectoryNameWidget directoryNameWidget_;
   private final FileDialogs fileDialogs_;
   private RemoteFileSystemContext fileSystemContext_;
}
