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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.command.ImageResourceProvider;
import org.rstudio.core.client.theme.res.ThemeResources;
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
      serverLabel_ = new Label();
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
   }
   
   @Override
   protected RStudioFrame createFrame(String url)
   {
      return new RStudioFrame(url);
   }

   @Override
   public void showClaimUrl(String serverName, String url)
   {
      showUrl(url);
   }
   
   private void onCancelAuth()
   {
      
   }
   
   private Label serverLabel_;
   private ToolbarButton cancelButton_;
}