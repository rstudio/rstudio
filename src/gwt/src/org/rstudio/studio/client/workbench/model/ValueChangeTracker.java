/*
 * ValueChangeTracker.java
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

public class ValueChangeTracker<TValue>
{
   public ValueChangeTracker(TValue value)
   {
      value_ = value;
   }

   public TValue getValue()
   {
      return value_;
   }

   public boolean checkForChange(TValue newValue)
   {
      boolean equal;
      if (value_ == null ^ newValue == null)
         equal = false;
      else if (value_ == null)
         equal = true;
      else
         equal = value_.equals(newValue);

      if (!equal)
         value_ = newValue;

      return !equal;
   }

   private TValue value_;
}
