/*
 * PresentationZoomPopup
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.presentation.zoom;

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
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.ModalPopupPanel;
import org.rstudio.core.client.widget.NineUpBorder;


public class PresentationZoomPopup
{
   interface Resources extends NineUpBorder.Resources, ClientBundle
   {
      @Override
      @Source("../../../../../../core/client/widget/NineUpBorder.css")
      Styles styles();

      @Override
      @Source("TopLeft.png")
      ImageResource topLeft();

      @Override
      @Source("Top.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource top();

      @Override
      @Source("TopRight.png")
      ImageResource topRight();

      @Override
      @Source("Left.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource left();

      @Override
      @Source("Right.png")
      @ImageOptions(repeatStyle = RepeatStyle.Vertical)
      ImageResource right();

      @Override
      @Source("BottomLeft.png")
      ImageResource bottomLeft();

      @Override
      @Source("Bottom.png")
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
      ImageResource bottom();

      @Override
      @Source("BottomRight.png")
      ImageResource bottomRight();

      @Source("Close.png")
      ImageResource close();
   }

   public interface Styles extends NineUpBorder.Styles
   {
   }

   public static void show()
   {
      Label mainWidget = new Label("mainWidget");
      mainWidget.setSize("100%", "100%");

      LayoutPanel layoutPanel = new LayoutPanel();
      layoutPanel.setSize("100%", "100%");
      layoutPanel.add(mainWidget);
      layoutPanel.setWidgetLeftRight(mainWidget, 0, Unit.PX, 0, Unit.PX);
      layoutPanel.setWidgetTopBottom(mainWidget, 0, Unit.PX, 0, Unit.PX);
     
      ModalPopupPanel popup = new ModalPopupPanel(false, false, true);
      Resources res = GWT.<Resources>create(Resources.class);
      NineUpBorder border = new NineUpBorder(
            res,
            32, 20, 17, 20);
      addCloseButton(popup, border);
      border.setSize("100%", "100%");
      border.setFillColor("white");
      border.setWidget(layoutPanel);
      popup.setWidget(border);
      popup.setGlassEnabled(true);

      Style popupStyle = popup.getElement().getStyle();
      popupStyle.setZIndex(1001);
      popupStyle.setPosition(Style.Position.ABSOLUTE);
      popupStyle.setTop(0, Unit.PX);
      popupStyle.setBottom(0, Unit.PX);
      popupStyle.setLeft(0, Unit.PX);
      popupStyle.setRight(0, Unit.PX);

      Style contentStyle =
            ((Element) popup.getElement().getFirstChild()).getStyle();
      contentStyle.setWidth(100, Unit.PCT);
      contentStyle.setHeight(100, Unit.PCT);

      popup.center();
   }

   private static void addCloseButton(final ModalPopupPanel popupPanel,
                                      NineUpBorder border)
   {
      Resources res = GWT.create(Resources.class);
      Image closeIcon = new Image(res.close());
      closeIcon.getElement().getStyle().setCursor(Style.Cursor.POINTER);
      closeIcon.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            popupPanel.close();
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
}
