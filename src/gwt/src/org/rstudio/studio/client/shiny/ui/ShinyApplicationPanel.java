/*
 * ShinyApplicationPanel.java
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
package org.rstudio.studio.client.shiny.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.shiny.ShinyApplicationPresenter;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.model.ShinyViewerOptions;

public class ShinyApplicationPanel extends SatelliteFramePanel<RStudioFrame>
                                   implements ShinyApplicationPresenter.Display
{
   @Inject
   public ShinyApplicationPanel(Commands commands, RSConnect rsconnect)
   {
      super(commands);
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
      refreshButton_ = 
            commands.reloadShinyApp().createToolbarButton();
      refreshButton_.setLeftImage(commands.viewerRefresh().getImageResource());
      refreshButton_.getElement().getStyle().setMarginTop(1, Unit.PX);
      toolbar.addLeftWidget(refreshButton_);
      
      publishButton_ = new RSConnectPublishButton(
            RSConnectPublishButton.HOST_SHINY_APP,
            RSConnect.CONTENT_TYPE_NONE, true, null);
      toolbar.addRightWidget(publishButton_);
   }
   
   @Override
   public void showApp(ShinyApplicationParams params, LoadHandler handler)
   {
      appParams_ = params;
      publishButton_.setShinyPreview(params);

      String url = params.getUrl();
      
      // ensure that we display a full url in server mode
      if (!url.startsWith("http"))
         url = GWT.getHostPageBaseURL() + url;
      urlBox_.setText(url);

      boolean removeTolbar = (appParams_.getViewerOptions() & ShinyViewerOptions.SHINY_VIEWER_OPTIONS_NOTOOLS) > 0;

      showUrl(url, removeTolbar, handler);
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
      return DomUtils.makeAbsoluteUrl(appParams_.getUrl());
   }
   
   @Override
   protected RStudioFrame createFrame(String url)
   {
      return new RStudioFrame("Shiny Application", url);
   }

   private Label urlBox_;
   private ShinyApplicationParams appParams_;
   private RSConnectPublishButton publishButton_;
   private ToolbarButton refreshButton_;
}
