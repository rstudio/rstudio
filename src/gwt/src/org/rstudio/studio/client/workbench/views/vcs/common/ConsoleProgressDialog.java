/*
 * ConsoleProgressDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.common;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
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
   interface Resources extends ClientBundle
   {
      @Source("ConsoleProgressDialog.css")
      Styles styles();
   }

   interface Styles extends CssResource
   {
      String shellDisplay();
   }

   public static void ensureStylesInjected()
   {
      resources_.styles().ensureInjected();
   }

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

      consoleProcess_ = consoleProcess;

      display_ = new ConsoleProgressWidget();
      display_.addStyleName(resources_.styles().shellDisplay());
      Style style = display_.getElement().getStyle();
      double skewFactor = (12 + BrowseCap.getFontSkew()) / 12.0;
      int width = Math.min((int)(skewFactor * 660),
                            Window.getClientWidth() - 100);
      style.setWidth(width, Unit.PX);
      
      display_.setMaxOutputLines(getMaxOutputLines());
      display_.setSuppressPendingInput(true);
     
      setDisplayWidget(display_);
      
      if (getInteractionMode() != ConsoleProcessInfo.INTERACTION_NEVER)
      {
         ShellInteractionManager shellInteractionManager = 
               new ShellInteractionManager(display_, server, inputHandler_);
         
         if (getInteractionMode() != ConsoleProcessInfo.INTERACTION_ALWAYS)
            shellInteractionManager.setHistoryEnabled(false);
         
         outputWriter_ = shellInteractionManager;
      }
      else
      {
         display_.setReadOnly(true);
         outputWriter_ = display_;
      }

      stopButton().addClickHandler(this);
      
      if (!StringUtil.isNullOrEmpty(initialOutput))
      {
         outputWriter_.consoleWriteOutput(initialOutput);
      }

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
            if (consoleProcess_ != null)
               consoleProcess_.reap(new VoidServerRequestCallback());
         }
      });

      if (exitCode != null)
         setExitCode(exitCode);
     
      // interaction-always mode is a shell -- customize ui accordingly
      if (getInteractionMode() == ConsoleProcessInfo.INTERACTION_ALWAYS)
      {
         stopButton().setText("Close");
         
         hideProgress();
         
         int height = Window.getClientHeight() - 150;
         display_.getElement().getStyle().setHeight(height, Unit.PX);
      }

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

   @Override
   public void onConsoleOutput(ConsoleOutputEvent event)
   {
      maybeShowOnOutput(event.getOutput());
      outputWriter_.consoleWriteOutput(event.getOutput());
   }
   
   @Override
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      maybeShowOnOutput(event.getPrompt());
      outputWriter_.consoleWritePrompt(event.getPrompt());
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
   
   private final ConsoleProcess consoleProcess_;
 
   private final ShellOutputWriter outputWriter_;
   
   @UiField(provided = true)
   ConsoleProgressWidget display_;
   
 
   private static final Resources resources_ = GWT.<Resources>create(Resources.class);
}
