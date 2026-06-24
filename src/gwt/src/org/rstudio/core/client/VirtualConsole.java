/*
 * VirtualConsole.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.rstudio.core.client.hyperlink.Hyperlink;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.shell.ShellWidget.ErrorClass;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsSubset;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorThemeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Simulates a console that behaves like the R console, specifically with
 * regard to \r (carriage return) and \b (backspace) characters.
 */
public class VirtualConsole
{
   public static enum Type { STDIN, STDOUT, STDERR };
   
   public interface Preferences
   {
      int truncateLongLinesInConsoleHistory();
      String consoleAnsiMode();
      boolean screenReaderEnabled();
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
   }
   
   private static class HyperlinkMatch
   {
      public static HyperlinkMatch create(String data, int offset)
      {
         String params;
         String contents;
         int endIndex;
         
         // ESC ']' '8' ';' <params> ';' <url> ( BEL | ESC ')')
         TextCursor cursor = new TextCursor(data, offset);
         if (!cursor.consume("\u001b]8;"))
            return null;
         
         int paramsStart = cursor.getIndex();
         if (!cursor.consumeUntil(';'))
            return null;
         
         int paramsEnd = cursor.getIndex();
         params = StringUtil.substring(data, paramsStart, paramsEnd);
         
         if (!cursor.consume(';'))
            return null;
         
         int contentsStart = cursor.getIndex();
         if (!cursor.consumeUntilRegex("(?:\\u0007|\\u001b\\))"))
            return null;
         
         int contentsEnd = cursor.getIndex();
         contents = StringUtil.substring(data, contentsStart, contentsEnd);
         cursor.advance(cursor.peek() == '\u0007' ? 1 : 2);
         endIndex = cursor.getIndex();
         
         return new HyperlinkMatch(params, contents, endIndex);
      }
      
      private HyperlinkMatch(String params,
                             String contents,
                             int endIndex)
      {
         params_ = params;
         contents_ = contents;
         endIndex_ = endIndex;
      }
      
      public final String params_;
      public final String contents_;
      public final int endIndex_;
   }

   @Inject
   public VirtualConsole(@Assisted Element parent, final Preferences prefs)
   {
      prefs_ = prefs;
      parent_ = parent;
      ansiColorMode_ = prefs.consoleAnsiMode();
   }
   
