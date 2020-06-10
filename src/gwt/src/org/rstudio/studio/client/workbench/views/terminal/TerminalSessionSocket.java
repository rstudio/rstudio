/*
 * TerminalSessionSocket.java
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

import com.google.gwt.user.client.Timer;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.sksamuel.gwt.websockets.CloseEvent;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListenerExt;

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
    * Constructor
    * @param session Session to callback with user input and server output.
    * @param xterm Terminal emulator that provides user input, and displays output.
    * @param webSocketPingInterval (seconds) how often to send a keep-alive, or zero for none
    * @param webSocketConnectTimeout (seconds) how long to wait for websocket connection before
    *                                switching to RPC, or zero for no timeout (in seconds)
    */
   public TerminalSessionSocket(Session session,
                                XTermWidget xterm,
                                int webSocketPingInterval,
                                int webSocketConnectTimeout)
   {
      session_ = session;
      xterm_ = xterm;
      localEcho_ = new TerminalLocalEcho(xterm_);
      webSocketPingInterval_ = webSocketPingInterval;
      webSocketConnectTimeout_ = webSocketConnectTimeout;

      // Keep WebSocket connections alive by sending and receiving a small message
      keepAliveTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            if (socket_ != null)
            {
               socket_.send(TerminalSocketPacket.keepAlivePacket());
            }
            else
            {
               keepAliveTimer_.cancel();
            }
         }
      };

      // Underlying WebSocket object (JavaScript) can take up to 2 minutes to timeout
      // for certain issues with the server; shorten that via this timer
      connectWebSocketTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            diagnosticError("Timeout connecting via WebSockets, switching to RPC");
            switchToRPC();
         }
      };
   }

   /**
    * Connect the input/output channel to the server. This requires that
    * an rsession has already been started via RPC and the consoleProcess
    * received.
    * @param consoleProcess
    * @param callback result of connect attempt
    */
   public void connect(ConsoleProcess consoleProcess,
                       final ConnectCallback callback)
   {
      consoleProcess_ = consoleProcess;
      connectCallback_ = callback;

      if (consoleProcess.getProcessInfo().getZombie())
      {
         diagnostic_.log("Zombie, not reconnecting");
         callback.onConnected();
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
         diagnostic_.log("Connected with RPC");
         callback.onConnected();
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
               url = "wss:" + url.substring(6) + "p/" + urlSuffix;
            }
            else if (url.startsWith("http:"))
            {
               url = "ws:" + url.substring(5) + "p/" + urlSuffix;
            }
            else
            {
               callback.onError("Unable to discover websocket protocol");
               return;
            }
         }

         diagnostic_.log("Connect WebSocket: '" + url + "'");
         socket_ = new Websocket(url);
         socket_.addListener(new WebsocketListenerExt()
         {
            @Override
            public void onClose(CloseEvent event)
            {
               diagnostic_.log("WebSocket closed");
               if (socket_ != null)
               {
                  // if socket is already null then we're probably in the middle of switching to RPC
                  // and don't want to kill the terminal in the middle of doing so
                  socket_ = null;
                  keepAliveTimer_.cancel();
                  connectWebSocketTimer_.cancel();
                  session_.connectionDisconnected();
               }
            }

            @Override
            public void onMessage(String msg)
            {
               if (TerminalSocketPacket.isKeepAlive(msg))
               {
                  receivedKeepAlive();
               }
               else
               {
                  onConsoleOutput(new ConsoleOutputEvent(TerminalSocketPacket.getMessage(msg)));
               }
            }

            @Override
            public void onOpen()
            {
               connectWebSocketTimer_.cancel();
               diagnostic_.log("WebSocket connected");
               callback.onConnected();
               if (webSocketPingInterval_ > 0)
               {
                  keepAliveTimer_.scheduleRepeating(webSocketPingInterval_ * 1000);
               }
            }

            @Override
            public void onError()
            {
               connectWebSocketTimer_.cancel();
               diagnosticError("WebSocket connect error, switching to RPC");
               switchToRPC();
            }
         });

         if (webSocketConnectTimeout_ > 0)
         {
            connectWebSocketTimer_.schedule(webSocketConnectTimeout_ * 1000);
         }
         socket_.open();
         break;

      case ConsoleProcessInfo.CHANNEL_PIPE:
      default:
         callback.onError("Channel type not implemented");
         break;
      }
   }

   private void switchToRPC()
   {
      socket_ = null;
      keepAliveTimer_.cancel();
      connectWebSocketTimer_.cancel();

      // Unable to connect client to server via websocket; let server
      // know we'll be using rpc, instead
      consoleProcess_.useRpcMode(new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void response)
         {
            diagnostic_.log("Switched to RPC");
            connectCallback_.onConnected();
         }

         @Override
         public void onError(ServerError error)
         {
            diagnostic_.log("Failed to switch to RPC: " + error.getMessage());
            connectCallback_.onError("Terminal failed to connect. Please try again.");
         }
      });
   }

   /**
    * Send user input to the server.
    * @param inputSequence used to fix out-of-order RPC calls
    * @param input text to send
    * @param localEcho echo input locally
    * @param requestCallback callback
    */
   public void dispatchInput(int inputSequence,
                             String input,
                             boolean localEcho,
                             VoidServerRequestCallback requestCallback)
   {
      if (localEcho)
         localEcho_.echo(input);
      else
         localEcho_.clear();

      switch (consoleProcess_.getChannelMode())
      {
      case ConsoleProcessInfo.CHANNEL_RPC:
         consoleProcess_.writeStandardInput(
               ShellInput.create(inputSequence, input,  true /*echo input*/),
               requestCallback);
         break;
      case ConsoleProcessInfo.CHANNEL_WEBSOCKET:
         if (socket_ != null)
         {
            socket_.send(TerminalSocketPacket.textPacket(input));
         }
         else
            diagnosticError("Tried to send user input over null websocket");

         requestCallback.onResponseReceived(null);
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
      diagnostic_.log(permanent ? "Permanently Disconnected" : "Disconnected");
      if (socket_ != null)
         socket_.close();
      socket_ = null;
      registrations_.removeHandler();
      if (permanent)
      {
         unregisterHandlers(); // gets rid of keyboard handler
      }
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

   private void receivedKeepAlive()
   {
   }

   private final HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final Session session_;
   private final XTermWidget xterm_;
   private ConsoleProcess consoleProcess_;
   private ConnectCallback connectCallback_;
   private HandlerRegistration terminalInputHandler_;
   private Websocket socket_;
   private final TerminalLocalEcho localEcho_;
   private final TerminalDiagnostics diagnostic_ = new TerminalDiagnostics();

   // RegEx to match common password prompts
   private static final String PASSWORD_REGEX = "(?:password:)|(?:passphrase:)";

   public static final Pattern PASSWORD_PATTERN = Pattern.create(PASSWORD_REGEX, "im");

   private final Timer keepAliveTimer_;
   private final int webSocketPingInterval_;
   private final Timer connectWebSocketTimer_;
   private final int webSocketConnectTimeout_;
}
