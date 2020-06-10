/*
 * ShellInputAnimator.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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


package org.rstudio.studio.client.workbench.views.console.shell;

import java.util.ArrayList;

import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.Command;

public class ShellInputAnimator
{
   public ShellInputAnimator(InputEditorDisplay display)
   {
      display_ = display;
   }
   
   public void enque(String code, final Command onFinished)
   {
      // wrap onFinished with a check of the queue for additional commands
      Command onAnimatedInputFinished = new Command() {
         @Override
         public void execute()
         {
            // perform stock finished action
            onFinished.execute();
            
            // remove from queue
            pendingAnimatedInput_.remove(0);
                       
            // execute any pending inputs
            executePendingAnimatedInput();   
         } 
      };
      
      // create the input and add it to the queue
      InputAnimator inputAnimator = new InputAnimator(
                     code,
                     onAnimatedInputFinished);
      pendingAnimatedInput_.add(inputAnimator);
      
      // if we are the only one in the queue then we need to manually
      // force execution (otherwise we'll just get execute when the
      // currently executing command completes)
      if (pendingAnimatedInput_.size() == 1)
         executePendingAnimatedInput();
   }
      
   
   private void executePendingAnimatedInput()
   {      
      if (pendingAnimatedInput_.size() > 0)
      {
         // get the input animator
         InputAnimator inputAnimator = pendingAnimatedInput_.get(0);
         
         // calculate the period (make sure the command takes no longer
         // than 1600ms to input)
         final int kMaxMs = 1600;
         String code = inputAnimator.getCode();
         int period = Math.min( kMaxMs / code.length(), 75);
         
         // schedule it
         Scheduler.get().scheduleFixedPeriod(inputAnimator, period);
      }
   }
   
   
   private class InputAnimator implements RepeatingCommand
   {
      public InputAnimator(String code, Command onFinished)
      {
         code_ = code;
         onFinished_ = onFinished;
      }
      
      public String getCode()
      {
         return code_;
      }
      
      @Override
      public boolean execute()
      {         
         // termination condition
         if ((nextChar_ + 1) > code_.length())
         {
            onFinished_.execute();
            return false;
         }
         
         // clear before first char
         if (nextChar_ == 0)
            display_.clear();
         
         display_.insertCode(code_.substring(nextChar_, nextChar_+1));
         
         nextChar_++;
         
         return true;
      }
      
      private int nextChar_ = 0;
      private final String code_;
      private final Command onFinished_;
      
   }
   
   private InputEditorDisplay display_;
   
   private ArrayList<InputAnimator> pendingAnimatedInput_ =
      new ArrayList<InputAnimator>();

}
