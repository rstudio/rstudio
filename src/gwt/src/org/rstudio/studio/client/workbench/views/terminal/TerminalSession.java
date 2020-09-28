/*
 * TerminalSession.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.user.client.Timer;
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
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.model.ProcessBufferChunk;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalReceivedConsoleProcessInfoEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStoppedEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalTitleEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermOptions;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermTheme;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;
import com.google.inject.Inject;

/**
 * A connected Terminal session.
 */
public class TerminalSession extends XTermWidget
                             implements TerminalSessionSocket.Session,
                                        XTermTitleEvent.Handler,
   SessionSerializationEvent.Handler,
                                        ThemeChangedEvent.Handler
{
   /**
    * @param info terminal metadata
    * @param options terminal emulator options
    * @param tabMovesFocus does pressing tab key move focus out of terminal
    * @param showWebLinks links detected and made clickable
    * @param createdByApi was this terminal just created by the rstudioapi
    */
   public TerminalSession(ConsoleProcessInfo info,
                          XTermOptions options,
                          boolean tabMovesFocus,
                          boolean showWebLinks,
                          boolean createdByApi)
   {
      super(options, tabMovesFocus, showWebLinks);

      RStudioGinjector.INSTANCE.injectMembers(this);
      procInfo_ = info;
      createdByApi_ = createdByApi;
      hasChildProcs_ = new Value<>(!BrowseCap.isWindowsDesktop() && info.getHasChildProcs());

      setTitle(info.getTitle());
      socket_ = new TerminalSessionSocket(
            this, this,
            sessionInfo_.getWebSocketPingInterval(),
            sessionInfo_.getWebSocketConnectTimeout());

      setHeight("100%");
   }

   @Inject
   private void initialize(WorkbenchServerOperations server,
                           EventBus events,
                           final Session session,
                           UserPrefs uiPrefs,
                           GlobalDisplay globalDisplay)
   {
      server_ = server;
      eventBus_ = events;
      uiPrefs_ = uiPrefs;
      sessionInfo_ = session.getSessionInfo();
      globalDisplay_ = globalDisplay;
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
      if (connected_ || connecting_ || terminating_ || !terminalEmulatorLoaded())
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
            eventBus_.fireEvent(new TerminalReceivedConsoleProcessInfoEvent(procInfo_));

            addHandlerRegistration(addXTermTitleHandler(TerminalSession.this));
            addHandlerRegistration(eventBus_.addHandler(SessionSerializationEvent.TYPE, TerminalSession.this));
            addHandlerRegistration(eventBus_.addHandler(ThemeChangedEvent.TYPE, TerminalSession.this));
            addHandlerRegistration(uiPrefs_.blinkingCursor().bind(arg -> updateOption("cursorBlink", arg)));
            addHandlerRegistration(uiPrefs_.tabKeyMoveFocus().bind(arg -> setTabMovesFocus(arg)));
            addHandlerRegistration(uiPrefs_.terminalBellStyle().bind(arg ->
            {
               // don't enable bell if we aren't done loading, don't want beeps if reloading
               // previous output containing '\a'
               if (haveLoadedBuffer_)
                  updateOption("bellStyle", arg);
            }));
            addHandlerRegistration(uiPrefs_.terminalRenderer().bind(arg ->
            {
               updateOption("rendererType", arg);
               onResize();
            }));
            addHandlerRegistration(uiPrefs_.fontSizePoints().bind(arg ->
            {
               updateOption("fontSize", XTermTheme.adjustFontSize(arg));
               updateOption("lineHeight", XTermTheme.computeLineHeight());
               onResize();
            }));

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
                  if (!StringUtil.isNullOrEmpty(errorMsg))
                  {
                     globalDisplay_.showMessage(GlobalDisplay.MSG_ERROR,
                           "Terminal Failed to Connect", errorMsg);
                  }
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
      setNotReloading();
      deferredOutput_.clear();
   }

   @Override
   public void resizePTY(int cols, int rows)
   {
      if (consoleProcess_ == null || (procInfo_.getCols() == cols && procInfo_.getRows() == rows))
         return;

      procInfo_.setDimensions(cols, rows);
      consoleProcess_.resizeTerminal(cols, rows,
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
   public void onThemeChanged(ThemeChangedEvent event)
   {
      // need a lag to ensure the new css has been applied, otherwise we pick up the
      // the original and the terminal stays in the previous style until reloaded
      new Timer()
      {
         @Override
         public void run()
         {
            updateTheme(XTermTheme.terminalThemeFromEditorTheme());
         }
      }.schedule(250);
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

   @Override
   public void connectionDisconnected()
   {
      disconnect(false);
   }

   public void receivedSendToTerminal(String input)
   {
      // tweak line endings
      String inputText = input;
      if (inputText != null && BrowseCap.isWindowsDesktop())
      {
         String shellType = getProcInfo().getShellType();
         if (shellType == UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_CMD ||
             shellType == UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_PS ||
             shellType == UserPrefs.WINDOWS_TERMINAL_SHELL_PS_CORE ||
               (BrowseCap.isWindowsDesktop() && shellType == UserPrefs.WINDOWS_TERMINAL_SHELL_CUSTOM))
         {
            inputText = StringUtil.normalizeNewLinesToCR(inputText);
         }
      }
      receivedInput(inputText);
   }

   public boolean isPosixShell()
   {
      String shellType = getProcInfo().getShellType();
      return shellType != UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_PS &&
             shellType != UserPrefs.WINDOWS_TERMINAL_SHELL_PS_CORE &&
             shellType != UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_CMD;
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
         inputQueue_.delete(0, MAXCHUNK);
      }
      else
      {
         userInput = inputQueue_.toString();
         inputQueue_.setLength(0);
      }

      // On desktop, rapid typing sometimes causes RPC messages for writeStandardInput
      // to arrive out of sequence in the terminal; send a sequence number with each
      // message so server can put messages back in order
      if (Desktop.hasDesktopFrame() &&
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
      String title = event.getTitle();

      // On default configuration of git-bash (Windows), the title is reported by the shell
      // with a leading colon character; this makes sense if $TITLEPREFIX is set, as that
      // will be put before the colon, such as "MINGW64:/c/Users/foo", but strip the colon
      // if there's nothing before it
      if (BrowseCap.isWindowsDesktop() && !StringUtil.isNullOrEmpty(title) && title.startsWith(":/"))
      {
         title = title.substring(1);
      }
      setTitle(title);
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
            new SimpleRequestCallback<>("Clearing Buffer"));
   }

   /**
    * Send an interrupt (SIGINT) to the terminal's child process
    */
   public void interruptTerminal()
   {
      server_.processInterruptChild(getHandle(),
            new SimpleRequestCallback<>("Interrupting child"));
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
         refresh();
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
                  Scheduler.get().scheduleDeferred(() -> onResize());
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
      if (event.getAction().getType() == SessionSerializationAction.SUSPEND_SESSION)
      {
         disconnect(false);
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
    * Reload terminal buffer.
    */
   public void reloadBuffer()
   {
      deferredOutput_.clear();
      if (newTerminal_)
      {
         setNotReloading();
         setNewTerminal(false);
      }
      else
      {
         setReloading();
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
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_CMD:
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_PS:
      case UserPrefs.WINDOWS_TERMINAL_SHELL_PS_CORE:
         // Do load the buffer if terminal was just created via API, as
         // the initial message and prompt may have been sent before the
         // client/server channel was opened.
         return createdByApi_;

      case UserPrefs.WINDOWS_TERMINAL_SHELL_CUSTOM:
         // on Windows we don't know if custom shell supports reload so
         // assume it does not
         if (BrowseCap.isWindowsDesktop())
            return createdByApi_;
         else
            return true;

      default:
         return true;
      }
   }

   private void fetchNextChunk(final int chunkToFetch)
   {
      if (!shellSupportsReload())
      {
         setNotReloading();
         return;
      }

      Scheduler.get().scheduleDeferred(() ->
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
                  accept(chunk.getChunk());
                  if (chunk.getMoreAvailable())
                  {
                     fetchNextChunk(chunk.getChunkNumber() + 1);
                  }
                  else
                  {
                     writeRestartSequence();
                     if (procInfo_.getZombie())
                        showZombieMessage();
                     setNotReloading();
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
                  setNotReloading();
                  deferredOutput_.clear();
               }
            });
         }
      });
   }

   public void showZombieMessage()
   {
      writeln("[Process completed]");
      accept("[Exit code: ");
      if (procInfo_.getExitCode() != null)
         accept(Integer.toString(procInfo_.getExitCode()));
      else
         accept("Unknown");
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
         accept(sequence);

         // ask server to delete last line of saved buffer to prevent
         // accumulation of prompts
         server_.processEraseBuffer(
               getHandle(),
               true /*lastLineOnly*/,
               new SimpleRequestCallback<>("Clearing Final Line of Buffer"));

         restartSequenceWritten_ = true;
      }

      if (showAltAfterReload_)
      {
         showAltBuffer();

         // Ctrl+L causes most ncurses program to refresh themselves
         receivedInput("\f");
         showAltAfterReload_ = false;
      }
   }

   /**
    * Set true if connecting to a newly created terminal session; i.e. one that won't have
    * any previous output to reload. False if this is a previous terminal session.
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

   private void setReloading()
   {
      reloading_ = true;
   }

   /**
    * Has this terminal loaded the buffer (e.g. upon reconnecting to an existing session)?
    */
   public boolean haveLoadedBuffer()
   {
      return haveLoadedBuffer_;
   }

   private void setNotReloading()
   {
      // Always start terminal emulator with BEL support off to avoid playing back previous
      // "\a" when reloading, then set it to desired value when done reloading. Slight delay
      // needed to ensure terminal has completed rendering the output.
      new Timer() {
         @Override
         public void run()
         {
            updateOption("bellStyle", uiPrefs_.terminalBellStyle().getValue());
         }
      }.schedule(500);
      reloading_ = false;
      haveLoadedBuffer_ = true;
   }

   private final HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final TerminalSessionSocket socket_;
   private ConsoleProcess consoleProcess_;
   private ConsoleProcessInfo procInfo_;
   private String title_;
   private final HasValue<Boolean> hasChildProcs_;
   private boolean connected_;
   private boolean connecting_;
   private boolean terminating_;
   private boolean reloading_;
   private boolean haveLoadedBuffer_;
   private final ArrayList<String> deferredOutput_ = new ArrayList<>();
   private boolean restartSequenceWritten_;
   private final StringBuilder inputQueue_ = new StringBuilder();
   private int inputSequence_ = ShellInput.IGNORE_SEQUENCE;
   private boolean newTerminal_ = true;
   private boolean showAltAfterReload_;
   private final boolean createdByApi_;

   // Injected ----
   private WorkbenchServerOperations server_;
   private EventBus eventBus_;
   private UserPrefs uiPrefs_;
   private SessionInfo sessionInfo_;
   private GlobalDisplay globalDisplay_;
}
