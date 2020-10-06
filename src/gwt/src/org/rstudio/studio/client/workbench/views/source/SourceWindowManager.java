/*
 * SourceWindowManager.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.JavaScriptObject;
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
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;
import org.rstudio.studio.client.workbench.views.source.model.SourceWindowParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

@Singleton
public class SourceWindowManager implements PopoutDocEvent.Handler,
                                            SourceDocAddedEvent.Handler,
                                            SourceFileSavedHandler,
                                            CodeBrowserCreatedEvent.Handler,
                                            SatelliteFocusedEvent.Handler,
                                            SatelliteClosedEvent.Handler,
                                            DocTabDragStartedEvent.Handler,
                                            DocWindowChangedEvent.Handler,
                                            DocTabClosedEvent.Handler,
                                            AllSatellitesClosingEvent.Handler,
                                            ShinyApplicationStatusEvent.Handler,
                                            CollabEditStartedEvent.Handler,
                                            CollabEditEndedEvent.Handler,
                                            DocFocusedEvent.Handler,
                                            EditorCommandDispatchEvent.Handler,
                                            RestartStatusEvent.Handler,
                                            ScrollToPositionEvent.Handler
{
   @Inject
   public SourceWindowManager(
         Provider<SatelliteManager> pSatelliteManager,
         Provider<Satellite> pSatellite,
         Provider<WorkbenchContext> pWorkbenchContext,
         SourceServerOperations server,
         EventBus events,
         FileTypeRegistry registry,
         GlobalDisplay display,
         Provider<Source> source,
         Session session,
         UserPrefs uiPrefs)
   {
      events_ = events;
      server_ = server;
      pSatelliteManager_ = pSatelliteManager;
      pSatellite_ = pSatellite;
      pWorkbenchContext_ = pWorkbenchContext;
      display_ = display;
      pSource_ = source;
      userPrefs_ = uiPrefs;

      events_.addHandler(DocWindowChangedEvent.TYPE, this);
      events_.addHandler(ScrollToPositionEvent.TYPE, this);

      if (isMainSourceWindow())
      {
         // most event handlers only make sense on the main window
         events_.addHandler(EditorCommandDispatchEvent.TYPE, this);
         events_.addHandler(PopoutDocEvent.TYPE, this);
         events_.addHandler(DocTabDragStartedEvent.TYPE, this);
         events_.addHandler(ShinyApplicationStatusEvent.TYPE, this);
         events_.addHandler(AllSatellitesClosingEvent.TYPE, this);
         events_.addHandler(SourceDocAddedEvent.TYPE, this);
         events_.addHandler(SourceFileSavedEvent.TYPE, this);
         events_.addHandler(CodeBrowserCreatedEvent.TYPE, this);
         events_.addHandler(SatelliteClosedEvent.TYPE, this);
         events_.addHandler(SatelliteFocusedEvent.TYPE, this);
         events_.addHandler(DocTabClosedEvent.TYPE, this);
         events_.addHandler(CollabEditStartedEvent.TYPE, this);
         events_.addHandler(CollabEditEndedEvent.TYPE, this);
         events_.addHandler(DocFocusedEvent.TYPE, this);
         events_.addHandler(RestartStatusEvent.TYPE, this);

         JsArray<SourceDocument> docs =
               session.getSessionInfo().getSourceDocuments();
         sourceDocs_ = docs;

         exportFromMain();

         new JSObjectStateValue(
                 "source-window",
                 "sourceWindowGeometry",
                 ClientState.PROJECT_PERSISTENT,
                 session.getSessionInfo().getClientState(),
                 false)
         {
            @Override
            protected void onInit(JsObject value)
            {
               // save the window geometries
               if (value != null)
                  windowGeometry_ = value;

               // compute the max ordinal value in the geometry set
               JsArrayString windowIds = windowGeometry_.keys();
               for (int i = 0; i < windowIds.length(); i++)
               {
                  SatelliteWindowGeometry geometry =
                        windowGeometry_.getObject(windowIds.get(i)).cast();
                  maxOrdinal_ = Math.max(geometry.getOrdinal(), maxOrdinal_);
               }
            }

            @Override
            protected JsObject getValue()
            {
               return windowGeometry_;
            }

            @Override
            protected boolean hasChanged()
            {
               return updateWindowGeometry();
            }
         };

         // keep track of whether the main window has focus (helps us infer the
         // user's perception of the 'active' doc)
         WindowEx.addFocusHandler(new FocusHandler()
         {
            @Override
            public void onFocus(FocusEvent arg0)
            {
               mainWindowFocused_ = true;
            }
         });

         WindowEx.addBlurHandler(new BlurHandler()
         {

            @Override
            public void onBlur(BlurEvent arg0)
            {
               mainWindowFocused_ = false;
            }
         });

         // open this session's source windows
         for (int i = 0; i < docs.length(); i++)
         {
            String windowId = docs.get(i).getSourceWindowId();
            if (!StringUtil.isNullOrEmpty(windowId) &&
                !isSourceWindowOpen(windowId))
            {
               openSourceWindow(windowId, null, null, null);
            }
         }
      }

      // signal that this window has focus
      if (WindowEx.get().getDocument().hasFocus())
      {
         MainWindowObject.lastFocusedWindow().set(WindowEx.getNative());
         MainWindowObject.lastFocusedWindowId().set(getSourceWindowId());
      }

      WindowEx.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            MainWindowObject.lastFocusedWindow().set(WindowEx.getNative());
            MainWindowObject.lastFocusedWindowId().set(getSourceWindowId());
         }
      });
   }

   // Public types ------------------------------------------------------------

   public class NavigationResult
   {
      public NavigationResult(int type, String docId)
      {
         type_ = type;
         docId_ = docId;
      }

      public NavigationResult(int type)
      {
         this(type, null);
      }


      public int getType()
      {
         return type_;
      }

      public String getDocId()
      {
         return docId_;
      }

      private final int type_;
      private final String docId_;

      // no navigation was performed
      public final static int RESULT_NONE = 0;

      // the document was found and should be moved to the requesting window
      public final static int RESULT_RELOCATE = 1;

      // the document was found, and the navigation was completed
      public final static int RESULT_NAVIGATED = 2;
   }

   // Public methods ----------------------------------------------------------

   public static String getSourceWindowId()
   {
      return sourceWindowId(Window.Location.getParameter("view"));
   }

   public String getLastFocusedSourceWindowId()
   {
      return MainWindowObject.lastFocusedSourceWindowId().get();
   }

   public String getLastFocusedWindowId()
   {
      return MainWindowObject.lastFocusedWindowId().get();
   }

   public int getSourceWindowOrdinal()
   {
      return thisWindowOrdinal_;
   }

   public void setSourceWindowOrdinal(int ordinal)
   {
      thisWindowOrdinal_ = ordinal;
   }

   public static boolean isMainSourceWindow()
   {
      return !Satellite.isCurrentWindowSatellite();
   }

   public JsArray<SourceDocument> getSourceDocs()
   {
      if (isMainSourceWindow())
         return sourceDocs_;
      else
         return getMainWindowSourceDocs();
   }

   /**
    * Given a path to a source document, returns a data structure with information about the document.
    *
    * @param path The path to the document.
    * @return A data structure with document metadata, or null if no metadata was found.
    */
   public SourceDocument getDocFromPath(String path)
   {
      JsArray<SourceDocument> docs = getSourceDocs();
      for (int i = 0; i < docs.length(); i++)
      {
         if (StringUtil.equals(docs.get(i).getPath(), path))
         {
            return docs.get(i);
         }
      }
      return null;
   }

   public boolean isSourceWindowOpen(String windowId)
   {
      return sourceWindows_.containsKey(windowId);
   }

   public boolean areSourceWindowsOpen()
   {
      return !sourceWindows_.isEmpty();
   }

   public String getWindowIdOfDocPath(String path)
   {
      SourceDocument doc = getDocFromPath(path);
      if (doc == null)
         return null;
      String windowId = doc.getSourceWindowId();
      if (windowId != null)
         return windowId;
      else
         return "";
   }

   public String getWindowIdOfDocId(String id)
   {
      JsArray<SourceDocument> docs = getSourceDocs();
      for (int i = 0; i < docs.length(); i++)
      {
         if (docs.get(i).getId() == id)
         {
            String windowId = docs.get(i).getSourceWindowId();
            if (windowId != null)
               return windowId;
            else
               return "";
         }
      }
      return null;
   }

   public void saveWithPrompt(UnsavedChangesItem item, Command onCompleted)
   {
      String windowId = getWindowIdOfDocId(item.getId());
      WindowEx window = getSourceWindowObject(windowId);
      if (window == null || window.isClosed())
      {
         onCompleted.execute();
         return;
      }
      // raise the window and ask it to save the item
      window.focus();
      saveWithPrompt(getSourceWindowObject(windowId), item, onCompleted);
   }

   public void saveUnsavedDocuments(final Set<String> ids,
                                    final Command onCompleted)
   {
      doForAllSourceWindows(new SourceWindowCommand()
      {
         @Override
         public void execute(String windowId,
                             WindowEx window,
                             Command continuation)
         {
            JsArrayString idArray = null;
            if (ids != null)
            {
               idArray = JavaScriptObject.createArray().cast();
               for (String id : ids)
                  idArray.push(id);
            }

            saveUnsavedDocuments(window, idArray, continuation);
         }
      }, onCompleted);
   }

   public void handleUnsavedChangesBeforeExit(
         ArrayList<UnsavedChangesTarget> targets,
         Command onCompleted)
   {
      // accumulate the unsaved change targets that represent satellite windows
      final JsArray<UnsavedChangesItem> items = JsArray.createArray().cast();
      for (UnsavedChangesTarget target: targets)
      {
         if (target instanceof UnsavedChangesItem)
         {
            items.push((UnsavedChangesItem)target);
         }
      }

      // let each window have a crack at saving the targets (the windows will
      // discard any targets they don't own)
      doForAllSourceWindows(new SourceWindowCommand()
      {
         @Override
         public void execute(String windowId, WindowEx window,
               Command continuation)
         {
            handleUnsavedChangesBeforeExit(window, items, continuation);
         }
      },
      onCompleted);
   }

   public void closeAllSatelliteDocs(final String caption,
         final Command onCompleted)
   {
      doForAllSourceWindows(new SourceWindowCommand()
      {
         @Override
         public void execute(String windowId, WindowEx window,
               Command continuation)
         {
            window.focus();
            closeAllDocs(window, caption, continuation);
         }
      },
      new Command()
      {
         @Override
         public void execute()
         {
            // return focus to the main window when finished
            if (Desktop.hasDesktopFrame() || !isMainSourceWindow())
               pSatellite_.get().focusMainWindow();

            // complete operation
            onCompleted.execute();
         }
      });
   }

   public ArrayList<UnsavedChangesTarget> getAllSatelliteUnsavedChanges(
         final int type)
   {
      final ArrayList<UnsavedChangesTarget> targets = new ArrayList<>();
      doForAllSourceWindows(new OperationWithInput<Pair<String,WindowEx>>()
      {
         @Override
         public void execute(Pair<String, WindowEx> input)
         {
            targets.addAll(JsArrayUtil.toArrayList(
                  getUnsavedChanges(input.second, type)));
         }
      });
      return targets;
   }

   // navigates to the file in a source window
   public NavigationResult navigateToFile(FileSystemItem file,
         FilePosition position,
         int navMethod)
   {
      return navigateToPath(file.getPath(),
                     new OpenSourceFileEvent(file, position, null, navMethod),
                     navMethod == NavigationMethods.DEFAULT);
   }

   public NavigationResult navigateToCodeBrowser(String path,
         CrossWindowEvent<?> codeBrowserEvent)
   {
      return navigateToPath(path, codeBrowserEvent,
            codeBrowserEvent instanceof CodeBrowserNavigationEvent);
   }

   public boolean activateLastFocusedSource()
   {
      // if this is a source window, it's a no-op
      if (!StringUtil.isNullOrEmpty(getSourceWindowId()))
         return true;

      // if we don't have the capacity to activate source windows, let the
      // current window handle it
      if (!canActivateSourceWindows())
         return false;

      // if the last window focused was the main one, or there's no longer an
      // addressable window, there's nothing to do
      WindowEx lastFocusedWindow = getLastFocusedSourceWindow();
      if (lastFocusedWindow != null)
      {
         // we found the window that last had focus--refocus it
         pSatelliteManager_.get().activateSatelliteWindow(
               SourceSatellite.NAME_PREFIX + getLastFocusedSourceWindowId());
         return true;
      }

      return false;
   }

   public String getCurrentDocPath()
   {
      // return the document that most recently had focus, whether it was in a
      // source window or the main window
      WindowEx lastFocusedWindow = getLastFocusedSourceWindow();
      if (lastFocusedWindow == null)
         return pSource_.get().getCurrentDocPath();
      else
         return getCurrentDocPath(lastFocusedWindow);
   }

   public String getCurrentDocId()
   {
      WindowEx lastFocusedWindow = getLastFocusedSourceWindow();
      if (lastFocusedWindow == null)
      {
         if (pSource_.get() == null)
            return null;
         else
            return pSource_.get().getCurrentDocId();
      }
      else
         return getCurrentDocId(lastFocusedWindow);
   }

   public CollabEditStartParams getDocCollabParams(String id)
   {
      JsArray<SourceDocument> docs = getSourceDocs();
      for (int i = 0; i < docs.length(); i++)
      {
         if (docs.get(i).getId() == id)
         {
            return docs.get(i).getCollabParams();
         }
      }
      return null;
   }

   public void maximizeSourcePaneIfNecessary()
   {
      if (SourceWindowManager.isMainSourceWindow())
      {
         // see if the Source and Console are paired
         PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
         if (paneConfig == null)
            paneConfig = PaneConfig.createDefault();
         if (hasSourceAndConsolePaired(paneConfig.getQuadrants()))
         {
            events_.fireEvent(new MaximizeSourceWindowEvent());
         }
      }
   }

   public void ensureVisibleSourcePaneIfNecessary()
   {
      if (SourceWindowManager.isMainSourceWindow())
      {
         // see if the Source and Console are paired
         PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
         if (paneConfig == null)
            paneConfig = PaneConfig.createDefault();
         if (hasSourceAndConsolePaired(paneConfig.getQuadrants()))
         {
            events_.fireEvent(new EnsureVisibleSourceWindowEvent());
         }
      }
   }

   public <T extends EventHandler>
   void fireEventToLastFocusedWindow(CrossWindowEvent<T> event)
   {
      String id = getLastFocusedWindowId();
      if (StringUtil.isNullOrEmpty(id))
         events_.fireEvent(event);
      else
         fireEventToSourceWindow(id, event, false);
   }

   public HashMap<String,String> assignSourceDocDisplay(String docId, String name)
   {
      return assignSourceDocDisplay(docId, name, false);
   }

   public HashMap<String,String> assignSourceDocDisplay(String docId, String name, boolean write)
   {
      JsArray<SourceDocument> docs = getSourceDocs();
      for (int i = 0; i < docs.length(); i++)
      {
         SourceDocument doc = docs.get(i);
         if (doc.getId() == docId)
         {
            doc.assignSourceDisplayName(name);
            break;
         }
      }
      HashMap<String,String> props = new HashMap<String,String>();
      props.put(SourceColumnManager.COLUMN_PREFIX, name);

      if (write)
         modifyDocumentProperties(docId, props, null);

      return props;
   }

   // Event handlers ----------------------------------------------------------

   @Override
   public void onEditorCommandDispatch(EditorCommandDispatchEvent dispatchEvent)
   {
      EditorCommandEvent event = dispatchEvent.getEvent();

      String type = event.getType();
      if (type == EditorCommandEvent.TYPE_EDITOR_CONTEXT)
      {
         GetEditorContextEvent.Data data = event.getData();
         fireEventToLastFocusedWindow(new GetEditorContextEvent(data));
      }
      else if (type == EditorCommandEvent.TYPE_REPLACE_RANGES)
      {
         ReplaceRangesEvent.Data data = event.getData();
         fireEventForDocument(data.getId(), new ReplaceRangesEvent(data));
      }
      else if (type == EditorCommandEvent.TYPE_SET_SELECTION_RANGES)
      {
         SetSelectionRangesEvent.Data data = event.getData();
         fireEventForDocument(data.getId(), new SetSelectionRangesEvent(data));
      }
      else
         assert false: "Unrecognized editor event type '" + type + "'";
   }

   @Override
   public void onPopoutDoc(final PopoutDocEvent evt)
   {
      // assign a new window ID to the source document
      final String windowId = createSourceWindowId();
      modifyDocumentProperties(evt.getDocId(), assignSourceDocWindowId(evt.getDocId(), windowId),
            new Command()
            {
               @Override
               public void execute()
               {
                  openSourceWindow(windowId, evt.getOriginator().getPosition(),
                        evt.getDocId(), evt.getSourcePosition());
               }
            });
   }

   @Override
   public void onSourceDocAdded(SourceDocAddedEvent e)
   {
      // if the window that fired the event is not already the owner of the
      // document, make it the owner
      if (e.getDoc().getSourceWindowId() != e.getWindowId() ||
          e.getDoc().getSourceDisplayName() != e.getDisplayName())
      {
         // assign on the doc itself
         e.getDoc().assignSourceWindowId(e.getWindowId());
         e.getDoc().assignSourceDisplayName(e.getDisplayName());

         // assign on the server
         HashMap<String,String> props = assignSourceDocWindowId(e.getDoc().getId(), e.getWindowId());
         props.putAll(assignSourceDocDisplay(e.getDoc().getId(), e.getDisplayName()));

         modifyDocumentProperties(e.getDoc().getId(), props, null);
      }

      // ensure the doc isn't already in our index
      for (int i = 0; i < sourceDocs_.length(); i++)
      {
         if (sourceDocs_.get(i).getId() == e.getDoc().getId())
            return;
      }

      sourceDocs_.push(e.getDoc());
   }

   @Override
   public void onSourceFileSaved(SourceFileSavedEvent event)
   {
      // when a user saves a new doc or does file -> save as, we need to update
      // our internal doc mappings so we can route navigations to the right
      // window for that path
      updateDocPath(event.getDocId(), event.getPath());
   }

   @Override
   public void onCodeBrowserCreated(CodeBrowserCreatedEvent event)
   {
      updateDocPath(event.getId(), event.getPath());
   }

   @Override
   public void onSatelliteClosed(final SatelliteClosedEvent event)
   {
      // if this satellite is closing for quit/shutdown/close/etc., ignore it
      // (we only care about user-initiated window closure)
      if (windowsClosing_)
         return;

      // ignore closure when not a source window
      if (!event.getName().startsWith(SourceSatellite.NAME_PREFIX))
         return;

      // we get this event when the window is unloaded; it could be that the
      // window is unloading for refresh (in which case its docs could be
      // preserved) or closing for good.
      WindowCloseMonitor.monitorSatelliteClosure(event.getName(), new Command()
      {
         @Override
         public void execute()
         {
            closeSourceWindowDocs(sourceWindowId(event.getName()));
         }
      }, null);
   }

   @Override
   public void onSatelliteFocused(SatelliteFocusedEvent event)
   {
      mainWindowFocused_ = false;
   }

   @Override
   public void onAllSatellitesClosing(AllSatellitesClosingEvent event)
   {
      windowsClosing_ = true;
   }

   @Override
   public void onDocTabDragStarted(DocTabDragStartedEvent event)
   {
      // we are the main source window; fire the event to all the source
      // satellites
      fireEventToAllSourceWindows(event);
   }

   @Override
   public void onDocWindowChanged(final DocWindowChangedEvent event)
   {
      if (event.getNewWindowId() == getSourceWindowId())
      {
         // if the doc is moving to this window, assign the ID before firing
         // events
         modifyDocumentProperties(event.getDocId(),
               assignSourceDocWindowId(event.getDocId(), event.getNewWindowId()),
               new Command()
               {
                  @Override
                  public void execute()
                  {
                     broadcastDocWindowChanged(event);
                  }
               });
      }
      else
      {
         broadcastDocWindowChanged(event);
      }
   }

   @Override
   public void onDocTabClosed(DocTabClosedEvent event)
   {
      JsArray<SourceDocument> sourceDocs = getSourceDocs();
      for (int i = 0; i < sourceDocs.length(); i++)
      {
         if (sourceDocs.get(i).getId() == event.getDocId())
         {
            JsArrayUtil.remove(sourceDocs, i);
            break;
         }
      }
   }

   @Override
   public void onShinyApplicationStatus(ShinyApplicationStatusEvent event)
   {
      fireEventToAllSourceWindows(event);
   }


   @Override
   public void onCollabEditStarted(CollabEditStartedEvent event)
   {
      JsArray<SourceDocument> sourceDocs = getSourceDocs();
      for (int i = 0; i < sourceDocs.length(); i++)
      {
         if (sourceDocs.get(i).getPath() == event.getStartParams().getPath())
         {
            sourceDocs.get(i).setCollabParams(event.getStartParams());
            break;
         }
      }
   }

   @Override
   public void onCollabEditEnded(CollabEditEndedEvent event)
   {
      JsArray<SourceDocument> sourceDocs = getSourceDocs();
      for (int i = 0; i < sourceDocs.length(); i++)
      {
         if (sourceDocs.get(i).getPath() == event.getPath())
         {
            sourceDocs.get(i).setCollabParams(null);
            break;
         }
      }
   }

   @Override
   public void onScrollToPosition(ScrollToPositionEvent event)
   {
      if (!StringUtil.isNullOrEmpty(mostRecentSourceWindow_) &&
          isSourceWindowOpen(mostRecentSourceWindow_))
         events_.fireEventToSatellite(event, getSourceWindowObject(mostRecentSourceWindow_));
      else if (!isMainSourceWindow())
         events_.fireEventToMainWindow(event);
   }

   @Override
   public void onDocFocused(final DocFocusedEvent event)
   {
      // defer to ensure that the containing window gets focus too
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand()
      {
         @Override
         public void execute()
         {
            // ignore this event if it's from main window but the main window
            // doesn't have focus
            if (!mainWindowFocused_ && event.isFromMainWindow())
               return;
            String id = event.isFromMainWindow()
                  ? ""
                  : sourceWindowId(event.originWindowName());

            MainWindowObject.lastFocusedSourceWindowId().set(id);
         }
      });
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      fireEventToAllSourceWindows(event);
   }

   // Private methods ---------------------------------------------------------

   private void fireEventForDocument(String docId, CrossWindowEvent<?> event)
   {
      if (StringUtil.isNullOrEmpty(docId))
      {
         fireEventToLastFocusedWindow(event);
         return;
      }

      String windowId = getWindowIdOfDocId(docId);
      if (StringUtil.isNullOrEmpty(windowId))
         events_.fireEventToMainWindow(event);
      else
         fireEventToSourceWindow(windowId, event, false);
   }

   private void fireEventToSourceWindow(String windowId,
         CrossWindowEvent<?> evt,
         boolean focus)
   {
      if (StringUtil.isNullOrEmpty(windowId) && !isMainSourceWindow())
      {
         pSatellite_.get().focusMainWindow();
         events_.fireEventToMainWindow(evt);
      }
      else
      {
         // focus window if requested
         if (focus)
         {
            pSatelliteManager_.get().activateSatelliteWindow(
                  SourceSatellite.NAME_PREFIX + windowId);
         }
         WindowEx window = getSourceWindowObject(windowId);
         if (window != null)
         {
            events_.fireEventToSatellite(evt, window);
         }

      }
   }

   private void openSourceWindow(String windowId, Point position,
         String docId, SourcePosition sourcePosition)
   {
      // create default options
      Size size = new Size(800, 800);
      Integer ordinal = null;

      // if we have geometry for the window, apply it
      SatelliteWindowGeometry geometry = windowGeometry_.getObject(windowId);
      if (geometry != null)
      {
         size = geometry.getSize();
         ordinal = geometry.getOrdinal();
         if (position == null)
            position = geometry.getPosition();
      }

      // if we don't have window geometry for this window, tile based on the
      // geometry of the most recently used source window (otherwise the new
      // window may exactly overlap an existing source window)
      if (geometry == null)
      {
         if (!StringUtil.isNullOrEmpty(mostRecentSourceWindow_) &&
             isSourceWindowOpen(mostRecentSourceWindow_))
         {
            WindowEx window = getSourceWindowObject(mostRecentSourceWindow_);
            if (window != null && !window.isClosed())
            {
               size = new Size(window.getInnerWidth(),
                     window.getInnerHeight());
               if (position == null)
                  position = Point.create(
                        window.getScreenX() + 50,
                        window.getScreenY() + 50);
            }
         }
      }

      // assign ordinal if not already assigned
      if (ordinal == null)
         ordinal = ++maxOrdinal_;

      pSatelliteManager_.get().openSatellite(
            SourceSatellite.NAME_PREFIX + windowId,
            SourceWindowParams.create(
                  ordinal,
                  pWorkbenchContext_.get().createWindowTitle(),
                  pWorkbenchContext_.get().getCurrentWorkingDir().getPath(),
                  docId, sourcePosition),
            size, false, position);

      mostRecentSourceWindow_ = windowId;
      sourceWindows_.put(windowId, ordinal);
   }

   private void broadcastDocWindowChanged(DocWindowChangedEvent event)
   {
      if (isMainSourceWindow() &&
         event.getOldWindowId() != getSourceWindowId())
      {
         // this is the main window; pass the event on to the window that just
         // lost its doc
         fireEventToSourceWindow(event.getOldWindowId(), event, false);

         // raise the window that received the doc (doesn't happen
         // automatically)
         focusSourceWindow(event.getNewWindowId());
      }
      else if (event.getNewWindowId() == getSourceWindowId())
      {
         // this is a satellite; pass the event on to the main window for
         // routing
         events_.fireEventToMainWindow(event);
      }
   }

   private String createSourceWindowId()
   {
      return "w" + StringUtil.makeRandomId(12);
   }

   private static String sourceWindowId(String input)
   {
      if (input != null && input.startsWith(SourceSatellite.NAME_PREFIX))
      {
         return input.substring(SourceSatellite.NAME_PREFIX.length());
      }
      return "";
   }

   private final native static JsArray<SourceDocument> getMainWindowSourceDocs() /*-{
      return $wnd.opener.rstudioSourceDocs;
   }-*/;

   private final native void exportFromMain() /*-{
      // the main window maintains an array of all open source documents
      // across all satellites; rather than attempt to synchronize this list
      // among satellites, the main window exposes it on its window object
      // for the satellites to read
      $wnd.rstudioSourceDocs = this.@org.rstudio.studio.client.workbench.views.source.SourceWindowManager::sourceDocs_;
   }-*/;

   private final native void closeAllDocs(WindowEx satellite, String caption,
         Command onCompleted) /*-{
      satellite.rstudioCloseAllDocs(caption, onCompleted);
   }-*/;

   private final native JsArray<UnsavedChangesItem> getUnsavedChanges(
         WindowEx satellite, int type) /*-{
      return satellite.rstudioGetUnsavedChanges(type);
   }-*/;

   private final native String getCurrentDocPath(WindowEx satellite) /*-{
      return satellite.rstudioGetCurrentDocPath();
   }-*/;

   private final native String getCurrentDocId(WindowEx satellite) /*-{
      return satellite.rstudioGetCurrentDocId();
   }-*/;

   private final native void handleUnsavedChangesBeforeExit(WindowEx satellite,
         JsArray<UnsavedChangesItem> items, Command onCompleted) /*-{
      satellite.rstudioHandleUnsavedChangesBeforeExit(items, onCompleted);
   }-*/;

   private final native void saveWithPrompt(WindowEx satellite,
         UnsavedChangesItem item, Command onCompleted) /*-{
      satellite.rstudioSaveWithPrompt(item, onCompleted);
   }-*/;

   private final native void saveUnsavedDocuments(WindowEx satellite,
                                                  JsArrayString ids,
                                                  Command onCompleted)
   /*-{
      satellite.rstudioSaveUnsavedDocuments(ids, onCompleted);
   }-*/;


   private boolean updateWindowGeometry()
   {
      final ArrayList<String> changedWindows = new ArrayList<String>();
      final JsObject newGeometries = JsObject.createJsObject();

      doForAllSourceWindows(new OperationWithInput<Pair<String,WindowEx>>()
      {
         @Override
         public void execute(Pair<String, WindowEx> input)
         {
            String windowId = input.first;
            WindowEx window = input.second;

            // read the window's current geometry
            SatelliteWindowGeometry newGeometry =
                  SatelliteWindowGeometry.create(
                        sourceWindows_.get(windowId),
                        window.getScreenX(),
                        window.getScreenY(),
                        window.getInnerWidth(),
                        window.getInnerHeight());

            // compare to the old geometry (if any)
            if (windowGeometry_.hasKey(windowId))
            {
               SatelliteWindowGeometry oldGeometry =
                     windowGeometry_.getObject(windowId);
               if (!oldGeometry.equals(newGeometry))
                  changedWindows.add(windowId);
            }
            else
            {
               changedWindows.add(windowId);
            }
            newGeometries.setObject(windowId, newGeometry);
         }
      });

      if (changedWindows.size() > 0)
         windowGeometry_ = newGeometries;

      return changedWindows.size() > 0;
   }

   private void fireEventToAllSourceWindows(CrossWindowEvent<?> event)
   {
      for (String sourceWindowId: sourceWindows_.keySet())
      {
         fireEventToSourceWindow(sourceWindowId, event, false);
      }
   }

   // execute a command on all source windows (synchronously)
   private void doForAllSourceWindows(
         OperationWithInput<Pair<String,WindowEx>> command)
   {
      for (final String windowId: sourceWindows_.keySet())
      {
         final WindowEx window = getSourceWindowObject(windowId);
         if (window == null || window.isClosed())
            continue;

         command.execute(new Pair<String,WindowEx>(windowId, window));
      }
   }

   // execute a command on all source windows (async continuation-style)
   private void doForAllSourceWindows(final SourceWindowCommand command,
         final Command completedCommand)
   {
      final SerializedCommandQueue queue = new SerializedCommandQueue();
      doForAllSourceWindows(new OperationWithInput<Pair<String,WindowEx>>()
      {
         @Override
         public void execute(final Pair<String, WindowEx> input)
         {
            queue.addCommand(new SerializedCommand()
            {
               @Override
               public void onExecute(Command continuation)
               {
                  command.execute(input.first, input.second, continuation);
               }
            });
         }
      });

      if (completedCommand != null)
      {
         queue.addCommand(new SerializedCommand() {
            public void onExecute(Command continuation)
            {
               completedCommand.execute();
               continuation.execute();
            }
         });
      }
   }

   private WindowEx getSourceWindowObject(String windowId)
   {
      return pSatelliteManager_.get().getSatelliteWindowObject(
            SourceSatellite.NAME_PREFIX + windowId);
   }

   private HashMap<String,String> assignSourceDocWindowId(String docId,
         String windowId)
   {
      // assign locally
      JsArray<SourceDocument> docs = getSourceDocs();
      for (int i = 0; i < docs.length(); i++)
      {
         SourceDocument doc = docs.get(i);
         if (doc.getId() == docId)
         {
            // no point in writing a value to the server if we're not changing
            // it
            if (doc.getSourceWindowId() == windowId)
               return new HashMap<String,String>();
            doc.assignSourceWindowId(windowId);
            break;
         }
      }

      // create the new property map
      HashMap<String,String> props = new HashMap<String,String>();
      props.put(SOURCE_WINDOW_ID, windowId);

      return props;
   }

   private void modifyDocumentProperties(String docId, HashMap<String,String> props,
         final Command onComplete)
   {
      if (props == null || props.isEmpty())
         return;

      // update the doc window ID on the server
      server_.modifyDocumentProperties(docId,
             props, new ServerRequestCallback<Void>()
            {
               @Override
               public void onResponseReceived(Void v)
               {
                  if (onComplete != null)
                     onComplete.execute();
               }

               @Override
               public void onError(ServerError error)
               {
                  display_.showErrorMessage("Can't Move Doc",
                        "The document could not be " +
                        "moved to a different window: \n" +
                        error.getMessage());
               }
            });
   }

   private void closeSourceWindowDocs(String windowId)
   {
      // when the user closes a source window, close all the source docs it
      // contained
      for (int i = 0; i < sourceDocs_.length(); i++)
      {
         final SourceDocument doc = sourceDocs_.get(i);
         if (doc.getSourceWindowId() == windowId)
         {
            // change the window ID of the doc back to the main window
            modifyDocumentProperties(doc.getId(),
                  assignSourceDocWindowId(doc.getId(), ""),
                  new Command()
                  {
                     @Override
                     public void execute()
                     {
                        // close the document when finished
                        server_.closeDocument(doc.getId(),
                              new VoidServerRequestCallback());
               }
            });
         }
      }

      // clean up our own reference to the window
      sourceWindows_.remove(windowId);
   }

   private static boolean canActivateSourceWindows()
   {
      return Desktop.hasDesktopFrame() || BrowseCap.isInternetExplorer();
   }

   private void focusSourceWindow(String windowId)
   {
      if (StringUtil.isNullOrEmpty(windowId))
      {
         // activate main window
         if (Desktop.hasDesktopFrame())
            Desktop.getFrame().bringMainFrameToFront();
         else
            WindowEx.get().focus();
      }
      else
      {
         // activate satellite window
         pSatelliteManager_.get().activateSatelliteWindow(
               SourceSatellite.NAME_PREFIX + windowId);
      }
   }

   // this function implements the core of routing navigation requests among
   // source windows; it does the following:
   // 1. attempts to find a window open with the given path
   //    (which can be a physical file or e.g. a code browser)
   // 2. if such a window is found, fires the given event to the window, or
   //    closes the window's instance of the tab (server mode w/tab stealing)
   // 3. if such a window is not found, indicates as much
   private NavigationResult navigateToPath(String path,
         CrossWindowEvent<?> event,
         boolean focus)
   {
      // if this is the main window, check to see if we should route an event
      // there instead
      String sourceWindowId = getWindowIdOfDocPath(path);
      if (isMainSourceWindow())
      {
         // if this is the main window but the doc is open in a satellite...
         if (!StringUtil.isNullOrEmpty(sourceWindowId) &&
             isSourceWindowOpen(sourceWindowId))
         {
            if (canActivateSourceWindows())
            {
               // in desktop mode (and IE) we can bring the appropriate window
               // forward
               fireEventToSourceWindow(sourceWindowId, event, focus);
               return new NavigationResult(NavigationResult.RESULT_NAVIGATED);
            }
            else
            {
               // otherwise, move the tab over to this window by closing it in
               // in its origin window
               JsArray<SourceDocument> sourceDocs = getSourceDocs();
               for (int i = 0; i < sourceDocs.length(); i++)
               {
                  if (sourceDocs.get(i).getPath() == path)
                  {
                     modifyDocumentProperties(sourceDocs.get(i).getId(),
                           assignSourceDocWindowId(sourceDocs.get(i).getId(), getSourceWindowId()),
                           null);
                     fireEventToSourceWindow(sourceWindowId,
                           new DocWindowChangedEvent(
                                 sourceDocs.get(i).getId(), sourceWindowId,
                                 null, sourceDocs.get(i).getCollabParams(), 0, -1),
                           true);
                     return new NavigationResult(
                           NavigationResult.RESULT_RELOCATE,
                           sourceDocs.get(i).getId());
                  }
               }
            }
         }
      }
      else if (sourceWindowId != null &&
               sourceWindowId != getSourceWindowId())
      {
         if (canActivateSourceWindows())
         {
            // in desktop mode (and IE) we can just route to the main window
           events_.fireEventToMainWindow(event);

            // if the destination is the main window, raise it
            if (sourceWindowId.isEmpty())
            {
               pSatellite_.get().focusMainWindow();
            }
            return new NavigationResult(NavigationResult.RESULT_NAVIGATED);
         }
         else
         {
            // otherwise, move the tab over to this window by closing it in
            // in its origin window
            JsArray<SourceDocument> sourceDocs = getSourceDocs();
            for (int i = 0; i < sourceDocs.length(); i++)
            {
               if (sourceDocs.get(i).getPath() == path)
               {
                  // take ownership of the doc immediately
                  modifyDocumentProperties(sourceDocs.get(i).getId(),
                                           assignSourceDocWindowId(sourceDocs.get(i).getId(),
                                                                   getSourceWindowId()),
                                           null);
                  events_.fireEventToMainWindow(new DocWindowChangedEvent(
                              sourceDocs.get(i).getId(), sourceWindowId, null,
                              sourceDocs.get(i).getCollabParams(), 0, -1));
                  return new NavigationResult(NavigationResult.RESULT_RELOCATE,
                        sourceDocs.get(i).getId());
               }
            }
         }
      }
      return new NavigationResult(NavigationResult.RESULT_NONE);
   }

   private void updateDocPath(String id, String path)
   {
      for (int i = 0; i < sourceDocs_.length(); i++)
      {
         if (sourceDocs_.get(i).getId() == id)
         {
            sourceDocs_.get(i).setPath(path);
            break;
         }
      }
   }

   private WindowEx getLastFocusedSourceWindow()
   {
      String lastFocusedSourceWindow =
            MainWindowObject.lastFocusedSourceWindowId().get();

      // if the last window focused was the main one, or there's no longer an
      // addressable window, there's nothing to do
      if (StringUtil.isNullOrEmpty(lastFocusedSourceWindow) ||
          !isSourceWindowOpen(lastFocusedSourceWindow))
         return null;

      WindowEx window = getSourceWindowObject(lastFocusedSourceWindow);
      if (window != null && !window.isClosed())
      {
         return window;
      }

      return null;
   }

   private boolean hasSourceAndConsolePaired(JsArrayString panes)
   {
      // default config
      if (panes == null)
         return true;

      // if there aren't 4 panes this is a configuration we
      // don't recognize
      if (panes.length() != 4)
         return false;

      // check for paired config
      return hasSourceAndConsolePaired(panes.get(0), panes.get(1)) ||
             hasSourceAndConsolePaired(panes.get(2), panes.get(3));
   }

   private boolean hasSourceAndConsolePaired(String pane1, String pane2)
   {
      return (pane1 == UserPrefsAccessor.Panes.QUADRANTS_SOURCE &&
              pane2 == UserPrefsAccessor.Panes.QUADRANTS_CONSOLE)
                 ||
             (pane1 == UserPrefsAccessor.Panes.QUADRANTS_CONSOLE &&
              pane2 == UserPrefsAccessor.Panes.QUADRANTS_SOURCE);
   }

   // Private types -----------------------------------------------------------

   private interface SourceWindowCommand
   {
      public void execute(String windowId, WindowEx window,
            Command continuation);
   }

   // Members -----------------------------------------------------------------

   private final EventBus events_;
   private final Provider<SatelliteManager> pSatelliteManager_;
   private final Provider<Satellite> pSatellite_;
   private final Provider<WorkbenchContext> pWorkbenchContext_;
   private final SourceServerOperations server_;
   private final GlobalDisplay display_;
   private final Provider<Source> pSource_;
   private final UserPrefs userPrefs_;

   private final HashMap<String, Integer> sourceWindows_ = new HashMap<>();
   private JsArray<SourceDocument> sourceDocs_ =
         JsArray.createArray().cast();
   private boolean windowsClosing_ = false;
   private JsObject windowGeometry_ = JsObject.createJsObject();
   private int maxOrdinal_ = 0;
   private int thisWindowOrdinal_ = 0;

   private String mostRecentSourceWindow_ = "";
   private boolean mainWindowFocused_ = true;

   public final static String SOURCE_WINDOW_ID = "source_window_id";
}
