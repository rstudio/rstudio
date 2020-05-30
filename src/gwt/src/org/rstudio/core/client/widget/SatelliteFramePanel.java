/*
 * SatelliteFramePanel.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;

import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.workbench.commands.Commands;

public abstract class SatelliteFramePanel <T extends RStudioFrame>
                      extends ResizeComposite
{
   public SatelliteFramePanel(Commands commands)
   {
      commands_ = commands;
      rootPanel_ = new LayoutPanel();
      
      toolbar_ = new Toolbar("Secondary Window");
      initToolbar(toolbar_, commands_);
      rootPanel_.add(toolbar_);
      rootPanel_.setWidgetLeftRight(toolbar_, 0, Unit.PX, 0, Unit.PX);
      rootPanel_.setWidgetTopHeight(toolbar_, 0, Unit.PX, toolbar_.getHeight(), Unit.PX);
      
      initWidget(rootPanel_);
   }
   
   protected void showUrl(String url)
   {
      showUrl(url, false);
   }
   
   protected void showUrl(String url, boolean removeToolbar)
   {
      showUrl(url, false, null);
   }

   protected void showUrl(String url, boolean removeToolbar, LoadHandler onLoad)
   {
      if (appFrame_ != null)
      {
         // first set the frame to about:blank so that the 
         // javascript "unload" event is triggered (this is
         // used by bookdown to save/restore scroll position)
         appFrame_.setUrl("about:blank");
         
         rootPanel_.remove(appFrame_);
         appFrame_ = null;
      }
      
      int widgetTop = toolbar_.getHeight() + 1;
      if (removeToolbar) {
         rootPanel_.remove(toolbar_);
         widgetTop = 0;
      }
      
      appFrame_ = createAppFrame(url);
      appFrame_.setSize("100%", "100%");
      glassPanel_ = new AutoGlassPanel(appFrame_);
      rootPanel_.add(glassPanel_);
      rootPanel_.setWidgetLeftRight(glassPanel_,  0, Unit.PX, 0, Unit.PX);
      rootPanel_.setWidgetTopBottom(glassPanel_, widgetTop, Unit.PX, 0, Unit.PX);

      if (onLoad != null)
      {
         // run supplied load handler if present
         appFrame_.addLoadHandler(onLoad);
      }
   }
   
   protected T getFrame()
   {
      return appFrame_;
   }
   
   private T createAppFrame(String url)
   {
      T frame = createFrame(url);
      frame.addLoadHandler(new LoadHandler()
      {
         @Override
         public void onLoad(LoadEvent event)
         {
            appFrame_.getIFrame().setFocus();
         }
      });
      return frame;
   }
   
   protected abstract void initToolbar(Toolbar toolbar, Commands commands);
   protected abstract T createFrame(String url);

   private final Commands commands_;

   private LayoutPanel rootPanel_;
   private Toolbar toolbar_;
   private T appFrame_;
   private AutoGlassPanel glassPanel_;
}
