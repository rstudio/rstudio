/*
 * StatusBarPopupRequest.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.status;

import com.google.gwt.user.client.ui.MenuItem;

public class StatusBarPopupRequest
{
   public StatusBarPopupRequest(StatusBarPopupMenu menu,
                                MenuItem defaultMenuItem)
   {
      menu_ = menu;
      defaultMenuItem_ = defaultMenuItem;
   }

   public StatusBarPopupMenu getMenu()
   {
      return menu_;
   }

   public MenuItem getDefaultMenuItem()
   {
      return defaultMenuItem_;
   }

   private final StatusBarPopupMenu menu_;
   private final MenuItem defaultMenuItem_;
}
