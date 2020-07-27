/*
 * JobRunScriptEvent.java
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
package org.rstudio.studio.client.workbench.views.jobs.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class JobRunScriptEvent extends CrossWindowEvent<JobRunScriptEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobRunScript(JobRunScriptEvent event);
   }

   public JobRunScriptEvent()
   {
      path_ = "";
   }

   public JobRunScriptEvent(String path)
   {
      path_ = path;
   }

   public String path()
   {
      return path_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJobRunScript(this);
   }

   private final String path_;

   public static final Type<Handler> TYPE = new Type<>();
}
