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

/**
 * Represents progress for all currently running jobs
 */
public class GlobalJobProgress
{
   public GlobalJobProgress(String name, int units, int max, int started)
   {
      name_ = name;
      units_ = units;
      max_ = max;
      started_ = started;
   }
   
   public int units()
   {
      return units_;
   }
   
   public int max()
   {
      return max_;
   }
   
   public int started()
   {
      return started_;
   }
   
   public String name()
   {
      return name_;
   }

   private final String name_;
   private final int units_;
   private final int max_;
   private final int started_;
}