   @Inject
   private void initialize(Session session,
                           EventBus events,
                           UserState userState,
                           UserPrefs userPrefs)
   {
      session_ = session;
      events_ = events;
      userState_ = userState;
      userPrefs_ = userPrefs;
      
      userPrefs.ansiConsoleMode().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            ansiColorMode_ = event.getValue();
         }
      });

      maxLineLength_ = userPrefs_.consoleLineLengthLimit().getValue();
      userPrefs_.consoleLineLengthLimit().addValueChangeHandler(new ValueChangeHandler<Integer>()
      {
         @Override
         public void onValueChange(ValueChangeEvent<Integer> event)
         {
            maxLineLength_ = event.getValue();
         }
      });
      
      events_.addHandler(SessionInitEvent.TYPE, new SessionInitEvent.Handler()
      {
         @Override
         public void onSessionInit(SessionInitEvent sie)
         {
            setErrorClass();
         }
      });
      
      events_.addHandler(EditorThemeChangedEvent.TYPE, new EditorThemeChangedEvent.Handler()
      {
         @Override
         public void onEditorThemeChanged(EditorThemeChangedEvent event)
         {
            setErrorClass();
         }
      });
      
      setErrorClass();
   }
   
   private void setErrorClass()
   {
      String rVersion = session_.getSessionInfo().getRVersionsInfo().getRVersion();
      AceTheme theme = userState_.theme().getValue().cast();
      aceThemeErrorClass_ = AceTheme.getThemeErrorClass(theme);
      
      boolean isCustom =
            Version.compare(rVersion, "4.0.0") >= 0 &&
            userPrefs_.consoleHighlightConditions().getGlobalValue() != UserPrefsAccessor.CONSOLE_HIGHLIGHT_CONDITIONS_NONE;
            
      if (isCustom)
      {
         // We have custom highlighting for R conditions enabled; just use
         // the default error style class when emitting errors.
         errorClass_ = new ErrorClass()
         {
            @Override
            public String get()
            {
               return "";
            }
         };
      }
      else
      {
         // Legacy behavior; ensure that all stderr output is colored according
         // to the editor theme's error text class.
         errorClass_ = new ErrorClass()
         {
            @Override
            public String get()
            {
               return aceThemeErrorClass_;
            }
         };
      }
      
   }

   public void clear()
   {
      formfeed();
   }

   public void setPreserveHTML(boolean preserveHTML)
   {
      preserveHTML_ = preserveHTML;
   }

   private void backspace()
   {
      backspace(1);
   }
   
   private void backspace(int count)
   {
      clearPartialAnsiCode();

      // Like a terminal's BS, backspace stays within the current line rather
      // than crossing the preceding '\n'; clamping at the line start keeps the
      // flat-buffer cursor from landing on another line's newline (which the
      // line-sensitive carriageReturn / eraseInLine would then misattribute).
      cursor_ = Math.max(currentLineStart(), cursor_ - count);
   }

   private void carriageReturn()
   {
      clearPartialAnsiCode();
      if (cursor_ == 0)
         return;

      // currentLineStart() searches from cursor_ - 1 so that a cursor resting
      // directly on a '\n' is treated as belonging to the line that '\n'
      // terminates, not the next one. Horizontal moves (backspace, CSI C/D)
      // are clamped to the current line, so the only way to land on a '\n' is
      // at the end of that same line (via CHA or CSI C) -- making this the
      // correct, unambiguous resolution.
      cursor_ = currentLineStart();
   }

   private void newline(String clazz)
   {
      clearPartialAnsiCode();

      cursor_ = output_.indexOf("\n", cursor_);
      if (cursor_ == -1)
         cursor_ = output_.length();

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
    * Returns the start offset of the current line: the line on which
    * {@code cursor_} sits. Uses {@code cursor_ - 1} so that a cursor resting
    * directly on a '\n' (e.g. an empty line) is treated as belonging to that
    * line rather than the next one, matching {@link #cursorUp} semantics.
    */
   private int currentLineStart()
   {
      return output_.lastIndexOf("\n", cursor_ - 1) + 1;
   }

   /**
    * Returns the end offset of the current line: the position of the '\n'
    * terminating the line {@code cursor_} sits on, or the buffer length for
    * the final, unterminated line. Moving the cursor here places it just past
    * the last character (on the '\n'), which {@link #currentLineStart} still
    * resolves to this same line.
    */
   private int currentLineEnd()
   {
      int lineEnd = output_.indexOf("\n", cursor_);
      return lineEnd == -1 ? output_.length() : lineEnd;
   }

   /**
    * Returns the start offset of the line {@code n} rows above the line
    * beginning at {@code lineStart}, stopping at the top of the buffer.
    */
   private int lineStartAbove(int lineStart, int n)
   {
      int targetLineStart = lineStart;
      for (int i = 0; i < n; i++)
      {
         if (targetLineStart == 0)
            break;
         targetLineStart = output_.lastIndexOf("\n", targetLineStart - 2) + 1;
      }
      return targetLineStart;
   }

   /**
    * Returns the start offset of the line {@code n} rows below the line
    * beginning at {@code lineStart}, stopping at the last line.
    */
   private int lineStartBelow(int lineStart, int n)
   {
      int targetLineStart = lineStart;
      for (int i = 0; i < n; i++)
      {
         int nextNewline = output_.indexOf("\n", targetLineStart);
         if (nextNewline == -1)
            break;
         targetLineStart = nextNewline + 1;
      }
      return targetLineStart;
   }

   /**
    * Cursor Up (CUU) -- CSI A
    *
    * Moves the cursor up n rows. If at the top of the buffer, no further
    * movement occurs. The column is preserved, clamped to the target line
    * length.
    *
    * @param n number of rows to move up
    */
   private void cursorUp(int n)
   {
      clearPartialAnsiCode();
      if (n <= 0)
         return;

      int lineStart = currentLineStart();
      int column = cursor_ - lineStart;
      int targetLineStart = lineStartAbove(lineStart, n);
      cursor_ = targetLineStart + Math.min(column, maxCursorColumn(targetLineStart));
   }

   /**
    * Cursor Down (CUD) -- CSI B
    *
    * Moves the cursor down n rows. If there are fewer than n rows below,
    * stops at the last row. The column is preserved, clamped to the target
    * line length.
    *
    * @param n number of rows to move down
    */
   private void cursorDown(int n)
   {
      clearPartialAnsiCode();
      if (n <= 0)
         return;

      int lineStart = currentLineStart();
      int column = cursor_ - lineStart;
      int targetLineStart = lineStartBelow(lineStart, n);
      cursor_ = targetLineStart + Math.min(column, maxCursorColumn(targetLineStart));
   }

   /**
    * Cursor Next Line (CNL) -- CSI E
    *
    * Moves the cursor to the beginning (column 0) of the line n rows down.
    * If there are fewer than n rows below, stops at the last row; the column
    * is still reset to 0.
    *
    * @param n number of rows to move down
    */
   private void cursorNextLine(int n)
   {
      clearPartialAnsiCode();
      if (n <= 0)
         return;

      cursor_ = lineStartBelow(currentLineStart(), n);
   }

   /**
    * Cursor Previous Line (CPL) -- CSI F
    *
    * Moves the cursor to the beginning (column 0) of the line n rows up. If
    * at the top of the buffer, stops at the first row; the column is still
    * reset to 0.
    *
    * @param n number of rows to move up
    */
   private void cursorPreviousLine(int n)
   {
      clearPartialAnsiCode();
      if (n <= 0)
         return;

      cursor_ = lineStartAbove(currentLineStart(), n);
   }

   /**
    * Cursor Horizontal Absolute (CHA) -- CSI G
    *
    * Moves the cursor to column n (1-based) on the current line. The column
    * is clamped to the line's content length (see {@link #lineLength});
    * values below 1 are treated as column 1.
    *
    * <p>Unlike {@link #cursorUp} / {@link #cursorDown}, which clamp to
    * {@link #maxCursorColumn} (one short of a terminated line's '\n' to avoid
    * the cursor-on-newline ambiguity when a column is implicitly preserved),
    * CHA addresses an explicit column and must be able to reach the cell just
    * past the last character. Landing there places the cursor on the '\n';
    * a subsequent write is handled by the newline-preservation path in
    * {@link #text}, which extends the line rather than overwriting its last
    * character or merging into the line below.
    *
    * @param n target column, 1-based
    */
   private void cursorToColumn(int n)
   {
      clearPartialAnsiCode();

      int lineStart = currentLineStart();
      int column = Math.max(0, n - 1);
      cursor_ = lineStart + Math.min(column, lineLength(lineStart));
   }

   /**
    * Returns the content length (excluding any trailing '\n') of the line
    * beginning at {@code lineStart}.
    */
   private int lineLength(int lineStart)
   {
      int lineEnd = output_.indexOf("\n", lineStart);
      if (lineEnd == -1)
         return output_.length() - lineStart;
      return lineEnd - lineStart;
   }

   /**
    * Returns the largest column offset {@link #cursorUp} / {@link #cursorDown}
    * may place the cursor at on the line beginning at {@code lineStart}.
    *
    * <p>The flat buffer cannot distinguish "cursor past the last character
    * but before the trailing '\n'" from "cursor on the '\n'": both are the
    * same position. For an implicitly preserved column (cursor up/down),
    * clamping to {@code lineLength - 1} on terminated lines keeps the cursor
    * on a real character of the intended line, at the cost of one column of
    * preservation precision -- the cursor lands on the line's last character
    * rather than just past it. For the final, unterminated line of the buffer
    * there is no '\n' to be confused with, so the full length is returned.
    *
    * <p>{@link #cursorToColumn} (CHA) intentionally allows the full line
    * length instead (see {@link #lineLength}); line-sensitive operations such
    * as {@link #carriageReturn} and {@link #eraseInLine} resolve a cursor on a
    * '\n' to the line that '\n' terminates, so landing there stays consistent.
    */
   private int maxCursorColumn(int lineStart)
   {
      int lineEnd = output_.indexOf("\n", lineStart);
      if (lineEnd == -1)
         return output_.length() - lineStart;
      return Math.max(0, lineEnd - lineStart - 1);
   }

   /**
    * Erase in Line (EL) -- CSI K
    *
    * The cursor position is not changed by this operation.
    *
    * @param mode 0 = erase from cursor to end of line (default),
    *             1 = erase from beginning of line to cursor (inclusive),
    *             2 = erase entire line
    */
   private void eraseInLine(int mode)
   {
      clearPartialAnsiCode();

      // Derive the line from cursor_ - 1 (via currentLineStart) so a cursor
      // left on a '\n' by CHA erases the line that '\n' terminates rather than
      // the following line. lineEnd is the '\n' the cursor sits on (or the next
      // one), i.e. the end of that same line.
      int lineStart = currentLineStart();
      int lineEnd = output_.indexOf("\n", cursor_);
      if (lineEnd == -1)
         lineEnd = output_.length();

      int eraseStart, eraseEnd;
      switch (mode)
      {
         case 0:  eraseStart = cursor_;    eraseEnd = lineEnd;                        break;
         case 1:  eraseStart = lineStart;  eraseEnd = Math.min(cursor_ + 1, lineEnd); break;
         case 2:  eraseStart = lineStart;  eraseEnd = lineEnd;                        break;
         default: return;
      }

      if (eraseStart >= eraseEnd)
         return;

      // Optimization: mode 0 at end of buffer -- truncate instead of writing spaces
      if (mode == 0 && lineEnd == output_.length())
      {
         // Trim any ClassRange that straddles the cursor position
         Entry<Integer, ClassRange> floor = class_.floorEntry(cursor_);
         if (floor != null && floor.getKey() < cursor_)
         {
            ClassRange range = floor.getValue();
            int rangeEnd = floor.getKey() + range.length;
            if (rangeEnd > cursor_)
               range.trimRight(rangeEnd - cursor_);
         }

         // Remove all ClassRanges starting at or after cursor
         List<Integer> keysToRemove = new ArrayList<>(class_.tailMap(cursor_, true).keySet());
         for (Integer key : keysToRemove)
         {
            ClassRange cr = class_.get(key);
            if (parent_ != null && cr.element.getParentElement() != null)
               cr.element.removeFromParent();
            class_.remove(key);
         }

         output_.setLength(cursor_);
         return;
      }

      // General case: overwrite with spaces
      int savedCursor = cursor_;
      HyperlinkInfo savedHyperlink = hyperlink_;
      try
      {
         cursor_ = eraseStart;
         hyperlink_ = null;

         int eraseLength = eraseEnd - eraseStart;
         StringBuilder spaces = new StringBuilder(eraseLength);
         for (int i = 0; i < eraseLength; i++)
            spaces.append(' ');

         text(spaces.toString(), null, false/*forceNewRange*/);
      }
      finally
      {
         cursor_ = savedCursor;
         hyperlink_ = savedHyperlink;
      }
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
      {
         Debug.logToConsole("null");
      }
      else
      {
         for (Map.Entry<Integer, ClassRange> entry : map.entrySet())
         {
            Debug.logToConsole(name + debugDumpClassEntry(entry));
         }
      }
      Debug.logToConsole("Done dumping " + name);
   }

   @Override
   public String toString()
   {
      return StringUtil.truncateLines(output_.toString(), maxLineLength_);
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

      if (hyperlink_ != null || range.hyperlink_ != null || !StringUtil.equals(range.clazz, clazz))
      {
         // force if this needs to display an hyperlink
         // or if the previous range was an hyperlink
         // or the classes differ (change of colour)
         forceNewRange = true;
      }
      
      if (forceNewRange)
      {
         // normalize previous bit of output if it was a section
         if (parent_ != null)
         {
            Node lastChildNode = parent_.getLastChild();
            if (Element.is(lastChildNode))
            {
               Element lastChildEl = Element.as(lastChildNode);
               if (lastChildEl.hasClassName(RES.styles().group()))
               {
                  trimLeadingNewlines(lastChildEl);
                  trimTrailingNewlines(lastChildEl);
               }
            }
         }
         
         // create a new output range with this class
         final ClassRange newRange = new ClassRange(cursor_, clazz, text, preserveHTML_, hyperlink_);
         appendChild(newRange.element);
         class_.put(cursor_, newRange);
      }
      else
      {
         // just append to the existing output stream
         range.appendRight(text, 0);
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
      {
         view = class_.subMap(left.getKey(), true, right.getKey(), true);
      } 
      else if (left == null && right != null) 
      {
         view = class_.tailMap(right.getKey(), true);
      } 
      else if (left != null) 
      {
         view = class_.headMap(left.getKey(), true);
      }
      
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
                  overlap.element.getParentElement().insertAfter(range.element, overlap.element);
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

               // move the shortened range to its new start position
               // unless it's empty
               if (overlap.length > 0) {
                  moves.put(l, overlap.start);
               }

               if (!range.text().isEmpty())
                  insertions.add(range);
               
               if (parent_ != null && !range.text().isEmpty())
                  overlap.element.getParentElement().insertBefore(range.element, overlap.element);

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
                  overlap.element.getParentElement().insertAfter(range.element, overlap.element);

               // add back the remainder
               ClassRange remainder = new ClassRange(
                     end,
                     overlap.clazz,
                     StringUtil.substring(text, (text.length() - (amountTrimmed - range.length))),
                     preserveHTML_,
                     overlap.hyperlink_);
               insertions.add(remainder);
               if (parent_ != null)
                  range.element.getParentElement().insertAfter(remainder.element, range.element);
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
      text = StringUtil.truncateLines(text, maxLineLength_);

      if (newText_ != null)
         newText_.append(text);

      int start = cursor_;
      int end = cursor_ + text.length();

      // Preserve newline boundaries: if a non-newline character in the write
      // would land on top of an existing '\n' in the buffer, extend the
      // current line with spaces so the overwrite stays inside it. Without
      // this, a cursor placed on an earlier line via CSI A -- including on
      // an empty line, where the cursor sits directly on the '\n' -- and
      // then asked to write text would clobber the line separator and run
      // into the next line's content. The newline()-driven self-overwrite
      // (text == "\n" landing on an existing '\n') is left alone so that
      // path remains idempotent and does not duplicate newlines.
      int nlPos = output_.indexOf("\n", start);
      if (nlPos >= start && nlPos < end && text.charAt(nlPos - start) != '\n')
      {
         extendLineBefore(nlPos, end - nlPos);
      }

      // real-time output if we have a parent
      if (parent_ != null)
      {
         // short circuit common case in which we're just adding output
         if (cursor_ == output_.length() && !class_.isEmpty())
            appendText(text, clazz, forceNewRange);
         else
            insertText(new ClassRange(start, clazz, text, preserveHTML_, hyperlink_));
      }

      output_.replace(start, end, text);
      cursor_ += text.length();
   }

   /**
    * Insert {@code count} spaces into the buffer immediately before the '\n'
    * at {@code nlPos}, extending the line that ends there. Class ranges and
    * the DOM are kept consistent: the range containing the newline absorbs
    * the spaces, and any range positioned strictly after the newline shifts
    * right by {@code count}.
    */
   private void extendLineBefore(int nlPos, int count)
   {
      if (count <= 0)
         return;

      StringBuilder spaces = new StringBuilder(count);
      for (int i = 0; i < count; i++)
         spaces.append(' ');
      String spacesStr = spaces.toString();

      output_.insert(nlPos, spacesStr);

      Entry<Integer, ClassRange> floor = class_.floorEntry(nlPos);
      ClassRange straddler = null;
      if (floor != null)
      {
         ClassRange r = floor.getValue();
         if (r.start <= nlPos && r.start + r.length > nlPos)
            straddler = r;
      }

      if (straddler != null)
      {
         String rText = straddler.text();
         int offset = nlPos - straddler.start;
         straddler.setText(
               StringUtil.substring(rText, 0, offset)
                     + spacesStr
                     + StringUtil.substring(rText, offset));
         straddler.length += count;
      }

      List<Integer> shiftKeys = new ArrayList<>(class_.tailMap(nlPos + 1).keySet());
      List<ClassRange> shifted = new ArrayList<>(shiftKeys.size());
      for (Integer key : shiftKeys)
         shifted.add(class_.remove(key));
      for (ClassRange r : shifted)
      {
         r.start += count;
         class_.put(r.start, r);
      }
   }
   
   public void submit(String data, Type type)
   {
      submit(data, type, false, false);
   }
   
   public void submit(String data, Type type, boolean forceNewRange, boolean ariaLiveAnnounce)
   {
      if (type == Type.STDIN)
      {
         submit(data, RES.styles().stdin() + ConsoleResources.KEYWORD_CLASS_NAME, forceNewRange, ariaLiveAnnounce);
      }
      else if (type == Type.STDOUT)
      {
         submit(data, RES.styles().stdout(), forceNewRange, ariaLiveAnnounce);
      }
      else if (type == Type.STDERR)
      {
         submit(data, RES.styles().stderr() + " " + errorClass_.get(), forceNewRange, ariaLiveAnnounce);
      }
   }

   public void submit(String data)
   {
      submit(data, (String) null, false, false);
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
      // If we're submitting new console output, but the previous submit request
      // asked us to force a new range, respect that.
      forceNewRange = forceNewRange || forceNewRange_;
      forceNewRange_ = false;
      
      // Only capture new elements when dealing with error output, which
      // is the only place that sets forceNewRange to true. This is just an
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

      // If previously determined classes from ANSI codes are available,
      // combine them with input class so they are ready to use if
      // there is text to output before any other ANSI codes in the
      // data (or there are no more ANSI codes).
      if (ansiColorMode_ == UserPrefs.ANSI_CONSOLE_MODE_ON && ansiCodeStyles_.inlineClazzes != null)
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
      
      // Look for a control character in the input.
      Match match = nextMatch(data, 0);

      // If nothing is found, we're in the "easy" case -- just submit all the text.
      if (match == null)
      {
         text(data, currentClazz, forceNewRange);
         return;
      }
      
      // Start processing control characters and escapes.
      int head = 0;
      int tail = 0;
      for (; match != null; match = nextMatch(data, tail))
      {
         // Update the match position.
         head = match.getIndex();
         
         // If we passed over any plain text on the way to this control
         // character, add it. Note that we're in an intermediate state
         // where tail is now 'behind' the just-updated head.
         if (head != tail)
         {
            text(StringUtil.substring(data, tail, head), currentClazz, forceNewRange);

            // once we've started a new range, rest of output for this submit
            // call should share that range (e.g. a multi-line error message)
            forceNewRange = false;
         }

         // Now, update the tail.
         tail = head + 1;

         switch (data.charAt(head))
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
               
            case '\033': // \x1b
            case '\233': // \x9b

               // VirtualConsole only supports ANSI SGR codes (colors, font, etc).
               // We want to identify and act on these codes, while discarding the codes
               // we don't support. Tricky part is we might get codes split across
               // submit calls.
               
               // If the only character we've seen so far is the escape code,
               // just buffer it and try again with next input.
               if (head == data.length() - 1)
               {
                  partialAnsiCode_ = StringUtil.substring(data, head);
                  return;
               }
               
               // match hyperlink, either start or end (if [url] is empty
               // <ESC> ] 8 ; [params] ; [url] \7
               HyperlinkMatch hyperlinkMatch = HyperlinkMatch.create(data, head);
               if (hyperlinkMatch != null)
               {
                  String params = hyperlinkMatch.params_;
                  String url = hyperlinkMatch.contents_;

                  // toggle hyperlink_
                  if (!StringUtil.equals(url, ""))
                  {
                     hyperlink_ = new HyperlinkInfo(url, params);
                  }
                  else
                  {
                     hyperlink_ = null;   
                  }

                  tail = hyperlinkMatch.endIndex_;
                  break;
               }
               
               // skip string end escapes
               if (data.substring(head, head + 2) == AnsiCode.ST)
               {
                  tail += 1;
                  break;
               }
               
               // check for an escape forcing a new span
               if (parent_ != null)
               {
                  Pattern groupStartPattern = Pattern.create("^\\033G(\\d+);", "");
                  Match groupStartMatch = groupStartPattern.match(data.substring(head), 0);
                  if (groupStartMatch != null)
                  {
                     String type = groupStartMatch.getGroup(1);
                     String groupClazz = groupTypeToClazz(type);
                     
                     // re-use the previous group if we're closing and re-opening
                     // a group of the same type
                     Node lastNode = parent_.getLastChild();
                     Element lastNodeEl = null;
                     if (Element.is(lastNode))
                     {
                        lastNodeEl = Element.as(lastNode);
                        if (lastNodeEl.hasClassName(groupClazz))
                        {
                           parent_ = lastNodeEl;
                           tail += groupStartMatch.getValue().length() - 1;
                           break;
                        }
                     }

                     // if we're starting a group, but the cursor is not
                     // located at the end of the output (e.g. a carriage
                     // return or something similar moved the cursor), then
                     // adjust output appropriately
                     if (lastNodeEl != null)
                     {
                        int numCharsToRemove = output_.length() - cursor_;
                        if (numCharsToRemove > 0)
                        {
                           String text = lastNodeEl.getInnerText();
                           if (text.length() <= numCharsToRemove)
                           {
                              lastNodeEl.removeFromParent();
                           }
                           else
                           {
                              lastNodeEl.setInnerText(
                                 text.substring(0, text.length() - numCharsToRemove));
                           }
                        }
                     }

                     text("", clazz, false);
                     cursor_ = output_.length();
                     
                     // otherwise, create a new group span and use it
                     SpanElement spanEl = Document.get().createSpanElement();
                     spanEl.addClassName(RES.styles().group());
                     spanEl.addClassName(groupClazz);
                     if (!parent_.hasClassName(RES.styles().group()))
                        spanEl.addClassName(RES.styles().groupTop());
                     parent_.appendChild(spanEl);
                     parent_ = spanEl;
                     tail += groupStartMatch.getValue().length() - 1;
                     forceNewRange_ = forceNewRange = true;
                     break;
                  }
                  
                  Pattern groupEndPattern = Pattern.create("^\\033g", "");
                  Match groupEndMatch = groupEndPattern.match(data.substring(head), 0);
                  if (groupEndMatch != null)
                  {
                     if (parent_.hasClassName(RES.styles().group()))
                     {
                        forceNewRange = forceNewRange_ = true;
                        parent_ = parent_.getParentElement();
                     }
                     
                     tail += groupEndMatch.getValue().length() - 1;
                     break;
                  }
               }
               
               // check for embedded custom highlight rules
               Pattern highlightStartPattern = Pattern.create("^\\033H(\\d+);", "");
               Match highlightStartMatch = highlightStartPattern.match(data.substring(head), 0);
               if (highlightStartMatch != null)
               {
                  String type = highlightStartMatch.getGroup(1);
                  savedClazz_ = currentClazz;
                  currentClazz = typeToClazz(type);
                  tail += highlightStartMatch.getValue().length() - 1;
                  break;
               }
               
               Pattern highlightEndPattern = Pattern.create("^\\033h", "");
               Match highlightEndMatch = highlightEndPattern.match(data.substring(head), 0);
               if (highlightEndMatch != null)
               {
                  currentClazz = savedClazz_;
                  savedClazz_ = "";
                  tail += highlightEndMatch.getValue().length() - 1;
                  break;
               }
               
               // match complete CSI codes
               Pattern csiPattern = Pattern.create("^" + AnsiCode.CSI_REGEX, "");
               Match csiMatch = csiPattern.match(data.substring(head), 0);
               if (csiMatch != null)
               {
                  String command = csiMatch.getGroup(2);
 
                  // handle SGR codes up-front
                  if (command == "m")
                  {
                     // process the SGR code
                     ansiCodeStyles_ = ansi_.processCode(csiMatch.getValue());
                     currentClazz = setCurrentClazz(clazz);
                     tail = head + csiMatch.getValue().length();
                     break;
                  }
                  
                  // handle other supported commands
                  if (command == "A")
                  {
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 1);
                     cursorUp(n);
                  }
                  else if (command == "B")
                  {
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 1);
                     cursorDown(n);
                  }
                  else if (command == "C")
                  {
                     // CUF: move right, but not past the end of the current line
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 0);
                     cursor_ = Math.min(currentLineEnd(), cursor_ + n);
                  }
                  else if (command == "D")
                  {
                     // CUB: move left, but not past the start of the current line
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 0);
                     cursor_ = Math.max(currentLineStart(), cursor_ - n);
                  }
                  else if (command == "E")
                  {
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 1);
                     cursorNextLine(n);
                  }
                  else if (command == "F")
                  {
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 1);
                     cursorPreviousLine(n);
                  }
                  else if (command == "G")
                  {
                     int n = StringUtil.parseInt(csiMatch.getGroup(1), 1);
                     cursorToColumn(n);
                  }
                  else if (command == "K")
                  {
                     int mode = StringUtil.parseInt(csiMatch.getGroup(1), 0);
                     eraseInLine(mode);
                  }

                  tail = head + csiMatch.getValue().length();
                  break;
               }
               
               // check for incomplete CSI escapes, and continue parsing those
               Pattern csiPrefixPattern = Pattern.create("^" + AnsiCode.CSI_PREFIX_REGEX, "");
               Match csiPrefixMatch = csiPrefixPattern.match(data.substring(head), 0);
               if (csiPrefixMatch != null)
               {
                  partialAnsiCode_ = StringUtil.substring(data, head);
                  return;
               }
               
               // handle all other kinds of unsupported ANSI escapes and discard them
               Pattern ansiPattern = Pattern.create("^" + AnsiCode.ANSI_REGEX, "");
               Match ansiMatch = ansiPattern.match(data.substring(head), 0);
               if (ansiMatch != null)
               {
                  tail = head + ansiMatch.getValue().length();
                  break;
               }
               
               // if we get here, we didn't know what to do with the escape character
               // just discard it and perform regular parsing from here on
               tail++;
               break;
               
            default:
               assert false : "Unknown control char, please check regex";
               text(data.charAt(head) + "", currentClazz, false/*forceNewRange*/);
               break;
         }
      }

      // If there was any plain text after the last control character, add it
      if (tail < data.length())
         text(StringUtil.substring(data, tail), currentClazz, forceNewRange);
   }
   
   private Match nextMatch(String data, int offset)
   {
      return (ansiColorMode_ == UserPrefs.ANSI_CONSOLE_MODE_OFF)
         ? CONTROL.match(data, offset)
         : AnsiCode.CONTROL_PATTERN.match(data, offset);
   }
   
   public void normalizePreviousOutput()
   {
      if (parent_ == null)
         return;
      
      Node childNode = parent_.getLastChild();
      if (!Element.is(childNode))
         return;
      
      Element childEl = Element.as(childNode);
      if (!childEl.hasClassName(RES.styles().group()))
         return;
      
      trimLeadingNewlines(childEl);
      trimTrailingNewlines(childEl);
   }
   
   private void trimLeadingNewlines(Element childEl)
   {
      Node firstChildNode = childEl.getFirstChild();
      if (firstChildNode == null)
         return;
      
      while (firstChildNode.getNodeType() != Node.TEXT_NODE)
      {
         firstChildNode = firstChildNode.getFirstChild();
         if (firstChildNode == null)
            return;
      }
 
      firstChildNode.setNodeValue(
            firstChildNode.getNodeValue().replaceFirst("^\\n+", ""));
   }
   
   private void trimTrailingNewlines(Element childEl)
   {
      Node lastChildNode = childEl.getLastChild();
      if (lastChildNode == null)
         return;
      
      while (lastChildNode.getNodeType() != Node.TEXT_NODE)
      {
         lastChildNode = lastChildNode.getLastChild();
         if (lastChildNode == null)
            return;
      }
 
      lastChildNode.setNodeValue(
            lastChildNode.getNodeValue().replaceFirst("\\n+$", ""));
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

   public void ensureStartingOnNewLine()
   {
      Node child = getParent().getLastChild();
      if (child == null)
         return;
      
      if (child.getNodeType() != Node.ELEMENT_NODE)
         return;
      
      Element nodeEl = Element.as(child);
      if (nodeEl.hasClassName(RES.styles().groupMessage()))
         return;
      
      String text = nodeEl.getInnerText();
      if (text.endsWith("\n"))
         return;
      
      submit("\n");
   }

   private String setCurrentClazz(String clazz)
   {
      String currentClazz;
      if (ansiColorMode_ == UserPrefs.ANSI_CONSOLE_MODE_STRIP)
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
      return currentClazz;
   }
   
   private String typeToClazz(String type)
   {
      if (type == HIGHLIGHT_TYPE_ERROR)
      {
         return RES.styles().error();
      }
      else if (type == HIGHLIGHT_TYPE_WARNING)
      {
         return RES.styles().warning();
      }
      else if (type == HIGHLIGHT_TYPE_MESSAGE)
      {
         return RES.styles().message();
      }
      else
      {
         return "";
      }
   }
   
   private String groupTypeToClazz(String type)
   {
      if (type == GROUP_TYPE_ERROR)
      {
         return RES.styles().groupError();
      }
      else if (type == GROUP_TYPE_WARNING)
      {
         return RES.styles().groupWarning();
      }
      else if (type == GROUP_TYPE_MESSAGE)
      {
         return RES.styles().groupMessage();
      }
      else if (type == GROUP_TYPE_AGENT)
      {
         return RES.styles().groupAgent();
      }
      else
      {
         return "";
      }
   }

   private class ClassRange
   {
      public ClassRange(int pos, String className, String text, boolean isHTML, HyperlinkInfo hyperlink)
      {
         clazz  = className;
         start = pos;
         length = text.length();
         isHTML_ = isHTML;
        
         hyperlink_ = hyperlink;
         
         if (hyperlink_ == null) 
         {
            element = Document.get().createSpanElement();
            if (className != null)
               element.addClassName(clazz);

            setText(text);
         }
         else 
         {
            element = Hyperlink.create(hyperlink.url_, hyperlink_.params_, text, clazz).getElement();
         }

         if (captureNewElements_)
            newElements_.add(element);
      }

      private void setText(String text)
      {
         if (isHTML_)
         {
            element.setInnerHTML(text);
         }
         else
         {
            element.setInnerText(text);
         }
      }

      public void trimLeft(int delta)
      {
         length -= delta;
         start += delta;
         setText(StringUtil.substring(element.getInnerText(), delta));
      }

      public void trimRight(int delta)
      {
         length -= delta;
         String text = element.getInnerText();
         setText(StringUtil.substring(text, 0, text.length() - delta));
      }

      public void appendLeft(String content, int delta)
      {
         length += content.length() - delta;
         start -= (content.length() - delta);
         setText(content + StringUtil.substring(element.getInnerText(), delta));
      }

      public void appendRight(String content, int delta)
      {
         length += content.length() - delta;
         String text = text();
         setText(StringUtil.substring(text, 0, text.length() - delta) + content);
      }

      public void overwrite(String content, int pos)
      {
         String text = element.getInnerText();
         setText(
               StringUtil.substring(text, 0, pos) + content +
               StringUtil.substring(text, pos + content.length()));
      }

      public String text()
      {
         return isHTML_ ? element.getInnerHTML() : element.getInnerText();
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
      public final Element element;
      public final HyperlinkInfo hyperlink_;
      private boolean isHTML_;
   }

   private class HyperlinkInfo
   {
      public HyperlinkInfo(String url, String params)
      {
         url_ = url;
         params_ = params;
      }

      public String url_;
      public String params_;
   }
   
   private static final Pattern CONTROL = Pattern.create("[\r\b\f\n]");

   // allows &entity_name; entities like &amp;
   private boolean preserveHTML_ = false;

   private final StringBuilder output_ = new StringBuilder();
   private final TreeMap<Integer, ClassRange> class_ = new TreeMap<>();
   private Element parent_;
   private String ansiColorMode_;

   private int cursor_ = 0;
   private AnsiCode ansi_ = new AnsiCode();
   private AnsiCode.AnsiClazzes ansiCodeStyles_ = new AnsiCode.AnsiClazzes();
   private String partialAnsiCode_;
   private HyperlinkInfo hyperlink_;
   private String savedClazz_ = "";

   private ErrorClass errorClass_;
   private String aceThemeErrorClass_;
   
   // Elements added by last submit call (only if forceNewRange was true)
   private boolean forceNewRange_ = false;
   private boolean captureNewElements_ = false;
   private final List<Element> newElements_ = new ArrayList<>();

   private StringBuilder newText_;
   
   // Styles ----
   
   public static interface Styles extends CssResource
   {
      String stdin();
      String stdout();
      String stderr();
      
      String group();
      String groupTop();
      String groupError();
      String groupWarning();
      String groupMessage();
      String groupAgent();
      
      String error();
      String warning();
      String message();
   }
   
   public static interface Resources extends ClientBundle
   {
      @Source("VirtualConsole.gss")
      Styles styles();
   }
   
   public static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.styles().ensureInjected();
   }
   
   private static final String HIGHLIGHT_TYPE_ERROR   = "1";
   private static final String HIGHLIGHT_TYPE_WARNING = "2";
   private static final String HIGHLIGHT_TYPE_MESSAGE = "3";
   
   private static final String GROUP_TYPE_ERROR   = "1";
   private static final String GROUP_TYPE_WARNING = "2";
   private static final String GROUP_TYPE_MESSAGE = "3";
   private static final String GROUP_TYPE_AGENT   = "4";

   private int maxLineLength_;
   

   // Injected ----
   private final Preferences prefs_;
   private Session session_;
   private EventBus events_;
   private UserState userState_;
   private UserPrefs userPrefs_;
   
}
