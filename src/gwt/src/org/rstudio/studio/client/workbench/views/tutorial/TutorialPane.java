/*
 * TutorialPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SearchWidget;
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
import com.google.gwt.dom.client.Style.Display;
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
      super(constants_.tutorialTitle(), events);

      globalDisplay_ = globalDisplay;
      commands_      = commands;
      session_       = session;
      dependencies_  = dependencies;
      server_        = server;

      indicator_ = globalDisplay_.getProgressIndicator(constants_.errorLoadingTutorialCaption());

      events.addHandler(ThemeChangedEvent.TYPE, this);

      initTutorialJsCallbacks();
      ensureWidget();
   }

   @Override
   protected Widget createMainWidget()
   {
      frame_ = new RStudioFrame(constants_.tutorialPaneTitle());
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
      toolbar_ = new Toolbar(constants_.tutorialTabLabel());

      // TODO: managing history within an iframe is surprisingly challenging,
      // so we just leave these buttons unavailable for now and just allow
      // navigation to home
      // toolbar_.addLeftWidget(commands_.tutorialBack().createToolbarButton());
      // toolbar_.addLeftWidget(commands_.tutorialForward().createToolbarButton());

      toolbar_.addLeftWidget(commands_.tutorialHome().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialPopout().createToolbarButton());
      toolbar_.addLeftWidget(commands_.tutorialStop().createToolbarButton());

      filterWidget_ = new SearchWidget(constants_.filterTutorialsLabel());
      filterWidget_.setPlaceholderText(constants_.filterTutorialsLabel());
      filterWidget_.addValueChangeHandler(event -> applyFilter());
      ElementIds.assignElementId(filterWidget_, ElementIds.SW_TUTORIAL);
      toolbar_.addRightWidget(filterWidget_);
      toolbar_.addRightSeparator();

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
                  indicator_.onProgress(constants_.loadingTutorialProgressMessage());
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

   private void onPageLoaded(Document doc)
   {
      initializeStyles(doc);
      applyFilter();
      doc.getBody().getStyle().setVisibility(Visibility.VISIBLE);
   }

   private void initializeStyles(Document doc)
   {
      BodyElement body = doc.getBody();
      RStudioThemes.initializeThemes(doc, body);
      body.addClassName("ace_editor_theme");
      body.addClassName(BrowseCap.operatingSystem());

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

   // The toolbar only re-checks its separators when a command's visibility
   // changes, so toggling the filter box has to ask for it explicitly.
   private void setFilterVisible(boolean visible)
   {
      filterWidget_.setVisible(visible);
      toolbar_.invalidateSeparators();
   }

   // The document shown in the frame, or null when there is none to work
   // with: a cross-origin page refuses the question, and a page still
   // loading has no body yet.
   private Document getFrameDocument()
   {
      return getFrameDocument(frame_.getIFrame());
   }

   private static final native Document getFrameDocument(IFrameElementEx frame)
   /*-{
      try {
         var doc = frame.contentWindow.document;
         return (doc && doc.body) ? doc : null;
      } catch (e) {
         return null;
      }
   }-*/;

   // true for the pane's own home page, whatever query or fragment it carries
   private static boolean isHomeUrl(String url)
   {
      return url.replaceFirst("[?#].*$", "").endsWith(TutorialPresenter.URLS_HOME);
   }

   @Override
   public boolean isHomePage()
   {
      Document doc = getFrameDocument();
      return doc != null && isHomeUrl(doc.getURL());
   }

   // Hides the tutorials on the home page that don't match the filter box.
   // Each term must appear somewhere in the tutorial's title, package name,
   // or tutorial name, so adding a term narrows the list. Whitespace and
   // ASCII punctuation separate terms, so the "package: name" line can be
   // copied in as shown and a fragment of a hyphenated name still matches it;
   // anything else, including non-Latin letters, stays part of its term.
   private void applyFilter()
   {
      Document doc = getFrameDocument();
      if (doc == null)
         return;

      Element container = DomUtils.querySelector(doc.getBody(), ".rstudio-tutorials-container");
      if (container == null)
         return;

      String[] terms = filterWidget_.getValue().toLowerCase().split(FILTER_SEPARATORS);
      NodeList<Element> entries = DomUtils.querySelectorAll(container, ".rstudio-tutorials-entry");

      int visibleCount = 0;
      for (int i = 0, n = entries.getLength(); i < n; i++)
      {
         Element entry = entries.getItem(i);
         String haystack = (
               entry.getAttribute("data-tutorial-title") + " " +
               entry.getAttribute("data-tutorial-package") + " " +
               entry.getAttribute("data-tutorial-name")).toLowerCase();

         boolean matches = true;
         for (String term : terms)
         {
            if (!haystack.contains(term))
            {
               matches = false;
               break;
            }
         }

         if (matches)
         {
            entry.getStyle().clearDisplay();
            visibleCount++;
         }
         else
         {
            entry.getStyle().setDisplay(Display.NONE);
         }
      }

      // the empty state only applies once there are tutorials to filter;
      // otherwise the page is already explaining why the list is empty
      Element empty = doc.getElementById(FILTER_EMPTY_ID);
      if (empty == null)
      {
         empty = doc.createDivElement();
         empty.setId(FILTER_EMPTY_ID);
         empty.setClassName("rstudio-tutorials-filter-empty");
         empty.setAttribute("role", "status");
         empty.setInnerText(constants_.noMatchingTutorialsMessage());
         container.appendChild(empty);
      }

      boolean showEmpty = entries.getLength() > 0 && visibleCount == 0;
      if (showEmpty)
         empty.getStyle().clearDisplay();
      else
         empty.getStyle().setDisplay(Display.NONE);
   }

   private void onFrameLoaded()
   {
      // The frame's src attribute lags behind navigation that happened inside
      // the frame, so ask the document where it is. A cross-origin page is
      // none of ours to set up, and the filter has nothing to act on there.
      Document doc = getFrameDocument();
      if (doc == null)
      {
         setFilterVisible(false);
         return;
      }

      String url = doc.getURL();
      setFilterVisible(isHomeUrl(url));

      if (TutorialUtil.isShinyUrl(url))
      {
         onTutorialLoaded();
      }
      else
      {
         onPageLoaded(doc);
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
      // only the pages we render ourselves follow the IDE theme; a running
      // tutorial is left alone
      Document doc = getFrameDocument();
      if (doc != null && !TutorialUtil.isShinyUrl(doc.getURL()))
         initializeStyles(doc);
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

   private void updateShiny()
   {
      new ImmediatelyInvokedFunctionExpression()
      {
         private HandlerRegistration handler_;
         private ProgressIndicator progress_;

         private final String errorCaption = constants_.errorInstallingShiny();
         private final String errorMessage =
               constants_.errorInstallingShinyMessage();

         @Override
         protected void invoke()
         {
            // double-check that we were able to successfully install shiny
            progress_ = globalDisplay_.getProgressIndicator(errorCaption);
            handler_ = events_.addHandler(ConsolePromptEvent.TYPE, new ConsolePromptEvent.Handler()
            {
               @Override
               public void onConsolePrompt(ConsolePromptEvent event)
               {
                  handler_.removeHandler();

                  server_.isPackageInstalled("shiny", "1.6.0", new ServerRequestCallback<Boolean>()
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
            progress_.onProgress(constants_.installingShinyCaption());
            SendToConsoleEvent event = new SendToConsoleEvent("install.packages(\"shiny\")", true);
            events_.fireEvent(event);
         }
      };
   }

   private void installLearnr()
   {
      new ImmediatelyInvokedFunctionExpression()
      {
         private HandlerRegistration handler_;
         private ProgressIndicator progress_;

         private final String errorCaption = constants_.errorInstallingLearnr();
         private final String errorMessage =
               constants_.errorInstallingLearnrMessage();

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
            progress_.onProgress(constants_.installingLearnrCaption());
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

      $wnd.tutorialUpdateShiny = $entry(function() {
         self.@org.rstudio.studio.client.workbench.views.tutorial.TutorialPane::updateShiny()();
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
   private SearchWidget filterWidget_;

   // Injected ----
   private final GlobalDisplay globalDisplay_;
   private final Commands commands_;
   private final Session session_;
   private final DependencyManager dependencies_;
   private final TutorialServerOperations server_;

   private static final String FILTER_EMPTY_ID = "rstudio_tutorials_filter_empty";

   // whitespace and the ASCII punctuation ranges !-/ :-@ [-` {-~
   private static final String FILTER_SEPARATORS = "[\\s!-/:-@\\[-`{-~]+";

   private static final Resources RES = GWT.create(Resources.class);
   private static final TutorialConstants constants_ = com.google.gwt.core.client.GWT.create(TutorialConstants.class);
}