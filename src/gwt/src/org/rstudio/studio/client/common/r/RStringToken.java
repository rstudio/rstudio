/*
 * RStringToken.java
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
package org.rstudio.studio.client.common.r;

public class RStringToken extends RToken
{
   public RStringToken(int tokenType, 
                       String content, 
                       int offset, 
                       int length, 
                       boolean wellFormed)
   {
      super(tokenType, content, offset, length);

      wellFormed_ = wellFormed;
   }
   
   public boolean isWellFormed()
   {
      return wellFormed_;
   }

   private final boolean wellFormed_;
}
