package org.rstudio.studio.client.common.satellite;


import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.inject.Provider;


public abstract class SatelliteWindow extends Composite
                                      implements RequiresResize, 
                                      ProvidesResize
{
   public SatelliteWindow(Provider<EventBus> pEventBus,
                          Provider<FontSizeManager> pFontSizeManager)
   {
      // save references
      pEventBus_ = pEventBus;
      pFontSizeManager_ = pFontSizeManager;
      
      // occupy full client area of the window
      Window.enableScrolling(false);
      Window.setMargin("0px");

      // create application panel
      mainPanel_ = new LayoutPanel();
       
      // init widget
      initWidget(mainPanel_);
   }
   
   // show the satellite window (subclasses shouldn't override this method,
   // rather they should override the abstract onInitialize method)
   public void show()
   {
      // react to font size changes
      EventBus eventBus = pEventBus_.get();
      eventBus.addHandler(ChangeFontSizeEvent.TYPE, new ChangeFontSizeHandler()
      {
         public void onChangeFontSize(ChangeFontSizeEvent event)
         {
            FontSizer.setNormalFontSize(Document.get(), event.getFontSize());
         }
      });
      FontSizeManager fontSizeManager = pFontSizeManager_.get();
      FontSizer.setNormalFontSize(Document.get(), fontSizeManager.getSize());

      // allow subclasses to initialize
      onInitialize(mainPanel_);
      
   }

   @Override
   public void onResize()
   {
      mainPanel_.onResize(); 
   }
   
   abstract protected void onInitialize(LayoutPanel mainPanel);
   
   protected LayoutPanel getMainPanel()
   {
      return mainPanel_;
   }

   private final Provider<EventBus> pEventBus_;
   private final Provider<FontSizeManager> pFontSizeManager_;
   private LayoutPanel mainPanel_;
}
