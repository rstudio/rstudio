/*
 * CheckboxLabel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class CheckboxLabel implements IsWidget
{
   public CheckboxLabel(final CheckBox checkbox, String label)
   {
      label_ = new Label(label);

      label_.getElement().getStyle().setCursor(Cursor.DEFAULT);
      label_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            event.preventDefault();

            checkbox.setValue(!checkbox.getValue(), true);
         }
      });
   }

   @Override
   public Widget asWidget()
   {
      return label_;
   }

   public Label getLabel()
   {
      return label_;
   }

   private final Label label_;
}
