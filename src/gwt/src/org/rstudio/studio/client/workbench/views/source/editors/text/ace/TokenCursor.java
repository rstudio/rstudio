/*
 * TokenCursor.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

public class TokenCursor extends JavaScriptObject
{
   protected TokenCursor() {}
   
   public static native final TokenCursor create(CodeModel codeModel) /*-{
      return codeModel.getTokenCursor();
   }-*/;
   
   public native final TokenCursor cloneCursor() /*-{
      return this.cloneCursor();
   }-*/;
   
   public native final Position currentPosition() /*-{
      return this.currentPosition();
   }-*/;
   
   public native final Token currentToken() /*-{
      return this.currentToken();
   }-*/;
   
   public native final String currentValue() /*-{
      return this.currentValue();
   }-*/;
   
   public final boolean valueEquals(String value)
   {
      return currentToken().valueEquals(value);
   }
   
   public final boolean typeEquals(String type)
   {
      return currentToken().typeEquals(type);
   }
   
   public final boolean hasType(String... types)
   {
      return currentToken().hasType(types);
   }
   
   public final Token peek(int offset)
   {
      return offset > 0 ? peekFwd(offset) : peekBwd(-offset);
   }
   
   public final Token peekFwd(int offset)
   {
      TokenCursor clone = cloneCursor();
      for (int i = 0; i < offset; i++)
         if (!clone.moveToNextToken())
            return Token.create();
      return clone.currentToken();
   }
   
   public final Token peekBwd(int offset)
   {
      TokenCursor clone = cloneCursor();
      for (int i = 0; i < offset; i++)
         if (!clone.moveToPreviousToken())
            return Token.create();
      return clone.currentToken();
   }
   
   public final String nextValue(int offset)
   {
      TokenCursor clone = cloneCursor();
      for (int i = 0; i < offset; i++)
         if (!clone.moveToNextToken())
            return "";
      
      return clone.currentValue();
   }
   
   public final String nextValue()
   {
      return nextValue(1);
   }
   
   public native final String currentType() /*-{
      return this.currentType();
   }-*/;
   
   public native final boolean moveToNextToken() /*-{
      return !! this.moveToNextToken();
   }-*/;
   
   public native final boolean moveToPreviousToken() /*-{
      return !! this.moveToPreviousToken();
   }-*/;
   
   public native final void moveToStartOfRow(int row) /*-{
      this.moveToStartOfRow(row);
   }-*/;
   
   public native final void moveToEndOfRow(int row) /*-{
      this.moveToEndOfRow(row);
   }-*/;
   
   public native final boolean seekToNearestToken(Position position, int maxRow) /*-{
      return !! this.seekToNearestToken(position, maxRow);
   }-*/;
   
   public native final boolean bwdToNearestToken(Position position) /*-{
      return !! this.bwdToNearestToken(position);
   }-*/;
   
   public native final boolean moveBackwardOverMatchingParens() /*-{
      return !! this.moveBackwardOverMatchingParens();
   }-*/;
   
   public native final boolean moveToPosition(Position position) /*-{
      return this.moveToPosition(position);
   }-*/;
   
   public native final boolean moveToPosition(Position position, boolean rightInclusive) /*-{
      return this.moveToPosition(position, rightInclusive);
   }-*/;
   
   public native final boolean findOpeningBracket(String token, boolean failOnOpenBrace) /*-{
      return this.findOpeningBracket(token, failOnOpenBrace);
   }-*/;
   
   public native final int findOpeningBracketCountCommas(String token, boolean failOnOpenBrace) /*-{
      return this.findOpeningBracketCountCommas(token, failOnOpenBrace);
   }-*/;
   
   public native final boolean findOpeningBracket(String[] tokens, boolean failOnOpenBrace) /*-{
      return this.findOpeningBracket(tokens, failOnOpenBrace);
   }-*/;
   
   public native final int findOpeningBracketCountCommas(String[] tokens, boolean failOnOpenBrace) /*-{
      return this.findOpeningBracketCountCommas(tokens, failOnOpenBrace);
   }-*/;
   
   public native final boolean bwdToMatchingToken() /*-{
      return this.bwdToMatchingToken();
   }-*/;
   
   public native final boolean fwdToMatchingToken() /*-{
      return this.fwdToMatchingToken();
   }-*/;
   
   public native final boolean isFirstSignificantTokenOnLine() /*-{
      return this.isFirstSignificantTokenOnLine();
   }-*/;
   
   public native final int getRow() /*-{
      return this.$row;
   }-*/;
   
   public native final int getOffset() /*-{
      return this.$offset;
   }-*/;
   
   
   public native final void setRow(int row) /*-{
      this.$row = row;
   }-*/;
   
   public native final void setOffset(int offset) /*-{
      this.$offset = offset;
   }-*/;
   
   public native final boolean findStartOfEvaluationContext() /*-{
      return this.findStartOfEvaluationContext();
   }-*/;
   
   public native final boolean moveToStartOfCurrentStatement() /*-{
      return this.moveToStartOfCurrentStatement &&
             this.moveToStartOfCurrentStatement();
   }-*/;
   
   public native final boolean moveToEndOfCurrentStatement() /*-{
      return this.moveToEndOfCurrentStatement &&
             this.moveToEndOfCurrentStatement();
   }-*/;
   
   public native final boolean isLookingAtBinaryOp() /*-{
      return this.isLookingAtBinaryOp();
   }-*/;
   
   public native final boolean isLeftAssign() /*-{
      var value = this.currentValue();
      return value === "<-" || value === "=";
   }-*/;
   
   public native final boolean isLeftBracket() /*-{
      var value = this.currentValue();
      return value === "(" || value === "[" || value === "{";
   }-*/;
   
   public native final boolean isRightBracket() /*-{
      var value = this.currentValue();
      return value === ")" || value === "]" || value === "}";
   }-*/;
   
   public native final boolean isExtractionOperator() /*-{
      var value = this.currentValue();
      return value === "$" || value === "@" || value === "?" || value === "~";
   }-*/;
   
   public final boolean moveToActiveFunction()
   {
      TokenCursor clone = cloneCursor();
      if (clone.moveToNextToken() && clone.currentValue() == "(")
         return true;
      
      clone = cloneCursor();
      if (TokenUtils.isRightBracket(clone))
      {
         if (!clone.bwdToMatchingToken())
            return false;
         
         if (!clone.moveToPreviousToken())
            return false;
      }
      
      if (!clone.findOpeningBracket("(", true))
         return false;
      
      if (!clone.moveToPreviousToken())
         return false;
      
      setRow(clone.getRow());
      setOffset(clone.getOffset());
      return true;
   }

   public final boolean isWithinFunctionCall()
   {
      TokenCursor clone = cloneCursor();
      do
      {
         if (clone.peekBwd(1).isValidForFunctionCall())
            return true;
         
         if (!clone.moveToPreviousToken())
            return false;
         
      } while (clone.findOpeningBracket("(", false));
      return false;
   }
   
   public final boolean isWithinFunctionDefinitionArgumentList()
   {
      TokenCursor clone = cloneCursor();
      do
      {
         if (clone.peekBwd(1).valueEquals("function"))
            return true;
      } while (clone.findOpeningBracket("(", false));
      return false;
   }
   
}

