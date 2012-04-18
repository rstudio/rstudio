/*
 * ShowWarningBarEvent.java
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
package org.rstudio.studio.client.workbench.events;

import org.rstudio.studio.client.workbench.model.WarningBarMessage;

import com.google.gwt.event.shared.GwtEvent;


public class ShowWarningBarEvent extends GwtEvent<ShowWarningBarHandler>
{
   public static final GwtEvent.Type<ShowWarningBarHandler> TYPE =
      new GwtEvent.Type<ShowWarningBarHandler>();
   
   public ShowWarningBarEvent(WarningBarMessage message)
   {
      message_ = message;
   }
   
   public WarningBarMessage getMessage()
   {
      return message_;
   }
   
   @Override
   protected void dispatch(ShowWarningBarHandler handler)
   {
      handler.onShowWarningBar(this);
   }

   @Override
   public GwtEvent.Type<ShowWarningBarHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private WarningBarMessage message_;
}