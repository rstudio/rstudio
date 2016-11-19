/*
 * TerminalSessionsPanel.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.common.shell.ShellSecureInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.views.terminal.events.ResizeTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.DeckLayoutPanel;

/**
 * A collection of zero or more Terminal sessions; only one is visible at a
 * time.
 */
public class TerminalSessionsPanel extends DeckLayoutPanel
                                   implements ConsoleOutputEvent.Handler, 
                                              ProcessExitEvent.Handler,
                                              ClickHandler,
                                              ResizeTerminalEvent.Handler,
                                              TerminalDataInputEvent.Handler
{
   public TerminalSessionsPanel(WorkbenchServerOperations server)
   {
      server_ = server;
      secureInput_ = new ShellSecureInput(server_); 
   }
   
//   @Override
//   protected Widget createMainWidget()
//   {
//      XTermWidget.load(new Command()
//      {
//         @Override
//         public void execute()
//         {
//            // TODO (gary) move this somewhere!
////            xterm_ = new XTermWidget();
////            xterm_.setHeight("100%");
////            host_.add(xterm_);
////            connectToTerminalProcess();
//         }
//      });
//      return terminalSessionsHost_;
//   }
   /**
    * Create a terminal process and connect to it.
    */
   private void connectToTerminalProcess()
   {
      server_.startShellDialog(ConsoleProcess.TerminalType.XTERM, 
                               80, 25,
                               false, /* not a modal dialog */
                               new ServerRequestCallback<ConsoleProcess>()
      {
         @Override
         public void onResponseReceived(ConsoleProcess consoleProcess)
         {
            consoleProcess_ = consoleProcess;
            
            if (getInteractionMode() != ConsoleProcessInfo.INTERACTION_ALWAYS)
            {
               // TODO (gary) add capability to display error messages in the terminal tab to
               // show fatal errors such as this one, and possibly async "loading..." style
               // message.
               throw new IllegalArgumentException("Unsupport ConsoleProcess interaction mode");
            } 

            if (consoleProcess_ != null)
            {
               addHandlerRegistration(consoleProcess_.addConsoleOutputHandler(TerminalSessionsPanel.this));
               addHandlerRegistration(consoleProcess_.addProcessExitHandler(TerminalSessionsPanel.this));
               addHandlerRegistration(xterm_.addResizeTerminalHandler(TerminalSessionsPanel.this));
               addHandlerRegistration(xterm_.addTerminalDataInputHandler(TerminalSessionsPanel.this));

               consoleProcess.start(new SimpleRequestCallback<Void>()
               {
                  @Override
                  public void onError(ServerError error)
                  {
                     // Show error and stop
                     super.onError(error);

                     // TODO (gary) show fatal errors in the terminal tab UI
                  }
               });
            }
         }
      
         @Override
         public void onError(ServerError error)
         {
            xterm_.writeln(error.getUserMessage());
         }
         
      });
   }

   private int getInteractionMode()
   {
      if (consoleProcess_ != null)
         return consoleProcess_.getProcessInfo().getInteractionMode();
      else
         return ConsoleProcessInfo.INTERACTION_NEVER;
   } 

   @Override
   public void onClick(ClickEvent event)
   {
      // TODO (gary) implement
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      xterm_.write(event.getOutput());
   }
   
   @Override
   public void onProcessExit(ProcessExitEvent event)
   {
      unregisterHandlers();

      if (consoleProcess_ != null)
         consoleProcess_.reap(new VoidServerRequestCallback());
   }

   protected void addHandlerRegistration(HandlerRegistration reg)
   {
      registrations_.add(reg);
   }
   
   protected void unregisterHandlers()
   {
      registrations_.removeHandler();
   } 
   
   @Override
   public void onResizeTerminal(ResizeTerminalEvent event)
   {
      consoleProcess_.resizeTerminal(
            event.getCols(), event.getRows(),
            new VoidServerRequestCallback() 
            {
               @Override
               public void onError(ServerError error)
               {
                  xterm_.writeln(error.getUserMessage());
               }
            });
   }
   
   @Override
   public void onTerminalDataInput(TerminalDataInputEvent event)
   {
      secureInput_.secureString(event.getData(), new CommandWithArg<String>() 
      {
         @Override
         public void execute(String arg) // success
         {
            consoleProcess_.writeStandardInput(
               ShellInput.create(arg,  true /* echo input*/), 
               new VoidServerRequestCallback() {
                  @Override
                  public void onError(ServerError error)
                  {
                     xterm_.writeln(error.getUserMessage());
                  }
               });
          }
      },
      new CommandWithArg<String>()
      {
         @Override
         public void execute(String errorMessage) // failure
         {
            xterm_.writeln(errorMessage); 
         }
      });
   }
   
   private XTermWidget xterm_;
   private WorkbenchServerOperations server_;
   private final ShellSecureInput secureInput_;
   private ConsoleProcess consoleProcess_;
   private HandlerRegistrations registrations_ = new HandlerRegistrations();
}
