/*
 * ShowVcsDiffEvent.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.events;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class ShowVcsDiffEvent extends CrossWindowEvent<ShowVcsDiffEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onShowVcsDiff(ShowVcsDiffEvent event);
   }

   public ShowVcsDiffEvent()
   {
   }

   public ShowVcsDiffEvent(FileSystemItem file)
   {
      file_ = file;
   }

   public FileSystemItem getFile()
   {
      return file_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onShowVcsDiff(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private FileSystemItem file_;
}
