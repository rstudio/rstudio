/*
 * TerminalSession.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.events.SessionSerializationHandler;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.model.ProcessBufferChunk;
import org.rstudio.studio.client.workbench.views.terminal.events.ResizeTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalTitleEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

/**
 * A connected Terminal session.
 */
public class TerminalSession extends XTermWidget
                             implements TerminalSessionSocket.Session,
                                        ProcessExitEvent.Handler,
                                        ResizeTerminalEvent.Handler,
                                        XTermTitleEvent.Handler,
                                        SessionSerializationHandler
{
   /**
    * 
    * @param sequence number used as part of default terminal title
    * @param handle terminal handle if reattaching, null if new terminal
    * @param caption terminal caption if reattaching, null if new terminal
    * @param title widget title
    * @param hasChildProcs does session have child processes
    * @param cols number of columns in terminal
    * @param rows number of rows in terminal
    * @param shellType type of shell to run
    */
   public TerminalSession(int sequence,
                          String handle,
                          String caption,
                          String title,
                          boolean hasChildProcs,
                          int cols,
                          int rows,
                          boolean cursorBlink,
                          int shellType)
   {
      super(cursorBlink);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      sequence_ = sequence;
      terminalHandle_ = handle;
      hasChildProcs_ = hasChildProcs;
      shellType_ = shellType;
      setTitle(title);
      socket_ = new TerminalSessionSocket(this, this);

      if (StringUtil.isNullOrEmpty(caption))
         caption_ = "Terminal " + sequence_;
      else
         caption_ = caption;

      setHeight("100%");
   }

   @Inject
   private void initialize(WorkbenchServerOperations server,
                           EventBus events,
                           UIPrefs uiPrefs)
   {
      server_ = server;
      eventBus_ = events; 
      uiPrefs_ = uiPrefs;
   } 

   /**
    * Create a terminal process and connect to it. Multiple async stages:
    * 
    * (1) get a ConsoleProcess from the server; this tracks the terminal
    *     session and process on the server
    * (2) get a TerminalSessionSocket which supplies input/output channel
    *     to the remote terminal (RPC or WebSocket)
    * (3) start (or reconnect to) the server-side process for the terminal
    */
   public void connect()
   {
      if (connected_ || connecting_ || terminating_)
         return;

      connecting_ = true;
      setNewTerminal(getHandle() == null);
      
      socket_.resetDiagnostics();

      server_.startTerminal(getShellType(),
            getCols(), getRows(), getHandle(), getCaption(), 
            getTitle(), uiPrefs_.terminalUseWebsockets().getValue(), 
            getSequence(), new ServerRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess consoleProcess)
         {
            consoleProcess_ = consoleProcess;
            if (consoleProcess_ == null)
            {
               writeError("No ConsoleProcess received from server");
               disconnect();
               return;
            }

            if (getInteractionMode() != ConsoleProcessInfo.INTERACTION_ALWAYS)
            {
               writeError("Unsupported ConsoleProcess interaction mode");
               disconnect();
               return;
            } 

            addHandlerRegistration(consoleProcess_.addProcessExitHandler(TerminalSession.this));
            addHandlerRegistration(addResizeTerminalHandler(TerminalSession.this));
            addHandlerRegistration(addXTermTitleHandler(TerminalSession.this));
            addHandlerRegistration(eventBus_.addHandler(SessionSerializationEvent.TYPE, TerminalSession.this));
            
            socket_.connect(consoleProcess_, new TerminalSessionSocket.ConnectCallback()
            {
               @Override
               public void onConnected()
               {
                  consoleProcess_.start(new ServerRequestCallback<Void>()
                  {
                     @Override
                     public void onResponseReceived(Void response)
                     {
                        connected_ = true;
                        connecting_ = false;
                        sendUserInput();
                        eventBus_.fireEvent(new TerminalSessionStartedEvent(TerminalSession.this));
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        disconnect();
                        writeError(error.getUserMessage());
                     }
                  });

               }

               @Override
               public void onError(String errorMsg)
               {
                  writeError(errorMsg);
                  disconnect();
                  return;
               }
            });
         }

         @Override
         public void onError(ServerError error)
         {
            disconnect();
            writeError(error.getUserMessage());
         }

      });
   }

   /**
    * Disconnect a connected terminal. Allows for reconnection.
    */
   private void disconnect()
   {
      inputQueue_.setLength(0);
      socket_.disconnect();
      registrations_.removeHandler();
      consoleProcess_ = null;
      connected_ = false;
      connecting_ = false;
   }

   @Override
   public void onProcessExit(ProcessExitEvent event)
   {
      unregisterHandlers();
      if (consoleProcess_ != null)
      {
         consoleProcess_.reap(new VoidServerRequestCallback());
      }

      eventBus_.fireEvent(new TerminalSessionStoppedEvent(this));
   }

   @Override
   public void onResizeTerminal(ResizeTerminalEvent event)
   {
      cols_ = event.getCols();
      rows_ = event.getRows();
      consoleProcess_.resizeTerminal(
            cols_, rows_,
            new VoidServerRequestCallback() 
            {
               @Override
               public void onError(ServerError error)
               {
                  writeError(error.getUserMessage());
               }
            });
   }

   @Override
   public void receivedOutput(String output)
   {
      socket_.dispatchOutput(output, doLocalEcho());
   }

   @Override
   public void receivedInput(String input)
   {
      if (input != null)
      {
         inputQueue_.append(input);
      }

      if (!connected_)
      {
         // accumulate user input until we are connected, then play it back
         connect();
         return;
      }

      sendUserInput();
   }

   /**
    * Send user input to the server, breaking down into chunks. We do this
    * for when a large amount of text is pasted into the terminal; we don't
    * want to overwhelm the RPC.
    * @param userInput string to send
    */
   private void sendUserInput()
   {
      final int MAXCHUNK = 128;
      String userInput;

      if (inputQueue_.length() == 0)
      {
         return;
      }
      if (inputQueue_.length() > MAXCHUNK)
      {
         userInput = inputQueue_.substring(0, MAXCHUNK);
         inputQueue_.delete(0,  MAXCHUNK);
      }
      else
      {
         userInput = inputQueue_.toString();
         inputQueue_.setLength(0);
      }

      // On Windows, rapid typing sometimes causes RPC messages for writeStandardInput
      // to arrive out of sequence in the terminal; send a sequence number with each
      // message so server can put messages back in order
      if (BrowseCap.isWindowsDesktop() && 
            consoleProcess_.getChannelMode() == ConsoleProcessInfo.CHANNEL_RPC)
      {
         if (inputSequence_ == ShellInput.IGNORE_SEQUENCE)
         {
            // First message sent for this client-side terminal instance, start
            // by flushing the server-side queue to reset server's "last-sequence"
            // back to default.
            inputSequence_ = ShellInput.FLUSH_SEQUENCE;
         }
         else if (inputSequence_ == ShellInput.FLUSH_SEQUENCE)
         {
            // Last message has flushed server, start tracking sequences again.
            inputSequence_ = 0;
         }
         else if (inputSequence_ >= Integer.MAX_VALUE - 100)
         {
            // Very diligent typist!  Tell server to flush its input
            // queue, temporarily ignoring sequences.
            inputSequence_ = ShellInput.FLUSH_SEQUENCE;
         }
         else
         {
            inputSequence_++;
         }
      }
      
      socket_.dispatchInput(inputSequence_, userInput, doLocalEcho(),
            new VoidServerRequestCallback() {

               @Override
               public void onResponseReceived(Void response)
               {
                  sendUserInput();
               }

               @Override
               public void onError(ServerError error)
               {
                  writeError(error.getUserMessage());
               }
            });
   }
   
   /**
    * Should we do local-echo at this time?
    * @return true if local-echo should be active
    */
   private boolean doLocalEcho()
   {
      // Local echo is an optimization where we directly output a typed
      // character to the terminal without waiting for the echo back from
      // the server. This is to improve responsiveness in the most common
      // case of typing at a command-prompt. The code must reconcile what
      // was local-echoed with what comes back from the server, and there
      // are many cases it can't handle where we want to stop local-echo.
      //
      // There is a user-setting to totally disable local-echo.
      //
      // Win32's pty implementation returns escape sequences even when 
      // doing simple single-character input at a command-prompt. 
      //
      // Don't do local-echo when something is running (busy),
      // indicating we are likely not at a command-prompt.
      //
      // Don't do local-echo if terminal is showing full screen buffer; 
      // indicates something like vim or tmux is running; usually caught
      // by "busy" but there can be a lag between starting a full-screen
      // program and it showing up as "busy".
      //
      // Finally, only local-echo if typing at the end of the current line to
      // avoid issues with line-editing, such as inserting characters in the
      // middle of a line.
      return 
            uiPrefs_.terminalLocalEcho().getValue() &&
            !BrowseCap.isWindowsDesktop() && 
            !hasChildProcs_ &&
            !altBufferActive() &&
            cursorAtEOL();
   }

   @Override
   public void onXTermTitle(XTermTitleEvent event)
   {
      setTitle(event.getTitle());
      eventBus_.fireEvent(new TerminalTitleEvent(this));
   }

   @Override
   public void setTitle(String title)
   {
      // don't call superclass, don't want this acting as default tool-tip
      // for the widget
      title_ = title;
   }

   @Override
   public String getTitle()
   {
      return title_;
   }

   /**
    * @return terminal caption, such as "Terminal 1"
    */
   public String getCaption()
   {
      return caption_;
   }

   /**
    * Set caption, user customizable identifier for this terminal session.
    * @param caption new caption
    */
   public void setCaption(String caption)
   {
      if (StringUtil.isNullOrEmpty(caption))
         return;

      caption_ = caption;
   }
   
   /**
    * Erase the scrollback buffer on the client and server.
    */
   public void clearBuffer()
   {
      clear();
      
      // talk directly to the server so it will wake up if suspended and
      // clear its buffer cache
      server_.processEraseBuffer(getHandle(), new SimpleRequestCallback<Void>());
   }

   /**
    * Show modal dialog with information about this terminal session.
    */
   public void showTerminalInfo()
   {
      new TerminalInfoDialog(this, socket_).showModal();
   }
   
   private int getInteractionMode()
   {
      if (consoleProcess_ != null)
         return consoleProcess_.getProcessInfo().getInteractionMode();
      else
         return ConsoleProcessInfo.INTERACTION_NEVER;
   } 

   protected void addHandlerRegistration(HandlerRegistration reg)
   {
      registrations_.add(reg);
   }

   protected void unregisterHandlers()
   {
      registrations_.removeHandler();
      socket_.unregisterHandlers();
   }

   protected void writeError(String msg)
   {
      socket_.dispatchOutput(AnsiCode.ForeColor.RED + "Error: " + 
            msg + AnsiCode.DEFAULTCOLORS, false /*detectLocalEcho*/);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      connect();
   }

   @Override
   protected void onDetach()
   {
      super.onDetach();
      disconnect();
      unregisterHandlers();
   }

   @Override
   public void setVisible(boolean isVisible)
   {
      super.setVisible(isVisible);
      if (isVisible)
      {
         connect();

         // Inform the terminal that there may have been a resize. This could 
         // happen on first display, or if the terminal was hidden behind other
         // terminal sessions and there was a resize.
         // A delay is needed to give the xterm.js implementation an
         // opportunity to be ready for this.
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               onResize();
            }
         });
      }
   }

   /**
    * A unique handle for this terminal instance. Corresponds to the 
    * server-side ConsoleProcess handle.
    * @return Opaque string handle for this terminal instance, or null if
    * terminal has never been attached to a server ConsoleProcess.
    */
   public String getHandle()
   {
      if (consoleProcess_ == null)
      {
         return terminalHandle_;
      }
      terminalHandle_ = consoleProcess_.getProcessInfo().getHandle();
      return terminalHandle_;
   }
   
   public int getShellType()
   {
      if (consoleProcess_ == null)
      {
         return shellType_;
      }
      return consoleProcess_.getProcessInfo().getShellType();
   }

   /**
    * Does this terminal's shell program (i.e. bash) have any child processes?
    * @return true if it has child processes, or it hasn't been determined yet
    */
   public boolean getHasChildProcs()
   {
      return hasChildProcs_;
   }

   /**
    * Set state of hasChildProcs flag
    * @param hasChildProcs new state for flag
    */
   public void setHasChildProcs(boolean hasChildProcs)
   {
      hasChildProcs_ = hasChildProcs;
   }


   /**
    * The sequence number of the terminal, used in creation of the default
    * title, e.g. "Terminal 3".
    * @return The sequence number that was passed to the constructor.
    */
   public int getSequence()
   {
      return sequence_;
   }

   public int getCols()
   {
      return cols_;
   }

   public int getRows()
   {
      return rows_;
   }

   /**
    * Forcibly terminate the process associated with this terminal session.
    */
   public void terminate()
   {
      terminating_ = true;

      // Talk directly to the server; this will wake it up if suspended so
      // it can actually get rid of the process record.
      server_.processInterrupt(getHandle(), new SimpleRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
            server_.processReap(getHandle(), new VoidServerRequestCallback());

            // Forcefully kill this session on the client instead of waiting 
            // for the ProcessExitEvent which we won't get in some scenarios 
            // such as issuing terminate while session was suspended, or if
            // something is just plain busted and the session isn't accepting
            // input.
            unregisterHandlers();
            eventBus_.fireEvent(new TerminalSessionStoppedEvent(TerminalSession.this));
         }
      });
   }

   @Override
   public void onSessionSerialization(SessionSerializationEvent event)
   {
      switch(event.getAction().getType())
      {
      case SessionSerializationAction.SUSPEND_SESSION:
         disconnect();
         break;
      }
   }

   /**
    * @return true if terminal is connected to server, false if not
    */
   public boolean isConnected()
   {
      return connected_;
   }

   /**
    * Perform actions when the terminal is ready.
    */
   @Override
   protected void terminalReady()
   {
      if (newTerminal_)
      {
         setNewTerminal(false);
      }
      else
      {
         fetchNextChunk(0);
      }
   }

   private void fetchNextChunk(final int chunkToFetch)
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            onResize();
            if (consoleProcess_ != null)
            {
               consoleProcess_.getTerminalBufferChunk(chunkToFetch,
                     new ServerRequestCallback<ProcessBufferChunk>()
               {
                  @Override
                  public void onResponseReceived(ProcessBufferChunk chunk)
                  {
                     write(chunk.getChunk());
                     if (chunk.getMoreAvailable())
                     {
                        fetchNextChunk(chunk.getChunkNumber() + 1);
                     }
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     writeError(error.getUserMessage());
                  }
               });
            }
         }
      });
   }

   /**
    * Set if connecting to a new terminal session.
    * @param isNew true if a new connection, false if a reconnect
    */
   private void setNewTerminal(boolean isNew)
   {
      newTerminal_ = isNew;
   }

   private HandlerRegistrations registrations_ = new HandlerRegistrations();
   private TerminalSessionSocket socket_;
   private ConsoleProcess consoleProcess_;
   private String caption_;
   private String title_;
   private final int sequence_;
   private String terminalHandle_;
   private boolean hasChildProcs_;
   private int shellType_;
   private boolean connected_;
   private boolean connecting_;
   private boolean terminating_;
   private StringBuilder inputQueue_ = new StringBuilder();
   private int inputSequence_ = ShellInput.IGNORE_SEQUENCE;
   private boolean newTerminal_ = true;
   private int cols_ = ConsoleProcessInfo.DEFAULT_COLS;
   private int rows_ = ConsoleProcessInfo.DEFAULT_ROWS;;

   // Injected ---- 
   private WorkbenchServerOperations server_; 
   private EventBus eventBus_;
   private UIPrefs uiPrefs_;
}
