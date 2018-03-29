/*
 * GlobalJobProgress.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

public class GlobalJobProgress
{
   /**
    * An object that summarizes the progress of all the currently running jobs
    * 
    * @param name The name of the job(s) running
    * @param units The total number of progress units completed
    * @param max The total number of progress units remaining
    * @param elapsed The time that has elapsed on the server
    * @param received The time that the client last received an update
    */
   public GlobalJobProgress(String name, int units, int max, 
                            int elapsed, int received)
   {
      name_ = name;
      units_ = units;
      max_ = max;
      elapsed_ = elapsed;
      received_ = received;
   }
   
   public int units()
   {
      return units_;
   }
   
   public int max()
   {
      return max_;
   }
   
   public int elapsed()
   {
      return elapsed_;
   }
   
   public int received()
   {
      return received_;
   }

   public String name()
   {
      return name_;
   }
   
   public double percent()
   {
      return ((double)units() / (double) max()) * 100;
   }

   private final String name_;
   private final int units_;
   private final int max_;
   private final int elapsed_;
   private final int received_;
}
