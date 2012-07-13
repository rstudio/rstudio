/*
 * SourceSelection.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.model;


public class SourceSelection
{
   public SourceSelection(int beginRow,
                          int beginColumn, 
                          int endRow, 
                          int endColumn) 
   {
      beginRow_ = beginRow;
      beginColumn_ = beginColumn;
      endRow_ = endRow;
      endColumn_ = endColumn;
   } 
   
   public SourceSelection(String sourceSelection)
   {
      if (sourceSelection.length() > 0)
      {
         String[] pieces = sourceSelection.split("\\|");
         int len = pieces.length;
         beginRow_ = len > 0 ? Integer.parseInt(pieces[0]) : 0;
         beginColumn_ = len > 1 ? Integer.parseInt(pieces[1]) : 0;
         endRow_ = len > 2 ? Integer.parseInt(pieces[2]) : 0;
         endColumn_ = len > 3 ? Integer.parseInt(pieces[3]) : 0;
      }
      else
      {
         beginRow_ = 0;
         beginColumn_ = 0;
         endRow_ = 0;
         endColumn_ = 0;
      }
   }
   
   public String encode()
   {
      StringBuilder result = new StringBuilder();
      result.append(beginRow_).append('|')
            .append(beginColumn_).append('|')
            .append(endRow_).append('|')
            .append(endColumn_);
      return result.toString();
   }
   
   public int beginRow() { return beginRow_; }
   public int beginColumn() { return beginColumn_; }
   public int endRow() { return endRow_; }
   public int endColumn() { return endColumn_; }
   
   public boolean isEmpty()
   {
      return beginRow_ == 0 &&
             beginColumn_ == 0 &&
             endRow_ == 0 &&
             endColumn_ == 0;
   }
   
   @Override
   public boolean equals(Object other)
   {
      if (this == other) 
         return true;
      if ( !(other instanceof SourceSelection)) 
         return false;
      SourceSelection otherSel = (SourceSelection)other;
      
      return beginRow_ == otherSel.beginRow_ &&
             beginColumn_ == otherSel.beginColumn_ &&
             endRow_ == otherSel.endRow_ &&
             endColumn_ == otherSel.endColumn_;
   }

   @Override
   public int hashCode()
   {
      int hashCode = beginRow_;
      hashCode *= 31;
      hashCode += beginColumn_;
      hashCode *= 31;
      hashCode += endRow_;
      hashCode *= 31;
      hashCode += endColumn_;
      hashCode *= 31;
      return hashCode;
   }
   
   private final int beginRow_;
   private final int beginColumn_;
   private final int endRow_;
   private final int endColumn_;
}
