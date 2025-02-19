/*
 * MemoryStat.java
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
package org.rstudio.studio.client.workbench.views.environment.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.studio.client.workbench.views.environment.ViewEnvironmentConstants;
import org.rstudio.core.client.StringUtil;

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

   public final native MemoryStat getLimit() /*-{
      return this.limit;
   }-*/;

   public final native boolean overLimit() /*-{
      return this.overLimit;
   }-*/;

   public final native boolean abort() /*-{
      return this.abort;
   }-*/;

   public final native boolean limitWarning() /*-{
      return this.limitWarning;
   }-*/;

   private int computePercent(int kb, int totalKb)
   {
      return (int)Math.round(((kb * 1.0) / (totalKb * 1.0)) * 100);
   }
   /**
    * Compute the percentage of memory used.
    *
    * @return The amount of memory used as a percentage of the total.
    */
   public final int getPercentUsed()
   {
      if (useProcessLimit())
         return computePercent(getProcess().getKb(), getLimit().getKb());
      return computePercent(getUsed().getKb(), getTotal().getKb());
   }

   /**
    * Compute the percentage of process memory used.
    *
    * @return The amount of memory used as a percentage of the total.
    */
   public final int getProcessPercentUsed()
   {
      if (useProcessLimit())
         return getPercentUsed();
      return computePercent(getProcess().getKb(), getTotal().getKb());
   }

   /**
    * If the session process has a specific limit, that's different than
    * the total, use it for the percentage.
    */
   public final boolean useProcessLimit()
   {
      long limit = getLimit().getKb();
      return limit != 0;
   }

   private final String limitMessage()
   {
      return useProcessLimit() ? constants_.memoryUsageLimit(StringUtil.prettyFormatNumber(getLimit().getKb()/1024)) : 
                                 constants_.unlimited();
   }

   private final int getPercentFree()
   {
      return ((int) (100 * (getTotal().getKb() - getUsed().getKb()) / getTotal().getKb()));
   }

   public final String multiLineStatusMessage()
   {
      return constants_.multiLineMemoryStatus(StringUtil.prettyFormatNumber(getProcess().getKb()/1024),
                                              limitMessage(),
                                              StringUtil.prettyFormatNumber((getTotal().getKb() - getUsed().getKb())/1024),
                                              String.valueOf(getPercentFree()));
   }

   public final String statusMessage()
   {
      return constants_.memoryUsageStatus(StringUtil.prettyFormatNumber(getProcess().getKb()/1024), 
                                          limitMessage(),
                                          StringUtil.prettyFormatNumber(getUsed().getKb()/1024),
                                          StringUtil.prettyFormatNumber(getTotal().getKb()/1024),
                                          String.valueOf(getPercentFree()));
   }

   private static final ViewEnvironmentConstants constants_ = GWT.create(ViewEnvironmentConstants.class);
}
