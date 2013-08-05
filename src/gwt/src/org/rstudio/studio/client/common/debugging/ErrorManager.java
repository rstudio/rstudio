/*
 * ErrorManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


package org.rstudio.studio.client.common.debugging;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.events.UnhandledErrorEvent;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ErrorManager
             implements UnhandledErrorEvent.Handler
{
   @Inject
   public ErrorManager(EventBus events)
   {
      events_ = events;
      
      events_.addHandler(UnhandledErrorEvent.TYPE, this);
   }

   @Override
   public void onUnhandledError(UnhandledErrorEvent event)
   {
      lastError_ = event.getError();
   }
   
   public UnhandledError consumeLastError()
   {
      UnhandledError err = lastError_;
      lastError_ = null;
      return err;
   }
   
   private final EventBus events_;

   private UnhandledError lastError_;
}
