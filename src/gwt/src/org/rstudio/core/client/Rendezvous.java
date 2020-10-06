/*
 * Rendezvous.java
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

import com.google.gwt.user.client.Command;

/**
 * A synchronization point for multiple async operations. (Similar to a barrier
 * object in multithreaded programming, but this is for async on a single
 * thread.)
 *
 * When creating a Rendezvous object, you specify the number of arrivals you're
 * anticipating. Each arrival is accompanied by an action (Command). Until all
 * participants have arrived, none of the actions execute; once the last
 * participant has arrived, all of the actions are executed.
 */
public class Rendezvous
{
   // Number of remaining participants we're waiting for
   private int remainingArrivals;
   private ArrayList<Command> joined;

   /**
    * @param count The number of calls to arrive() that are necessary for all
    * participants to be unblocked.
    */
   public Rendezvous(int count)
   {
      this.remainingArrivals = count;
      this.joined = new ArrayList<Command>();
   }

   /**
    * Indicate the arrival of a participant, along with the operation they want
    * to carry out once everyone has arrived.
    *
    * If a call to arrive() is the last one, then all of the joined tasks are
    * synchronously invoked before arrive() returns.
    *
    * @param cmd The command to execute once all participants have arrived
    * @param atHead If true, when everyone arrives, this command should be added
    * to the beginning of the list of commands instead of the end; i.e. it takes
    * precedence over any participants that have arrived so far (though can be
    * superceded by participants that arrive later that also have atHead=true).
    */
   public void arrive(Command cmd, boolean atHead)
   {
      if (this.remainingArrivals == 0)
         throw new IllegalStateException("Rendezvous point was arrived at too many times");
      if (cmd == null)
         throw new IllegalArgumentException("Rendezvous cmd must not be null");

      this.joined.add(
         atHead ? 0 : this.joined.size(),
         cmd);

      this.remainingArrivals--;

      if (this.remainingArrivals <= 0)
         this.execute();
   }

   private void execute()
   {
      try
      {
         this.joined.forEach(cmd -> {
            cmd.execute();  
         });
      }
      finally
      {
         this.joined.clear();
      }
   }
}