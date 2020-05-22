/*
 * RnwWeaveDirective.java
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
package org.rstudio.studio.client.common.rnw;

import org.rstudio.core.client.tex.TexMagicComment;
import org.rstudio.studio.client.RStudioGinjector;

public class RnwWeaveDirective 
{
   public static RnwWeaveDirective fromTexMagicComment(
                                          TexMagicComment magicComment) 
   {
      if (magicComment.getScope().equalsIgnoreCase("rnw") &&
          magicComment.getVariable().equalsIgnoreCase("weave"))
      { 
         return new RnwWeaveDirective(magicComment.getValue());
      }
      else
      {
         return null;
      }
   }
   
   private RnwWeaveDirective(String rnwWeaveName)
   {
      rnwWeaveName_ = rnwWeaveName;
   }
   
   public String getName()
   {
      return rnwWeaveName_;
   }
   
   public RnwWeave getRnwWeave()
   {
      return RStudioGinjector.INSTANCE.getRnwWeaveRegistry()
                                                .findTypeIgnoreCase(getName());
   }
  
   
   private String rnwWeaveName_;
}
