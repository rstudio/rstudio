/*
 * SourceWindowManager.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.events.SourceDocAddedEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceWindowParams;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SourceWindowManager implements PopoutDocEvent.Handler,
                                            SourceDocAddedEvent.Handler
{
   @Inject
   public SourceWindowManager(
         Provider<SatelliteManager> pSatelliteManager, 
         SourceServerOperations server,
         EventBus events)
   {
      events_ = events;
      server_ = server;
      pSatelliteManager_ = pSatelliteManager;
      events_.addHandler(PopoutDocEvent.TYPE, this);
      events_.addHandler(SourceDocAddedEvent.TYPE, this);
   }

   @Override
   public void onPopoutDoc(final PopoutDocEvent evt)
   {
      // assign a new window ID to the source document
      final String windowId = createSourceWindowId();
      HashMap<String,String> props = new HashMap<String,String>();
      props.put(SOURCE_WINDOW_ID, windowId);
      evt.getDoc().getProperties().setString(
            SOURCE_WINDOW_ID, windowId);
      
      // update the document's properties to assign this ID
      server_.modifyDocumentProperties(
            evt.getDoc().getId(), props, new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  SourceWindowParams params = SourceWindowParams.create(
                        windowId, evt.getDoc());
                  pSatelliteManager_.get().openSatellite(
                        SourceSatellite.NAME_PREFIX + windowId, params, 
                        new Size(500, 800));
                  sourceWindows_.put(windowId, 
                        pSatelliteManager_.get().getSatelliteWindowObject(
                              SourceSatellite.NAME_PREFIX + windowId));
               }

               @Override
               public void onError(ServerError error)
               {
                  // do nothing here (we just won't pop out the doc)
               }
            });
   }
   
   @Override
   public void onSourceDocAdded(SourceDocAddedEvent e)
   {
      // ensure the doc isn't already in our index
      for (SourceDocument doc: sourceDocs_)
      {
         if (doc.getId() == e.getDoc().getId())
            return;
      }
      
      sourceDocs_.add(e.getDoc());
   }

   public boolean isSourceWindowOpen(String windowId)
   {
      return sourceWindows_.containsKey(windowId);
   }
   
   public String getSourceWindowId()
   {
      String view = Window.Location.getParameter("view");
      if (view != null && view.startsWith(SourceSatellite.NAME_PREFIX))
      {
         return view.substring(SourceSatellite.NAME_PREFIX.length());
      }
      return "";
   }
   
   public void setSourceDocs(JsArray<SourceDocument> sourceDocs)
   {
      JsArrayUtil.fillList(sourceDocs, sourceDocs_);
   }
   
   public JsArray<SourceDocument> getSourceDocs()
   {
      return JsArrayUtil.toJsArray(sourceDocs_);
   }
   
   // Private methods ---------------------------------------------------------
   
   private String createSourceWindowId()
   {
      String alphanum = "0123456789abcdefghijklmnopqrstuvwxyz";
      String id = "";
      for (int i = 0; i < 12; i++)
      {
         id += alphanum.charAt((int)(Math.random() * alphanum.length()));
      }
      return id;
   }
   
   private EventBus events_;
   private Provider<SatelliteManager> pSatelliteManager_;
   private SourceServerOperations server_;
   private HashMap<String, WindowEx> sourceWindows_ = 
         new HashMap<String,WindowEx>();
   private ArrayList<SourceDocument> sourceDocs_ = 
         new ArrayList<SourceDocument>();
   
   public final static String SOURCE_WINDOW_ID = "source_window_id";
}
