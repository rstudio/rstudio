/*
 * TextEditingTargetScopeHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.regex.Pattern.ReplaceOperation;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList.ScopePredicate;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

public class TextEditingTargetScopeHelper
{
   public TextEditingTargetScopeHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
   }

   public Scope getCurrentSweaveChunk()
   {
      return getCurrentSweaveChunk(null);
   }

   public Scope getCurrentSweaveChunk(Position position)
   {
      if (position != null)
         return docDisplay_.getChunkAtPosition(position);
      else
         return docDisplay_.getCurrentChunk();
   }

   private class SweaveIncludeContext
   {
      private SweaveIncludeContext()
      {
         scopeList_ = new ScopeList(docDisplay_);
         scopeList_.selectAll(ScopeList.CHUNK);
      }

      public String getSweaveChunkText(final Scope chunk, Range range)
      {
         String text = docDisplay_.getCode(range.getStart(), range.getEnd());
         return Pattern.create("^<<(.*?)>>.*").replaceAll(text, new ReplaceOperation()
         {
            @Override
            public String replace(Match m)
            {
               String label = m.getGroup(1).trim();
               Scope included = getScopeByChunkLabel(label,
                                                     chunk.getPreamble());
               if (included == null)
                  return m.getValue();
               else
                  return getSweaveChunkText(included,
                                            getSweaveChunkInnerRange(included));
            }
         });
      }

      private Scope getScopeByChunkLabel(String label, Position beforeHere)
      {
         for (Scope s : scopeList_)
         {
            if (beforeHere != null
                && s.getPreamble().isAfterOrEqualTo(beforeHere))
               return null;
            if (StringUtil.notNull(s.getChunkLabel()).equals(label))
               return s;
         }
         return null;
      }

      private final ScopeList scopeList_;
   }

   public String getSweaveChunkText(Scope chunk)
   {
      return getSweaveChunkText(chunk, null);
   }

   public String getSweaveChunkText(Scope chunk, Range range)
   {
      if (range == null)
         range = getSweaveChunkInnerRange(chunk);

      assert chunk.getPreamble().isBeforeOrEqualTo(range.getStart())
            && chunk.getEnd().isAfterOrEqualTo(range.getEnd());

      return new SweaveIncludeContext().getSweaveChunkText(chunk, range);
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

   public Scope[] getPreviousSweaveChunks()
   {
      return getSweaveChunks(null, PREVIOUS_CHUNKS);
   }

   public Scope[] getSweaveChunks(Position startPosition, 
         final int which)
   {
      // provide default position based on selection if necessary
      final Position position = startPosition != null ? 
                                   startPosition :
                                   docDisplay_.getSelectionStart();
                                          
      ScopeList scopeList = new ScopeList(docDisplay_);
      scopeList.selectAll(ScopeList.CHUNK);
      scopeList.selectAll(new ScopePredicate() {

         @Override
         public boolean test(Scope scope)
         {
            if (!scope.isChunk())
               return false;
            
            int dir = scope.getEnd().compareTo(position);
            if (which == PREVIOUS_CHUNKS)
               return dir < 0;
            else
               return dir > 0;
         }
      });
      
      return scopeList.getScopes();   
   }
   
   public Scope getNextSweaveChunk()
   {
      ScopeList scopeList = new ScopeList(docDisplay_);
      scopeList.selectAll(ScopeList.CHUNK);
      final Position selectionEnd = docDisplay_.getSelectionEnd();
      return scopeList.findFirst(new ScopePredicate()
      {
         @Override
         public boolean test(Scope scope)
         {
            return scope.getPreamble().compareTo(selectionEnd) > 0;
         }
      });
   }

   public Scope getNextFunction(final Position position)
   {
      ScopeList scopeList = new ScopeList(docDisplay_);
      scopeList.selectAll(ScopeList.FUNC);
      return scopeList.findFirst(new ScopePredicate()
      {
         @Override
         public boolean test(Scope scope)
         {
            return scope.getPreamble().compareTo(position) > 0;
         }
      });
   }

   public Scope getPreviousFunction(final Position position)
   {
      ScopeList scopeList = new ScopeList(docDisplay_);
      scopeList.selectAll(ScopeList.FUNC);
      return scopeList.findLast(new ScopePredicate()
      {
         @Override
         public boolean test(Scope scope)
         {
            return scope.getPreamble().compareTo(position) < 0;
         }
      });
   }
   
   public final static int PREVIOUS_CHUNKS  = 0;
   public final static int FOLLOWING_CHUNKS = 1;

   private DocDisplay docDisplay_;
}
