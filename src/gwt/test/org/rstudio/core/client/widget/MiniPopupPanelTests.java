/*
 * MiniPopupPanelTests.java
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Event;

public class MiniPopupPanelTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Fire an Escape keydown through the native preview dispatch. Returns
   // true when no preview handler cancelled it -- i.e. nothing is left
   // registered that would swallow the keypress.
   private boolean fireEscape()
   {
      NativeEvent event = Document.get().createKeyDownEvent(
            false, false, false, false, KeyCodes.KEY_ESCAPE);
      return Event.fireNativePreviewEvent(event);
   }

   public void testEscapeDismissesPopup()
   {
      MiniPopupPanel panel = new MiniPopupPanel(true);
      panel.show();

      assertFalse(fireEscape());
      assertFalse(panel.isShowing());

      assertTrue(fireEscape());
   }

   public void testRepeatedShowDoesNotLeakEscapeHandlers()
   {
      // MathJax.renderPopup() shows the popup once offscreen and once more
      // after typesetting; the second show() must not leave an extra Escape
      // handler behind (#18474)
      MiniPopupPanel panel = new MiniPopupPanel(true);
      panel.show();
      panel.show();

      assertFalse(fireEscape());
      assertFalse(panel.isShowing());

      assertTrue(fireEscape());
   }

   public void testAutoHideDismissalRemovesEscapeHandler()
   {
      // clicking away from an auto-hide popup dismisses it via the
      // hide(boolean) overload (PopupPanel.previewNativeEvent), not hide();
      // handlers must be released on that path too
      MiniPopupPanel panel = new MiniPopupPanel(true);
      panel.show();

      panel.hide(true);
      assertFalse(panel.isShowing());

      assertTrue(fireEscape());
   }
}
