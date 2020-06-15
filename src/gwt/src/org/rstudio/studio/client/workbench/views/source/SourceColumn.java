/*
 * SourceColumn.java
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.ResultCallback;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.DocTabSelectionEvent;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.synctex.events.SynctexStatusChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SourceColumn implements SelectionHandler<Integer>,
                                     TabClosingHandler,
                                     TabCloseHandler,
                                     TabClosedHandler,
                                     TabReorderHandler
{
   interface Binder extends CommandBinder<Commands, SourceColumn>
   {
   }

   SourceColumn()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(Binder binder,
                          Commands commands,
                          EventBus events,
                          EditingTargetSource editingTargetSource,
                          RemoteFileSystemContext fileContext,
                          SourceServerOperations sourceServerOperations)
   {
      commands_ = commands;
      binder.bind(commands_, this);

      events_ = events;
      editingTargetSource_ = editingTargetSource;
      fileContext_ = fileContext;
      server_ = sourceServerOperations;


      events_.addHandler(FileTypeChangedEvent.TYPE, event -> manageCommands(false));
      events_.addHandler(SourceOnSaveChangedEvent.TYPE, event -> manageSaveCommands());
      events_.addHandler(SynctexStatusChangedEvent.TYPE, event -> manageSynctexCommands());

      initialized_ = true;
   }

   public void loadDisplay(String name,
                           Source.Display display,
                           SourceColumnManager manager)
   {
      name_ = name;
      display_ = display;
      manager_ = manager;

      display_.addSelectionHandler(this);
      display_.addTabClosingHandler(this);
      display_.addTabCloseHandler(this);
      display_.addTabClosedHandler(this);
      display_.addTabReorderHandler(this);

      ensureVisible(false);
   }

   public String getName()
   {
      return name_;
   }

   public EditingTarget getActiveEditor()
   {
      return activeEditor_;
   }

   public ArrayList<EditingTarget> getEditors()
   {
      return editors_;
   }

   public ArrayList<EditingTarget> getDirtyEditors(final EditingTarget excludeEditor)
   {
      ArrayList<EditingTarget> dirtyEditors = new ArrayList<>();
      for (EditingTarget target : editors_)
      {
         if (excludeEditor != null && target == excludeEditor)
            continue;
         if (target.dirtyState().getValue())
            dirtyEditors.add(target);
      }
      return dirtyEditors;
   }

   public ArrayList<EditingTarget> getUnsavedEditors(int type, Set<String> ids)
   {
      ArrayList<EditingTarget> unsavedEditors = new ArrayList<>();
      for (EditingTarget target : editors_)
      {
         if (!isUnsavedTarget(target, type))
            continue;
         if (ids != null && !ids.contains(target.getId()))
            continue;
         unsavedEditors.add(target);
      }
      return unsavedEditors;
   }

   public ArrayList<Integer> getTabOrder()
   {
      return tabOrder_;
   }

   public Widget asWidget()
   {
      return display_.asWidget();
   }

   // Display wrapper methods
   public void cancelTabDrag()
   {
      display_.cancelTabDrag();
   }
   
   public void closeTab(Widget child, boolean interactive)
   {
      display_.closeTab(child, interactive);
   }

   public void closeTab(Widget child, boolean interactive, Command onClosed)
   {
      display_.closeTab(child, interactive, onClosed);
   }
   
   public void closeTab(int index, boolean interactive)
   {
      display_.closeTab(index, interactive);
   }

   public void closeTab(int index, boolean interactive, Command onClosed)
   {
	   display_.closeTab(index, interactive, onClosed);
   }

   public int getTabCount()
   {
      return display_.getTabCount();
   }
   
   public void manageChevronVisibility()
   {
	   display_.manageChevronVisibility();
   }
   
   public void moveTab(int index, int delta)
   {
	   display_.moveTab(index, delta);
   }
   
   public void selectTab(Widget widget)
   {
	   display_.selectTab(widget);
   }
   
   public void showOverflowPopout()
   {
	   display_.showOverflowPopup();
   }
   
   public void showUnsavedChangesDialog(
         String title,
         ArrayList<UnsavedChangesTarget> dirtyTargets,
         OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
         Command onCancelled)
   {
      display_.showUnsavedChangesDialog(title, dirtyTargets, saveOperation, onCancelled);
   }
   
   public void initialSelect(int index)
   {
      if (index >= 0 && display_.getTabCount() > index)
         display_.selectTab(index);
      if (display_.getTabCount() > 0 && display_.getActiveTabIndex() >= 0)
         editors_.get(index).onInitiallyLoaded();
   }

   /**
    * @param isNewTabPending True if a new tab is about to be created. (If
    *    false and there are no tabs already, then a new source doc might
    *    be created to make sure we don't end up with a source pane showing
    *    with no tabs in it.)
    */
   public void ensureVisible(boolean isNewTabPending)
   {
      newTabPending_++;
      try
      {
         display_.ensureVisible();
      }
      finally
      {
         newTabPending_--;
      }
   }

   // when tabs have been reordered in the session, the physical layout of the
   // tabs doesn't match the logical order of editors_. it's occasionally
   // necessary to get or set the tabs by their physical order.
   public int getPhysicalTabIndex()
   {
      int idx = display_.getActiveTabIndex();
      if (idx < tabOrder_.size())
      {
         idx = tabOrder_.indexOf(idx);
      }
      return idx;
   }

   public void setPhysicalTabIndex(int idx)
   {
      if (idx < tabOrder_.size())
      {
         idx = tabOrder_.get(idx);
      }
      display_.selectTab(idx);
   }

   private void onActivate(EditingTarget target)
   {
       // return if we're already set properly
       if (activeEditor_ != null && activeEditor_ == target)
          return;

       // deactivate prior active editor
       if (activeEditor_ != null)
          activeEditor_.onDeactivate();

       // set and active editor
       activeEditor_ = target;
       if (activeEditor_ != null)
          activeEditor_.onActivate();
       manageCommands();
   }

   void setActiveEditor()
   {
       if (activeEditor_ == null &&
           editors_.size() > display_.getActiveTabIndex())
          onActivate(editors_.get(display_.getActiveTabIndex()));
   }

   EditingTarget setActiveEditor(String docId)
   {
      if (StringUtil.isNullOrEmpty(docId) &&
          activeEditor_ != null)
      {
         activeEditor_.onDeactivate();
         activeEditor_ = null;
         return null;
      }

      for (EditingTarget target : editors_)
      {
         if (target.getId().equals(docId))
         {
             onActivate(target);
            return target;
         }
      }
      return null;
   }

   public void setActiveEditor(EditingTarget target)
   {
      // This should never happen
      if (!editors_.contains(target))
      {
         Debug.logWarning("Attempting to set active editor to an unknown target.");
         return;
      }
      onActivate(target);
   }

   private void syncTabOrder()
   {
      // ensure the tab order is synced to the list of editors
      for (int i = tabOrder_.size(); i < editors_.size(); i++)
      {
         tabOrder_.add(i);
      }
      for (int i = editors_.size(); i < tabOrder_.size(); i++)
      {
         tabOrder_.remove(i);
      }
   }

   public void fireDocTabsChanged()
   {
      if (!initialized_)
         return;

      // ensure we have a tab order (we want the popup list to match the order
      // of the tabs)
      syncTabOrder();

      ArrayList<EditingTarget> editors = editors_;
      String[] ids = new String[editors.size()];
      FileIcon[] icons = new FileIcon[editors.size()];
      String[] names = new String[editors.size()];
      String[] paths = new String[editors.size()];
      for (int i = 0; i < ids.length; i++)
      {
         EditingTarget target = editors.get(getTabOrder().get(i));
         ids[i] = target.getId();
         icons[i] = target.getIcon();
         names[i] = target.getName().getValue();
         paths[i] = target.getPath();
      }

      String activeId = (activeEditor_ != null)
            ? activeEditor_.getId()
            : null;

      events_.fireEvent(new DocTabsChangedEvent(activeId, ids, icons, names, paths));

      manageChevronVisibility();
   }

   public boolean hasDoc(String docId)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(docId, target.getId()))
            return true;
      }
      return false;
   }

   public boolean hasDoc()
   {
      return editors_.size() > 0;
   }

   public boolean isSaveCommandActive()
   {
      for (EditingTarget target : editors_)
      {
         if (target.isSaveCommandActive())
            return true;
      }
      return false;
   }

   public EditingTarget getDoc(String docId)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(docId, target.getId()))
            return target;
      }
      return null;
   }

   public boolean hasDocWithPath(String path)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(path, target.getPath()))
            return true;
      }
      return false;
   }

   public EditingTarget getEditorWithPath(String path)
   {
      for (EditingTarget target : editors_)
      {
         if (StringUtil.equals(path, target.getPath()))
            return target;
      }
      return null;
   }

   private Widget createWidget(EditingTarget target)
   {
      return target.asWidget();
   }
   
   public EditingTarget addTab(SourceDocument doc, int mode)
   {
      return addTab(doc, false, mode);
   }
   
   public EditingTarget addTab(SourceDocument doc, boolean atEnd,
         int mode)
   {
      // by default, add at the tab immediately after the current tab
      return addTab(doc, atEnd ? null : getPhysicalTabIndex() + 1,
            mode);
   }

   public EditingTarget addTab(SourceDocument doc, Integer position, int mode)
   {
      final String defaultNamePrefix = editingTargetSource_.getDefaultNamePrefix(doc);
      final EditingTarget target = editingTargetSource_.getEditingTarget(
            doc, fileContext_, () -> getNextDefaultName(defaultNamePrefix));
      final Widget widget = createWidget(target);

      if (position == null)
      {
         editors_.add(target);
      }
      else
      {
         // we're inserting into an existing permuted tabset -- push aside
         // any tabs physically to the right of this tab
         editors_.add(position, target);
         for (int i = 0; i < tabOrder_.size(); i++)
         {
            int pos = tabOrder_.get(i);
            if (pos >= position)
               tabOrder_.set(i, pos + 1);
         }

         // add this tab in its "natural" position
         tabOrder_.add(position, position);
      }

      display_.addTab(widget,
                      target.getIcon(),
                      target.getId(),
                      target.getName().getValue(),
                      target.getTabTooltip(), // used as tooltip, if non-null
                      position,
                      true);
      fireDocTabsChanged();

      target.getName().addValueChangeHandler(event -> {
         display_.renameTab(widget,
                            target.getIcon(),
                            event.getValue(),
                            target.getPath());
         fireDocTabsChanged();
      });

      display_.setDirty(widget, target.dirtyState().getValue());
      target.dirtyState().addValueChangeHandler(event -> {
         display_.setDirty(widget, event.getValue());
         manageCommands(false);
      });

      target.addEnsureVisibleHandler(event -> display_.selectTab(widget));

      target.addCloseHandler(voidCloseEvent -> closeTab(widget, false));

      events_.fireEvent(new SourceDocAddedEvent(doc, mode, getName()));

      if (target instanceof TextEditingTarget && doc.isReadOnly())
      {
         ((TextEditingTarget) target).setIntendedAsReadOnly(
            JsUtil.toList(doc.getReadOnlyAlternatives()));
      }

      // adding a tab may enable commands that are only available when
      // multiple documents are open; if this is the second document, go check
      if (editors_.size() == 2)
         manageMultiTabCommands();

      // if the target had an editing session active, attempt to resume it
      if (doc.getCollabParams() != null)
         target.beginCollabSession(doc.getCollabParams());

      return target;
   }

   public void addTab(Widget widget,
                      FileIcon icon,
                      String id,
                      String value,
                      String tabTooltip,
                      Integer position,
                      boolean switchToTab)
   {
      display_.addTab(widget, icon, id, value, tabTooltip, position, switchToTab);
   }

   public void closeDoc(String docId)
   {
      suspendDocumentClose_ = true;
      for (int i = 0; i < editors_.size(); i++)
      {
         if (editors_.get(i).getId().equals(docId))
         {
            display_.closeTab(i, false);
            break;
         }
      }
      suspendDocumentClose_ = false;
   }

   public void closeTabs(JsArrayString ids)
   {
      for (EditingTarget target : editors_)
      {
         if (JsArrayUtil.jsArrayStringContains(ids, target.getId()))
         {
            closeTab(target.asWidget(), false /* non interactive */);
         }
      }
   }

   public void setPendingDebugSelection()
   {
      if (!isDebugSelectionPending())
      {
         debugSelectionTimer_ = new Timer()
         {
            public void run()
            {
               debugSelectionTimer_ = null;
            }
         };
         debugSelectionTimer_.schedule(250);
      }
   }

   public boolean insertCode(String code, boolean isBlock)
   {
      if (activeEditor_ != null &&
          activeEditor_ instanceof TextEditingTarget &&
          commands_.executeCode().isEnabled())
      {
         TextEditingTarget textEditor = (TextEditingTarget) activeEditor_;
         textEditor.insertCode(code, isBlock);
         return true;
      }
      return false;
   }

   public void incrementNewTabPending()
   {
       newTabPending_++;
   }

   public void decrementNewTabPending()
   {
      newTabPending_--;
   }

   public int getUntitledNum(String prefix)
   {
      int max = 0;
      for (EditingTarget target : editors_)
      {
         String name = target.getName().getValue();
         max = Math.max(max, manager_.getUntitledNum(name, prefix));
      }
      return max;
   }

   private String getNextDefaultName(String defaultNamePrefix)
   {
      if (StringUtil.isNullOrEmpty(defaultNamePrefix))
         defaultNamePrefix = "Untitled";

      int max = manager_.getUntitledNum(defaultNamePrefix);
      return defaultNamePrefix + (max + 1);
   }

   private boolean isDebugSelectionPending()
   {
      return debugSelectionTimer_ != null;
   }

   private boolean isUnsavedTarget(EditingTarget target, int type)
   {
      boolean fileBacked = target.getPath() != null;
      return target.dirtyState().getValue() &&
         ((type == Source.TYPE_FILE_BACKED &&  fileBacked) ||
            (type == Source.TYPE_UNTITLED    && !fileBacked));
   }

   private void clearPendingDebugSelection()
   {
      if (debugSelectionTimer_ != null)
      {
         debugSelectionTimer_.cancel();
         debugSelectionTimer_ = null;
      }
   }

   public void manageCommands(boolean forceSync)
   {
      boolean hasDocs = hasDoc();

      commands_.newSourceDoc().setEnabled(true);
      commands_.closeSourceDoc().setEnabled(hasDocs);
      commands_.closeAllSourceDocs().setEnabled(hasDocs);
      commands_.nextTab().setEnabled(hasDocs);
      commands_.previousTab().setEnabled(hasDocs);
      commands_.firstTab().setEnabled(hasDocs);
      commands_.lastTab().setEnabled(hasDocs);
      commands_.switchToTab().setEnabled(hasDocs);
      commands_.setWorkingDirToActiveDoc().setEnabled(hasDocs);

      HashSet<AppCommand> newCommands = activeEditor_ != null
              ? activeEditor_.getSupportedCommands()
              : new HashSet<>();

      if (forceSync)
      {
         for (AppCommand command : activeCommands_)
         {
            command.setEnabled(false);
            command.setVisible(false);
         }

         for (AppCommand command : newCommands)
         {
            command.setEnabled(true);
            command.setVisible(true);
         }
      }
      else
      {
         HashSet<AppCommand> commandsToEnable = new HashSet<>(newCommands);
         commandsToEnable.removeAll(activeCommands_);

         HashSet<AppCommand> commandsToDisable = new HashSet<>(activeCommands_);
         commandsToDisable.removeAll(newCommands);

         for (AppCommand command : commandsToEnable)
         {
            command.setEnabled(true);
            command.setVisible(true);
         }

         for (AppCommand command : commandsToDisable)
         {
            command.setEnabled(false);
            command.setVisible(false);
         }
      }

      // commands which should always be visible even when disabled
      commands_.saveSourceDoc().setVisible(true);
      commands_.saveSourceDocAs().setVisible(true);
      commands_.printSourceDoc().setVisible(true);
      commands_.setWorkingDirToActiveDoc().setVisible(true);
      commands_.debugBreakpoint().setVisible(true);

      // manage synctex commands
      manageSynctexCommands();

      // manage vcs commands
      manageVcsCommands();

      // manage save and save all
      manageSaveCommands();

      // manage source navigation
      manageSourceNavigationCommands();

      // manage RSConnect commands
      manageRSConnectCommands();

      // manage R Markdown commands
      manageRMarkdownCommands();

      // manage multi-tab commands
      manageMultiTabCommands();

      manageTerminalCommands();

      activeCommands_ = newCommands;

      // give the active editor a chance to manage commands
      if (activeEditor_ != null)
         activeEditor_.manageCommands();

      assert verifyNoUnsupportedCommands(newCommands)
              : "Unsupported commands detected (please add to SourceColumnManager.getDynamicCommands())";
   }

   private void manageSynctexCommands()
   {
      // synctex commands are enabled if we have synctex for the active editor
      boolean synctexAvailable = manager_.getSynctex().isSynctexAvailable();
      if (synctexAvailable)
      {
         if ((activeEditor_ != null) &&
                 (activeEditor_.getPath() != null) &&
                 activeEditor_.canCompilePdf())
         {
            synctexAvailable = manager_.getSynctex().isSynctexAvailable();
         }
         else
         {
            synctexAvailable = false;
         }
      }

      manager_.getSynctex().enableCommands(synctexAvailable);
   }

   private void manageVcsCommands()
   {
      // manage availability of vcs commands
      boolean vcsCommandsEnabled =
              manager_.getSession().getSessionInfo().isVcsEnabled() &&
                      (activeEditor_ != null) &&
                      (activeEditor_.getPath() != null) &&
                      activeEditor_.getPath().startsWith(
                              manager_.getSession().getSessionInfo().getActiveProjectDir().getPath());

      commands_.vcsFileLog().setVisible(vcsCommandsEnabled);
      commands_.vcsFileLog().setEnabled(vcsCommandsEnabled);
      commands_.vcsFileDiff().setVisible(vcsCommandsEnabled);
      commands_.vcsFileDiff().setEnabled(vcsCommandsEnabled);
      commands_.vcsFileRevert().setVisible(vcsCommandsEnabled);
      commands_.vcsFileRevert().setEnabled(vcsCommandsEnabled);

      if (vcsCommandsEnabled)
      {
         String name = FileSystemItem.getNameFromPath(activeEditor_.getPath());
         commands_.vcsFileDiff().setMenuLabel("_Diff \"" + name + "\"");
         commands_.vcsFileLog().setMenuLabel("_Log of \"" + name +"\"");

         commands_.vcsFileRevert().setMenuLabel("_Revert \"" + name + "\"...");
      }

      boolean isGithubRepo = manager_.getSession().getSessionInfo().isGithubRepository();
      if (vcsCommandsEnabled && isGithubRepo)
      {
         String name = FileSystemItem.getNameFromPath(activeEditor_.getPath());

         commands_.vcsViewOnGitHub().setVisible(true);
         commands_.vcsViewOnGitHub().setEnabled(true);
         commands_.vcsViewOnGitHub().setMenuLabel(
                 "_View \"" + name + "\" on GitHub");

         commands_.vcsBlameOnGitHub().setVisible(true);
         commands_.vcsBlameOnGitHub().setEnabled(true);
         commands_.vcsBlameOnGitHub().setMenuLabel(
                 "_Blame \"" + name + "\" on GitHub");
      }
      else
      {
         commands_.vcsViewOnGitHub().setVisible(false);
         commands_.vcsViewOnGitHub().setEnabled(false);
         commands_.vcsBlameOnGitHub().setVisible(false);
         commands_.vcsBlameOnGitHub().setEnabled(false);
      }
   }

   public void manageSaveCommands()
   {
      boolean saveEnabled = (activeEditor_ != null) &&
              activeEditor_.isSaveCommandActive();
      commands_.saveSourceDoc().setEnabled(saveEnabled);
      manageSaveAllCommand();
   }

   private void manageSaveAllCommand()
   {
      // if source windows are open, managing state of the command becomes
      // complicated, so leave it enabled
      if (manager_.areSourceWindowsOpen())
      {
         commands_.saveAllSourceDocs().setEnabled(true);
         return;
      }

      // if one document is dirty then we are enabled
      commands_.saveAllSourceDocs().setEnabled(isSaveCommandActive());
   }

   void manageSourceNavigationCommands()
   {
      commands_.sourceNavigateBack().setEnabled(
              manager_.getSourceNavigationHistory().isBackEnabled());

      commands_.sourceNavigateForward().setEnabled(
              manager_.getSourceNavigationHistory().isForwardEnabled());
   }

   private void manageRSConnectCommands()
   {
      boolean rsCommandsAvailable =
              SessionUtils.showPublishUi(manager_.getSession(), manager_.getUserState()) &&
                      (activeEditor_ != null) &&
                      (activeEditor_.getPath() != null) &&
                      ((activeEditor_.getExtendedFileType() != null &&
                              activeEditor_.getExtendedFileType() .startsWith(SourceDocument.XT_SHINY_PREFIX)) ||
                              activeEditor_.getExtendedFileType() == SourceDocument.XT_RMARKDOWN ||
                              activeEditor_.getExtendedFileType() == SourceDocument.XT_PLUMBER_API);
      commands_.rsconnectDeploy().setVisible(rsCommandsAvailable);
      if (activeEditor_ != null)
      {
         String deployLabel = null;
         if (activeEditor_.getExtendedFileType() != null)
         {
            if (activeEditor_.getExtendedFileType().startsWith(SourceDocument.XT_SHINY_PREFIX))
            {
               deployLabel = "Publish Application...";
            }
            else if (activeEditor_.getExtendedFileType() == SourceDocument.XT_PLUMBER_API)
            {
               deployLabel = "Publish Plumber API...";
            }
         }
         if (deployLabel == null)
            deployLabel = "Publish Document...";

         commands_.rsconnectDeploy().setLabel(deployLabel);
      }
      commands_.rsconnectConfigure().setVisible(rsCommandsAvailable);
   }

   private void manageRMarkdownCommands()
   {
      boolean rmdCommandsAvailable =
              manager_.getSession().getSessionInfo().getRMarkdownPackageAvailable() &&
                      (activeEditor_ != null) &&
                      activeEditor_.getExtendedFileType() == SourceDocument.XT_RMARKDOWN;
      commands_.editRmdFormatOptions().setVisible(rmdCommandsAvailable);
      commands_.editRmdFormatOptions().setEnabled(rmdCommandsAvailable);
   }

   public void manageMultiTabCommands()
   {
      boolean hasMultipleDocs = hasDoc();

      // special case--these editing targets always support popout, but it's
      // nonsensical to show it if it's the only tab in a satellite; hide it in
      // this case
      if (commands_.popoutDoc().isEnabled() &&
              activeEditor_ != null &&
              (activeEditor_ instanceof TextEditingTarget ||
                      activeEditor_ instanceof CodeBrowserEditingTarget) &&
              !SourceWindowManager.isMainSourceWindow())
      {
         commands_.popoutDoc().setVisible(hasMultipleDocs);
      }

      commands_.closeOtherSourceDocs().setEnabled(hasMultipleDocs);
   }

   private void manageTerminalCommands()
   {
      if (!manager_.getSession().getSessionInfo().getAllowShell())
         commands_.sendToTerminal().setVisible(false);
   }

   private boolean verifyNoUnsupportedCommands(HashSet<AppCommand> commands)
   {
      HashSet<AppCommand> temp = new HashSet<>(commands);
      temp.removeAll(manager_.getDynamicCommands());
      return temp.size() == 0;
   }

   private void manageCommands()
   {
      manageCommands(false);
   }
   
   public void newDoc(EditableFileType fileType,
                      ResultCallback<EditingTarget, ServerError> callback)
   {
      ensureVisible(true);
      if (fileType instanceof TextFileType)
      {
         // This is a text file, so see if the user has defined a template for it.
         TextFileType textType = (TextFileType)fileType;
         server_.getSourceTemplate("",
               "default" + textType.getDefaultExtension(),
               new ServerRequestCallback<String>()
               {
                  @Override
                  public void onResponseReceived(String template)
                  {
                     // Create a new document with the supplied template.
                     newDoc(fileType, template, callback);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     // Ignore errors; there's just not a template for this type.
                     newDoc(fileType, null, callback);
                  }
               });
      }
      else
      {
         newDoc(fileType, null, callback);
      }
   }

   public void newDoc(EditableFileType fileType,
                      final String contents,
                      final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      ensureVisible(true);
      server_.newDocument(
            fileType.getTypeId(),
            contents,
            JsObject.createJsObject(),
            new SimpleRequestCallback<SourceDocument>(
                  "Error Creating New Document")
            {
               @Override
               public void onResponseReceived(SourceDocument newDoc)
               {
                  EditingTarget target =
                     addTab(newDoc, Source.OPEN_INTERACTIVE);

                  if (contents != null)
                  {
                     target.forceSaveCommandActive();
                     manageSaveCommands();
                  }

                  if (resultCallback != null)
                     resultCallback.onSuccess(target);
               }

               @Override
               public void onError(ServerError error)
               {
                  if (resultCallback != null)
                     resultCallback.onFailure(error);
               }
            });
   }
   // event handlers

   public void onBeforeShow()
   {
      if (getTabCount() == 0 && newTabPending_ == 0)
      {
         // Avoid scenarios where the Source tab comes up but no tabs are
         // in it. (But also avoid creating an extra source tab when there
         // were already new tabs about to be created!)
         newDoc(FileTypeRegistry.R, null);
      }
   }

   public void onSelection(SelectionEvent<Integer> event)
   {
      if (activeEditor_ != null)
         activeEditor_.onDeactivate();

      activeEditor_ = null;

      if (event.getSelectedItem() >= 0)
      {
         activeEditor_ = editors_.get(event.getSelectedItem());
         activeEditor_.onActivate();
         manager_.setActive(getName());

         // let any listeners know this tab was activated
         events_.fireEvent(new DocTabActivatedEvent(
               activeEditor_.getPath(),
               activeEditor_.getId()));

         // don't send focus to the tab if we're expecting a debug selection
         // event
         if (initialized_ && !isDebugSelectionPending())
         {
            Scheduler.get().scheduleDeferred(() -> {
               // presume that we will give focus to the tab
               boolean focus = true;

               if (event instanceof DocTabSelectionEvent)
               {
                  // however, if this event was generated from a doc tab
                  // selection that did not have focus, don't steal focus
                  DocTabSelectionEvent tabEvent = (DocTabSelectionEvent) event;
                  focus = tabEvent.getFocus();
               }

               if (focus && activeEditor_ != null)
                  activeEditor_.focus();
            });
         }
         else if (isDebugSelectionPending())
         {
            // we're debugging, so send focus to the console instead of the
            // editor
            //commands_.activateConsole().execute();
            clearPendingDebugSelection();
         }
      }

      manageCommands();
   }

   @Override
   public void onTabClosing(final TabClosingEvent event)
   {
      EditingTarget target = editors_.get(event.getTabIndex());
      if (!target.onBeforeDismiss())
         event.cancel();
   }
 
   @Override
   public void onTabClose(TabCloseEvent event)
   {
      // can't proceed if there is no active editor or display
      if (activeEditor_ == null)
         return;

      if (event.getTabIndex() >= editors_.size())
         return; // Seems like this should never happen...?

      final String activeEditorId = activeEditor_.getId();

      if (editors_.get(event.getTabIndex()).getId() == activeEditorId)
      {
         // scan the source navigation history for an entry that can
         // be used as the next active tab (anything that doesn't have
         // the same document id as the currently active tab)
         SourceNavigation srcNav = manager_.getSourceNavigationHistory().scanBack(
                 navigation -> navigation.getDocumentId() != activeEditorId);

         // see if the source navigation we found corresponds to an active
         // tab -- if it does then set this on the event
         if (srcNav != null)
         {
            for (int i=0; i<editors_.size(); i++)
            {
               if (srcNav.getDocumentId() == editors_.get(i).getId())
               {
                  display_.selectTab(i);
                  break;
               }
            }
         }
      }
   }

   @Override
   public void onTabClosed(TabClosedEvent event)
   {
      closeTabIndex(event.getTabIndex(), !suspendDocumentClose_);
   }

   @Override
   public void onTabReorder(TabReorderEvent event)
   {
      syncTabOrder();

      // sanity check: make sure we're moving from a valid location and to a
      // valid location
      if (event.getOldPos() < 0 || event.getOldPos() >= tabOrder_.size() ||
          event.getNewPos() < 0 || event.getNewPos() >= tabOrder_.size())
      {
         return;
      }

      // remove the tab from its old position
      int idx = tabOrder_.get(event.getOldPos());
      tabOrder_.remove(new Integer(idx));  // force type box

      // add it to its new position
      tabOrder_.add(event.getNewPos(), idx);

      // sort the document IDs and send to the server
      ArrayList<String> ids = new ArrayList<>();
      for (Integer integer : tabOrder_)
      {
         ids.add(editors_.get(integer).getId());
      }
      server_.setDocOrder(ids, new VoidServerRequestCallback());

      // activate the tab
      setPhysicalTabIndex(event.getNewPos());

      syncTabOrder();
      fireDocTabsChanged();
   }

   private void closeTabIndex(int idx, boolean closeDocument)
   {
      EditingTarget target = editors_.remove(idx);

      tabOrder_.remove(new Integer(idx));
      for (int i = 0; i < tabOrder_.size(); i++)
      {
         if (tabOrder_.get(i) > idx)
         {
            tabOrder_.set(i, tabOrder_.get(i) - 1);
         }
      }

      target.onDismiss(closeDocument ? EditingTarget.DISMISS_TYPE_CLOSE :
         EditingTarget.DISMISS_TYPE_MOVE);
      
      if (activeEditor_ == target)
      {
         activeEditor_.onDeactivate();
         activeEditor_ = null;
      }

      if (closeDocument)
      {
         events_.fireEvent(new DocTabClosedEvent(target.getId()));
         server_.closeDocument(target.getId(),
                               new VoidServerRequestCallback());
      }

      manageCommands(false);
      fireDocTabsChanged();

      if (display_.getTabCount() == 0)
      {
         manager_.clearSourceNavigationHistory();
         events_.fireEvent(new LastSourceDocClosedEvent(getName()));
      }
   }

   private Commands commands_;

   private boolean initialized_;
   private boolean suspendDocumentClose_ = false;

   // If positive, a new tab is about to be created
   private int newTabPending_;

   private String name_;
   private Source.Display display_;
   private EditingTarget activeEditor_;
   private final ArrayList<EditingTarget> editors_ = new ArrayList<>();
   private final ArrayList<Integer> tabOrder_ = new ArrayList<>();
   private HashSet<AppCommand> activeCommands_ = new HashSet<>();

   private RemoteFileSystemContext fileContext_;
   private SourceServerOperations server_;
   private Timer debugSelectionTimer_ = null;
   private EventBus events_;
   private EditingTargetSource editingTargetSource_;
   
   private SourceColumnManager manager_;

}
