/*
 * AriaUtil.java
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

package org.rstudio.core.client;

import com.google.gwt.aria.client.CheckedValue;
import com.google.gwt.aria.client.MenuitemRole;
import com.google.gwt.aria.client.MenuitemcheckboxRole;
import com.google.gwt.aria.client.MenuitemradioRole;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;

public class AriaUtil
{
   public static boolean isMenuChecked(MenuitemRole role, Element element)
   {
      if (role instanceof MenuitemradioRole)
         return Roles.getMenuitemradioRole().getAriaCheckedState(element) == CheckedValue.TRUE.getAriaValue();
      else if (role instanceof MenuitemcheckboxRole)
         return Roles.getMenuitemcheckboxRole().getAriaCheckedState(element) == CheckedValue.TRUE.getAriaValue();
      else
         return false;
   }
}
