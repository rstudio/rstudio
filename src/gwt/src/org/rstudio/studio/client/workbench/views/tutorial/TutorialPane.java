/*
 * TutorialPane.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.tutorial;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ImmediatelyInvokedFunctionExpression;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.URIConstants;
import org.rstudio.core.client.URIUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
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
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.tutorial.TutorialPresenter.Tutorial;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialNavigateEvent;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialNavigateEvent.Handler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Visibility;
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
                          Commands commands,
                          Session session,
                          DependencyManager dependencies,
                          TutorialServerOperations server)
   {
      super("Tutorial", events);
      
      globalDisplay_ = globalDisplay;
      commands_      = commands;
      session_       = session;
      dependencies_  = dependencies;
      server_        = server;
      
      indicator_ = globalDisplay_.getProgressIndicator("Error Loading Tutorial");
      
      events.addHandler(ThemeChangedEvent.TYPE, this);
      
      initTutorialJsCallbacks();
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
      
      // TODO: managing history within an iframe is surprisingly challenging,
      // so we just leave these buttons unavailable for now and just allow
      // navigation to home
      // toolbar_.addLeftWidget(commands_.tutorialBack().createToolbarButton());
      // toolbar_.addLeftWidget(commands_.tutorialForward().createToolbarButton());
      
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
      final String url = frame_.getUrl();
      
      server_.tutorialMetadata(url, new ServerRequestCallback<JsObject>()
      {
         @Override
         public void onResponseReceived(JsObject response)
         {
            onPopout(
                  url,
                  response.getString("name"),
                  response.getString("package"));
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            onPopout(url, null, null);
         }
      });
      
   }
   
   private void onPopout(String tutorialUrl,
                         String tutorialName,
                         String tutorialPackage)
   {
      int width = Math.max(800, frame_.getElement().getClientWidth());
      int height = Math.max(800, frame_.getElement().getClientHeight());
      
      String windowName = "rstudio-tutorial-" + StringUtil.makeRandomId(16);
      
      NewWindowOptions options = new NewWindowOptions();
      options.setAppendClientId(false);
      options.setName(windowName);
      options.setCallback((WindowEx window) ->
      {
         initExternalWindowJsCallbacks(
               window,
               tutorialUrl,
               tutorialName,
               tutorialPackage,
               windowName);
      });
      
      globalDisplay_.openWebMinimalWindow(
            tutorialUrl,
            false,
            width,
            height,
            options);
      
      home();
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
      
      String url = "./tutorial/run" +
            "?package=" + tutorial.getPackageName() +
            "&name=" + tutorial.getTutorialName();
      
      navigate(url, false);
   }
   
   @Override
   public void openTutorial(String url)
   {
      commands_.tutorialStop().setVisible(true);
      commands_.tutorialStop().setEnabled(true);
      navigate(url, true);
   }
   
   @Override
   public void home()
   {
      frame_.setUrl("." + TutorialPresenter.URLS_HOME);
   }
   
   @Override
   public String getUrl()
   {
      return frame_.getUrl();
   }
   
   private void runTutorial(String tutorialName,
                            String tutorialPackage)
   {
      // check and see if this tutorial is running in a child window
      if (focusExistingTutorialWindow(tutorialName, tutorialPackage))
         return;
      
      // otherwise, launch tutorial in pane
      dependencies_.withTutorialDependencies(() ->
      {
         Tutorial tutorial = new Tutorial(tutorialName, tutorialPackage);
         launchTutorial(tutorial);
      });
   }
   
   private void stopTutorial(String url)
   {
      server_.tutorialStop(url, new VoidServerRequestCallback());
   }
   
   private void navigate(String url, boolean replaceUrl)
   {
      if (URIUtils.isLocalUrl(url))
      {
         frame_.getElement().removeAttribute("sandbox");
      }
      else
      {
         frame_.getElement().setAttribute("sandbox", "allow-scripts allow-same-origin allow-forms allow-popups");
      }
     
      frame_.setUrl(url);
   }
   
   private void onTutorialLoaded()
   {
      // because we proxy Shiny applications on RSP, we're free
      // to inject our own JS into the frame for running Shiny
      // applications. this is not true on Desktop, so we avoid
      // this here
      if (!frame_.getUrl().startsWith(GWT.getHostPageBaseURL()))
         return;
      
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
      
      body.getStyle().setVisibility(Visibility.VISIBLE);
   }
   
   private void onFrameLoaded()
   {
      String url = frame_.getUrl();
      if (TutorialUtil.isShinyUrl(url))
      {
         onTutorialLoaded();
      }
      else
      {
         onPageLoaded();
      }
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
   
   @Override
   public HandlerRegistration addTutorialNavigateHandler(Handler handler)
   {
      return frame_.addHandler(handler, TutorialNavigateEvent.TYPE);
   }
   
   private void installLearnr()
   {
      new ImmediatelyInvokedFunctionExpression()
      {
         private HandlerRegistration handler_;
         private ProgressIndicator progress_;
         
         private final String errorCaption = "Error installing learnr";
         private final String errorMessage =
               "RStudio was unable to install the learnr package.";
         
         @Override
         protected void invoke()
         {
            // double-check that we were able to successfully install learnr
            progress_ = globalDisplay_.getProgressIndicator(errorCaption);
            handler_ = events_.addHandler(ConsolePromptEvent.TYPE, new ConsolePromptEvent.Handler()
            {
               @Override
               public void onConsolePrompt(ConsolePromptEvent event)
               {
                  handler_.removeHandler();
                  
                  String version = session_.getSessionInfo().getPackageDependencies().getPackage("learnr").getVersion();
                  server_.isPackageInstalled("learnr", version, new ServerRequestCallback<Boolean>()
                  {
                     @Override
                     public void onResponseReceived(Boolean installed)
                     {
                        if (!installed)
                        {
                           progress_.onError(errorMessage);
                           return;
                        }
                        
                        progress_.onCompleted();
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        Debug.logError(error);
                        progress_.onError(errorMessage);
                     }
                  });
               }
            });
            
            // fire console event installing learnr
            progress_.onProgress("Installing learnr...");
            SendToConsoleEvent event = new SendToConsoleEvent("install.packages(\"learnr\")", true);
            events_.fireEvent(event);
         }
      };
   }
   
   private final native void initTutorialJsCallbacks()
   /*-{
   
      var self = this;
      
      $wnd.tutorialRun = $entry(function(tutorialName, tutorialPackage) {
         self.@org.rstudio.studio.client.workbench.views.tutorial.TutorialPane::runTutorial(*)(tutorialName, tutorialPackage);
      });
      
      $wnd.tutorialInstallLearnr = $entry(function() {
         self.@org.rstudio.studio.client.workbench.views.tutorial.TutorialPane::installLearnr()();
      });
      
   }-*/;
   
   private final native void initExternalWindowJsCallbacks(WindowEx window,
                                                           String tutorialUrl,
                                                           String tutorialName,
                                                           String tutorialPackage,
                                                           String windowName)
   /*-{
      
      // register this window
      $wnd.tutorialWindows = $wnd.tutorialWindows || {};
      $wnd.tutorialWindows[tutorialUrl] = {
         "package": tutorialPackage,
         "name": tutorialName,
         "window": window,
         "windowName": windowName
      };
      
      // start polling for window closure
      var self = this;
      $wnd.tutorialWindowsCallback = $wnd.tutorialWindowsCallback || setInterval(function() {
         
         // stop any tutorials whose associated window was closed
         for (var url in $wnd.tutorialWindows)
         {
            var entry = $wnd.tutorialWindows[url];
            
            var window = entry["window"];
            if (window.closed)
            {
               self.@org.rstudio.studio.client.workbench.views.tutorial.TutorialPane::stopTutorial(*)(url);
               delete $wnd.tutorialWindows[url];
            }
         }
         
         // stop polling if we have no more child windows
         var keys = Object.keys($wnd.tutorialWindows);
         if (keys.length === 0)
         {
            clearInterval($wnd.tutorialWindowsCallback);
            $wnd.tutorialWindowsCallback = null;
         }
         
      }, 500);
      
   }-*/;
   
   private final native boolean focusExistingTutorialWindow(String tutorialName,
                                                            String tutorialPackage)
   /*-{
   
      var windows = $wnd.tutorialWindows || {};
      for (var url in windows)
      {
         var entry = $wnd.tutorialWindows[url];
         
         var match =
            entry["name"] === tutorialName &&
            entry["package"] === tutorialPackage;
         
         if (match)
         {
            var windowName = entry["windowName"];
            this.@org.rstudio.studio.client.workbench.views.tutorial.TutorialPane::focusExistingTutorialWindowImpl(*)(windowName);
            return true;
         }
      }
      
      return false;
      
   }-*/;
   
   private void focusExistingTutorialWindowImpl(String name)
   {
      globalDisplay_.bringWindowToFront(name);
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
   
   // Injected ----
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final Session session_;
   private final DependencyManager dependencies_;
   private final TutorialServerOperations server_;

   private static final Resources RES = GWT.create(Resources.class);
}
