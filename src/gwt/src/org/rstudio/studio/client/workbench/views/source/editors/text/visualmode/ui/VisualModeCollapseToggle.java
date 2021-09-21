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
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.common.Value;

public class VisualModeCollapseToggle extends Composite
{
   public interface CollapseImages extends ClientBundle
   {
      @Source("expand_2x.png")
      ImageResource expand2x();

      @Source("collapsed_2x.png")
      ImageResource collapsed2x();

      @Source("CollapseToggle.css")
      Styles collapseStyles();
   }

   static interface Styles extends CssResource
   {
      String toggle();
      String collapsed();
   }

   public VisualModeCollapseToggle(boolean initial)
   {
      // Host element that contains all the collapse/toggle UI
      HTMLPanel host = new HTMLPanel("");

      // Initialize value that contains the expansion state.
      expanded = new Value<Boolean>(initial);

      // Initialize CSS and image resources.
      CollapseImages images = GWT.create(CollapseImages.class);
      images.collapseStyles().ensureInjected();

      // Create expander tool; this is a small triangle like ">" that toggles
      // the expansion state of the chunk.
      toggle_ = new Image(new ImageResource2x(images.expand2x()));
      toggle_.setStyleName(images.collapseStyles().toggle(), true);
      @SuppressWarnings("unused")
      Style style = toggle_.getElement().getStyle();
      host.add(toggle_);

      // Toggle expansion state on/off on click.
      DOM.sinkEvents(toggle_.getElement(), Event.ONCLICK);
      DOM.setEventListener(toggle_.getElement(), evt ->
      {
         expanded.setValue(!expanded.getValue(), true);
      });
      setShowToggle(false);

      // Create decorative image to show that the chunk is in a collapsed state.
      collapsed_ = new Image(new ImageResource2x(images.collapsed2x()));
      collapsed_.setStyleName(images.collapseStyles().collapsed(), true);
      A11y.setDecorativeImage(collapsed_.getElement());
      host.add(collapsed_);

      // Set initial expansion state and listen for changes
      setExpanded(initial);
      expanded.addValueChangeHandler(evt ->
      {
         setExpanded(evt.getValue());
      });

      initWidget(host);
   }

   /**
    * Sets the visibility of the collapse toggle.
    *
    * @param show Whether to show the collapse toggle
    */
   public void setShowToggle(boolean show)
   {
      toggle_.getElement().getStyle().setOpacity(show ? 1 : 0);
   }

   /**
    * Sets the expansion state.
    *
    * @param expanded Whether to show as expanded
    */
   private void setExpanded(boolean expanded)
   {
      // Rotate arrow to point right when collapsed
      toggle_.getElement().getStyle().setProperty("transform", expanded ?
         "rotate(0deg)" : "rotate(-90deg)");

      // Show collapse hint when collapsed
      collapsed_.setVisible(!expanded);

      // Change hint text.
      toggle_.setAltText((expanded ? "Collapse" : "Expand") + " code chunk");
   }

   public Value<Boolean> expanded;

   private Image toggle_;
   private Image collapsed_;
}
