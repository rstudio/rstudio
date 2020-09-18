/*
 * VirtualConsole.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.virtualscroller.VirtualScrollerManager;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsSubset;

/**
 * Simulates a console that behaves like the R console, specifically with
 * regard to \r (carriage return) and \b (backspace) characters.
 */
public class VirtualConsole
{
   public interface Preferences
   {
      int truncateLongLinesInConsoleHistory();
      String consoleAnsiMode();
      boolean screenReaderEnabled();
      boolean limitConsoleVisible();
   }

   public static class PreferencesImpl extends UserPrefsSubset
                                       implements Preferences
   {
      @Inject
      public PreferencesImpl(Provider<UserPrefs> pUserPrefs)
      {
         super(pUserPrefs);
      }

      @Override
      public int truncateLongLinesInConsoleHistory()
      {
         return getUserPrefs().consoleLineLengthLimit().getGlobalValue();
      }

      @Override
      public String consoleAnsiMode()
      {
         return getUserPrefs().ansiConsoleMode().getValue();
      }

      @Override
      public boolean screenReaderEnabled()
      {
         return getUserPrefs().enableScreenReader().getValue();
      }

      @Override
      public boolean limitConsoleVisible()
      {
         return getUserPrefs().limitVisibleConsole().getValue();
      }
   }

   @Inject
   public VirtualConsole(@Assisted Element parent, final Preferences prefs)
   {
      prefs_ = prefs;
      parent_ = parent;

      VirtualScrollerManager.init();
   }

   public void clear()
   {
      if (isVirtualized())
         clearVirtualScroller();
      else
         formfeed();
   }

   public boolean isLimitConsoleVisible() { return prefs_.limitConsoleVisible(); }

   public void setVirtualizedDisableOverride(boolean override)
   {
      virtualizedDisableOverride_ = override;
   }

   public boolean isVirtualized()
   {
      return !virtualizedDisableOverride_ && prefs_.limitConsoleVisible() && parent_ != null;
   }

