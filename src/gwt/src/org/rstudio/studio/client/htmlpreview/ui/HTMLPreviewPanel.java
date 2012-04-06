package org.rstudio.studio.client.htmlpreview.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;

import org.rstudio.core.client.widget.DynamicIFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.htmlpreview.HTMLPreviewPresenter;

public class HTMLPreviewPanel extends ResizeComposite
                              implements HTMLPreviewPresenter.Display
{
   public HTMLPreviewPanel()
   {
      LayoutPanel panel = new LayoutPanel();
      
      Toolbar toolbar = new Toolbar();
      int tbHeight = toolbar.getHeight();
      panel.add(toolbar);
      panel.setWidgetLeftRight(toolbar, 0, Unit.PX, 0, Unit.PX);
      panel.setWidgetTopHeight(toolbar, 0, Unit.PX, tbHeight, Unit.PX);
      
      previewFrame_ = new PreviewFrame();
      previewFrame_.setSize("100%", "100%");
      panel.add(previewFrame_);
      panel.setWidgetLeftRight(previewFrame_,  0, Unit.PX, 0, Unit.PX);
      panel.setWidgetTopBottom(previewFrame_, tbHeight+1, Unit.PX, 0, Unit.PX);
      
      initWidget(panel);
   }
   
   @Override
   public void showPreview(String url)
   {
      previewFrame_.navigate(url);
   }
   
   
   private class PreviewFrame extends DynamicIFrame
   {
      public PreviewFrame()
      {
         setStylePrimaryName("rstudio-HelpFrame");
      }
      
      public void navigate(final String url)
      {
         navigated_ = true;
         RepeatingCommand navigateCommand = new RepeatingCommand() {
            @Override
            public boolean execute()
            {
               if (getIFrame() != null && getWindow() != null)
               {
                  getWindow().replaceLocationHref(url);
                  setUrl(url);
                  return false;
               }
               else
               {
                  return true;
               }
            }
         };

         if (navigateCommand.execute())
            Scheduler.get().scheduleFixedDelay(navigateCommand, 100);      
      }

      @Override
      protected void onFrameLoaded()
      {
         if (!navigated_)
         {
            BodyElement body = getDocument().getBody();
            body.getStyle().setMargin(0, Unit.PX);
            body.getStyle().setBackgroundColor("white");
         }
      }

      private boolean navigated_ = false;
      
   }
 
   private final PreviewFrame previewFrame_;

 
}
