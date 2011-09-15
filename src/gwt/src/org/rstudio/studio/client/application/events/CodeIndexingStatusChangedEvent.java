/*
 * CodeIndexingStatusChangedEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

public class CodeIndexingStatusChangedEvent extends GwtEvent<CodeIndexingStatusChangedHandler>
{   
   public static final GwtEvent.Type<CodeIndexingStatusChangedHandler> TYPE =
      new GwtEvent.Type<CodeIndexingStatusChangedHandler>();
   
   public CodeIndexingStatusChangedEvent(boolean enabled)
   {
      enabled_ = enabled;
   }
   
   public boolean getEnabled()
   {
      return enabled_;
   }
   
   @Override
   protected void dispatch(CodeIndexingStatusChangedHandler handler)
   {
      handler.onCodeIndexingStatusChanged(this);
   }

   @Override
   public GwtEvent.Type<CodeIndexingStatusChangedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   
   private final boolean enabled_;
}
