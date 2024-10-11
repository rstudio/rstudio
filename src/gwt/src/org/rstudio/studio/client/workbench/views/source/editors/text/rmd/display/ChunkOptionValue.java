/*
 * ChunkOptionValue.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

package org.rstudio.studio.client.workbench.views.source.editors.text.rmd.display;

/**
 * Stores the value of a chunk option and whether it's stored in the YAML options (#| ...)
 * or in the first line of the chunk. Example of both styles:
 * 
 *     ```{r, option=value, another.option=value}
 *     #| option: value
 *     #| another-option: value
 *     ```
 */
public class ChunkOptionValue {

   /**
    * Where is the option stored in the document?
    */
   public enum OptionLocation
   {
      FirstLine,
      Yaml
   }

   public ChunkOptionValue(String value, OptionLocation location)
   {
      optionValue_ = value;
      optionLocation_ = location;
   }

   public ChunkOptionValue(boolean value, OptionLocation location)
   {
      optionValue_ = boolForLocation(value, location);
      optionLocation_ = location;
   }

   public void setOptionValue(String value)
   {
      optionValue_ = value;
   }

   public String getOptionValue()
   {
      return optionValue_;
   }

   public void setLocation(OptionLocation location)
   {
      optionLocation_ = location;
   }

   public OptionLocation getLocation()
   {
      return optionLocation_;
   }

   /**
    * Return a boolean string appropriate for the location (R vs. YAML)
    */
   public static String boolForLocation(boolean boolValue, OptionLocation location)
   {
      if (location == OptionLocation.FirstLine)
         return boolValue ? "TRUE" : "FALSE";
      else
         return boolValue ? "true" : "false";
   }

   private String optionValue_;
   private OptionLocation optionLocation_;
}
