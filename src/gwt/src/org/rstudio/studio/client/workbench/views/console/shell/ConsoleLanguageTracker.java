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
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConsoleLanguageTracker
      implements SessionInitEvent.Handler,
                 ConsolePromptEvent.Handler,
                 RestartStatusEvent.Handler
{
   @Inject
   public ConsoleLanguageTracker(Session session,
                                 EventBus events,
                                 ConsoleServerOperations server)
   {
      session_ = session;
      events_ = events;
      server_ = server;

      init();
   }

   public void adaptToLanguage(final String language,
                               final Command command)
   {
      if (!StringUtil.equals(language, language_))
      {
         server_.adaptToLanguage(
               language,
               new ServerRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                     language_ = language;
                     command.execute();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     command.execute();
                  }
               });
      }
      else
      {
         command.execute();
      }
   }

   private void init()
   {
      events_.addHandler(SessionInitEvent.TYPE, this);
      events_.addHandler(ConsolePromptEvent.TYPE, this);
      events_.addHandler(RestartStatusEvent.TYPE, this);
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

   public static final String LANGUAGE_R      = "R";
   public static final String LANGUAGE_PYTHON = "Python";

   private String language_;

   // Injected ----
   private final Session session_;
   private final EventBus events_;
   private final ConsoleServerOperations server_;
}
