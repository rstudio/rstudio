/*
 * SourceDocAddedEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class SourceDocAddedEvent
             extends CrossWindowEvent<SourceDocAddedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSourceDocAdded(SourceDocAddedEvent e);
   }

   public SourceDocAddedEvent()
   {
   }

   public SourceDocAddedEvent(SourceDocument doc, int mode, String displayName)
   {
      doc_ = doc;
      mode_ = mode;
      displayName_ = displayName;
      windowId_ = SourceWindowManager.getSourceWindowId();
   }

   public SourceDocument getDoc()
   {
      return doc_;
   }

   public String getDisplayName()
   {
      return displayName_;
   }

   public String getWindowId()
   {
      return windowId_;
   }

   public int getMode()
   {
      return mode_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSourceDocAdded(this);
   }

   private SourceDocument doc_;
   private String displayName_;
   private String windowId_;
   private int mode_;

   public static final Type<Handler> TYPE = new Type<>();
}
