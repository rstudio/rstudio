/*
 * RSConnectAuthPanel.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

package org.rstudio.studio.client.rsconnect.ui;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.commands.Commands;

public class RSConnectAuthPanel extends SatelliteFramePanel<RStudioFrame>
                             implements RSConnectAuthPresenter.Display
{
   @Inject
   public RSConnectAuthPanel(Commands commands)
   {
      super(commands);
   }
   
   @Override 
   protected void initToolbar(Toolbar toolbar, Commands commands)
   {
      toolbar.addLeftWidget(new Label("Connecting RStudio Connect account on "));
      serverLabel_ = new Label();
      serverLabel_.getElement().getStyle().setFontWeight(FontWeight.BOLD);
      toolbar.addLeftWidget(serverLabel_);
      
      cancelButton_ = new ToolbarButton("Cancel", (ImageResource)null,
            new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            onCancelAuth();
         }
      });
      toolbar.addRightWidget(cancelButton_);
   }
   
   @Override
   protected RStudioFrame createFrame(String url)
   {
      return new RStudioFrame(url);
   }

   @Override
   public void showClaimUrl(String serverName, String url)
   {
      serverLabel_.setText(serverName);
      showUrl(url);
   }
   
   private final native void onCancelAuth() /*-{
      window.close();
   }-*/;
   
   private Label serverLabel_;
   private ToolbarButton cancelButton_;
}