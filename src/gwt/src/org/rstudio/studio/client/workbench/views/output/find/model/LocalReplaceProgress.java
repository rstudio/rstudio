/*
 * LocalReplaceProgress.java
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
package org.rstudio.studio.client.workbench.views.output.find.model;

public class LocalReplaceProgress
{
   /**
    * Summarize currently running replaces
    * 
    * @param units The total number of progress units completed
    * @param max The total number of progress units remaining
    * @param elapsed The time that has elapsed on the server
    * @param received The time that the client last received an update
    */
   public LocalReplaceProgress(int units, int max, 
                               int elapsed, int received)
   {
      units_ = units;
      max_ = max;
      elapsed_ = elapsed;
      received_ = received;
   }
   
   /**
    * Summarize (copy) progress for a single replace
    * 
    * @param replace The replace to summarize.
    */
   /*public LocalReplaceProgress(Replace replace)
   {
      units_ = replace.progress;
      max_ = replace.max;
      elapsed_ = replace.elapsed;
      received_ = replace.received;
      */
  // }
   
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

   public double percent()
   {
      return ((double)units() / (double) max()) * 100;
   }

   private final int units_;
   private final int max_;
   private final int elapsed_;
   private final int received_;
}
