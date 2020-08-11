/*
 * ObjectExplorerEditingTargetStatusBar.java
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class ObjectExplorerEditingTargetStatusBar extends Composite
{
   public ObjectExplorerEditingTargetStatusBar(ObjectExplorerEditingTargetWidget widget,
                                               ObjectExplorerDataGrid grid)
   {
      widget_ = widget;
      grid_ = grid;

      panel_ = new FlowPanel();
      label_ = new Label(NO_SELECTION);

      panel_.setSize("100%", "100%");
      panel_.addStyleName("rstudio-themes-background");
      panel_.addStyleName(RES.styles().panel());
      panel_.add(label_);

      label_.addStyleName(RES.styles().label());

      initWidget(panel_);

      initializeHandlers();
   }

   public void setText(String text)
   {
      label_.setText(text);
   }

   private void initializeHandlers()
   {
      grid_.addSelectionChangedHandler(selectionChangedEvent ->
      {
         ObjectExplorerDataGrid.Data data = grid_.getCurrentSelection();
         if (data == null)
         {
            label_.setText(NO_SELECTION);
            return;
         }

         String accessor = ObjectExplorerDataGrid.generateExtractingCode(
               data,
               widget_.getHandle().getTitle());

         label_.setText(accessor);
      });
   }

   private final ObjectExplorerEditingTargetWidget widget_;
   private final ObjectExplorerDataGrid grid_;
   private final FlowPanel panel_;
   private final Label label_;

   private static final String NO_SELECTION = "(No selection)";

   // Boilerplate ----

   public interface Styles extends CssResource
   {
      String panel();
      String label();
   }

   public interface Resources extends ClientBundle
   {
      @Source("ObjectExplorerEditingTargetStatusBar.css")
      Styles styles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

}
