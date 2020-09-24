/*
 * ThemedPopupPanel.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import org.rstudio.studio.client.application.ui.RStudioThemes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;

public class ThemedPopupPanel extends DecoratedPopupPanel
{
   public interface Resources extends ClientBundle
   {
      @Source("ThemedPopupPanel.css")
      Styles styles();

      ImageResource popupTopLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource popupTopCenter();
      ImageResource popupTopRight();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource popupMiddleLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource popupMiddleRight();
      ImageResource popupBottomLeft();
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource popupBottomCenter();
      ImageResource popupBottomRight();
   }

   public interface Styles extends CssResource
   {
      String themedPopupPanel();
   }

   public ThemedPopupPanel()
   {
      super();
      commonInit(RES);
   }

   public ThemedPopupPanel(boolean autoHide)
   {
      super(autoHide);
      commonInit(RES);
   }

   public ThemedPopupPanel(boolean autoHide, boolean modal)
   {
      super(autoHide, modal);
      commonInit(RES);
   }

   public ThemedPopupPanel(boolean autoHide, boolean modal, Resources res)
   {
      super(autoHide, modal);
      commonInit(res);
   }

   private void commonInit(Resources res)
   {
      autoConstrain_ = true;
      addStyleName(res.styles().themedPopupPanel());

      if (RStudioThemes.usesScrollbars())
         addStyleName("rstudio-themes-scrollbars");
   }

   @Override
   public void setPopupPosition(int left, int top)
   {
      if (autoConstrain_ && left < 10)
         left = 10;

      super.setPopupPosition(left, top);

      if (autoConstrain_)
         sizeToWindow(top, Style.Overflow.AUTO);
   }

   public void setAutoConstrain(boolean autoConstrain)
   {
      autoConstrain_ = autoConstrain;
   }

   // Size the table to the window
   private void sizeToWindow(int top, Style.Overflow overflowY)
   {
      NodeList<Element> e = getElement().getElementsByTagName("table");
      if (e.getLength() < 1)
         return;

      Element table = e.getItem(0);
      int windowHeight = Window.getClientHeight();

      table.getStyle().setOverflowY(overflowY);
      table.getStyle().setDisplay(Style.Display.BLOCK);
      table.getStyle().setPropertyPx("maxHeight", windowHeight - top - 30);
   }

   private static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   // fit the popup and its overflow inside the app body as best as possible
   protected boolean autoConstrain_;
}
