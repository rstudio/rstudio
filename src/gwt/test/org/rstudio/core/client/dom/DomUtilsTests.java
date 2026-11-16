/*
 * DomUtilsTests.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

public class DomUtilsTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
   
   // Nests divs with the given overflow-y values, outermost first, in the
   // page, and returns the innermost.
   private static Element nest(String... overflows)
   {
      Element parent = Document.get().getBody();
      for (String overflow : overflows)
      {
         DivElement div = Document.get().createDivElement();
         div.getStyle().setProperty("overflowY", overflow);
         parent.appendChild(div);
         parent = div;
      }
      return parent;
   }

   public void testFindScrollingAncestorSkipsClippingAncestors()
   {
      Element inner = nest("auto", "hidden", "visible", "hidden");
      Element scroller = inner.getParentElement().getParentElement().getParentElement();
      Assert.assertEquals(scroller, DomUtils.findScrollingAncestor(inner));
      scroller.removeFromParent();
   }

   public void testFindScrollingAncestorAcceptsScroll()
   {
      Element inner = nest("scroll", "visible");
      Element scroller = inner.getParentElement();
      Assert.assertEquals(scroller, DomUtils.findScrollingAncestor(inner));
      scroller.removeFromParent();
   }

   public void testFindScrollingAncestorIgnoresElementItself()
   {
      Element inner = nest("hidden", "auto");
      Assert.assertNull(DomUtils.findScrollingAncestor(inner));
      inner.getParentElement().removeFromParent();
   }

   public void testFindScrollingAncestorIgnoresPage()
   {
      // A scrolling page doesn't count: the viewport is then the editor.
      Element body = Document.get().getBody();
      String saved = body.getStyle().getProperty("overflowY");
      body.getStyle().setProperty("overflowY", "auto");
      Element inner = nest("hidden");
      try
      {
         Assert.assertNull(DomUtils.findScrollingAncestor(inner));
      }
      finally
      {
         body.getStyle().setProperty("overflowY", saved);
         inner.removeFromParent();
      }
   }

   public void testCountLinesRecursive()
   {
      PreElement pre = Document.get().createPreElement();
      SpanElement s1 = Document.get().createSpanElement();
      s1.setInnerText("Line1\nLine2\n");
      pre.appendChild(s1);
      SpanElement s2 = Document.get().createSpanElement();
      s2.setInnerText("Line3\n");
      pre.appendChild(s2);
      
      Assert.assertEquals(3, DomUtils.countLines(pre, true));
   }
   
   public void testTrimExcess()
   {
      PreElement pre = Document.get().createPreElement();
      pre.setInnerText("Line1\nLine2\nLine3\n");
      int trimmed = DomUtils.trimLines(pre, 2);
      Assert.assertEquals("Line3\n", pre.getInnerText());
      Assert.assertEquals(trimmed, 2);
   }
   
   public void testMakeAbsoluteUrl()
   {
      // test prefixing relative URLs
      String url1 = "path/to/file";
      String absolute1 = DomUtils.makeAbsoluteUrl(url1);
      Assert.assertEquals(absolute1.substring(0, 4), "http");
      
      // already-absolute URLs should be untouched
      String url2 = "https://hostname:port/endpoint?query=yes";
      String absolute2 = DomUtils.makeAbsoluteUrl(url2);
      Assert.assertEquals(url2, absolute2);
   }
}
