/*
 * ToolbarSeparator.java
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

package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.OrientationValue;
import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.theme.res.ThemeResources;

public class ToolbarSeparator extends DecorativeImage
{
   public ToolbarSeparator()
   {
      this(false);
   }

   /**
    * @param significant should this separator be mentioned by accessibility tools?
    */
   public ToolbarSeparator(boolean significant)
   {
      super(ThemeResources.INSTANCE.toolbarSeparator());
      setStylePrimaryName(
               ThemeResources.INSTANCE.themeStyles().toolbarSeparator());
      if (significant)
      {
         Roles.getSeparatorRole().set(getElement());
         Roles.getSeparatorRole().setAriaOrientationProperty(getElement(), OrientationValue.VERTICAL);
      }
   }
}
