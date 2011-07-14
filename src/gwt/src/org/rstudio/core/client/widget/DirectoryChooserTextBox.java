package org.rstudio.core.client.widget;

import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Focusable;

public class DirectoryChooserTextBox extends TextBoxWithButton
{
   public DirectoryChooserTextBox(String label, Focusable focusAfter)
   {
      this(label, 
           focusAfter,
           RStudioGinjector.INSTANCE.getFileDialogs(),
           RStudioGinjector.INSTANCE.getRemoteFileSystemContext());
   }
   
   public DirectoryChooserTextBox(String label, 
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      super(label, "Browse...", null);
      
      addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            fileDialogs.chooseFolder(
                  "Choose Directory",
                  fsContext,
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
                        if (focusAfter != null)
                           focusAfter.setFocus(true);
                     }
                  });
         }
      });
      
   }    
}
