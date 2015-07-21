/*
 * SourceWindow.java
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

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.DesktopHooks;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.SaveAction;
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.views.source.events.DocTabDragStartedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedHandler;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SourceWindow implements LastSourceDocClosedHandler,
                                     PopoutDocEvent.Handler,
                                     DocTabDragStartedEvent.Handler
{
   @Inject
   public SourceWindow(
         Provider<DesktopHooks> pDesktopHooks,
         SourceWindowManager windowManager,
         EventBus events,
         SourceShim shim)
   {
      sourceShim_ = shim;
      events_ = events;
      
      // this class is for satellite source windows only; if an instance gets
      // created in the main window, don't hook up any of its behaviors
      if (windowManager.isMainSourceWindow())
         return;
      
      // add event handlers
      events.addHandler(LastSourceDocClosedEvent.TYPE, this);
      events.addHandler(PopoutDocEvent.TYPE, this);
      events.addHandler(DocTabDragStartedEvent.TYPE, this);

      // set up desktop hooks (required to e.g. process commands issued by 
      // the desktop frame)
      pDesktopHooks.get();
      
      // export callbacks for main window
      exportFromSatellite();
   }
   
   // Event handlers ----------------------------------------------------------

   @Override
   public void onLastSourceDocClosed(LastSourceDocClosedEvent event)
   {
      // if this is a source document window and its last document closed,
      // close the window itself
      WindowEx.get().close();
   }
   

   @Override
   public void onPopoutDoc(PopoutDocEvent event)
   {
      // can't pop out directly from a satellite to another satellite; fire
      // this one on the main window
      events_.fireEventToMainWindow(event);
   }

   @Override
   public void onDocTabDragStarted(DocTabDragStartedEvent event)
   {
      if (!event.isFromMainWindow())
      {
         // if this is a satellite, broadcast the event to the main window
         events_.fireEventToMainWindow(event);
      }
   }

   // Private methods ---------------------------------------------------------

   private final native void exportFromSatellite() /*-{
      var satellite = this;
      $wnd.rstudioCloseAllDocs = $entry(function(caption, onCompleted) {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::closeAllDocs(Ljava/lang/String;Lcom/google/gwt/user/client/Command;)(caption, onCompleted);
      });
      
      $wnd.rstudioGetUnsavedChanges = $entry(function() {
         return satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::getUnsavedChanges()();
      });
      
      $wnd.rstudioHandleUnsavedChangesBeforeExit = $entry(function(targets, onCompleted) {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::handleUnsavedChangesBeforeExit(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/user/client/Command;)(targets, onCompleted);
      });
      
      $wnd.rstudioReadyToClose = false;
      $wnd.rstudioCloseSourceWindow = $entry(function() {
         satellite.@org.rstudio.studio.client.workbench.views.source.SourceWindow::closeSourceWindow()();
      });
   }-*/;
   
   private void handleUnsavedChangesBeforeExit(JavaScriptObject jsoItems, 
         Command onCompleted)
   {
      JsArray<UnsavedChangesItem> items = jsoItems.cast();
      ArrayList<UnsavedChangesTarget> targets = 
            new ArrayList<UnsavedChangesTarget>();
      for (int i = 0; i < items.length(); i++)
      {
         targets.add(items.get(i));
      }
      sourceShim_.handleUnsavedChangesBeforeExit(targets, onCompleted);
   }
   
   private void closeAllDocs(String caption, Command onCompleted)
   {
      sourceShim_.closeAllSourceDocs(caption, onCompleted);
   }
   
   private JsArray<UnsavedChangesItem> getUnsavedChanges()
   {
      JsArray<UnsavedChangesItem> items = JsArray.createArray().cast();
      ArrayList<UnsavedChangesTarget> targets = sourceShim_.getUnsavedChanges();
      for (UnsavedChangesTarget target: targets)
      {
         items.push(UnsavedChangesItem.create(target));
      }
      return items;
   }
   
   private void closeSourceWindow()
   {
      ApplicationQuit.handleUnsavedChanges(SaveAction.SAVEASK, 
            "Close Source Window", sourceShim_, null, null, 
            new ApplicationQuit.QuitContext()
            {
               @Override
               public void onReadyToQuit(boolean saveChanges)
               {
                  markReadyToClose();
                  WindowEx.get().close();
               }
            });
   }
   
   private final native void markReadyToClose() /*-{
      $wnd.rstudioReadyToClose = true;
   }-*/;
   
   private final EventBus events_;
   private final SourceShim sourceShim_;
}
