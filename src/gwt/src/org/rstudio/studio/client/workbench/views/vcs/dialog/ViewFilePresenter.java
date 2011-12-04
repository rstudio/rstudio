package org.rstudio.studio.client.workbench.views.vcs.dialog;

import java.util.ArrayList;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

// TODO: lack of ace styles (line numbers or syntax highlight)

public class ViewFilePresenter
{
   public interface Display extends TextDisplay
   {
      DocDisplay getDocDisplay();
     
      void showFile(FileSystemItem file, String commitId, String contents);
   }
   
   @Inject
   public ViewFilePresenter(Display view,
                            FontSizeManager fontSizeManager,
                            EventBus events,
                            UIPrefs uiPrefs)
   {
      view_ = view;
      
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
      view_.showFile(file, commitId, contents);
   }
   
   private final Display view_;
   
   private final ArrayList<HandlerRegistration> releaseOnDismiss_ =
                                 new ArrayList<HandlerRegistration>();
}
