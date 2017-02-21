/*
 * ConsoleProgressDialog.java
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsolePromptEvent;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.common.shell.ShellInteractionManager;
import org.rstudio.studio.client.common.shell.ShellOutputWriter;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

public class ConsoleProgressDialog extends ProgressDialog
                                   implements ConsoleOutputEvent.Handler, 
                                              ConsolePromptEvent.Handler,
                                              ProcessExitEvent.Handler,
                                              ClickHandler
{
   public ConsoleProgressDialog(ConsoleProcess consoleProcess,
                                CryptoServerOperations server)
   {
      this(consoleProcess.getProcessInfo().getCaption(), 
           consoleProcess, 
           "", 
           null, 
           server);
   }

   public ConsoleProgressDialog(String title, 
                                String output, 
                                int exitCode)
   {
      this(title, null, output, exitCode, null);
   }

   public ConsoleProgressDialog(String title,
                                ConsoleProcess consoleProcess,
                                String initialOutput,
                                Integer exitCode,
                                CryptoServerOperations server)
   {
      super(title);
      
      if (consoleProcess == null && exitCode == null)
      {
         throw new IllegalArgumentException(
               "Invalid combination of arguments to ConsoleProgressDialog");
      }
      
      if (getInteractionMode() != ConsoleProcessInfo.INTERACTION_NEVER)
      {
         ShellInteractionManager shellInteractionManager = 
               new ShellInteractionManager(display_, inputHandler_);
         
         if (getInteractionMode() != ConsoleProcessInfo.INTERACTION_ALWAYS)
            shellInteractionManager.setHistoryEnabled(false);
         
         outputWriter_ = shellInteractionManager;
      }
      else
      {
         display_.setReadOnly(true);
         outputWriter_ = display_;
      }
      
      if (!StringUtil.isNullOrEmpty(initialOutput))
      {
         outputWriter_.consoleWriteOutput(initialOutput);
      }

      if (exitCode != null)
         setExitCode(exitCode);
     
      attachToProcess(consoleProcess);
   }
   
   @Override
   protected Widget createDisplayWidget(Object param)
   {
      display_ = new ConsoleProgressWidget();
      display_.setMaxOutputLines(getMaxOutputLines());
      display_.setSuppressPendingInput(true);
      return display_.getShellWidget();
   }
   
   public void attachToProcess(final ConsoleProcess consoleProcess)
   {
      consoleProcess_ = consoleProcess;
      
      // interaction-always mode is a shell -- customize ui accordingly
      if (getInteractionMode() == ConsoleProcessInfo.INTERACTION_ALWAYS)
      {
         hideProgress();
         stopButton().setText("Close");
         
         int height = Window.getClientHeight() - 150;
         int width = Math.min(800, Window.getClientWidth() - 150);
         Style style = display_.getShellWidget().getElement().getStyle();
         style.setHeight(height, Unit.PX);
         style.setWidth(width, Unit.PX);
      }
      else
      {
         showProgress();
         stopButton().setText("Stop");
      }
      
      stopButton().addClickHandler(this);
      
      if (consoleProcess != null)
      {
         addHandlerRegistration(consoleProcess.addConsolePromptHandler(this));
         addHandlerRegistration(consoleProcess.addConsoleOutputHandler(this));
         addHandlerRegistration(consoleProcess.addProcessExitHandler(this));

         consoleProcess.start(new SimpleRequestCallback<Void>()
         {
            @Override
            public void onError(ServerError error)
            {
               // Show error and stop
               super.onError(error);
               
               // if this is showOnOutput_ then we will never get
               // a ProcessExitEvent or an onUnload so we should unsubscribe 
               // from events here
               unregisterHandlers();
               
               closeDialog();
            }
         });
      }

      addCloseHandler(new CloseHandler<PopupPanel>()
      {
         @Override
         public void onClose(CloseEvent<PopupPanel> popupPanelCloseEvent)
         {
            if (consoleProcess != null)
               consoleProcess.reap(new VoidServerRequestCallback());
         }
      });
   }

   public ConsoleProcess getConsoleProcess()
   {
      return consoleProcess_;
   }
   
   public void showOnOutput()
   {
      showOnOutput_ = true;
   }
   
   @Override 
   protected boolean handleEnterKey()
   {
      if (!running_)
      {
         stopButton().click();
         return true;
      }
      else
      {
         return false;
      }
   }
   
   public void writeOutput(String output)
   {
      maybeShowOnOutput(output);
      outputWriter_.consoleWriteOutput(output);
   }
   
   public void writePrompt(String prompt)
   {
      maybeShowOnOutput(prompt);
      outputWriter_.consoleWritePrompt(prompt);
   }

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      writeOutput(event.getOutput());
   }
   
   @Override
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      writePrompt(event.getPrompt());
   }

   @Override
   public void onProcessExit(ProcessExitEvent event)
   {    
      setExitCode(event.getExitCode());
      
      if (isShowing())
      {
         display_.setReadOnly(true);
         stopButton().setFocus(true);
      
         // when a shell exits we close the dialog
         if (getInteractionMode() == ConsoleProcessInfo.INTERACTION_ALWAYS)
            stopButton().click();
         
         // when we were showOnOutput and the process succeeded then
         // we also auto-close
         else if (showOnOutput_ && (event.getExitCode() == 0))
            stopButton().click();
      }
      
      // the dialog was showOnOutput_ but was never shown so just tear
      // down registrations and reap the process
      else if (showOnOutput_)
      {
         unregisterHandlers();
         
         if (consoleProcess_ != null)
            consoleProcess_.reap(new VoidServerRequestCallback());
      }
      
   }
   
   private void setExitCode(int exitCode)
   {
      running_ = false;
      stopButton().setText("Close");
      stopButton().setDefault(true);
      hideProgress();
   }

   @Override
   public void onClick(ClickEvent event)
   {
      if (running_)
      {
         consoleProcess_.interrupt(new SimpleRequestCallback<Void>() {
            @Override
            public void onResponseReceived(Void response)
            {
               closeDialog();
            }

            @Override
            public void onError(ServerError error)
            {
               stopButton().setEnabled(true);
               super.onError(error);
            }
         });
         stopButton().setEnabled(false);
      }
      else
      {
         closeDialog();
      }

      // Whether success or failure, we don't want to interrupt again
      running_ = false;
   }
   
   private CommandWithArg<ShellInput> inputHandler_ = 
                                          new CommandWithArg<ShellInput>() 
   {
      @Override
      public void execute(ShellInput input)
      {         
         consoleProcess_.writeStandardInput(
            input, 
            new VoidServerRequestCallback() {
               @Override
               public void onError(ServerError error)
               {
                  outputWriter_.consoleWriteError(error.getUserMessage());
               }
            });
      }

   };
   
  
   private int getInteractionMode()
   {
      if (consoleProcess_ != null)
         return consoleProcess_.getProcessInfo().getInteractionMode();
      else
         return ConsoleProcessInfo.INTERACTION_NEVER;
   }
   
   private int getMaxOutputLines()
   {
      if (consoleProcess_ != null)
         return consoleProcess_.getProcessInfo().getMaxOutputLines();
      else
         return 1000;
   }
   
   private void maybeShowOnOutput(String output)
   {    
      // NOTE: we have to trim the output because when the password
      // manager provides a password non-interactively the back-end
      // process sometimes echos a newline back to us
      if (!isShowing() && showOnOutput_ && (output.trim().length() > 0))
         showModal();
   }

   private boolean running_ = true;
   
   private boolean showOnOutput_ = false;
   
   private ConsoleProcess consoleProcess_;
 
   private final ShellOutputWriter outputWriter_;
  
   private ShellDisplay display_;
}
