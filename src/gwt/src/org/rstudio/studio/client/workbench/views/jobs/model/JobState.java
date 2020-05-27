/*
 * JobState.java
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
package org.rstudio.studio.client.workbench.views.jobs.model;

import java.util.Date;

import org.rstudio.core.client.js.JsObject;

public class JobState extends JsObject
{
   protected JobState()
   {
   }
   
   public final Job getJob(String id)
   {
      return getElement(id);
   }
   
   public final void updateJob(Job job)
   {
      setElement(job.id, job);
   }
   
   public final void addJob(Job job)
   {
      updateJob(job);
   }
   
   public final void removeJob(Job job)
   {
      unset(job.id);
   }

   public final void recordReceived()
   {
      // apply timestamp to each received job
      for (String id: iterableKeys())
      {
         Job job = getElement(id);
         recordReceived(job);
      }
   }
   
   public final static void recordReceived(Job job)
   {
      // set the timestamp to the current time
      int timestamp = (int)((new Date()).getTime() * 0.001);
      job.received = timestamp;     
   }

   public final static JobState create()
   {
      return (JobState)JsObject.createJsObject();
   }

   public final boolean hasJobs()
   {
      for (@SuppressWarnings("unused") String id : iterableKeys())
      {
         return true;
      }
      return false;
   }
}
