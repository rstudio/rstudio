package org.rstudio.studio.client.workbench.views.vcs.dialog;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

// TODO: lack of ace styles (line numbers or syntax highlight)

// TODO: what is the encoding of git show output?

// TODO: are filenames with spaces supported?

// TODO: don't try to show binary files

public class ViewFilePresenter
{
   public interface Display extends TextDisplay
   {
      DocDisplay getDocDisplay();
     
      void show();
   }
   
   @Inject
   public ViewFilePresenter(Display view,
                            FileTypeRegistry fileTypeRegistry,
                            FontSizeManager fontSizeManager,
                            EventBus events,
                            UIPrefs uiPrefs)
   {
      view_ = view;
      fileTypeRegistry_ = fileTypeRegistry;
      
      TextEditingTarget.registerPrefs(releaseOnDismiss_, 
                                      uiPrefs, 
                                      view.getDocDisplay());
      
      TextEditingTarget.syncFontSize(releaseOnDismiss_, 
                                     events, 
                                     view_, 
                                     fontSizeManager); 
   }
   
   
   public void showFile(FileSystemItem file, String commitId, String contents)
   {
      view_.getDocDisplay().setCode(contents, false);  
      view_.adaptToFileType(fileTypeRegistry_.getTextTypeForFile(file));
      view_.show();
   }
   
   private final Display view_;
   private final FileTypeRegistry fileTypeRegistry_;
   
   private final ArrayList<HandlerRegistration> releaseOnDismiss_ =
                                 new ArrayList<HandlerRegistration>();
}
