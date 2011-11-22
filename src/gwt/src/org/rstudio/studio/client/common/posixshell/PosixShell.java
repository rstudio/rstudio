/*
 * PosixShell.java
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
package org.rstudio.studio.client.common.posixshell;

import java.util.ArrayList;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.crypto.PublicKeyInfo;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.common.posixshell.events.PosixShellExitEvent;
import org.rstudio.studio.client.common.posixshell.events.PosixShellOutputEvent;
import org.rstudio.studio.client.common.posixshell.model.PosixShellServerOperations;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PosixShell implements PosixShellOutputEvent.Handler,
                                   PosixShellExitEvent.Handler
 
{
   public interface Display extends ShellDisplay
   { 
   }
   
   public interface Observer 
   {
      void onShellTerminated();
   }
   
  
   @Inject
   public PosixShell(Display display,
                     GlobalDisplay globalDisplay,
                     EventBus eventBus,
                     PosixShellServerOperations server)
   {
      // save references
      display_ = display;
      globalDisplay_ = globalDisplay;
      server_ = server;
      
      // set max lines
      int maxLines = 1000;
      display_.setMaxOutputLines(maxLines);
   
      // subscribe to display events
      display_.addCapturingKeyDownHandler(new InputKeyDownHandler());
      
      // hookup input editor display and command line history
      input_ = display_.getInputEditorDisplay();
      historyManager_ = new CommandLineHistory(input_);
      
      // subscribe to server events
      eventBusHandlers_.add(
            eventBus.addHandler(PosixShellOutputEvent.TYPE, this));
      eventBusHandlers_.add(
            eventBus.addHandler(PosixShellExitEvent.TYPE, this));
   }
   
   
   public Widget getWidget()
   {
      return display_.getShellWidget();
   }
   
   public void start(int width, 
                     final Observer observer, 
                     final ProgressIndicator progressIndicator)
   {
      observer_ = observer;
      
      progressIndicator.onProgress("Starting shell...");
      
      server_.startPosixShell(
            width,
            display_.getMaxOutputLines(), 
            new ServerRequestCallback<PublicKeyInfo>()
            {
               @Override 
               public void onResponseReceived(PublicKeyInfo publicKeyInfo)
               {
                  publicKeyInfo_ = publicKeyInfo;
                  progressIndicator.onCompleted();
               }

               @Override
               public void onError(ServerError error)
               {
                  progressIndicator.onCompleted();
                  
                  globalDisplay_.showErrorMessage(
                        "Error Starting Shell",
                        error.getUserMessage(),
                        new Operation() {
                           @Override
                           public void execute()
                           {
                              if (observer_ != null)
                                 observer_.onShellTerminated();
                           }
                           
                        });
               }

            });
   }
   
   public void terminate()
   {
      // detach our event bus handlers so we don't get the exited event
      detachEventBusHandlers();
      
      // terminate the shell
      server_.terminatePosixShell(new VoidServerRequestCallback());
   }
   
   @Override
   public void onPosixShellOutput(PosixShellOutputEvent event)
   {
      // if the output ends with a newline then just send it all
      // in a big batch
      String output = event.getOutput();
      if (output.endsWith("\n"))
      {
         display_.consoleWriteOutput(output);
      }
      else
      {
         // look for the last newline and take the content after
         // that as the prompt
         int lastLoc = output.lastIndexOf('\n');
         if (lastLoc != -1)
         {
            display_.consoleWriteOutput(output.substring(0, lastLoc));
            maybeConsolePrompt(output.substring(lastLoc + 1));
         }
         else
         {
            maybeConsolePrompt(output);
         }
      }
   }
   
   @Override
   public void onPosixShellExit(PosixShellExitEvent event)
   {
      detachEventBusHandlers();
      
      if (observer_ != null)
         observer_.onShellTerminated();    
   }

   public void detachEventBusHandlers()
   {
      for (int i=0; i<eventBusHandlers_.size(); i++)
         eventBusHandlers_.get(i).removeHandler();
      eventBusHandlers_.clear();
   }
   
   // prompt as long as there are no special control characters
   // (otherwise treat it as output)
   private void maybeConsolePrompt(String output)
   {
      if (CONTROL_SPECIAL.match(output, 0) == null)
         consolePrompt(output, true);
      else
         display_.consoleWriteOutput(output);
   }
    
   private void consolePrompt(String prompt, boolean addToHistory)
   {
      display_.consolePrompt(prompt) ;

      addToHistory_ = addToHistory;
      historyManager_.resetPosition();
      lastPromptText_ = prompt ;
   }
   
   private void navigateHistory(int offset)
   {
      historyManager_.navigateHistory(offset);
      display_.ensureInputVisible();
   }
   
   private final class InputKeyDownHandler implements KeyDownHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
         int keyCode = event.getNativeKeyCode();
         int modifiers = KeyboardShortcut.getModifierValue(
                                                event.getNativeEvent());
         
         if (event.isUpArrow() && modifiers == 0)
         {
            if (input_.getCurrentLineNum() == 0)
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(-1);
            }
         }
         else if (event.isDownArrow() && modifiers == 0)
         {
            if (input_.getCurrentLineNum() == input_.getCurrentLineCount() - 1)
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(1);
            }
         }
         else if (keyCode == KeyCodes.KEY_ENTER && modifiers == 0)
         {
            event.preventDefault();
            event.stopPropagation();

            processCommandEntry();
         }
         else if (modifiers == KeyboardShortcut.CTRL && keyCode == 'C')
         {
            event.preventDefault();
            event.stopPropagation();
         
            if (display_.isPromptEmpty())
               display_.consoleWriteOutput("^C");
            
            server_.interruptPosixShell(new VoidServerRequestCallback());
         }
         else if (modifiers == KeyboardShortcut.CTRL && keyCode == 'L')
         {
            event.preventDefault();
            event.stopPropagation();
           
            display_.clearOutput();
         }
      }
   }
   
   private void processCommandEntry()
   {
      // get the current prompt text
      String promptText = display_.getPromptText();
      
      // process command entry 
      String commandEntry = display_.processCommandEntry();
      if (addToHistory_)
         historyManager_.addToHistory(commandEntry);
      
      // input is entry + newline
      String input = commandEntry + "\n";
      
      // update console with prompt and input
      display_.consoleWritePrompt(promptText);
      display_.consoleWriteInput(input);
      
      // encrypt input and send it to the to server
      RSAEncrypt.encrypt_ServerOnly(
         publicKeyInfo_, 
         input, 
         new CommandWithArg<String>() {
            @Override
            public void execute(String encryptedInput)
            {
               server_.sendInputToPosixShell(
                  encryptedInput, 
                  new ServerRequestCallback<Void>() {
                     @Override
                     public void onError(ServerError error)
                     {
                        // show the error in the console then re-prompt
                        display_.consoleWriteError(
                              "Error: " + error.getUserMessage() + "\n");
                        if (lastPromptText_ != null)
                           consolePrompt(lastPromptText_, false);
                     }
                  });
            }
       });
   }
   
   private final Display display_;
   private final GlobalDisplay globalDisplay_;
   private Observer observer_ = null;
   private final PosixShellServerOperations server_;
   private PublicKeyInfo publicKeyInfo_;
 
   // indicates whether the next command should be added to history
   private boolean addToHistory_ ;
   private String lastPromptText_ ;

   private final InputEditorDisplay input_ ;
   private final CommandLineHistory historyManager_;
   
   private ArrayList<HandlerRegistration> eventBusHandlers_ = 
                                    new ArrayList<HandlerRegistration>();
   
   private static final Pattern CONTROL_SPECIAL = Pattern.create("[\r\b]");
}
