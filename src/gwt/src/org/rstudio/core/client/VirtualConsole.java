/*
 * VirtualConsole.java
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

import java.util.ArrayList;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;

/**
 * Simulates a console that behaves like the R console, specifically with
 * regard to \r (carriage return) and \b (backspace) characters.
 */
public class VirtualConsole
{
   public VirtualConsole()
   {
   }
   
   public boolean submit(String data)
   {
      return submit(data, null);
   }

   // Adds the given data to the console. Returns true if the data can be 
   // processed as an append-only operation, false if characters were 
   // overwritten.
   public boolean submit(String data, String className)
   {
      boolean appendOnly = true;
      if (StringUtil.isNullOrEmpty(data))
         return true;

      if (CONTROL_SPECIAL.match(data, 0) == null)
      {
         text(data, className);
         return true;
      }

      int tail = 0;
      Match match = CONTROL.match(data, 0);
      while (match != null)
      {
         int pos = match.getIndex();

         // If we passed over any plain text on the way to this control
         // character, add it.
         text(data.substring(tail, pos), className);

         tail = pos + 1;

         switch (data.charAt(pos))
         {
            case '\r':
               carriageReturn();
               // the sequence \r\n or \n\r can be represented in an append-only
               // way, so treat these cases as an append
               appendOnly = 
                     ((pos > 0 && data.charAt(pos - 1) == '\n') ||
                      (tail < data.length() && data.charAt(tail) == '\n'));
               break;
            case '\b':
               backspace();
               appendOnly = false;
               break;
            case '\n':
               newline();
               break;
            case '\f':
               formfeed();
               appendOnly = false;
               break;
            default:
               assert false : "Unknown control char, please check regex";
               text(data.charAt(pos) + "", className);
               break;
         }

         match = match.nextMatch();
      }

      // If there was any plain text after the last control character, add it
      text(data.substring(tail), className);
      return appendOnly;
   }

   private void backspace()
   {
      if (pos_ == 0)
         return;
      o.deleteCharAt(--pos_);
   }

   private void carriageReturn()
   {
      if (pos_ == 0)
         return;
      while (pos_ > 0 && o.charAt(pos_ - 1) != '\n')
         pos_--;
      // Now we're either at the beginning of the buffer, or just past a '\n'
   }

   private void newline()
   {
      while (pos_ < o.length() && o.charAt(pos_) != '\n')
         pos_++;
      // Now we're either at the end of the buffer, or on top of a '\n'
      text("\n", null);
   }

   private void formfeed()
   {
      o.setLength(0);
      pos_ = 0;
      charClass.clear();
   }

   private void text(String text, String className)
   {
      assert text.indexOf('\r') < 0 && text.indexOf('\b') < 0;

      int endPos = pos_ + text.length();
      
      o.replace(pos_, endPos, text);
      
      // record the class of each character emitted
      if (className != null) 
      {
         padCharClass(endPos);
         for (int i = pos_; i < endPos; i++)
         {
            charClass.set(i, className);
         }
      }

      pos_ = endPos;
   }
   
   // ensures that the character class mapping buffer is at least 'len' 
   // characters long (note that ensureCapacity just reallocs the underlying
   // JavaScript array if necessary)
   private void padCharClass(int len)
   {
      int curSize = charClass.size();
      if (curSize >= len)
         return;
      charClass.ensureCapacity(len);
      for (int i = 0; i < (len - curSize); i++)
         charClass.add(null);
   }
   
   @Override
   public String toString()
   {
      return o.toString();
   }
   
   public int getLength()
   {
      return o.length();
   }
   
   public void submitAndRender(String data, String clazz, Element parent)
   {
      if (!submit(data, clazz))
      {
         // output isn't append-only; redraw the whole thing
         // (note that even this isn't technically necessary but control 
         // characters are relatively infrequent and additional bookkeeping
         // would be required to determine the invalidated range when 
         // control characters are used)
         redraw(parent);
      }
      else
      {
         // consolify just the data to be rendered
         emitRange(consolify(data), clazz, parent);
      }
   }
   
   public void clear()
   {
      formfeed();
   }
   
   public static String consolify(String text)
   {
      VirtualConsole console = new VirtualConsole();
      console.submit(text);
      return console.toString();
   }

   private void emitRange(String text, String clazz, Element parent)
   {
      if (StringUtil.isNullOrEmpty(text))
         return;
      Text textNode = Document.get().createTextNode(text);
      if (clazz != null)
      {
         SpanElement span = Document.get().createSpanElement();
         span.addClassName(clazz);
         parent.appendChild(span);
         parent = span;
      }
      parent.appendChild(textNode);
   }
   
   private void redraw(Element parent)
   {
      // convert to a plain-text string
      String plainText = toString();
      int len = plainText.length();
      String lastClass = null;
      padCharClass(len);
      
      // clean existing content
      parent.setInnerHTML("");
      
      // for performance reasons, we don't emit one character at a time;
      // instead, we keep track of the string indices that correspond to
      // contiguous runs of characters to emit into the stream
      int accumulateBegin = 0;
      int accumulateEnd = 0;

      // iterate in lockstep over the plain-text string and character class
      // assignment list; emit the appropriate tags when switching classes
      for (int i = 0; i < len; i++)
      {
         if (!charClass.get(i).equals(lastClass))
         {
            emitRange(
                  plainText.substring(accumulateBegin, accumulateEnd),
                  lastClass, parent);
            
            // begin accumulating from the emitted point
            accumulateBegin = accumulateEnd;
         }
         lastClass = charClass.get(i);
         accumulateEnd = i;
      }

      // finishing up--emit accumulated text into stream
      emitRange(
            plainText.substring(accumulateBegin),
            lastClass, parent);
      return;
   }
   private final StringBuilder o = new StringBuilder();
   private final ArrayList<String> charClass = new ArrayList<String>();
   private int pos_ = 0;
   private static final Pattern CONTROL = Pattern.create("[\r\b\f\n]");
   private static final Pattern CONTROL_SPECIAL = Pattern.create("[\r\b\f]");
}
