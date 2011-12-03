package org.rstudio.studio.client.workbench.views.vcs.dialog;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;

import com.google.inject.Inject;

public class ViewFilePresenter
{
   public interface Display extends TextDisplay
   {
      DocDisplay getDocDisplay();
     
      void show();
   }
   
   @Inject
   public ViewFilePresenter(Display view,
                        FileTypeRegistry fileTypeRegistry)
   {
      view_ = view;
      fileTypeRegistry_ = fileTypeRegistry;
   }
   
   
   public void showFile(FileSystemItem file, String commitId, String contents)
   {
      view_.adaptToFileType(fileTypeRegistry_.getTextTypeForFile(file));
      view_.getDocDisplay().setCode(contents, false);  
      view_.show();
   }
   
   private final Display view_;
   private final FileTypeRegistry fileTypeRegistry_;
}
