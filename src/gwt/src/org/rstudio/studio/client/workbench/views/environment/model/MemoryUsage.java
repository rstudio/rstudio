/*
 * MemoryStat.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.JavaScriptObject;

public class MemoryUsage extends JavaScriptObject
{
   protected MemoryUsage() {}

   public final native MemoryStat getTotal() /*-{
      return this.total;
   }-*/;

   public final native MemoryStat getUsed() /*-{
      return this.used;
   }-*/;

   public final native MemoryStat getProcess() /*-{
      return this.process;
   }-*/;

   /**
    * Compute the percentage of memory used.
    *
    * @return The amount of memory used as a percentage of the total.
    */
   public final int getPercentUsed()
   {
      return (int)Math.round(((getUsed().getKb() * 1.0) / (getTotal().getKb() * 1.0)) * 100);
   }

   /**
    * Gets a color code representing the memory usage (green = low, red = high, etc.)
    *
    * @return The color code, in a CSS-compatible format.
    */
   public final String getColorCode()
   {
      int percent = getPercentUsed();

      // These values are chosen to align with those used in rstudio.cloud.
      String color = "#5f9a91";   // under 70%, green
      if (percent > 90) {
         color = "#e55037";       // 90% and above, red
      } else if (percent > 80) {
         color = "#e58537";       // 80-90%, orange
      } else if (percent > 70) {
         color = "#fcbf49";       // 70-80%, yellow
      }

      return color;
   }
}
