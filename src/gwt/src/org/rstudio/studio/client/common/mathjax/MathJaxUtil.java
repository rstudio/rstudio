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

import java.util.ArrayList;
import java.util.List;

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
      
      // avoid case where token iterator moves back across lines
      // to discover a latex block
      Token startToken = startIt.moveToPosition(pos);
      if (startToken != null &&
          startToken.hasAllTypes("latex", "end") &&
          startIt.getCurrentTokenRow() != pos.getRow())
      {
         return null;
      }
      
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
      if (range == null)
         return false;
      
      Token startToken = docDisplay.getTokenAt(range.getStart().getRow(), 0);
      if (startToken == null || !startToken.getValue().equals("$$"))
         return false;
      
      Token endToken = docDisplay.getTokenAt(range.getEnd().getRow(), 0);
      if (endToken == null || !endToken.getValue().equals("$$"))
         return false;
      
      return true;
   }
   
   public static List<Range> findLatexChunks(DocDisplay docDisplay)
   {
      docDisplay.tokenizeDocument();
      List<Range> ranges = new ArrayList<Range>();
      
      Position startPos = null;
      for (int i = 0, n = docDisplay.getRowCount(); i < n; i++)
      {
         Position pos = Position.create(i, 0);
         Token token = docDisplay.getTokenAt(Position.create(i, 0));
         if (token == null)
            continue;
         
         if (token.hasAllTypes("latex", "begin"))
         {
            startPos = pos;
            continue;
         }
         
         if (token.hasAllTypes("latex", "end"))
         {
            ranges.add(Range.fromPoints(startPos, Position.create(i, 2)));
            continue;
         }
      }
      
      return ranges;
   }
}
