/*
 * ConfusableCharacterLinter.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.lint;

import java.util.HashMap;
import java.util.Map;

import org.rstudio.studio.client.workbench.views.output.OutputConstants;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

/**
 * Flags characters in R code that look like ASCII but are not: Cyrillic and
 * Greek lookalike letters, the Greek question mark, typographic quotes and
 * dashes (as pasted from the web), the Unicode minus sign, no-break spaces
 * and fullwidth forms. R happily parses e.g. Cyrillic "c" as a new symbol,
 * so these are invisible bugs. Strings and comments are left alone, as is
 * prose outside chunks in R Markdown. A token that also contains non-ASCII
 * characters with no ASCII lookalike is a genuine non-Latin word (e.g. a
 * Cyrillic identifier) and is not flagged. See #14485.
 */
public class ConfusableCharacterLinter
{
   public static JsArray<LintItem> lint(DocDisplay docDisplay)
   {
      JsArray<LintItem> lint = JsArray.createArray().cast();
      boolean isRmd = docDisplay.getFileType().isRmd();

      int rowCount = docDisplay.getRowCount();
      for (int row = 0; row < rowCount; row++)
      {
         if (isRmd && !isChunkBodyRow(docDisplay, row))
            continue;

         JsArray<Token> tokens = docDisplay.getTokens(row);
         if (tokens == null)
            continue;

         for (int i = 0; i < tokens.length(); i++)
         {
            Token token = tokens.get(i);
            String type = token.getType();
            if (type.startsWith("string") || type.startsWith("comment"))
               continue;

            String value = token.getValue();
            if (isNonLatinWord(value))
               continue;

            for (int j = 0; j < value.length(); j++)
            {
               String lookalike = lookalikeFor(value.charAt(j));
               if (lookalike == null)
                  continue;

               int column = token.getColumn() + j;
               lint.push(LintItem.create(
                     row, column, row, column + 1,
                     constants_.confusableCharacterWarning(codepoint(value.charAt(j)), lookalike),
                     "warning"));
            }
         }
      }

      return lint;
   }

   // a token with non-ASCII characters that don't resemble ASCII is a word
   // deliberately written in another script, not a stray lookalike
   private static boolean isNonLatinWord(String value)
   {
      for (int i = 0; i < value.length(); i++)
      {
         char ch = value.charAt(i);
         if (ch >= 0x80 && lookalikeFor(ch) == null)
            return true;
      }
      return false;
   }

   // the chunk header row (```{r}) is fenced markup, not R code
   private static boolean isChunkBodyRow(DocDisplay docDisplay, int row)
   {
      Scope chunk = docDisplay.getChunkAtPosition(Position.create(row, 0));
      return chunk != null && chunk.isChunk() && chunk.getPreamble().getRow() != row;
   }

   private static String lookalikeFor(char ch)
   {
      if (ch < 0x80)
         return null;

      // fullwidth ASCII (U+FF01..U+FF5E) maps onto U+0021..U+007E by offset
      if (ch >= 0xFF01 && ch <= 0xFF5E)
         return String.valueOf((char) (ch - 0xFF01 + 0x21));

      return LOOKALIKES.get(ch);
   }

   private static String codepoint(char ch)
   {
      String hex = Integer.toHexString(ch).toUpperCase();
      while (hex.length() < 4)
         hex = "0" + hex;
      return "U+" + hex;
   }

   private static void addLookalikes(String from, String to)
   {
      assert from.length() == to.length();
      for (int i = 0; i < from.length(); i++)
         LOOKALIKES.put(from.charAt(i), String.valueOf(to.charAt(i)));
   }

   private static final Map<Character, String> LOOKALIKES = new HashMap<>();
   static
   {
      // Cyrillic lowercase: a e o p c y x i j s h q w v
      addLookalikes("\u0430\u0435\u043E\u0440\u0441\u0443\u0445\u0456\u0458\u0455\u04BB\u051B\u051D\u0475",
                    "aeopcyxijshqwv");
      // Cyrillic uppercase: A B E K M H O P C T X I J S
      addLookalikes("\u0410\u0412\u0415\u041A\u041C\u041D\u041E\u0420\u0421\u0422\u0425\u0406\u0408\u0405",
                    "ABEKMHOPCTXIJS");
      // Greek: o p v i (lowercase); A B E Z H I K M N O P T Y X (uppercase)
      addLookalikes("\u03BF\u03C1\u03BD\u03B9\u0391\u0392\u0395\u0396\u0397\u0399\u039A\u039C\u039D\u039F\u03A1\u03A4\u03A5\u03A7",
                    "opviABEZHIKMNOPTYX");
      // Greek question mark, typographic quotes, dashes and the minus sign
      addLookalikes("\u037E\u2018\u2019\u201A\u201B\u201C\u201D\u201E\u201F\u2010\u2011\u2012\u2013\u2014\u2212",
                    ";''''\"\"\"\"------");
      // no-break space
      LOOKALIKES.put('\u00A0', " ");
   }

   private static final OutputConstants constants_ = GWT.create(OutputConstants.class);
}
