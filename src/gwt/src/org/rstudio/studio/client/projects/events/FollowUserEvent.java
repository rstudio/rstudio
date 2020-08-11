/*
 * FollowUserEvent.java
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

package org.rstudio.studio.client.projects.events;

import org.rstudio.studio.client.projects.model.ProjectUser;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class FollowUserEvent
   extends GwtEvent<FollowUserEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onFollowUser(FollowUserEvent event);
   }

   public FollowUserEvent(ProjectUser user, boolean following)
   {
      user_ = user;
      following_ = following;
   }

   public ProjectUser getUser()
   {
      return user_;
   }

   public boolean following()
   {
      return following_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onFollowUser(this);
   }

   private final ProjectUser user_;
   private final boolean following_;

   public static final Type<Handler> TYPE = new Type<>();
}
