/*
 * SecondaryToolbar.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

public class SecondaryToolbar extends Toolbar
{
   public SecondaryToolbar(String label)
   {
      this(false, label);
   }
   
   public SecondaryToolbar(boolean appearAsPrimary, String label)
   {
      super(label);

      if (!appearAsPrimary)
         addStyleName(styles_.rstheme_secondaryToolbar());

      // size the wrapper to fill the layout slot reserved by getHeight()
      getWrapper().addStyleName(styles_.tallerToolbarWrapper());
   }
   
   @Override
   public void manageSeparators()
   {
      super.manageSeparators();
   }

   // The wrapper's -1px top margin tucks its first pixel row under the pane
   // above, so this slot shows 24px of toolbar, room for a 24px target
   // (WCAG 2.5.8), over a 1px bottom border.
   @Override
   public int getHeight()
   {
      return 25;
   }
}
