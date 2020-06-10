/*
 * SerializedCommandQueue.java
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
package org.rstudio.core.client;

import java.util.ArrayList;

public class SerializedCommandQueue
{
   public SerializedCommandQueue(boolean log)
   {
      log_ = log;
   }

   public SerializedCommandQueue()
   {
      this(false);
   }

   public void addCommand(SerializedCommand command)
   {
      addCommand(command, true);
   }

   public void addCommand(SerializedCommand command, boolean run)
   {
      if (command != null)
         commands_.add(command);
      log("addCommand");
      if (run)
         run();
   }

   public void addPriorityCommand(SerializedCommand command)
   {
      addPriorityCommand(command, true);
   }

   public void addPriorityCommand(SerializedCommand command, boolean run)
   {
      if (command != null)
         commands_.add(0, command);
      log("addPriorityCommand");
      if (run)
         run();
   }

   public void run()
   {
      if (running_)
      {
         log("already running");
         return;
      }
      running_ = true;

      executeNextCommand();
   }

   private void executeNextCommand()
   {
      log("executeNextCommand");

      if (commands_.isEmpty())
      {
         log("done");
         running_ = false;
         return;
      }

      SerializedCommand head = commands_.remove(0);
      head.onExecute(() ->
      {
         log("continuation");
         executeNextCommand();
      });
   }

   private void log(String label)
   {
      if (log_)
      {
         Debug.log(hashCode() + " " + label + " size=" + commands_.size());
      }
   }

   private boolean running_ = false;
   private final ArrayList<SerializedCommand> commands_ = new ArrayList<>();
   private final boolean log_;
}
