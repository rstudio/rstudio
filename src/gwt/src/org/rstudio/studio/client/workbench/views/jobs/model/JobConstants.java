/*
 * JobConstants.java
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
package org.rstudio.studio.client.workbench.views.jobs.model;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.workbench.views.jobs.JobsConstants;

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
   public final static String ACTION_REPLAY = "replay";

   // job types
   public final static int JOB_TYPE_UNKNOWN = 0;
   public final static int JOB_TYPE_SESSION = 1; // local job, child of rsession
   public final static int JOB_TYPE_LAUNCHER = 2; // cluster job via job launcher

   public final static String stateDescription(int state)
   {
      switch(state)
      {
         case JobConstants.STATE_RUNNING:
            return constants_.runningState();
         case JobConstants.STATE_IDLE:
            return constants_.idleState();
         case JobConstants.STATE_CANCELLED:
            return constants_.cancelledState();
         case JobConstants.STATE_FAILED:
            return constants_.failedState();
         case JobConstants.STATE_SUCCEEDED:
            return constants_.succeededState();
      }
      return constants_.unknownState(state);
   }
   private static final JobsConstants constants_ = GWT.create(JobsConstants.class);
}