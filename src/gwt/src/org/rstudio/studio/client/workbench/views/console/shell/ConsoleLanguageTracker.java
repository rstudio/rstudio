/*
 * ConsoleLanguageTracker.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.shell;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConsoleLanguageTracker
      implements SessionInitEvent.Handler,
                 ConsolePromptEvent.Handler,
                 RestartStatusEvent.Handler
{
   public interface Binder extends CommandBinder<Commands, ConsoleLanguageTracker> {}
   
   public static final String LANGUAGE_R      = "R";
   public static final String LANGUAGE_PYTHON = "Python";
   
   @Inject
   public ConsoleLanguageTracker(Session session,
                                 Commands commands,
                                 Binder binder,
                                 EventBus events,
                                 DependencyManager depman,
                                 ConsoleServerOperations server)
   {
      session_ = session;
      commands_ = commands;
      events_ = events;
      depman_ = depman;
      server_ = server;

      binder.bind(commands_, this);
      
      events_.addHandler(SessionInitEvent.TYPE, this);
      events_.addHandler(ConsolePromptEvent.TYPE, this);
      events_.addHandler(RestartStatusEvent.TYPE, this);
      
      init();
   }
   
   @Handler
   public void onConsoleActivateR()
   {
      adaptToLanguage(LANGUAGE_R, null);
   }
   
   @Handler
   public void onConsoleActivatePython()
   {
      adaptToLanguage(LANGUAGE_PYTHON, null);
   }

   public void adaptToLanguage(final String language,
                               final Command command)
   {
      Command adaptCommand = () ->
      {
         server_.adaptToLanguage(
               language,
               new ServerRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                     language_ = language;
                     
                     if (command != null)
                        command.execute();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     
                     if (command != null)
                        command.execute();
                  }
               });
      };
      
      if (!StringUtil.equals(language, language_))
      {
         if (language.equals(LANGUAGE_PYTHON))
         {
            depman_.withReticulate(
                  CONSTANTS.executingPythonCodeProgressCaption(),
                  CONSTANTS.executingPythonCodeProgressCaption(),
                  adaptCommand::execute);
         }
         else
         {
            adaptCommand.execute();
         }
      }
      else
      {
         if (command != null)
            command.execute();
      }
   }
   
   public void adaptToLanguage(final String language)
   {
      adaptToLanguage(language, null);
   }

   private void init()
   {
   }

   @Override
   public void onSessionInit(SessionInitEvent event)
   {
      language_ = session_.getSessionInfo().getConsoleLanguage();
   }

   @Override
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      language_ = event.getPrompt().getLanguage();
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // on session restart, the console will return to R mode
      if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
      {
         language_ = LANGUAGE_R;
      }
   }

   private String language_;
   
   private static final ConsoleConstants CONSTANTS = GWT.create(ConsoleConstants.class);

   // Injected ----
   private final Session session_;
   private final Commands commands_;
   private final EventBus events_;
   private final DependencyManager depman_;
   private final ConsoleServerOperations server_;
}
