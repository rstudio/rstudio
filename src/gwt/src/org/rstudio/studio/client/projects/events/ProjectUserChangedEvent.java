/*
 * ProjectUserChangedEvent.java
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ProjectUserChangedEvent
   extends GwtEvent<ProjectUserChangedEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getChangeType() /*-{
         return this.change_type;
      }-*/;

      public final native JsArray<ProjectUser> getUsers() /*-{
         return this.users;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onProjectUserChanged(ProjectUserChangedEvent event);
   }

   public ProjectUserChangedEvent(Data data)
   {
      data_ = data;
   }

   public String getChangeType()
   {
      return data_.getChangeType();
   }

   public JsArray<ProjectUser> getUsers()
   {
      return data_.getUsers();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onProjectUserChanged(this);
   }

   private final Data data_;

   public static final String JOINED   = "joined";
   public static final String LEFT     = "left";
   public static final String REPLACED = "replaced";
   public static final String CHANGED  = "changed";

   public static final Type<Handler> TYPE = new Type<>();
}
