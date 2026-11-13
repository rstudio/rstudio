/*
 * PopupPositionerTests.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.source.editors.text.r.RCompletionToolTip;

import com.google.gwt.junit.client.GWTTestCase;

public class PopupPositionerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // A line whose top is at y = 113, like line 1 of a Source editor whose
   // text area starts at 113, below the tab bar and editor toolbar.
   private static final Rectangle LINE_ONE = new Rectangle(100, 113, 8, 17);
   private static final int EDITOR_TOP = 113;
   private static final Rectangle WINDOW = new Rectangle(0, 0, 1400, 900);

   public void testPreferTopGoesAboveWhenItFitsInEditor()
   {
      Rectangle line = new Rectangle(100, 400, 8, 17);
      assertFalse(PopupPositioner.isPlacedBelow(
            100, line, false, EDITOR_TOP, WINDOW));
   }

   public void testPreferTopGoesBelowOnFirstLine()
   {
      // #19614: there is no room above line 1 inside the editor.
      assertTrue(PopupPositioner.isPlacedBelow(
            68, LINE_ONE, false, EDITOR_TOP, WINDOW));
   }

   public void testPreferTopGoesBelowRatherThanCoverToolbar()
   {
      // There is room above the line in the window, but not in the editor.
      Rectangle line = new Rectangle(100, 150, 8, 17);
      assertTrue(PopupPositioner.isPlacedBelow(
            68, line, false, EDITOR_TOP, WINDOW));
   }

   public void testPreferTopUsesExactFitAbove()
   {
      // The popup's top lands exactly on the editor's: it clears the line
      // by 25px above the bounds' bottom.
      Rectangle line = new Rectangle(100, EDITOR_TOP + 68 + 25 - 17, 8, 17);
      assertFalse(PopupPositioner.isPlacedBelow(
            68, line, false, EDITOR_TOP, WINDOW));
   }

   public void testPreferTopGoesAboveToolbarRatherThanOffWindow()
   {
      // A short editor low in the window: no room above the line in the
      // editor, nor below it in the window, so use the room above in the
      // window rather than run off its bottom.
      Rectangle line = new Rectangle(100, 718, 8, 17);
      assertFalse(PopupPositioner.isPlacedBelow(
            200, line, false, 700, WINDOW));
   }

   public void testPreferTopGoesBelowWhenNeitherSideFits()
   {
      Rectangle line = new Rectangle(100, 150, 8, 17);
      assertTrue(PopupPositioner.isPlacedBelow(
            2000, line, false, EDITOR_TOP, WINDOW));
   }

   public void testPreferBottomGoesBelowWhenItFits()
   {
      assertTrue(PopupPositioner.isPlacedBelow(
            200, LINE_ONE, true, EDITOR_TOP, WINDOW));
   }

   public void testPreferBottomGoesAboveWhenBelowOverflows()
   {
      // The console input sits at the bottom of the window; its popup goes
      // above even when that overflows the top, as it always has.
      Rectangle line = new Rectangle(100, 870, 8, 17);
      assertFalse(PopupPositioner.isPlacedBelow(
            2000, line, true, EDITOR_TOP, WINDOW));
   }

   // A signature tooltip, 47px tall and 8px from its line, beside a line
   // whose bounds are 80% of its height, as AceEditor.getPositionBounds gives.
   private static int tooltipTop(int lineTop, int lineHeight)
   {
      Rectangle bounds = new Rectangle(100, lineTop, 8, (int) (lineHeight * 0.8));
      Rectangle anchors = RCompletionToolTip.getAnchors(bounds, lineHeight);
      return PopupPositioner.getTopPreferringAbove(
            47, anchors, 8, EDITOR_TOP, WINDOW);
   }

   public void testTooltipGoesAboveWhenItFitsInEditor()
   {
      // Line 10: as before, 28px above the middle of the line's bounds.
      int lineTop = EDITOR_TOP + 9 * 17;
      int middle = lineTop + 13 / 2;
      assertEquals(middle - 28 - 47, tooltipTop(lineTop, 17));
   }

   public void testTooltipGoesBelowWholeLineOnFirstLine()
   {
      // #19614: 8px below the line, not 8px below its middle.
      assertEquals(EDITOR_TOP + 17 + 8, tooltipTop(EDITOR_TOP, 17));
   }

   public void testTooltipClearsTallLine()
   {
      // A 20pt font: the bounds' middle is 14px into a 37px line.
      assertEquals(EDITOR_TOP + 37 + 8, tooltipTop(EDITOR_TOP, 37));
   }
}
