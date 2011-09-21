/*
 * ConsoleProcess.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.console;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;

public class ConsoleProcess implements ConsoleOutputEvent.HasHandlers,
                                       ProcessExitEvent.HasHandlers
{
   @Singleton
   public static class ConsoleProcessFactory
   {
      @Inject
      public ConsoleProcessFactory(ConsoleServerOperations server,
                                   EventBus eventBus)
      {
         server_ = server;
         eventBus_ = eventBus;
      }

      /**
       * Creates a new ConsoleProcess object for the given command but
       * doesn't actually start the process on the server. Instead, the
       * process must be started manually later using the ConsoleProcess.start
       * method. This allows client code to attach event handlers before
       * execution begins.
       */
      public void allocNewProcess(
            String command,
            final ServerRequestCallback<ConsoleProcess> requestCallback)
      {
         server_.processInit(command, new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String response)
            {
               requestCallback.onResponseReceived(
                     new ConsoleProcess(server_, eventBus_, response));
            }

            @Override
            public void onError(ServerError error)
            {
               requestCallback.onError(error);
            }
         });
      }

      public void connectToProcess(
            String handle,
            ServerRequestCallback<ConsoleProcess> requestCallback)
      {
         // NOTE: We could roundtrip to the server to validate the handle, but
         //   right now there are no codepaths that would warrant that
         requestCallback.onResponseReceived(new ConsoleProcess(server_,
                                                               eventBus_,
                                                               handle));
      }

      private final ConsoleServerOperations server_;
      private final EventBus eventBus_;
   }

   private ConsoleProcess(ConsoleServerOperations server,
                          EventBus eventBus,
                          final String handle)
   {
      server_ = server;
      handle_ = handle;
      registrations_.add(eventBus.addHandler(
            ServerConsoleOutputEvent.TYPE,
            new ServerConsoleOutputEvent.Handler()
            {
               @Override
               public void onServerConsoleProcess(ServerConsoleOutputEvent event)
               {
                  if (event.getProcessHandle().equals(handle))
                     fireEvent(new ConsoleOutputEvent(event.getOutput(),
                                                      event.getError()));
               }
            }));
      registrations_.add(eventBus.addHandler(
            ServerProcessExitEvent.TYPE,
            new ServerProcessExitEvent.Handler()
            {
               @Override
               public void onServerProcessExit(ServerProcessExitEvent event)
               {
                  // no more events are coming
                  registrations_.removeHandler();

                  if (event.getProcessHandle().equals(handle))
                     fireEvent(new ProcessExitEvent(event.getExitCode()));
               }
            }
      ));
   }

   public void start(ServerRequestCallback<Void> requestCallback)
   {
      server_.processStart(handle_, requestCallback);
   }

   public void writeStandardInput(String input,
                                  ServerRequestCallback<Void> requestCallback)
   {
      server_.processWriteStdin(handle_, input, requestCallback);
   }

   public void interrupt(ServerRequestCallback<Void> requestCallback)
   {
      server_.processInterrupt(handle_, requestCallback);
   }

   @Override
   public HandlerRegistration addConsoleOutputHandler(
                                             ConsoleOutputEvent.Handler handler)
   {
      return handlers_.addHandler(ConsoleOutputEvent.TYPE, handler);
   }

   @Override
   public HandlerRegistration addProcessExitHandler(
                                               ProcessExitEvent.Handler handler)
   {
      return handlers_.addHandler(ProcessExitEvent.TYPE, handler);
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   private HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final HandlerManager handlers_ = new HandlerManager(this);
   private final ConsoleServerOperations server_;
   private final String handle_;
}
