/*
 * ModalDialogTrackerTests.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.PopupPanel;

public class ModalDialogTrackerTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   // Show the panel as a tracked modal and return its aria-modal attribute.
   private String ariaModalWhenShown(PopupPanel panel)
   {
      panel.show();
      ModalDialogTracker.onShow(panel);
      try
      {
         return panel.getElement().getAttribute("aria-modal");
      }
      finally
      {
         ModalDialogTracker.onHide(panel);
         panel.hide();
      }
   }

   public void testDialogIsMarkedModal()
   {
      PopupPanel panel = new PopupPanel();
      Roles.getDialogRole().set(panel.getElement());
      assertEquals("true", ariaModalWhenShown(panel));
   }

   public void testAlertDialogIsMarkedModal()
   {
      // axe only treats an alertdialog as an open modal when it has
      // aria-modal="true" (#19251)
      PopupPanel panel = new PopupPanel();
      Roles.getAlertdialogRole().set(panel.getElement());
      assertEquals("true", ariaModalWhenShown(panel));
   }

   public void testPanelWithoutDialogRoleIsNotMarkedModal()
   {
      // aria-modal is only valid on dialog and alertdialog roles
      assertEquals("", ariaModalWhenShown(new PopupPanel()));
   }
}
