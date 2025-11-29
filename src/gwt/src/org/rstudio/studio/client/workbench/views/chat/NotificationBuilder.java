/*
 * NotificationBuilder.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * Builder class for creating notification buttons with consistent styling and behavior.
 */
public class NotificationBuilder
{
   private final HorizontalPanel buttonPanel_;
   private final String buttonStyleName_;

   /**
    * Creates a new NotificationBuilder.
    *
    * @param buttonPanel The panel where buttons will be added
    * @param buttonStyleName The CSS style name to apply to buttons
    */
   public NotificationBuilder(HorizontalPanel buttonPanel, String buttonStyleName)
   {
      buttonPanel_ = buttonPanel;
      buttonStyleName_ = buttonStyleName;
      buttonPanel_.clear();
   }

   /**
    * Adds a button with the specified label and click handler.
    *
    * @param label The button label
    * @param handler The click handler to execute when button is clicked
    * @return This builder for method chaining
    */
   public NotificationBuilder addButton(String label, ClickHandler handler)
   {
      Button button = new Button(label);
      button.addStyleName(buttonStyleName_);
      button.addClickHandler(handler);
      buttonPanel_.add(button);
      return this;
   }

   /**
    * Adds a button with the specified label and runnable action.
    *
    * @param label The button label
    * @param action The action to execute when button is clicked
    * @return This builder for method chaining
    */
   public NotificationBuilder addButton(String label, Runnable action)
   {
      return addButton(label, new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            action.run();
         }
      });
   }

   /**
    * Clears all buttons from the panel.
    *
    * @return This builder for method chaining
    */
   public NotificationBuilder clear()
   {
      buttonPanel_.clear();
      return this;
   }
}
