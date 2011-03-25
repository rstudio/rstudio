/*
 * Source.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Widgetable;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.EditableFileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileHandler;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.MRUList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.data.events.ViewDataEvent;
import org.rstudio.studio.client.workbench.views.data.events.ViewDataHandler;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource;
import org.rstudio.studio.client.workbench.views.source.editors.data.DataEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.events.*;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashSet;

public class Source implements InsertSourceHandler,
                             Widgetable,
                             OpenSourceFileHandler,
                             TabClosingHandler,
                             SelectionHandler<Integer>,
                             TabClosedHandler,
                             FileEditHandler,
                             ShowContentHandler,
                             ShowDataHandler,
                             BeforeShowHandler
{
   public interface Display extends Widgetable,
                                    HasTabClosingHandlers,
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
      void closeTab(int index, boolean interactive);
      void setDirty(Widget widget, boolean dirty);
      void manageChevronVisibility();
      void showOverflowPopup();

      void ensureVisible();

      void renameTab(Widget child,
                     ImageResource icon,
                     String value,
                     String tooltip);

      HandlerRegistration addBeforeShowHandler(BeforeShowHandler handler);
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
                 Session session,
                 MRUList mruList,
                 UIPrefs uiPrefs)
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
      mruList_ = mruList;
      uiPrefs_ = uiPrefs;

      view_.addTabClosingHandler(this);
      view_.addTabClosedHandler(this);
      view_.addSelectionHandler(this);
      view_.addBeforeShowHandler(this);

      dynamicCommands_ = new HashSet<AppCommand>();
      dynamicCommands_.add(commands.saveSourceDoc());
      dynamicCommands_.add(commands.reopenSourceDocWithEncoding());
      dynamicCommands_.add(commands.saveSourceDocAs());
      dynamicCommands_.add(commands.printSourceDoc());
      dynamicCommands_.add(commands.executeCode());
      dynamicCommands_.add(commands.executeAllCode());
      dynamicCommands_.add(commands.compilePDF());
      dynamicCommands_.add(commands.publishPDF());
      dynamicCommands_.add(commands.popoutDoc());
      dynamicCommands_.add(commands.findReplace());
      dynamicCommands_.add(commands.extractFunction());
      dynamicCommands_.add(commands.commentUncomment());
      for (AppCommand command : dynamicCommands_)
      {
         command.setVisible(false);
         command.setEnabled(false);
      }
      
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

      events.addHandler(FileTypeChangedEvent.TYPE, new FileTypeChangedHandler()
      {
         public void onFileTypeChanged(FileTypeChangedEvent event)
         {
            manageCommands();
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
            mruList_.add(event.getPath());
         }
      });

      restoreDocuments(session);

      new IntStateValue(MODULE_SOURCE, KEY_ACTIVETAB, true,
                        session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(Integer value)
         {
            if (value == null)
               return;
            if (value >= 0 && view_.getTabCount() > value)
               view_.selectTab(value);
         }

         @Override
         protected Integer getValue()
         {
            return view_.getActiveTabIndex();
         }
      };
      
      initialized_ = true;
      // As tabs were added before, manageCommands() was suppressed due to
      // initialized_ being false, so we need to run it explicitly
      manageCommands();
      // Same with this event
      fireDocTabsChanged();
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

   public Widget toWidget()
   {
      return view_.toWidget();
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
      ensureVisible(true);
      DataItem data = event.getData();

      for (int i = 0; i < editors_.size(); i++)
      {
         String path = editors_.get(i).getPath();
         if (path != null && path.equals(data.getURI()))
         {
            ((DataEditingTarget)editors_.get(i)).updateData(data);

            view_.selectTab(i);
            return;
         }
      }

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
   public void onNewSourceDoc()
   {
      newDoc(FileTypeRegistry.R, uiPrefs_.defaultEncoding().getValue(), null);
   }

   private void newDoc(EditableFileType fileType,
                       String encoding,
                       final CommandWithArg<EditingTarget> executeOnSuccess)
   {
      ensureVisible(true);
      server_.newDocument(
            fileType.getTypeId(),
            encoding,
            JsObject.createJsObject(),
            new SimpleRequestCallback<SourceDocument>(
                  "Error Creating New Document")
            {
               @Override
               public void onResponseReceived(SourceDocument newDoc)
               {
                  EditingTarget target = addTab(newDoc);
                  if (executeOnSuccess != null)
                     executeOnSuccess.execute(target);
               }
            });
   }

   @Handler
   public void onActivateSource()
   {
      ensureVisible(false);
      if (activeEditor_ != null)
         activeEditor_.focus();
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

   @Handler
   public void onCloseAllSourceDocs()
   {

   }

   @Handler
   public void onOpenSourceDoc()
   {
      fileDialogs_.openFile(
            "Open File",
            fileContext_,
            null,
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                     return;

                  indicator.onCompleted();
                  DeferredCommand.addCommand(new Command()
                  {
                     public void execute()
                     {
                        fileTypeRegistry_.openFile(input);
                     }
                  });
               }
            });
   }

   public void onOpenSourceFile(final OpenSourceFileEvent event)
   {
      openFile(event.getFile(), event.getFileType(), event.getEncoding());
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
                         final String encoding)
   {
      ensureVisible(true);

      if (file == null)
      {
         newDoc(fileType, encoding, null);
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
            mruList_.add(thisPath);
            return;
         }
      }

      EditingTarget target = editingTargetSource_.getEditingTarget(fileType);

      if (file.getLength() > target.getFileSizeLimit())
      {
         showFileTooLargeWarning(file, target.getFileSizeLimit());
      }
      else if (file.getLength() > target.getLargeFileSize())
      {
         confirmOpenLargeFile(file,  new Operation() {
            public void execute()
            {
               openFileFromServer(file, fileType, encoding);
            }
         });
      }
      else
      {
         openFileFromServer(file, fileType, encoding);
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
                                     Operation openOperation)
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

   private void openFileFromServer(final FileSystemItem file,
                                   final TextFileType fileType,
                                   String encoding)
   {
      final Command dismissProgress = globalDisplay_.showProgress(
                                                         "Opening file...");

      server_.openDocument(
            file.getPath(),
            fileType.getTypeId(),
            encoding,
            new ServerRequestCallback<SourceDocument>()
            {
               @Override
               public void onError(ServerError error)
               {
                  dismissProgress.execute();
                  Debug.logError(error);
                  globalDisplay_.showMessage(GlobalDisplay.MSG_ERROR,
                                             "Error while opening file",
                                             error.getUserMessage());
               }

               @Override
               public void onResponseReceived(SourceDocument document)
               {
                  dismissProgress.execute();
                  mruList_.add(document.getPath());
                  addTab(document);
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
      
      final Widget widget = target.toWidget();

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
                uiPrefs_.defaultEncoding().getValue(),
                new CommandWithArg<EditingTarget>()
         {
            public void execute(EditingTarget arg)
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
         if (initialized_)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  activeEditor_.focus();
               }
            });
         }
      }

      if (initialized_)
         manageCommands();
   }

   private void manageCommands()
   {
      boolean hasDocs = editors_.size() > 0;

      commands_.closeSourceDoc().setEnabled(hasDocs);
      commands_.nextTab().setEnabled(hasDocs);
      commands_.previousTab().setEnabled(hasDocs);
      commands_.firstTab().setEnabled(hasDocs);
      commands_.lastTab().setEnabled(hasDocs);
      commands_.switchToTab().setEnabled(hasDocs);
      commands_.activateSource().setEnabled(hasDocs);

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

      // Save/Save As should always stay visible
      commands_.saveSourceDoc().setVisible(true);
      commands_.saveSourceDocAs().setVisible(true);

      activeCommands_ = newCommands;

      assert verifyNoUnsupportedCommands(newCommands)
            : "Unsupported commands detected (please add to Source.dynamicCommands_)";
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

   ArrayList<EditingTarget> editors_ = new ArrayList<EditingTarget>();
   private EditingTarget activeEditor_;
   private final Commands commands_;
   private final Display view_;
   private final SourceServerOperations server_;
   private final EditingTargetSource editingTargetSource_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;
   private final RemoteFileSystemContext fileContext_;
   private final EventBus events_;
   private final MRUList mruList_;
   private final UIPrefs uiPrefs_;
   private HashSet<AppCommand> activeCommands_ = new HashSet<AppCommand>();
   private final HashSet<AppCommand> dynamicCommands_;

   private static final String MODULE_SOURCE = "source";
   private static final String KEY_ACTIVETAB = "activeTab";
   private boolean initialized_;

   // If positive, a new tab is about to be created
   private int newTabPending_;
}