   public void clearVirtualScroller()
   {
      if (isVirtualized())
      {
         VirtualScrollerManager.clear(parent_.getParentElement());
      }
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
      text("\n", clazz, false/*forceNewRange*/);
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

   /**
    * Debugging aid
    * @param entry
    * @return diagnostic string summarizing the Entry
    */
   private String debugDumpClassEntry(Entry<Integer, ClassRange> entry)
   {
      if (entry == null)
         return("[null]");
      else
         return("[" + entry.getKey() + "]=" + entry.getValue().debugDump());
   }

   /**
    * Debugging aid
    */
   @SuppressWarnings("unused")
   private void debugDumpClassMap(String name, Map<Integer, ClassRange> map)
   {
      Debug.logToConsole("Dumping " + name);
      if (map == null)
         Debug.logToConsole("null");
      else
         for (Map.Entry<Integer, ClassRange> entry : map.entrySet())
         {
            Debug.logToConsole(name + debugDumpClassEntry(entry));
         }
      Debug.logToConsole("Done dumping " + name);
   }

   @Override
   public String toString()
   {
      String output = output_.toString();

      int maxLength = prefs_.truncateLongLinesInConsoleHistory();
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

      return splat.join("\n");
   }

   public int getLength()
   {
      return output_.length();
   }

   public Element getParent()
   {
      return parent_;
   }

   /**
    * Appends text to the end of the virtual console.
    *
    * @param text The text to append
    * @param clazz Style of the text to append
    * @param forceNewRange start a new output range even if last range had same
    * output style as this one
    */
   private void appendText(String text, String clazz, boolean forceNewRange)
   {
      Entry<Integer, ClassRange> last = class_.lastEntry();
      ClassRange range = last.getValue();
      if (!forceNewRange && StringUtil.equals(range.clazz, clazz))
      {
         // just append to the existing output stream
         range.appendRight(text, 0);
      }
      else
      {
         // create a new output range with this class
         final ClassRange newRange = new ClassRange(cursor_, clazz, text);
         appendChild(newRange.element);
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
      else if (left != null)
         view = class_.headMap(left.getKey(), true);

      // if no overlapping ranges exist, we can just create a new one
      if (view == null)
      {
         class_.put(start, range);
         if (parent_ != null)
            appendChild(range.element);
         return;
      }

      // accumulators for actions to take after we finish iterating over the
      // overlapping ranges (we don't do this in place to avoid invalidating
      // iterators)
      Set<Integer> deletions = new TreeSet<>();
      List<ClassRange> insertions = new ArrayList<>();
      Map<Integer, Integer> moves = new TreeMap<>();

      boolean haveInsertedRange = false;

      for (Entry<Integer, ClassRange> entry: view.entrySet())
      {
         ClassRange overlap = entry.getValue();
         int l = entry.getKey();
         int r = l + overlap.length;
         boolean matches = StringUtil.equals(range.clazz, overlap.clazz);
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
               haveInsertedRange = true;
               if (parent_ != null)
                  parent_.insertAfter(range.element, overlap.element);
            }
         }
         else if (start <= l && end <= r && end > l)
         {
            // overlapping on the right side of the new range
            int delta = end - l;
            if (matches)
            {
               // extend the original range
               overlap.appendLeft(range.text(), delta);

               // if the original range becomes empty, then delete it
               if (overlap.length == 0)
               {
                  deletions.add(l);
                  if (overlap.element.getParentElement() != null)
                     overlap.element.removeFromParent();
               }
               else
               {
                  // If we previously inserted the new range (i.e. overlapped a prior
                  // range that had a different clazz) then undo that and use the one
                  // we found with the same clazz.
                  range.clearText();
                  if (haveInsertedRange)
                  {
                     insertions.remove(range);
                     haveInsertedRange = false;
                  }

                  moves.put(l, overlap.start);
               }
            }
            else
            {
               // reduce the original range and add ours
               overlap.trimLeft(delta);

               if (!range.text().isEmpty())
                  insertions.add(range);

               // move the shortened range to its new start position
               moves.put(l, overlap.start);

               if (parent_ != null && !range.text().isEmpty())
                  parent_.insertBefore(range.element, overlap.element);

            }
         }
         else if (l > start && r < end)
         {
            // this range is fully overwritten, just delete it
            deletions.add(l);
            if (parent_ != null)
               overlap.element.removeFromParent();
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
               int amountTrimmed = overlap.length - (start - l);
               overlap.trimRight(amountTrimmed);

               // insert the new range
               insertions.add(range);
               if (parent_ != null)
                  parent_.insertAfter(range.element, overlap.element);

               // add back the remainder
               ClassRange remainder = new ClassRange(
                     end,
                     overlap.clazz,
                     text.substring((text.length() - (amountTrimmed - range.length))));
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

   /**
    * Add a child to the parent or the virtual scroller, depending on preference
    * @param element to add
    */
   private void appendChild(Element element)
   {
      if (isVirtualized())
         VirtualScrollerManager.append(parent_.getParentElement(), element);
      else
         parent_.appendChild(element);
   }

   /**
    * Write text to DOM
    * @param text text to write
    * @param clazz text style
    * @param forceNewRange start a new range even if style matches previous
    * range's style
    */
   private void text(String text, String clazz, boolean forceNewRange)
   {
      if (newText_ != null)
         newText_.append(text);

      int start = cursor_;
      int end = cursor_ + text.length();

      // real-time output if we have a parent
      if (parent_ != null)
      {
         // short circuit common case in which we're just adding output
         if (cursor_ == output_.length() && !class_.isEmpty())
            appendText(text, clazz, forceNewRange);
         else
            insertText(new ClassRange(start, clazz, text));
      }

      output_.replace(start, end, text);
      cursor_ += text.length();
   }

   public void submit(String data)
   {
      submit(data, null);
   }

   public void submit(String data, String clazz)
   {
      submit(data, clazz, false/*forceNewRange*/, false/*ariaLiveAnnounce*/);
   }

   /**
    * Submit text to console
    * @param data text to output
    * @param clazz text style
    * @param forceNewRange force any output from this call to be in a new
    * @param ariaLiveAnnounce include in aria-live output announcement
    * output range (span) even if style matches previous output
    */
   public void submit(String data, String clazz, boolean forceNewRange, boolean ariaLiveAnnounce)
   {
      // Only capture new elements when dealing with error output, which
      // is only place that sets forceNewRange to true. This is just an
      // optimization to avoid unnecessary overhead for large (non-error)
      // output.
      captureNewElements_ = forceNewRange;
      newElements_.clear();

      newText_ = ariaLiveAnnounce && prefs_.screenReaderEnabled() ? new StringBuilder() : null;

      // If previous submit ended with an incomplete ANSI code, add new data
      // to the previous (unwritten) data so we can try again to recognize
      // ANSI code.
      if (partialAnsiCode_ != null)
      {
         data = partialAnsiCode_ + data;
         partialAnsiCode_ = null;
      }

      String currentClazz = clazz;

      String ansiColorMode = prefs_.consoleAnsiMode();

      // If previously determined classes from ANSI codes are available,
      // combine them with input class so they are ready to use if
      // there is text to output before any other ANSI codes in the
      // data (or there are no more ANSI codes).
      if (ansiColorMode == UserPrefs.ANSI_CONSOLE_MODE_ON && ansiCodeStyles_.inlineClazzes != null)
      {
         if (clazz != null)
         {
            currentClazz = clazz + " " + ansiCodeStyles_.inlineClazzes;
         }
         else
         {
            currentClazz = ansiCodeStyles_.inlineClazzes;
         }
      }

      Match match = (ansiColorMode == UserPrefs.ANSI_CONSOLE_MODE_OFF) ?
            CONTROL.match(data, 0) :
            AnsiCode.CONTROL_PATTERN.match(data, 0);
      if (match == null)
      {
         text(data, currentClazz, forceNewRange);
         return;
      }

      int tail = 0;
      while (match != null)
      {
         int pos = match.getIndex();

         // If we passed over any plain text on the way to this control
         // character, add it.
         if (tail != pos)
         {
            text(data.substring(tail, pos), currentClazz, forceNewRange);

            // once we've started a new range, rest of output for this submit
            // call should share that range (e.g. a multi-line error message)
            forceNewRange = false;
         }

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

               // VirtualConsole only supports ANSI SGR codes (colors, font, etc).
               // We want to identify and act on these codes, while discarding the codes
               // we don't support. Tricky part is we might get codes split across
               // submit calls.

               // match complete SGR codes
               Match sgrMatch = AnsiCode.SGR_ESCAPE_PATTERN.match(data, pos);
               if (sgrMatch == null)
               {
                  if (StringUtil.equals(data.substring(tail), "["))
                  {
                     // only have "[" at end, could be any ANSI code, so save remainder
                     // of string to see if we can recognize it when more arrives
                     partialAnsiCode_ = data.substring(pos);
                     return;
                  }

                  // potentially an incomplete SGR code
                  Match partialMatch = AnsiCode.SGR_PARTIAL_ESCAPE_PATTERN.match(data, pos);
                  if (partialMatch != null)
                  {
                     // Might have an SGR ANSI code that was split across submit calls;
                     // save remainder of string to see if we can recognize it
                     // when more arrives
                     partialAnsiCode_ = data.substring(pos);
                     return;
                  }

                  // how about an unsupported ANSI code?
                  Match ansiMatch = AnsiCode.ANSI_ESCAPE_PATTERN.match(data, pos);
                  if (ansiMatch != null)
                  {
                     // discard it
                     tail = pos + ansiMatch.getValue().length();
                  }
                  else
                  {
                     // nothing useful we can do, just throw away the ESC
                     tail++;
                  }
               }
               else
               {
                  // process the SGR code
                  if (ansi_ == null)
                     ansi_ = new AnsiCode();
                  ansiCodeStyles_ = ansi_.processCode(sgrMatch.getValue());
                  if (ansiColorMode == UserPrefs.ANSI_CONSOLE_MODE_STRIP)
                  {
                     currentClazz = clazz;
                  }
                  else
                  {
                     if (clazz != null)
                     {
                        currentClazz = clazz;
                        if (ansiCodeStyles_.inlineClazzes != null)
                        {
                           currentClazz = currentClazz + " " + ansiCodeStyles_.inlineClazzes;
                        }
                     }
                     else
                     {
                        currentClazz = ansiCodeStyles_.inlineClazzes;
                     }
                  }
                  tail = pos + sgrMatch.getValue().length();
               }
               break;
            default:
               assert false : "Unknown control char, please check regex";
               text(data.charAt(pos) + "", currentClazz, false/*forceNewRange*/);
               break;
         }

         match = match.nextMatch();
      }

      // If there was any plain text after the last control character, add it
      if (tail < data.length())
         text(data.substring(tail), currentClazz, forceNewRange);
   }

   // Elements added by last submit call; only captured if forceNewRange was true
   public List<Element> getNewElements()
   {
      return newElements_;
   }

   // Text added by last submit() call (all ANSI codes and control characters except newlines
   // stripped), only captured if screen reader is enabled when submit() is invoked. Intended
   // for use in reporting output to screen readers.
   public String getNewText()
   {
      return newText_ == null ? "" : newText_.toString();
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

         if (captureNewElements_)
         {
            newElements_.add(element);
         }
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
         start -= (content.length() - delta);
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
               text.substring(pos + content.length()));
      }

      public String text()
      {
         return element.getInnerText();
      }

      public void clearText()
      {
         element.setInnerText("");
      }

      public String debugDump()
      {
         return "start=" + start + ", length=" + length + ", clazz=[" + clazz +
               "], text=[" + text() + "]";
      }

      public final String clazz;
      public int length;
      public int start;
      public final SpanElement element;
   }

   private static final Pattern CONTROL = Pattern.create("[\r\b\f\n]");

   // some panels might want to never virtualize the scrolling based
   // on how they use the VirtualConsole (e.g. Build Pane)
   private boolean virtualizedDisableOverride_ = false;

   private final StringBuilder output_ = new StringBuilder();
   private final TreeMap<Integer, ClassRange> class_ = new TreeMap<>();
   private final Element parent_;

   private int cursor_ = 0;
   private AnsiCode ansi_;
   private String partialAnsiCode_;
   private AnsiCode.AnsiClazzes ansiCodeStyles_ = new AnsiCode.AnsiClazzes();

   // Elements added by last submit call (only if forceNewRange was true)
   private boolean captureNewElements_ = false;
   private final List<Element> newElements_ = new ArrayList<>();

   private StringBuilder newText_;

   // Injected ----
   private final Preferences prefs_;
}
