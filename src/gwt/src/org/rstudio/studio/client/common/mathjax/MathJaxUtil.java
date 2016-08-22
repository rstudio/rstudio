/*
 * MathJaxUtil.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.common.mathjax;

import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenIterator;

public class MathJaxUtil
{
   public static Range getLatexRange(DocDisplay docDisplay)
   {
      return getLatexRange(docDisplay, docDisplay.getCursorPosition());
   }
   
   public static Range getLatexRange(DocDisplay docDisplay, Position pos)
   {
      if (pos == null)
         pos = docDisplay.getCursorPosition();
      
      // find start of latex block
      TokenIterator startIt = docDisplay.createTokenIterator();
      
      for (Token token = startIt.moveToPosition(pos);
           token != null;
           token = startIt.stepBackward())
      {
         if (!token.hasType("latex"))
            return null;
         
         if (token.hasType("begin"))
            break;
      }
      
      // find end of latex block
      TokenIterator endIt = docDisplay.createTokenIterator();
      for (Token token = endIt.moveToPosition(pos);
           token != null;
           token = endIt.stepForward())
      {
         if (!token.hasType("latex"))
            return null;
         
         if (token.hasType("end"))
            break;
      }
      
      if (startIt.getCurrentToken() == null || endIt.getCurrentToken() == null)
         return null;
      
      Position startPos = startIt.getCurrentTokenPosition();
      Position endPos = endIt.getCurrentTokenPosition();
      endPos.setColumn(endPos.getColumn() + endIt.getCurrentToken().getValue().length());
      
      return Range.fromPoints(startPos, endPos);
   }
   
   public static boolean isSelectionWithinLatexChunk(DocDisplay docDisplay)
   {
      Range range = getLatexRange(docDisplay);
      
      Token startToken = docDisplay.getTokenAt(range.getStart().getRow(), 0);
      if (startToken == null || !startToken.getValue().equals("$$"))
         return false;
      
      Token endToken = docDisplay.getTokenAt(range.getEnd().getRow(), 0);
      if (endToken == null || !endToken.getValue().equals("$$"))
         return false;
      
      return true;
      
   }
}
