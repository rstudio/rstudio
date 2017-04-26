/*
 * TerminalSessionSocket.java
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

import java.util.LinkedList;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.Stopwatch;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;
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
   }
   
   public interface ConnectCallback
   {
      void onConnected();
      void onError(String message);
   }
   
   // Monitor and report input/display lag to console
   class InputEchoTimeMonitor
   {
      class InputDatapoint
      {
         InputDatapoint(String input)
         {
            input_ = input;
            stopWatch_.reset();
         }

         boolean matches(String input, long runningAverage)
         {
            // startsWith allows better chance of matching on Windows, where 
            // winpty often follows each typed character with an escape sequence
            if (input != null && input.startsWith(input_))
            {
               duration_ = stopWatch_.mark("Average " + runningAverage);
               return true;
            }
            return false;
         }
         
         long duration()
         {
            return duration_;
         }
         
         private String input_;
         private Stopwatch stopWatch_ = new Stopwatch();
         private long duration_;
      }
      
      public InputEchoTimeMonitor()
      {
         pending_ = new LinkedList<InputDatapoint>();
      }
      
      public void inputReceived(String input)
      {
         pending_.add(new InputDatapoint(input));
      }
      
      public void outputReceived(String output)
      {
         InputDatapoint item = pending_.poll();
         if (item == null)
            return;
         
         long average = 0;
         if (accumulatedPoints_ > 0)
         {
            average = accumulatedTime_ / accumulatedPoints_;
         }
         if (!item.matches(output, average))
         {
            // output not what we expected, reset the whole list
            pending_.clear();
         }
         else
         {
            accumulatedPoints_++;
            accumulatedTime_ += item.duration();
         }
      }
      
      private LinkedList<InputDatapoint> pending_;
      private long accumulatedPoints_;
      private long accumulatedTime_;
   }
   
   /**
    * Constructor
    * @param session Session to callback with user input and server output.
    * @param xterm Terminal emulator that provides user input, and displays output.
    */
   public TerminalSessionSocket(Session session,
                                XTermWidget xterm)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      session_ = session;
      xterm_ = xterm;
      localEcho_ = new TerminalLocalEcho(xterm_);

      // Show delay between receiving a keystroke and sending it to the 
      // terminal emulator; for diagnostics on laggy typing. Intended for
      // brief use from a command-prompt. Time between input/display shown
      // in console.
      reportTypingLag_ = uiPrefs_.enableReportTerminalLag().getValue();
      if (reportTypingLag_)
      {
         inputEchoTiming_ = new InputEchoTimeMonitor();
      }
   }

   @Inject
   private void initialize(UIPrefs uiPrefs)
   {
      uiPrefs_ = uiPrefs;
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

      // We keep this handler connected after a disconnect so
      // user input sent via RPC can wake up a suspended session
      if (terminalInputHandler_ == null)
         terminalInputHandler_ = xterm_.addTerminalDataInputHandler(this);

      addHandlerRegistration(consoleProcess_.addConsoleOutputHandler(this));

      switch (consoleProcess_.getChannelMode())
      {
      case ConsoleProcessInfo.CHANNEL_RPC:
         diagnostic("Connected with RPC");
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

         diagnostic("Connect WebSocket: '" + url + "'");
         socket_ = new Websocket(url);
         socket_.addListener(new WebsocketListenerExt() 
         {
            @Override
            public void onClose(CloseEvent event)
            {
               diagnostic("WebSocket closed");
               socket_ = null;
            }

            @Override
            public void onMessage(String msg)
            {
               onConsoleOutput(new ConsoleOutputEvent(msg));
            }

            @Override
            public void onOpen()
            {
               diagnostic("WebSocket connected");
               callback.onConnected();
            }

            @Override
            public void onError()
            {
               diagnostic("WebSocket connect error, switching to RPC");
               socket_ = null;
               
               // Unable to connect client to server via websocket; let server
               // know we'll be using rpc, instead
               consoleProcess_.useRpcMode(new ServerRequestCallback<Void>()
               {
                  @Override
                  public void onResponseReceived(Void response)
                  {
                     diagnostic("Switched to RPC");
                     callback.onConnected();
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     callback.onError("Unable to switch back to RPC mode");
                  }
               });
               return;
            }
         });
         
         socket_.open();
         break;
         
      case ConsoleProcessInfo.CHANNEL_PIPE:
      default:
         callback.onError("Channel type not implemented");
         break;
      }
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
         socket_.send(input);
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
         xterm_.write(output);
         return;
      }
      
      localEcho_.write(output);
   }
   
   @Override
   public void onTerminalDataInput(TerminalDataInputEvent event)
   {
      if (reportTypingLag_)
         inputEchoTiming_.inputReceived(event.getData());
      session_.receivedInput(event.getData());
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      if (reportTypingLag_)
         inputEchoTiming_.outputReceived(event.getOutput());
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

   public void disconnect()
   {
      diagnostic("Disconnected");
      socket_.close();
      socket_ = null;
      registrations_.removeHandler();
   }
   
   public void resetDiagnostics()
   {
      diagnostic_ = null;
      localEcho_.resetDiagnostics();
   }
   
   public String getConnectionDiagnostics()
   {
      if (diagnostic_ == null || diagnostic_.length() == 0)
         return("<none>\n");
      else
         return diagnostic_.toString();
   }
   
   public String getLocalEchoDiagnostics()
   {
      return localEcho_.getDiagnostics();
   }
   
   private void diagnostic(String msg)
   {
      if (diagnostic_ == null)
         diagnostic_ = new StringBuilder();
     
      diagnostic_.append(StringUtil.getTimestamp());
      diagnostic_.append(": ");
      diagnostic_.append(msg);
      diagnostic_.append("\n");
   }
 
   private HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final Session session_;
   private final XTermWidget xterm_;
   private ConsoleProcess consoleProcess_;
   private HandlerRegistration terminalInputHandler_;
   private boolean reportTypingLag_;
   private InputEchoTimeMonitor inputEchoTiming_;
   private Websocket socket_;
   private TerminalLocalEcho localEcho_;
   private StringBuilder diagnostic_;
   
   // RegEx to match common password prompts
   private static final String PASSWORD_REGEX = 
         "(?:password:)|(?:passphrase:)";
   
   public static final Pattern PASSWORD_PATTERN =
         Pattern.create(PASSWORD_REGEX, "im");
 
   // Injected ---- 
   private UIPrefs uiPrefs_;
}
