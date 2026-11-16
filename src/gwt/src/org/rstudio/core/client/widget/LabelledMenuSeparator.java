/*
 * LabelledMenuSeparator.java
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

import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItemSeparator;

// A heading row in a menu. It is text, not a divider: as a separator, screen
// readers announce it as a "splitter".
public class LabelledMenuSeparator extends MenuItemSeparator
{
   // A section heading, below a divider line
   public LabelledMenuSeparator(String text)
   {
      this(text, true);
   }

   // A group heading in a list of items: flush left, with no divider line
   public static LabelledMenuSeparator subheader(String text)
   {
      return new LabelledMenuSeparator(text, false);
   }

   private LabelledMenuSeparator(String text, boolean divider)
   {
      Element el = getElement();
      Label label = new Label(text);
      label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
      if (!divider)
      {
         el.removeAllChildren();
         label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
      }
      el.appendChild(label.getElement());

      el.removeAttribute("role");
      el.removeAttribute("aria-disabled");
   }
}
