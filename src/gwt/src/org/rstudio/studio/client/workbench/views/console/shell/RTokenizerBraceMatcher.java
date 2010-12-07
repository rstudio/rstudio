/*
 * RTokenizerBraceMatcher.java
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

import org.rstudio.studio.client.common.r.RToken;
import org.rstudio.studio.client.common.r.RTokenRange;

public class RTokenizerBraceMatcher extends BraceMatcher<RToken>
{
   private class Source implements BraceMatcher.TokenSource<RToken>
   {
      public Source(RTokenRange range)
      {
         range_ = range;
      }

      public RToken next()
      {
         return range_.next();
      }

      public RToken prev()
      {
         return range_.prev();
      }

      public RToken currentToken()
      {
         return range_.currentToken();
      }

      private final RTokenRange range_;
   }

   /**
    * @param forward   The direction of traversal.
    * @param openType  The token type that has "open" semantics.
    * @param closeType The token type that has "close" semantics.
    */
   private RTokenizerBraceMatcher(boolean forward, int openType, int closeType)
   {
      super(forward);
      this.openType_ = openType;
      this.closeType_ = closeType;
   }

   public RToken match(RTokenRange range)
   {
      return match(new Source(range));
   }

   @Override
   protected int value(RToken token)
   {
      return (token.getTokenType() == openType_) ? 1
            : (token.getTokenType() == closeType_) ? -1 : 0 ;
   }

   public static RTokenizerBraceMatcher createForToken(int tokenType)
   {
      switch (tokenType)
      {
         case RToken.LBRACE:
            return new RTokenizerBraceMatcher(true,
                                              RToken.LBRACE,
                                              RToken.RBRACE);
         case RToken.LBRACKET:
            return new RTokenizerBraceMatcher(true,
                                              RToken.LBRACKET,
                                              RToken.RBRACKET);
         case RToken.LPAREN:
            return new RTokenizerBraceMatcher(true,
                                              RToken.LPAREN,
                                              RToken.RPAREN);
         case RToken.LDBRACKET:
            return new RTokenizerBraceMatcher(true,
                                              RToken.LDBRACKET,
                                              RToken.RDBRACKET);
         case RToken.RBRACE:
            return new RTokenizerBraceMatcher(false,
                                              RToken.LBRACE,
                                              RToken.RBRACE);
         case RToken.RBRACKET:
            return new RTokenizerBraceMatcher(false,
                                              RToken.LBRACKET,
                                              RToken.RBRACKET);
         case RToken.RPAREN:
            return new RTokenizerBraceMatcher(false,
                                              RToken.LPAREN,
                                              RToken.RPAREN);
         case RToken.RDBRACKET:
            return new RTokenizerBraceMatcher(false,
                                              RToken.LDBRACKET,
                                              RToken.RDBRACKET);
         default:
            return null;
      }
   }

   private final int openType_;
   private final int closeType_;
}
