/*
 * ElementIdsTests.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;

public class ElementIdsTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testExactMatchElementId()
   {
      Element ele = DOM.createDiv();
      ElementIds.assignElementId(ele, ElementIds.CONSOLE_INPUT);
      assertTrue(ElementIds.isInstanceOf(ele, ElementIds.CONSOLE_INPUT));
   }

   public void testMismatchedElementId()
   {
      Element ele = DOM.createDiv();
      ElementIds.assignElementId(ele, ElementIds.CONSOLE_INPUT);
      assertFalse(ElementIds.isInstanceOf(ele, ElementIds.CONSOLE_OUTPUT));
   }

   public void testMatchingUndupedElementId()
   {
      Element ele = DOM.createDiv();
      ElementIds.assignElementId(ele, ElementIds.CONSOLE_INPUT + "_123");
      assertTrue(ElementIds.isInstanceOf(ele, ElementIds.CONSOLE_INPUT));
   }

   public void testMisMatchedUndupedElementId()
   {
      Element ele = DOM.createDiv();
      assertFalse(ElementIds.isInstanceOf(ele, ElementIds.CONSOLE_OUTPUT));
   }

   public void testElementIdWithoutStandardPrefix()
   {
      Element ele = DOM.createDiv();
      ele.setId("some-randomID");
      assertFalse(ElementIds.isInstanceOf(ele, ElementIds.CONSOLE_OUTPUT));
   }

   public void testElementIdWithoutStandardSuffix()
   {
      Element ele = DOM.createDiv();
      ElementIds.assignElementId(ele, ElementIds.CONSOLE_INPUT + "_123_");
      assertFalse(ElementIds.isInstanceOf(ele, ElementIds.CONSOLE_OUTPUT));
   }
}
