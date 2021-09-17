/*
 * VisualModeCollapseToggle.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.Value;

public class VisualModeCollapseToggle extends Composite
{
   public interface CollapseImages extends ClientBundle
   {
      @Source("expand_2x.png")
      ImageResource expand2x();

      @Source("CollapseToggle.css")
      Styles collapseStyles();
   }

   static interface Styles extends CssResource
   {
      String toggle();
   }

   public VisualModeCollapseToggle(boolean initial)
   {
      expanded = new Value<Boolean>(initial);

      CollapseImages images = GWT.create(CollapseImages.class);
      Image image = new Image(new ImageResource2x(images.expand2x()));
      images.collapseStyles().ensureInjected();
      image.setStyleName(images.collapseStyles().toggle(), true);
      Style style = image.getElement().getStyle();
      style.setProperty("transform", initial ? "rotate(0deg)" : "rotate(-90deg)");

      DOM.sinkEvents(image.getElement(), Event.ONCLICK);
      DOM.setEventListener(image.getElement(), evt ->
      {
         expanded.setValue(!expanded.getValue(), true);
      });

      expanded.addValueChangeHandler(evt ->
      {
         style.setProperty("transform", evt.getValue() ?
            "rotate(0deg)" : "rotate(-90deg)");
      });

      initWidget(image);
   }

   public Value<Boolean> expanded;
}
