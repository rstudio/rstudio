/*
 * CustomMenuItemSeparator.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.Widget;

public abstract class CustomMenuItemSeparator extends MenuItemSeparator
{
   // the actual widget to be used as a separator
   public abstract Widget createMainWidget();
   
   public Widget getMainWidget()
   {
      return widget_;
   }
   
   public CustomMenuItemSeparator()
   {
      widget_ = createMainWidget();
      getElement().removeAllChildren();
      getElement().appendChild(widget_.getElement());
   }
   
   private final Widget widget_;
}
