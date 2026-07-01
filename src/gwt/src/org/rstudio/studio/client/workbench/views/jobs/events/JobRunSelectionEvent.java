/*
 * JobRunSelectionEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
public class JobRunSelectionEvent extends CrossWindowEvent<JobRunSelectionEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onJobRunSelection(JobRunSelectionEvent event);
   }

   public JobRunSelectionEvent()
   {
      code_ = "";
      path_ = "";
   }

   public JobRunSelectionEvent(String path, String code)
   {
      path_ = path;
      code_ = code;
   }

   public String code()
   {
      return code_;
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

   // when fired from a detached source window, this event is forwarded to the
   // main window, which shows the job launcher dialog. bring the main window
   // forward so that dialog is visible (otherwise it can appear behind the
   // source window and the command appears to do nothing; see rstudio#18101)
   @Override
   public int focusMode()
   {
      return CrossWindowEvent.MODE_FOCUS;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onJobRunSelection(this);
   }

   private final String code_;
   private final String path_;

   public static final Type<Handler> TYPE = new Type<>();
}
