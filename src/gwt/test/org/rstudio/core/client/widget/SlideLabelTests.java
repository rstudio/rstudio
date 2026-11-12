/*
 * SlideLabelTests.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.junit.client.GWTTestCase;

public class SlideLabelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testPercentageIsAbsentUnlessReserved()
   {
      SlideLabel label = new SlideLabel(true);

      // the column is hidden by its style rule, which holds nothing to size it
      assertEquals("", label.percentColumn_.getStyle().getDisplay());
      assertEquals("", label.percentSizer_.getInnerText());
      assertEquals("", label.percent_.getInnerText());
      assertFalse(label.innerTable_.hasAttribute("aria-valuenow"));
   }

   public void testReservedPercentageIsSizedForItsWidestValue()
   {
      SlideLabel label = new SlideLabel(true);
      label.reservePercent();

      // room for 100% from the start, and nothing claimed before a report
      assertEquals("table-cell", label.percentColumn_.getStyle().getDisplay());
      assertEquals("100%", label.percentSizer_.getInnerText());
      assertEquals("true", label.percentSizer_.getAttribute("aria-hidden"));
      assertEquals("", label.percent_.getInnerText());
      assertFalse(label.innerTable_.hasAttribute("aria-valuenow"));
   }

   public void testSetPercentShowsAndExposesTheValue()
   {
      SlideLabel label = new SlideLabel(true);
      label.reservePercent();

      label.setPercent(0);
      assertEquals("0%", label.percent_.getInnerText());
      assertEquals("0", label.innerTable_.getAttribute("aria-valuenow"));

      label.setPercent(42);
      assertEquals("42%", label.percent_.getInnerText());
      assertEquals("42", label.innerTable_.getAttribute("aria-valuenow"));

      // the sizer is untouched: the label's width does not follow the value
      assertEquals("100%", label.percentSizer_.getInnerText());
   }

   public void testSetPercentKeepsToWhatTheSizerHolds()
   {
      SlideLabel label = new SlideLabel(true);
      label.reservePercent();

      label.setPercent(150);
      assertEquals("100%", label.percent_.getInnerText());
      assertEquals("100", label.innerTable_.getAttribute("aria-valuenow"));

      label.setPercent(-3);
      assertEquals("0%", label.percent_.getInnerText());
      assertEquals("0", label.innerTable_.getAttribute("aria-valuenow"));
   }
}
