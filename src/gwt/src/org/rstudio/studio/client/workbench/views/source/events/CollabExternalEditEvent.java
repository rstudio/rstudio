/*
 * CollabExternalEditEvent.java
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class CollabExternalEditEvent 
             extends CrossWindowEvent<CollabExternalEditEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onCollabExternalEdit(CollabExternalEditEvent event);
   }

   public static final GwtEvent.Type<CollabExternalEditEvent.Handler> TYPE =
      new GwtEvent.Type<CollabExternalEditEvent.Handler>();

   public CollabExternalEditEvent()
   {
   }
   
   public CollabExternalEditEvent(String docId, String docPath, 
         double lastModified)
   {
      docId_ = docId;
      docPath_ = docPath;
      lastModified_ = lastModified;
   }
   
   public String getDocPath()
   {
      return docPath_;
   }
   
   public String getDocId()
   {
      return docId_;
   }
   
   public double getLastModified()
   {
      return lastModified_;
   }
   
   @Override
   public boolean forward()
   {
      return false;
   }
   
   @Override
   protected void dispatch(CollabExternalEditEvent.Handler handler)
   {
      handler.onCollabExternalEdit(this);
   }

   @Override
   public GwtEvent.Type<CollabExternalEditEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String docId_;
   private String docPath_;
   private double lastModified_;
}
