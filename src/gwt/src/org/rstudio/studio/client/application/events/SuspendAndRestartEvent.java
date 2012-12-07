/*
 * SuspendAndRestartEvent.java
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

import org.rstudio.studio.client.application.model.SuspendOptions;

import com.google.gwt.event.shared.GwtEvent;

public class SuspendAndRestartEvent extends GwtEvent<SuspendAndRestartHandler>
{
   public static final GwtEvent.Type<SuspendAndRestartHandler> TYPE =
      new GwtEvent.Type<SuspendAndRestartHandler>();
   
   public SuspendAndRestartEvent(SuspendOptions suspendOptions,
                                 String afterRestartCommand)
   {
      suspendOptions_ = suspendOptions;
      afterRestartCommand_ = afterRestartCommand;
   }
   
   public SuspendAndRestartEvent(String afterRestartCommand)
   {
      this(SuspendOptions.createSaveAll(false), afterRestartCommand);
   }
 
   public SuspendOptions getSuspendOptions()
   {
      return suspendOptions_;
   }
   
   public String getAfterRestartCommand()
   {
      return afterRestartCommand_;
   }
   
   @Override
   protected void dispatch(SuspendAndRestartHandler handler)
   {
      handler.onSuspendAndRestart(this);
   }

   @Override
   public GwtEvent.Type<SuspendAndRestartHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final SuspendOptions suspendOptions_;
   private final String afterRestartCommand_;
}