/*
 * PreemptiveTaskQueue.java
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

import java.util.LinkedList;
import java.util.Queue;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;

// Task queue that allows tasks to preempt others in the queue (even after
// they have been added to the queue).

public class PreemptiveTaskQueue
{
   public interface Task
   {
      String getLabel(); // used for debug/log output 
      boolean shouldPreempt();
      void execute(Command done);
   }
   
   public PreemptiveTaskQueue()
   {
      this(true, false);
   }
   
   public PreemptiveTaskQueue(boolean safe)
   {
      this(safe, false);
   }
   
   public PreemptiveTaskQueue(boolean safe, boolean log)
   {
      log_ = log;
      safe_ = safe;
   }
   
   public void addTask(Task task)
   {
      log("adding " + task.getLabel());
      taskQueue_.add(task);
      processTasks();
   }
   
   private void processTasks()
   {
      if (processing_)
      {
         log("already running");
         return;
      }
      
      processing_ = true;
      processNextTask();
   }
   
   private void processNextTask()
   {
      log("process next task");
      
      if (taskQueue_.isEmpty())
      {
         log("done");
         processing_ = false;
         return;
      }
      
      // see if any of the tasks have priority
      Task nextTask = null;
      for (Task task : taskQueue_)
      {
         if (task.shouldPreempt())
         {
            nextTask = task;
            log("executing " + nextTask.getLabel() + " [Priority]");
            break;
         }
      }
      
      // if there is no priority task then just remove from the queue
      if (nextTask == null)
      {
         nextTask = taskQueue_.peek();
         log("executing " + nextTask.getLabel());  
      }
      
      // remove the task
      taskQueue_.remove(nextTask);
      
      // run the next task and then continue processing. catch any exceptions
      // so that we can continue processing if 'safe' was requested
      try
      {
         nextTask.execute(() -> {
            log("continuation");
            // defer next task to give event loop a chance
            // to process other user input/actions
            Scheduler.get().scheduleDeferred(() -> {
               processNextTask(); 
            });
           
         });
      }
      catch(Exception e)
      {
         if (safe_)
         {
            log("exception processing " + nextTask.getLabel());
            Debug.logException(e);
            processNextTask();
         }
         else
         {
            throw e;
         }  
      }
      
   }
   
   private void log(String label)
   {
      if (log_)
      {
         Debug.logToConsole(hashCode() + " " + label + " size=" + taskQueue_.size());
      }
   }
   
   
   private Queue<Task> taskQueue_ = new LinkedList<Task>();
   private boolean processing_ = false;
   private final boolean log_;
   private final boolean safe_;
   
}
