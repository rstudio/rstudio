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

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ImmediatelyInvokedFunctionExpression;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.Timers;
import org.rstudio.studio.client.common.GlobalDisplay.NewWindowOptions;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialPresenter.Tutorial;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class TutorialPane
      extends WorkbenchPane
      implements LoadHandler,
                 TutorialPresenter.Display,
                 ThemeChangedEvent.Handler
{
   @Inject
   protected TutorialPane(GlobalDisplay globalDisplay,
                          EventBus events,
                          Commands commands)
   {
      super("Tutorial");
      
      globalDisplay_ = globalDisplay;
      commands_ = commands;
      
      indicator_ = globalDisplay_.getProgressIndicator("Error Loading Tutorial");
      
      events.addHandler(ThemeChangedEvent.TYPE, this);
      
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame("Tutorial Pane");
      frame_.setSize("100%", "100%");
      frame_.setStylePrimaryName("rstudio-TutorialFrame");
      frame_.addStyleName("ace_editor_theme");
      frame_.setUrl(URIConstants.ABOUT_BLANK);
      ElementIds.assignElementId(frame_.getElement(), ElementIds.TUTORIAL_FRAME);
    
      frame_.addLoadHandler(this);
      
      home();
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar("Tutorial Tab");
      toolbar_.addLeftWidget(commands_.tutorialBack().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialForward().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialHome().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialPopout().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialStop().createToolbarButton());
      toolbar_.addRightWidget(commands_.tutorialRefresh().createToolbarButton());
      return toolbar_;
   }
   
   @Override
   public void back()
   {
      frame_.getWindow().back();
   }
   
   @Override
   public void forward()
   {
      frame_.getWindow().forward();
   }
   
   @Override
   public void popout()
   {
      WindowEx window = frame_.getWindow();
      String href = window.getLocationHref();
      NewWindowOptions options = new NewWindowOptions();
      
      int width = Math.max(800, window.getInnerWidth());
      int height = Math.max(800, window.getInnerHeight());
      
      globalDisplay_.openWebMinimalWindow(
            href,
            false,
            width,
            height,
            options);
   }
   
   @Override
   public void refresh()
   {
      frame_.getWindow().reload();
   }
   
   @Override
   public void onTutorialStarted(Tutorial tutorial)
   {
      new ImmediatelyInvokedFunctionExpression()
      {
         private HandlerRegistration handler_;
         private boolean loaded_ = false;
         
         @Override
         protected void invoke()
         {
            Timers.singleShot(500, () ->
            {
               if (!loaded_)
                  indicator_.onProgress("Loading tutorial...");
            });
            
            handler_ = frame_.addLoadHandler((LoadEvent event) ->
            {
               if (!isShinyUrl(frame_.getWindowUrl()))
                  return;
               
               loaded_ = true;
               indicator_.onCompleted();
               handler_.removeHandler();
            });
         }
      };
   }
   
   @Override
   public void launchTutorial(Tutorial tutorial)
   {
      commands_.tutorialStop().setVisible(false);
      commands_.tutorialStop().setEnabled(false);
      String url = GWT.getHostPageBaseURL() +
            "tutorial/run" +
            "?package=" + tutorial.getPackageName() +
            "&name=" + tutorial.getTutorialName();
      navigate(url, true);
   }
   
   @Override
   public void openTutorial(ShinyApplicationParams params)
   {
      commands_.tutorialStop().setVisible(true);
      commands_.tutorialStop().setEnabled(true);
      navigate(params.getUrl(), true);
   }
   
   @Override
   public void home()
   {
      frame_.setUrl(TutorialPresenter.URLS_HOME);
   }
   
   @Override
   public String getUrl()
   {
      return frame_.getUrl();
   }
   
   @Override
   public String getName()
   {
      return frame_.getWindow().getName();
   }
   
   private void navigate(String url, boolean useRawURL)
   {
      if (URIUtils.isLocalUrl(url))
      {
         frame_.getElement().removeAttribute("sandbox");
      }
      else
      {
         frame_.getElement().setAttribute("sandbox", "allow-scripts");
      }
      
      frame_.setUrl(url);
   }
   
   private void onTutorialLoaded()
   {
      // find all links in the document, and ensure that they open
      // in a separate frame
      Document doc = frame_.getWindow().getDocument();
      NodeList<Element> els = doc.getElementsByTagName("a");
      for (int i = 0, n = els.getLength(); i < n; i++)
      {
         Element el = els.getItem(i);
         
         String href = el.getPropertyString("href");
         if (href == null)
            continue;
         
         boolean isNonLocalHref =
               href.contains("://") &&
               !href.startsWith(GWT.getHostPageBaseURL());
         
         if (isNonLocalHref)
            el.setPropertyString("target", "_blank");
      }
   }
   
   private void onPageLoaded()
   {
      // initialize styles for frame
      Document doc = frame_.getWindow().getDocument();
      BodyElement body = doc.getBody();
      RStudioThemes.initializeThemes(doc, body);
      body.addClassName("ace_editor_theme");
      body.addClassName(BrowseCap.operatingSystem());

      // inject styles
      final String STYLES_ID = "rstudio_tutorials_home_styles";
      if (doc.getElementById(STYLES_ID) == null)
      {
         StyleElement styleEl = doc.createStyleElement();
         styleEl.setId(STYLES_ID);
         styleEl.setType("text/css");
         styleEl.setInnerHTML(RES.styles().getText());
         doc.getHead().appendChild(styleEl);
      }
      
   }
   
   private boolean isShinyUrl(String url)
   {
      String shinyPrefix = GWT.getHostPageBaseURL() + "p/";
      return url.startsWith(shinyPrefix);
   }
   
   private void onFrameLoaded()
   {
      String url = frame_.getWindowUrl();
      if (isShinyUrl(url))
         onTutorialLoaded();
      else
         onPageLoaded();
   }
   
   @Override
   public void onLoad(LoadEvent event)
   {
      onFrameLoaded();
   }
   
   @Override
   public void onThemeChanged(ThemeChangedEvent event)
   {
      onFrameLoaded();
   }
   
   @Override
   public HandlerRegistration addLoadHandler(LoadHandler handler)
   {
      return frame_.addLoadHandler(handler);
   }
   
   // Resources ---- 
   public interface Resources extends ClientBundle
   {
      @Source("TutorialPane.css")
      CssResource styles();
   }


   
   private final ProgressIndicator indicator_;
   
   private RStudioFrame frame_;
   private Toolbar toolbar_;
   private HandlerRegistration tutorialLoadHandler_;
   
   private static int popoutCount_ = 0;
   private static final Map<String, Pair<String, String>> URL_TO_TUTORIAL_MAP = new HashMap<>();
   
   // Injected ----
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;

   private static final Resources RES = GWT.create(Resources.class);
}
