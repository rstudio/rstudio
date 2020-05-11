/*
 * SafeHtmlUtilTests.java
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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class SafeHtmlUtilTests extends GWTTestCase
{
   
   /**
    * Are multiple needles discovered in the haystack?
    */
   public void testHighlightSearchMatch()
   {
      String haystack = "Sphinx of black quartz, judge my vow.";
      String[] needles = { "black", "judge" };

      SafeHtmlBuilder sb = new SafeHtmlBuilder();

      SafeHtmlUtil.highlightSearchMatch(sb, haystack, needles, "match");
      
      assertEquals(
            "Sphinx of <span class=\"match\">black</span> quartz, " +
            "<span class=\"match\">judge</span> my vow.",
            sb.toSafeHtml().asString());
   }

   /**
    * Is the haystack undisturbed when it contains no needles?
    */
   public void testNoSearchMatch()
   {
      String haystack = "Sphinx of black quartz, judge my vow.";
      String[] needles = { "watermelon" };

      SafeHtmlBuilder sb = new SafeHtmlBuilder();

      SafeHtmlUtil.highlightSearchMatch(sb, haystack, needles, "match");
      
      assertEquals(haystack, sb.toSafeHtml().asString());
   }

   /**
    * Are overlapping search matches only emitted once?
    */
   public void testOverlappingMatch()
   {
      String haystack = "A closed mouth gathers no foot.";
      String[] needles = { "gather", "gat", "her" };

      SafeHtmlBuilder sb = new SafeHtmlBuilder();

      SafeHtmlUtil.highlightSearchMatch(sb, haystack, needles, "match");
      
      assertEquals(
            "A closed mouth <span class=\"match\">gather</span>s no foot.",
            sb.toSafeHtml().asString());
            
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
}
