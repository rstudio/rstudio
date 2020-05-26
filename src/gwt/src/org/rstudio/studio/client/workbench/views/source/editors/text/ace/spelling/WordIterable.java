/*
 * WordIterable.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.spelling.CharClassifier.CharClass;

import java.util.Iterator;

public class WordIterable implements Iterable<Range>
{
   public WordIterable(EditSession session,
                       TokenPredicate checkableToken,
                       CharClassifier wordChar,
                       Position start)
   {
      this(session, checkableToken, wordChar, start, null);
   }


   public WordIterable(EditSession session,
                       TokenPredicate checkableToken,
                       CharClassifier wordChar,
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
      charClassifier_ = wordChar;
      start_ = start;
      end_ = end;
   }

   @Override
   public Iterator<Range> iterator()
   {
      return new RangeIterator(session_,
                               isCheckableToken_,
                               charClassifier_,
                               start_,
                               end_);
   }

   private final EditSession session_;
   private final TokenPredicate isCheckableToken_;
   private final CharClassifier charClassifier_;
   private final Position start_;
   private final Position end_;
}

class RangeIterator implements Iterator<Range>
{
   public RangeIterator(EditSession session,
                        TokenPredicate isCheckableToken,
                        CharClassifier charClassifier,
                        Position start,
                        Position end)
   {
      start_ = start;
      end_ = end;
      isCheckableToken_ = isCheckableToken;
      charClassifier_ = charClassifier;

      tokenIterator_ = TokenIterator.create(session, start.getRow(),
                                            start.getColumn());
      currentValue_ = "";
      initialize();
   }

   private void initialize()
   {
      Token token = tokenIterator_.getCurrentToken();
      if (token != null && isCheckableToken_.test(token,
                                                  tokenIterator_.getCurrentTokenRow(),
                                                  tokenIterator_.getCurrentTokenColumn()))
      {
         currentValue_ = token.getValue();
         tokenPos_ = 0;

         if (tokenIterator_.getCurrentTokenRow() == start_.getRow()
               && tokenIterator_.getCurrentTokenColumn() < start_.getColumn())
         {
            // If start_ is inside the current token, skip over any words that
            // end before start_

            for (Range range; null != (range = nextWord()); )
            {
               if (range.getEnd().isAfter(start_))
               {
                  tokenPos_ = range.getStart().getColumn() -
                              tokenIterator_.getCurrentTokenColumn();
                  break;
               }
            }
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

         if (isCheckableToken_.test(token,
                                    tokenIterator_.getCurrentTokenRow(),
                                    tokenIterator_.getCurrentTokenColumn()))
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
             charClassifier_.classify(currentValue_.charAt(tokenPos_)) != CharClass.Word)
      {
         tokenPos_++;
      }

      if (tokenPos_ == currentValue_.length())
         return null;

      int wordStart = tokenPos_++;

      while (tokenPos_ < currentValue_.length() &&
             charClassifier_.classify(currentValue_.charAt(tokenPos_)) != CharClass.NonWord)
      {
         tokenPos_++;
      }

      while (charClassifier_.classify(currentValue_.charAt(tokenPos_-1)) == CharClass.Boundary)
      {
         tokenPos_--;
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
   private final CharClassifier charClassifier_;
   private final TokenIterator tokenIterator_;

   private boolean ended_;
   private String currentValue_;
   private int tokenPos_;

   private Range nextValue_;
}
