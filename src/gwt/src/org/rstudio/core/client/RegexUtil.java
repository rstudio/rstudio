/*
 * RegexUtil.java
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
package org.rstudio.core.client;

import org.rstudio.core.client.regex.Pattern;

// We do lazy initialization of all the regex singleton strings here
public class RegexUtil
{
   public static final String wordCharacter()
   {
      return WORD_CHARACTER;
   }
   
   private static final void initialize()
   {
      WORD_CHARACTER = makeWordCharacter();
   }
   
   public static final boolean isSyntacticRIdentifier(String identifier)
   {
      String regex =
            "^" +
            "[" + WORD_CHARACTER + ".]" +
            "[" + WORD_CHARACTER +  "._]*" +
            "$";
      
      Pattern pattern = Pattern.create(regex, "");
      return pattern.test(identifier);
   }
   
   private static final native String makeWordCharacter()
   /*-{
      var unicode = $wnd.require("ace/unicode");
      return unicode.wordChars;
   }-*/;
   
   private static String WORD_CHARACTER = null;
   
   public static final Pattern RE_RMARKDOWN_CHUNK_BEGIN =
         Pattern.create("^\\s*```\\{(.*?)\\}\\s*$", "");
   
   public static final Pattern RE_RMARKDOWN_CHUNK_END =
         Pattern.create("^\\s*```\\s*$", "");
   
   public static final Pattern RE_RHTML_CHUNK_BEGIN =
         Pattern.create("^\\s*<!-{2,}\\s*(.*?)\\s*$", "");
   
   public static final Pattern RE_RHTML_CHUNK_END =
         Pattern.create("end\\.rcode\\s*-{2,}\\>", "");
   
   public static final Pattern RE_SWEAVE_CHUNK_BEGIN =
         Pattern.create("^\\s*\\<\\<(.*?)\\>\\>=\\s*$", "");
   
   public static final Pattern RE_SWEAVE_CHUNK_END =
         Pattern.create("^\\s*@\\s*$", "");
   
   public static final Pattern RE_EMBEDDED_R_CHUNK_BEGIN =
         Pattern.create("^\\s*\\{(.*?)\\}\\s*$", "");
   
   static { initialize(); }
   
}
