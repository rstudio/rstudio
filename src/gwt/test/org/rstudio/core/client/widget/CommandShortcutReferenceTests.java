/*
 * CommandShortcutReferenceTests.java
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeySequence;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.command.ShortcutInfo;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;

public class CommandShortcutReferenceTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   private static ShortcutInfo shortcut(KeySequence keys, String group, String title)
   {
      return new ShortcutInfo(new KeyboardShortcut(keys, group, title, ""), null);
   }

   private static ShortcutInfo shortcut(String key, int keyCode, String group, String title)
   {
      KeySequence keys = new KeySequence();
      keys.add(new KeyCombination(key, keyCode, KeyboardShortcut.CTRL));
      return shortcut(keys, group, title);
   }

   private static String render(ShortcutInfo... shortcuts)
   {
      List<ShortcutInfo> list = new ArrayList<>(Arrays.asList(shortcuts));
      return CommandShortcutReference.buildHtml(list).asString();
   }

   private static int count(String haystack, String needle)
   {
      int count = 0;
      for (int i = haystack.indexOf(needle); i != -1; i = haystack.indexOf(needle, i + 1))
         count++;
      return count;
   }

   /**
    * Are key names from customized shortcuts escaped rather than rendered as markup?
    */
   public void testEscapesCustomKeyName()
   {
      String html = render(shortcut("<i>q</i>", 0, "Console", "Custom"));

      assertFalse(html, html.contains("<i>"));
      assertTrue(html, html.contains("&lt;i&gt;q&lt;/i&gt;"));
   }

   /**
    * Is a custom key name escaped when its pretty form differs from the plain one,
    * as on macOS? PageUp is abbreviated to PgUp in the pretty form on every platform.
    */
   public void testEscapesCustomKeyNameInPrettyForm()
   {
      KeySequence keys = new KeySequence();
      keys.add(new KeyCombination("PageUp", KeyCodes.KEY_PAGEUP, KeyboardShortcut.CTRL));
      keys.add(new KeyCombination("<i>q</i>", 0, KeyboardShortcut.NONE));
      String html = render(shortcut(keys, "Console", "Custom"));

      assertTrue(html, html.contains("aria-hidden"));
      assertFalse(html, html.contains("<i>"));
      assertEquals(html, 2, count(html, "&lt;i&gt;q&lt;/i&gt;"));
   }

   /**
    * Do groups appear once each, in order of first use, without the hidden group?
    */
   public void testGroupsInFirstUseOrder()
   {
      String html = render(
            shortcut("A", 'A', "Console", "First"),
            shortcut("B", 'B', "Source Editor", "Second"),
            shortcut("C", 'C', "Console", "Third"),
            shortcut("D", 'D', "Not Displayed", "Hidden"));

      assertEquals(html, 2, count(html, "<h2>"));
      int console = html.indexOf("<h2>Console</h2>");
      int editor = html.indexOf("<h2>Source Editor</h2>");
      assertTrue(html, console != -1 && editor > console);

      // both Console entries land in the Console table, before the next heading
      int third = html.indexOf("<td>Third</td>");
      assertTrue(html, third > console && third < editor);
      assertFalse(html, html.contains("Hidden"));
   }

   /**
    * Are shortcuts without a description left out, along with a group that ends up empty?
    */
   public void testSkipsUndescribedShortcuts()
   {
      String html = render(
            shortcut("A", 'A', "Console", "Shown"),
            shortcut("B", 'B', "Files", ""));

      assertTrue(html, html.contains("<td>Shown</td>"));
      assertFalse(html, html.contains("<h2>Files</h2>"));
   }
}
