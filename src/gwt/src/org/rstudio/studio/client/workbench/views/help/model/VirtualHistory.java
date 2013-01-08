/*
 * VirtualHistory.java
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
package org.rstudio.studio.client.workbench.views.help.model;

import java.util.ArrayList;

public class VirtualHistory
{
   public void navigate(String url)
   {
      // truncate the stack to the current pos
      while (stack_.size() > pos_ + 1)
         stack_.remove(stack_.size() - 1) ;
      
      stack_.add(url) ;
      pos_ = stack_.size() - 1;
      
      dump() ;
   }

   public String back()
   {
      if (pos_ <= 0)
         return null ;
      pos_-- ;

      dump() ;

      return stack_.get(pos_) ;
   }
   
   public String forward()
   {
      if (pos_ >= stack_.size() - 1)
         return null ;
      pos_++ ;
      
      dump() ;

      return stack_.get(pos_) ;
   }
   
   private void dump()
   {
      /*
      StringBuffer out = new StringBuffer() ;
      for (int i = stack_.size() - 1; i >= 0; i--)
      {
         if (i == pos_)
            out.append('*') ;
         out.append(stack_.get(i)) ;
         out.append('\n') ;
      }
      Debug.log(out.toString()) ;
      */
   }
   
   private ArrayList<String> stack_ = new ArrayList<String>() ;
   private int pos_ = -1 ;
}
