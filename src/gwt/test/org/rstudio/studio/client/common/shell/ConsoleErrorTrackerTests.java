/*
 * ConsoleErrorTrackerTests.java
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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

public class ConsoleErrorTrackerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Generous enough for a slow CI machine; the regressions these guard
   // against (quadratic keying, or keying every chunk eagerly) take many
   // seconds to minutes on this much input.
   private static final double TIME_LIMIT_MS = 5000;

   // Roughly one stderr chunk from gert::git_clone(verbose = TRUE): a few
   // hundred KB of CR-separated progress lines with no colon anywhere.
   private static String progressFlood(int lines)
   {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < lines; i++)
         sb.append("\rTransferred ").append(i).append(" of ").append(lines).append(" objects...");
      return sb.toString();
   }

   private static List<Element> elements(int n)
   {
      List<Element> els = new ArrayList<>();
      for (int i = 0; i < n; i++)
         els.add(Document.get().createSpanElement());
      return els;
   }

   public void testErrorKeyStripsErrorPrefix()
   {
      assertEquals("object 'x' not found",
                   ConsoleErrorTracker.errorKey("Error in foo(x) : object 'x' not found\n"));
      assertEquals("boom", ConsoleErrorTracker.errorKey("Error: boom"));
   }

   public void testErrorKeyStripsOnlyUpToFirstColon()
   {
      assertEquals("a: b", ConsoleErrorTracker.errorKey("Error in f() : a: b"));
   }

   public void testErrorKeyStripsAnsiCodes()
   {
      assertEquals("boom", ConsoleErrorTracker.errorKey("\033[1mError\033[0m: \033[31mboom\033[0m"));
   }

   public void testErrorKeyLeavesColonFreeTextAlone()
   {
      assertEquals("Transferred 12 of 5000 objects...",
                   ConsoleErrorTracker.errorKey("\rTransferred 12 of 5000 objects...\n"));
   }

   // Regression guard for #19080; see errorKey(). Unanchored, the prefix
   // regex takes minutes on this input.
   public void testErrorKeyIsFastOnLargeColonFreeOutput()
   {
      String flood = progressFlood(12000);

      double start = Duration.currentTimeMillis();
      String key = ConsoleErrorTracker.errorKey(flood);
      double elapsed = Duration.currentTimeMillis() - start;

      assertEquals(flood.trim(), key);
      assertTrue("errorKey took " + elapsed + "ms", elapsed < TIME_LIMIT_MS);
   }

   public void testTakeMatchesByKey()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      List<Element> els = elements(2);
      tracker.add("\033[1mError in f()\033[0m : boom\n", els);

      ConsoleErrorTracker.Entry entry = tracker.take("Error in f() : boom");
      assertNotNull(entry);
      assertSame(els, entry.getElements());
      assertEquals(0, tracker.size());
   }

   // The extended error widget re-renders the message, so it needs the text
   // exactly as it was written -- ANSI codes and all -- not the key.
   public void testTakeReturnsOriginalErrorText()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String error = "\033[31mError in f() : boom\033[0m\n";
      tracker.add(error, elements(1));

      ConsoleErrorTracker.Entry entry = tracker.take("Error in f() : boom");
      assertNotNull(entry);
      assertEquals(error, entry.getError());
   }

   public void testTakeReturnsNullWithoutMatch()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      tracker.add("Error in f() : boom\n", elements(1));

      assertNull(tracker.take("Error in g() : other"));
      assertEquals(1, tracker.size());
   }

   public void testTakePrefersMostRecentMatch()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      List<Element> first = elements(1);
      List<Element> second = elements(1);
      tracker.add("Error in f() : boom\n", first);
      tracker.add("Error in g() : boom\n", second);

      assertSame(second, tracker.take("Error: boom").getElements());
      assertSame(first, tracker.take("Error: boom").getElements());
      assertNull(tracker.take("Error: boom"));
   }

   public void testTakeSkipsNonMatchingNewerEntries()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      List<Element> els = elements(1);
      tracker.add("Error in f() : boom\n", els);
      tracker.add("Warning message:\nsomething else\n", elements(1));

      assertSame(els, tracker.take("Error in f() : boom").getElements());
      assertEquals(1, tracker.size());
   }

   public void testClearDropsEverything()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      tracker.add("Error: boom\n", elements(1));
      tracker.add("Error: bang\n", elements(1));

      tracker.clear();

      assertEquals(0, tracker.size());
      assertNull(tracker.take("Error: boom"));
   }

   public void testRepeatedChunksAreBounded()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      tracker.add("Error: oldest", elements(1));
      List<Element> els = elements(1);
      for (int i = 0; i < 10000; i++)
         tracker.add("Warning: repeated", els);
      List<Element> newest = elements(1);
      tracker.add("Warning: repeated", newest);

      assertEquals(ConsoleErrorTracker.MAX_ENTRIES, tracker.size());
      assertNull(tracker.take("Error: oldest"));
      assertSame(newest, tracker.take("Warning: repeated").getElements());
      assertEquals(ConsoleErrorTracker.MAX_ENTRIES - 1, tracker.size());
   }

   public void testLargeChunksAreBoundedByTextSize()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String flood = progressFlood(12000);
      int capacity = ConsoleErrorTracker.MAX_RETAINED_CHARS / flood.length();
      List<Element> els = elements(1);
      tracker.add("Error: oldest", els);
      for (int i = 0; i < 200; i++)
         tracker.add(flood, els);
      List<Element> newest = elements(1);
      tracker.add(flood, newest);

      assertEquals(capacity, tracker.size());
      assertNull(tracker.take("Error: oldest"));
      assertSame(newest, tracker.take(flood).getElements());
   }

   public void testOversizedNewestChunkCanStillMatch()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String flood = progressFlood(40000);
      assertTrue(flood.length() > ConsoleErrorTracker.MAX_RETAINED_CHARS);
      List<Element> els = elements(1);
      tracker.add("Error: oldest", elements(1));
      tracker.add(flood, els);

      assertEquals(1, tracker.size());
      assertSame(els, tracker.take(flood).getElements());

      tracker.add(flood, els);
      tracker.add("Error: newest", elements(1));

      assertEquals(1, tracker.size());
      assertNull(tracker.take(flood));
      assertNotNull(tracker.take("Error: newest"));
   }

   public void testTakeReleasesTextBudget()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String flood = progressFlood(12000);
      List<Element> els = elements(1);
      tracker.add(flood, els);
      assertNotNull(tracker.take(flood));
      tracker.add("Error: keep", els);
      int capacity = ConsoleErrorTracker.MAX_RETAINED_CHARS / flood.length();
      for (int i = 0; i < capacity; i++)
         tracker.add(flood, els);

      assertNotNull(tracker.take("Error: keep"));
      assertEquals(capacity, tracker.size());
   }

   public void testClearReleasesTextBudget()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String flood = progressFlood(12000);
      List<Element> els = elements(1);
      tracker.add(flood, els);
      tracker.clear();
      tracker.add("Error: keep", els);
      int capacity = ConsoleErrorTracker.MAX_RETAINED_CHARS / flood.length();
      for (int i = 0; i < capacity; i++)
         tracker.add(flood, els);

      assertNotNull(tracker.take("Error: keep"));
      assertEquals(capacity, tracker.size());
   }

   // Keying is deferred until an extended error arrives, including when old
   // chunks are evicted. Eagerly keying each chunk (strip + regex + trim + map
   // insert) took seconds here.
   public void testAddDoesNotKeyChunks()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String flood = progressFlood(12000);

      double start = Duration.currentTimeMillis();
      for (int i = 0; i < 200; i++)
         tracker.add(flood, elements(1));
      double elapsed = Duration.currentTimeMillis() - start;

      assertEquals(ConsoleErrorTracker.MAX_RETAINED_CHARS / flood.length(), tracker.size());
      assertTrue("add took " + elapsed + "ms", elapsed < TIME_LIMIT_MS);
   }

   // After a flood, recent errors can still be matched behind newer noise.
   public void testTakeAfterFloodFindsTheError()
   {
      ConsoleErrorTracker tracker = new ConsoleErrorTracker();
      String flood = progressFlood(12000);
      List<Element> els = elements(1);
      for (int i = 0; i < 200; i++)
         tracker.add(flood, elements(1));
      tracker.add("Error in git_clone() : failed\n", els);
      tracker.add("Warning: unrelated\n", elements(1));

      double start = Duration.currentTimeMillis();
      ConsoleErrorTracker.Entry entry = tracker.take("Error in git_clone() : failed");
      double elapsed = Duration.currentTimeMillis() - start;

      assertNotNull(entry);
      assertSame(els, entry.getElements());
      assertEquals(ConsoleErrorTracker.MAX_RETAINED_CHARS / flood.length() + 1, tracker.size());
      assertTrue("take took " + elapsed + "ms", elapsed < TIME_LIMIT_MS);
   }
}
