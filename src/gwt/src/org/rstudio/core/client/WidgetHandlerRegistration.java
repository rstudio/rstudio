/*
 * WidgetHandlerRegistration.java
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
package org.rstudio.core.client;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

/**
 * Automatically registers and unregisters a handler as a widget
 * becomes attached and detached from the DOM.
 *
 * To use, create a concrete subclass and override the doRegister
 * method to do the actual addHandler call.
 */
public abstract class WidgetHandlerRegistration
{
   public WidgetHandlerRegistration(Widget widget)
   {
      widget.addAttachHandler(attachEvent ->
      {
         unregister();
         if (attachEvent.isAttached())
            register();
      });

      if (widget.isAttached())
         register();
   }

   public void register()
   {
      registration_ = doRegister();
   }

   protected abstract HandlerRegistration doRegister();

   private void unregister()
   {
      if (registration_ != null)
      {
         registration_.removeHandler();
         registration_ = null;
      }
   }

   private HandlerRegistration registration_;
}
