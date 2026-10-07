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
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleInputEvent;
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
                 ConsoleInputEvent.Handler,
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
      events_.addHandler(ConsoleInputEvent.TYPE, this);
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
               new ServerRequestCallback<VoidResponse>()
               {
                  @Override
                  public void onResponseReceived(VoidResponse response)
                  {
                     // the session projected over everything queued, so
                     // this is where the console ends up once that drains
                     setLanguage(language);
                     
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
      
      // the local copy is only trusted while the last prompt confirmed it;
      // otherwise the session decides, projecting over its pending input
      // (adaptToLanguage() in SessionModuleContext.cpp)
      if (!confirmed_ || !StringUtil.equals(language, language_))
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

   // Console input that moves the console to 'language': the same input the
   // session enqueues in adaptToLanguage() (SessionModuleContext.cpp).
   public static String consoleLanguageSwitch(String language)
   {
      return StringUtil.equals(language, LANGUAGE_PYTHON)
            ? "reticulate::repl_python()"
            : "quit";
   }

   // The language the console is in once 'input' has run, starting from
   // 'language'. Follows the REPL through the input's lines the same way
   // the session does in fixupPendingConsoleInput() (SessionConsoleInput.cpp).
   public static String languageAfterInput(String language, String input)
   {
      boolean python = StringUtil.equals(language, LANGUAGE_PYTHON);
      for (String line : StringUtil.notNull(input).split("\n"))
      {
         if (python && (line.equals("quit") || line.equals("exit")))
            python = false;
         else if (!python && (line.equals("reticulate::repl_python()") || line.equals("repl_python()")))
            python = true;
      }

      return python ? LANGUAGE_PYTHON : LANGUAGE_R;
   }

   private void init()
   {
   }

   @Override
   public void onSessionInit(SessionInitEvent event)
   {
      setLanguage(session_.getSessionInfo().getConsoleLanguage());
   }

   @Override
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      // a prompt can arrive before input the client already sent is
      // buffered: the session enqueues the switch adaptToLanguage() asks for
      // itself, and the REPL it starts (or returns to) prompts if the input
      // queued behind it has not landed yet. that prompt reports where the
      // input starts, not where it ends, so until a later prompt agrees with
      // the projection the local copy is not a safe basis for skipping the
      // RPC. an error or interrupt that cut the input short reads the same.
      language_ = event.getPrompt().getLanguage();
      confirmed_ = StringUtil.equals(language_, expected_);
   }

   @Override
   public void onConsoleInput(ConsoleInputEvent event)
   {
      // no prompt arrives while queued input drains, so follow the language
      // through the input itself: code queued behind a batch that switches
      // languages part-way has to be judged against where that batch ends.
      // the next prompt corrects any drift.
      //
      // input typed into the console gets here without going through
      // adaptToLanguage(), so it must not re-trust a copy the last prompt
      // disagreed with. the two readings are followed separately: the
      // console may be idle where that prompt left it (language_), or still
      // draining input that ends at expected_. while they agree (confirmed_)
      // this is the same as updating both; while they disagree, confirmed_
      // stays false and the next prompt or RPC response settles it.
      if ((event.getFlags() & (ConsoleInputEvent.FLAG_CANCEL | ConsoleInputEvent.FLAG_EOF)) != 0)
         return;

      String input = event.getInput();
      language_ = languageAfterInput(language_, input);
      expected_ = languageAfterInput(expected_, input);
   }
   
   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // on session restart, the console will return to R mode
      if (event.getStatus() == RestartStatusEvent.RESTART_COMPLETED)
      {
         setLanguage(LANGUAGE_R);
      }
   }

   // record where the console is (or will be, once pending input drains)
   // and that the next prompt is expected to report the same language
   private void setLanguage(String language)
   {
      language_ = language;
      expected_ = language;
      confirmed_ = true;
   }

   // the console's language, as far as the client can tell
   private String language_;

   // the language the next prompt should report, i.e. where the input the
   // client sent leaves the console
   private String expected_;

   // whether the last prompt agreed with expected_
   private boolean confirmed_ = true;
   
   private static final ConsoleConstants CONSTANTS = GWT.create(ConsoleConstants.class);

   // Injected ----
   private final Session session_;
   private final Commands commands_;
   private final EventBus events_;
   private final DependencyManager depman_;
   private final ConsoleServerOperations server_;
}
