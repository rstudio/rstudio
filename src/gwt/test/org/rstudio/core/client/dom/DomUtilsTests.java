/*
 * DomUtilsTests.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.Document;
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
