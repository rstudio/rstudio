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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.events.RSConnectActionEvent;
import org.rstudio.studio.client.shiny.ShinyApplicationPresenter;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;

public class ShinyApplicationPanel extends SatelliteFramePanel<RStudioFrame>
                                   implements ShinyApplicationPresenter.Display
{
   @Inject
   public ShinyApplicationPanel(Commands commands, EventBus events,
                                RSConnect rsconnect)
   {
      super(commands);
      events_ = events;
      rsconnect.ensureSessionInit();
   }
   
   @Override 
   protected void initToolbar(Toolbar toolbar, Commands commands)
   {
      urlBox_ = new Label("");
      Style style = urlBox_.getElement().getStyle();
      style.setColor("#606060");
      urlBox_.addStyleName(ThemeStyles.INSTANCE.selectableText());
      urlBox_.getElement().getStyle().setMarginRight(7, Unit.PX);
      toolbar.addLeftWidget(urlBox_);
      toolbar.addLeftSeparator();  

      ToolbarButton popoutButton = 
            commands.viewerPopout().createToolbarButton();
      popoutButton.setText("Open in Browser");
      toolbar.addLeftWidget(popoutButton);

      toolbar.addLeftSeparator();
      ToolbarButton refreshButton = 
            commands.reloadShinyApp().createToolbarButton();
      refreshButton.setLeftImage(commands.viewerRefresh().getImageResource());
      refreshButton.getElement().getStyle().setMarginTop(1, Unit.PX);
      toolbar.addLeftWidget(refreshButton);
      
      deployButton_ = new ToolbarButton("Publish", 
            commands.rsconnectDeploy().getImageResource(), 
            new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent evt)
         {
            if (appParams_ != null)
            {
               // we initiate deployment from a specific file; choose server.R
               // (it's okay if it doesn't exist since we're just going to 
               // deploy its parent)
               String deployPath = appParams_.getPath();
               if (!deployPath.endsWith("/"))
                  deployPath += "/";
               deployPath += "server.R";
               events_.fireEvent(new RSConnectActionEvent(
                     RSConnectActionEvent.ACTION_TYPE_DEPLOY,
                     deployPath));
            }
         }
      });
      toolbar.addRightWidget(deployButton_);
   }
   
   @Override
   public void showApp(ShinyApplicationParams params, boolean showDeploy)
   {
      appParams_ = params;

      deployButton_.setVisible(showDeploy);
         
      String url = params.getUrl();
      
      // ensure that we display a full url in server mode
      if (!url.startsWith("http"))
         url = GWT.getHostPageBaseURL() + url;
      urlBox_.setText(url);

      showUrl(url);
   }
   
   @Override
   public void reloadApp()
   {
      // appFrame_.getWindow().reload() would be better, but won't work here
      // due to same-origin policy restrictions
      getFrame().setUrl(getFrame().getUrl());
   }

   @Override
   public String getDocumentTitle()
   {
      return getFrame().getWindow().getDocument().getTitle();
   }

   @Override
   public String getUrl()
   {
      return appParams_.getUrl();
   }

   @Override
   public String getAbsoluteUrl()
   {
      return StringUtil.makeAbsoluteUrl(appParams_.getUrl());
   }
   
   @Override
   protected RStudioFrame createFrame(String url)
   {
      return new RStudioFrame(url);
   }

   private Label urlBox_;
   private ShinyApplicationParams appParams_;
   private ToolbarButton deployButton_;
   
   private final EventBus events_; 
}