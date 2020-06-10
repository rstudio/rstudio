/*
 * ValueChangeHandlerManager.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

public class ValueChangeHandlerManager<T> implements HasValueChangeHandlers<T>
{
   public ValueChangeHandlerManager(Object source)
   {
      manager_ = new HandlerManager(source);
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      manager_.fireEvent(event);
   }

   @Override
   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<T> handler)
   {
      return manager_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public final HandlerManager manager_;
}
