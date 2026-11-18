/*
 * BaseMenuBarTests.java
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
package org.rstudio.core.client.command;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.MenuItem;

public class BaseMenuBarTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtSetUp()
   {
      menu_ = new BaseMenuBar(true);
      first_ = menu_.addItem("First", () -> {});
      second_ = menu_.addItem("Second", () -> {});
      fired_ = new ArrayList<>();
      menu_.addSelectionHandler(event -> fired_.add(event.getSelectedItem()));
   }

   public void testSelectionEventFiresAsHighlightMoves()
   {
      menu_.selectItem(first_);
      menu_.selectItem(second_);
      menu_.selectItem(null);

      List<MenuItem> expected = new ArrayList<>();
      expected.add(first_);
      expected.add(second_);
      expected.add(null);
      assertEquals(expected, fired_);
   }

   public void testSelectionEventSkipsUnchangedHighlight()
   {
      menu_.selectItem(first_);
      menu_.selectItem(first_);
      assertEquals(1, fired_.size());

      // Clearing the highlight fires once; clearing it again isn't a change
      fired_.clear();
      menu_.selectItem(null);
      menu_.selectItem(null);
      assertEquals(1, fired_.size());
   }

   private BaseMenuBar menu_;
   private MenuItem first_;
   private MenuItem second_;
   private List<MenuItem> fired_;
}
