/*
 * RTokenizer.java
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
package org.rstudio.studio.client.common.r;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import java.util.ArrayList;

public class RTokenizer
{
   public RTokenizer(String data)
   {
      this.data_ = data;
      this.pos_ = 0;
   }
   
   public static ArrayList<RToken> asTokens(String code)
   {
      ArrayList<RToken> results = new ArrayList<RToken>();
      RTokenizer rt = new RTokenizer(code);
      RToken t;
      while (null != (t = rt.nextToken()))
         results.add(t);
      return results;
   }

   public RToken nextToken()
   {
      if (eol())
         return null;
      
      char c = peek();
      
      switch (c)
      {
      case '(': case ')':
      case '{': case '}':
      case ';': case ',':
         return consumeToken(c, 1);
      case '[':
         if (peek(1, false) == '[')
            return consumeToken(RToken.LDBRACKET, 2);
         else
            return consumeToken(c, 1);
      case ']':
         if (peek(1, false) == ']')
            return consumeToken(RToken.RDBRACKET, 2);
         else
            return consumeToken(c, 1);
      case '"':
      case '\'':
         return matchStringLiteral();
      case '`':
         return matchQuotedIdentifier();
      case '#':
         return matchComment();
      case '%':
         return matchUserOperator();
      case ' ': case '\t': case '\r': case '\n':
      case '\u00A0': case '\u3000':
         return matchWhitespace();
      }
      
      char cNext = peek(1, false);
      
      if ((c >= '0' && c <= '9')
            || (c == '.' && cNext >= '0' && cNext <= '9'))
      {
         RToken numberToken = matchNumber();
         if (numberToken.getLength() > 0)
            return numberToken;
         
         assert false : "matchNumber() returned a zero-length token";
      }
      
      if (StringUtil.isLetter(c) || c == '.')
      {
         // From Section 10.3.2, identifiers must not start with
         // a period followed by a digit.
         //
         // Since we're not checking that the second character is
         // not a digit, we must match on identifiers AFTER we have
         // already tried to match on number.
         return matchIdentifier();
      }

      RToken oper = matchOperator();
      if (oper != null)
         return oper;
      
      // Error!!
      return consumeToken(RToken.ERROR, 1);
   }
   
   private RToken matchWhitespace()
   {
      String whitespace = peek("[\\s\\u00A0]+");
      assert whitespace != null;
      return consumeToken(RToken.WHITESPACE, whitespace.length());
   }
   
   private RToken matchStringLiteral()
   {
      int start = pos_;
      char quot = eat();
      
      assert quot == '"' || quot == '\'';
      
      boolean wellFormed = false;
      
      while (!eol())
      {
         eatUntil("[\\\\\'\"]", true);
         if (eol())
            break;
         
         char c = eat();
         if (c == quot)
         {
            wellFormed = true;
            break;
         }

         if (c == '\\')
         {
            if (!eol())
               eat();
            // Actually the escape expression can be longer than
            // just the backslash plus one character--but we don't
            // need to distinguish escape expressions from other 
            // literal text other than for the purposes of breaking
            // out of the string
         }
      }
      
      return new RStringToken(RToken.STRING, 
                        data_.substring(start, pos_), 
                        start, 
                        pos_-start, wellFormed);
   }
   
   private RToken matchNumber()
   {
      String num = peek("0x[0-9a-fA-F]*L?");
      if (num == null)
         num = peek("[0-9]*(\\.[0-9]*)?([eE][+-]?[0-9]*)?[Li]?");

      // We should only be in this method if 0-9 was matched, so this should
      // be a safe assumption
      assert num != null;

      return consumeToken(RToken.NUMBER, num.length());
   }
   
   private RToken matchIdentifier()
   {
      int start = pos_;
      eat();
      String rest = peek("[\\w.]*");
      pos_ += (rest != null ? rest : "").length();
      return new RToken(RToken.ID, 
                        data_.substring(start, pos_), 
                        start, 
                        pos_ - start);
   }

   private RToken matchQuotedIdentifier()
   {
      String iden = peek("`[^`]*`");
      if (iden == null)
         return consumeToken(RToken.ERROR, 1);
      else
         return consumeToken(RToken.ID, iden.length());
   }

   private RToken matchComment()
   {
      String comment = peek("#.*?$");
      return consumeToken(RToken.COMMENT, comment.length());
   }
   
   private RToken matchUserOperator()
   {
      String oper = peek("%[^%]*%");
      if (oper == null)
         return consumeToken(RToken.ERROR, 1);
      else
         return consumeToken(RToken.UOPER, oper.length());
   }
   
   private RToken matchOperator()
   {
      char cNext = peek(1, false);
      
      switch (peek())
      {
      case '+': case '*': case '/':
      case '^': case '&': case '|':
      case '~': case '$': case ':':
         // single-character operators
         return consumeToken(RToken.OPER, 1);
      case '-': // also ->
         return consumeToken(RToken.OPER, cNext == '>' ? 2 : 1);
      case '>': // also >=
         return consumeToken(RToken.OPER, cNext == '=' ? 2 : 1);
      case '<': // also <- and <=
         return consumeToken(RToken.OPER, cNext == '=' ? 2 :
                                          cNext == '-' ? 2 :
                                          1);
      case '=': // also ==
         return consumeToken(RToken.OPER, cNext == '=' ? 2 : 1);
      case '!': // also !=
         return consumeToken(RToken.OPER, cNext == '=' ? 2 : 1);
      default:
         return null;
      }
   }

   private boolean eol()
   {
      return pos_ >= data_.length();
   }
   
   private char peek()
   {
      return peek(0, true);
   }
   
   private char peek(int lookahead, boolean throwOnEOL)
   {
      if (!throwOnEOL && (pos_ + lookahead) >= data_.length())
         return 0;
      return data_.charAt(pos_ + lookahead);
   }
   
   private char eat()
   {
      char result = data_.charAt(pos_);
      pos_++; // don't inline--we want the previous line to throw if EOL
      return result;
   }
   
   private String peek(String regex)
   {
      Match match = Pattern.create(regex).match(data_, pos_);
      if (match == null)
         return null;
      int idx = match.getIndex();
      if (idx != pos_)
         return null;
      
      return match.getValue();
   }
   
   private String eatUntil(String regex, boolean eatAllOnFailure)
   {
      int start = pos_;
      Match match = Pattern.create(regex).match(data_, pos_);
      if (match == null)
      {
         if (eatAllOnFailure)
         {
            pos_ = data_.length();
            return data_.substring(start);
         }
         else
         {
            return null;
         }
      }
      else
      {
         pos_ = match.getIndex();
         return data_.substring(start, pos_);
      }
   }
   
   private RToken consumeToken(int tokenType, int length)
   {
      if (length == 0)
         throw new IllegalArgumentException("Can't create zero-length token");
      if (pos_ + length > data_.length())
         throw new IllegalArgumentException("Premature EOF");
      
      int start = pos_;
      pos_ += length;
      return new RToken(tokenType, data_.substring(start, pos_), start, length);
   }
   
   private final String data_;
   private int pos_;
}
