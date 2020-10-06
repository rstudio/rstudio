/*
 * RenameFileEvent.java
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
package org.rstudio.studio.client.common.filetypes.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class RenameSourceFileEvent extends CrossWindowEvent<RenameSourceFileEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onRenameSourceFile(RenameSourceFileEvent event);
   }

   public RenameSourceFileEvent(String path)
   {
      path_ = path;
   }

   public RenameSourceFileEvent()
   {
   }

   public String getPath()
   {
      return path_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRenameSourceFile(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public static final Type<Handler> TYPE = new Type<>();

   private String path_;
}
