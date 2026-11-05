/*
 * MenuItemAccessibilityTests.java
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

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

public class MenuItemAccessibilityTests extends GWTTestCase
{
   private static String aria(String shortcut)
   {
      return KeySequence.fromShortcutString(shortcut).toAriaKeyShortcuts();
   }

   public void testModifiersUseAriaNamesInFixedOrder()
   {
      assertEquals("Control+Alt+Shift+Meta+K", aria("Cmd+Shift+Alt+Ctrl+K"));
      assertEquals("Alt+Shift+K", aria("Alt+Shift+K"));
      assertEquals("F1", aria("F1"));
   }

   public void testKeysUseKeyboardEventKeyValues()
   {
      assertEquals("Control+ArrowLeft", aria("Ctrl+Left"));
      assertEquals("Control+ArrowDown", aria("Ctrl+Down"));
      assertEquals("Control+Space", aria("Ctrl+Space"));
      assertEquals("Control+Shift+Enter", aria("Ctrl+Shift+Enter"));
      assertEquals("Control+Shift+PageUp", aria("Ctrl+Shift+PageUp"));
      assertEquals("Shift+Meta+Home", aria("Cmd+Shift+Home"));
      assertEquals("Control+=", aria("Ctrl+="));
      assertEquals("Control+\\", aria("Ctrl+Backslash"));
      assertEquals("Alt+-", aria("Alt+-"));
      assertEquals("Control+Shift+/", aria("Ctrl+Shift+/"));
   }

   public void testInexpressibleShortcutsHaveNoAriaValue()
   {
      assertNull(aria(""));
      assertNull(aria("Ctrl+K Ctrl+S"));
      assertNull(aria("Ctrl++"));
      assertNull(aria("Ctrl+NumpadAdd"));
      assertNull(aria("Ctrl+Numpad0"));
   }

   public void testIconCellIsHiddenAndShortcutCellIsMarked()
   {
      Element label = render(AppCommand.formatMenuLabel(null, "Item", "Ctrl+K"));
      Element iconCell = label.getElementsByTagName("td").getItem(0);
      assertEquals("true", iconCell.getAttribute("aria-hidden"));

      Element shortcutCell = label.getElementsByTagName("td").getItem(2);
      assertEquals(AppCommand.MENU_SHORTCUT_CLASS, shortcutCell.getClassName());
      assertTrue(shortcutCell.getInnerText().endsWith("Ctrl+K"));
   }

   public void testNoShortcutCellWithoutShortcut()
   {
      Element label = render(AppCommand.formatMenuLabel(null, "Item", null));
      assertNull(DomUtils.getFirstElementWithClassName(label, AppCommand.MENU_SHORTCUT_CLASS));
   }

   private static Element render(String html)
   {
      Element div = Document.get().createDivElement();
      div.setInnerHTML(html);
      return div;
   }

   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }
}
