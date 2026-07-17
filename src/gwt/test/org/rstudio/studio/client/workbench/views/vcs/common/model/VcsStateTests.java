/*
 * VcsStateTests.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.model;

import com.google.gwt.junit.client.GWTTestCase;

import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.common.vcs.StatusAndPathInfo;

import java.util.ArrayList;
import java.util.Arrays;

// Tests for the batched file-change merge used by VcsState (#18257): changes
// are applied to the status list in a single pass, an empty status removes
// the entry, and a no-op merge returns null (so no refresh event is fired).
public class VcsStateTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static StatusAndPath entry(String status, String path)
   {
      return StatusAndPath.fromInfo(
            StatusAndPathInfo.create(status, path, path, true, false));
   }

   private static ArrayList<StatusAndPath> list(StatusAndPath... entries)
   {
      return new ArrayList<>(Arrays.asList(entries));
   }

   public void testReplacesExistingEntryInPlace()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"), entry("M ", "b"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, list(entry("D ", "a")));

      assertNotNull(merged);
      assertEquals(2, merged.size());
      assertEquals("a", merged.get(0).getRawPath());
      assertEquals("D ", merged.get(0).getStatus());
      assertEquals("b", merged.get(1).getRawPath());
   }

   public void testAppendsNewEntry()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, list(entry("??", "b")));

      assertNotNull(merged);
      assertEquals(2, merged.size());
      assertEquals("a", merged.get(0).getRawPath());
      assertEquals("b", merged.get(1).getRawPath());
      assertEquals("??", merged.get(1).getStatus());
   }

   public void testEmptyStatusRemovesEntry()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"), entry("M ", "b"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, list(entry("", "a")));

      assertNotNull(merged);
      assertEquals(1, merged.size());
      assertEquals("b", merged.get(0).getRawPath());
   }

   public void testWhitespaceStatusRemovesEntry()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, list(entry("  ", "a")));

      assertNotNull(merged);
      assertTrue(merged.isEmpty());
   }

   public void testRemovalOfUnknownPathIsNoOp()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, list(entry("", "zzz")));

      assertNull(merged);
   }

   public void testNoChangesIsNoOp()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, new ArrayList<>());

      assertNull(merged);
   }

   public void testChangesApplyInOrderSoLastWriteWins()
   {
      ArrayList<StatusAndPath> status = list(entry("M ", "a"), entry("M ", "b"));

      // a modified then reverted within one batch: the later (empty) status
      // must win
      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status, list(entry("D ", "a"), entry("", "a")));

      assertNotNull(merged);
      assertEquals(1, merged.size());
      assertEquals("b", merged.get(0).getRawPath());
   }

   public void testBulkMergePreservesOrderAndAppendsAdditions()
   {
      ArrayList<StatusAndPath> status = list(
            entry("M ", "a"), entry("M ", "b"), entry("M ", "c"));

      ArrayList<StatusAndPath> merged = VcsState.mergeFileChanges(
            status,
            list(entry("", "b"), entry("D ", "c"), entry("??", "d")));

      assertNotNull(merged);
      assertEquals(3, merged.size());
      assertEquals("a", merged.get(0).getRawPath());
      assertEquals("c", merged.get(1).getRawPath());
      assertEquals("D ", merged.get(1).getStatus());
      assertEquals("d", merged.get(2).getRawPath());
   }
}
