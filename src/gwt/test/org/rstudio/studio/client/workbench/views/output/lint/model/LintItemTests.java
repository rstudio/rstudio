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

import com.google.gwt.junit.client.GWTTestCase;

public class LintItemTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testAsAceAnnotationPopulatesText()
   {
      // Regression #17581: Ace's gutter tooltip reads 'text' and renders it via
      // createTextNode. An unpopulated 'text' yields an empty tooltip.
      LintItem item = LintItem.create(0, 0, 0, 0, "unexpected token", "error");
      assertEquals("unexpected token", item.asAceAnnotation().text());
   }

   public void testAsAceAnnotationStripsEscapesFromText()
   {
      LintItem item = LintItem.create(0, 0, 0, 0, "\033[31mred\033[0m", "error");

      assertEquals("red", item.asAceAnnotation().text());
      assertEquals("\033[31mred\033[0m", item.getText());
   }
}
