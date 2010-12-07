/*
 * RTokenRange.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.r;

import java.util.ArrayList;
import java.util.List;

public class RTokenRange
{  
   public RTokenRange(String code)
   {
      tokens_ = RTokenizer.asTokens(code) ;
   }
   
   public RTokenRange(List<RToken> tokens)
   {
      tokens_ = new ArrayList<RToken>(tokens) ;
   }
   
   public boolean isBOD()
   {
      return pos_ < 0 ;
   }
   
   public boolean isEOD()
   {
      return pos_ >= tokens_.size() ;
   }
   
   public RToken currentToken()
   {
      if (pos_ >= 0 && tokens_.size() > pos_)
         return tokens_.get(pos_) ;
      
      return null ;
   }
   
   public RToken next()
   {
      pos_++ ;
      ensureValidIndex() ;
      return currentToken() ;
   }
   
   public RToken prev()
   {
      pos_-- ;
      ensureValidIndex() ;
      return currentToken() ;
   }
   
   public void moveTo(int index)
   {
      if (index < -1 || index > tokens_.size())
         throw new ArrayIndexOutOfBoundsException() ;

      pos_ = index ;
   }
   
   public void moveToBOD()
   {
      pos_ = -1 ;
   }
   
   public void moveToEOD()
   {
      pos_ = tokens_.size() ;
   }
   
   private void ensureValidIndex()
   {
      pos_ = Math.min(Math.max(-1, pos_), tokens_.size()) ;
   }
   
   private final ArrayList<RToken> tokens_ ;
   private int pos_ = -1 ;
}
