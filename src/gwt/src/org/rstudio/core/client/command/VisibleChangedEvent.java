/*
 * VisibleChangedEvent.java
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

public class VisibleChangedEvent extends GwtEvent<VisibleChangedHandler>
{
   public static final Type<VisibleChangedHandler> TYPE = new Type<VisibleChangedHandler>();

   public VisibleChangedEvent(AppCommand command)
   {
      command_ = command;
   }

   public VisibleChangedEvent(AppCommand command,
                              String columnName)
   {
      command_ = command;
      columnName_ = columnName;
      buttonVisible_ = command_.isVisible();
   }

   public VisibleChangedEvent(AppCommand command,
                              String columnName,
                              boolean buttonVisible)
   {
      command_ = command;
      columnName_ = columnName;
      buttonVisible_ = buttonVisible;
   }

   public AppCommand getCommand()
   {
      return command_;
   }

   public String getColumnName()
   {
      return columnName_;
   }

   public boolean getButtonVisible()
   {
      return buttonVisible_;
   }

   @Override
   public Type<VisibleChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(VisibleChangedHandler handler)
   {
      handler.onVisibleChanged(this);
   }

   private final AppCommand command_;
   private String columnName_;
   private boolean buttonVisible_;
}
