/*
 * PresentationPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.presentation;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ReloadableFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarLabel;

import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.presentation.model.PresentationState;

public class PresentationPane extends WorkbenchPane implements Presentation.Display
{
   @Inject
   public PresentationPane(Commands commands, Session session)
   {
      super("Presentation");
      commands_ = commands;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addLeftWidget(commands_.presentationHome().createToolbarButton());
      toolbar.addLeftSeparator();
      titleLabel_ = new ToolbarLabel();
      toolbar.addLeftWidget(titleLabel_);
      
      toolbar.addRightWidget(commands_.presentationFullscreen().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.refreshPresentation().createToolbarButton());
        
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {  
      frame_ = new ReloadableFrame(false) ;
      frame_.setSize("100%", "100%");
      frame_.setStylePrimaryName("rstudio-HelpFrame") ;
      
      // allow full-screen view of iframe
      Element el = frame_.getElement();
      el.setAttribute("webkitallowfullscreen", "");
      el.setAttribute("mozallowfullscreen", "");
      el.setAttribute("allowfullscreen", "");
      
      frame_.addLoadHandler(new LoadHandler() {

         @Override
         public void onLoad(LoadEvent event)
         {
            String title = StringUtil.notNull(
                           frame_.getWindow().getDocument().getTitle());
            titleLabel_.setText(title);
         }
      });

      return new AutoGlassPanel(frame_);
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad() ;

      if (!initialized_)
      {
         initialized_ = true;

         
      }
   }
   
   @Override
   public void load(String url, PresentationState state)
   {   
      frame_.navigate(url);
   }
   
   @Override
   public boolean hasSlides()
   {
      String href = frame_.getWindow().getLocationHref();
      return !"about:blank".equals(href);
   }
   
   @Override
   public void refresh(boolean resetAnchor)
   {
      frame_.reload(resetAnchor);
   }
   
   @Override
   public void pauseMedia()
   {
      pausePlayers(frame_.getWindow());
   }
   
   private static final native void pausePlayers(WindowEx window) /*-{
      window.pauseAllPlayers();
   }-*/;
   
   @Override
   public void fullScreen()
   {
      enterFullscreen(frame_.getWindow());
   }
   
   private static final native void enterFullscreen(WindowEx window) /*-{
      window.revealEnterFullscreen();
   }-*/;
   
   @Override
   public void home()
   {
      Reveal.fromWindow(frame_.getWindow()).home();
   }
   
   @Override
   public void next()
   {
      Reveal.fromWindow(frame_.getWindow()).next();
   }
   
   @Override
   public void prev()
   {
      Reveal.fromWindow(frame_.getWindow()).prev();
   }
   
   
   private static class Reveal extends JavaScriptObject
   {
      protected Reveal()
      {
      }
      
      public static final native Reveal fromWindow(WindowEx window) /*-{
         return window.Reveal;
      }-*/;
      
      public final native void home() /*-{
         this.slide(0);
      }-*/;
      
      public final native void next() /*-{
         this.next();
      }-*/;
      
      public final native void prev() /*-{
         this.prev();
      }-*/;
   }
   
   
   private boolean initialized_ = false;
   
   private ToolbarLabel titleLabel_;
   
   private ReloadableFrame frame_ ;
   
   private final Commands commands_;

}
