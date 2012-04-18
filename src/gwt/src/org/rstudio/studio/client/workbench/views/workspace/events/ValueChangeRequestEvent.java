/*
 * ValueChangeRequestEvent.java
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
package org.rstudio.studio.client.workbench.views.workspace.events;

import com.google.gwt.event.shared.GwtEvent;

public class ValueChangeRequestEvent<T> extends GwtEvent<ValueChangeRequestHandler<T>>
{
   public static final GwtEvent.Type<ValueChangeRequestHandler<?>> TYPE
      = new GwtEvent.Type<ValueChangeRequestHandler<?>>() ;

   @Override
   protected void dispatch(ValueChangeRequestHandler<T> handler)
   {
      handler.onValueChangeRequest(this) ;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   @Override
   public GwtEvent.Type<ValueChangeRequestHandler<T>> getAssociatedType()
   {
      return (Type) TYPE ;
   }

   
   
   public ValueChangeRequestEvent(T value)
   {
      super() ;
      value_ = value ;
   }

   public T getValue()
   {
      return value_ ;
   }

   private final T value_ ;
}
