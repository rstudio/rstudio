/*
 * LintItemTests.java
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
package org.rstudio.studio.client.workbench.views.output.lint.model;

import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.junit.client.GWTTestCase;

public class LintItemTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testAsAceAnnotationsKeepsTextWhenHtmlIsAlsoSet()
   {
      // Regression #17581: Ace's gutter tooltip uses 'text' for displayText
      // and renders it via createTextNode. If 'text' is dropped when 'html'
      // is set, the html surfaces as literal markup in the tooltip.
      LintItem item = LintItem.create(0, 0, 0, 0, "unexpected token", "error");
      item.setHtml("<span>unexpected token</span>");

      JsArray<LintItem> items = JsArray.createArray().cast();
      items.push(item);

      JsArray<AceAnnotation> annotations = LintItem.asAceAnnotations(items);
      assertEquals(1, annotations.length());
      assertEquals("unexpected token", annotations.get(0).text());
      assertEquals("<span>unexpected token</span>", annotations.get(0).html());
   }

   public void testAsAceAnnotationsPopulatesTextWhenHtmlAbsent()
   {
      LintItem item = LintItem.create(0, 0, 0, 0, "unexpected token", "error");
      JsArray<LintItem> items = JsArray.createArray().cast();
      items.push(item);

      AceAnnotation annotation = LintItem.asAceAnnotations(items).get(0);
      assertEquals("unexpected token", annotation.text());
      assertTrue("html should be empty when not set, got: " + annotation.html(),
                 StringUtil.isNullOrEmpty(annotation.html()));
   }

   public void testAceAnnotationCreateKeepsTextWhenHtmlIsAlsoSet()
   {
      AceAnnotation annotation = AceAnnotation.create(
            0, 0, "<span>msg</span>", "msg", "error", "ace_error", null);
      assertEquals("msg", annotation.text());
      assertEquals("<span>msg</span>", annotation.html());
   }

   public void testGutterTooltipDisplayTextIsRawMessage()
   {
      // Mirrors the regression scenario from #17581: confirm that the value
      // Ace would feed into createTextNode is the raw lint text, not the html.
      LintItem item = LintItem.create(0, 0, 0, 0, "unexpected token", "error");
      item.setHtml("<span>unexpected token</span>");
      JsArray<LintItem> items = JsArray.createArray().cast();
      items.push(item);

      AceAnnotation annotation = LintItem.asAceAnnotations(items).get(0);
      String displayText = simulateAceDisplayText(annotation);
      assertEquals("unexpected token", displayText);
      assertFalse("displayText must not contain <span> markup, got: " + displayText,
                  displayText.contains("<span"));
   }

   // Mirrors Ace's Gutter.setAnnotations: prefers 'text', falls back to 'html'.
   private static native String simulateAceDisplayText(AceAnnotation annotation) /*-{
      return annotation.text ? annotation.text : (annotation.html || "");
   }-*/;
}
