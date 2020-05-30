/*
 * SafeUriStringImpl.java
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

import com.google.gwt.safehtml.shared.SafeUri;

public class SafeUriStringImpl implements SafeUri
{
   public SafeUriStringImpl(String value)
   {
      value_ = value;
   }

   @Override
   public String asString()
   {
      return value_;
   }

   @Override
   public int hashCode()
   {
      return value_.hashCode();
   }

   @Override
   public boolean equals(Object o)
   {
      if (o == null ^ value_ == null)
         return false;
      if (value_ == null)
         return false;
      return value_.equals(o.toString());
   }

   @Override
   public String toString()
   {
      return value_;
   }

   private final String value_;
}
