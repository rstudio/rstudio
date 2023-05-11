/*
 * HelpButton.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;

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

import elemental2.dom.URL;

public class HelpButton extends FocusWidget
                        implements HasClickHandlers
{
   private final static int kDefaultTopMargin = 3;
   
   public static void addHelpButton(SelectWidget selectWidget,
                                    String rstudioLinkName,
                                    String altText)
   {
      addHelpButton(selectWidget, rstudioLinkName, altText, kDefaultTopMargin);
   }
   
   public static void addHelpButton(SelectWidget selectWidget,
                                    String rstudioLinkName,
                                    String altText,
                                    int marginTop)
   {
      selectWidget.addWidget(createHelpButton(rstudioLinkName, altText, marginTop));
   }

   public static HelpButton createHelpButton(String rstudioLinkName, String altText)
   {
      return createHelpButton(rstudioLinkName, altText, kDefaultTopMargin);
   }
   
  
   public static HelpButton createHelpButton(String rstudioLinkName, String altText, int marginTop)
   {
      HelpButton helpButton = new HelpButton(rstudioLinkName, false, altText);
      Style style = helpButton.getElement().getStyle();
      style.setMarginTop(marginTop, Unit.PX);
      style.setMarginLeft(2, Unit.PX);
      return helpButton;
   }
   
   public static HorizontalPanel checkBoxWithHelp(CheckBox checkBox, HelpButton helpButton)
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.add(checkBox);
      Style helpStyle = helpButton.getElement().getStyle();
      helpStyle.setMarginTop(1, Unit.PX);
      helpStyle.setMarginLeft(6, Unit.PX);
      panel.add(helpButton);
      return panel;
   }

   /**
    * @param rstudioLinkName
    * @param altText a11y name
    */
   public HelpButton(String rstudioLinkName, String altText)
   {
      this(rstudioLinkName, true, altText);
   }

   /**
    * @param rstudioLinkName
    * @param includeVersionInfo
    * @param altText a11y name
    */
   public HelpButton(final String rstudioLinkName,
                     final boolean includeVersionInfo,
                     final String altText)
   {
      this(altText, event -> {
         GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
         globalDisplay.openRStudioLink(rstudioLinkName, includeVersionInfo);
      });
   }
   
   public HelpButton(final URL url, final String altText)
   {
      this(altText, event -> {
         GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
         globalDisplay.openWindow(url.toString());
      });
   }
   
   public HelpButton(String altText, ClickHandler handler)
   {
      ButtonElement button = Document.get().createPushButtonElement();
      button.setClassName("rstudio-HelpButton");

      Image helpImage = new Image(new ImageResource2x(ThemeResources.INSTANCE.help2x()));
      helpImage.getElement().getStyle().setCursor(Cursor.POINTER);
      helpImage.setAltText(altText);
      button.insertFirst(helpImage.getElement());
      setElement(button);
      
      addClickHandler(handler);

      this.helpImage_ = helpImage;
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return addDomHandler(handler, ClickEvent.getType());
   }

   public void setTitle(String title) {
      this.helpImage_.setTitle(title);
   }

   public void setAltText(String altText) {
      this.helpImage_.setAltText(altText);
   }

   private Image helpImage_;
}
