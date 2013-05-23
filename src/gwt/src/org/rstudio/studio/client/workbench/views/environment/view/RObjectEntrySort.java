/*
 * RObjectEntrySort.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.view;

import java.util.Comparator;

class RObjectEntrySort implements Comparator<RObjectEntry>
{
   public int compare(RObjectEntry first, RObjectEntry second)
   {
      int result = first.getCategory() - second.getCategory();
      if (result == 0)
      {
         result = localeCompare(first.rObject.getName(), second.rObject.getName());
      }
      return result;
   }

   private native int localeCompare(String first, String second) /*-{
       return first.localeCompare(second);
   }-*/;
}
