/*
 * JobConstants.java
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

public class JobConstants
{
   // job update types
   public final static int JOB_ADDED   = 0;
   public final static int JOB_UPDATED = 1;
   public final static int JOB_REMOVED = 2;

   // possible job states
   public final static int STATE_IDLE      = 1;
   public final static int STATE_RUNNING   = 2;
   public final static int STATE_SUCCEEDED = 3;
   public final static int STATE_CANCELLED = 4;
   public final static int STATE_FAILED    = 5;
   
   // special job actions
   public final static String ACTION_STOP = "stop";
   public final static String ACTION_INFO = "info";
   
   // job types
   public final static int JOB_TYPE_UNKNOWN = 0;
   public final static int JOB_TYPE_SESSION = 1; // local job, child of rsession
   public final static int JOB_TYPE_LAUNCHER = 2; // cluster job via job launcher
   
   public final static String stateDescription(int state)
   {
      switch(state)
      {
         case JobConstants.STATE_RUNNING:
            return "Running";
         case JobConstants.STATE_IDLE:
            return "Idle";
         case JobConstants.STATE_CANCELLED:
            return "Cancelled";
         case JobConstants.STATE_FAILED:
            return "Failed";
         case JobConstants.STATE_SUCCEEDED:
            return "Succeeded";
      }
      return "Unknown " + state;
   }
}
