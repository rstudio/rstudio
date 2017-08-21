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

import java.util.ArrayList;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.events.SessionSerializationHandler;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
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
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.inject.Inject;

/**
 * A connected Terminal session.
 */
public class TerminalSession extends XTermWidget
                             implements TerminalSessionSocket.Session,
                                        ResizeTerminalEvent.Handler,
                                        XTermTitleEvent.Handler,
                                        SessionSerializationHandler
{
   /**
    * @param info terminal metadata
    * @param cursorBlink should terminal cursor blink
    * @param focus should terminal automatically get focus
    */
   public TerminalSession(ConsoleProcessInfo info, 
                          boolean cursorBlink, 
                          boolean focus)
   {
      super(cursorBlink, focus);
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      procInfo_ = info;
      hasChildProcs_ = new Value<Boolean>(info.getHasChildProcs());
      
      setTitle(info.getTitle());
      socket_ = new TerminalSessionSocket(this, this);

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
   public void connect(final ResultCallback<Boolean, String> callback)
   {
      if (connected_ || connecting_ || terminating_)
      {
         callback.onSuccess(connected_ && !terminating_);
         return;
      }

      connecting_ = true;
      setNewTerminal(StringUtil.isNullOrEmpty(getHandle()));
      
      socket_.resetDiagnostics();

      server_.startTerminal(
            getProcInfo(),
            new ServerRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess consoleProcess)
         {
            consoleProcess_ = consoleProcess;
            if (consoleProcess_ == null)
            {
               disconnect(false);
               callback.onFailure("No Terminal ConsoleProcess received from server");
               return;
            }

            if (consoleProcess_.getProcessInfo().getCaption().isEmpty())
            {
               disconnect(false);
               callback.onFailure("Empty Terminal caption");
               return;
            } 

            if (consoleProcess_.getProcessInfo().getTerminalSequence() <= ConsoleProcessInfo.SEQUENCE_NO_TERMINAL)
            {
               disconnect(false);
               callback.onFailure("Undetermined Terminal sequence");
               return;
            } 
              
            // Keep an instance of the ProcessInfo so it is available even if the terminal
            // goes offline, which causes consoleProcess_ to become null.
            procInfo_ = consoleProcess_.getProcessInfo();

            addHandlerRegistration(addResizeTerminalHandler(TerminalSession.this));
            addHandlerRegistration(addXTermTitleHandler(TerminalSession.this));
            addHandlerRegistration(eventBus_.addHandler(SessionSerializationEvent.TYPE, TerminalSession.this));
            
            showAltAfterReload_ = false;
            if (!getProcInfo().getAltBufferActive() && xtermAltBufferActive())
            {
               // If server reports the terminal is not showing alt-buffer, but local terminal
               // emulator is showing alt-buffer, terminal was killed while running
               // a full-screen program. Switch local terminal back to primary buffer before
               // we reload the cache from the server.
               showPrimaryBuffer();
            }
            else if (getProcInfo().getAltBufferActive() && !xtermAltBufferActive())
            {
               // Server is targeting alt-buffer, but local terminal emulator is showing
               // the main buffer. Possible when refreshing with a full-screen program running.
               // Switch to alt-buffer after we reload the cache from the server.
               showAltAfterReload_ = true;
            }
            
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
                        callback.onSuccess(true /*connected*/);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        disconnect(false);
                        callback.onFailure(error.getUserMessage());
                     }
                  });

               }

               @Override
               public void onError(String errorMsg)
               {
                  disconnect(false);
                  callback.onFailure(errorMsg);
                  return;
               }
            });
         }

         @Override
         public void onError(ServerError error)
         {
            disconnect(false);
            callback.onFailure(error.getUserMessage());
         }
      });
   }
   
   /**
    * Disconnect a terminal.
    * @param permanent If true, the connection cannot be reopened.
    */
   public void disconnect(boolean permanent)
   {
      inputQueue_.setLength(0);
      inputSequence_ = ShellInput.IGNORE_SEQUENCE;
      socket_.disconnect(permanent);
      registrations_.removeHandler();
      consoleProcess_ = null;
      connected_ = false;
      connecting_ = false;
      restartSequenceWritten_ = false;
      reloading_ = false;
      deferredOutput_.clear();
   }

   @Override
   public void onResizeTerminal(ResizeTerminalEvent event)
   {
      procInfo_.setDimensions(event.getCols(), event.getRows());
      consoleProcess_.resizeTerminal(
            event.getCols(), event.getRows(),
            new VoidServerRequestCallback() 
            {
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  writeError(error.getUserMessage());
               }
            });
   }

   @Override
   public void receivedOutput(String output)
   {
      if (reloading_)
      {
         deferredOutput_.add(output);
         return;
      }
      socket_.dispatchOutput(output, doLocalEcho());
   }

   public void receivedSendToTerminal(String input)
   {
      // tweak line endings 
      String inputText = input;
      if (inputText != null && BrowseCap.isWindowsDesktop())
      {
         int shellType = getProcInfo().getShellType();
         if (shellType == TerminalShellInfo.SHELL_CMD32 ||
               shellType == TerminalShellInfo.SHELL_CMD64 ||
               shellType == TerminalShellInfo.SHELL_PS32 ||
               shellType == TerminalShellInfo.SHELL_PS64)
         {
            inputText = StringUtil.normalizeNewLinesToCR(inputText);
         }
      }
      receivedInput(inputText);
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
         connect(new ResultCallback<Boolean, String>()
         {
            @Override
            public void onSuccess(Boolean connected) 
            {
               if (connected)
               {
                  sendUserInput();
                  return;
               }
            }
            
            @Override
            public void onFailure(String msg)
            {
               Debug.log(msg);
               writeError(msg);
            }
         });
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

      // On desktop, rapid typing sometimes causes RPC messages for writeStandardInput
      // to arrive out of sequence in the terminal; send a sequence number with each
      // message so server can put messages back in order
      if (Desktop.isDesktop() && 
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
                  Debug.logError(error);
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
            !getHasChildProcs() &&
            !xtermAltBufferActive() &&
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
      return procInfo_.getCaption();
   }

   /**
    * Erase the scrollback buffer on the client and server.
    */
   public void clearBuffer()
   {
      clear();
      
      // talk directly to the server so it will wake up if suspended and
      // clear its buffer cache
      server_.processEraseBuffer(
            getHandle(), 
            false /*lastLineOnly*/,
            new SimpleRequestCallback<Void>("Clearing Buffer"));
   }

   /**
    * Send an interrupt (SIGINT) to the terminal's child process
    */
   public void interruptTerminal()
   {
      server_.processInterruptChild(getHandle(), 
            new SimpleRequestCallback<Void>("Interrupting child"));
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
      writeln(AnsiCode.ForeColor.RED + "Error: " + msg + AnsiCode.DEFAULTCOLORS);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      connect(new ResultCallback<Boolean, String>()
      {
         @Override
         public void onSuccess(Boolean connected) 
         {
         }

         @Override
         public void onFailure(String msg)
         {
            Debug.log(msg);
            writeError(msg);
         }
      });
   }

   @Override
   protected void onDetach()
   {
      super.onDetach();
      disconnect(false);
      unregisterHandlers();
   }

   @Override
   public void setVisible(boolean isVisible)
   {
      super.setVisible(isVisible);
      if (isVisible)
      {
         connect(new ResultCallback<Boolean, String>()
         {
            @Override
            public void onSuccess(Boolean connected) 
            {
               if (connected)
               {
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

            @Override
            public void onFailure(String msg)
            {
               Debug.log(msg);
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
      return procInfo_.getHandle(); 
   }
   
   /**
    * Does this terminal's shell program (i.e. bash) have any child processes?
    * @return true if it has child processes, or it hasn't been determined yet
    */
   public boolean getHasChildProcs()
   {
      return hasChildProcs_.getValue();
   }

   /**
    * Set state of hasChildProcs flag
    * @param hasChildProcs new state for flag
    */
   public void setHasChildProcs(boolean hasChildProcs)
   {
      hasChildProcs_.setValue(hasChildProcs, true);
   }

   public HandlerRegistration addHasChildProcsChangeHandler(ValueChangeHandler<Boolean> handler)
   {
      return hasChildProcs_.addValueChangeHandler(handler);
   }

   /**
    * Forcibly terminate the process associated with this terminal session.
    */
   public void terminate()
   {
      terminating_ = true;

      // Talk directly to the server; this will wake it up if suspended so
      // it can actually get rid of the process record.
      server_.processInterrupt(getHandle(), new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
            server_.processReap(getHandle(), new VoidServerRequestCallback());
            cleanupAfterTerminate();
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);

            // If we are in a state where the server doesn't know about the 
            // terminal we are trying to kill and returned an error, 
            // eradicate it on the client so it goes away.
            cleanupAfterTerminate();
         }
      });
   }
   
   private void cleanupAfterTerminate()
   {
      // Forcefully kill this session on the client instead of waiting 
      // for the ProcessExitEvent which we won't get in some scenarios 
      // such as issuing terminate while session was suspended, or if
      // something is just plain busted and the session isn't accepting
      // input.
      unregisterHandlers();
      eventBus_.fireEvent(new TerminalSessionStoppedEvent(TerminalSession.this));
   }

   @Override
   public void onSessionSerialization(SessionSerializationEvent event)
   {
      switch(event.getAction().getType())
      {
      case SessionSerializationAction.SUSPEND_SESSION:
         disconnect(false);
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
      deferredOutput_.clear();
      if (newTerminal_)
      {
         reloading_ = false;
         setNewTerminal(false);
      }
      else
      {
         reloading_ = true;
         fetchNextChunk(0);
      }
   }

   private boolean shellSupportsReload()
   {
      if (consoleProcess_ == null)
         return false;
      
      switch (consoleProcess_.getProcessInfo().getShellType())
      {
      // Windows command-prompt and PowerShell don't support buffer reloading
      // due to limitations of how they work with WinPty.
      case TerminalShellInfo.SHELL_CMD32:
      case TerminalShellInfo.SHELL_CMD64:
      case TerminalShellInfo.SHELL_PS32:
      case TerminalShellInfo.SHELL_PS64:
         return false;

      default:
         return true;
      }
   }
   
   private void fetchNextChunk(final int chunkToFetch)
   {
      if (!shellSupportsReload())
      {
         reloading_ = false;
         return;
      }

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
                  public void onResponseReceived(final ProcessBufferChunk chunk)
                  {
                     write(chunk.getChunk());
                     if (chunk.getMoreAvailable())
                     {
                        fetchNextChunk(chunk.getChunkNumber() + 1);
                     }
                     else
                     {
                        writeRestartSequence();
                        if (procInfo_.getZombie())
                           showZombieMessage();
                        reloading_ = false;
                        for (String outputStr : deferredOutput_)
                        {
                           socket_.dispatchOutput(outputStr, doLocalEcho());
                        }
                        deferredOutput_.clear();
                     }
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                     writeError(error.getUserMessage());
                     reloading_ = false;
                     deferredOutput_.clear();
                  }
               });
            }
         }
      });
   }
   
   public void showZombieMessage()
   {
      writeln("[Process completed]");
      write("[Exit code: ");
      if (procInfo_.getExitCode() != null)
         write(Integer.toString(procInfo_.getExitCode()));
      else
         write("Unknown");
      writeln("]");
   }

   /**
    * Write to terminal after a terminal has restarted (on the server). We
    * use this to cleanup the current line, as a new prompt is typically
    * output by the server upon reconnect.
    * 
    * For a full-screen program, switch the terminal back into the alt-buffer.
    */
   public void writeRestartSequence()
   {
      if (consoleProcess_ != null && 
            consoleProcess_.getProcessInfo().getRestarted() &&
            !restartSequenceWritten_)
      {
         // Move cursor to first column and clear to end-of-line
         final String sequence = AnsiCode.CSI + AnsiCode.CHA + AnsiCode.CSI + AnsiCode.EL;

         // immediately clear line locally
         write(sequence);
         
         // ask server to delete last line of saved buffer to prevent
         // accumulation of prompts
         server_.processEraseBuffer(
               getHandle(), 
               true /*lastLineOnly*/,
               new SimpleRequestCallback<Void>("Clearing Final Line of Buffer"));

         restartSequenceWritten_ = true;
      }

      if (showAltAfterReload_)
      {
         showAltBuffer();
         showAltAfterReload_ = false;
      }
   }
   
   /**
    * Set if connecting to a new terminal session.
    * @param isNew true if a new connection, false if a reconnect
    */
   private void setNewTerminal(boolean isNew)
   {
      newTerminal_ = isNew;
   }
   
   public ConsoleProcessInfo getProcInfo()
   {
      return procInfo_;
   }
   
   public void getBuffer(final boolean stripAnsiCodes, final ResultCallback<String, String> callback)
   {
      consoleProcess_.getTerminalBuffer(stripAnsiCodes, new ServerRequestCallback<ProcessBufferChunk>()
      {
         @Override
         public void onResponseReceived(final ProcessBufferChunk chunk)
         {
            String buffer = chunk.getChunk();
            callback.onSuccess(buffer);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   public TerminalSessionSocket getSocket()
   {
      return socket_;
   }

   private HandlerRegistrations registrations_ = new HandlerRegistrations();
   private TerminalSessionSocket socket_;
   private ConsoleProcess consoleProcess_;
   private ConsoleProcessInfo procInfo_;
   private String title_;
   private final HasValue<Boolean> hasChildProcs_;
   private boolean connected_;
   private boolean connecting_;
   private boolean terminating_;
   private boolean reloading_;
   private ArrayList<String> deferredOutput_ = new ArrayList<String>();
   private boolean restartSequenceWritten_;
   private StringBuilder inputQueue_ = new StringBuilder();
   private int inputSequence_ = ShellInput.IGNORE_SEQUENCE;
   private boolean newTerminal_ = true;
   private boolean showAltAfterReload_;

   // Injected ---- 
   private WorkbenchServerOperations server_; 
   private EventBus eventBus_;
   private UIPrefs uiPrefs_;
}
