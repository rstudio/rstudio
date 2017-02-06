/*
 * ShellInteractionManager.java
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
package org.rstudio.studio.client.common.shell;


import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.CommandLineHistory;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;


public class ShellInteractionManager implements ShellOutputWriter
{
   public ShellInteractionManager(ShellDisplay display,
                                  CommandWithArg<ShellInput> inputHandler)
   {
      display_ = display;
      input_ = display_.getInputEditorDisplay();
      historyManager_ = new CommandLineHistory(input_);
      inputHandler_ = inputHandler;
      
      display_.addCapturingKeyDownHandler(new InputKeyDownHandler());
   }
   
   public void setHistoryEnabled(boolean enabled)
   {
      historyEnabled_ = enabled;
   }

   @Override
   public void consoleWriteOutput(String output)
   {
      output = maybeSuppressOutputPrefix(output);
      if (StringUtil.isNullOrEmpty(output))
         return;

      display_.consoleWriteOutput(output);
   }

   private String maybeSuppressOutputPrefix(String output)
   {
      if (!Desktop.isDesktop() || !BrowseCap.isWindows())
         return output;

      if (StringUtil.isNullOrEmpty(outputPrefixToSuppress_))
         return output;

      String prefix = outputPrefixToSuppress_;
      outputPrefixToSuppress_ = null;

      if (output.startsWith(prefix))
         return output.substring(prefix.length());

      return output;
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
   public void consoleWriteExtendedError(
         String error, UnhandledError traceInfo, 
         boolean expand, String command)
   {
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
      
      outputPrefixToSuppress_ = null;
      // update console with prompt and input
      display_.consoleWritePrompt(promptText);
      final boolean echoInput = showInputForPrompt(promptText);
      if (echoInput)
      {
         display_.consoleWriteInput(input, "");
         if (Desktop.isDesktop() && BrowseCap.isWindows())
            outputPrefixToSuppress_ = commandEntry;
      }

      onInputReady.execute(ShellInput.create(input, echoInput));
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
         if (historyEnabled_ && 
               ((event.isUpArrow() && modifiers == 0) ||
                (keyCode == 'P'    && modifiers == KeyboardShortcut.CTRL)))
         {
            InputEditorDisplay input = display_.getInputEditorDisplay();
            if (input.getCurrentLineNum() == 0)
            {
               event.preventDefault();
               event.stopPropagation();

               navigateHistory(-1);
            }
         }
         else if (historyEnabled_ && 
               ((event.isDownArrow() && modifiers == 0) ||
                (keyCode == 'N'      && modifiers == KeyboardShortcut.CTRL)))
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
    
   private final ShellDisplay display_;
   
   private boolean addToHistory_ ;
   private boolean historyEnabled_ = true;
   private String lastPromptText_ ;
   private String defaultPromptSuffix_ = null;
   
   private boolean firstPromptShown_ = false;

   private final InputEditorDisplay input_ ;
   private final CommandLineHistory historyManager_;
   
   private final CommandWithArg<ShellInput> inputHandler_;
   
   /* Hack to fix echoing problems on Windows.
    * For echoed input like username, Windows always echoes input back to the
    * client. We don't have a good way to avoid this happening on the server,
    * nor can we simply not echo locally on the client because there is a
    * several-hundred-millisecond delay between when we send input and when the
    * server echoes it back to us (normally would be a much shorter delay but
    * consoleio.exe makes it longer due to console polling instead of
    * streaming). Therefore, we echo the input locally, and then look for the
    * same string at the head of the next output event. If we find it, we strip
    * it off.
    */
   private String outputPrefixToSuppress_;
}
