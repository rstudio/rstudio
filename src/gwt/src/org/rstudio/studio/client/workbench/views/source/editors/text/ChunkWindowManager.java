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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.core.client.*;
import org.rstudio.core.client.dom.WindowCloseMonitor;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.model.NavigationMethods;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.satellite.events.AllSatellitesClosingEvent;
import org.rstudio.studio.client.common.satellite.events.SatelliteClosedEvent;
import org.rstudio.studio.client.common.satellite.events.SatelliteFocusedEvent;
import org.rstudio.studio.client.common.satellite.model.SatelliteWindowGeometry;
import org.rstudio.studio.client.events.*;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.shiny.events.ShinyApplicationStatusEvent;
import org.rstudio.studio.client.workbench.MainWindowObject;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.UnsavedChangesItem;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceWindowParams;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ChunkWindowManager
{
   @Inject
   public ChunkWindowManager(
         Provider<SatelliteManager> pSatelliteManager, 
         Provider<Satellite> pSatellite)
   {
      pSatelliteManager_ = pSatelliteManager;
      pSatellite_ = pSatellite;
   }

   // Public methods ---------------------------------------------------------

   private void openChunkWindow(String docId, String chunkId)
   {
      Size size = new Size(800, 800);
      Point position = new Point(0, 0);
      
      pSatelliteManager_.get().openSatellite(
         NAME_PREFIX + docId + "_" + chunkId, 
         ChunkWindowParams.create(docId, chunkId), 
         size, false, position);
   }
   
   // Members -----------------------------------------------------------------
   
   private final Provider<SatelliteManager> pSatelliteManager_;
   private final Provider<Satellite> pSatellite_;
   
   public final static String NAME_PREFIX = "chunk_window_";
}
