package org.rstudio.studio.client.htmlpreview.ui;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ResizeComposite;

import org.rstudio.studio.client.htmlpreview.HTMLPreviewPresenter;

public class HTMLPreviewPanel extends ResizeComposite
                              implements HTMLPreviewPresenter.Display
{
   public HTMLPreviewPanel()
   {
      label_ = new RequiresResizeLabel();
      initWidget(label_);
   }
   
   private class RequiresResizeLabel extends Label implements RequiresResize
   {
      @Override
      public void onResize()
      {

      }
   }

   private RequiresResizeLabel label_;
}
