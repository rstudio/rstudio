/*
 * ParallelCommandList.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import java.util.ArrayList;

import com.google.gwt.user.client.Command;

/*
 * A class that acts like Promise.all - allows for a list of commands to be
 * executed asynchronously as quickly as possible in parallel, and calls the
 * callback when all commands have been completed
 */
public class ParallelCommandList
{
   public ParallelCommandList(Command onCompleted, boolean log)
   {
      log_ = log;
      onCompleted_ = onCompleted;
   }

   public ParallelCommandList(Command onCompleted)
   {
      this(onCompleted, false);
   }

   public void addCommand(SerializedCommand command)
   {
      if (running_)
      {
         log("cannot add - already running");
         return;
      }

      commands_.add(command);
   }

   public void run()
   {
      if (running_)
      {
         log("already running");
         return;
      }
      running_ = true;
   
      if (commands_.size() <= 0)
      {
         countdown();
         return;
      }

      commandsRemaining = commands_.size();

      for (SerializedCommand command : commands_) 
      {
         command.onExecute(() ->
         {
            log("finished cmd");
            countdown();
         });
      }
   }

   private void countdown()
   {
      log("countdown");

      commandsRemaining--;

      if (commandsRemaining <= 0)
      {
         log("done");
         running_ = false;
         onCompleted_.execute();
         commands_.clear();
         return;
      }

   }

   private void log(String label)
   {
      if (log_)
      {
         Debug.log(hashCode() + " " + label + " size=" + commands_.size());
      }
   }

   private boolean running_ = false;
   private int commandsRemaining = 0;
   private final ArrayList<SerializedCommand> commands_ = new ArrayList<>();
   private final boolean log_;
   private final Command onCompleted_;
}
