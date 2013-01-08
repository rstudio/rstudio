/*
 * StatusBarPopupMenu.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;

public class StatusBarPopupMenu extends ScrollableToolbarPopupMenu
{
   public StatusBarPopupMenu()
   {
      super();
      addStyleName(ThemeStyles.INSTANCE.statusBarMenu());
   }

   public void showRelativeToUpward(final UIObject target)
   {
      setPopupPositionAndShow(new PositionCallback()
      {
         public void setPosition(int offsetWidth, int offsetHeight)
         {
            setPopupPosition(target.getAbsoluteLeft(),
                             target.getAbsoluteTop() - offsetHeight);
         }
      });
   }
   
   @Override
   protected int getMaxHeight()
   {
      return 350;
   }
}
