/*
 * CompletionOptions.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import java.util.ArrayList;

public class CompletionOptions
{
   public void addOption(String optionValue,
                         int minPrefixChars)
   {
      options_.add(optionValue);
      filterPrefixes_.add(optionValue.substring(0, minPrefixChars));
   }

   public ArrayList<String> getCompletions(String prefix)
   {
      ArrayList<String> results = new ArrayList<String>();
      for (int i = 0; i < options_.size(); i++)
      {
         if (options_.get(i).startsWith(prefix) &&
             prefix.startsWith(filterPrefixes_.get(i)))
         {
            results.add(options_.get(i));
         }
      }
      return results;
   }

   private ArrayList<String> options_ = new ArrayList<String>();
   private ArrayList<String> filterPrefixes_ = new ArrayList<String>();
}
