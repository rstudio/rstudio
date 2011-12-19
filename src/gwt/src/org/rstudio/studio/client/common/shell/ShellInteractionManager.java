/*
 * ShellInteractionManager.java
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
package org.rstudio.studio.client.common.shell;


import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations;
import org.rstudio.studio.client.common.crypto.PublicKeyInfo;
import org.rstudio.studio.client.common.crypto.RSAEncrypt;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;


public class ShellInteractionManager implements ShellOutputWriter
{
   public ShellInteractionManager(ShellDisplay display,
                                  CryptoServerOperations server,
                                  CommandWithArg<ShellInput> inputHandler)
   {
      display_ = display;
      server_ = server;
      input_ = display_.getInputEditorDisplay();
      historyManager_ = new CommandLineHistory(input_);
      inputHandler_ = inputHandler;
      
      display_.addCapturingKeyDownHandler(new InputKeyDownHandler());
   }
   
   public void setHistoryEnabled(boolean enabled)
   {
      historyEnabled_ = enabled;
   }
   
   public void setNoEchoForColonPrompts(boolean noEchoForColonPrompts)
   {
      noEchoForColonPrompts_ = noEchoForColonPrompts;
   }
  
   @Override
   public void consoleWriteOutput(String output)
   {
      display_.consoleWriteOutput(output); 
   }
      
   @Override
   public void consoleWriteError(String error)
   {
      // show the error in the console then re-prompt
      display_.consoleWriteError(
            "Error: " + error + "\n");
      if (lastPromptText_ != null)
         consolePrompt(lastPromptText_, false);
   }
   
   @Override
   public void consoleWritePrompt(String prompt)
   {
      consolePrompt(prompt);
   }
   
   private void processInput(final CommandWithArg<ShellInput> onInputReady)
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
      final boolean echoInput = showInputForPrompt(promptText);
      if (echoInput)
         display_.consoleWriteInput(input);
      else if (!Desktop.isDesktop() || !BrowseCap.isWindows())
         display_.consoleWriteInput("\n");
      
      // encrypt the input and return it
      encryptInput(input, new CommandWithArg<String>() {

         @Override
         public void execute(String arg)
         {
            onInputReady.execute(ShellInput.create(arg, echoInput));
         }
      });
   }
 
   private void navigateHistory(int offset)
   {
      historyManager_.navigateHistory(offset);
      display_.ensureInputVisible();
   }
    
   private void consolePrompt(String prompt)
   {
      // determine whether we should add this to the history
      boolean addToHistory = false;
     
      if (historyEnabled_)
      {
         // figure out what the suffix of the default prompt is by inspecting
         // the first prompt which comes our way
         if (defaultPromptSuffix_ == null)
         {
            if (prompt.length() > 1)
               defaultPromptSuffix_ = prompt.substring(prompt.length()-2);
            else if (prompt.length() > 0)
               defaultPromptSuffix_ = prompt;
            
            addToHistory = true;
         }
         else if (prompt.endsWith(defaultPromptSuffix_))
         {
            addToHistory = true;
         }
      }
      
      consolePrompt(prompt, addToHistory);
   }
    
   private void consolePrompt(String prompt, boolean addToHistory)
   {
      boolean showInput = showInputForPrompt(prompt);
      display_.consolePrompt(prompt, showInput) ;

      addToHistory_ = addToHistory && showInput;
      historyManager_.resetPosition();
      lastPromptText_ = prompt ;
      
      // set focus on the first prompt
      if (!firstPromptShown_)
      {
         firstPromptShown_ = true;
         display_.getInputEditorDisplay().setFocus(true);
      }
   }
   
   private boolean showInputForPrompt(String prompt)
   {
      // always filter out english password prompts
      if (!isNotEnglishPasswordPrompt(prompt))
         return false;
      
      // optionally implement further restrictions
      else if (noEchoForColonPrompts_ && prompt.endsWith(": "))
         return false;
      
      else
         return true;
   }
   
   private boolean isNotEnglishPasswordPrompt(String prompt)
   {
      String promptLower = prompt.trim().toLowerCase();
      boolean hasPassword = promptLower.contains("password") || 
                            promptLower.contains("passphrase");
      
      
      // if there is no password or passphrase then show input
      if (!hasPassword)
      {
         return true;
      }
      else
      {
         // detect yes/no prompt and make that an exception (subversion
         // does a yes/no for asking whether to store the password unencrypted)
         boolean hasYesNo = promptLower.endsWith("(yes/no)?") ||
                            promptLower.endsWith("(y/n)?");
         return hasYesNo;
      }
   }
   
   private final class InputKeyDownHandler implements KeyDownHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
         int keyCode = event.getNativeKeyCode();
         int modifiers = KeyboardShortcut.getModifierValue(
                                                event.getNativeEvent());
         
         if (historyEnabled_ && event.isUpArrow() && modifiers == 0)
         {
            InputEditorDisplay input = display_.getInputEditorDisplay();
            if (input.getCurrentLineNum() == 0)
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(-1);
            }
         }
         else if (historyEnabled_ && event.isDownArrow() && modifiers == 0)
         {
            InputEditorDisplay input = display_.getInputEditorDisplay();
            if (input.getCurrentLineNum() == input.getCurrentLineCount() - 1)
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

            processInput(inputHandler_);
         }
         else if (modifiers == KeyboardShortcut.CTRL && keyCode == 'C')
         {
            event.preventDefault();
            event.stopPropagation();
         
            if (display_.isPromptEmpty())
               display_.consoleWriteOutput("^C");
            
            inputHandler_.execute(ShellInput.createInterrupt());
         }
         else if (modifiers == KeyboardShortcut.CTRL && keyCode == 'L')
         {
            event.preventDefault();
            event.stopPropagation();
           
            display_.clearOutput();
         }
      }
   }
    
   private void encryptInput(final String input, 
                             final CommandWithArg<String> onInputReady)
   {
      if (Desktop.isDesktop())
      {
         onInputReady.execute(input);
      }
      else if (publicKeyInfo_ != null)
      {
         RSAEncrypt.encrypt_ServerOnly(publicKeyInfo_, input, onInputReady);
      }
      else
      {
         server_.getPublicKey(new ServerRequestCallback<PublicKeyInfo>() {

            @Override
            public void onResponseReceived(PublicKeyInfo publicKeyInfo)
            {
               publicKeyInfo_ = publicKeyInfo;
               RSAEncrypt.encrypt_ServerOnly(publicKeyInfo_, 
                                             input, 
                                             onInputReady);
            }
            
            @Override
            public void onError(ServerError error)
            {
               consoleWriteError(error.getUserMessage());
            }
            
         });
      }
        
   }
   
   
   private final ShellDisplay display_;
   
   private boolean addToHistory_ ;
   private boolean historyEnabled_ = true;
   private boolean noEchoForColonPrompts_ = false;
   private String lastPromptText_ ;
   private String defaultPromptSuffix_ = null;
   
   private boolean firstPromptShown_ = false;

   private final InputEditorDisplay input_ ;
   private final CommandLineHistory historyManager_;
   
   private final CommandWithArg<ShellInput> inputHandler_;
   
   private final CryptoServerOperations server_;
   private PublicKeyInfo publicKeyInfo_ = null;
}
