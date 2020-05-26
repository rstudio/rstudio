/*
 * TextEditingTargetReformatHelper.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.Pair;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Tokenizer;

public class TextEditingTargetReformatHelper
{
   
   public TextEditingTargetReformatHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
   }
   
   private static final Pattern ENDS_WITH_NEWLINE =
         Pattern.create("\\n\\s*$", "");

   private static final Pattern STARTS_WITH_NEWLINE =
         Pattern.create("^\\s*\\n", "");
   
   class SimpleTokenCursor {
      
      public SimpleTokenCursor(List<Token> tokens)
      {
         this(tokens, 0, tokens.size());
      }
      
      private SimpleTokenCursor(List<Token> tokens,
                                int offset,
                                int n)
      {
         complements_ = new HashMap<String, String>();
         
         complements_.put("(", ")");
         complements_.put("[", "]");
         complements_.put("{", "}");
         complements_.put("[[", "]]");
         
         complements_.put(")", "(");
         complements_.put("]", "[");
         complements_.put("}", "{");
         complements_.put("]]", "[[");
         
         tokens_ = tokens;
         offset_ = offset;
         n_ = n;
      }
      
      private SimpleTokenCursor(List<Token> tokens,
                                int offset,
                                int n,
                                HashMap<String, String> complements)
      {
         tokens_ = tokens;
         offset_ = offset;
         n_ = n;
         complements_ = complements;
      }
      
      public int getOffset()
      {
         return offset_;
      }
      
      public SimpleTokenCursor clone()
      {
         return new SimpleTokenCursor(
               tokens_,
               offset_,
               n_,
               complements_);
      }
      
      public SimpleTokenCursor clone(int offset)
      {
         return new SimpleTokenCursor(
               tokens_,
               offset_ + offset,
               n_,
               complements_);
      }
      
      public boolean moveToNextToken()
      {
         if (offset_ >= n_ - 1)
            return false;
         ++offset_;
         return true;
      }
      
      public boolean moveToPreviousToken()
      {
         if (offset_ <= 0)
            return false;
         --offset_;
         return true;
      }
      
      public boolean moveToNextSignificantToken()
      {
         if (!moveToNextToken())
            return false;
         
         while (isWhitespaceOrNewline())
            if (!moveToNextToken())
               return false;
         
         return true;
      }
      
      public boolean moveToPreviousSignificantToken()
      {
         if (!moveToPreviousToken())
            return false;
         
         while (isWhitespaceOrNewline())
            if (!moveToPreviousToken())
               return false;
         
         return true;
      }
      
      public Token previousToken()
      {
         SimpleTokenCursor clone = clone();
         if (!clone.moveToPreviousToken())
            return Token.create();
         return clone.currentToken();
      }
      
      public Token previousSignificantToken()
      {
         SimpleTokenCursor clone = clone();
         if (!clone.moveToPreviousToken())
            return Token.create();
         
         while (clone.isWhitespaceOrNewline())
            if (!clone.moveToPreviousToken())
               return Token.create();
         
         return clone.currentToken();
      }
      
      public Token nextToken()
      {
         SimpleTokenCursor clone = clone();
         if (!clone.moveToNextToken())
            return Token.create();
         
         return clone.currentToken();
      }
      
      public Token nextSignificantToken()
      {
         SimpleTokenCursor clone = clone();
         if (!clone.moveToNextToken())
            return Token.create();
         
         while (clone.isWhitespaceOrNewline())
            if (!clone.moveToNextToken())
               return Token.create();
         
         return clone.currentToken();
      }
      
      public SimpleTokenCursor peek(int offset)
      {
         int index = offset_ + offset;
         if (index < 0 || index >= n_)
         {
            ArrayList<Token> dummyTokens = new ArrayList<Token>();
            dummyTokens.add(Token.create("__ERROR__", "error", 0));
            return new SimpleTokenCursor(dummyTokens, 0, 1, complements_);
         }
         
         SimpleTokenCursor clone = clone();
         clone.offset_ = index;
         return clone;
      }
      
      private boolean isLeftBrace()
      {
         String value = currentValue();
         return value == "(" ||
                value == "[" ||
                value == "[[" ||
                value == "{";
      }
      
      private boolean isRightBrace()
      {
         String value = currentValue();
         return value == ")" ||
                value == "]" ||
                value == "]]" ||
                value == "}";
      }
      
      public boolean fwdToMatchingToken()
      {
         return fwdToMatchingToken(null);
      }
      
      public boolean fwdToMatchingToken(Mutable<Integer> counter)
      {
         String lhs = this.currentValue();
         if (!isLeftBrace())
            return false;
         
         boolean isCounterActive = true;
         
         Stack<String> braceStack = new Stack<String>();
         
         int stack = 0;
         String rhs = complements_.get(lhs);
         SimpleTokenCursor cursor = clone();
         while (cursor.moveToNextToken())
         {
            String value = cursor.currentValue();
            if (isCounterActive &&
                  counter != null &&
                  !cursor.isComment())
            {
               counter.set(counter.get() + value.replaceAll("\\s", "").length());
            }

            if (cursor.isLeftBrace())
            {
               braceStack.push(value);
               if (value == lhs)
                  stack++;
               isCounterActive = false;
            }
            else if (cursor.isRightBrace())
            {
               if (!braceStack.isEmpty())
                  braceStack.pop();

               isCounterActive = braceStack.isEmpty();

               if (value == rhs)
               {
                  if (stack == 0)
                  {
                     offset_ = cursor.offset_;
                     return true;
                  }
                  stack--;
               }
            }
         }
         
         return false;
      }
      
      public boolean bwdToMatchingToken()
      {
         String rhs = this.currentValue();
         if (!isRightBrace())
            return false;
         
         int stack = 0;
         String lhs = complements_.get(rhs);
         SimpleTokenCursor cursor = clone();
         
         while (cursor.moveToPreviousToken())
         {
            if (cursor.currentValue() == rhs)
            {
               stack++;
            } else if (cursor.currentValue() == lhs)
            {
               if (stack == 0)
               {
                  offset_ = cursor.offset_;
                  return true;
               }
               stack--;
            }
         }
         
         return false;
      }
      
      public void ensureNewlinePreceeds()
      {
         String value = getValue();
         String prev = peek(-1).getValue();
         
         if (ENDS_WITH_NEWLINE.test(prev) ||
             STARTS_WITH_NEWLINE.test(value))
            return;
             
         SimpleTokenCursor clone = clone();
         clone.moveToPreviousToken();
         clone.setValue(clone.getValue() + "\n");
      }
      
      public void ensureNewlineFollows()
      {
         String value = getValue();
         String next = peek(1).getValue();
         
         if (ENDS_WITH_NEWLINE.test(value) ||
             STARTS_WITH_NEWLINE.test(next))
            return;
         
         SimpleTokenCursor clone = clone();
         clone.moveToNextToken();
         clone.setValue("\n" + clone.getValue());
      }
      
      public void ensureSingleSpaceFollows()
      {
         setValue(getValue().replaceAll("\\s*$", ""));
         SimpleTokenCursor clone = clone();
         clone.moveToNextToken();
         clone.setValue(clone.getValue().replaceAll("^\\s*", ""));
         setValue(getValue() + " ");
      }
      
      public void ensureWhitespaceFollows()
      {
         String value = getValue();
         boolean mightWantNewline = value == "&&" ||
             value == "||" ||
             value == "&" ||
             value == "|" ||
             value == "<-" ||
             value == "<<-";
         
         if (mightWantNewline && 
             getCurrentLineLength() >= 70)
         {
            if (peek(1).getValue().indexOf('\n') == -1)
               setValue(getValue() + "\n");
         }
         else
         {
            String newlineOrSpace = getDistanceToPreviousNewline() >= 70 ?
                  "\n" :
                  " ";
            if (!peek(1).isWhitespaceOrNewline())
               setValue(currentValue() + newlineOrSpace);
         }
      }
      
      public void ensureWhitespacePreceeds()
      {
         if (!peek(-1).isWhitespaceOrNewline())
            setValue(" " + getValue());
      }
      
      public Token currentToken()
      {
         return tokens_.get(offset_);
      }
      
      public String currentValue()
      {
         return currentToken().getValue();
      }
      
      public String getComplement(String value)
      {
         return complements_.get(value);
      }
      
      public String getValue()
      {
         return currentValue();
      }
      
      public void setValue(String value)
      {
         currentToken().setValue(value);
      }
      
      public String valueAtOffset(int offset)
      {
         int index = offset_ + offset;
         if (index < 0 || index >= n_)
            return "";
         
         return tokens_.get(index).getValue();
      }
      
      public boolean hasType(String... targetTypes)
      {
         String tokenType = tokens_.get(offset_).getType();
         for (String targetType : targetTypes)
         {
            if (tokenType == targetType ||
                tokenType.contains(targetType + ".") ||
                tokenType.contains("." + targetType))
            {
               return true;
            }
         }
         return false;
      }
      
      public String currentType()
      {
         return tokens_.get(offset_).getType();
      }
      
      // NOTE: see 'r_highlight_rules.js' for token type info
      public boolean isWhitespaceOrNewline()
      {
         Token token = currentToken();
         return token.getType() == "text" &&
                token.getValue().matches("^[\\s\\n]*$");
      }
      
      public void trimWhitespaceFwd()
      {
         SimpleTokenCursor clone = clone();
         while (clone.isWhitespaceOrNewline())
         {
            clone.setValue("");
            if (!clone.moveToNextToken())
               return;
         }
      }
      
      public void trimWhitespaceBwd()
      {
         SimpleTokenCursor clone = clone();
         while (clone.isWhitespaceOrNewline())
         {
            clone.setValue("");
            if (!clone.moveToPreviousToken())
               return;
         }
      }
      
      public boolean isComment()
      {
         return tokens_.get(offset_).getType().indexOf("comment") != -1;
      }
      
      public boolean hasNewline()
      {
         return tokens_.get(offset_).getValue().indexOf('\n') != -1;
      }
      
      public boolean isKeyword()
      {
         return tokens_.get(offset_).getType() == "keyword";
      }
      
      public boolean isControlFlowKeyword()
      {
         return tokens_.get(offset_).getValue().matches(
               "^\\s*(?:if|else|try|for|while|repeat|break|next|function)\\s*$");
      }
      
      public boolean isOperator()
      {
         String type = tokens_.get(offset_).getType();
         return type == "keyword.operator" ||
                type == "keyword.operator.infix";
      }
      
      public int getCurrentLineLength()
      {
         int length = 0;
         SimpleTokenCursor bwdCursor = clone();
         
         while (bwdCursor.moveToPreviousToken())
         {
            String value = bwdCursor.getValue();
            int index = value.lastIndexOf('\n');
            
            if (index == -1)
               length += value.length();
            else
            {
               length += value.length() - index;
               break;
            }
         }
         
         SimpleTokenCursor fwdCursor = clone();
         do
         {
            String value = fwdCursor.currentValue();
            int index = value.indexOf('\n');
            
            if (index == -1)
               length += value.length();
            else
            {
               length += index;
               break;
            }
         } while (fwdCursor.moveToNextToken());
         
         return length;
      }
      
      public int getDistanceToPreviousNewline()
      {
         int distance = 0;
         SimpleTokenCursor clone = clone();
         while (clone.moveToPreviousToken())
         {
            String value = clone.getValue();
            int index = value.lastIndexOf('\n');
            if (index == -1)
               distance += value.length();
            else
            {
               distance += (value.length() - index);
               break;
            }
         }
         return distance;
      }
      
      @Override
      public boolean equals(Object object)
      {
         if (!(object instanceof SimpleTokenCursor))
            return false;
         
         return offset_ == ((SimpleTokenCursor) object).offset_;
      }
      
      private final List<Token> tokens_;
      private int offset_;
      private int n_;
      private HashMap<String, String> complements_;
   }
   
   // The main driver of the new line inserter.
   //
   // 'cursor': The current token cursor, unique to this block.
   // 'opener': The open brace ('[', '{', '(', '[['),
   // 'closer': The closing brace (']', '}', ')', ']]')
   // 'parenNestLevel': The nesting level within parentheses; e.g. '()'.
   //                   This is used to infer appropriate newline levels
   //                   for deeply nested function calls.
   // 'braceNestLevel': The number of braces encompassing this scope.
   // 'topLevel': Is this a top level cursor?
   void doInsertPrettyNewlines(SimpleTokenCursor cursor,
                               String opener,
                               String closer,
                               int parenNestLevel,
                               int braceNestLevel,
                               boolean topLevel)
   {
      // Root state == top level of document; no open braces yet
      // encountered.
      boolean rootState = parenNestLevel == 0 && opener.isEmpty();
      
      boolean newlineAfterComma = false;
      boolean newlineAfterBrace = false;
      
      int commaCount = 0;
      int equalsCount = 0;
      
      // We may override newline insertions in special cases to ensure
      // certain code structures remain intact, e.g.
      //
      //     lapply(x, function() { ... })
      //
      // We almost never want the anonymous function to lie on its own
      // line.
      boolean overrideNewlineInsertionAsFalse = false;
      
      String startValue = cursor.currentValue();
      SimpleTokenCursor beforeStartCursor = cursor.clone();
      beforeStartCursor.moveToPreviousSignificantToken();
      
      String prevSignificantValue = beforeStartCursor.getValue();
      
      // Trim whitespace following the 'opener' -- we may add it back later.
      if (!rootState)
         cursor.peek(1).trimWhitespaceFwd();
      
      // Scan through once to figure out whether we want to insert newlines.
      SimpleTokenCursor clone = cursor.clone();
      
      // Accumulate the length of the (non-whitespace)
      // tokens within this scope.
      int accumulatedLength = 0;
      
      while (clone.moveToNextToken())
      {
         if (clone.isComment())
            continue;
         
         accumulatedLength += clone.getValue().replaceAll("\\s", "").length();
         
         if (clone.currentType() == "text")
            commaCount += StringUtil.countMatches(
                  clone.currentValue(), ',');
         
         // If we encounter an (anonymous) function token, or an
         // opening brace, we prefer not inserting newlines (to preserve
         // structures like:
         //
         //    lapply(foo, function(x) { ... })
         //
         // or
         //
         //    tryCatch({
         //
         if (clone.currentValue() == "function")
            if (clone.previousSignificantToken().getValue().contains(","))
               overrideNewlineInsertionAsFalse = true;
         
         if (clone.currentValue() == "{")
         {
            SimpleTokenCursor peek = clone.clone();
            if (peek.moveToPreviousSignificantToken())
               if (peek.isLeftBrace())
                  overrideNewlineInsertionAsFalse = true;
         }
         
         // If we encounter an '=', presumedly
         // this is for a named function call.
         if (clone.currentValue() == "=")
         {
            equalsCount++;
            
            // If there is a function token ahead of the '=', we prefer
            // inserting newlines after braces, so that function objects
            // assigned within lists (or function calls) are placed on
            // their own line, e.g.
            //
            //  foo = list(
            //     y = function(...) { ... }
            //   )
            //
            if (clone.moveToNextSignificantToken())
            {
               if (clone.currentValue() == "function")
               {
                  newlineAfterBrace = true;
                  newlineAfterComma = true;
                  continue;
               }
            }
         }
         
         // If we encounter a '{' or '[', skip over -- we don't want to
         // enumerate things in 'child' scopes.
         if (clone.currentValue() == "{" ||
             clone.currentValue() == "[")
         {
            clone.fwdToMatchingToken();
            continue;
         }
         
         // If we encounter a '(', we will want to accumulate the length
         // of tokens in that scope. This, used alongside the nesting level,
         // helps us infer the appropriate place to insert newlines when
         // within nested function calls.
         if (clone.currentValue() == "(")
         {
            if (clone.moveToPreviousSignificantToken())
            {
               // For keywords, we prefer not accumulating -- this helps us
               // ensure we don't insert unnecessary newlines within
               // 'for', 'if', 'while' statements and the like.
               boolean isKeyword = clone.isKeyword();
               clone.moveToNextSignificantToken();
               if (isKeyword)
                  clone.fwdToMatchingToken();
               else
               {
                  Mutable<Integer> counter = new Mutable<Integer>(0);
                  clone.fwdToMatchingToken(counter);
                  accumulatedLength += counter.get();
               }
            }
            continue;
         }
         
         // If we find the associated closing paren, and we're not at the
         // top level, break. (The top level cursor gets to iterate over
         // the entire scope, sending out recursive searches as we encounter
         // opening parens.
         if (!topLevel && clone.currentValue() == closer)
            break;
      }
      
      // If this is a '{', and the immediately previous token is a ')',
      // insert some whitespace.
      // TODO: Allow preferences e.g. 1TBS, always newline before brace, etc?
      if (startValue == "{")
      {
         if (cursor.peek(-1).currentValue() == ")")
            cursor.peek(-1).setValue(") ");
      }
      
      // Heuristically decide if we want to insert newlines after
      // commas, parens. We 'score' whether we would like to insert
      // newlines after commas, and after braces.
      int commaScore = commaCount == 0 ?
            0 :
            (commaCount - 1) * 15;
      
      // Within a function argument list, we almost always want to insert
      // newlines after commas, expect for very short function argument
      // lists.
      if (prevSignificantValue == "function")
         commaScore += 20;
      
      // For scopes containing many `=`, we typically prefer inserting a
      // newline following a '('.
      int equalsScore = equalsCount == 0 ?
            0 :
            (equalsCount - 1) * 20;
      
      /*
      Debug.logToConsole("Accumulated length: " + accumulatedLength);
      Debug.logToConsole("Root state: " + rootState);
      Debug.logToConsole("Paren Nest level: " + parenNestLevel);
      Debug.logToConsole("Brace Nest level: " + braceNestLevel);
      Debug.logToConsole("Comma count: " + commaCount);
      Debug.logToConsole("Equals count: " + equalsCount);
      Debug.logToConsole("Cursor value: " + cursor.currentValue());
      Debug.logToConsole("Previous value: " + cursor.previousSignificantToken().getValue());
      Debug.logToConsole("Comma Score: " + commaScore);
      Debug.logToConsole("Equals score: " + equalsScore);
      */
      
      if (!rootState && startValue == "(")
      {
         if (accumulatedLength +
               commaScore +
               equalsScore +
               parenNestLevel * 20 +
               braceNestLevel * docDisplay_.getTabSize() >= 80)
         {
            newlineAfterBrace = true;
            parenNestLevel = 0;
         }
         
         if (accumulatedLength +
             commaScore +
             equalsScore +
             braceNestLevel * docDisplay_.getTabSize() >= 60)
            newlineAfterComma = true;
      }
      
      // If the previous token is a control-flow keyword, override the
      // 'newlineAfterParen' behaviour. We almost always prefer e.g.
      //
      //    if ( ... )
      //
      // over
      //
      //    if (
      //       ...
      //    )
      //
      if (cursor.moveToPreviousSignificantToken())
      {
         if (cursor.isControlFlowKeyword())
            newlineAfterBrace = false;
         
         // Special casing for tryCatch -- we prefer newlines everywhere.
         if (cursor.currentValue() == "tryCatch" &&
             accumulatedLength >= 20)
         {
            newlineAfterBrace = true;
            newlineAfterComma = true;
         }
         
         cursor.moveToNextSignificantToken();
      }
      
      if (overrideNewlineInsertionAsFalse)
      {
         newlineAfterComma = false;
         newlineAfterBrace = false;
      }
      
      SimpleTokenCursor peekFwd = cursor.peek(1);
      
      // Always insert newlines following '{'.
      // TODO: Allow very compact single line functions?
      if (startValue == "{")
      {
         if (cursor.peek(1).currentValue().indexOf('\n') == -1)
            cursor.setValue("{\n");
      }
      
      // Otherwise, use the heuristics. Note that it is okay to
      // collect multiple braces on one line, e.g.
      //
      //    apple(banana(cherry(danish(
      //       ...
      //    ))))
      //
      // so we do not want to indiscriminately insert newlines after
      // all parens.
      else if (newlineAfterBrace)
      {
         if (!rootState &&
             !peekFwd.isLeftBrace() &&
             !peekFwd.isRightBrace() &&
             peekFwd.getValue().indexOf('\n') == -1)
            cursor.setValue(opener + "\n");
      }
      else if (!rootState)
         peekFwd.trimWhitespaceFwd();
      
      // Now, walk through and replace tokens with appropriately white-spaced
      // versions.
      while (cursor.moveToNextToken())
      {
         if (cursor.isComment())
            continue;
         
         // Bail when we find a closing paren
         if (!rootState && cursor.isRightBrace())
            break;
         
         // Ensure a single space follows control flow statements
         if (cursor.currentValue() == "if" ||
             cursor.currentValue() == "for" ||
             cursor.currentValue() == "while" ||
             cursor.currentValue() == "repeat")
         {
            cursor.ensureSingleSpaceFollows();
         }
         
         // Ensure newlines around 'naked' else
         if (cursor.currentValue() == "else")
         {
            if (cursor.previousSignificantToken().getValue() != "}" &&
                cursor.getOffset() >= 2)
            {
               cursor.ensureNewlinePreceeds();
            }
            
            if (!(cursor.previousToken().getType().contains("comment") ||
                  cursor.previousToken().getValue().matches(".*\\s+")))
            {
               cursor.ensureWhitespacePreceeds();
            }
            
            String nextValue = cursor.nextSignificantToken().getValue();
            if (!(nextValue == "{" || nextValue == "if"))
               cursor.ensureNewlineFollows();
            
            continue;
         }
         
         // Ensure spaces around operators.
         if (cursor.isOperator())
         {
            String value = cursor.currentValue();
            
            // Prefer newlines after comparison operators within 'if'
            // statements when the enclosed selection is long
            if (prevSignificantValue == "if")
            {
               if (accumulatedLength >= 20 &&
                   value == "&&" ||
                   value == "||" ||
                   value == "&"  ||
                   value == "|")
               {
                  if (cursor.peek(1).currentValue().indexOf('\n') == -1)
                     cursor.setValue(cursor.currentValue() + "\n");
               }
            }
            
            else if (value == "$" ||
                value == "@" ||
                value == ":" ||
                value == "::" ||
                value == ":::")
            {
               cursor.peek(-1).trimWhitespaceBwd();
               cursor.peek(1).trimWhitespaceFwd();
            }
            else
            {
               // Unary operators are tricky, especially '-'. We need to make
               // sure that we don't e.g. transform this:
               //
               //    if (- x < - y)
               //
               // into
               //
               //    if (-x <- y)
               //
               // for example.
               if (value == "-" || value == "+" || value == "!")
               {
                  // Figure out if the current token is binary or unary.
                  SimpleTokenCursor previousCursor =
                        cursor.clone();
                  previousCursor.moveToPreviousSignificantToken();
                  
                  SimpleTokenCursor nextCursor =
                        cursor.clone();
                  nextCursor.moveToNextSignificantToken();
                  
                  boolean isBinary =
                    (previousCursor.isRightBrace() ||
                     previousCursor.currentType().indexOf("identifier") != -1 ||
                     previousCursor.currentType().indexOf("constant") != -1) &&
                    (nextCursor.isLeftBrace() ||
                     nextCursor.currentType().indexOf("operator") == -1 ||
                     nextCursor.currentType().indexOf("constant") != -1);
                  
                  // Binary operators should have whitespace surrounding.
                  if (isBinary)
                  {
                     cursor.ensureWhitespaceFollows();
                     cursor.ensureWhitespacePreceeds();
                  }
                  
                  // Unary operators should have no whitespace after the token,
                  // but __may__ have whitespace before; e.g. if the previous
                  // significant token is an operator. In other words,
                  // only trim whitespace if that token is not an operator.
                  else
                  {
                     cursor.peek(1).trimWhitespaceFwd();
                     if (previousCursor.currentType().indexOf("operator") == -1)
                     {
                        cursor.peek(-1).trimWhitespaceBwd();
                     }
                  }
               }
               
               // Regular case -- ensure whitespace surrounds binary operators.
               else
               {
                  cursor.ensureWhitespaceFollows();
                  cursor.ensureWhitespacePreceeds();
               }
            }
         }
         
         // Ensure spaces, or newlines, after commas, if so desired.
         if (cursor.currentValue() == ",")
         {
            if (newlineAfterComma &&
                cursor.peek(1).currentValue().indexOf('\n') == -1)
            {
               cursor.setValue(
                     cursor.currentValue().replaceAll(",(?!\\n)", ",\n"));
            }
            
            else if (!newlineAfterComma &&
                     !cursor.peek(1).isWhitespaceOrNewline())
            {
               cursor.setValue(", ");
            }
         }
            
            // Transform semi-colons into newlines.
            // TODO: Too destructive?
         if (cursor.currentValue() == ";")
         {
            cursor.setValue("\n");
         }
         
         // If we encounter an opening paren, recurse a new token cursor within,
         // and step over the block. This ensures that indentation rules are
         // consistent within a particular scope.
         if (cursor.currentValue() == "{" ||
             cursor.currentValue() == "(" ||
             cursor.currentValue() == "[" ||
             cursor.currentValue() == "[[")
         {
            // If we encounter a non-paren opener, this implies that we can
            // reset the function nesting level.
            if (startValue != "(")
               parenNestLevel = 0;
            
            // Otherwise, if we inserted newlines after parens for this
            // block, reset the nest level
            else
            {
               if (newlineAfterBrace)
                  parenNestLevel = 0;
            }
            
            // Increment the nest level for non-keyword '(' calls
            int incrementParenNest = startValue == "(" &&
                  !beforeStartCursor.isControlFlowKeyword() ? 1 : 0;
            
            /*
            Debug.logToConsole("Found opening paren");
            Debug.logToConsole("--------------------");
            Debug.logToConsole("-- Previous token: '" + cursor.previousSignificantToken().getValue() + "'");
            Debug.logToConsole("-- Recursing with: '" + cursor.currentValue() + "'");
            */
            
            // Update brace nest level
            int incrementBraceNest =
                  cursor.currentValue() == "{" ? 1 : 0;
            
            SimpleTokenCursor recursingCursor = cursor.clone();
            boolean success = cursor.fwdToMatchingToken();
            
            // Signal children scopes whether we'd prefer them to insert
            // newlines. TODO: less magic numbers
            
            doInsertPrettyNewlines(
                  recursingCursor,
                  recursingCursor.currentValue(),
                  recursingCursor.getComplement(recursingCursor.currentValue()),
                  parenNestLevel + incrementParenNest,
                  braceNestLevel + incrementBraceNest,
                  false);
            
            // If we weren't able to move the current active cursor to a
            // matching token, give up. This implies a different recursing
            // token will eventually hit the end of the token stream.
            if (!success)
               return;
         }
      }
      
      // If we ended on a ')' in e.g.
      //
      //    function(a, b) a
      //
      // that is, a function without an opening brace, ensure
      // a newline following the closing paren.
      // Similar logic applies for e.g.
      //
      //    if (foo) bar
      //
      if (cursor.currentValue() == ")" &&
          beforeStartCursor.isControlFlowKeyword() &&
          cursor.nextSignificantToken().getValue() != "{")
      {
         cursor.ensureNewlineFollows();
      }
      
      // If we ended on a ')', maybe insert newline before
      if (cursor.currentValue() == closer)
      {
         SimpleTokenCursor peek = cursor.peek(-1);
         if (newlineAfterBrace || cursor.currentValue() == "}")
         {
            if (peek.currentValue().indexOf('\n') == -1)
               peek.setValue(peek.currentValue() + "\n");
         }
         
         // Otherwise, ensure no whitespace before the token
         else peek.trimWhitespaceBwd();
      }
   }
   
   void insertPrettyNewlines()
   {
      AceEditor editor = (AceEditor) docDisplay_;
      if (editor != null)
      {
         String selectionText = docDisplay_.getSelectionValue();
         
         // Tokenize the selection and walk through and replace
         // TODO: Enable for other modes?
         Tokenizer tokenizer = Tokenizer.createRTokenizer();
         List<Token> tokens = tokenizer.tokenize(selectionText);
         
         SimpleTokenCursor cursor = new SimpleTokenCursor(tokens);
         
         // Set the initial state -- we recurse every time we encounter
         // an opening paren, so check for that initially.
         String lhs = "";
         String rhs = "";
         if (cursor.isLeftBrace())
         {
            lhs = cursor.currentValue();
            rhs = cursor.getComplement(lhs);
         }
         
         // TODO: Figure out current nesting level for the
         // active selection.
         doInsertPrettyNewlines(cursor, lhs, rhs, 0, 0, true);
         
         // Build the replacement from the modified token set
         StringBuilder builder = new StringBuilder();
         for (int i = 0; i < tokens.size(); i++)
            builder.append(tokens.get(i).getValue());
         String replacement = builder.toString();
         
         // Trim off trailing whitespace
         replacement = replacement.replaceAll("[ \\t]*\\n", "\n");
         replacement = replacement.replaceAll("\\n+$", "\n");
         
         docDisplay_.replaceSelection(replacement);
         docDisplay_.reindent(docDisplay_.getSelectionRange());
         
      }
   }
   
   void alignAssignment()
   {
      InputEditorSelection initialSelection =
            docDisplay_.getSelection();
      
      ArrayList<Pair<Integer, Integer>> ranges =
            getAlignmentRanges();
      
      if (ranges.isEmpty())
         return;
      
      for (Pair<Integer, Integer> range : ranges)
         doAlignAssignment(range.first, range.second);
      
      docDisplay_.setSelection(
            initialSelection.extendToLineStart().extendToLineEnd());
      
   }

   private void doAlignAssignment(int startRow,
                                  int endRow)
   {
      docDisplay_.setSelectionRange(Range.fromPoints(
            Position.create(startRow, 0),
            Position.create(endRow, docDisplay_.getLine(endRow).length())));
      
      String[] splat = docDisplay_.getSelectionValue().split("\n");
      
      ArrayList<String> starts = new ArrayList<String>();
      ArrayList<String> delimiters = new ArrayList<String>();
      ArrayList<String> ends = new ArrayList<String>();
      for (int i = 0; i < splat.length; i++)
      {
         String line = splat[i];
         String masked = StringUtil.maskStrings(splat[i]);
         Match match = DELIM_PATTERN.match(masked, 0);
         
         if (match == null)
         {
            starts.add(line);
            delimiters.add("");
            ends.add("");
         }
         else
         {
            String start = line.substring(0, match.getGroup(1).length());
            start = start.replaceAll("\\s*$", "");
            starts.add(start);
            
            delimiters.add(match.getGroup(2));
            
            int endOfDelim = match.getGroup(1).length() +
                  match.getGroup(2).length();
            
            String end = line.substring(endOfDelim);
            end = end.replaceAll("^\\s*", "");
            ends.add(end);
         }
      }
      
      // Transform the ends if they appear numeric-y -- we want to
      // right-align numbers, e.g.
      //
      //    x =   1,
      //    y =  10,
      //    z = 100
      //
      ArrayList<Integer> endPrefixes = new ArrayList<Integer>();
      boolean success = true;
      for (int i = 0; i < ends.size(); i++)
      {
         String current = ends.get(i).replaceAll("[\\s,\\);]*", "");
         try
         {
            endPrefixes.add(("" + Integer.parseInt(current)).length());
         }
         catch (Exception e)
         {
            success = false;
            break;
         }
         
      }
      
      if (success)
      {
         int maxLength = 0;
         for (int i = 0; i < endPrefixes.size(); i++)
            maxLength = Math.max(maxLength, endPrefixes.get(i));
         
         for (int i = 0; i < ends.size(); i++)
            ends.set(i, StringUtil.repeat(" ",
                  maxLength - endPrefixes.get(i)) +
                  ends.get(i).replaceAll("^\\s*", ""));  
      }
      
      // Pad the 'start's with whitespace, to align the delimiter.
      int maxLength = 0;
      for (int i = 0; i < starts.size(); i++)
         maxLength = Math.max(maxLength, starts.get(i).replaceAll("\\s*$", "").length());
      
      for (int i = 0; i < starts.size(); i++)
         starts.set(i, starts.get(i) +
               StringUtil.repeat(" ", maxLength - starts.get(i).length()));
      
      // Build a new selection by concatenating the (transformed)
      // pieces.
      StringBuilder newSelectionBuilder = new StringBuilder();
      
      for (int i = 0; i < starts.size(); i++)
      {
         newSelectionBuilder.append(starts.get(i));
         newSelectionBuilder.append(" " + delimiters.get(i) + " ");
         newSelectionBuilder.append(ends.get(i));
         if (i < starts.size() - 1)
            newSelectionBuilder.append("\n");
      }
      
      docDisplay_.replaceSelection(newSelectionBuilder.toString());
      
   }
   
   private static final String VALID_WORD_FOR_ALIGN =
         "[-+\\w._$@'\"]+";
   
   private static final Pattern DELIM_PATTERN =
         Pattern.create("(^\\s*" +
               VALID_WORD_FOR_ALIGN +
               "\\s*)" + "(<<-|<-|=(?!=))" + "(\\s*" +
               VALID_WORD_FOR_ALIGN +
               "[,;]?\\s*$)");
   
   private ArrayList<Pair<Integer, Integer>> getAlignmentRanges()
   {
      int selectionStart = docDisplay_.getSelectionStart().getRow();
      int selectionEnd = docDisplay_.getSelectionEnd().getRow();
      
      ArrayList<Pair<Integer, Integer>> ranges =
            new ArrayList<Pair<Integer, Integer>>();
      
      for (int i = selectionStart; i <= selectionEnd; i++)
      {
         String line = docDisplay_.getLine(i);
         String masked = StringUtil.maskStrings(line);
         
         Match match = DELIM_PATTERN.match(masked, 0);
         if (match != null)
         {
            String delimiter = match.getGroup(2);
            int rangeStart = i;
            
            while (i++ <= selectionEnd)
            {
               line = docDisplay_.getLine(i);
               masked = StringUtil.maskStrings(line);
               
               // Allow empty lines, and comments, to live within the range.
               if (masked.matches("^\\s*$") ||
                   masked.matches("^\\s*#.*$"))
                  continue;
               
               // If this line doesn't match, bail
               match = DELIM_PATTERN.match(masked, 0);
               if (match == null || match.getGroup(2) != delimiter)
                  break;
            }
            
            // But don't allow comments or whitespaces to exist at the
            // end of a range.
            int rangeEnd = i - 1;
            line = docDisplay_.getLine(rangeEnd);
            while (line.matches("^\\s*$") || line.matches("^\\s*#.*$"))
            {
               rangeEnd--;
               line = docDisplay_.getLine(rangeEnd);
            }
            
            ranges.add(new Pair<Integer, Integer>(rangeStart, rangeEnd));
         }
      }
      
      return ranges;
      
   }
   
   
   
   private final DocDisplay docDisplay_;

}
