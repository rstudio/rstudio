/*
 * OpenProjectNewWindowEvent.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.application.model.RVersionSpec;

import com.google.gwt.event.shared.GwtEvent;

public class OpenProjectNewWindowEvent extends GwtEvent<OpenProjectNewWindowEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onOpenProjectNewWindow(OpenProjectNewWindowEvent event);
   }

   public OpenProjectNewWindowEvent(String project, RVersionSpec rVersion)
   {
      project_ = project;
      rVersion_ = rVersion;
   }

   public String getProject()
   {
      return project_;
   }

   public RVersionSpec getRVersion()
   {
      return rVersion_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onOpenProjectNewWindow(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final String project_;
   private final RVersionSpec rVersion_;
}
