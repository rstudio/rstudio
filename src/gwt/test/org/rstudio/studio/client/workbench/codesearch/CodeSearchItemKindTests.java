/*
 * CodeSearchItemKindTests.java
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
package org.rstudio.studio.client.workbench.codesearch;

import org.rstudio.studio.client.workbench.codesearch.model.SourceItem;

import com.google.gwt.junit.client.GWTTestCase;

public class CodeSearchItemKindTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCodeItemsAreLabeledByType()
   {
      assertEquals("Function", CodeSearchItemKind.label(SourceItem.FUNCTION, null));
      assertEquals("Method", CodeSearchItemKind.label(SourceItem.METHOD, null));
      assertEquals("Enum value", CodeSearchItemKind.label(SourceItem.ENUM_VALUE, null));
      assertEquals("Package documentation",
            CodeSearchItemKind.label(SourceItem.PACKAGE, null));
   }

   // The backend gives R variables the NONE type.
   public void testNoneTypeIsVariable()
   {
      assertEquals("Variable", CodeSearchItemKind.label(SourceItem.NONE, null));
   }

   // Theorem-style xrefs share the function icon, so labeling by icon would be wrong.
   public void testTheoremXRefIsLabeledByXRefType()
   {
      assertEquals("Theorem", CodeSearchItemKind.label(SourceItem.MATH, "thm"));
      assertEquals("Equation", CodeSearchItemKind.label(SourceItem.MATH, "eq"));
      assertEquals("Example", CodeSearchItemKind.label(SourceItem.MATH, "exm"));
   }

   public void testHeadingXRefIncludesLevel()
   {
      assertEquals("Heading level 3", CodeSearchItemKind.label(SourceItem.SECTION, "h3"));
   }

   public void testTableXRefAliasesShareLabel()
   {
      assertEquals("Table", CodeSearchItemKind.label(SourceItem.TABLE, "tab"));
      assertEquals("Table", CodeSearchItemKind.label(SourceItem.TABLE, "tbl"));
   }

   // An unrecognized xref type, such as a Quarto custom cross-reference type,
   // has no known kind; it must not read as "Variable".
   public void testUnknownXRefTypeIsUnlabeled()
   {
      assertEquals("", CodeSearchItemKind.label(SourceItem.NONE, "custom"));
   }
}
