/*
 * CodeSearchSuggestionTests.java
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

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.icons.code.CodeIcons;
import org.rstudio.studio.client.workbench.codesearch.model.FileItem;
import org.rstudio.studio.client.workbench.codesearch.model.SourceItem;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

public class CodeSearchSuggestionTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCodeResultIconIsNamedByKind()
   {
      SourceItem item = JsonUtils.safeEval(
            "{\"type\": 1, \"name\": \"addOne\", \"context\": \"~/addOne.R\"}");
      Element img = iconOf(new CodeSearchSuggestion(item, null));
      assertEquals("Function", img.getAttribute("alt"));
   }

   public void testXRefResultIconIsNamedByXRefType()
   {
      Element img = iconOf(new CodeSearchSuggestion(xrefItem(10, "thm", "pythagoras"), null));
      assertEquals("Theorem", img.getAttribute("alt"));
   }

   // Examples are theorem-style blocks, so they share the theorem icon.
   public void testExampleXRefShowsTheoremIcon()
   {
      Element img = iconOf(new CodeSearchSuggestion(xrefItem(10, "exm", "fizz"), null));
      assertEquals(new ImageResource2x(CodeIcons.INSTANCE.function2x()).getSafeUri().asString(),
            img.getAttribute("src"));
      assertEquals("Example", img.getAttribute("alt"));
   }

   // The file name's extension already says what the file-type icon shows.
   public void testFileResultIconIsDecorative()
   {
      FileItem item = JsonUtils.safeEval(
            "{\"filename\": \"addOne.R\", \"path\": \"~/addOne.R\"}");
      Element img = iconOf(new CodeSearchSuggestion(item));
      assertTrue(img.hasAttribute("alt"));
      assertEquals("", img.getAttribute("alt"));
   }

   private static SourceItem xrefItem(int type, String xrefType, String id)
   {
      return JsonUtils.safeEval(
            "{\"type\": " + type + ", \"name\": \"" + xrefType + ":" + id + "\", " +
            "\"context\": \"~/book\", \"metadata\": {\"type\": \"xref\", \"xref\": " +
            "{\"file\": \"01-intro.Rmd\", \"type\": \"" + xrefType + "\", " +
            "\"id\": \"" + id + "\"}}}");
   }

   private static Element iconOf(CodeSearchSuggestion suggestion)
   {
      DivElement row = Document.get().createDivElement();
      row.setInnerHTML(suggestion.getDisplayString());
      assertEquals(1, row.getElementsByTagName("img").getLength());
      return row.getElementsByTagName("img").getItem(0);
   }
}
