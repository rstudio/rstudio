/*
 * Invalidation.java
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

public class Invalidation
{
   public class Token
   {
      public Token(int thisSequence)
      {
         thisSequence_ = thisSequence;
      }

      public boolean isInvalid()
      {
         return thisSequence_ != sequence_;
      }
      
      public boolean isValid()
      {
         return thisSequence_ == sequence_;
      }

      private final int thisSequence_;
   }

   public void invalidate()
   {
      sequence_++;
   }

   public Token getInvalidationToken()
   {
      return new Token(sequence_);
   }

   private int sequence_ = 0;
}
