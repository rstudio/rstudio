/*
 * BinarySplitLayoutPanelTests.java
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
package org.rstudio.core.client.layout;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class BinarySplitLayoutPanelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testValueNowReportsPositionFromTop()
   {
      // 207px panel with a 7px splitter: a 200px range.
      BinarySplitLayoutPanel panel = createAttachedPanel(207);

      panel.setSplitterPos(50, true);
      panel.forceLayout();
      assertEquals("25", getAriaValueNow(panel));

      // Measured from the bottom: the splitter's top is at 200 - 50 = 150.
      panel.setSplitterPos(50, false);
      panel.forceLayout();
      assertEquals("75", getAriaValueNow(panel));

      panel.removeFromParent();
   }

   public void testValueNowClampedInPanelShorterThanTwoSplitters()
   {
      // setSplitterPos() keeps the position at least the splitter's height (7px), which is
      // past the end of this panel's 5px range.
      BinarySplitLayoutPanel panel = createAttachedPanel(12);

      panel.setSplitterPos(0, true);
      panel.forceLayout();
      assertEquals("100", getAriaValueNow(panel));

      panel.setSplitterPos(0, false);
      panel.forceLayout();
      assertEquals("0", getAriaValueNow(panel));

      panel.removeFromParent();
   }

   public void testNoValueNowWhenDetached()
   {
      BinarySplitLayoutPanel panel = createPanel();
      panel.setSplitterPos(50, true);
      panel.forceLayout();
      assertEquals("", getAriaValueNow(panel));
   }

   private static BinarySplitLayoutPanel createPanel()
   {
      return new BinarySplitLayoutPanel(
            "test", new Widget[] { new Label("top"), new Label("bottom") }, 7);
   }

   private static BinarySplitLayoutPanel createAttachedPanel(int height)
   {
      BinarySplitLayoutPanel panel = createPanel();
      RootPanel.get().add(panel);
      panel.setSize("100px", height + "px");
      return panel;
   }

   private static String getAriaValueNow(BinarySplitLayoutPanel panel)
   {
      // The splitter is the last child the panel adds.
      Widget splitter = panel.getWidget(panel.getWidgetCount() - 1);
      return splitter.getElement().getAttribute("aria-valuenow");
   }
}
