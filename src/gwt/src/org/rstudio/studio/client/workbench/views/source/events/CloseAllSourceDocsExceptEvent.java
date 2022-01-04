/*
 * CloseAllSourceDocsExceptEvent.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

@JavaScriptSerializable
public class CloseAllSourceDocsExceptEvent extends CrossWindowEvent<CloseAllSourceDocsExceptEvent.Handler>
{
   public CloseAllSourceDocsExceptEvent()
   {
      this(null);
   }

   public CloseAllSourceDocsExceptEvent(String keepDocId)
   {
      keepDocId_ = keepDocId;
   }

   public String getKeepDocId()
   {
      return keepDocId_;
   }

   public interface Handler extends EventHandler
   {
      void onCloseAllSourceDocsExcept(CloseAllSourceDocsExceptEvent event);
   }

   protected void dispatch(Handler handler)
   {
      handler.onCloseAllSourceDocsExcept(this);
   }

   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   @Override
   public GwtEvent.Type<Handler> getAssociatedType() { return TYPE; }
   
   private String keepDocId_;
}
