/*
 * CompletionListAria.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.dom.DomUtils;

/**
 * ARIA roles and states for completion popups whose items are announced
 * through the editor input's aria-activedescendant, shared so every such
 * popup reads the same way to a screen reader.
 */
public final class CompletionListAria
{
   private CompletionListAria()
   {
   }

   public static void setListRole(Element list, String ariaLabel)
   {
      if (USE_MENU_ROLES)
         Roles.getMenuRole().set(list);
      else
         Roles.getListboxRole().set(list);
      list.setAttribute("aria-label", ariaLabel);
   }

   /**
    * Gives a list item its role and an id, which aria-activedescendant needs.
    */
   public static void setItemRole(Element item)
   {
      if (USE_MENU_ROLES)
         Roles.getMenuitemRole().set(item);
      else
         Roles.getOptionRole().set(item);
      DomUtils.ensureHasId(item);
   }

   public static void setItemActive(Element item, boolean active)
   {
      if (active)
         item.setAttribute(ACTIVE_STATE, "true");
      else
         item.removeAttribute(ACTIVE_STATE);
   }

   // Ace's own completion popup uses menu roles in Safari, for VoiceOver;
   // match it so all completion popups read the same way there.
   private static final boolean USE_MENU_ROLES = BrowseCap.isSafari();
   private static final String ACTIVE_STATE =
         USE_MENU_ROLES ? "aria-current" : "aria-selected";
}
