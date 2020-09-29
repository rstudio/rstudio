/*
 * SourceColumnManager.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.rstudio.core.client.*;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.*;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.events.GetEditorContextEvent;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.palette.model.CommandPaletteItem;
import org.rstudio.studio.client.rmarkdown.model.RmdChosenTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.server.ErrorLoggingServerRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.FileMRUList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.ui.PaneConfig;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.environment.events.DebugModeChangedEvent;
import org.rstudio.studio.client.workbench.views.output.find.events.FindInFilesEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Selection;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditingTargetSelectedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRMarkdownDialog;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class SourceColumnManager implements CommandPaletteEntrySource,
                                            SourceExtendedTypeDetectedEvent.Handler,
                                            DebugModeChangedEvent.Handler
{
   public interface CPSEditingTargetCommand
   {
      void execute(EditingTarget editingTarget, Command continuation);
   }

   public static class State extends JavaScriptObject
   {
      public static native State createState(JsArrayString names, String activeColumn) /*-{
         return {
            names: names,
            activeColumn: activeColumn
         }
      }-*/;

      protected State()
      {}

      public final String[] getNames()
      {
         return JsUtil.toStringArray(getNamesNative());
      }

      public final native String getActiveColumn() /*-{
         return this.activeColumn || "";
      }-*/;

      private native JsArrayString getNamesNative() /*-{
          return this.names;
      }-*/;
   }

   interface Binder extends CommandBinder<Commands, SourceColumnManager>
   {
   }

   public static class ColumnName
   {
      public ColumnName()
      {
         name_ = "";
         accessibleName_ = "";
      }

      public ColumnName(String name, String accessibleName)
      {
         name_ = name;
         accessibleName_ = accessibleName;
      }

      public String getName()
      {
         return name_;
      }

      public String getAccessibleName()
      {
         return accessibleName_;
      }

      private final String name_;
      private final String accessibleName_;
   }

   SourceColumnManager() { RStudioGinjector.INSTANCE.injectMembers(this);}

   @Inject
   public SourceColumnManager(Binder binder,
                              Source.Display display,
                              SourceServerOperations server,
                              GlobalDisplay globalDisplay,
                              Commands commands,
                              EditingTargetSource editingTargetSource,
                              FileTypeRegistry fileTypeRegistry,
                              EventBus events,
                              DependencyManager dependencyManager,
                              final Session session,
                              Synctex synctex,
                              UserPrefs userPrefs,
                              UserState userState,
                              Provider<FileMRUList> pMruList,
                              SourceWindowManager windowManager)
   {
      commands_ = commands;
      binder.bind(commands_, this);

      server_ = server;
      globalDisplay_ = globalDisplay;
      editingTargetSource_ = editingTargetSource;
      fileTypeRegistry_ = fileTypeRegistry;
      events_ = events;
      dependencyManager_ = dependencyManager;
      session_ = session;
      synctex_ = synctex;
      userPrefs_ = userPrefs;
      userState_ = userState;
      pMruList_ = pMruList;
      windowManager_ = windowManager;

      rmarkdown_ = new TextEditingTargetRMarkdownHelper();
      vimCommands_ = new SourceVimCommands();
      columnState_ = null;
      initDynamicCommands();

      events_.addHandler(SourceExtendedTypeDetectedEvent.TYPE, this);
      events_.addHandler(DebugModeChangedEvent.TYPE, this);

      events_.addHandler(EditingTargetSelectedEvent.TYPE,
         new EditingTargetSelectedEvent.Handler()
         {
            @Override
            public void onEditingTargetSelected(EditingTargetSelectedEvent event)
            {
               setActive(event.getTarget());
            }
         });

      events_.addHandler(SourceFileSavedEvent.TYPE, new SourceFileSavedHandler()
      {
         public void onSourceFileSaved(SourceFileSavedEvent event)
         {
            pMruList_.get().add(event.getPath());
         }
      });

      events_.addHandler(DocTabActivatedEvent.TYPE, new DocTabActivatedEvent.Handler()
      {
         public void onDocTabActivated(DocTabActivatedEvent event)
         {
            setActiveDocId(event.getId());
         }
      });

      events_.addHandler(SwitchToDocEvent.TYPE, new SwitchToDocHandler()
      {
         public void onSwitchToDoc(SwitchToDocEvent event)
         {
            ensureVisible(false);
            activeColumn_.setPhysicalTabIndex(event.getSelectedIndex());

            // Fire an activation event just to ensure the activated
            // tab gets focus
            commands_.activateSource().execute();
         }
      });

      sourceNavigationHistory_.addChangeHandler(event -> manageSourceNavigationCommands());

      SourceColumn column = GWT.create(SourceColumn.class);
      column.loadDisplay(MAIN_SOURCE_NAME, MAIN_SOURCE_NAME, display, this);
      columnList_.add(column);

      new JSObjectStateValue("source-column-manager",
                             "column-info",
                              ClientState.PERSISTENT,
                              session_.getSessionInfo().getClientState(),
                              false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            if (!userPrefs_.allowSourceColumns().getGlobalValue())
            {
               if (columnList_.size() > 1)
               {
                  PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
                  userPrefs_.panes().setGlobalValue(PaneConfig.create(
                     JsArrayUtil.copy(paneConfig.getQuadrants()),
                     paneConfig.getTabSet1(),
                     paneConfig.getTabSet2(),
                     paneConfig.getHiddenTabSet(),
                     paneConfig.getConsoleLeftOnTop(),
                     paneConfig.getConsoleRightOnTop(),
                     0).cast());
                  consolidateColumns(1);
               }
               return;
            }

            if (value == null)
            {
               // default to main column name here because we haven't loaded any columns yet
               columnState_ =
                  State.createState(JsUtil.toJsArrayString(getNames(false)),
                                    MAIN_SOURCE_NAME);
               return;
            }

            State state = value.cast();
            for (int i = 0; i < state.getNames().length; i++)
            {
               String name = state.getNames()[i];
               if (!StringUtil.equals(name, MAIN_SOURCE_NAME))
                  add(name, false);
            }
         }

         @Override
         protected JsObject getValue()
         {
            return columnState_.cast();
         }
      };

      setActive(column.getName());

      // register custom focus handler for case where ProseMirror
      // instance (or element within) had focus
      ModalDialogBase.registerReturnFocusHandler((Element el) ->
      {
         final String sourceClass = ClassIds.getClassId(ClassIds.SOURCE_PANEL);
         Element sourceEl = DomUtils.findParentElement(el, (Element parent) ->
         {
            return parent.hasClassName(sourceClass);
         });

         if (sourceEl != null)
         {
            commands_.activateSource().execute();
            return true;
         }

         return false;
      });
   }

   public ColumnName add()
   {
      Source.Display display = GWT.create(SourcePane.class);
      return add(display, false);
   }

   public ColumnName add(Source.Display display)
   {
      return add(display, false);
   }

   public ColumnName add(String name, boolean updateState)
   {
      return add(name, false, updateState);
   }

   public ColumnName add (String name, boolean activate, boolean updateState)
   {
      Source.Display display = GWT.create(SourcePane.class);
      return add(name, computeAccessibleName(), display, activate, updateState);
   }

   public ColumnName add(Source.Display display, boolean activate)
   {
      return add(display, activate, true);
   }

   public ColumnName add(Source.Display display, boolean activate, boolean updateState)
   {
      return add(COLUMN_PREFIX + StringUtil.makeRandomId(12),
                  computeAccessibleName(),
                  display,
                  activate,
                  updateState);
   }

   public ColumnName add(String name, String accessibleName, Source.Display display,
                     boolean activate, boolean updateState)
   {
      if (contains(name))
         return new ColumnName();

      SourceColumn column = GWT.create(SourceColumn.class);
      column.loadDisplay(name, accessibleName, display, this);
      columnList_.add(column);

      if (activate || activeColumn_ == null)
         setActive(column);

      if (updateState)
         columnState_ = State.createState(JsUtil.toJsArrayString(getNames(false)),
                                          getActive().getName());
      return new ColumnName(column.getName(), column.getAccessibleName());
   }

   public void initialSelect(int index)
   {
      SourceColumn lastActive = getByName(columnState_.getActiveColumn());
      if (lastActive != null)
         setActive(getByName(columnState_.getActiveColumn()));
      getActive().initialSelect(index);
   }

   public void setActive(int xpos)
   {
      SourceColumn column = findByPosition(xpos);
      if (column == null)
         return;
      setActive(column);
   }

   public void setActive(String name)
   {
      if (StringUtil.isNullOrEmpty(name) &&
          activeColumn_ != null)
      {
         if (hasActiveEditor())
            activeColumn_.setActiveEditor("");
         activeColumn_ = null;
         return;
      }

      // If we can't find the column, use the main column. This may happen on start up.
      SourceColumn column = getByName(name);
      if (column == null)
         column = getByName(MAIN_SOURCE_NAME);
      setActive(column);
   }

   private void setActive(EditingTarget target)
   {
      setActive(findByDocument(target.getId()));
      activeColumn_.setActiveEditor(target);
   }

   public void setActive(SourceColumn column)
   {
      SourceColumn prevColumn = activeColumn_;
      activeColumn_ = column;

      // If the active column changed, we need to update the active editor
      if (prevColumn != null && prevColumn != activeColumn_)
      {
         prevColumn.setActiveEditor("");
         if (!hasActiveEditor())
            activeColumn_.setActiveEditor();
         manageCommands(true);
      }

      columnState_ = State.createState(JsUtil.toJsArrayString(getNames(false)),
                                       activeColumn_ == null ? "" : activeColumn_.getName());
   }

   private void setActiveDocId(String docId)
   {
      if (StringUtil.isNullOrEmpty(docId))
         return;

      for (SourceColumn column : columnList_)
      {
         EditingTarget target = column.setActiveEditor(docId);
         if (target != null)
         {
            setActive(target);
            return;
         }
      }
   }

   public void setDocsRestored()
   {
      docsRestored_ = true;
   }

   public void setOpeningForSourceNavigation(boolean value)
   {
      openingForSourceNavigation_ = value;
   }

   public void activateColumn(String name, final Command afterActivation)
   {
      if (!StringUtil.isNullOrEmpty(name))
         setActive(getByName(name));
      if (!hasActiveEditor())
      {
         if (activeColumn_ == null)
            setActive(MAIN_SOURCE_NAME);
         newDoc(FileTypeRegistry.R, new ResultCallback<EditingTarget, ServerError>()
         {
            @Override
            public void onSuccess(EditingTarget target)
            {
               setActive(target);
               doActivateSource(afterActivation);
            }
         });
      }
      else
      {
         doActivateSource(afterActivation);
      }
   }

   public String getLeftColumnName()
   {
     return columnList_.get(columnList_.size() - 1).getName();
   }

   public String getNextColumnName()
   {
      int index = columnList_.indexOf(getActive());
      if (index < 1)
         return "";
      else
         return columnList_.get(index - 1).getName();
   }

   public String getPreviousColumnName()
   {
      int index = columnList_.indexOf(getActive());
      if (index == getSize() - 1)
         return "";
      else
         return columnList_.get(index + 1).getName();

   }

   // This method sets activeColumn_ to the main column if it is null. It should be used in cases
   // where it is better for the column to be the main column than null.
   public SourceColumn getActive()
   {
      if (activeColumn_ != null &&
         (!columnList_.get(0).asWidget().isAttached() ||
          activeColumn_.asWidget().isAttached() &&
          activeColumn_.asWidget().getOffsetWidth() > 0))
         return activeColumn_;
      setActive(MAIN_SOURCE_NAME);

      return activeColumn_;
   }

   public String getActiveDocId()
   {
      if (hasActiveEditor())
         return activeColumn_.getActiveEditor().getId();
      return null;
   }

   public String getActiveDocPath()
   {
      if (hasActiveEditor())
         return activeColumn_.getActiveEditor().getPath();
      return null;
   }

   public boolean hasActiveEditor()
   {
      return activeColumn_ != null && activeColumn_.getActiveEditor() != null;
   }

   public boolean isActiveEditor(EditingTarget editingTarget)
   {
      return hasActiveEditor() && activeColumn_.getActiveEditor() == editingTarget;
   }

   public boolean getDocsRestored()
   {
      return docsRestored_;
   }

   // see if there are additional command palette items made available
   // by the active editor
   public List<CommandPaletteItem> getCommandPaletteItems()
   {
      if (!hasActiveEditor())
         return null;

      return activeColumn_.getActiveEditor().getCommandPaletteItems();
   }

   public int getTabCount()
   {
      return getActive().getTabCount();
   }

   public int getPhysicalTabIndex()
   {
      if (getActive() != null)
         return getActive().getPhysicalTabIndex();
      else
         return -1;
   }

   public ArrayList<String> getNames(boolean excludeMain)
   {
      ArrayList<String> result = new ArrayList<>();
      columnList_.forEach((column) ->{
         if (!excludeMain || !StringUtil.equals(column.getName(), MAIN_SOURCE_NAME))
            result.add(column.getName());
      });
      return result;
   }

   public ArrayList<Widget> getWidgets(boolean excludeMain)
   {
      ArrayList<Widget> result = new ArrayList<>();
      for (SourceColumn column : columnList_)
      {
         if (!excludeMain || !StringUtil.equals(column.getName(), MAIN_SOURCE_NAME))
            result.add(column.asWidget());
      }
      return result;
   }

   public ArrayList<SourceColumn> getColumnList()
   {
      return columnList_;
   }

   public Element getActiveElement()
   {
      if (activeColumn_ == null)
         return null;
      return activeColumn_.asWidget().getElement();
   }

   public Widget getWidget(String name)
   {
      return getByName(name) == null ? null : getByName(name).asWidget();
   }

   public Session getSession()
   {
      return session_;
   }

   public SourceNavigationHistory getSourceNavigationHistory()
   {
      return sourceNavigationHistory_;
   }

   public void recordCurrentNavigationHistoryPosition()
   {
      if (hasActiveEditor())
         activeColumn_.getActiveEditor().recordCurrentNavigationPosition();
   }

   public String getEditorPositionString()
   {
      if (hasActiveEditor())
         return activeColumn_.getActiveEditor().getCurrentStatus();
      return "No document tabs open";
   }

   public Synctex getSynctex()
   {
      return synctex_;
   }

   public UserState getUserState()
   {
      return userState_;
   }

   public int getSize()
   {
      return columnList_.size();
   }

   public SourceColumn get(int index)
   {
      if (index >= columnList_.size())
         return null;
      return columnList_.get(index);
   }

   public int getUntitledNum(String prefix)
   {
      AtomicInteger max = new AtomicInteger();
      columnList_.forEach((column) ->
          max.set(Math.max(max.get(), column.getUntitledNum(prefix))));
      return max.intValue();
   }

   public native final int getUntitledNum(String name, String prefix) /*-{
       var match = (new RegExp("^" + prefix + "([0-9]{1,5})$")).exec(name);
       if (!match)
           return 0;
       return parseInt(match[1]);
   }-*/;

   public void clearSourceNavigationHistory()
   {
      if (!hasDoc())
         sourceNavigationHistory_.clear();
   }

   public void manageCommands(boolean forceSync)
   {
      boolean saveAllEnabled = false;

      for (SourceColumn column : columnList_)
      {
         if (column.isInitialized() &&
            !StringUtil.equals(getActive().getName(), column.getName()))
            column.manageCommands(forceSync, activeColumn_);

         // if one document is dirty then we are enabled
         if (!saveAllEnabled && column.isSaveCommandActive())
           saveAllEnabled = true;
      }

      // the active column is always managed last because any column can disable a command, but
      // only the active one can enable a command
      if (activeColumn_.isInitialized())
         activeColumn_.manageCommands(forceSync, activeColumn_);

      if (!session_.getSessionInfo().getAllowShell())
         commands_.sendToTerminal().setVisible(false);

      // if source windows are open, managing state of the command becomes
      // complicated, so leave it enabled
      if (windowManager_.areSourceWindowsOpen())
         commands_.saveAllSourceDocs().setEnabled(saveAllEnabled);

      if (activeColumn_ != null &&
          !StringUtil.equals(activeColumn_.getName(), MAIN_SOURCE_NAME))
         commands_.focusSourceColumnSeparator().setEnabled(true);
      else
         commands_.focusSourceColumnSeparator().setEnabled(false);
      manageSourceNavigationCommands();
   }

   private void manageSourceNavigationCommands()
   {
      commands_.sourceNavigateBack().setEnabled(
         sourceNavigationHistory_.isBackEnabled());
      commands_.sourceNavigateForward().setEnabled(
         sourceNavigationHistory_.isForwardEnabled());
   }

   public EditingTarget addTab(SourceDocument doc, int mode, SourceColumn column)
   {
      if (column == null)
         column = getActive();
      return column.addTab(doc, mode);
   }

   public EditingTarget addTab(SourceDocument doc, boolean atEnd,
                               int mode, SourceColumn column)
   {
      if (column == null || getByName(column.getName()) == null)
         column = getActive();
      return column.addTab(doc, atEnd, mode);
   }

   public EditingTarget findEditor(String docId)
   {
      for (SourceColumn column : columnList_)
      {
         EditingTarget target = column.getDoc(docId);
         if (target != null)
            return target;
      }
      return null;
   }

   public EditingTarget findEditorByPath(String path)
   {
      if (StringUtil.isNullOrEmpty(path))
         return null;

      for (SourceColumn column : columnList_)
      {
         EditingTarget target = column.getEditorWithPath(path);
         if (target != null)
            return target;
      }
      return null;
   }

   public SourceColumn findByDocument(String docId)
   {
      for (SourceColumn column : columnList_)
      {
         if (column.hasDoc(docId))
            return column;
      }
      return null;
   }

   public SourceColumn findByPosition(int x)
   {
      for (SourceColumn column : columnList_)
      {

         Widget w = column.asWidget();
         int left = w.getAbsoluteLeft();
         int right = w.getAbsoluteLeft() + w.getOffsetWidth();

         if (x >= left && x <= right)
            return column;
      }
      return null;
   }

   public boolean isEmpty(String name)
   {
      return getByName(name) == null || getByName(name).getTabCount() == 0;
   }

   public boolean requestActiveEditorContext()
   {
      boolean hasActiveEditor =
            hasActiveEditor() &&
            activeColumn_.getActiveEditor() instanceof TextEditingTarget;

      if (!hasActiveEditor)
         return false;

      TextEditingTarget editingTarget = (TextEditingTarget) activeColumn_.getActiveEditor();
      editingTarget.getEditorContext();
      return true;
   }

   public void activateCodeBrowser(
      final String codeBrowserPath,
      boolean replaceIfActive,
      final ResultCallback<CodeBrowserEditingTarget, ServerError> callback)
   {
      // first check to see if this request can be fulfilled with an existing
      // code browser tab
      EditingTarget target = selectTabWithDocPath(codeBrowserPath);
      if (target != null)
      {
         callback.onSuccess((CodeBrowserEditingTarget) target);
         return;
      }

      // then check to see if the active editor is a code browser -- if it is,
      // we'll use it as is, replacing its contents
      if (replaceIfActive &&
          hasActiveEditor() &&
          activeColumn_.getActiveEditor() instanceof CodeBrowserEditingTarget)
      {
         events_.fireEvent(new CodeBrowserCreatedEvent(activeColumn_.getActiveEditor().getId(),
            codeBrowserPath));
         callback.onSuccess((CodeBrowserEditingTarget) activeColumn_.getActiveEditor());
         return;
      }

      // create a new one
      newDoc(FileTypeRegistry.CODEBROWSER,
         new ResultCallback<EditingTarget, ServerError>()
         {
            @Override
            public void onSuccess(EditingTarget arg)
            {
               events_.fireEvent(new CodeBrowserCreatedEvent(
                  arg.getId(), codeBrowserPath));
               callback.onSuccess((CodeBrowserEditingTarget) arg);
            }

            @Override
            public void onFailure(ServerError error)
            {
               callback.onFailure(error);
            }

            @Override
            public void onCancelled()
            {
               callback.onCancelled();
            }

         });
   }

   public void activateObjectExplorer(ObjectExplorerHandle handle)
   {
      columnList_.forEach((column) -> {
         for (EditingTarget target : column.getEditors())
         {
            // bail if this isn't an object explorer filetype
            FileType fileType = target.getFileType();
            if (!(fileType instanceof ObjectExplorerFileType))
               continue;

            // check for identical titles
            if (handle.getTitle() == target.getTitle())
            {
               ((ObjectExplorerEditingTarget) target).update(handle);
               ensureVisible(false);
               column.selectTab(target.asWidget());
               return;
            }
         }
      });

      ensureVisible(true);
      server_.newDocument(
         FileTypeRegistry.OBJECT_EXPLORER.getTypeId(),
         null,
         (JsObject) handle.cast(),
         new SimpleRequestCallback<SourceDocument>("Show Object Explorer")
         {
            @Override
            public void onResponseReceived(SourceDocument response)
            {
               getActive().addTab(response, Source.OPEN_INTERACTIVE);
            }
         });
   }

   public void showOverflowPopout()
   {
      ensureVisible(false);
      getActive().showOverflowPopout();
   }

   public void showDataItem(DataItem data)
   {
      for (SourceColumn column : columnList_)
      {
         for (EditingTarget target : column.getEditors())
         {
            String path = target.getPath();
            if (path != null && path.equals(data.getURI()))
            {
               ((DataEditingTarget) target).updateData(data);

               ensureVisible(false);
               column.selectTab(target.asWidget());
               return;
            }
         }
      }

      ensureVisible(true);
      server_.newDocument(
         FileTypeRegistry.DATAFRAME.getTypeId(),
         null,
         (JsObject) data.cast(),
         new SimpleRequestCallback<SourceDocument>("Show Data Frame")
         {
            @Override
            public void onResponseReceived(SourceDocument response)
            {
               getActive().addTab(response, Source.OPEN_INTERACTIVE);
            }
         });
   }

   public void showUnsavedChangesDialog(
      String title,
      ArrayList<UnsavedChangesTarget> dirtyTargets,
      OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
      Command onCancelled)
   {
      getActive().showUnsavedChangesDialog(title, dirtyTargets, saveOperation, onCancelled);
   }

   public boolean insertSource(String code, boolean isBlock)
   {
      if (!hasActiveEditor())
         return false;
      return getActive().insertCode(code, isBlock);
   }

   @Handler
   public void onMoveTabRight()
   {
      getActive().moveTab(activeColumn_.getPhysicalTabIndex(), 1);
   }

   @Handler
   public void onMoveTabLeft()
   {
      getActive().moveTab(activeColumn_.getPhysicalTabIndex(), -1);
   }

   @Handler
   public void onMoveTabToFirst()
   {
      getActive().moveTab(activeColumn_.getPhysicalTabIndex(),
         activeColumn_.getPhysicalTabIndex() * -1);
   }

   @Handler
   public void onMoveTabToLast()
   {
      getActive().moveTab(activeColumn_.getPhysicalTabIndex(),
         (activeColumn_.getTabCount() -
            activeColumn_.getPhysicalTabIndex()) - 1);
   }

   @Handler
   public void onSwitchToTab()
   {
      if (getActive().getTabCount() == 0)
         return;
      showOverflowPopout();
   }

   @Handler
   public void onFirstTab()
   {
      if (getActive().getTabCount() == 0)
         return;

      ensureVisible(false);
      if (getActive().getTabCount() > 0)
         getActive().setPhysicalTabIndex(0);
   }

   @Handler
   public void onPreviousTab()
   {
      switchToTab(-1, userPrefs_.wrapTabNavigation().getValue());
   }

   @Handler
   public void onNextTab()
   {
      switchToTab(1, userPrefs_.wrapTabNavigation().getValue());
   }

   @Handler
   public void onLastTab()
   {
      if (getActive().getTabCount() == 0)
         return;

      activeColumn_.ensureVisible(false);
      if (activeColumn_.getTabCount() > 0)
         activeColumn_.setPhysicalTabIndex(activeColumn_.getTabCount() - 1);
   }

   @Handler
   public void onCloseSourceDoc()
   {
      closeSourceDoc(true);
   }

   @Handler
   public void onFindInFiles()
   {
      String searchPattern = "";
      if (hasActiveEditor() && activeColumn_.getActiveEditor() instanceof TextEditingTarget)
      {
         TextEditingTarget textEditor = (TextEditingTarget) activeColumn_.getActiveEditor();
         String selection = textEditor.getSelectedText();
         boolean multiLineSelection = selection.indexOf('\n') != -1;

         if ((selection.length() != 0) && !multiLineSelection)
            searchPattern = selection;
      }

      events_.fireEvent(new FindInFilesEvent(searchPattern));
   }

   @Override
   public void onDebugModeChanged(DebugModeChangedEvent evt)
   {
      // when debugging ends, always disengage any active debug highlights
      if (!evt.debugging() && hasActiveEditor())
      {
         activeColumn_.getActiveEditor().endDebugHighlighting();
      }
   }

   @Override
   public void onSourceExtendedTypeDetected(SourceExtendedTypeDetectedEvent e)
   {
      // set the extended type of the specified source file

      EditingTarget target = findEditor(e.getDocId());
      if (target != null)
         target.adaptToExtendedFileType(e.getExtendedType());
   }

   public void nextTabWithWrap()
   {
      switchToTab(1, true);
   }

   public void prevTabWithWrap()
   {
      switchToTab(-1, true);
   }

   private void switchToTab(int delta, boolean wrap)
   {
      if (getActive().getTabCount() == 0)
         return;

      activeColumn_.ensureVisible(false);

      int targetIndex = activeColumn_.getPhysicalTabIndex() + delta;
      if (targetIndex > (activeColumn_.getTabCount() - 1))
      {
         if (wrap)
            targetIndex = 0;
         else
            return;
      }
      else if (targetIndex < 0)
      {
         if (wrap)
            targetIndex = activeColumn_.getTabCount() - 1;
         else
            return;
      }
      activeColumn_.setPhysicalTabIndex(targetIndex);
   }

   private void doActivateSource(final Command afterActivation)
   {
      getActive().ensureVisible(false);
      if (hasActiveEditor())
      {
         activeColumn_.getActiveEditor().focus();
         activeColumn_.getActiveEditor().ensureCursorVisible();
      }

      if (afterActivation != null)
         afterActivation.execute();
   }

   // new doc functions

   public void newRMarkdownV1Doc()
   {
      newSourceDocWithTemplate(FileTypeRegistry.RMARKDOWN,
         "",
         "v1.Rmd",
         Position.create(3, 0));
   }

   public void newRMarkdownV2Doc()
   {
      rmarkdown_.showNewRMarkdownDialog(
         new OperationWithInput<NewRMarkdownDialog.Result>()
         {
            @Override
            public void execute(final NewRMarkdownDialog.Result result)
            {
               if (result == null)
               {
                  // No document chosen, just create an empty one
                  newSourceDocWithTemplate(FileTypeRegistry.RMARKDOWN, "", "default.Rmd");
               }
               else if (result.isNewDocument())
               {
                  NewRMarkdownDialog.RmdNewDocument doc =
                     result.getNewDocument();
                  String author = doc.getAuthor();
                  if (author.length() > 0)
                  {
                     userPrefs_.documentAuthor().setGlobalValue(author);
                     userPrefs_.writeUserPrefs();
                  }
                  newRMarkdownV2Doc(doc);
               }
               else
               {
                  newDocFromRmdTemplate(result);
               }
            }
         });
   }

   private void newDocFromRmdTemplate(final NewRMarkdownDialog.Result result)
   {
      final RmdChosenTemplate template = result.getFromTemplate();
      if (template.createDir())
      {
         rmarkdown_.createDraftFromTemplate(template);
         return;
      }

      rmarkdown_.getTemplateContent(template,
         new OperationWithInput<String>()
         {
            @Override
            public void execute(final String content)
            {
               if (content.length() == 0)
                  globalDisplay_.showErrorMessage("Template Content Missing",
                     "The template at " + template.getTemplatePath() +
                        " is missing.");
               newDoc(FileTypeRegistry.RMARKDOWN, content, null);
            }
         });
   }


   private void newRMarkdownV2Doc(
      final NewRMarkdownDialog.RmdNewDocument doc)
   {
      rmarkdown_.frontMatterToYAML((RmdFrontMatter) doc.getJSOResult().cast(),
         null,
         new CommandWithArg<String>()
         {
            @Override
            public void execute(final String yaml)
            {
               String template = "";
               // select a template appropriate to the document type we're creating
               if (doc.getTemplate().equals(RmdTemplateData.PRESENTATION_TEMPLATE))
                  template = "presentation.Rmd";
               else if (doc.isShiny())
               {
                  if (doc.getFormat().endsWith(
                     RmdOutputFormat.OUTPUT_PRESENTATION_SUFFIX))
                     template = "shiny_presentation.Rmd";
                  else
                     template = "shiny.Rmd";
               }
               else
                  template = "document.Rmd";
               newSourceDocWithTemplate(FileTypeRegistry.RMARKDOWN,
                  "",
                  template,
                  Position.create(1, 0),
                  null,
                  new TransformerCommand<String>()
                  {
                     @Override
                     public String transform(String input)
                     {
                        return RmdFrontMatter.FRONTMATTER_SEPARATOR +
                           yaml +
                           RmdFrontMatter.FRONTMATTER_SEPARATOR + "\n" +
                           input;
                     }
                  });
            }
         });
   }

   public void newSourceDocWithTemplate(final TextFileType fileType,
                                        String name,
                                        String template)
   {
      newSourceDocWithTemplate(fileType, name, template, null);
   }

   public void newSourceDocWithTemplate(final TextFileType fileType,
                                        String name,
                                        String template,
                                        final Position cursorPosition)
   {
      newSourceDocWithTemplate(fileType, name, template, cursorPosition, null);
   }

   public void newSourceDocWithTemplate(
      final TextFileType fileType,
      String name,
      String template,
      final Position cursorPosition,
      final CommandWithArg<EditingTarget> onSuccess)
   {
      newSourceDocWithTemplate(fileType, name, template, cursorPosition, onSuccess, null);
   }

   public void startDebug()
   {
      getActive().setPendingDebugSelection();
   }

   private EditingTarget selectTabWithDocPath(String path)
   {
      for (SourceColumn column : columnList_)
      {
         EditingTarget editor = column.getEditorWithPath(path);
         if (editor != null)
         {
            column.selectTab(editor.asWidget());
            return editor;
         }
      }
      return null;
   }

   private void newSourceDocWithTemplate(
      final TextFileType fileType,
      String name,
      String template,
      final Position cursorPosition,
      final CommandWithArg<EditingTarget> onSuccess,
      final TransformerCommand<String> contentTransformer)
   {
      final ProgressIndicator indicator = new GlobalProgressDelayer(
         globalDisplay_, 500, "Creating new document...").getIndicator();

      server_.getSourceTemplate(name,
         template,
         new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String templateContents)
            {
               indicator.onCompleted();

               if (contentTransformer != null)
                  templateContents = contentTransformer.transform(templateContents);

               newDoc(fileType,
                  templateContents,
                  new ResultCallback<EditingTarget, ServerError>()
                  {
                     @Override
                     public void onSuccess(EditingTarget target)
                     {
                        if (cursorPosition != null)
                           target.setCursorPosition(cursorPosition);

                        if (onSuccess != null)
                           onSuccess.execute(target);
                     }
                  });
            }

            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }
         });
   }

   public void newDoc(EditableFileType fileType,
                      ResultCallback<EditingTarget, ServerError> callback)
   {
      getActive().newDoc(fileType, callback);
   }

   public void newDoc(EditableFileType fileType,
                      final String contents,
                      final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      getActive().newDoc(fileType, contents, resultCallback);
   }

   public void disownDoc(String docId)
   {
      SourceColumn column = findByDocument(docId);
      column.closeDoc(docId);
   }

   // When dragging between columns/windows, we need to be specific about which column we're
   // removing the document from as it may exist in more than one column. If the column is null,
   // it is assumed that we are a satellite window and do not have multiple displays.
   public void disownDocOnDrag(String docId, SourceColumn column)
   {
      if (column == null)
      {
         if (getSize() > 1)
            Debug.logWarning("Warning: No column was provided to remove the doc from.");
         column = getActive();
      }
      boolean setNewActiveEditor = false;
      if (column == getActive() && column.getEditors().size() > 1)
         setNewActiveEditor = true;
      
      column.closeDoc(docId);
      column.cancelTabDrag();
      if (setNewActiveEditor)
         column.setActiveEditor();
   }

   public void selectTab(EditingTarget target)
   {
      SourceColumn column = findByDocument(target.getId());
      column.ensureVisible(false);
      column.selectTab(target.asWidget());
   }

   public void closeTabs(JsArrayString ids)
   {
      if (ids != null)
         columnList_.forEach((column) -> column.closeTabs(ids));
   }

   public void closeTabWithPath(String path, boolean interactive)
   {
      EditingTarget target = findEditorByPath(path);
      closeTab(target, interactive);
   }

   public void closeTab(boolean interactive)
   {
      if (hasActiveEditor())
         closeTab(activeColumn_.getActiveEditor(), interactive);
   }

   public void closeTab(EditingTarget target, boolean interactive)
   {
      findByDocument(target.getId()).closeTab(target.asWidget(), interactive, null);
   }

   public void closeTab(EditingTarget target, boolean interactive, Command onClosed)
   {
      findByDocument(target.getId()).closeTab(
         target.asWidget(), interactive, onClosed);
   }
   
   public void closeAllTabs(boolean excludeActive,
                            boolean excludeMain,
                            Command onCompleted)
   {
      columnList_.forEach((column) ->
      {
         closeAllTabs(column, excludeActive, excludeMain, onCompleted);
      });
         
   }

   public void closeAllTabs(SourceColumn column,
                            boolean excludeActive,
                            boolean excludeMain,
                            Command onCompleted)
   {
      if (!excludeMain || !StringUtil.equals(column.getName(), MAIN_SOURCE_NAME))
      {
         final CPSEditingTargetCommand command = (EditingTarget target, Command continuation) ->
         {
            if (excludeActive &&
                  (hasActiveEditor() && target == activeColumn_.getActiveEditor()))
            {
               continuation.execute();
               return;
            }
            else
            {
               column.closeTab(target.asWidget(), false, continuation);
            }
         };
         
         cpsExecuteForEachEditor(column.getEditors(), command, onCompleted);
      }
   }

   void closeSourceDoc(boolean interactive)
   {
      if (activeColumn_.getTabCount() == 0)
         return;

      closeTab(interactive);
   }

   public void saveAllSourceDocs()
   {
      columnList_.forEach((column) -> cpsExecuteForEachEditor(
          column.getEditors(),
          (editingTarget, continuation) -> {
             if (editingTarget.dirtyState().getValue())
             {
                editingTarget.save(continuation);
             }
             else
             {
                continuation.execute();
             }
          }));
   }

   public void revertUnsavedTargets(Command onCompleted)
   {
      ArrayList<EditingTarget> unsavedTargets = new ArrayList<>();
      columnList_.forEach((column) -> unsavedTargets.addAll(
          column.getUnsavedEditors(Source.TYPE_FILE_BACKED, null)));

      // revert all of them
      cpsExecuteForEachEditor(

         // targets the user chose not to save
         unsavedTargets,

         // save each editor
          (saveTarget, continuation) -> {
             if (saveTarget.getPath() != null)
             {
                // file backed document -- revert it
                saveTarget.revertChanges(continuation);
             }
             else
             {
                // untitled document -- just close the tab non-interactively
                closeTab(saveTarget, false, continuation);
             }
          },

         // onCompleted at the end
         onCompleted
      );
   }

   public void closeAllLocalSourceDocs(String caption,
                                       SourceColumn sourceColumn,
                                       Command onCompleted,
                                       final boolean excludeActive)
   {
      // save active editor for exclusion (it changes as we close tabs)
      final EditingTarget excludeEditor = (excludeActive) ? activeColumn_.getActiveEditor() :
         null;

      // collect up a list of dirty documents
      ArrayList<EditingTarget> dirtyTargets = new ArrayList<>();
      // if sourceColumn is not provided, assume we are closing editors for every column
      if (sourceColumn == null)
         columnList_.forEach((column) ->
           dirtyTargets.addAll(column.getDirtyEditors(excludeEditor)));
      else
         dirtyTargets.addAll(sourceColumn.getDirtyEditors(excludeEditor));

      // create a command used to close all tabs
      final Command closeAllTabsCommand = sourceColumn == null
            ? () -> closeAllTabs(excludeActive, false, null)
            : () -> closeAllTabs(sourceColumn, excludeActive, false, null);

      saveEditingTargetsWithPrompt(caption,
         dirtyTargets,
         CommandUtil.join(closeAllTabsCommand, onCompleted),
         null);
   }

   public ArrayList<String> consolidateColumns(int num)
   {
      ArrayList<String> removedColumns = new ArrayList<>();
      if (num >= columnList_.size() || num < 1)
         return removedColumns;

      for (SourceColumn column : columnList_)
      {
         if (!column.hasDoc() && column.getName() != MAIN_SOURCE_NAME)
         {
            removedColumns.add(column.getName());
            closeColumn(column.getName());
            if (num >= columnList_.size() || num == 1)
               break;
         }
      }

      // if we could not remove empty columns to get to the desired amount, consolidate editors
      SourceColumn mainColumn = getByName(MAIN_SOURCE_NAME);
      if (num < columnList_.size())
      {
         ArrayList<SourceColumn> moveColumns = new ArrayList<>(columnList_);
         moveColumns.remove(mainColumn);

         // remove columns from the end of the list first
         int additionalColumnCount = num - 1;
         if (num > 1 && moveColumns.size() != additionalColumnCount)
            moveColumns = new ArrayList<>(
               moveColumns.subList(additionalColumnCount - 1, moveColumns.size() - 1));

         for (SourceColumn column : moveColumns)
         {
            ArrayList<EditingTarget> editors = column.getEditors();
            for (EditingTarget target : editors)
            {
               if (!target.dirtyState().getValue())
               {
                  column.closeTab(target.asWidget(), false);
                  continue;
               }
               server_.getSourceDocument(target.getId(),
                  new ServerRequestCallback<SourceDocument>()
                  {
                     @Override
                     public void onResponseReceived(final SourceDocument doc)
                     {
                        mainColumn.addTab(doc, Source.OPEN_INTERACTIVE);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        globalDisplay_.showErrorMessage("Document Tab Move Failed",
                           "Couldn't move the tab to this window: \n" +
                              error.getMessage());
                     }
                  });
            }
            removedColumns.add(column.getName());
            closeColumn(column, true);
         }
      }

      columnState_ = State.createState(JsUtil.toJsArrayString(getNames(false)),
                                       getActive().getName());
      return removedColumns;
   }

   public void closeColumn(String name)
   {
      SourceColumn column = getByName(name);
      if (column.getTabCount() > 0)
         return;
      if (column == activeColumn_)
         setActive(MAIN_SOURCE_NAME);

      columnList_.remove(column);
      columnState_ = State.createState(JsUtil.toJsArrayString(getNames(false)),
                                       getActive().getName());
   }

   public void closeColumn(SourceColumn column, boolean force)
   {
      if (column.getTabCount() > 0)
      {
         if (!force)
            return;
         else
         {
            for (EditingTarget target : column.getEditors())
               column.closeDoc(target.getId());
         }
      }

      if (column == activeColumn_)
         setActive(MAIN_SOURCE_NAME);
      columnList_.remove(column);
      columnState_ = State.createState(JsUtil.toJsArrayString(getNames(false)),
                                       getActive().getName());
   }

   public void ensureVisible(boolean newTabPending)
   {
      if (getActive() == null)
         return;
      getActive().ensureVisible(newTabPending);
   }

   public void openFile(FileSystemItem file)
   {
      openFile(file, fileTypeRegistry_.getTextTypeForFile(file));
   }

   public void openFile(FileSystemItem file, TextFileType fileType)
   {
      openFile(file,
         fileType,
         new CommandWithArg<EditingTarget>()
         {
            @Override
            public void execute(EditingTarget arg)
            {

            }
         });
   }

   public void openFile(final FileSystemItem file,
                        final TextFileType fileType,
                        final CommandWithArg<EditingTarget> executeOnSuccess)
   {
      // add this work to the queue
      openFileQueue_.add(new OpenFileEntry(file, fileType, executeOnSuccess));

      // begin queue processing if it's the only work in the queue
      if (openFileQueue_.size() == 1)
         processOpenFileQueue();
   }

   public void editFile(String path,
                        ResultCallback<EditingTarget, ServerError> callback)
   {
      server_.ensureFileExists(path, new ErrorLoggingServerRequestCallback<Boolean>()
      {
         @Override
         public void onResponseReceived(Boolean success)
         {
            if (success)
            {
               FileSystemItem file = FileSystemItem.createFile(path);
               openFile(file, callback);
            }
         }
      });
   }

   private void vimEditFile(String path)
   {
      editFile(path, new ResultCallback<EditingTarget, ServerError>() {});
   }
   
   public void openProjectDocs(final Session session, boolean mainColumn)
   {
      if (mainColumn && activeColumn_ != getByName(MAIN_SOURCE_NAME))
         setActive(MAIN_SOURCE_NAME);

      JsArrayString openDocs = session.getSessionInfo().getProjectOpenDocs();
      if (openDocs.length() > 0)
      {
         // set new tab pending for the duration of the continuation
         activeColumn_.incrementNewTabPending();

         // create a continuation for opening the source docs
         SerializedCommandQueue openCommands = new SerializedCommandQueue();

         for (int i = 0; i < openDocs.length(); i++)
         {
            String doc = openDocs.get(i);
            final FileSystemItem fsi = FileSystemItem.createFile(doc);

            openCommands.addCommand(new SerializedCommand()
            {

               @Override
               public void onExecute(final Command continuation)
               {
                  openFile(fsi,
                     fileTypeRegistry_.getTextTypeForFile(fsi),
                     new CommandWithArg<EditingTarget>()
                     {
                        @Override
                        public void execute(EditingTarget arg)
                        {
                           continuation.execute();
                        }
                     });
               }
            });
         }

         // decrement newTabPending and select first tab when done
         openCommands.addCommand(new SerializedCommand()
         {

            @Override
            public void onExecute(Command continuation)
            {
               activeColumn_.decrementNewTabPending();
               onFirstTab();
               continuation.execute();
            }
         });

         // execute the continuation
         openCommands.run();
      }
   }

   public void fireDocTabsChanged()
   {
      activeColumn_.fireDocTabsChanged();
   }

   public void scrollToPosition(FilePosition position, boolean moveCursor)
   {
      // ensure we have an active source column
      getActive();

      if (hasActiveEditor())
      {
         SourcePosition srcPosition = SourcePosition.create(
            position.getLine() - 1,
            position.getColumn() - 1);
         activeColumn_.getActiveEditor().navigateToPosition(
            srcPosition, false, false, moveCursor, null);
      }
   }

   private boolean hasDoc()
   {
      for (SourceColumn column : columnList_)
      {
         if (column.hasDoc())
            return true;
      }
      return false;
   }

   private void vimSetTabIndex(int index)
   {
      int tabCount = activeColumn_.getTabCount();
      if (index >= tabCount)
         return;
      activeColumn_.setPhysicalTabIndex(index);
   }

   private void processOpenFileQueue()
   {
      // no work to do
      if (openFileQueue_.isEmpty())
         return;

      // find the first work unit
      final OpenFileEntry entry = openFileQueue_.peek();

      // define command to advance queue
      final Command processNextEntry = new Command()
      {
         @Override
         public void execute()
         {
            openFileQueue_.remove();
            if (!openFileQueue_.isEmpty())
               processOpenFileQueue();

         }
      };
      openFile(
         entry.file,
         entry.fileType,
         new ResultCallback<EditingTarget, ServerError>()
         {
            @Override
            public void onSuccess(EditingTarget target)
            {
               processNextEntry.execute();
               if (entry.executeOnSuccess != null)
                  entry.executeOnSuccess.execute(target);
            }

            @Override
            public void onCancelled()
            {
               super.onCancelled();
               processNextEntry.execute();
            }

            @Override
            public void onFailure(ServerError error)
            {
               String message = error.getUserMessage();

               // see if a special message was provided
               JSONValue errValue = error.getClientInfo();
               if (errValue != null)
               {
                  JSONString errMsg = errValue.isString();
                  if (errMsg != null)
                     message = errMsg.stringValue();
               }

               globalDisplay_.showMessage(GlobalDisplay.MSG_ERROR,
                  "Error while opening file",
                  message);

               processNextEntry.execute();
            }
         });
   }

   public void openFile(FileSystemItem file,
                        final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      openFile(file, fileTypeRegistry_.getTextTypeForFile(file), resultCallback);
   }

   // top-level wrapper for opening files. takes care of:
   //  - making sure the view is visible
   //  - checking whether it is already open and re-selecting its tab
   //  - prohibit opening very large files (>500KB)
   //  - confirmation of opening large files (>100KB)
   //  - finally, actually opening the file from the server
   //    via the call to the lower level openFile method
   public void openFile(final FileSystemItem file,
                        final TextFileType fileType,
                        final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      activeColumn_.ensureVisible(true);

      if (fileType.isRNotebook())
      {
         openNotebook(file, resultCallback);
         return;
      }

      if (file == null)
      {
         newDoc(fileType, resultCallback);
         return;
      }

      if (openFileAlreadyOpen(file, resultCallback))
         return;

      EditingTarget target = editingTargetSource_.getEditingTarget(fileType);

      if (file.getLength() > target.getFileSizeLimit())
      {
         if (resultCallback != null)
            resultCallback.onCancelled();
         showFileTooLargeWarning(file, target.getFileSizeLimit());
      }
      else if (file.getLength() > target.getLargeFileSize())
      {
         confirmOpenLargeFile(file, new Operation()
         {
            public void execute()
            {
               openFileFromServer(file, fileType, resultCallback);
            }
         }, new Operation()
         {
            public void execute()
            {
               // user (wisely) cancelled
               if (resultCallback != null)
                  resultCallback.onCancelled();
            }
         });
      }
      else
      {
         openFileFromServer(file, fileType, resultCallback);
      }
   }

   public void openNotebook(
      final FileSystemItem rmdFile,
      final SourceDocumentResult doc,
      final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      if (!StringUtil.isNullOrEmpty(doc.getDocPath()))
      {
         // this happens if we created the R Markdown file, or if the R Markdown
         // file on disk matched the one inside the notebook
         openFileFromServer(rmdFile,
            FileTypeRegistry.RMARKDOWN, resultCallback);
      }
      else if (!StringUtil.isNullOrEmpty(doc.getDocId()))
      {
         // this happens when we have to open an untitled buffer for the the
         // notebook (usually because the of a conflict between the Rmd on disk
         // and the one in the .nb.html file)
         server_.getSourceDocument(doc.getDocId(),
            new ServerRequestCallback<SourceDocument>()
            {
               @Override
               public void onResponseReceived(SourceDocument doc)
               {
                  // create the editor
                  EditingTarget target = getActive().addTab(doc, Source.OPEN_INTERACTIVE);

                  // show a warning bar
                  if (target instanceof TextEditingTarget)
                  {
                     ((TextEditingTarget) target).showWarningMessage(
                        "This notebook has the same name as an R Markdown " +
                           "file, but doesn't match it.");
                  }
                  resultCallback.onSuccess(target);
               }

               @Override
               public void onError(ServerError error)
               {
                  globalDisplay_.showErrorMessage(
                     "Notebook Open Failed",
                     "This notebook could not be opened. " +
                        "If the error persists, try removing the " +
                        "accompanying R Markdown file. \n\n" +
                        error.getMessage());
                  resultCallback.onFailure(error);
               }
            });
      }
   }

   public void beforeShow(boolean excludeMain)
   {
      for (SourceColumn column : columnList_)
      {
         if (!excludeMain ||
             !StringUtil.equals(column.getName(), MAIN_SOURCE_NAME))
            column.onBeforeShow();
      }
   }

   public void beforeShow(String name)
   {
      SourceColumn column = getByName(name);
      if (column == null)
      {
         Debug.logWarning("WARNING: Unknown column " + name);
         return;
      }
      column.onBeforeShow();
   }

   public void inEditorForId(String id, OperationWithInput<EditingTarget> onEditorLocated)
   {
      EditingTarget editor = findEditor(id);
      if (editor != null)
         onEditorLocated.execute(editor);
   }

   public void inEditorForPath(String path, OperationWithInput<EditingTarget> onEditorLocated)
   {
      EditingTarget editor = findEditorByPath(path);
      if (editor != null)
         onEditorLocated.execute(editor);
   }

   public void withTarget(String id, CommandWithArg<TextEditingTarget> command)
   {
      withTarget(id, command, null);
   }

   public void withTarget(String id,
                          CommandWithArg<TextEditingTarget> command,
                          Command onFailure)
   {
      EditingTarget target = StringUtil.isNullOrEmpty(id)
         ? activeColumn_.getActiveEditor()
         : findEditor(id);

      if (target == null)
      {
         if (onFailure != null)
            onFailure.execute();
         return;
      }

      if (!(target instanceof TextEditingTarget))
      {
         if (onFailure != null)
            onFailure.execute();
         return;
      }

      command.execute((TextEditingTarget) target);
   }

   public HashSet<AppCommand> getDynamicCommands()
   {
      return dynamicCommands_;
   }

   private void getEditorContext(String id, String path, DocDisplay docDisplay)
   {
      getEditorContext(id, path, docDisplay, server_);
   }

   public static void getEditorContext(String id, String path, DocDisplay docDisplay,
                                       SourceServerOperations server)
   {
      AceEditor editor = (AceEditor) docDisplay;
      Selection selection = editor.getNativeSelection();
      Range[] ranges = selection.getAllRanges();

      // clamp ranges to document boundaries
      for (Range range : ranges)
      {
         Position start = range.getStart();
         start.setRow(MathUtil.clamp(start.getRow(), 0, editor.getRowCount()));
         start.setColumn(MathUtil.clamp(start.getColumn(), 0, editor.getLine(start.getRow()).length()));

         Position end = range.getEnd();
         end.setRow(MathUtil.clamp(end.getRow(), 0, editor.getRowCount()));
         end.setColumn(MathUtil.clamp(end.getColumn(), 0, editor.getLine(end.getRow()).length()));
      }

      JsArray<GetEditorContextEvent.DocumentSelection> docSelections = JavaScriptObject.createArray().cast();
      for (Range range : ranges)
      {
         docSelections.push(GetEditorContextEvent.DocumentSelection.create(
            range,
            editor.getTextForRange(range)));
      }

      id = StringUtil.notNull(id);
      path = StringUtil.notNull(path);

      GetEditorContextEvent.SelectionData data =
         GetEditorContextEvent.SelectionData.create(id, path, editor.getCode(), docSelections);

      server.getEditorContextCompleted(data, new VoidServerRequestCallback());
   }

   private void initDynamicCommands()
   {
      dynamicCommands_ = new HashSet<>();
      dynamicCommands_.add(commands_.saveSourceDoc());
      dynamicCommands_.add(commands_.reopenSourceDocWithEncoding());
      dynamicCommands_.add(commands_.saveSourceDocAs());
      dynamicCommands_.add(commands_.saveSourceDocWithEncoding());
      dynamicCommands_.add(commands_.printSourceDoc());
      dynamicCommands_.add(commands_.vcsFileLog());
      dynamicCommands_.add(commands_.vcsFileDiff());
      dynamicCommands_.add(commands_.vcsFileRevert());
      dynamicCommands_.add(commands_.executeCode());
      dynamicCommands_.add(commands_.executeCodeWithoutFocus());
      dynamicCommands_.add(commands_.executeAllCode());
      dynamicCommands_.add(commands_.executeToCurrentLine());
      dynamicCommands_.add(commands_.executeFromCurrentLine());
      dynamicCommands_.add(commands_.executeCurrentFunction());
      dynamicCommands_.add(commands_.executeCurrentSection());
      dynamicCommands_.add(commands_.executeLastCode());
      dynamicCommands_.add(commands_.insertChunk());
      dynamicCommands_.add(commands_.insertSection());
      dynamicCommands_.add(commands_.executeSetupChunk());
      dynamicCommands_.add(commands_.executePreviousChunks());
      dynamicCommands_.add(commands_.executeSubsequentChunks());
      dynamicCommands_.add(commands_.executeCurrentChunk());
      dynamicCommands_.add(commands_.executeNextChunk());
      dynamicCommands_.add(commands_.previewJS());
      dynamicCommands_.add(commands_.previewSql());
      dynamicCommands_.add(commands_.sourceActiveDocument());
      dynamicCommands_.add(commands_.sourceActiveDocumentWithEcho());
      dynamicCommands_.add(commands_.knitDocument());
      dynamicCommands_.add(commands_.toggleRmdVisualMode());
      dynamicCommands_.add(commands_.enableProsemirrorDevTools());
      dynamicCommands_.add(commands_.previewHTML());
      dynamicCommands_.add(commands_.compilePDF());
      dynamicCommands_.add(commands_.compileNotebook());
      dynamicCommands_.add(commands_.synctexSearch());
      dynamicCommands_.add(commands_.popoutDoc());
      dynamicCommands_.add(commands_.returnDocToMain());
      dynamicCommands_.add(commands_.findReplace());
      dynamicCommands_.add(commands_.findNext());
      dynamicCommands_.add(commands_.findPrevious());
      dynamicCommands_.add(commands_.findFromSelection());
      dynamicCommands_.add(commands_.replaceAndFind());
      dynamicCommands_.add(commands_.extractFunction());
      dynamicCommands_.add(commands_.extractLocalVariable());
      dynamicCommands_.add(commands_.commentUncomment());
      dynamicCommands_.add(commands_.reindent());
      dynamicCommands_.add(commands_.reflowComment());
      dynamicCommands_.add(commands_.jumpTo());
      dynamicCommands_.add(commands_.jumpToMatching());
      dynamicCommands_.add(commands_.goToHelp());
      dynamicCommands_.add(commands_.goToDefinition());
      dynamicCommands_.add(commands_.setWorkingDirToActiveDoc());
      dynamicCommands_.add(commands_.debugDumpContents());
      dynamicCommands_.add(commands_.debugImportDump());
      dynamicCommands_.add(commands_.goToLine());
      dynamicCommands_.add(commands_.checkSpelling());
      dynamicCommands_.add(commands_.wordCount());
      dynamicCommands_.add(commands_.codeCompletion());
      dynamicCommands_.add(commands_.findUsages());
      dynamicCommands_.add(commands_.debugBreakpoint());
      dynamicCommands_.add(commands_.vcsViewOnGitHub());
      dynamicCommands_.add(commands_.vcsBlameOnGitHub());
      dynamicCommands_.add(commands_.editRmdFormatOptions());
      dynamicCommands_.add(commands_.reformatCode());
      dynamicCommands_.add(commands_.showDiagnosticsActiveDocument());
      dynamicCommands_.add(commands_.renameInScope());
      dynamicCommands_.add(commands_.insertRoxygenSkeleton());
      dynamicCommands_.add(commands_.expandSelection());
      dynamicCommands_.add(commands_.shrinkSelection());
      dynamicCommands_.add(commands_.toggleDocumentOutline());
      dynamicCommands_.add(commands_.knitWithParameters());
      dynamicCommands_.add(commands_.clearKnitrCache());
      dynamicCommands_.add(commands_.goToNextSection());
      dynamicCommands_.add(commands_.goToPrevSection());
      dynamicCommands_.add(commands_.goToNextChunk());
      dynamicCommands_.add(commands_.goToPrevChunk());
      dynamicCommands_.add(commands_.profileCode());
      dynamicCommands_.add(commands_.profileCodeWithoutFocus());
      dynamicCommands_.add(commands_.saveProfileAs());
      dynamicCommands_.add(commands_.restartRClearOutput());
      dynamicCommands_.add(commands_.restartRRunAllChunks());
      dynamicCommands_.add(commands_.notebookCollapseAllOutput());
      dynamicCommands_.add(commands_.notebookExpandAllOutput());
      dynamicCommands_.add(commands_.notebookClearOutput());
      dynamicCommands_.add(commands_.notebookClearAllOutput());
      dynamicCommands_.add(commands_.notebookToggleExpansion());
      dynamicCommands_.add(commands_.sendToTerminal());
      dynamicCommands_.add(commands_.openNewTerminalAtEditorLocation());
      dynamicCommands_.add(commands_.sendFilenameToTerminal());
      dynamicCommands_.add(commands_.renameSourceDoc());
      dynamicCommands_.add(commands_.sourceAsLauncherJob());
      dynamicCommands_.add(commands_.sourceAsJob());
      dynamicCommands_.add(commands_.runSelectionAsJob());
      dynamicCommands_.add(commands_.runSelectionAsLauncherJob());
      dynamicCommands_.add(commands_.toggleSoftWrapMode());
      for (AppCommand command : dynamicCommands_)
      {
         command.setVisible(false);
         command.setEnabled(false);
      }
   }

   public void initVimCommands()
   {
      vimCommands_.save(this);
      vimCommands_.selectTabIndex(this);
      vimCommands_.selectNextTab(this);
      vimCommands_.selectPreviousTab(this);
      vimCommands_.closeActiveTab(this);
      vimCommands_.closeAllTabs(this, () -> commands_.activateConsole().execute());
      vimCommands_.createNewDocument(this);
      vimCommands_.saveAndCloseActiveTab(this);
      vimCommands_.readFile(this, userPrefs_.defaultEncoding().getValue());
      vimCommands_.runRScript(this);
      vimCommands_.reflowText(this);
      vimCommands_.showVimHelp(
          RStudioGinjector.INSTANCE.getShortcutViewer());
      vimCommands_.showHelpAtCursor(this);
      vimCommands_.reindent(this);
      vimCommands_.expandShrinkSelection(this);
      vimCommands_.openNextFile(this);
      vimCommands_.openPreviousFile(this);
      vimCommands_.addStarRegister();
   }

   public SourceAppCommand getSourceCommand(AppCommand command, SourceColumn column)
   {
      // check if we've already create a SourceAppCommand for this command
      String key = command.getId() + column.getName();
       if (sourceAppCommands_.get(key) != null)
         return sourceAppCommands_.get(key);

      // if not found, create it
      SourceAppCommand sourceCommand =
         new SourceAppCommand(command, column.getName(), this);
      sourceAppCommands_.put(key, sourceCommand);
      return sourceCommand;
   }

   private void openNotebook(final FileSystemItem rnbFile,
                             final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      // construct path to .Rmd
      final String rnbPath = rnbFile.getPath();
      final String rmdPath = FilePathUtils.filePathSansExtension(rnbPath) + ".Rmd";
      final FileSystemItem rmdFile = FileSystemItem.createFile(rmdPath);

      // if we already have associated .Rmd file open, then just edit it
      // TODO: should we perform conflict resolution here as well?
      if (openFileAlreadyOpen(rmdFile, resultCallback))
         return;

      // ask the server to extract the .Rmd, then open that
      Command extractRmdCommand = new Command()
      {
         @Override
         public void execute()
         {
            server_.extractRmdFromNotebook(
                  rnbPath,
                  new ServerRequestCallback<SourceDocumentResult>()
                  {
                     @Override
                     public void onResponseReceived(SourceDocumentResult doc)
                     {
                        openNotebook(rmdFile, doc, resultCallback);
                     }

                     @Override
                     public void onError(ServerError error)
                     {
                        globalDisplay_.showErrorMessage("Notebook Open Failed",
                              "This notebook could not be opened. \n\n" +
                              error.getMessage());
                        resultCallback.onFailure(error);
                     }
                  });
         }
      };

      dependencyManager_.withRMarkdown("R Notebook", "Using R Notebooks", extractRmdCommand);
   }

   private void openFileFromServer(
         final FileSystemItem file,
         final TextFileType fileType,
         final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      final Command dismissProgress = globalDisplay_.showProgress(
                                                         "Opening file...");

      server_.openDocument(
            file.getPath(),
            fileType.getTypeId(),
            userPrefs_.defaultEncoding().getValue(),
            new ServerRequestCallback<SourceDocument>()
            {
               @Override
               public void onError(ServerError error)
               {
                  dismissProgress.execute();
                  pMruList_.get().remove(file.getPath());
                  Debug.logError(error);
                  if (resultCallback != null)
                     resultCallback.onFailure(error);
               }

               @Override
               public void onResponseReceived(SourceDocument document)
               {
                  // apply (dynamic) doc property defaults
                  SourceColumn.applyDocPropertyDefaults(document, false, userPrefs_);

                  // if we are opening for a source navigation then we
                  // need to force Rmds into source mode
                  if (openingForSourceNavigation_)
                  {
                     document.getProperties().setString(
                       TextEditingTarget.RMD_VISUAL_MODE,
                       DocUpdateSentinel.PROPERTY_FALSE
                     );
                  }

                  dismissProgress.execute();
                  pMruList_.get().add(document.getPath());
                  EditingTarget target = getActive().addTab(document, Source.OPEN_INTERACTIVE);
                  if (resultCallback != null)
                     resultCallback.onSuccess(target);
               }
            });
   }

   private boolean openFileAlreadyOpen(final FileSystemItem file,
                                       final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      for (SourceColumn column : columnList_)
      {
         // check to see if any local editors have the file open
         for (int i = 0; i < column.getEditors().size(); i++)
         {
            EditingTarget target = column.getEditors().get(i);
            String thisPath = target.getPath();
            if (thisPath != null
                && thisPath.equalsIgnoreCase(file.getPath()))
            {
               column.selectTab(target.asWidget());
               pMruList_.get().add(thisPath);
               if (resultCallback != null)
                  resultCallback.onSuccess(target);
               return true;
            }
         }
      }
      return false;
   }

   private void showFileTooLargeWarning(FileSystemItem file,
                                        long sizeLimit)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("The file '" + file.getName() + "' is too ");
      msg.append("large to open in the source editor (the file is ");
      msg.append(StringUtil.formatFileSize(file.getLength()) + " and the ");
      msg.append("maximum file size is ");
      msg.append(StringUtil.formatFileSize(sizeLimit) + ")");

      globalDisplay_.showMessage(GlobalDisplay.MSG_WARNING,
                                 "Selected File Too Large",
                                 msg.toString());
   }

   private void confirmOpenLargeFile(FileSystemItem file,
                                     Operation openOperation,
                                     Operation noOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("The source file '" + file.getName() + "' is large (");
      msg.append(StringUtil.formatFileSize(file.getLength()) + ") ");
      msg.append("and may take some time to open. ");
      msg.append("Are you sure you want to continue opening it?");
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                                      "Confirm Open",
                                      msg.toString(),
                                      false, // Don't include cancel
                                      openOperation,
                                      noOperation,
                                      false);   // 'No' is default
   }

   private void saveEditingTargetsWithPrompt(
      String title,
      ArrayList<EditingTarget> editingTargets,
      final Command onCompleted,
      final Command onCancelled)
   {
      // execute on completed right away if the list is empty
      if (editingTargets.size() ==  0)
      {
         onCompleted.execute();
      }

      // if there is just one thing dirty then go straight to the save dialog
      else if (editingTargets.size() == 1)
      {
         editingTargets.get(0).saveWithPrompt(onCompleted, onCancelled);
      }

      // otherwise use the multi save changes dialog
      else
      {
         // convert to UnsavedChangesTarget collection
         ArrayList<UnsavedChangesTarget> unsavedTargets =
            new ArrayList<>(editingTargets);

         // show dialog
         showUnsavedChangesDialog(
            title,
            unsavedTargets,
            new OperationWithInput<UnsavedChangesDialog.Result>()
            {
               @Override
               public void execute(UnsavedChangesDialog.Result result)
               {
                  saveChanges(result.getSaveTargets(), onCompleted);
               }
            },
            onCancelled);
      }
   }

   public ArrayList<UnsavedChangesTarget> getUnsavedChanges(int type, Set<String> ids)
   {
      ArrayList<UnsavedChangesTarget> targets = new ArrayList<>();
      columnList_.forEach((column) -> targets.addAll(column.getUnsavedEditors(type, ids)));
      return targets;
   }

   public void saveChanges(ArrayList<UnsavedChangesTarget> targets,
                            Command onCompleted)
   {
      // convert back to editing targets
      ArrayList<EditingTarget> saveTargets = new ArrayList<>();
      for (UnsavedChangesTarget target: targets)
      {
         EditingTarget saveTarget =
            findEditor(target.getId());
         if (saveTarget != null)
            saveTargets.add(saveTarget);
      }

      CPSEditingTargetCommand saveCommand =
         new CPSEditingTargetCommand()
         {
            @Override
            public void execute(EditingTarget saveTarget,
                                Command continuation)
            {
               saveTarget.save(continuation);
            }
         };

      // execute the save
      cpsExecuteForEachEditor(

         // targets the user chose to save
         saveTargets,

         // save each editor
         saveCommand,

         // onCompleted at the end
         onCompleted
      );
   }

   private void pasteFileContentsAtCursor(final String path, final String encoding)
   {
      if (activeColumn_ == null)
         return;

      EditingTarget activeEditor = activeColumn_.getActiveEditor();
      if (activeEditor instanceof TextEditingTarget)
      {
         final TextEditingTarget target = (TextEditingTarget) activeEditor;
         server_.getFileContents(path, encoding, new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String content)
            {
               target.insertCode(content, false);
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
   }

   private void pasteRCodeExecutionResult(final String code)
   {
      server_.executeRCode(code, new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String output)
         {
            if (hasActiveEditor() &&
                activeColumn_.getActiveEditor() instanceof TextEditingTarget)
            {
               TextEditingTarget editor = (TextEditingTarget) activeColumn_.getActiveEditor();
               editor.insertCode(output, false);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private void reflowText()
   {
      if (hasActiveEditor() &&
          activeColumn_.getActiveEditor() instanceof TextEditingTarget)
      {
         TextEditingTarget editor = (TextEditingTarget) activeColumn_.getActiveEditor();
         editor.reflowText();
      }
   }

   private void reindent()
   {
      if (hasActiveEditor() &&
          activeColumn_.getActiveEditor() instanceof TextEditingTarget)
      {
         TextEditingTarget editor = (TextEditingTarget) activeColumn_.getActiveEditor();
         editor.getDocDisplay().reindent();
      }
   }

   private void saveActiveSourceDoc()
   {
      if (hasActiveEditor() &&
          activeColumn_.getActiveEditor() instanceof TextEditingTarget)
      {
         TextEditingTarget target = (TextEditingTarget) activeColumn_.getActiveEditor();
         target.save();
      }
   }

   private void saveAndCloseActiveSourceDoc()
   {
      if (hasActiveEditor() &&
          activeColumn_.getActiveEditor() instanceof TextEditingTarget)
      {
         TextEditingTarget target = (TextEditingTarget) activeColumn_.getActiveEditor();
         target.save(new Command()
         {
            @Override
            public void execute()
            {
               onCloseSourceDoc();
            }
         });
      }
   }

   private void revertActiveDocument()
   {
      if (!hasActiveEditor())
         return;

      if (activeColumn_.getActiveEditor().getPath() != null)
         activeColumn_.getActiveEditor().revertChanges(null);

      // Ensure that the document is in view
      activeColumn_.getActiveEditor().ensureCursorVisible();
   }

   private void showHelpAtCursor()
   {
      if (hasActiveEditor() &&
          activeColumn_.getActiveEditor() instanceof TextEditingTarget)
      {
         TextEditingTarget editor = (TextEditingTarget) activeColumn_.getActiveEditor();
         editor.showHelpAtCursor();
      }
   }

   /**
    * Execute the given command for each editor, using continuation-passing
    * style. When executed, the CPSEditingTargetCommand needs to execute its
    * own Command parameter to continue the iteration.
    * @param command The command to run on each EditingTarget
    */
   private void cpsExecuteForEachEditor(ArrayList<EditingTarget> editors,
                                       final CPSEditingTargetCommand command,
                                       final Command completedCommand)
   {
      SerializedCommandQueue queue = new SerializedCommandQueue();

      // Clone editors_, since the original may be mutated during iteration
      for (final EditingTarget editor : new ArrayList<>(editors))
      {
         queue.addCommand(new SerializedCommand()
         {
            @Override
            public void onExecute(Command continuation)
            {
               command.execute(editor, continuation);
            }
         });
      }

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

   public SourceColumn getByName(String name)
   {
      for (SourceColumn column : columnList_)
      {
         if (StringUtil.equals(column.getName(), name))
            return column;
      }
      return null;
   }

   private boolean contains(String name)
   {
     for (SourceColumn column : columnList_)
     {
        if (StringUtil.equals(column.getName(), name))
           return true;
     }
     return false;
   }

   private void cpsExecuteForEachEditor(ArrayList<EditingTarget> editors,
                                       final CPSEditingTargetCommand command)
   {
      cpsExecuteForEachEditor(editors, command, null);
   }

   private static class OpenFileEntry
   {
      public OpenFileEntry(FileSystemItem fileIn, TextFileType fileTypeIn,
            CommandWithArg<EditingTarget> executeIn)
      {
         file = fileIn;
         fileType = fileTypeIn;
         executeOnSuccess = executeIn;
      }
      public final FileSystemItem file;
      public final TextFileType fileType;
      public final CommandWithArg<EditingTarget> executeOnSuccess;
   }

   private String computeAccessibleName()
   {
      return "Source Column " + sourceColumnCounter_++;
   }

   private State columnState_;
   private SourceColumn activeColumn_;

   private boolean openingForSourceNavigation_ = false;
   private boolean docsRestored_ = false;

   private final Queue<OpenFileEntry> openFileQueue_ = new LinkedList<>();
   private final ArrayList<SourceColumn> columnList_ = new ArrayList<>();
   private HashSet<AppCommand> dynamicCommands_ = new HashSet<>();
   private final HashMap<String, SourceAppCommand> sourceAppCommands_ = new HashMap<>();
   private SourceVimCommands vimCommands_;

   private Commands commands_;
   private EventBus events_;
   private Provider<FileMRUList> pMruList_;
   private SourceWindowManager windowManager_;
   private Session session_;
   private Synctex synctex_;
   private UserPrefs userPrefs_;
   private UserState userState_;
   private GlobalDisplay globalDisplay_;
   private TextEditingTargetRMarkdownHelper rmarkdown_;
   private EditingTargetSource editingTargetSource_;
   private FileTypeRegistry fileTypeRegistry_;

   private SourceServerOperations server_;
   private DependencyManager dependencyManager_;

   private final SourceNavigationHistory sourceNavigationHistory_ =
       new SourceNavigationHistory(30);

   public final static String COLUMN_PREFIX = "Source";
   public final static String MAIN_SOURCE_NAME = COLUMN_PREFIX;
   static int sourceColumnCounter_ = 1;
}
