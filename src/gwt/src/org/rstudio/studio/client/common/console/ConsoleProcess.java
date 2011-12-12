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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

public class ConsoleProcess implements ConsoleOutputEvent.HasHandlers,
                                       ProcessExitEvent.HasHandlers
{
   @Singleton
   public static class ConsoleProcessFactory
   {
      @Inject
      public ConsoleProcessFactory(ConsoleServerOperations server,
                                   final CryptoServerOperations cryptoServer,
                                   EventBus eventBus,
                                   final Session session)
      {
         server_ = server;
         eventBus_ = eventBus;

         eventBus_.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
         {
            @Override
            public void onSessionInit(SessionInitEvent sie)
            {
               JsArray<ConsoleProcessInfo> procs =
                     session.getSessionInfo().getConsoleProcesses();

               for (int i = 0; i < procs.length(); i++)
               {
                  final ConsoleProcessInfo proc = procs.get(i);

                  connectToProcess(
                        proc,
                        new ServerRequestCallback<ConsoleProcess>()
                        {
                           @Override
                           public void onResponseReceived(
                                 final ConsoleProcess cproc)
                           {
                              if (proc.isDialog())
                              {
                                 new ConsoleProgressDialog(
                                       proc.getCaption(),
                                       cproc,
                                       proc.getBufferedOutput(),
                                       proc.getExitCode(),
                                       cryptoServer).showModal();
                              }
                              else
                              {
                                 cproc.addProcessExitHandler(new ProcessExitEvent.Handler()
                                 {
                                    @Override
                                    public void onProcessExit(ProcessExitEvent event)
                                    {
                                       cproc.reap(new VoidServerRequestCallback());
                                    }
                                 });
                              }
                           }

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                           }
                        });
               }
            }
         });
      }

      public void connectToProcess(
            ConsoleProcessInfo procInfo,
            ServerRequestCallback<ConsoleProcess> requestCallback)
      {
         requestCallback.onResponseReceived(new ConsoleProcess(server_,
                                                               eventBus_,
                                                               procInfo));
      }

      private final ConsoleServerOperations server_;
      private final EventBus eventBus_;
   }

   private ConsoleProcess(ConsoleServerOperations server,
                          EventBus eventBus,
                          final ConsoleProcessInfo procInfo)
   {
      server_ = server;
      procInfo_ = procInfo;
      registrations_.add(eventBus.addHandler(
            ServerConsoleOutputEvent.TYPE,
            new ServerConsoleOutputEvent.Handler()
            {
               @Override
               public void onServerConsoleProcess(ServerConsoleOutputEvent event)
               {
                  if (event.getProcessHandle().equals(procInfo.getHandle()))
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

                  if (event.getProcessHandle().equals(procInfo.getHandle()))
                     fireEvent(new ProcessExitEvent(event.getExitCode()));
               }
            }
      ));
   }
   
   public ConsoleProcessInfo getProcessInfo()
   {
      return procInfo_;
   }

   public void start(ServerRequestCallback<Void> requestCallback)
   {
      server_.processStart(procInfo_.getHandle(), requestCallback);
   }

   public void writeStandardInput(ShellInput input,
                                  ServerRequestCallback<Void> requestCallback)
   {
      server_.processWriteStdin(procInfo_.getHandle(), input, requestCallback);
   }

   public void interrupt(ServerRequestCallback<Void> requestCallback)
   {
      server_.processInterrupt(procInfo_.getHandle(), requestCallback);
   }
  
   public void reap(ServerRequestCallback<Void> requestCallback)
   {
      server_.processReap(procInfo_.getHandle(), requestCallback);
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
   private final ConsoleProcessInfo procInfo_;
}
