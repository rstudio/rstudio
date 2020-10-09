/*
 * QuotaStatus.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

public class QuotaStatus extends JavaScriptObject
{
   protected QuotaStatus()
   {
   }

   public final long getUsed()
   {
      return getLongValue("used");
   }

   public final long getQuota()
   {
      return getLongValue("quota");
   }

   public final long getLimit()
   {
      return getLongValue("limit");
   }

   public final boolean isNearQuota()
   {
      return isNear(getQuota());
   }

   public final boolean isOverQuota()
   {
      return isOver(getQuota());
   }

   public final boolean isNearLimit()
   {
     return isNear(getLimit());
   }

   private final boolean isOver(double threshold)
   {
      return getUsed() > threshold;
   }

   private final boolean isNear(double threshold)
   {
      // defend against dbz
      if (threshold == 0)
         return false;

      return (getUsed() / threshold) > 0.90;
   }

   private final long getLongValue(String value)
   {
      return Double.valueOf(getValueNative(value)).longValue();
   }

   private final native double getValueNative(String value) /*-{
      return this[value];
   }-*/;

}
