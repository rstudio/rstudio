/*
 * VirtualConsole.java
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
package org.rstudio.core.client;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.inject.Inject;

/**
 * Simulates a console that behaves like the R console, specifically with
 * regard to \r (carriage return) and \b (backspace) characters.
 */
public class VirtualConsole
{
   public VirtualConsole()
   {
      this(null);
   }
   
   public VirtualConsole(Element parent)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      parent_ = parent;
   }
   
   @Inject
   private void initialize(UIPrefs prefs)
   {
      prefs_ = prefs;
   }
   
   public void submit(String data)
   {
      submit(data, null);
   }
   
   public void clear()
   {
      formfeed();
   }

   private void backspace()
   {
      if (cursor_ == 0)
         return;
      output_.deleteCharAt(--cursor_);
   }

   private void carriageReturn()
   {
      if (cursor_ == 0)
         return;
      while (cursor_ > 0 && output_.charAt(cursor_ - 1) != '\n')
         cursor_--;
   }

   private void newline()
   {
      while (cursor_ < output_.length() && output_.charAt(cursor_) != '\n')
         cursor_++;
      // Now we're either at the end of the buffer, or on top of a '\n'
      text("\n", null);
   }

   private void formfeed()
   {
      output_.setLength(0);
      cursor_ = 0;
      class_.clear();
      if (parent_ != null)
         parent_.setInnerHTML("");
   }

   
   @Override
   public String toString()
   {
      String output = output_.toString();
      
      int maxLength = prefs_.truncateLongLinesInConsoleHistory().getGlobalValue();
      if (maxLength == 0)
         return output;
      
      JsArrayString splat = StringUtil.split(output, "\n");
      for (int i = 0; i < splat.length(); i++)
      {
         String string = splat.get(i);
         String trimmed = StringUtil.trimRight(string);
         if (trimmed.length() > maxLength)
            splat.set(i, trimmed.substring(0, maxLength) + "... <truncated>");
         else if (string.length() > maxLength)
            splat.set(i, string.substring(0, maxLength));
      }
      
      String joined = splat.join("\n");
      return joined;
   }
   
   public int getLength()
   {
      return output_.length();
   }
   
   public static String consolify(String text)
   {
      VirtualConsole console = new VirtualConsole();
      console.submit(text);
      return console.toString();
   }

   private void insertText(ClassRange range)
   {
      int start = range.start;
      int end = start + range.length;
      
      Entry<Integer, ClassRange> left = class_.floorEntry(start);
      Entry<Integer, ClassRange> right = class_.ceilingEntry(end);

      // create a view into the map representing the ranges that this class
      // overlaps
      SortedMap<Integer, ClassRange> view = null;
      if (left != null && right != null)
         view = class_.subMap(left.getKey(), right.getKey());
      else if (left == null && right != null)
         view = class_.tailMap(right.getKey());
      else if (left != null && right == null)
         view = class_.headMap(left.getKey());
      
      // if no overlapping ranges exist, we can just create a new one
      if (view == null)
      {
         class_.put(start, range);
         if (parent_ != null)
            parent_.appendChild(range.element);
         return;
      }

      // accumulators for actions to take after we finish iterating over the
      // overlapping ranges (we don't do this in place to avoid invalidating
      // iterators)
      Set<Integer> deletions = new TreeSet<Integer>();
      Set<ClassRange> insertions = new TreeSet<ClassRange>();
      Map<Integer, Integer> moves = new TreeMap<Integer, Integer>();

      for (Entry<Integer, ClassRange> entry: view.entrySet())
      {
         ClassRange overlap = entry.getValue();
         int l = entry.getKey();
         int r = l + overlap.length;
         boolean matches = range.clazz == overlap.clazz;
         if (start >= l && end >= r) 
         {
            // overlapping on the left side
            int delta = r - start;
            if (matches)
            {
               // extend the original range
               overlap.appendRight(range.text(), delta);
            }
            else
            {
               // reduce the original range and add ours
               overlap.trimRight(delta);
               insertions.add(range);
               if (parent_ != null)
                  parent_.insertAfter(range.element, overlap.element);
            }
         }
         else if (start <= l && end <= r)
         {
            // overlapping on the right side
            int delta = end - l;
            if (matches)
            {
               // extend the original range
               overlap.appendLeft(range.text(), delta);
               moves.put(l, start);
            }
            else
            {
               // reduce the original range and add ours
               insertions.add(range);
               overlap.trimLeft(delta);
               if (parent_ != null)
                  parent_.insertBefore(range.element, overlap.element);
            }
         }
         else if (l > start && r < end)
         {
            // this range is fully overwritten, just delete it
            deletions.add(l);
            if (parent_ != null)
               parent_.removeChild(overlap.element);
         }
         else if (start > l && end < r)
         {
            // this range is fully contained
            if (matches)
            {
               // just write over the existing text
               overlap.overwrite(range.text(), start - l);
            }
            else
            {
               String text = overlap.text();
               int delta = r - end;
               // trim the original range
               overlap.trimLeft(start - l);
               
               // insert the new range
               insertions.add(range);
               if (parent_ != null)
                  parent_.insertAfter(range.element, overlap.element);
               
               // add the new range
               ClassRange remainder = new ClassRange(
                     end + 1,
                     text.substring(text.length() - delta, text.length()),
                     overlap.clazz);
               insertions.add(remainder);
               if (parent_ != null)
                  parent_.insertAfter(range.element, remainder.element);
            }
         }
      }
      
      // process accumulated actions
      for (Integer key: deletions)
      {
         class_.remove(key);
      }
      
      for (Integer key: moves.keySet())
      {
         ClassRange moved = class_.get(key);
         class_.remove(key);
         class_.put(moves.get(key), moved);
      }
      
      for (ClassRange val: insertions)
      {
         class_.put(val.start, val);
      }
   }
   
   private void text(String text, String clazz)
   {
      int start = cursor_;
      int end = Math.min(cursor_, output_.length());
      output_.replace(start, end, text);
      insertText(new ClassRange(start, clazz, text));
      cursor_ += text.length();
   }
   
   public void submit(String data, String clazz)
   {
      if (CONTROL_SPECIAL.match(data, 0) == null)
      {
         text(data, clazz);
         return;
      }

      int tail = 0;
      Match match = CONTROL.match(data, 0);
      while (match != null)
      {
         int pos = match.getIndex();

         // If we passed over any plain text on the way to this control
         // character, add it.
         text(data.substring(tail, pos), clazz);

         tail = pos + 1;

         switch (data.charAt(pos))
         {
            case '\r':
               carriageReturn();
               break;
            case '\b':
               backspace();
               break;
            case '\n':
               newline();
               break;
            case '\f':
               formfeed();
               break;
            default:
               assert false : "Unknown control char, please check regex";
               text(data.charAt(pos) + "", clazz);
               break;
         }

         match = match.nextMatch();
      }

      // If there was any plain text after the last control character, add it
      text(data.substring(tail), clazz);
   }
   
   private class ClassRange
   {
      public ClassRange(int pos, String className, String text)
      {
         clazz  = className;
         start = pos;
         length = text.length();
         element = Document.get().createSpanElement();
         if (className != null)
            element.addClassName(clazz);
         element.setInnerText(text);
      }
      
      public void trimLeft(int delta)
      {
         length -= delta;
         start += delta;
         element.setInnerText(element.getInnerText().substring(delta));
      }
      
      public void trimRight(int delta)
      {
         length -= delta;
         String text = element.getInnerText();
         element.setInnerText(text.substring(text.length() - delta,
               text.length()));
      }
      
      public void appendLeft(String content, int delta)
      {
         length += delta;
         start -= delta;
         element.setInnerText(content + 
               element.getInnerText().substring(delta));
      }
      
      public void appendRight(String content, int delta)
      {
         length += delta;
         String text = element.getInnerText();
         element.setInnerText(text.substring(text.length() - delta,
               text.length()) + content);
      }
      
      public void overwrite(String content, int pos)
      {
         String text = element.getInnerText();
         element.setInnerText(
               text.substring(0, pos) + content +
               text.substring(pos + 1, text.length()));
      }
      
      public String text()
      {
         return element.getInnerText();
      }

      public String clazz;
      public int length;
      public int start;
      public SpanElement element;
   }

   private static final Pattern CONTROL = Pattern.create("[\r\b\f\n]");
   private static final Pattern CONTROL_SPECIAL = Pattern.create("[\r\b\f]");
   
   private final StringBuilder output_ = new StringBuilder();
   private final TreeMap<Integer, ClassRange> class_ = new TreeMap<Integer, ClassRange>();
   private final Element parent_;
   
   private int cursor_ = 0;
   
   // Injected ----
   private UIPrefs prefs_;
}
