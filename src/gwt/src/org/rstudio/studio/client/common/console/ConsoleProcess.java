/*
 * ConsoleProcess.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.console;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.console.model.ConsoleServerOperations;
import org.rstudio.studio.client.workbench.views.console.model.ProcessBufferChunk;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
import com.google.inject.Singleton;

public class ConsoleProcess implements ConsoleOutputEvent.HasHandlers,
                                       ConsolePromptEvent.HasHandlers,
                                       ProcessExitEvent.HasHandlers
{
   public enum TerminalType {
      DUMB, // simple canonical (line-by-line) terminal
      XTERM; // xterm-compatible non-canonical terminal
   }
   
   @Singleton
   public static class ConsoleProcessFactory
   {
      @Inject
      public ConsoleProcessFactory(ConsoleServerOperations server,
                                   final CryptoServerOperations cryptoServer,
                                   EventBus eventBus,
                                   final Session session,
                                   final Satellite satellite,
                                   final SatelliteManager satelliteManager)
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
                  if (proc.isTerminal())
                  {
                     // non-modal processes represent terminals and are handled
                     // by the terminal UI
                     continue;
                  }
                  
                  connectToProcess(
                        proc,
                        new ServerRequestCallback<ConsoleProcess>()
                        {
                           @Override
                           public void onResponseReceived(
                                 final ConsoleProcess cproc)
                           {
                              // first determine whether to create and/or
                              // show the dialog immediately
                              boolean createDialog = false;
                              boolean showDialog = false;

                              // standard dialog -- always show it
                              if (!proc.getShowOnOutput())
                              {
                                 createDialog = true;
                                 showDialog = true;
                              }

                              // showOnOutput dialog that already has 
                              // output -- make sure the user sees it
                              //
                              // NOTE: we have to trim the  buffered output
                              // for the comparison because when the password
                              // manager provides a password the back-end
                              // process sometimes echos a newline back to us
                              //
                              else if (proc.getBufferedOutput().trim().length() > 0)
                              {
                                 createDialog = true;
                                 showDialog = true;
                              }

                              // showOnOutput dialog that has exited
                              // and has no output -- reap it
                              else if (proc.getExitCode() != null)
                              {
                                 cproc.reap(new VoidServerRequestCallback());
                              }

                              // showOnOutput dialog with no output that is
                              // still running -- create but don't show yet
                              else
                              {
                                 createDialog = true;
                              }

                              // take indicated actions
                              if (createDialog)
                              {
                                 ConsoleProgressDialog dlg = new ConsoleProgressDialog(
                                       proc.getCaption(),
                                       cproc,
                                       proc.getBufferedOutput(),
                                       proc.getExitCode(),
                                       cryptoServer);

                                 if (showDialog)
                                    dlg.showModal();
                                 else
                                    dlg.showOnOutput();
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

         eventBus_.addHandler(
               ConsoleProcessCreatedEvent.TYPE,
               new ConsoleProcessCreatedEvent.Handler()
               {
                  private boolean handleEvent(String targetWindow)
                  {
                     // calculate the current window name
                     String window = StringUtil.notNull(
                                          satellite.getSatelliteName());
                     
                     // handle it if the target is us
                     if (window.equals(targetWindow))
                        return true;
                     
                     // also handle if we are the main window and the specified
                     // satellite doesn't exist
                     if (!Satellite.isCurrentWindowSatellite() &&
                         !satelliteManager.satelliteWindowExists(targetWindow))
                        return true;
                     
                     // otherwise don't handle
                     else
                        return false;
                  }
                  
                  
                  @Override
                  public void onConsoleProcessCreated(
                                    ConsoleProcessCreatedEvent event)
                  {
                     if (!handleEvent(event.getTargetWindow()))
                        return;
                     
                     ConsoleProcessInfo procInfo = event.getProcessInfo();
                     ConsoleProcess proc = new ConsoleProcess(server_, 
                                                              eventBus_, 
                                                              procInfo);
                     
                     ConsoleProgressDialog dlg = new ConsoleProgressDialog(
                                               procInfo.getCaption(),
                                               proc,
                                               procInfo.getBufferedOutput(),
                                               procInfo.getExitCode(),
                                               cryptoServer);
                     
                     if (procInfo.getShowOnOutput())
                        dlg.showOnOutput();
                     else
                        dlg.showModal();
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

      /**
       * Terminate and reap a process
       * @param handle process to kill and reap
       */
      public void interruptAndReap(final String handle)
      {
         interrupt(handle,
               new SimpleRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                    reap(handle, new VoidServerRequestCallback());
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
      }

      /**
       * Interrupt process with given handle.
       * @param handle process to interrupt
       * @param requestCallback callback to invoke when done
       */
      public void interrupt(final String handle, ServerRequestCallback<Void> requestCallback)
      {
         server_.processInterrupt(handle, requestCallback);
      }
      
      /**
       * Reap process with given handle
       * @param handle process to reap
       * @param requestCallback callback to invoke when done
       */
      public void reap(final String handle, ServerRequestCallback<Void> requestCallback)
      {
         server_.processReap(handle, requestCallback);
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
               public void onServerConsoleOutput(
                                             ServerConsoleOutputEvent event)
               {
                  if (event.getProcessHandle().equals(procInfo.getHandle()))
                     fireEvent(new ConsoleOutputEvent(event.getOutput()));
               }
            }));
      registrations_.add(eventBus.addHandler(
            ServerConsolePromptEvent.TYPE,
            new ServerConsolePromptEvent.Handler()
            {
               @Override
               public void onServerConsolePrompt(
                                             ServerConsolePromptEvent event)
               {
                  if (event.getProcessHandle().equals(procInfo.getHandle()))
                     fireEvent(new ConsolePromptEvent(event.getPrompt()));
               }
            }));
      registrations_.add(eventBus.addHandler(
            ServerProcessExitEvent.TYPE,
            new ServerProcessExitEvent.Handler()
            {
               @Override
               public void onServerProcessExit(ServerProcessExitEvent event)
               {   
                  if (event.getProcessHandle().equals(procInfo.getHandle()))
                  {
                     // no more events are coming
                     registrations_.removeHandler();
                     
                     fireEvent(new ProcessExitEvent(event.getExitCode()));
                  }
               }
            }
      ));
   }
   

   public ConsoleProcessInfo getProcessInfo()
   {
      return procInfo_;
   }
   
   public int getChannelMode()
   {
      if (forceRpc_)
         return ConsoleProcessInfo.CHANNEL_RPC;
      
      return procInfo_.getChannelMode(); 
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

   public void resizeTerminal(int cols, int rows, ServerRequestCallback<Void> requestCallback)
   {
      server_.processSetShellSize(procInfo_.getHandle(), cols, rows, requestCallback);
   }

   public void getTerminalBufferChunk(int chunk, ServerRequestCallback<ProcessBufferChunk> requestCallback)
   {
      server_.processGetBufferChunk(procInfo_.getHandle(), chunk, requestCallback);
   }
   
   public void useRpcMode(ServerRequestCallback<Void> requestCallback)
   {
      forceRpc_ = true;
      server_.processUseRpc(procInfo_.getHandle(), requestCallback);
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
   public HandlerRegistration addConsolePromptHandler(
                                          ConsolePromptEvent.Handler handler)
   {
      return handlers_.addHandler(ConsolePromptEvent.TYPE, handler);
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
   private boolean forceRpc_;
}
