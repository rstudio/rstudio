/*
 * StyleBuilder.java
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
package org.rstudio.core.client.dom;

import java.util.ArrayList;
import org.rstudio.core.client.Pair;

public class StyleBuilder
{
   public StyleBuilder()
   {
      data_ = new ArrayList<Pair<String, String>>();
   }
   
   public void add(String key, String value)
   {
      data_.add(new Pair<String, String>(key, value));
   }
   
   @Override
   public String toString()
   {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < data_.size(); i++)
      {
         Pair<String, String> pair = data_.get(i);
         String key = pair.first;
         String value = pair.second;
         builder.append(key + ": " + value + "; ");
      }
      return builder.toString();
   }
   
   private final ArrayList<Pair<String, String>> data_;

}
