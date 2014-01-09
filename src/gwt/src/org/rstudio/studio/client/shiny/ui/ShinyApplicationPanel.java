/*
 * ShinyApplicationPanel.java
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
package org.rstudio.studio.client.shiny.ui;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.shiny.ShinyApplicationPresenter;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;

public class ShinyApplicationPanel extends ResizeComposite
                                   implements ShinyApplicationPresenter.Display
{
   @Inject
   public ShinyApplicationPanel(Commands commands)
   {
      rootPanel_ = new LayoutPanel();
      
      toolbar_ = createToolbar(commands);
      rootPanel_.add(toolbar_);
      rootPanel_.setWidgetLeftRight(toolbar_, 0, Unit.PX, 0, Unit.PX);
      rootPanel_.setWidgetTopHeight(toolbar_, 0, Unit.PX, toolbar_.getHeight(), Unit.PX);
      
      initWidget(rootPanel_);
   }
   
   private Toolbar createToolbar(Commands commands)
   {
      Toolbar toolbar = new Toolbar();
      appPathLabel_ = new Label();
      toolbar.addLeftWidget(appPathLabel_);
      return toolbar;
   }
   
   @Override
   public void showApp(ShinyApplicationParams params)
   {
      appParams_ = params;
      appPathLabel_.setText(appParams_.getPath());

      if (appFrame_ != null)
      {
         rootPanel_.remove(appFrame_);
         appFrame_ = null;
      }

      appFrame_ = new RStudioFrame(appParams_.getUrl());
      appFrame_.setSize("100%", "100%");
      rootPanel_.add(appFrame_);
      rootPanel_.setWidgetLeftRight(appFrame_,  0, Unit.PX, 0, Unit.PX);
      rootPanel_.setWidgetTopBottom(appFrame_, toolbar_.getHeight()+1, Unit.PX, 0, Unit.PX);
   }
   
   @Override
   public String getDocumentTitle()
   {
      return appFrame_.getWindow().getDocument().getTitle();
   }

   @Override
   public String getUrl()
   {
      return appParams_.getUrl();
   }

   private LayoutPanel rootPanel_;
   private Label appPathLabel_;
   private Toolbar toolbar_;

   private RStudioFrame appFrame_;
   private ShinyApplicationParams appParams_;
}