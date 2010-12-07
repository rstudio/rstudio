/*
 * BraceMatcher.java
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
package org.rstudio.studio.client.workbench.views.console.shell;

/**
 * Abstracts the algorithm for walking over a range of tokens, to find the
 * token that matches the current token. (i.e. brace-matching)
 * @param <T>
 */
public abstract class BraceMatcher<T>
{
   public interface TokenSource<T>
   {
      T next();

      T prev();

      T currentToken();
   }

   protected BraceMatcher(boolean forward)
   {
      this(forward, -1);
   }

   /**
    * @param forward   The direction of traversal.
    * @param maxAbsValue   If the absolute value of the accumulated total
    *    exceeds this number, the matching operation will abort. If -1, then no
    *    limit.
    */
   protected BraceMatcher(boolean forward, int maxAbsValue)
   {
      forward_ = forward;
      maxAbsValue_ = maxAbsValue;
   }

   /**
    * Find the matching brace from a range.
    *
    * @param range A range that is positioned on a token that is of openType or
    *              closeType--this is the token we will attempt to find a match
    *              for.
    * @return The matching token, or null if none is found.
    */
   public T match(TokenSource<T> range)
   {
      assert value(range.currentToken()) != 0 : "Programmer error";

      int total = 0;
      do
      {
         total += value(range.currentToken());
         // If a huge value results, just terminate. We use this to
         // intentionally short-circuit without the use of exceptions.
         if (maxAbsValue_ > 0 && Math.abs(total) > maxAbsValue_)
            return null; 
      } while (total != 0 && advance(range) != null);

      return range.currentToken();
   }

   /**
    * Returns a value for this token--normally, 1 for "open" and -1 for "close".
    * The match() method calls this method with one token after another until
    * the value (which initially is != 0) becomes 0. 
    */
   protected abstract int value(T token);

   private T advance(TokenSource<T> range)
   {
      return forward_ ? range.next() : range.prev();
   }

   protected final boolean forward_ ;
   private final int maxAbsValue_;
}
