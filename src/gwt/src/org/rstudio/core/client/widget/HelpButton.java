/*
 * HelpButton.java
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
package org.rstudio.core.client.widget;


import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;

public class HelpButton extends Composite
{
   public static void addHelpButton(SelectWidget selectWidget, 
                                    String rstudioLinkName)
   {
      selectWidget.addWidget(createHelpButton(rstudioLinkName));
   }
  
   public static HelpButton createHelpButton(String rstudioLinkName)
   {
      HelpButton helpButton = new HelpButton(rstudioLinkName);
      Style style = helpButton.getElement().getStyle();
      style.setMarginTop(3, Unit.PX);
      style.setMarginLeft(4, Unit.PX);
      return helpButton;
   }
   
   public HelpButton(String rstudioLinkName)
   {
      this(rstudioLinkName, true);
   }
   
   public HelpButton(final String rstudioLinkName, 
                     final boolean includeVersionInfo)
   {
      Image helpImage = new Image(new ImageResource2x(ThemeResources.INSTANCE.help2x()));
      helpImage.getElement().getStyle().setCursor(Cursor.POINTER);
      helpImage.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            GlobalDisplay globalDisplay = 
                                 RStudioGinjector.INSTANCE.getGlobalDisplay();
            globalDisplay.openRStudioLink(rstudioLinkName, includeVersionInfo);
         }
      });
      
      
      initWidget(helpImage);
   }
}
