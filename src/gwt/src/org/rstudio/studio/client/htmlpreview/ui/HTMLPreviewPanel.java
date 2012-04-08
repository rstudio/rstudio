package org.rstudio.studio.client.htmlpreview.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;

import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
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
   
   
   private class PreviewFrame extends Frame
   {
      public PreviewFrame()
      {
         setStylePrimaryName("rstudio-HelpFrame");
         
         // setup an iframe sandbox which disallows scripts (by not specifying
         // the allow-scripts value) but which allows the iframe to be 
         // treated as same-origin. we want the sandbox in the first place
         // to avoid javascript escaping from the iframe however if we don't
         // set allow-same-origin then the iframe does not retain its scroll
         // position on reload. there is basically a zero-sum choice as to 
         // whether we allow scripts or preserve scroll position on reload,
         // and the latter is considered very generally useful and the former
         // a fairly narrow/rare scenario.
         getElement().setAttribute("sandbox", "allow-same-origin");
      }
      
      public void navigate(final String url)
      {
         RepeatingCommand navigateCommand = new RepeatingCommand() {
            @Override
            public boolean execute()
            {
               if (getIFrame() != null && getWindow() != null)
               {
                  if (url.equals(getWindow().getLocationHref()))
                  {
                     getWindow().reload();
                  }
                  else
                  {
                     getWindow().replaceLocationHref(url);
                  }
                  return false;
               }
               else
               {
                  return true;
               }
            }
         };

         if (navigateCommand.execute())
            Scheduler.get().scheduleFixedDelay(navigateCommand, 50);      
      }
      
      private IFrameElementEx getIFrame()
      {
         return getElement().cast();
      }
      
      protected WindowEx getWindow()
      {
         return getIFrame().getContentWindow();
      }

   }
 
   private final PreviewFrame previewFrame_;

 
}
