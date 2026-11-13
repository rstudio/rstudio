/*
 * CompletionPopupPanelTests.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;

import com.google.gwt.junit.client.GWTTestCase;

public class CompletionPopupPanelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtTearDown()
   {
      if (panel_ != null)
         panel_.hide();
   }

   private static QualifiedName[] completions()
   {
      return new QualifiedName[] {
            new QualifiedName("library", "base"),
            new QualifiedName("library.dynam", "base"),
            new QualifiedName("library.dynam.unload", "base"),
      };
   }

   private void show(Rectangle line)
   {
      // Prefers the top, as when an AI assistant is enabled, within an
      // editor whose text area starts at the top of the page.
      Rectangle viewport = new Rectangle(0, 0, 800, 3000);
      panel_ = new CompletionPopupPanel(null);
      panel_.showCompletionValues(
            completions(),
            new PopupPositioner(line, panel_, false, viewport),
            false);
   }

   public void testListRunsTopDownWhenNoRoomAbove()
   {
      // #19614: on the editor's first line the popup goes below, so the best
      // match belongs at its top, next to the line, and starts selected.
      Rectangle line = new Rectangle(10, 0, 8, 13);
      show(line);

      assertTrue(panel_.getPopupTop() > line.getBottom());
      assertEquals("library", panel_.getItems()[0].name);
      assertEquals("library", panel_.getSelectedValue().name);
   }

   public void testListRunsBottomUpWhenAbove()
   {
      Rectangle line = new Rectangle(10, 2000, 8, 13);
      show(line);

      QualifiedName[] items = panel_.getItems();
      assertTrue(panel_.getPopupTop() < line.getTop());
      assertEquals("library", items[items.length - 1].name);
      assertEquals("library", panel_.getSelectedValue().name);
   }

   private CompletionPopupPanel panel_;
}
