/*
 * RTokenizerTests.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import junit.framework.Assert;

import com.google.gwt.junit.client.GWTTestCase;
import org.rstudio.studio.client.common.r.RToken;
import org.rstudio.studio.client.common.r.RTokenizer;

public class RTokenizerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testVoid()
   {
      RTokenizer rt = new RTokenizer("") ;
      Assert.assertNull(rt.nextToken()) ;
   }
   
   public void testSimple()
   {
      Verifier v = new Verifier(RToken.ERROR, " ", " ") ;
      v.verify(RToken.LPAREN, "(") ;
      v.verify(RToken.RPAREN, ")") ;
      v.verify(RToken.LBRACKET, "[") ;
      v.verify(RToken.RBRACKET, "]") ;
      v.verify(RToken.LBRACE, "{") ;
      v.verify(RToken.RBRACE, "}") ;
      v.verify(RToken.COMMA, ",") ;
      v.verify(RToken.SEMI, ";") ;
   }
   
   public void testError()
   {
   }

   public void testComment()
   {
      Verifier v = new Verifier(RToken.COMMENT, " ", "\n") ;
      v.verify("#");
      v.verify("# foo #");

      Verifier v2 = new Verifier(RToken.COMMENT, " ", "\r\n") ;
      v2.verify("#");
      v2.verify("# foo #");
   }
   
   public void testNumbers()
   {
      Verifier v = new Verifier(RToken.NUMBER, " ", " ") ;
      v.verify(new String[] {
            "1", "10", "0.1", ".2", "1e-7", "1.2e+7", "2e", "3e+",
            "0x", "0x0", "0xDEADBEEF", "0xcafebad", "1L", "0x10L",
            "1000000L", "1e6L", "1.1L", "1e-3L", "2i", "4.1i", 
            "1e-2i"
      }) ;
   }
   
   public void testOperators()
   {
      Verifier v = new Verifier(RToken.OPER, " ", " ") ;
      v.verify(
            "+ - * / ^ > >= < <= == != ! & | ~ -> <- $ : =".split(" ")) ;
   }
   
   public void testUOperators()
   {
      Verifier v = new Verifier(RToken.UOPER, " ", " ") ;
      v.verify(new String[] {
         "%%", "%test test%"   
      }) ;
   }
   
   public void testStrings()
   {
      Verifier v = new Verifier(RToken.STRING, " ", " ") ;
      v.verify("\"test\"") ;
      v.verify("\" '$\t\r\n\\\"\"") ;
      v.verify("\"\"") ;
      v.verify("''") ;
      v.verify("'\"'") ;
      v.verify("'\\\"'") ;
      v.verify("'\n'") ;
      v.verify("'foo bar \\U654'") ;
   }
   
   public void testIdentifiers()
   {
      Verifier v = new Verifier(RToken.ID, " ", " ") ;
      v.verify(new String[] {
            ".", "...", "..1", "..2", "foo", "FOO", "f1",
            "a_b", "ab_", "\u00C1qc1", "`foo`", "`$@!$@#$`", "`a\n\"'b`"
      }) ;
   }
   
   public void testWhitespace()
   {
      Verifier v = new Verifier(RToken.WHITESPACE, "a", "z") ;
      v.verify("\u00A0") ;
      v.verify(new String[] {
         " ", "      ", "\u00A0", "\t\n"  
      }) ;
   }
   
   protected void verify(String data,
                         int tokenType,
                         String content)
   {
      RTokenizer rt = new RTokenizer(data) ;
      RToken token = rt.nextToken() ;
      Assert.assertNotNull(token) ;
      Assert.assertEquals(tokenType, token.getTokenType()) ;
      Assert.assertEquals(0, token.getOffset()) ;
      Assert.assertEquals(content.length(), token.getLength()) ;
      Assert.assertEquals(content, token.getContent()) ;
   }
   
   class Verifier
   {
      private final int defaultTokenType ;
      private final String prefix ;
      private final String suffix ;

      public Verifier(int defaultTokenType, String prefix, String suffix)
      {
         super() ;
         this.defaultTokenType = defaultTokenType ;
         this.prefix = prefix ;
         this.suffix = suffix ;
      }
      
      public void verify(String value)
      {
         verify(defaultTokenType, value) ;
      }
      
      public void verify(int tokenType, String value)
      {
         RTokenizer rt = new RTokenizer(prefix + value + suffix) ;
         RToken t ;
         while (null != (t = rt.nextToken()))
         {
            if (t.getOffset() == prefix.length())
            {
               Assert.assertEquals(tokenType, t.getTokenType()) ;
               Assert.assertEquals(value.length(), t.getLength()) ;
               Assert.assertEquals(value, t.getContent()) ;
               return ;
            }
         }
         Assert.fail("Bad prefix?") ;
      }
      
      public void verify(String[] values)
      {
         verify(defaultTokenType, values) ;
      }
      
      public void verify(int tokenType, String[] values)
      {
         for (String value : values)
            verify(tokenType, value) ;
      }
   }
}
