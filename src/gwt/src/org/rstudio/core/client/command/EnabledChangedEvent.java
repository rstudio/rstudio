/*
 * EnabledChangedEvent.java
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
package org.rstudio.core.client.command;

import com.google.gwt.event.shared.GwtEvent;

public class EnabledChangedEvent extends GwtEvent<EnabledChangedHandler>
{
   public static final Type<EnabledChangedHandler> TYPE = new Type<EnabledChangedHandler>();

   public EnabledChangedEvent(AppCommand command)
   {
      command_ = command;
   }

   public EnabledChangedEvent(AppCommand command, String columnName)
   {
      command_ = command;
      columnName_ = columnName;
      buttonEnabled_ = command.isEnabled();
   }

   public EnabledChangedEvent(AppCommand command, String columnName, boolean buttonEnabled)
   {
      command_ = command;
      columnName_ = columnName;
      buttonEnabled_ = buttonEnabled;
   }

   public AppCommand getCommand()
   {
      return command_;
   }

   public String getColumnName()
   {
      return columnName_;
   }

   public boolean getButtonEnabled()
   {
      return buttonEnabled_;
   }

   @Override
   public Type<EnabledChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EnabledChangedHandler handler)
   {
      handler.onEnabledChanged(this);
   }

   private final AppCommand command_;
   private String columnName_;
   private boolean buttonEnabled_;
}
