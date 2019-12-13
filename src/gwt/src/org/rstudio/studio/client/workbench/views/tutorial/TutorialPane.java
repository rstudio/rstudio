/*
 * TutorialPane.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.tutorial;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIBuilder;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class TutorialPane
      extends WorkbenchPane
      implements TutorialPresenter.Display
{
   @Inject
   protected TutorialPane(Commands commands)
   {
      super("Tutorial");
      
      commands_ = commands;
      
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame("Tutorial Pane");
      frame_.setSize("100%", "100%");
      frame_.addStyleName("ace_editor_theme");
      showLandingPage();
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar("Tutorial Tab");
      
      // add navigation buttons
      toolbar_.addLeftWidget(commands_.tutorialBack().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialForward().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.tutorialZoom().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.tutorialPopout().createToolbarButton());
      
      toolbar_.addRightWidget(commands_.tutorialStop().createToolbarButton());
     
      manageCommands(false);
      return toolbar_;
   }
   
   @Override
   public void back()
   {
      frame_.getWindow().back();
      manageCommands(true);
   }
   
   @Override
   public void forward()
   {
      frame_.getWindow().forward();
      manageCommands(true);
   }
   
   @Override
   public void openTutorial(ShinyApplicationParams params)
   {
      commands_.tutorialStop().setVisible(true);
      commands_.tutorialStop().setEnabled(true);
      navigate(params.getUrl(), true);
   }
   
   @Override
   public void showLandingPage()
   {
      String url = GWT.getHostPageBaseURL() + "tutorial/home/";
      frame_.setUrl(url);
   }
   
   private static final native void showLandingPageImpl(Element frame, String html)
   /*-{
      frame.contentWindow.document.write(html);
   }-*/;
   
   private void navigate(String url, boolean useRawURL)
   {
      // save the unmodified URL for pop-out
      baseUrl_ = url;
      
      // in desktop mode we need to be careful about loading URLs which are
      // non-local; before changing the URL, set the iframe to be sandboxed
      // based on whether we're working with a local URL (note that prior to
      // RStudio 1.2 local URLs were forbidden entirely)
      if (Desktop.hasDesktopFrame())
      {
         if (URIUtils.isLocalUrl(url))
         {
            frame_.getElement().removeAttribute("sandbox");
         }
         else
         {
            frame_.getElement().setAttribute("sandbox", "allow-scripts");
         }
      }

      if (!useRawURL && !StringUtil.equals(URIConstants.ABOUT_BLANK, baseUrl_))
      {
         url = URIBuilder.fromUrl(url)
               .queryParam("tutorial_pane", "1")
               .get();
         
         frame_.setUrl(url);
      }
      else
      {
         frame_.setUrl(baseUrl_);
      }
   }
   
   private void manageCommands(boolean enabled)
   {
      commands_.tutorialBack().setEnabled(false);
      commands_.tutorialForward().setEnabled(false);
      commands_.tutorialZoom().setEnabled(false);
      commands_.tutorialPopout().setEnabled(false);
      commands_.tutorialStop().setEnabled(false);
      commands_.tutorialStop().setVisible(false);
   }
   
   
   private RStudioFrame frame_;
   private Toolbar toolbar_;
   private String baseUrl_;
   
   // Injected ----
   private final Commands commands_;

}
