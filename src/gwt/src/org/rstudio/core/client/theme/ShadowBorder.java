/*
 * ShadowBorder.java
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
package org.rstudio.core.client.theme;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

/**
 * Creates the visual effect of a "window" with shadowed border and
 * titlebar. This widget isn't designed to contain any content, but
 * rather be layered behind the content that should be shadowed (see
 * WindowFrame).
 */
class ShadowBorder extends Composite
{
   public ShadowBorder()
   {
      ThemeStyles styles = ThemeResources.INSTANCE.themeStyles();

      layout_ = new LayoutPanel();

      addPanel(styles.NW(), LEFT, TOP);
      addPanel(styles.N(), CENTER, TOP);
      addPanel(styles.NE(), RIGHT, TOP);
      addPanel(styles.W(), LEFT, MIDDLE);
      addPanel(styles.C(), CENTER, MIDDLE);
      addPanel(styles.E(), RIGHT, MIDDLE);
      addPanel(styles.SW(), LEFT, BOTTOM);
      addPanel(styles.S(), CENTER, BOTTOM);
      addPanel(styles.SE(), RIGHT, BOTTOM);

      initWidget(layout_);
   }

   private DivElement addPanel(String styleName, int halign, int valign)
   {
      DivElement div = Document.get().createDivElement();
      div.setClassName(styleName);

      Style style = div.getStyle();
      style.setPosition(Style.Position.ABSOLUTE);

      switch (halign)
      {
         case LEFT:
            style.setPropertyPx("left", 0);
            style.setPropertyPx("width", 10);
            break;
         case CENTER:
            style.setPropertyPx("left", 10);
            style.setPropertyPx("right", 10);
            break;
         case RIGHT:
            style.setPropertyPx("right", 0);
            style.setPropertyPx("width", 10);
            break;
      }
      switch (valign)
      {
         case TOP:
            style.setPropertyPx("top", 0);
            style.setPropertyPx("height", 26);
            break;
         case MIDDLE:
            style.setPropertyPx("top", 26);
            style.setPropertyPx("bottom", 26);
            break;
         case BOTTOM:
            style.setPropertyPx("bottom", 0);
            style.setPropertyPx("height", 26);
      }

      layout_.getElement().appendChild(div);
      return div;
   }

   private final LayoutPanel layout_;

   private static final int LEFT=0, CENTER=1, RIGHT=2;
   private static final int TOP=0, MIDDLE=1, BOTTOM=2;

   public static final int TOP_SHADOW_WIDTH = 3,
                           LEFT_SHADOW_WIDTH = 3,
                           RIGHT_SHADOW_WIDTH = 3,
                           BOTTOM_SHADOW_WIDTH = 5,
                           TITLEBAR_REGION_BOTTOM = 26,
                           CONTENT_REGION_TOP = 26;
}
