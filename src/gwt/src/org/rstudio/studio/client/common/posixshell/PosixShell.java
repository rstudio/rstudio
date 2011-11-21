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

import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.posixshell.events.PosixShellExitEvent;
import org.rstudio.studio.client.common.posixshell.events.PosixShellOutputEvent;
import org.rstudio.studio.client.common.posixshell.model.PosixShellServerOperations;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.server.VoidServerRequestCallback;

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
      void onShellExited();
   }
   
  
   @Inject
   public PosixShell(Display display,
                     EventBus eventBus,
                     PosixShellServerOperations server)
   {
      // save references
      display_ = display;
      server_ = server;
      
      // set max lines
      int maxLines = 1000;
      display_.setMaxOutputLines(maxLines);
   
      // subscribe to display events
      display_.addCapturingKeyDownHandler(new InputKeyDownHandler());
      
      // subscribe to server events
      eventBusHandlers_.add(
            eventBus.addHandler(PosixShellOutputEvent.TYPE, this));
      eventBusHandlers_.add(
            eventBus.addHandler(PosixShellExitEvent.TYPE, this));
      
      // startup the shell
      int width = 60;
      server.startPosixShell(width, maxLines, new VoidServerRequestCallback());
      
   }
   
   public void setObserver(Observer observer)
   {
      observer_ = observer;
   }
   
   public Widget getWidget()
   {
      return display_.getShellWidget();
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
            consolePrompt(output.substring(lastLoc + 1));
         }
         else
         {
            consolePrompt(output);
         }
      }
   }
   
   @Override
   public void onPosixShellExit(PosixShellExitEvent event)
   {
      if (observer_ != null)
         observer_.onShellExited();    
   }

   public void detachEventBusHandlers()
   {
      for (int i=0; i<eventBusHandlers_.size(); i++)
         eventBusHandlers_.get(i).removeHandler();
      eventBusHandlers_.clear();
   }
   
   
   private void consolePrompt(String prompt)
   {
      lastPrompt_ = prompt;
      display_.consolePrompt(prompt);
   }
   
   
   
   private final class InputKeyDownHandler implements KeyDownHandler
   {
      public void onKeyDown(KeyDownEvent event)
      {
         int keyCode = event.getNativeKeyCode();
         int modifiers = KeyboardShortcut.getModifierValue(
                                                event.getNativeEvent());
         
         if (keyCode == KeyCodes.KEY_ENTER && modifiers == 0)
         {
            event.preventDefault();
            event.stopPropagation();

            String input = display_.processCommandEntry() + "\n";
            
            if (lastPrompt_ != null)
            {
               display_.consoleWritePrompt(lastPrompt_);
               lastPrompt_ = null;
            }
               
            display_.consoleWriteInput(input);
            
            server_.sendInputToPosixShell(input, 
                                          new VoidServerRequestCallback());
         }
         else if (modifiers == KeyboardShortcut.CTRL && keyCode == 'C')
         {
            event.preventDefault();
            event.stopPropagation();
         
            if (display_.isPromptEmpty())
               display_.consoleWriteOutput("^C");
            
            server_.interruptPosixShell(new VoidServerRequestCallback());
         }
      }
   }
   
   private String lastPrompt_ = null;
   
   private final Display display_;
   private Observer observer_ = null;
   private final PosixShellServerOperations server_;
  

   
   private ArrayList<HandlerRegistration> eventBusHandlers_ = 
                                    new ArrayList<HandlerRegistration>();

   
   
}
