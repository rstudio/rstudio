/*
 * Mutable.java
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

// A utility class primarily used for creating mutable integers etc.
// Useful for mutable objects that need to be accessible by anonymous classes,
// callbacks, and so on.
public class Mutable<T>
{
   public Mutable()
   {
      data_ = null;
   }
   
   public Mutable(T data)
   {
      data_ = data;
   }
   
   public T get()
   {
      return data_;
   }
   
   public void set(T data)
   {
      data_ = data;
   }
   
   public void clear()
   {
      data_ = null;
   }
   
   private T data_;
}
