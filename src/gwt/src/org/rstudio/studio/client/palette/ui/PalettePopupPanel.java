/*
 * PalettePopupPanel.java
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
package org.rstudio.studio.client.palette.ui;

import org.rstudio.core.client.widget.ModalPopupPanel;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

/**
 * The modal popup that hosts a palette, such as the Command Palette. Shared
 * so that a fix to how one palette's popup is set up reaches every palette.
 */
public class PalettePopupPanel extends ModalPopupPanel
{
   public PalettePopupPanel(String ariaLabel, boolean closeOnEscape)
   {
      super(true,  // Auto hide
            true,  // Modal
            false, // Glass (main window overlay)
            closeOnEscape);

      // Required for accessibility (see #6962)
      getElement().setTabIndex(-1);

      // Copy classes from the root RStudio container onto this panel, so that
      // it inherits the theme's colors.
      Element root = Document.get().getElementById("rstudio_container");
      if (root != null)
         addStyleName(root.getClassName());

      Roles.getDialogRole().set(getElement());
      Roles.getDialogRole().setAriaLabelProperty(getElement(), ariaLabel);
   }

   @Override
   public void show()
   {
      super.show();

      // Above the pane splitters, which would otherwise overlap the popup.
      // Set after showing, which assigns a z-index of its own.
      getElement().getStyle().setZIndex(250);
   }
}
