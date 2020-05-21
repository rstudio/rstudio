/*
 * CheckboxLabel.java
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
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class CheckboxLabel implements IsWidget
{
   public CheckboxLabel(final CheckBox checkbox, String label)
   {
      // CheckBox consists of a span with input (checkbox) element followed
      // by a label element, which is blank in this case so we associate our
      // label with it and disassociate the existing one to avoid confusing 
      // screen readers
      label_ = new FormLabel(label, checkbox.getElement().getFirstChildElement());
      Element blankLabel = DOM.getChild(checkbox.getElement(), 1);
      if (blankLabel != null)
      {
         blankLabel.removeAttribute("for");
      }
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

   private final FormLabel label_;
}
