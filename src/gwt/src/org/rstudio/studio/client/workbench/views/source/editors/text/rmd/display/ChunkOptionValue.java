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
 * Stores the value of a chunk option and whether its stored in the YAML options (#| ...)
 * or inthe first line of the chunk. Example of both styles:
 * 
 *     ```{r, option=value}
 *     #| option: value
 *     #| another-option: value
 *     ```
 */
public class ChunkOptionValue {

   public ChunkOptionValue(String value, boolean isYaml)
   {
      value_ = value;
      yaml_ = isYaml;
   }

   public void setOptionValue(String value)
   {
      value_ = value;
   }

   public String getOptionValue()
   {
      return value_;
   }

   public void setIsYaml(boolean isYaml)
   {
      yaml_ = isYaml;
   }

   public boolean setIsYaml()
   {
      return yaml_;
   }

   private String value_;
   private boolean yaml_;
}
