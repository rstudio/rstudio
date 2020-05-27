/*
 * DocWindowChangedEvent.java
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
import org.rstudio.studio.client.workbench.views.source.model.DocTabDragParams;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class DocWindowChangedEvent 
             extends CrossWindowEvent<DocWindowChangedEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onDocWindowChanged(DocWindowChangedEvent event);
   }

   public static final GwtEvent.Type<DocWindowChangedEvent.Handler> TYPE =
      new GwtEvent.Type<DocWindowChangedEvent.Handler>();
   
   public DocWindowChangedEvent()
   {
   }

   public DocWindowChangedEvent(String docId, String oldWindowId, 
                                DocTabDragParams params, 
                                CollabEditStartParams collabParams,
                                int pos, int xpos)
   {
      this(docId, oldWindowId, SourceWindowManager.getSourceWindowId(), params, 
           collabParams, pos, xpos);
   }
   
   // this event is fired by the window on which the document was dropped; it
   // is rebroadcast to the the window from which the drop originated to 
   // negotiate its end of the transfer
   public DocWindowChangedEvent(String docId, String oldWindowId, 
         String newWindowId, DocTabDragParams params, 
         CollabEditStartParams collabParams, int pos, int xpos)
   {
      docId_ = docId;
      oldWindowId_ = oldWindowId;
      newWindowId_ = newWindowId;
      pos_ = pos;
      xpos_ = xpos;
      params_ = params;
      collabParams_ = collabParams;
   }
   
   public String getDocId()
   {
      return docId_;
   }
   
   public String getOldWindowId()
   {
      return oldWindowId_;
   }
   
   public String getNewWindowId()
   {
      return newWindowId_;
   }

   public int getPos()
   {
      return pos_;
   }

   public int getXPos()
   {
      return xpos_;
   }
   
   public DocTabDragParams getParams()
   {
      return params_;
   }
   
   public CollabEditStartParams getCollabParams()
   {
      return collabParams_;
   }

   @Override
   public boolean forward() 
   {
      return false;
   }
   
   @Override
   protected void dispatch(DocWindowChangedEvent.Handler handler)
   {
      handler.onDocWindowChanged(this);
   }

   @Override
   public GwtEvent.Type<DocWindowChangedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String oldWindowId_;
   private String newWindowId_;
   private String docId_;
   private DocTabDragParams params_;
   private CollabEditStartParams collabParams_;
   private int pos_;
   private int xpos_;
}
