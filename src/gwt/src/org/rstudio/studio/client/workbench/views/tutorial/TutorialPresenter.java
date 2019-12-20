/*
 * TutorialPresenter.java
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

import java.util.Map;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.InterruptStatusEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.shiny.ShinyDisconnectNotifier;
import org.rstudio.studio.client.shiny.ShinyDisconnectNotifier.ShinyDisconnectSource;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialCommandEvent;
import org.rstudio.studio.client.workbench.views.tutorial.events.TutorialNavigateEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

public class TutorialPresenter
      extends
         BasePresenter
      implements
         TutorialCommandEvent.Handler,
         TutorialNavigateEvent.Handler,
         ShinyApplicationStatusEvent.Handler,
         ShinyDisconnectSource,
         InterruptStatusEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, TutorialPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void back();
      void forward();
      void clear();
      void popout();
      void refresh();
      void home();
      
      String getUrl();
      String getName();
      
      void launchTutorial(Tutorial tutorial);
      
      void onTutorialStarted(Tutorial tutorial);
      void openTutorial(ShinyApplicationParams params);
      
      HandlerRegistration addLoadHandler(LoadHandler handler);
      HandlerRegistration addTutorialNavigateHandler(TutorialNavigateEvent.Handler handler);
   }
   
   public static class Tutorial
   {
      public Tutorial(String tutorialName,
                      String packageName)
      {
         tutorialName_ = tutorialName;
         packageName_ = packageName;
      }
      
      public String getTutorialName()
      {
         return tutorialName_;
      }
      
      public String getPackageName()
      {
         return packageName_;
      }
      
      public JsObject toJsObject()
      {
         JsObject object = JsObject.createJsObject();
         object.setString("name", tutorialName_);
         object.setString("package", packageName_);
         return object;
      }
      
      public static Tutorial fromJsObject(JsObject object)
      {
         return new Tutorial(
               object.getString("name"),
               object.getString("package"));
      }
      
      private final String tutorialName_;
      private final String packageName_;
   }
   
   @Inject
   protected TutorialPresenter(Display display,
                               EventBus events,
                               Commands commands,
                               Binder binder,
                               TutorialServerOperations server)
   {
      super(display);
      
      binder.bind(commands, this);
      
      display_ = display;
      events_ = events;
      commands_ = commands;
      server_ = server;
      
      disconnectNotifier_ = new ShinyDisconnectNotifier(this);
      
      events_.addHandler(TutorialCommandEvent.TYPE, this);
      events_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
      events_.addHandler(InterruptStatusEvent.TYPE, this);
      
      display_.addTutorialNavigateHandler(this);
   }
   
   @Override
   public void onTutorialNavigate(TutorialNavigateEvent event)
   {
      manageCommands();
   }
   
   @Override
   public void onTutorialCommand(TutorialCommandEvent event)
   {
      String type = event.getType();
      
      if (StringUtil.equals(type, TutorialCommandEvent.TYPE_STARTED))
      {
         final Tutorial tutorial = Tutorial.fromJsObject(event.getData());
       
         if (tutorialLoadHandler_ != null)
         {
            tutorialLoadHandler_.removeHandler();
            tutorialLoadHandler_ = null;
         }
         
         tutorialLoadHandler_ = display_.addLoadHandler((LoadEvent loadEvent) -> {
            
            String url = display_.getUrl();
            if (!isShinyUrl(url))
               return;
            
            // cache tutorial URL
            URL_TO_TUTORIAL_MAP.put(url, tutorial);
            tutorialLoadHandler_.removeHandler();
            
         });
         
         display_.onTutorialStarted(tutorial);
      }
      else if (StringUtil.equals(type, TutorialCommandEvent.TYPE_INDEXING_COMPLETED))
      {
         if (StringUtil.equals(display_.getUrl(), TutorialPresenter.URLS_HOME))
         {
            display_.refresh();
         }
      }
      else if (StringUtil.equals(type, TutorialCommandEvent.TYPE_LAUNCH_DEFAULT_TUTORIAL))
      {
         final Tutorial tutorial = Tutorial.fromJsObject(event.getData());
         display_.launchTutorial(tutorial);
      }
   }
   
   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      // discard non-tutorial events
      String type = event.getParams().getViewerType();
      if (!type.startsWith(VIEWER_TYPE_TUTORIAL))
         return;
      
      String state = event.getParams().getState();
      if (StringUtil.equals(state, ShinyApplicationParams.STATE_STARTED))
      {
         display_.bringToFront();
         if (Desktop.hasDesktopFrame())
         {
            String url = event.getParams().getUrl();
            Desktop.getFrame().setTutorialUrl(url);
         }
         
         params_ = event.getParams();
         display_.openTutorial(params_);
      }
      else if (StringUtil.equals(state, ShinyApplicationParams.STATE_STOPPING))
      {
         Debug.logToRConsole("Tutorial: stopping");
      }
      else if (StringUtil.equals(state, ShinyApplicationParams.STATE_STOPPED))
      {
         Debug.logToRConsole("Tutorial: stopped");
      }
   }
   
   @Override
   public void onInterruptStatus(InterruptStatusEvent event)
   {
      // TODO Auto-generated method stub
   }
   
   private void onTutorialStopped()
   {
      display_.home();
   }
   
   private Tutorial tutorialForUrl(String url)
   {
      for (Map.Entry<String, Tutorial> entry : URL_TO_TUTORIAL_MAP.entrySet())
      {
         String mappedUrl = entry.getKey();
         Tutorial tutorial = entry.getValue();
         if (url.startsWith(mappedUrl))
            return tutorial;
      }
      
      return null;
   }
   
   @Handler
   void onTutorialStop()
   {
      String url = display_.getUrl();
      Tutorial tutorial = tutorialForUrl(url);
      if (tutorial == null)
      {
         Debug.logWarning("No known tutorial for URL " + url);
         return;
      }
      
      server_.tutorialStop(
            tutorial.getTutorialName(),
            tutorial.getPackageName(),
            new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  onTutorialStopped();
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   @Handler
   void onTutorialBack()
   {
      display_.back();
   }
   
   @Handler
   void onTutorialForward()
   {
      display_.forward();
   }
   
   @Handler
   void onTutorialPopout()
   {
      display_.popout();
   }
   
   @Handler
   void onTutorialRefresh()
   {
      display_.refresh();
   }
   
   @Handler
   void onTutorialHome()
   {
      display_.home();
   }
   
   @Override
   public String getShinyUrl()
   {
      return display_.getUrl();
   }
   
   @Override
   public String getWindowName()
   {
      return display_.getName();
   }

   @Override
   public void onShinyDisconnect()
   {
      commands_.tutorialStop().setEnabled(false);
   }
   
   private void manageCommands()
   {
      String url = display_.getUrl();
      boolean isShiny = isShinyUrl(url);
      commands_.tutorialRefresh().setEnabled(isShiny);
      commands_.tutorialStop().setEnabled(isShiny);
      commands_.tutorialStop().setVisible(isShiny);
      commands_.tutorialBack().setEnabled(isShiny);
      commands_.tutorialPopout().setEnabled(isShiny);
   }
   
   private boolean isShinyUrl(String url)
   {
      String shinyPrefix = GWT.getHostPageBaseURL() + "p/";
      return url.startsWith(shinyPrefix);
   }
   
   
   private final Display display_;
   private final EventBus events_;
   private final Commands commands_;
   private final TutorialServerOperations server_;
   private final ShinyDisconnectNotifier disconnectNotifier_;
   
   private ShinyApplicationParams params_;
   
   private HandlerRegistration tutorialLoadHandler_;
   
   private static final SafeMap<String, Tutorial> URL_TO_TUTORIAL_MAP = new SafeMap<>();
   public static final String VIEWER_TYPE_TUTORIAL = "tutorial";
   
   public static final String URLS_HOME = GWT.getHostPageBaseURL() + "tutorial/home";
   
}
