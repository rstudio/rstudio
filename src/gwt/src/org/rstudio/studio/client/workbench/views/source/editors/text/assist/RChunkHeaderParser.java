/*
 * RChunkHeaderParser.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.assist;

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.Mutable;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TextCursor;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

public class RChunkHeaderParser
{
   public static Map<String, String> parse(String line)
   {
      Map<String, String> options = new HashMap<String, String>();
      parse(line, options);
      return options;
   }
   
   public static final void parse(String line, Map<String, String> options)
   {
      // set up state
      Mutable<String> key = new Mutable<String>();
      Consumer keyConsumer = new MutableConsumer(key);
      
      Mutable<String> val = new Mutable<String>();
      Consumer valConsumer = new MutableConsumer(val);
      
      // determine an appropriate pattern for extracting options from
      // this header (infer based on the line contents)
      Pattern pattern = null;
      boolean isRmd = false;
      if (RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN.test(line))
      {
         pattern = RegexUtil.RE_RMARKDOWN_CHUNK_BEGIN;
         isRmd = true;
      }
      else if (RegexUtil.RE_SWEAVE_CHUNK_BEGIN.test(line))
      {
         pattern = RegexUtil.RE_SWEAVE_CHUNK_BEGIN;
         isRmd = false;
      }
      else if (RegexUtil.RE_RHTML_CHUNK_BEGIN.test(line))
      {
         pattern = RegexUtil.RE_RHTML_CHUNK_BEGIN;
         isRmd = false;
      }
      
      if (pattern != null)
      {
         Match match = pattern.match(line, 0);
         if (match != null)
            line = match.getGroup(1);
      }
      
      TextCursor cursor = new TextCursor(line);
      
      // force default R engine
      options.put("engine", ensureQuoted("r"));
      
      // for R Markdown documents, we need to also parse
      // an engine and an optional label, which adds a bit
      // of extra work for the parser
      if (isRmd)
      {
         // consume engine
         if (!consumeEngine(cursor, options))
            return;

         // consume whitespace and commas
         if (!cursor.consumeUntilRegex("[^\\s,]"))
            return;

         // consume next token -- need to determine
         // whether this is a chunk option name or
         // a label soon after
         if (!consumeKey(cursor, keyConsumer))
            return;

         // consume until ',' or '='. if nothing is
         // found, this must have been a label
         if (!cursor.consumeUntilRegex("[,=]"))
         {
            options.put("label", ensureQuoted(key.get().trim()));
            return;
         }

         char ch = cursor.peek();
         if (ch == ',')
         {
            // found a comma -- this must have been a label
            options.put("label", ensureQuoted(key.get().trim()));
         }
         else
         {
            // found an '=' -- this was a key for a chunk option
            if (!cursor.consume('='))
               return;

            // eat whitespace
            if (!cursor.consumeUntilRegex("\\S"))
               return;

            // consume value
            if (!consumeValue(cursor, valConsumer))
               return;

            // set option
            options.put(key.get(), val.get());

            // move to next comma
            if (!cursor.fwdToCharacter(',', false))
               return;
         }
      }
      
      do
      {
         // eat whitespace and commas
         if (!cursor.consumeUntilRegex("[^\\s,]"))
            return;
         
         // consume key
         if (!consumeKey(cursor, keyConsumer))
            return;
         
         // eat whitespace
         if (!cursor.consumeUntilRegex("\\S"))
            return;
         
         // check '='
         if (!cursor.consume('='))
            return;
         
         // eat whitespace
         if (!cursor.consumeUntilRegex("\\S"))
            return;
         
         // consume value
         if (!consumeValue(cursor, valConsumer))
            return;
         
         // update options
         options.put(
               StringUtil.stringValue(key.get().trim()),
               val.get().trim());
         
         // find next comma
         if (!cursor.consumeUntil(','))
            return;
      }
      while (cursor.peek() == ',');
      
      return;
   }
   
   private static final boolean consumeEngine(final TextCursor cursor,
                                              final Map<String, String> options)
   {
      Consumer consumer = new Consumer()
      {
         @Override
         public void consume(String value)
         {
            options.put("engine", ensureQuoted(value));
         }
      };
      
      if (consumeUntilRegex(cursor, "(?:$|[\\s,])", consumer))
         return true;
      
      return false;
   }
   
   private static final boolean consumeUntilRegex(TextCursor cursor,
                                                  String regex,
                                                  Consumer consumer)
   {
      Pattern pattern = Pattern.create(regex);
      Match match = pattern.match(cursor.getData(), cursor.getIndex());
      if (match == null)
         return false;
      
      int startIdx = cursor.getIndex();
      int endIdx = match.getIndex();
      if (consumer != null)
      {
         String value = cursor.getData().substring(startIdx, endIdx);
         consumer.consume(value);
      }

      cursor.setIndex(endIdx);
      return true;
   }
   
   private static final boolean consumeQuotedItem(TextCursor cursor, Consumer consumer)
   {
      if (!isQuote(cursor.peek()))
         return false;
      
      int startIndex = cursor.getIndex();
      if (cursor.fwdToMatchingCharacter())
      {
         int endIndex = cursor.getIndex() + 1;
         if (consumer != null)
         {
            String value = cursor.getData().substring(startIndex, endIndex);
            consumer.consume(value);
         }
         cursor.setIndex(endIndex);
         return true;
      }
      return false;
   }
   
   private static final boolean consumeKey(TextCursor cursor, Consumer consumer)
   {
      if (isQuote(cursor.peek()) && consumeQuotedItem(cursor, consumer))
         return true;
      
      return consumeUntilRegex(cursor, "(?:$|[^a-zA-Z0-9_.])", consumer);
   }
   
   private static final boolean consumeValue(TextCursor cursor,
                                             Consumer consumer)
   {
      if (consumeQuotedItem(cursor, consumer))
         return true;
      
      int startIdx = cursor.getIndex();
      do
      {
         if (cursor.isLeftBracket() && cursor.fwdToMatchingCharacter())
            continue;
         
         if (cursor.peek() == ',')
            break;
      }
      while (cursor.moveToNextCharacter());
      
      int endIdx = cursor.getIndex();
      if (consumer != null)
      {
         String value = cursor.getData().substring(startIdx, endIdx);
         consumer.consume(value);
      }
      cursor.setIndex(endIdx);
      return true;
   }
   
   private static final boolean isQuote(char ch)
   {
      return ch == '\'' || ch == '"' || ch == '`';
   }
   
   private static final String ensureQuoted(String string)
   {
      String[] quotes = new String[] { "\"", "'", "`" };
      for (String quote : quotes)
         if (string.startsWith(quote) && string.endsWith(quote))
            return string;
      
      return "\"" + string.replaceAll("\"", "\\\\\"") + "\"";
   }
   
   private static interface Consumer
   {
      public void consume(String value);
   }
   
   private static class MutableConsumer implements Consumer
   {
      public MutableConsumer(Mutable<String> value)
      {
         value_ = value;
      }
      
      @Override
      public void consume(String value)
      {
         value_.set(value);
      }
      
      private final Mutable<String> value_;
   }
}
