/*
 * Value.java
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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasValue;

public class Value<T> implements HasValue<T>, ReadOnlyValue<T>
{
   public Value(T initialValue)
   {
      handlers_ = new HandlerManager(this);
      value_ = initialValue;
   }

   public T getValue()
   {
      return value_;
   }

   public void setValue(T value)
   {
      setValue(value, false);
   }

   public void setValue(T value, boolean fireEvents)
   {
      if (!areEqual(value_, value))
      {
         value_ = value;
         if (fireEvents)
            ValueChangeEvent.fire(this, value);
      }
   }

   public void fireChangeEvent()
   {
      ValueChangeEvent.fire(this, value_);
   }

   private boolean areEqual(T a, T b)
   {
      if (a == null ^ b == null)
         return false;
      if (a == null)
         return true;
      return a.equals(b);
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<T> handler)
   {
      return handlers_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   private final HandlerManager handlers_;
   private T value_;
}
