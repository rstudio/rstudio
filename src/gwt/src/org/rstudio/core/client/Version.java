/*
 * Version.java
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

import java.util.ArrayList;
import java.util.List;

// A helper class for parsing numeric versions.
public class Version implements Comparable<Version>
{
   public static final int compare(String lhs, String rhs)
   {
      Version lhsVersion = new Version(lhs);
      Version rhsVersion = new Version(rhs);
      return lhsVersion.compareTo(rhsVersion);
   }
      
   private Version(String version)
   {
      components_ = new ArrayList<>();
      
      String[] parts = version.split("[.-]");
      for (int i = 0; i < parts.length; i++)
      {
         int value = 0;
         
         try
         {
            value = Integer.parseInt(parts[i]);
         }
         catch (Exception e)
         {
            Debug.logException(e);
         }
         
         components_.add(value);
      }
   }
   
   @Override
   public int compareTo(Version other)
   {
      int n = Math.max(size(), other.size());
      for (int i = 0; i < n; i++)
      {
         int lhs = component(i);
         int rhs = other.component(i);
         
         if (lhs < rhs)
            return -1;
         else if (lhs > rhs)
            return 1;
      }
      
      return 0;
   }
   
   private int component(int index)
   {
      if (index < 0)
      {
         return 0;
      }
      else if (index < components_.size())
      {
         return components_.get(index);
      }
      else
      {
         return 0;
      }
   }
   
   private int size()
   {
      return components_.size();
   }
   
   private final List<Integer> components_;
}
