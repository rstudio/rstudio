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

import java.util.ArrayList;
import java.util.List;
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
   // use preference to determine ANSI color behavior
   private final static int ANSI_COLOR_UNSET = -1;

   // don't do any processing of ANSI escape codes
   public final static int ANSI_COLOR_OFF = 0;
   
   // convert ANSI color escape codes into css styles
   public final static int ANSI_COLOR_ON = 1;
   
   // strip out ANSI escape sequences but don't apply styles
   public final static int ANSI_COLOR_STRIP = 2;
  
   public VirtualConsole()
   {
      this(null);
   }
   
   public VirtualConsole(Element parent)
   {
      this(parent, ANSI_COLOR_UNSET);
   }

   /**
    * VirtualConsole constructor
    * @param parent parent element
    * @param ansiColorMode ANSI_COLOR_OFF: don't process ANSI escapes,
    * ANSI_COLOR_ON: translate ANSI escapes into css styles, ANSI_COLOR_STRIP:
    * strip out ANSI escape sequences but don't apply styles
    * sequences, otherwise just strip them out
    */
   public VirtualConsole(Element parent, int ansiColorMode)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      parent_ = parent;
      if (ansiColorMode == ANSI_COLOR_UNSET)
         ansiColorMode = prefs_.consoleAnsiMode().getValue();
      ansiColorMode_ = ansiColorMode;
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
      clearPartialAnsiCode();
      if (cursor_ == 0)
         return;
      cursor_--;
   }

   private void carriageReturn()
   {
      clearPartialAnsiCode();
      if (cursor_ == 0)
         return;
      while (cursor_ > 0 && output_.charAt(cursor_ - 1) != '\n')
         cursor_--;
   }

   private void newline(String clazz)
   {
      clearPartialAnsiCode();
      while (cursor_ < output_.length() && output_.charAt(cursor_) != '\n')
         cursor_++;
      // Now we're either at the end of the buffer, or on top of a '\n'
      text("\n", clazz);
   }

   private void formfeed()
   {
      clearPartialAnsiCode();
      output_.setLength(0);
      cursor_ = 0;
      class_.clear();
      if (parent_ != null)
         parent_.setInnerHTML("");
   }
   
   private void clearPartialAnsiCode()
   {
      partialAnsiCode_ = null;
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
   
   public Element getParent()
   {
      return parent_;
   }
   
   /**
    * Appends text to the end of the virtual console.
    * 
    * @param text The text to append
    * @param clazz The content to append to the text.
    */
   private void appendText(String text, String clazz)
   {
      Entry <Integer, ClassRange> last = class_.lastEntry();
      ClassRange range = last.getValue();
      if (range.clazz == clazz)
      {
         // just append to the existing output stream
         range.appendRight(text, 0);
      }
      else
      {
         // create a new output range with this class
         final ClassRange newRange = new ClassRange(cursor_, clazz, text);
         parent_.appendChild(newRange.element);
         class_.put(cursor_, newRange);
      }
   }

   /**
    * Inserts text which overlaps existing text in the virtual console.
    * 
    * @param range
    */
   private void insertText(ClassRange range)
   {
      int start = range.start;
      int end = start + range.length;
      
      Entry<Integer, ClassRange> left = class_.floorEntry(start);
      Entry<Integer, ClassRange> right = class_.floorEntry(end);

      // create a view into the map representing the ranges that this class
      // overlaps
      SortedMap<Integer, ClassRange> view = null;
      if (left != null && right != null)
         view = class_.subMap(left.getKey(), true, right.getKey(), true);
      else if (left == null && right != null)
         view = class_.tailMap(right.getKey(), true);
      else if (left != null && right == null)
         view = class_.headMap(left.getKey(), true);
      
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
      List<ClassRange> insertions = new ArrayList<ClassRange>();
      Map<Integer, Integer> moves = new TreeMap<Integer, Integer>();

      for (Entry<Integer, ClassRange> entry: view.entrySet())
      {
         ClassRange overlap = entry.getValue();
         int l = entry.getKey();
         int r = l + overlap.length;
         boolean matches = range.clazz == overlap.clazz;
         if (start >= l && start < r && end >= r) 
         {
            // overlapping on the left side of the new range
            int delta = r - start;
            if (matches)
            {
               // extend the original range
               overlap.appendRight(range.text(), delta);
               range.clearText();
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
         else if (start <= l && end <= r && end >= l)
         {
            // overlapping on the right side of the new range
            int delta = end - l;
            if (matches)
            {
               // extend the original range
               overlap.appendLeft(range.text(), delta);
               range.clearText();
               moves.put(l, start);
            }
            else
            {
               // reduce the original range and add ours (if not present)
               overlap.trimLeft(delta);
               insertions.add(range);
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

               // trim the original range
               overlap.trimRight(overlap.length - (start - l));
               
               // insert the new range
               insertions.add(range);
               if (parent_ != null)
                  parent_.insertAfter(range.element, overlap.element);
               
               // add the new range
               ClassRange remainder = new ClassRange(
                     end,
                     overlap.clazz,
                     text.substring((text.length() - range.length), 
                                    text.length()));
               insertions.add(remainder);
               if (parent_ != null)
                  parent_.insertAfter(remainder.element, range.element);
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
      int end = cursor_ + text.length();
      
      // real-time output if we have a parent
      if (parent_ != null)
      {
         // short circuit common case in which we're just adding output
         if (cursor_ == output_.length() && !class_.isEmpty())
            appendText(text, clazz);
         else
            insertText(new ClassRange(start, clazz, text));
      }

      output_.replace(start, end, text);
      cursor_ += text.length();
   }
   
   public void submit(String data, String clazz)
   {
      // If previous submit ended with an incomplete ANSI code, add new data
      // to the previous (unwritten) data so we can try again to recognize
      // ANSI code.
      if (partialAnsiCode_ != null)
      {
         data = partialAnsiCode_ + data;
         partialAnsiCode_ = null;
      }
     
      String currentClazz = clazz;
      
      // If previously determined classes from ANSI codes are available,
      // combine them with input class so they are ready to use if
      // there is text to output before any other ANSI codes in the
      // data (or there are no more ANSI codes).
      if (ansiColorMode_ == ANSI_COLOR_ON && ansiCodeStyles_ != null)
      {
         if (clazz != null)
         {
            currentClazz = clazz + " " + ansiCodeStyles_;
         }
         else
         {
            currentClazz = ansiCodeStyles_;
         }
      }

      Match match = (ansiColorMode_ == ANSI_COLOR_OFF) ?
            CONTROL.match(data, 0) :
            AnsiCode.CONTROL_PATTERN.match(data, 0);
      if (match == null)
      {
         text(data, currentClazz);
         return;
      }
      
      int tail = 0;
      while (match != null)
      {
         int pos = match.getIndex();

         // If we passed over any plain text on the way to this control
         // character, add it.
         if (tail != pos)
            text(data.substring(tail, pos), currentClazz);
         
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
               newline(clazz);
               break;
            case '\f':
               formfeed();
               break;
            case '\033':
            case '\233':
               Match ansiMatch = AnsiCode.ANSI_ESCAPE_PATTERN.match(data, pos);
               if (ansiMatch == null || AnsiCode.partialSequence(ansiMatch.getValue()))
               {
                  // Might have an ANSI code that was split across submit calls;
                  // save remainder of string to see if we can recognize it
                  // when more arrives
                  partialAnsiCode_ = data.substring(pos);
                  return;
               }
               if (ansi_ == null)
                  ansi_ = new AnsiCode();
               ansiCodeStyles_ = ansi_.processCode(ansiMatch.getValue());
               if (ansiColorMode_ == ANSI_COLOR_STRIP)
               {
                  currentClazz = clazz;
               }
               else
               {
                  if (clazz != null)
                  {
                     currentClazz = clazz + " " + ansiCodeStyles_;
                  }
                  else
                  {
                     currentClazz = ansiCodeStyles_;
                  }
               }
               tail = pos + ansiMatch.getValue().length();
               break;
            default:
               assert false : "Unknown control char, please check regex";
               text(data.charAt(pos) + "", currentClazz);
               break;
         }

         match = match.nextMatch();
      }

      // If there was any plain text after the last control character, add it
      if (tail < data.length())
         text(data.substring(tail), currentClazz);
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
         element.setInnerText(text.substring(0, text.length() - delta));
      }
      
      public void appendLeft(String content, int delta)
      {
         length += content.length() - delta;
         start -= delta;
         element.setInnerText(content + 
               element.getInnerText().substring(delta));
      }
      
      public void appendRight(String content, int delta)
      {
         length += content.length() - delta;
         String text = text();
         element.setInnerText(text.substring(0,
               text.length() - delta) + content);
      }
      
      public void overwrite(String content, int pos)
      {
         String text = element.getInnerText();
         element.setInnerText(
               text.substring(0, pos) + content +
               text.substring(pos + content.length(), text.length()));
      }
      
      public String text()
      {
         return element.getInnerText();
      }
      
      public void clearText()
      {
         element.setInnerText("");
      }

      public String clazz;
      public int length;
      public int start;
      public SpanElement element;
   }

   private static final Pattern CONTROL = Pattern.create("[\r\b\f\n]");
   
   private final StringBuilder output_ = new StringBuilder();
   private final TreeMap<Integer, ClassRange> class_ = new TreeMap<Integer, ClassRange>();
   private final Element parent_;
   
   private int cursor_ = 0;
   private AnsiCode ansi_;
   private String partialAnsiCode_;
   private String ansiCodeStyles_;
   private int ansiColorMode_;
   
   // Injected ----
   private UIPrefs prefs_;
}
