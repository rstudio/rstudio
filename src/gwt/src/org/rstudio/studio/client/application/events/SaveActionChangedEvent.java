/*
 * SaveActionChangedEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.studio.client.application.model.SaveAction;

public class SaveActionChangedEvent extends GwtEvent<SaveActionChangedHandler>
{   
   public static final GwtEvent.Type<SaveActionChangedHandler> TYPE =
      new GwtEvent.Type<SaveActionChangedHandler>();
   
   public SaveActionChangedEvent(SaveAction action)
   {
      action_ = action;
   }
   
   public SaveAction getAction()
   {
      return action_;
   }
   
   @Override
   protected void dispatch(SaveActionChangedHandler handler)
   {
      handler.onSaveActionChanged(this);
   }

   @Override
   public GwtEvent.Type<SaveActionChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   
   private SaveAction action_;
}
