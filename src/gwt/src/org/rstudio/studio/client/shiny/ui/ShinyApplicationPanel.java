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

import org.rstudio.core.client.widget.AnchorableFrame;
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
      LayoutPanel panel = new LayoutPanel();
      
      Toolbar toolbar = createToolbar(commands);
      int tbHeight = toolbar.getHeight();
      panel.add(toolbar);
      panel.setWidgetLeftRight(toolbar, 0, Unit.PX, 0, Unit.PX);
      panel.setWidgetTopHeight(toolbar, 0, Unit.PX, tbHeight, Unit.PX);
      
      previewFrame_ = new AnchorableFrame();
      previewFrame_.setSize("100%", "100%");
      panel.add(previewFrame_);
      panel.setWidgetLeftRight(previewFrame_,  0, Unit.PX, 0, Unit.PX);
      panel.setWidgetTopBottom(previewFrame_, tbHeight+1, Unit.PX, 0, Unit.PX);
      
      initWidget(panel);
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
      previewFrame_.navigate(appParams_.getUrl());
      appPathLabel_.setText(appParams_.getPath());
   }
   
   @Override
   public String getDocumentTitle()
   {
      return previewFrame_.getWindow().getDocument().getTitle();
   }

   private final AnchorableFrame previewFrame_;
   private Label appPathLabel_;
   private ShinyApplicationParams appParams_;
}