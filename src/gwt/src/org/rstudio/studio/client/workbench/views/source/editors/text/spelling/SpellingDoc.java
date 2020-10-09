/*
 * SpellingDoc.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import org.rstudio.core.client.Rectangle;

public interface SpellingDoc
{ 
   public interface Anchor
   {
      int getPosition();
   }
   
   public class WordRange
   {
      public WordRange(int start, int end)
      {
         this.start = start;
         this.end = end;
      }
      public int start;
      public int end;
   }
   
   Iterable<WordRange> getWords(int start, int end);
   Anchor createAnchor(int position);
   
   boolean shouldCheck(WordRange range);
   
   void setSelection(WordRange range);
   String getText(WordRange range);
   void replaceSelection(String text);
   
   int getCursorPosition();
   int getSelectionStart();
   int getSelectionEnd();
   
   Rectangle getCursorBounds(); 
   void moveCursorNearTop();
   
   void dispose();
}
 
  
  

