/*
 * OpenProjectNewWindowEvent.java
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

import org.rstudio.studio.client.application.model.RVersionSpec;

import com.google.gwt.event.shared.GwtEvent;

public class OpenProjectNewWindowEvent extends GwtEvent<OpenProjectNewWindowHandler>
{
   public static final GwtEvent.Type<OpenProjectNewWindowHandler> TYPE =
      new GwtEvent.Type<OpenProjectNewWindowHandler>();
   
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
   protected void dispatch(OpenProjectNewWindowHandler handler)
   {
      handler.onOpenProjectNewWindow(this);
   }

   @Override
   public GwtEvent.Type<OpenProjectNewWindowHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private final String project_;
   private final RVersionSpec rVersion_;
}
