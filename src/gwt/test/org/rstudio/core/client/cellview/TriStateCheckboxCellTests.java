/*
 * TriStateCheckboxCellTests.java
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
package org.rstudio.core.client.cellview;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class TriStateCheckboxCellTests extends GWTTestCase
{
   private static String render(Boolean value)
   {
      SafeHtmlBuilder sb = new SafeHtmlBuilder();
      new TriStateCheckboxCell<String>(null).render(new Cell.Context(0, 0, "key"), value, sb);
      return sb.toSafeHtml().asString();
   }

   private static void assertTagged(String html, String glyph)
   {
      assertTrue(html, html.startsWith("<img "));
      assertTrue(html, html.contains("class=\"rstudio-forced-colors-glyph " + glyph + "\""));
   }

   public void testCheckedIsTaggedForContrastThemes()
   {
      assertTagged(render(true), "rstudio-glyph-checkbox-on");
   }

   public void testUncheckedIsTaggedForContrastThemes()
   {
      assertTagged(render(false), "rstudio-glyph-checkbox-off");
   }

   public void testIndeterminateIsTaggedForContrastThemes()
   {
      assertTagged(render(null), "rstudio-glyph-checkbox-indeterminate");
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
}
