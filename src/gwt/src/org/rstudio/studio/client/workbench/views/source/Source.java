/*
 * Source.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.*;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenPresentationSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent.NavigationMethod;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileHandler;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.rnw.RnwWeaveRegistry;
import org.rstudio.studio.client.common.synctex.Synctex;
import org.rstudio.studio.client.common.synctex.events.SynctexStatusChangedEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.FileMRUList;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.data.events.ViewDataEvent;
import org.rstudio.studio.client.workbench.views.data.events.ViewDataHandler;
import org.rstudio.studio.client.workbench.views.output.find.events.FindInFilesEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.codebrowser.CodeBrowserEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.ProfilerEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.profiler.model.ProfilerContents;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetPresentationHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.SourceOnSaveChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.NewRdDialog;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.RdShellResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigationHistory;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashSet;

public class Source implements InsertSourceHandler,
                               IsWidget,
                             OpenSourceFileHandler,
                             TabClosingHandler,
                             TabCloseHandler,
                             SelectionHandler<Integer>,
                             TabClosedHandler,
                             FileEditHandler,
                             ShowContentHandler,
                             ShowDataHandler,
                             CodeBrowserNavigationHandler,
                             CodeBrowserFinishedHandler,
                             BeforeShowHandler
{
   public interface Display extends IsWidget,
                                    HasTabClosingHandlers,
                                    HasTabCloseHandlers,
                                    HasTabClosedHandlers,
                                    HasBeforeSelectionHandlers<Integer>,
                                    HasSelectionHandlers<Integer>
   {
      void addTab(Widget widget,
                  ImageResource icon,
                  String name,
                  String tooltip,
                  boolean switchToTab);
      void selectTab(int tabIndex);
      void selectTab(Widget widget);
      int getTabCount();
      int getActiveTabIndex();
      void closeTab(Widget widget, boolean interactive);
      void closeTab(Widget widget, boolean interactive, Command onClosed);
      void closeTab(int index, boolean interactive);
      void closeTab(int index, boolean interactive, Command onClosed);
      void setDirty(Widget widget, boolean dirty);
      void manageChevronVisibility();
      void showOverflowPopup();
      
      void showUnsavedChangesDialog(
            String title,
            ArrayList<UnsavedChangesTarget> dirtyTargets,
            OperationWithInput<UnsavedChangesDialog.Result> saveOperation,
            Command onCancelled);

      void ensureVisible();

      void renameTab(Widget child,
                     ImageResource icon,
                     String value,
                     String tooltip);

      HandlerRegistration addBeforeShowHandler(BeforeShowHandler handler);
   }

   public interface CPSEditingTargetCommand
   {
      void execute(EditingTarget editingTarget, Command continuation);
   }

   @Inject
   public Source(Commands commands,
                 Display view,
                 SourceServerOperations server,
                 EditingTargetSource editingTargetSource,
                 FileTypeRegistry fileTypeRegistry,
                 GlobalDisplay globalDisplay,
                 FileDialogs fileDialogs,
                 RemoteFileSystemContext fileContext,
                 EventBus events,
                 final Session session,
                 Synctex synctex,
                 WorkbenchContext workbenchContext,
                 Provider<FileMRUList> pMruList,
                 UIPrefs uiPrefs,
                 RnwWeaveRegistry rnwWeaveRegistry)
   {
      commands_ = commands;
      view_ = view;
      server_ = server;
      editingTargetSource_ = editingTargetSource;
      fileTypeRegistry_ = fileTypeRegistry;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileContext_ = fileContext;
      events_ = events;
      session_ = session;
      synctex_ = synctex;
      workbenchContext_ = workbenchContext;
      pMruList_ = pMruList;
      uiPrefs_ = uiPrefs;
      rnwWeaveRegistry_ = rnwWeaveRegistry;

      view_.addTabClosingHandler(this);
      view_.addTabCloseHandler(this);
      view_.addTabClosedHandler(this);
      view_.addSelectionHandler(this);
      view_.addBeforeShowHandler(this);

      dynamicCommands_ = new HashSet<AppCommand>();
      dynamicCommands_.add(commands.saveSourceDoc());
      dynamicCommands_.add(commands.reopenSourceDocWithEncoding());
      dynamicCommands_.add(commands.saveSourceDocAs());
      dynamicCommands_.add(commands.saveSourceDocWithEncoding());
      dynamicCommands_.add(commands.printSourceDoc());
      dynamicCommands_.add(commands.vcsFileLog());
      dynamicCommands_.add(commands.vcsFileDiff());
      dynamicCommands_.add(commands.vcsFileRevert());
      dynamicCommands_.add(commands.executeCode());
      dynamicCommands_.add(commands.executeCodeWithoutFocus());
      dynamicCommands_.add(commands.executeAllCode());
      dynamicCommands_.add(commands.executeToCurrentLine());
      dynamicCommands_.add(commands.executeFromCurrentLine());
      dynamicCommands_.add(commands.executeCurrentFunction());
      dynamicCommands_.add(commands.executeLastCode());
      dynamicCommands_.add(commands.insertChunk());
      dynamicCommands_.add(commands.insertSection());
      dynamicCommands_.add(commands.executeCurrentChunk());
      dynamicCommands_.add(commands.executeNextChunk());
      dynamicCommands_.add(commands.sourceActiveDocument());
      dynamicCommands_.add(commands.sourceActiveDocumentWithEcho());
      dynamicCommands_.add(commands.markdownHelp());
      dynamicCommands_.add(commands.usingRMarkdownHelp());
      dynamicCommands_.add(commands.authoringRPresentationsHelp());
      dynamicCommands_.add(commands.knitToHTML());
      dynamicCommands_.add(commands.previewHTML());
      dynamicCommands_.add(commands.compilePDF());
      dynamicCommands_.add(commands.compileNotebook());
      dynamicCommands_.add(commands.synctexSearch());
      dynamicCommands_.add(commands.popoutDoc());
      dynamicCommands_.add(commands.findReplace());
      dynamicCommands_.add(commands.findNext());
      dynamicCommands_.add(commands.findPrevious());
      dynamicCommands_.add(commands.replaceAndFind());
      dynamicCommands_.add(commands.extractFunction());
      dynamicCommands_.add(commands.commentUncomment());
      dynamicCommands_.add(commands.reindent());
      dynamicCommands_.add(commands.reflowComment());
      dynamicCommands_.add(commands.jumpTo());
      dynamicCommands_.add(commands.jumpToMatching());
      dynamicCommands_.add(commands.goToHelp());
      dynamicCommands_.add(commands.goToFunctionDefinition());
      dynamicCommands_.add(commands.setWorkingDirToActiveDoc());
      dynamicCommands_.add(commands.debugDumpContents());
      dynamicCommands_.add(commands.debugImportDump());
      dynamicCommands_.add(commands.goToLine());
      dynamicCommands_.add(commands.checkSpelling());
      dynamicCommands_.add(commands.codeCompletion());
      dynamicCommands_.add(commands.rcppHelp());
      dynamicCommands_.add(commands.debugBreakpoint());
      for (AppCommand command : dynamicCommands_)
      {
         command.setVisible(false);
         command.setEnabled(false);
      }
      
      // fake shortcuts for commands which we handle at a lower level
      commands.goToHelp().setShortcut(new KeyboardShortcut(112));
      commands.goToFunctionDefinition().setShortcut(new KeyboardShortcut(113));
      commands.codeCompletion().setShortcut(
                                    new KeyboardShortcut(KeyCodes.KEY_TAB));
             
      events.addHandler(ShowContentEvent.TYPE, this);
      events.addHandler(ShowDataEvent.TYPE, this);

      events.addHandler(ViewDataEvent.TYPE, new ViewDataHandler()
      {
         public void onViewData(ViewDataEvent event)
         {
            server_.newDocument(
                  FileTypeRegistry.DATAFRAME.getTypeId(),
                  null,
                  JsObject.createJsObject(),
                  new SimpleRequestCallback<SourceDocument>("Edit Data Frame") {
                     public void onResponseReceived(SourceDocument response)
                     {
                        addTab(response);
                     }
                  });
         }
      });
      
      events.addHandler(CodeBrowserNavigationEvent.TYPE, this);
      
      events.addHandler(CodeBrowserFinishedEvent.TYPE, this);

      events.addHandler(FileTypeChangedEvent.TYPE, new FileTypeChangedHandler()
      {
         public void onFileTypeChanged(FileTypeChangedEvent event)
         {
            manageCommands();
         }
      });
      
      events.addHandler(SourceOnSaveChangedEvent.TYPE, 
                        new SourceOnSaveChangedHandler() {
         @Override
         public void onSourceOnSaveChanged(SourceOnSaveChangedEvent event)
         {
            manageSaveCommands();
         }
      });

      events.addHandler(SwitchToDocEvent.TYPE, new SwitchToDocHandler()
      {
         public void onSwitchToDoc(SwitchToDocEvent event)
         {
            ensureVisible(false);
            view_.selectTab(event.getSelectedIndex());
         }
      });

      events.addHandler(SourceFileSavedEvent.TYPE, new SourceFileSavedHandler()
      {
         public void onSourceFileSaved(SourceFileSavedEvent event)
         {
            pMruList_.get().add(event.getPath());
         }
      });
            
      events.addHandler(SourceNavigationEvent.TYPE, 
                        new SourceNavigationHandler() {
         @Override
         public void onSourceNavigation(SourceNavigationEvent event)
         {
            if (!suspendSourceNavigationAdding_)
            {
               sourceNavigationHistory_.add(event.getNavigation());
            }
         }
      });
      
      sourceNavigationHistory_.addChangeHandler(new ChangeHandler()
      {

         @Override
         public void onChange(ChangeEvent event)
         {
            manageSourceNavigationCommands();
         }
      });
      
      events.addHandler(SynctexStatusChangedEvent.TYPE, 
                        new SynctexStatusChangedEvent.Handler()
      {
         @Override
         public void onSynctexStatusChanged(SynctexStatusChangedEvent event)
         {
            manageSynctexCommands();
         }
      });

      restoreDocuments(session);

      new IntStateValue(MODULE_SOURCE, KEY_ACTIVETAB, ClientState.PROJECT_PERSISTENT,
                        session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(Integer value)
         {
            if (value == null)
               return;
            if (value >= 0 && view_.getTabCount() > value)
               view_.selectTab(value);

            if (view_.getTabCount() > 0 && view_.getActiveTabIndex() >= 0)
            {
               editors_.get(view_.getActiveTabIndex()).onInitiallyLoaded();
            }

            // clear the history manager
            sourceNavigationHistory_.clear();
         }

         @Override
         protected Integer getValue()
         {
            return view_.getActiveTabIndex();
         }
      };

      uiPrefs_.verticallyAlignArgumentIndent().bind(new CommandWithArg<Boolean>()
      {
         @Override
         public void execute(Boolean arg)
         {
            AceEditorNative.setVerticallyAlignFunctionArgs(arg);
         }
      });
       
      initialized_ = true;
      // As tabs were added before, manageCommands() was suppressed due to
      // initialized_ being false, so we need to run it explicitly
      manageCommands();
      // Same with this event
      fireDocTabsChanged();
      
      // open project docs
      openProjectDocs(session);    
   }

   /**
    * @param isNewTabPending True if a new tab is about to be created. (If
    *    false and there are no tabs already, then a new source doc might
    *    be created to make sure we don't end up with a source pane showing
    *    with no tabs in it.)
    */
   private void ensureVisible(boolean isNewTabPending)
   {
      newTabPending_++;
      try
      {
         view_.ensureVisible();
      }
      finally
      {
         newTabPending_--;
      }
   }

   public Widget asWidget()
   {
      return view_.asWidget();
   }

   private void restoreDocuments(final Session session)
   {
      final JsArray<SourceDocument> docs =
            session.getSessionInfo().getSourceDocuments();

      for (int i = 0; i < docs.length(); i++)
      {
         addTab(docs.get(i));
      }
   }
   
   private void openProjectDocs(final Session session)
   {
      JsArrayString openDocs = session.getSessionInfo().getProjectOpenDocs();
      if (openDocs.length() > 0)
      {
         // set new tab pending for the duration of the continuation
         newTabPending_++;
                 
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
               newTabPending_--;
               onFirstTab();
               continuation.execute();
            }
            
         });
         
         // execute the continuation
         openCommands.run();
      }
   }
   
   public void onShowContent(ShowContentEvent event)
   {
      ensureVisible(true);
      ContentItem content = event.getContent();
      server_.newDocument(
            FileTypeRegistry.URLCONTENT.getTypeId(),
            null,
            (JsObject) content.cast(),
            new SimpleRequestCallback<SourceDocument>("Show")
            {
               @Override
               public void onResponseReceived(SourceDocument response)
               {
                  addTab(response);
               }
            });
   }

   public void onShowData(ShowDataEvent event)
   {
      DataItem data = event.getData();

      for (int i = 0; i < editors_.size(); i++)
      {
         String path = editors_.get(i).getPath();
         if (path != null && path.equals(data.getURI()))
         {
            ((DataEditingTarget)editors_.get(i)).updateData(data);

            ensureVisible(false);
            view_.selectTab(i);
            return;
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
                  addTab(response);
               }
            });
   }
   
   @Handler
   public void onShowProfiler()
   {
      // first try to activate existing
      for (int idx = 0; idx < editors_.size(); idx++)
      {
         String path = editors_.get(idx).getPath();
         if (ProfilerEditingTarget.PATH.equals(path))
         {
            ensureVisible(false);
            view_.selectTab(idx);
            return;
         }
      }
      
      // create new profiler 
      ensureVisible(true);
      server_.newDocument(
            FileTypeRegistry.PROFILER.getTypeId(),
            null,
            (JsObject) ProfilerContents.createDefault().cast(),
            new SimpleRequestCallback<SourceDocument>("Show Profiler")
            {
               @Override
               public void onResponseReceived(SourceDocument response)
               {
                  addTab(response);
               }
            });
   }
   

   @Handler
   public void onNewSourceDoc()
   {
      newDoc(FileTypeRegistry.R, null);
   }
   
   @Handler
   public void onNewTextDoc()
   {
      newDoc(FileTypeRegistry.TEXT, null);
   }
   
   @Handler
   public void onNewCppDoc()
   {
      if (uiPrefs_.useRcppTemplate().getValue())
      {
         newSourceDocWithTemplate(
             FileTypeRegistry.CPP, 
             "", 
             "rcpp.cpp",
             Position.create(0, 0),
             new CommandWithArg<EditingTarget> () {
               @Override
               public void execute(EditingTarget target)
               {
                  target.verifyCppPrerequisites(); 
               }
             }
         );
      }
      else
      {
         newDoc(FileTypeRegistry.CPP,
                new ResultCallback<EditingTarget, ServerError> () {
                   @Override
                   public void onSuccess(EditingTarget target)
                   {
                      target.verifyCppPrerequisites();
                   }
                });
      }
   }
   
   @Handler
   public void onNewSweaveDoc()
   {
      String concordance = new String();
      if (uiPrefs_.alwaysEnableRnwConcordance().getValue())
      {
         RnwWeave activeWeave = rnwWeaveRegistry_.findTypeIgnoreCase(
                                    uiPrefs_.defaultSweaveEngine().getValue());
         if (activeWeave.getInjectConcordance())
            concordance = "\\SweaveOpts{concordance=TRUE}\n";
      }
      final boolean hasConcordance = concordance.length() > 0;
      
      String contents = "\\documentclass{article}\n" +
                        "\n" +
                        "\\begin{document}\n" +
                        concordance +
                        "\n\n\n\n" +
                        "\\end{document}";
      
      newDoc(FileTypeRegistry.SWEAVE, 
             contents, 
             new ResultCallback<EditingTarget, ServerError> () {
                @Override
                public void onSuccess(EditingTarget target)
                {
                   int startRow = 4 + (hasConcordance ? 1 : 0);
                   target.setCursorPosition(Position.create(startRow, 0));
                }
             });
   }
   
   @Handler
   public void onNewRMarkdownDoc()
   {
      newSourceDocWithTemplate(FileTypeRegistry.RMARKDOWN, 
                               "", 
                               "r_markdown.Rmd",
                               Position.create(3, 0));
   }
   
   @Handler
   public void onNewRHTMLDoc()
   {
      newSourceDocWithTemplate(FileTypeRegistry.RHTML, 
                               "", 
                               "r_html.Rhtml");
   }
   
   
   @Handler
   public void onNewRDocumentationDoc()
   {
      new NewRdDialog(
         new OperationWithInput<NewRdDialog.Result>() {
           
            @Override
            public void execute(final NewRdDialog.Result result)
            {
               final Command createEmptyDoc = new Command() {
                  @Override
                  public void execute()
                  {
                     newSourceDocWithTemplate(FileTypeRegistry.RD, 
                           result.name, 
                           "r_documentation_empty.Rd",
                           Position.create(3, 7));
                  }  
               };
               
               if (!result.type.equals(NewRdDialog.Result.TYPE_NONE))
               {
                  server_.createRdShell(
                     result.name, 
                     result.type,
                     new SimpleRequestCallback<RdShellResult>() {
                        @Override
                        public void onResponseReceived(RdShellResult result)
                        {
                           if (result.getPath() != null)
                           {
                              fileTypeRegistry_.openFile(
                                 FileSystemItem.createFile(result.getPath()));
                           }
                           else if (result.getContents() != null)
                           {
                              newDoc(FileTypeRegistry.RD, 
                                     result.getContents(),
                                     null);
                           }
                           else
                           {
                              createEmptyDoc.execute();
                           }
                        }  
                   });
                 
               }
               else
               {
                  createEmptyDoc.execute();
               }
               
            }
          }).showModal();
   }
   
   @Handler
   public void onNewRPresentationDoc()
   {
      fileDialogs_.saveFile(
         "New R Presentation", 
         fileContext_,
         workbenchContext_.getDefaultFileDialogDir(), 
         ".Rpres", 
         true, 
         new ProgressOperationWithInput<FileSystemItem>() {

            @Override
            public void execute(final FileSystemItem input,
                                final ProgressIndicator indicator)
            {
               if (input == null)
               {
                  indicator.onCompleted();
                  return;
               }
               
               indicator.onProgress("Creating Presentation...");
               
               server_.createNewPresentation(
                 input.getPath(),
                 new VoidServerRequestCallback(indicator) {
                    @Override
                    public void onSuccess()
                    { 
                       openFile(input, 
                          FileTypeRegistry.RPRESENTATION,
                          new CommandWithArg<EditingTarget>() {

                           @Override
                           public void execute(EditingTarget arg)
                           {
                              server_.showPresentationPane(
                                          input.getPath(),
                                          new VoidServerRequestCallback());
                              
                           }
                          
                       });
                    }
                 });
               
            }
      
      });
   }
   
   private void newSourceDocWithTemplate(final TextFileType fileType, 
                                         String name,
                                         String template)
   {
      newSourceDocWithTemplate(fileType, name, template, null);
   }

   private void newSourceDocWithTemplate(final TextFileType fileType, 
                                         String name,
                                         String template,
                                         final Position cursorPosition)
   {
      newSourceDocWithTemplate(fileType, name, template, cursorPosition, null);
   }
   
   private void newSourceDocWithTemplate(
                       final TextFileType fileType, 
                       String name,
                       String template,
                       final Position cursorPosition,
                       final CommandWithArg<EditingTarget> onSuccess)
   {
      final ProgressIndicator indicator = new GlobalProgressDelayer(
            globalDisplay_, 500, "Creating new document...").getIndicator();

      server_.getSourceTemplate(name, 
                                template, 
                                new ServerRequestCallback<String>() {
         @Override
         public void onResponseReceived(String templateContents)
         {
            indicator.onCompleted();

            newDoc(fileType, 
                  templateContents, 
                  new ResultCallback<EditingTarget, ServerError> () {
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

   
   private void newDoc(EditableFileType fileType,
                       ResultCallback<EditingTarget, ServerError> callback)
   {
      newDoc(fileType, null, callback);
   }
   
   private void newDoc(EditableFileType fileType,
                       String contents,
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
                  EditingTarget target = addTab(newDoc);
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
   
   @Handler
   public void onFindInFiles()
   {
      String searchPattern = "";
      if (activeEditor_ != null && activeEditor_ instanceof TextEditingTarget)
      {  
         TextEditingTarget textEditor = (TextEditingTarget) activeEditor_;
         String selection = textEditor.getSelectedText();
         boolean multiLineSelection = selection.indexOf('\n') != -1;
         
         if ((selection.length() != 0) && !multiLineSelection)
            searchPattern = selection; 
      }
      
      events_.fireEvent(new FindInFilesEvent(searchPattern));
   }

   @Handler
   public void onActivateSource()
   {
      ensureVisible(false);
      if (activeEditor_ != null)
      {
         activeEditor_.focus();
         activeEditor_.ensureCursorVisible();
      }
   }

   @Handler
   public void onSwitchToTab()
   {
      if (view_.getTabCount() == 0)
         return;

      ensureVisible(false);

      view_.showOverflowPopup();
   }

   @Handler
   public void onFirstTab()
   {
      if (view_.getTabCount() == 0)
         return;

      ensureVisible(false);
      if (view_.getTabCount() > 0)
         view_.selectTab(0);
   }

   @Handler
   public void onPreviousTab()
   {
      if (view_.getTabCount() == 0)
         return;

      ensureVisible(false);
      int index = view_.getActiveTabIndex();
      if (index >= 1)
         view_.selectTab(index - 1);
   }

   @Handler
   public void onNextTab()
   {
      if (view_.getTabCount() == 0)
         return;

      ensureVisible(false);
      int index = view_.getActiveTabIndex();
      if (index < view_.getTabCount() - 1)
         view_.selectTab(index + 1);
   }

   @Handler
   public void onLastTab()
   {
      if (view_.getTabCount() == 0)
         return;

      ensureVisible(false);
      if (view_.getTabCount() > 0)
         view_.selectTab(view_.getTabCount() - 1);
   }

   @Handler
   public void onCloseSourceDoc()
   {
      if (view_.getTabCount() == 0)
         return;

      view_.closeTab(view_.getActiveTabIndex(), true);
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
   
   private void cpsExecuteForEachEditor(ArrayList<EditingTarget> editors,
                                       final CPSEditingTargetCommand command)
   {
      cpsExecuteForEachEditor(editors, command, null);
   }
   
   
   @Handler
   public void onSaveAllSourceDocs()
   {
      cpsExecuteForEachEditor(editors_, new CPSEditingTargetCommand()
      {
         @Override
         public void execute(EditingTarget target, Command continuation)
         {
            if (target.dirtyState().getValue())
            {
               target.save(continuation);
            }
            else
            {
               continuation.execute();
            }
         }
      });
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
                                    new ArrayList<UnsavedChangesTarget>();
         unsavedTargets.addAll(editingTargets);
         
         // show dialog
         view_.showUnsavedChangesDialog(
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
   
   private void saveChanges(ArrayList<UnsavedChangesTarget> targets,
                            Command onCompleted)
   {
      // convert back to editing targets
      ArrayList<EditingTarget> saveTargets = 
                                    new ArrayList<EditingTarget>();
      for (UnsavedChangesTarget target: targets)
      {
         EditingTarget saveTarget = 
                           getEditingTargetForId(target.getId());
         if (saveTarget != null)
            saveTargets.add(saveTarget);
      }
        
      // execute the save
      cpsExecuteForEachEditor(
         
         // targets the user chose to save
         saveTargets, 
         
         // save each editor
         new CPSEditingTargetCommand()
         {
            @Override
            public void execute(EditingTarget saveTarget, 
                                Command continuation)
            {         
               saveTarget.save(continuation); 
            }
         },
         
         // onCompleted at the end
         onCompleted
      );          
   }
          
   
   private EditingTarget getEditingTargetForId(String id)
   {
      for (EditingTarget target : editors_)
         if (id.equals(target.getId()))
            return target;

      return null;
   }
   
   @Handler
   public void onCloseAllSourceDocs()
   {
      closeAllSourceDocs("Close All",  null);
   }
   
   public void closeAllSourceDocs(String caption, Command onCompleted)
   { 
      // collect up a list of dirty documents
      ArrayList<EditingTarget> dirtyTargets = new ArrayList<EditingTarget>();
      for (EditingTarget target : editors_)
         if (target.dirtyState().getValue())
            dirtyTargets.add(target);
      
      // create a command used to close all tabs 
      final Command closeAllTabsCommand = new Command()
      {
         @Override
         public void execute()
         {
            cpsExecuteForEachEditor(editors_, new CPSEditingTargetCommand()
            {
               @Override
               public void execute(EditingTarget target, Command continuation)
               {
                  view_.closeTab(target.asWidget(), false, continuation);
               }
            });
            
         }     
      };
      
      // save targets
      saveEditingTargetsWithPrompt(caption,
                                   dirtyTargets, 
                                   CommandUtil.join(closeAllTabsCommand,
                                                    onCompleted),
                                   null);
      
   }
   
   private boolean isUnsavedFileBackedTarget(EditingTarget target)
   {
      return target.dirtyState().getValue() && (target.getPath() != null);
   }
   
   public ArrayList<UnsavedChangesTarget> getUnsavedChanges()
   {
      ArrayList<UnsavedChangesTarget> targets = 
                                       new ArrayList<UnsavedChangesTarget>();
      for (EditingTarget target : editors_)
         if (isUnsavedFileBackedTarget(target))
            targets.add(target);
      
      return targets;
   }
   
   public void saveAllUnsaved(Command onCompleted)
   {
      saveChanges(getUnsavedChanges(), onCompleted);
   }
   
   public void saveWithPrompt(UnsavedChangesTarget target, 
                              Command onCompleted,
                              Command onCancelled)
   {
      EditingTarget editingTarget = getEditingTargetForId(target.getId());
      if (editingTarget != null)
         editingTarget.saveWithPrompt(onCompleted, onCancelled);
   }
   
   public void handleUnsavedChangesBeforeExit(
                        ArrayList<UnsavedChangesTarget> saveTargets,
                        final Command onCompleted)
   {
      // first handle saves, then revert unsaved, then callback on completed
      saveChanges(saveTargets, new Command() {

         @Override
         public void execute()
         {
            // revert unsaved
            revertUnsavedTargets(onCompleted);
         }
      });   
   }
   
   private void revertUnsavedTargets(Command onCompleted)
   {
      // collect up unsaved targets
      ArrayList<EditingTarget> unsavedTargets =  new ArrayList<EditingTarget>();
      for (EditingTarget target : editors_)
         if (isUnsavedFileBackedTarget(target))
            unsavedTargets.add(target);
      
      // revert all of them
      cpsExecuteForEachEditor(
         
         // targets the user chose not to save
         unsavedTargets, 
         
         // save each editor
         new CPSEditingTargetCommand()
         {
            @Override
            public void execute(EditingTarget saveTarget, 
                                Command continuation)
            {
               if (saveTarget.getPath() != null)
               {
                  // file backed document -- revert it
                  saveTarget.revertChanges(continuation);
               }
               else
               {
                  // untitled document -- just close the tab non-interactively
                  view_.closeTab(saveTarget.asWidget(), false, continuation);
               }
            }
         },
         
         // onCompleted at the end
         onCompleted
      );          
            
   }
   
   
   @Handler
   public void onOpenSourceDoc()
   {
      fileDialogs_.openFile(
            "Open File",
            fileContext_,
            workbenchContext_.getDefaultFileDialogDir(),
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                     return;

                  workbenchContext_.setDefaultFileDialogDir(
                                                   input.getParentPath());

                  indicator.onCompleted();
                  Scheduler.get().scheduleDeferred(new ScheduledCommand()
                  {
                     public void execute()
                     {
                        fileTypeRegistry_.openFile(input);
                     }
                  });
               }
            });
   }
   
    
   public void onOpenSourceFile(OpenSourceFileEvent event)
   {
      doOpenSourceFile(event.getFile(),
                       event.getFileType(),
                       event.getPosition(),
                       null, 
                       event.getNavigationMethod(),
                       false);
   }
   
   
   
   public void onOpenPresentationSourceFile(OpenPresentationSourceFileEvent event)
   {
      // don't do the navigation if the active document is a source
      // file from this presentation module
      
      doOpenSourceFile(event.getFile(),
                       event.getFileType(),
                       event.getPosition(),
                       event.getPattern(),
                       NavigationMethod.HighlightLine,
                       true);
      
   }
   
   public void onEditPresentationSource(final EditPresentationSourceEvent event)
   { 
      openFile(
            event.getSourceFile(), 
            FileTypeRegistry.RPRESENTATION,
            new CommandWithArg<EditingTarget>() {
               @Override
               public void execute(final EditingTarget editor)
               {   
                  TextEditingTargetPresentationHelper.navigateToSlide(
                                                         editor, 
                                                         event.getSlideIndex());
               }
         });
   }
   
   
   private void doOpenSourceFile(final FileSystemItem file,
                                 final TextFileType fileType,
                                 final FilePosition position,
                                 final String pattern,
                                 final NavigationMethod navMethod, 
                                 final boolean forceHighlightMode)
   {
      final boolean isDebugNavigation = 
            navMethod == NavigationMethod.DebugStep ||
            navMethod == NavigationMethod.DebugFrame || 
            navMethod == NavigationMethod.DebugEnd;
      if (isDebugNavigation)
      {
         setPendingDebugSelection();
      }

      final CommandWithArg<FileSystemItem> action = new CommandWithArg<FileSystemItem>()
      {
         @Override
         public void execute(FileSystemItem file)
         {
            openFile(file,
                     fileType,
                     new CommandWithArg<EditingTarget>() {

               @Override
               public void execute(EditingTarget target)
               {
                  if (position != null)
                  {
                     SourcePosition endPosition = null;
                     if (isDebugNavigation)
                     {
                        DebugFilePosition filePos = 
                              (DebugFilePosition) position.cast();
                        endPosition = SourcePosition.create(
                              filePos.getEndLine() - 1,
                              filePos.getEndColumn() + 1);
                     }
                     navigate(target, 
                              SourcePosition.create(position.getLine() - 1,
                                                    position.getColumn() - 1),
                              endPosition);
                  }
                  else if (pattern != null)
                  {
                     Position pos = target.search(pattern);
                     if (pos != null)
                     {
                        navigate(target, 
                                 SourcePosition.create(pos.getRow(), 0),
                                 null);
                     }
                  }
               }

            });
         }
         
         private void navigate(final EditingTarget target,
                               final SourcePosition srcPosition,
                               final SourcePosition srcEndPosition)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  if (navMethod == NavigationMethod.DebugStep)
                  {
                     target.highlightDebugLocation(
                           srcPosition, 
                           srcEndPosition, 
                           true);
                  }
                  else if (navMethod == NavigationMethod.DebugFrame)
                  {
                     target.highlightDebugLocation(
                           srcPosition,
                           srcEndPosition,
                           false);
                  }
                  else if (navMethod == NavigationMethod.DebugEnd)
                  {
                     target.endDebugHighlighting();
                  }
                  else
                  {
                     // force highlight mode if requested
                     if (forceHighlightMode)
                        target.forceLineHighlighting();
                     
                     // now navigate to the new position
                     boolean highlight = 
                           navMethod == NavigationMethod.HighlightLine &&
                           !uiPrefs_.highlightSelectedLine().getValue();
                     target.navigateToPosition(srcPosition,
                                               false,
                                               highlight);
                  }
               }
            });
         }
      };

      // Warning: event.getFile() can be null (e.g. new Sweave document)
      if (file != null && file.getLength() < 0)
      {
         // If the file has no size info, stat the file from the server. This
         // is to prevent us from opening large files accidentally.

         server_.stat(file.getPath(), new ServerRequestCallback<FileSystemItem>()
         {
            @Override
            public void onResponseReceived(FileSystemItem response)
            {
               action.execute(response);
            }

            @Override
            public void onError(ServerError error)
            {
               // Couldn't stat the file? Proceed anyway. If the file doesn't
               // exist, we'll let the downstream code be the one to show the
               // error.
               action.execute(file);
            }
         });
      }
      else
      {
         action.execute(file);
      }
   }
   

   private void openFile(FileSystemItem file)
   {
      openFile(file, fileTypeRegistry_.getTextTypeForFile(file));
   }
   
   private void openFile(FileSystemItem file,  TextFileType fileType)
   {
      openFile(file, 
               fileType, 
               new CommandWithArg<EditingTarget>() {
                  @Override
                  public void execute(EditingTarget arg)
                  {
                     
                  }
               });
   }
   
   private void openFile(final FileSystemItem file,
                         final TextFileType fileType,
                         final CommandWithArg<EditingTarget> executeOnSuccess)
   {
      openFile(file,
            fileType,
            new ResultCallback<EditingTarget, ServerError>() {
               @Override
               public void onSuccess(EditingTarget target)
               {
                  if (executeOnSuccess != null)
                     executeOnSuccess.execute(target);
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
   private void openFile(final FileSystemItem file,
                         final TextFileType fileType,
                         final ResultCallback<EditingTarget, ServerError> resultCallback)
   {
      ensureVisible(true);

      if (file == null)
      {
         newDoc(fileType, resultCallback);
         return;
      }

      for (int i = 0; i < editors_.size(); i++)
      {
         EditingTarget target = editors_.get(i);
         String thisPath = target.getPath();
         if (thisPath != null
             && thisPath.equalsIgnoreCase(file.getPath()))
         {
            view_.selectTab(i);
            pMruList_.get().add(thisPath);
            if (resultCallback != null)
               resultCallback.onSuccess(target);
            return;
         }
      }

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
                                     Operation cancelOperation)
   {
      StringBuilder msg = new StringBuilder();
      msg.append("The source file '" + file.getName() + "' is large (");
      msg.append(StringUtil.formatFileSize(file.getLength()) + ") ");
      msg.append("and may take some time to open. ");
      msg.append("Are you sure you want to continue opening it?");
      globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                                      "Confirm Open",
                                      msg.toString(),
                                      openOperation,
                                      false);   // 'No' is default
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
            uiPrefs_.defaultEncoding().getValue(),
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
                  dismissProgress.execute();
                  pMruList_.get().add(document.getPath());
                  EditingTarget target = addTab(document);
                  if (resultCallback != null)
                     resultCallback.onSuccess(target);
               }
            });
   }


   private EditingTarget addTab(SourceDocument doc)
   {
      final EditingTarget target = editingTargetSource_.getEditingTarget(
            doc, fileContext_, new Provider<String>()
            {
               public String get()
               {
                  return getNextDefaultName();
               }
            });
      
      final Widget widget = target.asWidget();

      editors_.add(target);
      view_.addTab(widget,
                   target.getIcon(),
                   target.getName().getValue(),
                   target.getTabTooltip(), // used as tooltip, if non-null
                   true);
      fireDocTabsChanged();

      target.getName().addValueChangeHandler(new ValueChangeHandler<String>()
      {
         public void onValueChange(ValueChangeEvent<String> event)
         {
            view_.renameTab(widget,
                            target.getIcon(),
                            event.getValue(),
                            target.getPath());
            fireDocTabsChanged();
         }
      });

      view_.setDirty(widget, target.dirtyState().getValue());
      target.dirtyState().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            view_.setDirty(widget, event.getValue());
            manageCommands();
         }
      });

      target.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            view_.selectTab(widget);
         }
      });

      target.addCloseHandler(new CloseHandler<Void>()
      {
         public void onClose(CloseEvent<Void> voidCloseEvent)
         {
            view_.closeTab(widget, false);
         }
      });

      return target;
   }

   private String getNextDefaultName()
   {
      int max = 0;
      for (EditingTarget target : editors_)
      {
         String name = target.getName().getValue();
         max = Math.max(max, getUntitledNum(name));
      }

      return "Untitled" + (max + 1);
   }

   private native final int getUntitledNum(String name) /*-{
      var match = /^Untitled([0-9]{1,5})$/.exec(name);
      if (!match)
         return 0;
      return parseInt(match[1]);
   }-*/;

   public void onInsertSource(final InsertSourceEvent event)
   {
      if (activeEditor_ != null
          && activeEditor_ instanceof TextEditingTarget
          && commands_.executeCode().isEnabled())
      {
         TextEditingTarget textEditor = (TextEditingTarget) activeEditor_;
         textEditor.insertCode(event.getCode(), event.isBlock());
      }
      else
      {
         newDoc(FileTypeRegistry.R,
                new ResultCallback<EditingTarget, ServerError>()
         {
            public void onSuccess(EditingTarget arg)
            {
               ((TextEditingTarget)arg).insertCode(event.getCode(),
                                                   event.isBlock());
            }
         });
      }
   }

   public void onTabClosing(final TabClosingEvent event)
   {
      EditingTarget target = editors_.get(event.getTabIndex());
      if (!target.onBeforeDismiss())
         event.cancel();
   }
   
   @Override
   public void onTabClose(TabCloseEvent event)
   {
      // can't proceed if there is no active editor
      if (activeEditor_ == null)
         return;

      if (event.getTabIndex() >= editors_.size())
         return; // Seems like this should never happen...?

      final String activeEditorId = activeEditor_.getId();

      if (editors_.get(event.getTabIndex()).getId().equals(activeEditorId))
      {
         // scan the source navigation history for an entry that can
         // be used as the next active tab (anything that doesn't have
         // the same document id as the currently active tab)
         SourceNavigation srcNav = sourceNavigationHistory_.scanBack(
               new SourceNavigationHistory.Filter()
               {
                  public boolean includeEntry(SourceNavigation navigation)
                  {
                     return !navigation.getDocumentId().equals(activeEditorId);
                  }
               });

         // see if the source navigation we found corresponds to an active
         // tab -- if it does then set this on the event
         if (srcNav != null)
         {
            for (int i=0; i<editors_.size(); i++)
            {
               if (srcNav.getDocumentId().equals(editors_.get(i).getId()))
               {
                  view_.selectTab(i);
                  break;
               }
            }
         }
      }
   }

   public void onTabClosed(TabClosedEvent event)
   {
      EditingTarget target = editors_.remove(event.getTabIndex());
      target.onDismiss();
      if (activeEditor_ == target)
      {
         activeEditor_.onDeactivate();
         activeEditor_ = null;
      }
      server_.closeDocument(target.getId(),
                            new VoidServerRequestCallback());

      manageCommands();
      fireDocTabsChanged();

      if (view_.getTabCount() == 0)
      {
         sourceNavigationHistory_.clear();
         events_.fireEvent(new LastSourceDocClosedEvent());
      }
   }

   private void fireDocTabsChanged()
   {
      if (!initialized_)
         return;

      String[] ids = new String[editors_.size()];
      ImageResource[] icons = new ImageResource[editors_.size()];
      String[] names = new String[editors_.size()];
      String[] paths = new String[editors_.size()];
      for (int i = 0; i < ids.length; i++)
      {
         EditingTarget target = editors_.get(i);
         ids[i] = target.getId();
         icons[i] = target.getIcon();
         names[i] = target.getName().getValue();
         paths[i] = target.getPath();
      }

      events_.fireEvent(new DocTabsChangedEvent(ids, icons, names, paths));

      view_.manageChevronVisibility();
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
         // don't send focus to the tab if we're expecting a debug selection
         // event
         if (initialized_ && !isDebugSelectionPending())
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  if (activeEditor_ != null)
                     activeEditor_.focus();
               }
            });
         }
         else if (isDebugSelectionPending())
         {
            clearPendingDebugSelection();
         }
      }
      
      if (initialized_)
         manageCommands();
   }

   private void manageCommands()
   {
      boolean hasDocs = editors_.size() > 0;

      commands_.closeSourceDoc().setEnabled(hasDocs);
      commands_.closeAllSourceDocs().setEnabled(hasDocs);
      commands_.nextTab().setEnabled(hasDocs);
      commands_.previousTab().setEnabled(hasDocs);
      commands_.firstTab().setEnabled(hasDocs);
      commands_.lastTab().setEnabled(hasDocs);
      commands_.switchToTab().setEnabled(hasDocs);
      commands_.activateSource().setEnabled(hasDocs);
      commands_.setWorkingDirToActiveDoc().setEnabled(hasDocs);

      HashSet<AppCommand> newCommands =
            activeEditor_ != null ? activeEditor_.getSupportedCommands()
                                  : new HashSet<AppCommand>();
            
      HashSet<AppCommand> commandsToEnable = new HashSet<AppCommand>(newCommands);
      commandsToEnable.removeAll(activeCommands_);

      HashSet<AppCommand> commandsToDisable = new HashSet<AppCommand>(activeCommands_);
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
      
      activeCommands_ = newCommands;

      assert verifyNoUnsupportedCommands(newCommands)
            : "Unsupported commands detected (please add to Source.dynamicCommands_)";
   }
   
   private void manageSynctexCommands()
   {
      // synctex commands are enabled if we have synctex for the active editor
      boolean synctexAvailable = synctex_.isSynctexAvailable();
      if (synctexAvailable)
      {
         if ((activeEditor_ != null) && 
             (activeEditor_.getPath() != null) &&
             activeEditor_.canCompilePdf())
         {
            synctexAvailable = synctex_.isSynctexAvailable();
         }
         else
         {
            synctexAvailable = false;
         }
      }
     
      synctex_.enableCommands(synctexAvailable);
   }
   
   private void manageVcsCommands()
   {
      // manage vcs log
      boolean vcsCommandsEnabled = session_.getSessionInfo().isVcsEnabled() &&
                                   activeEditor_ != null &&
                                   activeEditor_.getPath() != null ;
      
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
   }
   
   private void manageSaveCommands()
   {
      boolean saveEnabled = (activeEditor_ != null) &&
            activeEditor_.isSaveCommandActive();
      commands_.saveSourceDoc().setEnabled(saveEnabled);
      manageSaveAllCommand();
   }
   
   
   private void manageSaveAllCommand()
   {      
      // if one document is dirty then we are enabled
      for (EditingTarget target : editors_)
      {
         if (target.isSaveCommandActive())
         {
            commands_.saveAllSourceDocs().setEnabled(true);
            return;
         }
      }
      
      // not one was dirty, disabled
      commands_.saveAllSourceDocs().setEnabled(false);
   }
   
  
   private boolean verifyNoUnsupportedCommands(HashSet<AppCommand> commands)
   {
      HashSet<AppCommand> temp = new HashSet<AppCommand>(commands);
      temp.removeAll(dynamicCommands_);
      return temp.size() == 0;
   }

   public void onFileEdit(FileEditEvent event)
   {
      fileTypeRegistry_.editFile(event.getFile());
   }

   public void onBeforeShow(BeforeShowEvent event)
   {
      if (view_.getTabCount() == 0 && newTabPending_ == 0)
      {
         // Avoid scenarios where the Source tab comes up but no tabs are
         // in it. (But also avoid creating an extra source tab when there
         // were already new tabs about to be created!)
         onNewSourceDoc();
      }
   }
      
   @Handler
   public void onSourceNavigateBack()
   {
      if (!sourceNavigationHistory_.isForwardEnabled())
      {
         if (activeEditor_ != null)
            activeEditor_.recordCurrentNavigationPosition();
      }

      SourceNavigation navigation = sourceNavigationHistory_.goBack();
      if (navigation != null)
         attemptSourceNavigation(navigation, commands_.sourceNavigateBack());
   }
   
   @Handler
   public void onSourceNavigateForward()
   {
      SourceNavigation navigation = sourceNavigationHistory_.goForward();
      if (navigation != null)
         attemptSourceNavigation(navigation, commands_.sourceNavigateForward());
   }
   
   private void attemptSourceNavigation(final SourceNavigation navigation,
                                        final AppCommand retryCommand)
   {
      // see if we can navigate by id
      String docId = navigation.getDocumentId();
      final EditingTarget target = getEditingTargetForId(docId);
      if (target != null)
      {
         // check for navigation to the current position -- in this
         // case execute the retry command
         if ( (target == activeEditor_) && 
               target.isAtSourceRow(navigation.getPosition()))
         {
            if (retryCommand.isEnabled())
               retryCommand.execute();
         }
         else
         {
            suspendSourceNavigationAdding_ = true;
            try
            {
               view_.selectTab(target.asWidget());
               target.restorePosition(navigation.getPosition());
            }
            finally
            {
               suspendSourceNavigationAdding_ = false;
            }
         }
      }
      
      // check for code browser navigation
      else if ((navigation.getPath() != null) &&
               navigation.getPath().equals(CodeBrowserEditingTarget.PATH))
      {
         activateCodeBrowser(
            new SourceNavigationResultCallback<CodeBrowserEditingTarget>(
                                                      navigation.getPosition(),
                                                      retryCommand));
      }
      
      // check for file path navigation
      else if ((navigation.getPath() != null) && 
               !navigation.getPath().startsWith(DataItem.URI_PREFIX))
      {
         FileSystemItem file = FileSystemItem.createFile(navigation.getPath());
         TextFileType fileType = fileTypeRegistry_.getTextTypeForFile(file);
         
         // open the file and restore the position
         openFile(file,
                  fileType,
                  new SourceNavigationResultCallback<EditingTarget>(
                                                   navigation.getPosition(),
                                                   retryCommand));
      } 
      else
      {
         // couldn't navigate to this item, retry
         if (retryCommand.isEnabled())
            retryCommand.execute();
      }
   }
   
   private void manageSourceNavigationCommands()
   {   
      commands_.sourceNavigateBack().setEnabled(
            sourceNavigationHistory_.isBackEnabled());

      commands_.sourceNavigateForward().setEnabled(
            sourceNavigationHistory_.isForwardEnabled());  
   }
   
    
   @Override
   public void onCodeBrowserNavigation(final CodeBrowserNavigationEvent event)
   {
      if (event.getDebugPosition() != null)
      {
         setPendingDebugSelection();
      }
      
      activateCodeBrowser(new ResultCallback<CodeBrowserEditingTarget,ServerError>() {
         @Override
         public void onSuccess(CodeBrowserEditingTarget target)
         {
            target.showFunction(event.getFunction());
            if (event.getDebugPosition() != null)
            {
               target.highlightDebugLocation(SourcePosition.create(
                        event.getDebugPosition().getLine(), 
                        event.getDebugPosition().getColumn() - 1),
                     SourcePosition.create(
                        event.getDebugPosition().getEndLine(),
                        event.getDebugPosition().getEndColumn() + 1),
                     event.getExecuting());
            }
         }
      });
   }
   
   @Override
   public void onCodeBrowserFinished(final CodeBrowserFinishedEvent event)
   {
      int codeBrowserTabIndex = indexOfCodeBrowserTab();
      if (codeBrowserTabIndex >= 0)
      {
         view_.closeTab(codeBrowserTabIndex, false);
         return;
      }
   }
   
   // returns the index of the tab currently containing the code browser, or
   // -1 if the code browser tab isn't currently open;
   private int indexOfCodeBrowserTab()
   {
      // see if there is an existing target to use
      for (int idx = 0; idx < editors_.size(); idx++)
      {
         String path = editors_.get(idx).getPath();
         if (CodeBrowserEditingTarget.PATH.equals(path))
         {
            return idx;
         }
      }
      return -1;
   }
     
   private void activateCodeBrowser(
         final ResultCallback<CodeBrowserEditingTarget,ServerError> callback)
   {
      int codeBrowserTabIndex = indexOfCodeBrowserTab();
      if (codeBrowserTabIndex >= 0)
      {
         ensureVisible(false);
         view_.selectTab(codeBrowserTabIndex);
         
         // callback
         callback.onSuccess( (CodeBrowserEditingTarget)
               editors_.get(codeBrowserTabIndex));
         
         // satisfied request
         return;
      }

      // create a new one
      newDoc(FileTypeRegistry.CODEBROWSER,
             new ResultCallback<EditingTarget, ServerError>()
             {
               @Override
               public void onSuccess(EditingTarget arg)
               {
                  callback.onSuccess( (CodeBrowserEditingTarget)arg);
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
   
   private boolean isDebugSelectionPending()
   {
      return debugSelectionTimer_ != null;
   }
   
   private void clearPendingDebugSelection()
   {
      if (debugSelectionTimer_ != null)
      {
         debugSelectionTimer_.cancel();
         debugSelectionTimer_ = null;
      }
   }
   
   private void setPendingDebugSelection()
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
      
   private class SourceNavigationResultCallback<T extends EditingTarget> 
                        extends ResultCallback<T,ServerError>
   {
      public SourceNavigationResultCallback(SourcePosition restorePosition,
                                            AppCommand retryCommand)
      {
         suspendSourceNavigationAdding_ = true;
         restorePosition_ = restorePosition;
         retryCommand_ = retryCommand;
      }
      
      @Override
      public void onSuccess(final T target)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               try
               {
                  target.restorePosition(restorePosition_);
               }
               finally
               {
                  suspendSourceNavigationAdding_ = false;
               }
            }
         });
      }

      @Override
      public void onFailure(ServerError info)
      {
         suspendSourceNavigationAdding_ = false;
         if (retryCommand_.isEnabled())
            retryCommand_.execute();
      }
      
      @Override
      public void onCancelled()
      {
         suspendSourceNavigationAdding_ = false;
      }
      
      private final SourcePosition restorePosition_;
      private final AppCommand retryCommand_;
   }

   ArrayList<EditingTarget> editors_ = new ArrayList<EditingTarget>();
   private EditingTarget activeEditor_;
   private final Commands commands_;
   private final Display view_;
   private final SourceServerOperations server_;
   private final EditingTargetSource editingTargetSource_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final GlobalDisplay globalDisplay_;
   private final WorkbenchContext workbenchContext_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileContext_;
   private final EventBus events_;
   private final Session session_;
   private final Synctex synctex_;
   private final Provider<FileMRUList> pMruList_;
   private final UIPrefs uiPrefs_;
   private final RnwWeaveRegistry rnwWeaveRegistry_;
   private HashSet<AppCommand> activeCommands_ = new HashSet<AppCommand>();
   private final HashSet<AppCommand> dynamicCommands_;
   private final SourceNavigationHistory sourceNavigationHistory_ = 
                                              new SourceNavigationHistory(30);

   private boolean suspendSourceNavigationAdding_;
  
   private static final String MODULE_SOURCE = "source-pane";
   private static final String KEY_ACTIVETAB = "activeTab";
   private boolean initialized_;
   private Timer debugSelectionTimer_ = null;

   // If positive, a new tab is about to be created
   private int newTabPending_;
}
