/*
 * FullscreenPopupPanel.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class FullscreenPopupPanel extends ModalPopupPanel
{
   public FullscreenPopupPanel(Widget titleWidget, 
                               Widget mainWidget,
                               boolean closeOnEscape)
   {
      this(titleWidget, mainWidget, 0, closeOnEscape);
   }
   
   public FullscreenPopupPanel(Widget titleWidget, 
                               Widget mainWidget,
                               int margin,
                               boolean closeOnEscape)
   {
      super(false, false, closeOnEscape);
      
      NineUpBorder border = new NineUpBorder(RES, 32, 20, 17, 20);
      if (titleWidget != null)
         addTitleWidget(border, titleWidget);
      addCloseButton(border);
      border.setSize("100%", "100%");
      border.setFillColor("white");
      border.setWidget(mainWidget);
      setWidget(border);
      setGlassEnabled(true);

      Style popupStyle = getElement().getStyle();
      popupStyle.setZIndex(1001);
      popupStyle.setPosition(Style.Position.ABSOLUTE);
      popupStyle.setTop(margin, Unit.PX);
      popupStyle.setBottom(margin, Unit.PX);
      popupStyle.setLeft(margin, Unit.PX);
      popupStyle.setRight(margin, Unit.PX);

      Style contentStyle =
            ((Element) getElement().getFirstChild()).getStyle();
      contentStyle.setWidth(100, Unit.PCT);
      contentStyle.setHeight(100, Unit.PCT);
   }
   
   private void addTitleWidget(NineUpBorder border, Widget titleWidget)
   {
      LayoutPanel layoutPanel = border.getLayoutPanel();
      layoutPanel.add(titleWidget);
      layoutPanel.setWidgetTopHeight(titleWidget,
                                     13, Unit.PX,
                                     21, Unit.PX);
      layoutPanel.setWidgetLeftRight(titleWidget,
                                     27, Unit.PX,
                                     27+RES.close2x().getWidth() + 15, Unit.PX);
   }
   
   private void addCloseButton(NineUpBorder border)
   {
      Image closeIcon = new Image(new ImageResource2x(RES.close2x()));
      closeIcon.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      closeIcon.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            close();
         }
      });

      LayoutPanel layoutPanel = border.getLayoutPanel();
      layoutPanel.add(closeIcon);
      layoutPanel.setWidgetTopHeight(closeIcon,
            15, Unit.PX,
            closeIcon.getHeight(), Unit.PX);
      layoutPanel.setWidgetRightWidth(closeIcon,
            27, Unit.PX,
            closeIcon.getWidth(), Unit.PX);
   }

   interface Resources extends NineUpBorder.Resources, ClientBundle
   {
      @Override
      @Source("NineUpBorder.css")
      Styles styles();

      @Override
      @Source("fullscreenPopupTopLeft.png")
      ImageResource topLeft();

      @Override
      @Source("fullscreenPopupTop.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();

      @Override
      @Source("fullscreenPopupTopRight.png")
      ImageResource topRight();

      @Override
      @Source("fullscreenPopupLeft.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();

      @Override
      @Source("fullscreenPopupRight.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();

      @Override
      @Source("fullscreenPopupBottomLeft.png")
      ImageResource bottomLeft();

      @Override
      @Source("fullscreenPopupBottom.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();

      @Override
      @Source("fullscreenPopupBottomRight.png")
      ImageResource bottomRight();

      @Source("fullscreenPopupClose_2x.png")
      ImageResource close2x();
   }

   public interface Styles extends NineUpBorder.Styles
   {
   }
   
   private static final Resources RES = GWT.<Resources>create(Resources.class); 
}
