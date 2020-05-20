/*
 * SeparatorManager.java
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
package org.rstudio.core.client;

import java.util.List;

/**
 * Implements a generic algorithm for managing visibility of separators in
 * a list of items, some of which may not be visible.
 *
 * @param <TItem> The common supertype of items and separators.
 */
public abstract class SeparatorManager<TItem>
{
   protected abstract boolean isSeparator(TItem item);
   protected abstract boolean isVisible(TItem item);
   protected abstract void setVisible(TItem item, boolean visible);

   public void manageSeparators(List<TItem> items)
   {
      /* allItems is a sorted list of items and separators. We'll make
       * two separate passes
       */

      // Pass one: make sure two separators never appear consecutively
      TItem pendingSeparator = null;
      for (TItem item : items)
      {
         boolean isSeparator = isSeparator(item);

         if (isSeparator)
         {
            if (pendingSeparator != null)
               setVisible(pendingSeparator, false);
            pendingSeparator = item;
         }
         else
         {
            if (isVisible(item))
            {
               if (pendingSeparator != null)
               {
                  setVisible(pendingSeparator, true);
                  pendingSeparator = null;
               }
            }
         }
      }

      // Pass 1.5: hide trailing separator
      if (pendingSeparator != null)
         setVisible(pendingSeparator, false);

      // Pass two: hide leading separator
      for (TItem item : items)
      {
         if (!isSeparator(item) && isVisible(item))
            return;
         if (isSeparator(item))
            setVisible(item, false);
      }
   }
}
