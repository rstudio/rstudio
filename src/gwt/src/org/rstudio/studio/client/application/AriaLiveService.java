/*
 * AriaLiveService.java
 *
 * Copyright (C) 2020 by RStudio, Inc.
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
package org.rstudio.studio.client.application;

import com.google.inject.Singleton;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent;
import org.rstudio.studio.client.application.events.EventBus;

import javax.inject.Inject;

@Singleton
public class AriaLiveService
{
   @Inject
   public AriaLiveService(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }

   /**
    * Report a message to screen reader after a debounce delay
    * @param message string to announce
    */
   public void reportStatusDebounced(String message)
   {
      eventBus_.fireEvent(new AriaLiveStatusEvent(message, false));
   }

   /**
    * Report a message to screen reader.
    * @param message string to announce
    */
   public void reportStatus(String message)
   {
      eventBus_.fireEvent(new AriaLiveStatusEvent(message, true));
   }

   // injected
   private final EventBus eventBus_;
}
