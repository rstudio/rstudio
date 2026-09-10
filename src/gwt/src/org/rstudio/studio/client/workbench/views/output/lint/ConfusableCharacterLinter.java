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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.output.OutputConstants;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

/**
 * Flags characters in R code that look like ASCII but are not: Cyrillic and
 * Greek lookalike letters, the Greek question mark, typographic quotes and
 * dashes (as pasted from the web), the Unicode minus sign, Unicode spaces
 * and fullwidth forms, plus zero-width characters that cannot be seen at
 * all. R happily parses e.g. Cyrillic "c" as a new symbol and rejects the
 * spaces with "unexpected input", so these are invisible bugs. Strings and
 * comments (roxygen included) are
 * left alone, as is everything outside R chunks in R Markdown. A token that
 * also contains non-ASCII characters with no ASCII lookalike is a genuine
 * non-Latin word (e.g. a Cyrillic identifier) and is not flagged. See #14485.
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
         if (isRmd && !isRChunkBodyRow(docDisplay, row))
            continue;

         JsArray<Token> tokens = docDisplay.getTokens(row);
         if (tokens == null)
            continue;

         // columns are accumulated from the token values rather than read
         // off the tokens: text the tokenizer matched no rule for (e.g. a
         // zero-width space) is stamped with the column of the next match
         int column = 0;
         for (int i = 0; i < tokens.length(); i++)
         {
            Token token = tokens.get(i);
            String value = token.getValue();
            int start = column;
            column += value.length();

            if (isStringOrComment(token.getType()) || isNonLatinWord(value))
               continue;

            for (int j = 0; j < value.length(); j++)
            {
               String message = warningFor(value.charAt(j));
               if (message == null)
                  continue;

               lint.push(LintItem.create(row, start + j, row, start + j + 1, message, "warning"));
            }
         }
      }

      return lint;
   }

   private static String warningFor(char ch)
   {
      if (INVISIBLES.contains(ch))
         return constants_.invisibleCharacterWarning(codepoint(ch));

      String lookalike = lookalikeFor(ch);
      if (lookalike == null)
         return null;

      return constants_.confusableCharacterWarning(codepoint(ch), lookalike);
   }

   // a token with non-ASCII characters that don't resemble ASCII is a word
   // deliberately written in another script, not a stray lookalike. This also
   // spares joiners in scripts that need them (e.g. ZWNJ in Persian).
   private static boolean isNonLatinWord(String value)
   {
      for (int i = 0; i < value.length(); i++)
      {
         char ch = value.charAt(i);
         if (ch >= 0x80 && lookalikeFor(ch) == null && !INVISIBLES.contains(ch))
            return true;
      }
      return false;
   }

   // roxygen prose is tokenized as e.g. constant.numeric.virtual-comment
   // (for **bold**), so the comment prefix alone doesn't cover it
   private static boolean isStringOrComment(String type)
   {
      return type.startsWith("string") ||
             type.startsWith("comment") ||
             type.contains("virtual-comment");
   }

   // the chunk header row (```{r}) is fenced markup, not R code, and chunks
   // in other engines (python, asis, ...) are not R at all. The header is
   // parsed rather than asking the highlighter, which falls back to R rules
   // for engines it doesn't know (e.g. {markdown}).
   private static boolean isRChunkBodyRow(DocDisplay docDisplay, int row)
   {
      Scope chunk = docDisplay.getChunkAtPosition(Position.create(row, 0));
      if (chunk == null || !chunk.isChunk())
         return false;

      int headerRow = chunk.getPreamble().getRow();
      if (headerRow == row)
         return false;

      Map<String, String> options = RChunkHeaderParser.parse(docDisplay.getLine(headerRow));
      String engine = StringUtil.stringValue(options.get("engine"));
      return engine.equalsIgnoreCase("r");
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
      // no-break space, ogham space mark, en/em/thin/hair spaces and friends
      // (U+2000..U+200A), narrow no-break, medium mathematical and
      // ideographic spaces: all render as a gap, none is whitespace to R
      String spaces = "\u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u202F\u205F\u3000";
      for (int i = 0; i < spaces.length(); i++)
         LOOKALIKES.put(spaces.charAt(i), " ");
   }

   // zero-width space, non-joiner and joiner, word joiner, soft hyphen and
   // the byte order mark: nothing to see at all, so no ASCII lookalike
   private static final Set<Character> INVISIBLES = new HashSet<>(
         Arrays.asList('\u200B', '\u200C', '\u200D', '\u2060', '\u00AD', '\uFEFF'));

   private static final OutputConstants constants_ = GWT.create(OutputConstants.class);
}
