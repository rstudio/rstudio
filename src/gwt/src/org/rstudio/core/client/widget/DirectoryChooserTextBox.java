package org.rstudio.core.client.widget;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class DirectoryChooserTextBox extends TextBoxWithButton
{
   public DirectoryChooserTextBox(String label)
   {
      this(label, 
           RStudioGinjector.INSTANCE.getFileDialogs(),
           RStudioGinjector.INSTANCE.getRemoteFileSystemContext());
   }
   
   public DirectoryChooserTextBox(String label, 
                                  FileDialogs fileDialogs,
                                  FileSystemContext fsContext)
   {
      super(label, "Browse...", null);
      fileDialogs_ = fileDialogs;
      fsContext_ = fsContext;
      
      addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fileDialogs_.chooseFolder(
                  "Choose Directory",
                  fsContext_,
                  FileSystemItem.createDir(getText()),
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         ProgressIndicator indicator)
                     {
                        if (input == null)
                           return;

                        setText(input.getPath());
                        indicator.onCompleted();
                     }
                  });
         }
      });
      
   }    
  
   private final FileDialogs fileDialogs_;
   private final FileSystemContext fsContext_;
}
