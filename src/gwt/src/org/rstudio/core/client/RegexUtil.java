/*
 * RegexUtil.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client;

// We do lazy initialization of all the regex singleton strings here
public class RegexUtil
{
   public static final String letter()
   {
      if (LETTER == null)
         LETTER = constructLetter();
      return LETTER;
   }
   
   public static final String wordCharacter()
   {
      if (WORD_CHARACTER == null)
         WORD_CHARACTER = constructWordCharacter();
      return WORD_CHARACTER;
   }
   
   public static boolean isSyntacticRIdentifier(String identifier)
   {
      if (SYNTACTIC_R_IDENTIFIER == null)
         SYNTACTIC_R_IDENTIFIER = constructSyntacticRIdentifierRegex();
      
      return identifier.matches(SYNTACTIC_R_IDENTIFIER);
   }
   
   
   private static final native String constructLetter() /*-{
      var unicode = $wnd.require("ace/unicode");
      return unicode.packages.L;
   }-*/;
   
   private static final native String constructWordCharacter() /*-{
      var unicode = $wnd.require("ace/unicode");
      return unicode.packages.L +
             unicode.packages.N;
   }-*/;
   
   private static final String constructSyntacticRIdentifierRegex()
   {
      return
            "[" + letter() + ".]" +
            "[" + wordCharacter() + "._]*";
   }
   
   private static String WORD_CHARACTER = null;
   private static String LETTER = null;
   
   private static String SYNTACTIC_R_IDENTIFIER = null;
}
