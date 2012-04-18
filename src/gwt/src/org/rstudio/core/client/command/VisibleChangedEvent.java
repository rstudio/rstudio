/*
 * VisibleChangedEvent.java
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
package org.rstudio.core.client.command;

import com.google.gwt.event.shared.GwtEvent;

public class VisibleChangedEvent extends GwtEvent<VisibleChangedHandler>
{
   public static final Type<VisibleChangedHandler> TYPE = new Type<VisibleChangedHandler>();

   public VisibleChangedEvent(AppCommand command)
   {
      command_ = command;
   }

   public AppCommand getCommand()
   {
      return command_;
   }

   @Override
   public Type<VisibleChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(VisibleChangedHandler handler)
   {
      handler.onVisibleChanged(command_);
   }

   private final AppCommand command_;
}
