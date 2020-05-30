/*
 * StyleUtils.java
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

package org.rstudio.studio.client.common;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.theme.res.ThemeStyles;

import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.user.client.ui.Widget;

public class StyleUtils
{
   public static void forceMacScrollbars(Widget widget)
   {
      if (!Desktop.hasDesktopFrame() &&
           BrowseCap.isMacintosh() && 
           !BrowseCap.isFirefox())
      {
         widget.addStyleName(ThemeStyles.INSTANCE.forceMacScrollbars());
      }
   }
}
