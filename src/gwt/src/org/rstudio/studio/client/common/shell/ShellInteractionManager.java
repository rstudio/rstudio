package org.rstudio.studio.client.common.shell;


import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.regex.Pattern;
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


public class ShellInteractionManager
{
   public ShellInteractionManager(ShellDisplay display,
                                  CryptoServerOperations server,
                                  CommandWithArg<String> inputHandler)
   {
      display_ = display;
      server_ = server;
      input_ = display_.getInputEditorDisplay();
      historyManager_ = new CommandLineHistory(input_);
      inputHandler_ = inputHandler;
      
      display_.addCapturingKeyDownHandler(new InputKeyDownHandler());
   }
   
   public void setPublicKeyInfo(PublicKeyInfo publicKeyInfo)
   {
      publicKeyInfo_ = publicKeyInfo;
   }
   
   public void setHistoryEnabled(boolean enabled)
   {
      historyEnabled_ = enabled;
   }
   
   public void displayOutput(String output)
   {
      // if the output ends with a newline then just send it all
      // in a big batch
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
   
   public void displayError(String error)
   {
      // show the error in the console then re-prompt
      display_.consoleWriteError(
            "Error: " + error + "\n");
      if (lastPromptText_ != null)
         consolePrompt(lastPromptText_, false);
   }
   
   private void processInput(CommandWithArg<String> onInputReady)
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
      if (showInputForPrompt(promptText))
         display_.consoleWriteInput(input);
      else
         display_.consoleWriteInput("\n");
      
      // encrypt the input and return it
      encryptInput(input, onInputReady);
   }
 
   private void navigateHistory(int offset)
   {
      historyManager_.navigateHistory(offset);
      display_.ensureInputVisible();
   }
   
   // prompt as long as there are no special control characters
   // (otherwise treat it as output)
   private void maybeConsolePrompt(String output)
   {
      if (CONTROL_SPECIAL.match(output, 0) == null)
      {
         // determine whether we should add this to the history
         boolean addToHistory = false;
        
         if (historyEnabled_)
         {
            // figure out what the suffix of the default prompt is by inspecting
            // the first prompt which comes our way
            if (defaultPromptSuffix_ == null)
            {
               if (output.length() > 1)
                  defaultPromptSuffix_ = output.substring(output.length()-2);
               else if (output.length() > 0)
                  defaultPromptSuffix_ = output;
               
               addToHistory = true;
            }
            else if (output.endsWith(defaultPromptSuffix_))
            {
               addToHistory = true;
            }
         }
         
         consolePrompt(output, addToHistory);
      }
      else
      {
         display_.consoleWriteOutput(output);
      }
   }
    
   private void consolePrompt(String prompt, boolean addToHistory)
   {
      boolean showInput = showInputForPrompt(prompt);
      display_.consolePrompt(prompt, showInput) ;

      addToHistory_ = addToHistory && showInput;
      historyManager_.resetPosition();
      lastPromptText_ = prompt ;
   }
   
   private boolean showInputForPrompt(String prompt)
   {
      return !prompt.contains("password") && !prompt.contains("passphrase");
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
            
            inputHandler_.execute(null);
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
               displayError(error.getUserMessage());
            }
            
         });
      }
        
   }
   
   
   private final ShellDisplay display_;
   
   private boolean addToHistory_ ;
   private boolean historyEnabled_ = true;
   private String lastPromptText_ ;
   private String defaultPromptSuffix_ = null;

   private final InputEditorDisplay input_ ;
   private final CommandLineHistory historyManager_;
   
   private final CommandWithArg<String> inputHandler_;
   
   private final CryptoServerOperations server_;
   private PublicKeyInfo publicKeyInfo_ = null;
   
   private static final Pattern CONTROL_SPECIAL = Pattern.create("[\r\b]");
}
