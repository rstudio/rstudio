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
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

/**
 * Manages input and output for the terminal session.
 * TODO Use a websocket to communicate with the server, falling back to RPC 
 * if unable to establish a websocket connection.
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
            if (input != null && input_.equals(input))
            {
               duration_ = stopWatch_.mark("Average " + runningAverage + 
                     " [" + input + "]");
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
    * @param callback returns true if using websockets, false for RPC
    */
   public void connect(ConsoleProcess consoleProcess, 
                       ServerRequestCallback<Boolean> callback)
   {
      consoleProcess_ = consoleProcess;
      
      // We keep this handler connected after a disconnect so
      // user input sent via RPC can wake up a suspended session
      if (terminalInputHandler_ == null)
         terminalInputHandler_ = xterm_.addTerminalDataInputHandler(this);

      addHandlerRegistration(consoleProcess_.addConsoleOutputHandler(this));
      
      // TODO (gary) attempt websocket connection here
      boolean haveWebsocket = false;
      
      callback.onResponseReceived(haveWebsocket);
   }
   
   /**
    * Send user input to the server.
    * @param inputSequence used to fix out-of-order RPC calls
    * @param input text to send
    * @param requestCallback callback
    */
   public void dispatchInput(int inputSequence,
                             String input,
                             VoidServerRequestCallback requestCallback)
   {
      // TODO (gary) write to websocket here
      
      consoleProcess_.writeStandardInput(
            ShellInput.create(inputSequence, input,  true /*echo input*/), 
            requestCallback);
   }
   
   /**
    * Send output to the terminal emulator.
    * @param output text to send to the terminal
    */
   public void dispatchOutput(String output)
   {
      xterm_.write(output);
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
      consoleProcess_ = null;
      registrations_.removeHandler();
   }
 
   private HandlerRegistrations registrations_ = new HandlerRegistrations();
   private final Session session_;
   private final XTermWidget xterm_;
   private ConsoleProcess consoleProcess_;
   private HandlerRegistration terminalInputHandler_;
   private boolean reportTypingLag_;
   private InputEchoTimeMonitor inputEchoTiming_;
   
   // Injected ---- 
   private UIPrefs uiPrefs_;
}
