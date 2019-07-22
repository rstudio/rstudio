/*
 * HelpButton.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FocusWidget;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;

public class HelpButton extends FocusWidget
                        implements HasClickHandlers
{
   /**
    * @param selectWidget
    * @param rstudioLinkName
    * @param title a11y name
    */
   public static void addHelpButton(SelectWidget selectWidget,
                                    String rstudioLinkName,
                                    String title)
   {
      selectWidget.addWidget(createHelpButton(rstudioLinkName, title));
   }

   /**
    * @param rstudioLinkName
    * @param title a11y name
    * @return HelpButton
    */
   public static HelpButton createHelpButton(String rstudioLinkName, String title)
   {
      HelpButton helpButton = new HelpButton(rstudioLinkName, title);
      Style style = helpButton.getElement().getStyle();
      style.setMarginTop(3, Unit.PX);
      style.setMarginLeft(4, Unit.PX);
      return helpButton;
   }

   /**
    * @param rstudioLinkName
    * @param title a11y name
    */
   public HelpButton(String rstudioLinkName, String title)
   {
      this(rstudioLinkName, true, title);
   }

   /**
    * @param rstudioLinkName
    * @param includeVersionInfo
    * @param title a11y name
    */
   public HelpButton(final String rstudioLinkName,
                     final boolean includeVersionInfo,
                     final String title)
   {
      ButtonElement button = Document.get().createPushButtonElement();
      button.setClassName("rstudio-HelpButton");

      Image helpImage = new Image(new ImageResource2x(ThemeResources.INSTANCE.help2x()));
      helpImage.getElement().getStyle().setCursor(Cursor.POINTER);
      helpImage.setAltText(title);
      button.insertFirst(helpImage.getElement());

      addClickHandler(event -> {
         GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
         globalDisplay.openRStudioLink(rstudioLinkName, includeVersionInfo);
      });

      setElement(button);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }
}
