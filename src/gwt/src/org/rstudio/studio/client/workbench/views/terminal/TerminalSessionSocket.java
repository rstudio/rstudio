/*
 * TerminalSessionSocket.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONValue;

/**
 * Manages input and output for the terminal session.
 */
public class TerminalSessionSocket
   implements ConsoleOutputEvent.Handler,
              TerminalDataInputEvent.Handler
{
   public interface Session
   {
      /**
       * Called when there is user input to process.
       * @param input user input
       */
      void receivedInput(String input);

      /**
       * Called when there is output from the server.
       * @param output output from server
       */
      void receivedOutput(String output);

      /**
       * Called to disconnect the terminal
       */
      void connectionDisconnected();
   }

   public interface ConnectCallback
   {
      /**
       * Callback when connection has been made.
       */
      void onConnected();

      /**
       * Callback when connection failed
       * @param message additional info about the connect failure, may be null
       */
      void onError(String message);
   }

   /**
    * The console-process operations a connection uses. ConsoleProcess
    * implements it; tests substitute controlled RPC completions.
    */
   public interface Process
   {
      ConsoleProcessInfo getProcessInfo();
      int getChannelMode();
      HandlerRegistration addConsoleOutputHandler(ConsoleOutputEvent.Handler handler);
      void useRpcMode(ServerRequestCallback<VoidResponse> callback);
      void writeStandardInput(ShellInput input, ServerRequestCallback<VoidResponse> callback);
   }

   /**
    * The terminal emulator that provides user input and displays output.
    */
   public interface Emulator extends Consumer<String>
   {
      HandlerRegistration addTerminalDataInputHandler(TerminalDataInputEvent.Handler handler);
   }

   /**
    * Constructor
    * @param session Session to callback with user input and server output.
    * @param xterm Terminal emulator that provides user input, and displays output.
    * @param webSocketPingInterval (seconds) how often to send a keep-alive, or zero for none
    * @param webSocketConnectTimeout (seconds) how long to wait for websocket connection before
    *                                switching to RPC, or zero for no timeout (in seconds)
    */
   public TerminalSessionSocket(Session session,
                                Emulator xterm,
                                int webSocketPingInterval,
                                int webSocketConnectTimeout)
   {
      this(session,
           xterm,
           webSocketPingInterval,
           webSocketConnectTimeout,
           TerminalWebSocket::new,
           TerminalWebSocket.browserTimers());
   }

   TerminalSessionSocket(Session session,
                         Emulator xterm,
                         int webSocketPingInterval,
                         int webSocketConnectTimeout,
                         TerminalWebSocket.Factory socketFactory,
                         TerminalWebSocket.Scheduler scheduler)
   {
      scheduler_ = scheduler;
      session_ = session;
      xterm_ = xterm;
      localEcho_ = new TerminalLocalEcho(xterm_);
      webSocketPingInterval_ = webSocketPingInterval;
      webSocketConnectTimeout_ = webSocketConnectTimeout;
      socketFactory_ = socketFactory;
   }

   /**
    * Connect the input/output channel to the server. This requires that
    * an rsession has already been started via RPC and the consoleProcess
    * received.
    * @param consoleProcess
    * @param callback result of connect attempt
    */
   public void connect(Process consoleProcess,
                       final ConnectCallback callback)
   {
      final int connection = connection_ + 1;
      disconnect(false);
      // A pending input's cancellation callback may have connected again.
      // That newer attempt owns the socket and callbacks; do not overwrite it.
      if (connection != connection_)
         return;
      consoleProcess_ = consoleProcess;
      connectCallback_ = callback;

      if (consoleProcess.getProcessInfo().getZombie())
      {
         diagnostic_.log(constants_.zombieNotReconnectingMessage());
         connectionCompleted();
         return;
      }

      // We keep this handler connected after a disconnect so
      // user input sent via RPC can wake up a suspended session
      if (terminalInputHandler_ == null)
         terminalInputHandler_ = xterm_.addTerminalDataInputHandler(this);

      addHandlerRegistration(consoleProcess_.addConsoleOutputHandler(this));

      switch (consoleProcess_.getChannelMode())
      {
      case ConsoleProcessInfo.CHANNEL_RPC:
         diagnostic_.log(constants_.connectedWithRPCMessage());
         connectionCompleted();
         break;

      case ConsoleProcessInfo.CHANNEL_WEBSOCKET:

         // For desktop IDE, talk directly to the websocket, anything else, go
         // through the server via the /p proxy.
         String urlSuffix = consoleProcess_.getProcessInfo().getChannelId() + "/terminal/" +
               consoleProcess_.getProcessInfo().getHandle() + "/";
         String url;
         if (Desktop.isDesktop())
         {
            url = "ws://127.0.0.1:" + urlSuffix;
         }
         else
         {
            url = GWT.getHostPageBaseURL();
            if (url.startsWith("https:"))
            {
               url = "wss:" + StringUtil.substring(url, 6) + "p/" + urlSuffix;
            }
            else if (url.startsWith("http:"))
            {
               url = "ws:" + StringUtil.substring(url, 5) + "p/" + urlSuffix;
            }
            else
            {
               callback.onError(constants_.unableToDiscoverWebsocketMessage());
               return;
            }
         }

         diagnostic_.log(constants_.connectWebSocketMessage() + url + "'");
         socket_ = socketFactory_.create(new TerminalWebSocket.Listener()
         {
            @Override
            public void onClose()
            {
               diagnostic_.log(constants_.websockedClosedMessage());
               session_.connectionDisconnected();
            }

            @Override
            public void onMessage(String msg)
            {
               onConsoleOutput(new ConsoleOutputEvent(TerminalSocketPacket.getMessage(msg)));
            }

            @Override
            public void onOpen()
            {
               diagnostic_.log(constants_.websocketConnectedMessage());
               connectionCompleted();
            }

            @Override
            public void onFailure(TerminalWebSocket.Failure failure)
            {
               diagnosticError(failureMessage(failure));
               switchToRPC();
            }
         }, webSocketPingInterval_, webSocketConnectTimeout_);
         socket_.open(url);
         break;

      case ConsoleProcessInfo.CHANNEL_PIPE:
      default:
         callback.onError(constants_.channelTypeNotImplementedError());
         break;
      }
   }

   private void switchToRPC()
   {
      if (holdingInput_)
         return;
      holdingInput_ = true;
      closeWebSocket();
      localEcho_.clear();
      final int connection = connection_;

      // Unable to connect client to server via websocket; let server
      // know we'll be using rpc, instead
      consoleProcess_.useRpcMode(new ServerRequestCallback<VoidResponse>()
      {
         @Override
         public void onResponseReceived(VoidResponse response)
         {
            if (connection != connection_)
               return;
            diagnostic_.log(constants_.switchedToRPCMessage());
            flushPendingInput(connection);
         }

         @Override
         public void onError(ServerError error)
         {
            if (connection != connection_)
               return;
            holdingInput_ = false;
            diagnostic_.log(constants_.failedToSwitchRPCMessage() + error.getMessage());
            List<PendingInput> pending = new ArrayList<>(pendingInput_);
            pendingInput_.clear();
            ConnectCallback callback = connectCallback_;
            connectCallback_ = null;
            if (callback != null)
               callback.onError(constants_.terminalFailedToConnectMessage());
            else
               session_.connectionDisconnected();
            cancelPendingInput(pending, error);
         }
      });
   }

   /**
    * Send user input to the server.
    * @param input text to send
    * @param localEcho echo input locally
    * @param requestCallback callback
    */
   public void dispatchInput(String input,
                             boolean localEcho,
                             VoidServerRequestCallback requestCallback)
   {
      if (localEcho)
         localEcho_.echo(input);
      else
         localEcho_.clear();

      PendingInput pending = new PendingInput(input, requestCallback);
      if (holdingInput_)
      {
         pendingInput_.add(pending);
         return;
      }

      switch (consoleProcess_.getChannelMode())
      {
      case ConsoleProcessInfo.CHANNEL_RPC:
         if (outstandingWrites_ > 0)
         {
            // One write in flight at a time: concurrent requests can reach
            // the server out of order (#19542). Input typed meanwhile is
            // held and sent as one write when that write completes.
            //
            // This is a deliberate trade of latency for order. Held input
            // waits out the round trip of the write ahead of it, so its echo
            // can take up to two round trips instead of one, and a request
            // that never completes holds later input for RPC_WRITE_TIMEOUT_MS.
            // The alternative, numbering concurrent writes for the server to
            // reorder, stalls on a lost request as well and cannot tell a
            // late write of a previous connection from a live one.
            pendingInput_.add(pending);
            holdingInput_ = true;
         }
         else
         {
            writeRpcInput(pending);
         }
         break;
      case ConsoleProcessInfo.CHANNEL_WEBSOCKET:
         if (socket_ != null && socket_.send(TerminalSocketPacket.textPacket(input)))
         {
            requestCallback.onResponseReceived(null);
         }
         else
         {
            diagnosticError(constants_.sendUserInputMessage());
            pendingInput_.add(pending);
            switchToRPC();
         }
         break;
      case ConsoleProcessInfo.CHANNEL_PIPE:
      default:
         break;
      }
   }

   /**
    * Send output to the terminal emulator.
    * @param output text to send to the terminal
    * @param detectLocalEcho local-echo detection
    */
   public void dispatchOutput(String output, boolean detectLocalEcho)
   {
      if (detectLocalEcho && PASSWORD_PATTERN.test(output))
      {
         // If user is changing password, temporarily stop local-echo
         // to reduce chances of showing their password. Note that echo
         // stops automatically once the terminal enters "busy" state, but
         // there's a delay before that happens. Also, if the user has
         // already started typing before the Password: prompt is sent back
         // by the server, those characters will local-echo. Still, between
         // the two mechanisms this reduces the chances of most or all of
         // their typed password characters being echoed.
         localEcho_.pause(1000);
      }
      if (!detectLocalEcho || localEcho_.isEmpty())
      {
         xterm_.accept(output);
         return;
      }

      localEcho_.write(output);
   }

   @Override
   public void onTerminalDataInput(TerminalDataInputEvent event)
   {
      session_.receivedInput(event.getData());
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      session_.receivedOutput(event.getOutput());
   }

   private void addHandlerRegistration(HandlerRegistration reg)
   {
      registrations_.add(reg);
   }

   public void unregisterHandlers()
   {
      registrations_.removeHandler();
      if (terminalInputHandler_ != null)
      {
         terminalInputHandler_.removeHandler();
         terminalInputHandler_ = null;
      }
   }

   public void disconnect(boolean permanent)
   {
      diagnostic_.log(permanent ? constants_.permanentlyDisconnectedLabel() : constants_.disconnectedLabel());
      // Detach before invoking callbacks: cancellation may reconnect or queue
      // input for another connection, which must not be cancelled with this one.
      List<PendingInput> pending = new ArrayList<>(pendingInput_);
      pendingInput_.clear();
      connection_++;
      holdingInput_ = false;
      outstandingWrites_ = 0;
      cancelWriteWatchdog_.run();
      cancelWriteWatchdog_ = () -> {};
      connectCallback_ = null;
      closeWebSocket();
      registrations_.removeHandler();
      if (permanent)
      {
         unregisterHandlers(); // gets rid of keyboard handler
      }
      cancelPendingInput(pending, new DisconnectedError());
   }

   public void resetDiagnostics()
   {
      diagnostic_.resetLog();
      localEcho_.resetDiagnostics();
   }

   public String getConnectionDiagnostics()
   {
      return diagnostic_.getLog();
   }

   public String getLocalEchoDiagnostics()
   {
      return localEcho_.getDiagnostics();
   }

   private void diagnosticError(String msg)
   {
      Debug.log(msg);
      diagnostic_.log(msg);
   }

   private static String failureMessage(TerminalWebSocket.Failure failure)
   {
      switch (failure)
      {
      case CONNECT_TIMEOUT:
         return constants_.timeoutConnectingMessage();
      case KEEP_ALIVE_EXPIRED:
         return constants_.keepAliveExpiredMessage();
      case ERROR:
      default:
         return constants_.websocketConnectError();
      }
   }

   private void closeWebSocket()
   {
      TerminalWebSocket socket = socket_;
      socket_ = null;
      if (socket != null)
         socket.close();
   }

   private void connectionCompleted()
   {
      ConnectCallback callback = connectCallback_;
      connectCallback_ = null;
      if (callback != null)
         callback.onConnected();
   }

   // Held input (typed while the switch to RPC was in flight, or while a
   // write was in flight) is written as one request once nothing else is in
   // flight, and input typed while that request is in flight stays queued
   // behind it. Each write's completion calls back here, so the hold ends
   // once nothing is left to write.
   private void flushPendingInput(final int connection)
   {
      // Only this connection's writes are counted: the completion of a
      // previous connection's write may never arrive.
      if (outstandingWrites_ > 0)
         return;

      if (pendingInput_.isEmpty())
      {
         // A fallback after opening must not start the process and reload
         // its output a second time; connectionCompleted() only completes
         // an initial connection.
         holdingInput_ = false;
         connectionCompleted();
         return;
      }

      final List<PendingInput> pending = new ArrayList<>(pendingInput_);
      pendingInput_.clear();
      StringBuilder text = new StringBuilder();
      for (PendingInput input : pending)
         text.append(input.text);

      writeRpcInput(new PendingInput(text.toString(), new VoidServerRequestCallback()
      {
         @Override
         public void onResponseReceived(VoidResponse response)
         {
            for (PendingInput input : pending)
               input.callback.onResponseReceived(response);
         }

         @Override
         public void onError(ServerError error)
         {
            cancelPendingInput(pending, error);
         }
      }));
   }

   // Writes carry no sequence number. They go out one at a time, so the
   // server receives a connection's input in order without one, and a number
   // would not let it tell a late write of a previous connection from a live
   // one anyway.
   private void writeRpcInput(final PendingInput input)
   {
      final int connection = connection_;
      final boolean[] abandoned = { false };
      outstandingWrites_++;

      // RPC requests have no timeout of their own, so a write whose response
      // never arrives would hold all later input. Stop waiting for it after
      // a while; if it completes later, its callback still runs. Scheduled
      // before the write, which may complete synchronously.
      final Runnable cancelWatchdog = scheduler_.schedule(RPC_WRITE_TIMEOUT_MS, () ->
      {
         if (connection != connection_ || abandoned[0])
            return;

         abandoned[0] = true;
         outstandingWrites_--;
         diagnosticError(constants_.rpcWriteTimedOutMessage());
         if (holdingInput_)
            flushPendingInput(connection);
      });
      cancelWriteWatchdog_ = cancelWatchdog;

      consoleProcess_.writeStandardInput(
            ShellInput.create(input.text, true /*echo input*/),
            new VoidServerRequestCallback()
            {
               @Override
               public void onResponseReceived(VoidResponse response)
               {
                  cancelWatchdog.run();
                  if (connection == connection_ && !abandoned[0])
                     outstandingWrites_--;
                  input.callback.onResponseReceived(response);
                  if (connection == connection_ && holdingInput_)
                     flushPendingInput(connection);
               }

               @Override
               public void onError(ServerError error)
               {
                  cancelWatchdog.run();
                  if (connection == connection_ && !abandoned[0])
                     outstandingWrites_--;
                  input.callback.onError(error);
                  if (connection == connection_ && holdingInput_)
                     flushPendingInput(connection);
               }
            });
   }

   // Queued input is one burst of typing that was lost together, so report
   // its failure once: every callback would otherwise print its own error
   // line in the terminal. The remaining callbacks are never invoked.
   private static void cancelPendingInput(List<PendingInput> pending, ServerError error)
   {
      if (!pending.isEmpty())
         pending.get(0).callback.onError(error);
   }

   private static class PendingInput
   {
      PendingInput(String text, VoidServerRequestCallback callback)
      {
         this.text = text;
         this.callback = callback;
      }

      final String text;
      final VoidServerRequestCallback callback;
   }

   private static class DisconnectedError implements ServerError
   {
      @Override
      public int getCode()
      {
         return ServerError.CONNECTION;
      }

      @Override
      public String getMessage()
      {
         return constants_.disconnectedLabel();
      }

      @Override
      public String getUserMessage()
      {
         return getMessage();
      }

      @Override
      public String getRedirectUrl()
      {
         return null;
      }

      @Override
      public ServerErrorCause getCause()
      {
         return null;
      }

      @Override
      public JSONValue getClientInfo()
      {
         return null;
      }
   }

   private final HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final Session session_;
   private final Emulator xterm_;
   private final TerminalWebSocket.Factory socketFactory_;
   private final TerminalWebSocket.Scheduler scheduler_;
   private Process consoleProcess_;
   private ConnectCallback connectCallback_;
   private HandlerRegistration terminalInputHandler_;
   private TerminalWebSocket socket_;
   private int connection_;
   private boolean holdingInput_;
   private int outstandingWrites_;
   private Runnable cancelWriteWatchdog_ = () -> {};
   private final List<PendingInput> pendingInput_ = new ArrayList<>();
   private final TerminalLocalEcho localEcho_;
   private final TerminalDiagnostics diagnostic_ = new TerminalDiagnostics();

   // How long held input waits for the write ahead of it. Long enough that a
   // write abandoned at this point is very unlikely to be overtaken by the
   // next one; short enough that a lost request does not look like a dead
   // terminal.
   static final int RPC_WRITE_TIMEOUT_MS = 5000;

   // RegEx to match common password prompts
   private static final String PASSWORD_REGEX = "(?:password:)|(?:passphrase:)";

   public static final Pattern PASSWORD_PATTERN = Pattern.create(PASSWORD_REGEX, "im");

   private final int webSocketPingInterval_;
   private final int webSocketConnectTimeout_;
   private static final TerminalConstants constants_ = com.google.gwt.core.client.GWT.create(TerminalConstants.class);
}
