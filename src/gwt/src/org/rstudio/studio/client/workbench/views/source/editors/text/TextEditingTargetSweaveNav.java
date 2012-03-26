/*
 * TextEditingTargetSweaveNav.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JsArray;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

public class TextEditingTargetSweaveNav
{
   public TextEditingTargetSweaveNav(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
   }

   public Scope getCurrentSweaveChunk()
   {
      return docDisplay_.getCurrentChunk();
   }

   public Scope getNextSweaveChunk()
   {
      Position cursorPosition = docDisplay_.getSelectionEnd();

      JsArray<Scope> scopeTree = docDisplay_.getScopeTree();
      for (int i = 0; i < scopeTree.length(); i++)
      {
         if (scopeTree.get(i).getPreamble().compareTo(cursorPosition) >= 0)
            return scopeTree.get(i);
      }
      return null;
   }

   public Range getSweaveChunkInnerRange(Scope chunk)
   {
      if (chunk == null)
         return null;

      assert chunk.isChunk();

      Position start = Position.create(chunk.getPreamble().getRow() + 1, 0);
      Position end = Position.create(chunk.getEnd().getRow(), 0);
      if (start.getRow() != end.getRow())
      {
         end = Position.create(end.getRow()-1,
                               docDisplay_.getLine(end.getRow()-1).length());
      }
      return Range.fromPoints(start, end);
   }

   private DocDisplay docDisplay_;
}
