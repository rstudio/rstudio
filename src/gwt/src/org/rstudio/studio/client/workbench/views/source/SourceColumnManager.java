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
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.rstudio.core.client.*;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.filetypes.*;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.rmarkdown.model.RmdChosenTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplateData;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.FileMRUList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.ObjectExplorerEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetRMarkdownHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRMarkdownDialog;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class SourceColumnManager implements SourceExtendedTypeDetectedEvent.Handler
{
   public interface CPSEditingTargetCommand
   {
      void execute(EditingTarget editingTarget, Command continuation);
   }

   @Inject
   public SourceColumnManager(Source.Display display,
                              SourceServerOperations server,
                              GlobalDisplay globalDisplay,
                              Commands commands,
                              EditingTargetSource editingTargetSource,
                              FileTypeRegistry fileTypeRegistry,
                              EventBus events,
                              DependencyManager dependencyManager,
                              Session session,
                              Synctex synctex,
                              UserPrefs userPrefs,
                              UserState userState,
                              Provider<FileMRUList> pMruList,
                              Provider<SourceWindowManager> pWindowManager)
   {
      SourceColumn column = GWT.create(SourceColumn.class);
      column.loadDisplay(Source.COLUMN_PREFIX, display, this);
      columnMap_.put(column.getName(), column);
      setActive(column.getName());

      server_ = server;
      commands_ = commands;
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
      pWindowManager_ = pWindowManager;

      rmarkdown_ = new TextEditingTargetRMarkdownHelper();
      initializeDynamicCommands();

      events_.addHandler(SourceExtendedTypeDetectedEvent.TYPE, this);

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

      sourceNavigationHistory_.addChangeHandler(new ChangeHandler()
      {

         @Override
         public void onChange(ChangeEvent event)
         {
            columnMap_.forEach((name, column) ->
            {
               column.manageSourceNavigationCommands();
            });
         }
      });
   }

   public String add()
   {
      Source.Display display = GWT.create(SourcePane.class);
      return add(display, false);
   }

   public String add(Source.Display display)
   {
      return add(display, false);
   }

   public String add(Source.Display display, boolean activate)
   {
      SourceColumn column = GWT.create(SourceColumn.class);
      column.loadDisplay(Source.COLUMN_PREFIX + StringUtil.makeRandomId(12),
              display,
              this);
      columnMap_.put(column.getName(), column);

      if (activate || activeColumn_ == null)
         activeColumn_ = column;

      return column.getName();
   }

   public void initialSelect(int index)
   {
      activeColumn_.initialSelect(index);
   }

   public void setActive(String name)
   {
      if (StringUtil.isNullOrEmpty(name))
      {
         activeColumn_ = null;
         activeColumn_.setActiveEditor(new String());
         return;
      }

      String prevColumn = activeColumn_ == null ? "" : activeColumn_.getName();
      activeColumn_ = columnMap_.get(name);

      // If the active column changed, we need to update the active editor
      if (!StringUtil.isNullOrEmpty(prevColumn) && !StringUtil.equals(name, prevColumn))
      {
         SourceColumn column = columnMap_.get(prevColumn);
         column.setActiveEditor("");
         if (activeColumn_.getActiveEditor() == null)
         {
            Debug.logWarning("Setting to random editor.");
            column.setActiveEditor();
         }
      }
   }

   public void setActive(EditingTarget target)
   {
      activeColumn_ = findByDocument(target.getId());
      activeColumn_.setActiveEditor(target);
   }

   public void setActiveDocId(String docId)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = entry.getValue();
         EditingTarget target = column.setActiveEditor(docId);
         if (target != null)
         {
            setActive(target);
            return;
         }
      }
      Debug.logWarning("Attempted to set unknown doc to active " + docId);
   }

   public void setOpeningForSourceNavigation(boolean value)
   {
      openingForSourceNavigation_ = value;
   }

   public void activateColumns(final Command afterActivation)
   {
      if (activeColumn_.getActiveEditor() == null)
      {
         if (activeColumn_ == null)
            setActive(Source.COLUMN_PREFIX);
         newDoc(FileTypeRegistry.R, new ResultCallback<EditingTarget, ServerError>()
         {
            @Override
            public void onSuccess(EditingTarget target)
            {
               setActive(target);
               doActivateSource(afterActivation);
            }
         });
      } else
      {
         doActivateSource(afterActivation);
      }
   }

   public SourceColumn getActive()
   {
      if (activeColumn_ != null)
         return activeColumn_;

      if (activeColumn_.getActiveEditor() != null)
      {
         setActive(findByDocument(activeColumn_.getActiveEditor().getId()).getName());
         return activeColumn_;
      } else
         setActive(Source.COLUMN_PREFIX);
      return activeColumn_;
   }

   public int getTabCount()
   {
      return activeColumn_.getTabCount();
   }

   public int getPhysicalTabIndex()
   {
      return activeColumn_.getPhysicalTabIndex();
   }

   public ArrayList<Widget> getWidgets(boolean excludeMain)
   {

      ArrayList<Widget> result = new ArrayList<Widget>();
      columnMap_.forEach((name, column) -> {
         if (!excludeMain || !StringUtil.equals(name, Source.COLUMN_PREFIX))
            result.add(column.asWidget());
      });
      return result;
   }

   public HashMap<String, SourceColumn> getMap()
   {
      return columnMap_;
   }

   public Widget getWidget(String name)
   {
      return columnMap_.get(name).asWidget();
   }

   public Session getSession()
   {
      return session_;
   }

   public SourceNavigationHistory getSourceNavigationHistory()
   {
      return sourceNavigationHistory_;
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
      return columnMap_.size();
   }

   public int getUntitledNum(String prefix)
   {
      AtomicInteger max = new AtomicInteger();
      columnMap_.forEach((name, column) -> {
         max.set(Math.max(max.get(), column.getUntitledNum(prefix)));
      });
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
      columnMap_.forEach((name, column) -> {
         column.manageCommands(forceSync);
      });
   }

   // !!! HOW TO HANDLE THESE?
   public EditingTarget addTab(SourceDocument doc, int mode, SourceColumn column)
   {
      if (column == null)
         column = activeColumn_;
      return column.addTab(doc, mode);
   }

   public EditingTarget addTab(SourceDocument doc, boolean atEnd,
                               int mode, SourceColumn column)
   {
      if (column == null)
         column = activeColumn_;
      return column.addTab(doc, atEnd, mode);
   }

   public EditingTarget addTab(SourceDocument doc, Integer position,
                               int mode, SourceColumn column)
   {
      if (column == null)
         column = activeColumn_;
      return column.addTab(doc, position, mode);
   }

   public EditingTarget findEditor(String docId)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn) entry.getValue();
         EditingTarget target = column.getDoc(docId);
         if (target != null)
            return target;
      }
      return null;
   }

   public EditingTarget findEditorByPath(String path)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn) entry.getValue();
         EditingTarget target = column.getEditorWithPath(path);
         if (target != null)
            return target;
      }
      return null;
   }

   public SourceColumn findByDocument(String docId)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn) entry.getValue();
         if (column.hasDoc(docId))
            return column;
      }
      return null;
   }

   public SourceColumn findByPath(String path)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn) entry.getValue();
         if (column.hasDocWithPath(path))
            return column;
      }
      return null;
   }

   public SourceColumn findByName(String name)
   {
      return columnMap_.get(name);
   }

   public SourceColumn findByPosition(int x)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn) entry.getValue();

         Widget w = column.asWidget();
         int left = w.getAbsoluteLeft();
         int right = w.getAbsoluteLeft() + w.getOffsetWidth();

         if (x > left && x < right)
            return column;
      }
      return null;
   }

   public boolean isEmpty(String name)
   {
      return columnMap_.get(name).getTabCount() == 0;
   }

   public boolean areSourceWindowsOpen()
   {
      return pWindowManager_.get().areSourceWindowsOpen();
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
              activeColumn_.getActiveEditor() != null &&
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
      columnMap_.forEach((name, column) -> {
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
                    activeColumn_.addTab(response, Source.OPEN_INTERACTIVE);
                 }
              });
   }

   public void showOverflowPopout()
   {
      ensureVisible(false);
      activeColumn_.showOverflowPopout();
   }

   public void showDataItem(DataItem data)
   {
      columnMap_.forEach((name, column) -> {
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
      });

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
                    activeColumn_.addTab(response, Source.OPEN_INTERACTIVE);
                 }
              });
   }

   public void showUnsavedChangesDialog(
           String title,
           ArrayList<UnsavedChangesTarget> dirtyTargets,
           OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
           Command onCancelled)
   {
      activeColumn_.showUnsavedChangesDialog(title, dirtyTargets, saveOperation, onCancelled);
   }

   @Handler
   public void onMoveTabRight()
   {
      activeColumn_.moveTab(activeColumn_.getPhysicalTabIndex(), 1);
   }

   @Handler
   public void onMoveTabLeft()
   {
      activeColumn_.moveTab(activeColumn_.getPhysicalTabIndex(), -1);
   }

   @Handler
   public void onMoveTabToFirst()
   {
      activeColumn_.moveTab(activeColumn_.getPhysicalTabIndex(),
              activeColumn_.getPhysicalTabIndex() * -1);
   }

   @Handler
   public void onMoveTabToLast()
   {
      activeColumn_.moveTab(activeColumn_.getPhysicalTabIndex(),
              (activeColumn_.getTabCount() -
                      activeColumn_.getPhysicalTabIndex()) - 1);
   }

   @Handler
   public void onSwitchToTab()
   {
      if (activeColumn_.getTabCount() == 0)
         return;
      showOverflowPopout();
   }

   @Handler
   public void onFirstTab()
   {
      if (activeColumn_.getTabCount() == 0)
         return;

      ensureVisible(false);
      if (activeColumn_.getTabCount() > 0)
         activeColumn_.setPhysicalTabIndex(0);
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
      if (activeColumn_.getTabCount() == 0)
         return;

      activeColumn_.ensureVisible(false);
      if (activeColumn_.getTabCount() > 0)
         activeColumn_.setPhysicalTabIndex(activeColumn_.getTabCount() - 1);
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
      if (activeColumn_.getTabCount() == 0)
         return;

      activeColumn_.ensureVisible(false);

      int targetIndex = activeColumn_.getPhysicalTabIndex() + delta;
      if (targetIndex > (activeColumn_.getTabCount() - 1))
      {
         if (wrap)
            targetIndex = 0;
         else
            return;
      } else if (targetIndex < 0)
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
      activeColumn_.ensureVisible(false);
      if (activeColumn_.getActiveEditor() != null)
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
                    } else if (result.isNewDocument())
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
                    } else
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
                    } else
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
      activeColumn_.setPendingDebugSelection();
   }

   private EditingTarget selectTabWithDocPath(String path)
   {
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn) entry.getValue();
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
      activeColumn_.newDoc(fileType, callback);
   }

   public void newDoc(EditableFileType fileType,
                      final String contents,
                      final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      activeColumn_.newDoc(fileType, contents, resultCallback);
   }

   public void disownDoc(String docId)
   {
      findByDocument(docId).closeTab(docId);
   }

   public void selectTab(EditingTarget target)
   {
      findByDocument(target.getId()).selectTab(target.asWidget());
   }

   public void closeTabs(JsArrayString ids)
   {
      if (ids != null)
      {
         columnMap_.forEach((name, column) -> {
            column.closeTabs(ids);
         });
      }
   }

   public void closeTabWithPath(String path, boolean interactive)
   {
      EditingTarget target = findEditorByPath(path);
      closeTab(target, interactive);
   }

   public void closeTab(boolean interactive)
   {
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

   public void closeAllTabs(boolean excludeActive, boolean excludeMain)
   {
      columnMap_.forEach((name, column) -> {
         if (!excludeMain || name != Source.COLUMN_PREFIX)
         {
            cpsExecuteForEachEditor(column.getEditors(),
                    new CPSEditingTargetCommand()
                    {
                       @Override
                       public void execute(EditingTarget target, Command continuation)
                       {
                          if (excludeActive && target == activeColumn_.getActiveEditor())
                          {
                             continuation.execute();
                             return;
                          } else
                          {
                             column.closeTab(target.asWidget(), false, continuation);
                          }
                       }
                    });
         }
      });
   }

   public ArrayList<Widget> consolidateColumns(int num)
   {
      // We are only removing the column from the column manager's knowledge.
      // Its widget still needs to be removed from the display so we return the widgets to be removed.
      ArrayList<Widget> result = new ArrayList<>();
      if (num >= columnMap_.size() || num < 1)
         return result;

      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = entry.getValue();
         if (!column.hasDoc())
         {
            if (column == activeColumn_)
               setActive("");
            result.add(column.asWidget());
            columnMap_.remove(column.getName());
            if (num >= columnMap_.size())
               break;
         }
      }

      ArrayList<EditingTarget> moveEditors = new ArrayList<>();
      // if we could not remove empty columns to get to the desired amount, consolidate editors
      for (Map.Entry<String, SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = entry.getValue();
         if (!StringUtil.equals(column.getName(), Source.COLUMN_PREFIX))
         {
            moveEditors.addAll(column.getEditors());
            column.closeAllLocalSourceDocs();
            closeColumn(column.getName());
            if (columnMap_.size() >= num)
               break;
         }
      }

      SourceColumn column = columnMap_.get(Source.COLUMN_PREFIX);
      for (EditingTarget target : moveEditors)
      {
         column.addTab(
                 target.asWidget(),
                 target.getIcon(),
                 target.getId(),
                 target.getName().getValue(),
                 target.getTabTooltip(), // used as tooltip, if non-null
                 null,
                 true);
      }

      return result;
   }

   public void closeColumn(String name)
   {
      SourceColumn column = findByName(name);
      if (column.getTabCount() > 0)
         return;
      if (column == activeColumn_)
         setActive("");

      columnMap_.remove(name);
   }

   public void ensureVisible(boolean newTabPending)
   {
      activeColumn_.ensureVisible(newTabPending);
   }

   public void manageChevronVisibility()
   {
      columnMap_.forEach((name, column) -> {
         column.manageChevronVisibility();
      });
   }

   public static boolean isMainColumn(SourceColumn column)
   {
      if (StringUtil.equals(column.getName(), Source.COLUMN_PREFIX))
         return true;
      return false;
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

   private void editFile(final String path)
   {
      server_.ensureFileExists(
              path,
              new ServerRequestCallback<Boolean>()
              {
                 @Override
                 public void onResponseReceived(Boolean success)
                 {
                    if (success)
                    {
                       FileSystemItem file = FileSystemItem.createFile(path);
                       openFile(file);
                    }
                 }

                 @Override
                 public void onError(ServerError error)
                 {
                    Debug.logError(error);
                 }
              });
   }

   public void openProjectDocs(final Session session, boolean mainColumn)
   {
      if (mainColumn && activeColumn_ != columnMap_.get(Source.COLUMN_PREFIX))
         setActive(Source.COLUMN_PREFIX);

      JsArrayString openDocs = session.getSessionInfo().getProjectOpenDocs();
      if (openDocs.length() > 0)
      {
         // set new tab pending for the duration of the continuation
          activeColumn_.incrementNewTabPending();

         // create a continuation for opening the source docs
         SerializedCommandQueue openCommands = new SerializedCommandQueue();

         for (int i=0; i<openDocs.length(); i++)
         {
            String doc = openDocs.get(i);
            final FileSystemItem fsi = FileSystemItem.createFile(doc);

            openCommands.addCommand(new SerializedCommand() {

               @Override
               public void onExecute(final Command continuation)
               {
                  openFile(fsi,
                           fileTypeRegistry_.getTextTypeForFile(fsi),
                           new CommandWithArg<EditingTarget>() {
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
         openCommands.addCommand(new SerializedCommand() {

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
   
   private boolean hasDoc()
   {
      for (Map.Entry<String,SourceColumn> column : columnMap_.entrySet())
      {
         if (column.getValue().hasDoc())
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
            new ResultCallback<EditingTarget, ServerError>() {
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
         openNotebook(file, fileType, resultCallback);
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
         confirmOpenLargeFile(file, new Operation() {
            public void execute()
            {
               openFileFromServer(file, fileType, resultCallback);
            }
         }, new Operation() {
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
               EditingTarget target = activeColumn_.addTab(doc, Source.OPEN_INTERACTIVE);

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

   public void beforeShow()
   {
      columnMap_.forEach((name, column) -> {
         column.onBeforeShow();
      });
   }

   public void beforeShow(String name)
   {
      SourceColumn column = findByName(name);
      if (column == null) {
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

   public HashSet<AppCommand> getDynamicCommands()
   {
      return dynamicCommands_;
   }

   private void initializeDynamicCommands()
   {
      dynamicCommands_ = new HashSet<AppCommand>();
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

   private void openNotebook(final FileSystemItem rnbFile,
                             final TextFileType fileType,
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
                  // if we are opening for a source navigation then we
                  // need to force Rmds into source mode
                  if (openingForSourceNavigation_)
                  {
                     document.getProperties()._setBoolean(
                        TextEditingTarget.RMD_VISUAL_MODE,
                        false
                     );
                  }

                  dismissProgress.execute();
                  pMruList_.get().add(document.getPath());
                  EditingTarget target = activeColumn_.addTab(document, Source.OPEN_INTERACTIVE);
                  if (resultCallback != null)
                     resultCallback.onSuccess(target);
               }
            });
   }

   private boolean openFileAlreadyOpen(final FileSystemItem file,
                                       final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      for (Map.Entry<String,SourceColumn> entry : columnMap_.entrySet())
      {
         SourceColumn column = (SourceColumn)entry.getValue();
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
      };
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
   /**
    * Execute the given command for each editor, using continuation-passing
    * style. When executed, the CPSEditingTargetCommand needs to execute its
    * own Command parameter to continue the iteration.
    * @param command The command to run on each EditingTarget
    */
   public void cpsExecuteForEachEditor(ArrayList<EditingTarget> editors,
                                       final CPSEditingTargetCommand command,
                                       final Command completedCommand)
   {
      SerializedCommandQueue queue = new SerializedCommandQueue();

      // Clone editors_, since the original may be mutated during iteration
      for (final EditingTarget editor : new ArrayList<EditingTarget>(editors))
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
   
   public void cpsExecuteForEachEditor(ArrayList<EditingTarget> editors,
                                       final CPSEditingTargetCommand command)
   {
      cpsExecuteForEachEditor(editors, command, null);
   }

   private class OpenFileEntry
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

   private Commands commands_;
   private SourceColumn activeColumn_;

   private boolean openingForSourceNavigation_ = false;

   private final Queue<OpenFileEntry> openFileQueue_ = new LinkedList<OpenFileEntry>();
   private HashMap<String,SourceColumn> columnMap_ = new HashMap<String,SourceColumn>();
   private HashSet<AppCommand> dynamicCommands_ = new HashSet<AppCommand>();

   private final SourceNavigationHistory sourceNavigationHistory_ =
      new SourceNavigationHistory(30);

   private final EventBus events_;
   private final Provider<FileMRUList> pMruList_;
   private final Provider<SourceWindowManager> pWindowManager_;
   private final Session session_;
   private final Synctex synctex_;
   private final UserPrefs userPrefs_;
   private final UserState userState_;
   private final GlobalDisplay globalDisplay_;
   private final TextEditingTargetRMarkdownHelper rmarkdown_;
   private final EditingTargetSource editingTargetSource_;
   private final FileTypeRegistry fileTypeRegistry_;

   private final SourceServerOperations server_;
   private DependencyManager dependencyManager_;
}
