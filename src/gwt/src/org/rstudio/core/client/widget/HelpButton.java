/*
 * HelpButton.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;


import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;

public class HelpButton extends Composite
{
   public HelpButton(final String rstudioLinkName)
   {
      Image helpImage = new Image(ThemeResources.INSTANCE.help());
      helpImage.getElement().getStyle().setCursor(Cursor.POINTER);
      helpImage.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            GlobalDisplay globalDisplay = 
                                 RStudioGinjector.INSTANCE.getGlobalDisplay();
            globalDisplay.openRStudioLink(rstudioLinkName);
         }
      });
      
      
      initWidget(helpImage);
   }
}
