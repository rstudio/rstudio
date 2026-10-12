/*
 * ConsoleErrorTracker.java
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
package org.rstudio.studio.client.common.shell;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.rstudio.core.client.AnsiCode;

import com.google.gwt.dom.client.Element;

/**
 * Tracks the elements written to the console for recent stderr chunks since the
 * last prompt, so that extended error info (an unhandled error event) arriving
 * later can find and replace the plain error output it belongs to.
 *
 * A chunk is matched by its key -- the text with ANSI codes and its
 * "Error in foo:" prefix removed -- but the key is only computed when an
 * extended error actually arrives. Most stderr output never gets one
 * (messages, warnings, progress bars), and stripping and keying every chunk
 * eagerly was measurable on large floods of output (#19080).
 */
public class ConsoleErrorTracker
{
   public static class Entry
   {
      private Entry(String error, List<Element> elements)
      {
         error_ = error;
         elements_ = elements;
      }

      public String getError()
      {
         return error_;
      }

      public List<Element> getElements()
      {
         return elements_;
      }

      private String getKey()
      {
         if (key_ == null)
            key_ = errorKey(error_);
         return key_;
      }

      private final String error_;
      private final List<Element> elements_;
      private String key_;
   }

   /**
    * Records the elements emitted to the console for a chunk of error output.
    */
   public void add(String error, List<Element> elements)
   {
      // Console trimming does not release the raw strings held here. Bound
      // both the number of chunks and their total text, without keying them.
      // Always keep the newest chunk, even if it alone exceeds the budget,
      // so that a large error can still receive its traceback.
      while (!entries_.isEmpty() &&
             (entries_.size() >= MAX_ENTRIES ||
              error.length() > MAX_RETAINED_CHARS - retainedChars_))
      {
         retainedChars_ -= entries_.removeFirst().error_.length();
      }

      entries_.add(new Entry(error, elements));
      retainedChars_ += error.length();
   }

   /**
    * Finds the most recent chunk whose key matches that of the given error
    * message and removes it, or returns null if there is none.
    */
   public Entry take(String error)
   {
      String key = errorKey(error);
      Iterator<Entry> it = entries_.descendingIterator();
      while (it.hasNext())
      {
         Entry entry = it.next();
         if (entry.getKey().equals(key))
         {
            it.remove();
            retainedChars_ -= entry.error_.length();
            return entry;
         }
      }

      return null;
   }

   public void clear()
   {
      entries_.clear();
      retainedChars_ = 0;
   }

   public int size()
   {
      return entries_.size();
   }

   /**
    * The error text with any ANSI codes and its "Error in foo:" prefix removed.
    *
    * The prefix pattern must stay anchored. Unanchored, a leading "[^:]*:" is
    * retried from every position of a string containing no colon at all,
    * which is quadratic, and a few hundred KB of colon-free output stalled the
    * UI for many seconds (#19080).
    */
   static String errorKey(String error)
   {
      String stripped = AnsiCode.strip(error);
      return stripped.replaceFirst("^[^:]*:\\s*", "").trim();
   }

   static final int MAX_ENTRIES = 256;
   static final int MAX_RETAINED_CHARS = 1024 * 1024;

   private final LinkedList<Entry> entries_ = new LinkedList<>();
   private int retainedChars_ = 0;
}
