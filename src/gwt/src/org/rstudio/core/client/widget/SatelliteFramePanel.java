/*
 * SatelliteFramePanel.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;

import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.commands.Commands;

public abstract class SatelliteFramePanel <T extends RStudioFrame>
                      extends ResizeComposite
{
   public SatelliteFramePanel(Commands commands)
   {
      commands_ = commands;
      rootPanel_ = new LayoutPanel();
      
      toolbar_ = createToolbar(commands_);
      initToolbar(toolbar_, commands_);
      rootPanel_.add(toolbar_);
      rootPanel_.setWidgetLeftRight(toolbar_, 0, Unit.PX, 0, Unit.PX);
      rootPanel_.setWidgetTopHeight(toolbar_, 0, Unit.PX, toolbar_.getHeight(), Unit.PX);
      
      initWidget(rootPanel_);
   }
   
   private Toolbar createToolbar(Commands commands)
   {
      Toolbar toolbar = new Toolbar();
      ToolbarButton popout = commands_.viewerPopout().createToolbarButton();
      popout.setText(openCommandText());
      toolbar.addRightWidget(popout);
      return toolbar;
   }
   
   protected void showUrl(String url)
   {
      if (appFrame_ != null)
      {
         rootPanel_.remove(appFrame_);
         appFrame_ = null;
      }
      
      appFrame_ = createFrame(url);
      appFrame_.setSize("100%", "100%");
      rootPanel_.add(appFrame_);
      rootPanel_.setWidgetLeftRight(appFrame_,  0, Unit.PX, 0, Unit.PX);
      rootPanel_.setWidgetTopBottom(appFrame_, toolbar_.getHeight()+1, Unit.PX, 0, Unit.PX);
   }
   
   protected T getFrame()
   {
      return appFrame_;
   }
   
   protected abstract void initToolbar(Toolbar toolbar, Commands commands);
   protected abstract T createFrame(String url);
   protected abstract String openCommandText();

   private final Commands commands_;

   private LayoutPanel rootPanel_;
   private Toolbar toolbar_;
   private T appFrame_;
}
