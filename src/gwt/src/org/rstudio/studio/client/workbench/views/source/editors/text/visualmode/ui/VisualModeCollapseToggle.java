/*
 * VisualModeCollapseToggle.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;

public class VisualModeCollapseToggle extends Composite
{
   public interface CollapseImages extends ClientBundle
   {
      @Source("expand_2x.png")
      DataResource expand2x();

      @Source("expandDark_2x.png")
      DataResource expandDark2x();

      @Source("collapsed_2x.png")
      DataResource collapsed2x();

      @Source("collapsedDark_2x.png")
      DataResource collapsedDark2x();

      @Source("CollapseToggle.css")
      Styles collapseStyles();
   }

   static interface Styles extends CssResource
   {
      String toggle();
      String collapsed();
      String expandIcon();
      String collapsedIcon();
   }

   public VisualModeCollapseToggle(boolean initial)
   {
      // Host element that contains all the collapse/toggle UI
      HTMLPanel host = new HTMLPanel("");

      // Initialize value that contains the expansion state.
      expanded = new Value<>(initial);

      // Initialize CSS and image resources.
      CollapseImages images = GWT.create(CollapseImages.class);
      images.collapseStyles().ensureInjected();

      // Create expander tool; this is a small triangle like ">" that toggles
      // the expansion state of the chunk.
      toggle_ = new HTMLPanel("");
      toggle_.setStyleName(images.collapseStyles().toggle(), true);
      HTMLPanel toggleIcon = new HTMLPanel("");
      toggleIcon.setStyleName(images.collapseStyles().expandIcon());
      toggle_.add(toggleIcon);
      host.add(toggle_);

      // Toggle expansion state on/off on click.
      DOM.sinkEvents(toggle_.getElement(), Event.ONCLICK);
      DOM.setEventListener(toggle_.getElement(), evt -> toggleExpansion());
      setShowToggle(false);

      // Create decorative image to show that the chunk is in a collapsed state.
      collapsed_ = new HTMLPanel("");
      collapsed_.setStyleName(images.collapseStyles().collapsed(), true);
      HTMLPanel collapsedIcon = new HTMLPanel("");
      collapsedIcon.setStyleName(images.collapseStyles().collapsedIcon());
      collapsed_.add(collapsedIcon);
      A11y.setDecorativeImage(collapsed_.getElement());
      host.add(collapsed_);

      DOM.sinkEvents(collapsed_.getElement(), Event.ONCLICK);
      DOM.setEventListener(collapsed_.getElement(), evt -> toggleExpansion());

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
      toggle_.setTitle(constants_.collapseOrExpandCodeChunk((expanded ? constants_.collapseCapitalized() : constants_.expandCapitalized())));
   }

   /**
    * Toggles the current expansion state
    */
   private void toggleExpansion()
   {
      expanded.setValue(!expanded.getValue(), true);
   }


   public Value<Boolean> expanded;

   private final HTMLPanel toggle_;
   private final HTMLPanel collapsed_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
