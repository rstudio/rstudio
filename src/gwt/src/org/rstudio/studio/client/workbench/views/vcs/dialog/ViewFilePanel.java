package org.rstudio.studio.client.workbench.views.vcs.dialog;

import org.rstudio.core.client.widget.FullscreenPopupPanel;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ViewFilePanel extends ResizeComposite 
                       implements ViewFilePresenter.Display
{
   @Inject
   public ViewFilePanel(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay; 
      
      initWidget(docDisplay_.asWidget());
   }
   
   @Override
   public void show()
   {
      new FullscreenPopupPanel(asWidget()).center();
   }
    
   @Override
   public void onActivate()
   {
      docDisplay_.onActivate();
      
   }

   @Override
   public void adaptToFileType(TextFileType fileType)
   {
      docDisplay_.setFileType(fileType, true);
   }

   @Override
   public void setFontSize(double size)
   {
      docDisplay_.setFontSize(size);
   }

   @Override
   public Widget asWidget()
   {
      return this;
   }

   @Override
   public DocDisplay getDocDisplay()
   {
      return docDisplay_;
   }
   
   private final DocDisplay docDisplay_;
}
