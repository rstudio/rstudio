/*
 * Session.java
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
package org.rstudio.studio.client.workbench.model;

import com.google.inject.Inject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.PushClientStateEvent;

public class Session
{
   @Inject
   public Session(EventBus events)
   {
      events_ = events;
   }

   public SessionInfo getSessionInfo()
   {
      return sessionInfo_;
   }

   public void setSessionInfo(SessionInfo sessionInfo)
   {
      sessionInfo_ = sessionInfo;
   }

   public void persistClientState()
   {
      events_.fireEvent(new PushClientStateEvent());
   }

   private SessionInfo sessionInfo_;
   private final EventBus events_;
}
