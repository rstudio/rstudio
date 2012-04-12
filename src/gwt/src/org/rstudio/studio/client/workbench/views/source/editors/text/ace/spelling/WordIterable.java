/*
 * WordIterable.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.*;

import java.util.Iterator;

public class WordIterable implements Iterable<Range>
{
   public WordIterable(EditSession session,
                       TokenPredicate checkableToken,
                       CharPredicate wordChar,
                       Position start)
   {
      this(session, checkableToken, wordChar, start, null);
   }


   public WordIterable(EditSession session,
                       TokenPredicate checkableToken,
                       CharPredicate wordChar,
                       Position start,
                       Position end)
   {
      if (end == null)
      {
         int lastRow = session.getLength() - 1;
         if (lastRow < 0)
            end = Position.create(0, 0);
         else
            end = Position.create(lastRow, session.getLine(lastRow).length());
      }

      session_ = session;
      isCheckableToken_ = checkableToken;
      isWordChar_ = wordChar;
      start_ = start;
      end_ = end;
   }

   @Override
   public Iterator<Range> iterator()
   {
      return new RangeIterator(session_,
                               isCheckableToken_,
                               isWordChar_,
                               start_,
                               end_);
   }

   private final EditSession session_;
   private final TokenPredicate isCheckableToken_;
   private final CharPredicate isWordChar_;
   private final Position start_;
   private final Position end_;
}

class RangeIterator implements Iterator<Range>
{
   public RangeIterator(EditSession session,
                        TokenPredicate isCheckableToken,
                        CharPredicate isWordChar,
                        Position start,
                        Position end)
   {
      start_ = start;
      end_ = end;
      isCheckableToken_ = isCheckableToken;
      isWordChar_ = isWordChar;

      tokenIterator_ = TokenIterator.create(session, start.getRow(),
                                            start.getColumn());
      currentValue_ = "";
      initialize();
   }

   private void initialize()
   {
      Token token = tokenIterator_.getCurrentToken();
      if (token != null && isCheckableToken_.test(token))
      {
         if (tokenIterator_.getCurrentTokenRow() == start_.getRow()
               && tokenIterator_.getCurrentTokenColumn() < start_.getColumn())
         {
            int endCol =
                  tokenIterator_.getCurrentTokenColumn() +
                  token.getValue().length();

            currentValue_ = token.getValue();
            tokenPos_ = start_.getColumn() -
                        tokenIterator_.getCurrentTokenColumn();

            if (tokenPos_ < currentValue_.length() &&
                isWordChar_.test(currentValue_.charAt(tokenPos_)))
            {
               while (tokenPos_ > 0
                      && isWordChar_.test(currentValue_.charAt(tokenPos_-1)))
               {
                  tokenPos_--;
               }
            }
         }
         else
         {
            currentValue_ = token.getValue();
            tokenPos_ = 0;
         }
      }
      advance();
   }

   private boolean nextToken()
   {
      if (ended_)
         return false;

      while (true)
      {
         Token token = tokenIterator_.stepForward();
         if (token == null)
         {
            ended_ = true;
            return false;
         }

         if (isCheckableToken_.test(token))
         {
            currentValue_ = token.getValue();
            tokenPos_ = 0;
            return true;
         }
      }
   }

   private Range nextWord()
   {
      if (ended_)
         return null;

      while (tokenPos_ < currentValue_.length() &&
             !isWordChar_.test(currentValue_.charAt(tokenPos_)))
      {
         tokenPos_++;
      }

      if (tokenPos_ == currentValue_.length())
         return null;

      int wordStart = tokenPos_++;

      while (tokenPos_ < currentValue_.length() &&
             isWordChar_.test(currentValue_.charAt(tokenPos_)))
      {
         tokenPos_++;
      }

      int row = tokenIterator_.getCurrentTokenRow();
      Position startPos = Position.create(
            row, tokenIterator_.getCurrentTokenColumn() + wordStart);
      Position endPos = Position.create(
            row, tokenIterator_.getCurrentTokenColumn() + tokenPos_);

      if (startPos.isAfterOrEqualTo(end_))
      {
         ended_ = true;
         return null;
      }

      return Range.fromPoints(startPos, endPos);
   }

   private void advance()
   {
      do
      {
         nextValue_ = nextWord();
         if (nextValue_ != null)
         {
            return;
         }
      } while (nextToken());
   }

   @Override
   public boolean hasNext()
   {
      return nextValue_ != null;
   }

   @Override
   public Range next()
   {
      Range result = nextValue_;
      advance();
      return result;
   }

   @Override
   public void remove()
   {
      throw new UnsupportedOperationException();
   }

   private final Position start_;
   private final Position end_;
   private final TokenPredicate isCheckableToken_;
   private final CharPredicate isWordChar_;
   private final TokenIterator tokenIterator_;

   private boolean ended_;
   private String currentValue_;
   private int tokenPos_;

   private Range nextValue_;
}
