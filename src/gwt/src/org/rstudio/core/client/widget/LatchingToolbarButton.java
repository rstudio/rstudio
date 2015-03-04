/*
 * LatchingToolbarButton.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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

import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;

public class LatchingToolbarButton extends ToolbarButton
{

   public LatchingToolbarButton(String text, 
                                ImageResource leftImage,
                                ClickHandler clickHandler)
   {
      super(text, leftImage, clickHandler);
      getElement().addClassName(ThemeStyles.INSTANCE.toolbarButtonLatchable());
   }
   
   public void setLatched(boolean latched)
   {
      // no op if we're already in requested state
      if (latched == latched_)
         return;
      latched_ = latched;
      
      if (latched_)
         getElement().addClassName(ThemeStyles.INSTANCE.toolbarButtonLatched());
      else
         getElement().removeClassName(ThemeStyles.INSTANCE.toolbarButtonLatched());
   }
   
   private boolean latched_ = false;
}
