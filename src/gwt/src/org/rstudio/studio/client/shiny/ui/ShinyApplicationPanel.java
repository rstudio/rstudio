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

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.shiny.ShinyApplicationPresenter;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;

public class ShinyApplicationPanel extends SatelliteFramePanel<RStudioFrame>
                                   implements ShinyApplicationPresenter.Display
{
   @Inject
   public ShinyApplicationPanel(Commands commands)
   {
      super(commands);
   }
   
   @Override 
   protected void initToolbar(Toolbar toolbar, Commands commands)
   {
      ToolbarButton refreshButton = 
            commands.reloadShinyApp().createToolbarButton();
      refreshButton.setLeftImage(commands.viewerRefresh().getImageResource());
      refreshButton.getElement().getStyle().setMarginTop(2, Unit.PX);
      toolbar.addLeftWidget(refreshButton);
      if (Desktop.isDesktop())
      {
         toolbar.addLeftSeparator();
         urlBox_ = new Label("");
         Style style = urlBox_.getElement().getStyle();
         style.setColor("#606060");
         urlBox_.addStyleName(ThemeStyles.INSTANCE.selectableText());
         toolbar.addLeftWidget(urlBox_);
      }
   }
   
   @Override
   public void showApp(ShinyApplicationParams params)
   {
      appParams_ = params;

      // We don't show the URL in server mode
      if (urlBox_ != null)
         urlBox_.setText(params.getUrl());

      showUrl(params.getUrl());
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
      // always return a full URL; if the app URL appears to be relative, 
      // make it absolute by prepending the relevant portion of this window's 
      // URL
      String url = appParams_.getUrl();
      if (!(url.startsWith("http://") || url.startsWith("https://")))
      {
         String thisUrl = Window.Location.getProtocol() + "//" +
                          Window.Location.getHost() + "/" + 
                          Window.Location.getPath();
         if (!thisUrl.endsWith("/"))
            thisUrl += "/";
         url = thisUrl + url;
      }
      return url;
   }
   
   @Override
   protected RStudioFrame createFrame(String url)
   {
      return new RStudioFrame(url);
   }
   
   private Label urlBox_;
   private ShinyApplicationParams appParams_;
}