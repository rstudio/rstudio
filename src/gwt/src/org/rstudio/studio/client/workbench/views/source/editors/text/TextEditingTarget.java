/*
 * TextEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.*;
import org.rstudio.core.client.Invalidation.Token;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.files.FilenameTransform;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.theme.ThemeFonts;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarPopupMenu;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.ChooseEncodingDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.ui.PublishPdfDialog;
import org.rstudio.studio.client.workbench.views.source.events.SourceFileSavedEvent;
import org.rstudio.studio.client.workbench.views.source.model.CheckForExternalEditResult;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.ArrayList;
import java.util.HashSet;

public class TextEditingTarget implements EditingTarget
{
   interface MyCommandBinder
         extends CommandBinder<Commands, TextEditingTarget>
   {
   }

   private static final MyCommandBinder commandBinder =
         GWT.create(MyCommandBinder.class);

   public interface Display extends HasEnsureVisibleHandlers
   {

      void adaptToFileType(TextFileType fileType);
      HasValue<Boolean> getSourceOnSave();
      void ensureVisible();
      void showWarningBar(String warning);
      void hideWarningBar();
      void showFindReplace();
      void onActivate();
      void setFontSize(double size);
      StatusBar getStatusBar();

      boolean isAttached();
   }

   public interface DocDisplay extends HasValueChangeHandlers<Void>,
                                       Widgetable,
                                       HasFocusHandlers,
                                       HasKeyDownHandlers
   {

      void setFileType(TextFileType fileType);
      String getCode();
      void setCode(String code, boolean preserveCursorPosition);
      void insertCode(String code, boolean blockMode);
      void focus();
      void print();
      String getSelectionValue();
      String getCurrentLine();
      void replaceSelection(String code);
      boolean moveSelectionToNextLine(boolean skipBlankLines);
      ChangeTracker getChangeTracker();

      void fitSelectionToLines(boolean expand);
      int getSelectionOffset(boolean start);

      // Fix bug 964
      void onActivate();

      void setFontSize(double size);

      void onVisibilityChanged(boolean visible);

      void setHighlightSelectedLine(boolean on);
      void setShowLineNumbers(boolean on);
      void setUseSoftTabs(boolean on);
      void setTabSize(int tabSize);
      void setShowPrintMargin(boolean on);
      void setPrintMarginColumn(int column);

      HandlerRegistration addCursorChangedHandler(CursorChangedHandler handler);
      Position getCursorPosition();
      void setCursorPosition(Position position);
      void moveCursorNearTop();

      FunctionStart getCurrentFunction();
      JsArray<FunctionStart> getFunctionTree();
   }

   private class SaveProgressIndicator implements ProgressIndicator
   {

      public SaveProgressIndicator(FileSystemItem file,
                                   TextFileType fileType,
                                   Command executeOnSuccess)
      {
         file_ = file;
         newFileType_ = fileType;
         executeOnSuccess_ = executeOnSuccess;
      }

      public void onProgress(String message)
      {
      }

      public void onCompleted()
      {
         if (newFileType_ != null)
            fileType_ = newFileType_;

         if (file_ != null)
         {
            ignoreDeletes_ = false;
            commands_.reopenSourceDocWithEncoding().setEnabled(true);
            name_.setValue(file_.getName(), true);
            // Make sure tooltip gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            dirtyState_.setValue(false, true);
         }

         if (newFileType_ != null)
         {
            // Make sure the icon gets updated, even if name hasn't changed
            name_.fireChangeEvent();
            updateStatusBarLanguage();
            view_.adaptToFileType(newFileType_);
            events_.fireEvent(new FileTypeChangedEvent());
            if (!fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave())
            {
               view_.getSourceOnSave().setValue(false, true);
            }
         }

         if (executeOnSuccess_ != null)
            executeOnSuccess_.execute();
      }

      public void onError(String message)
      {
         globalDisplay_.showErrorMessage(
               "Error Saving File",
               message);
      }

      private final FileSystemItem file_;

      private final TextFileType newFileType_;
      private final Command executeOnSuccess_;
   }

   @Inject
   public TextEditingTarget(Commands commands,
                            SourceServerOperations server,
                            WorkbenchServerOperations server2,
                            EventBus events,
                            GlobalDisplay globalDisplay,
                            FileDialogs fileDialogs,
                            FileTypeRegistry fileTypeRegistry,
                            Provider<PublishPdf> pPublishPdf,
                            Session session,
                            FontSizeManager fontSizeManager,
                            DocDisplay docDisplay,
                            UIPrefs prefs)
   {
      commands_ = commands;
      server_ = server;
      server2_ = server2;
      events_ = events;
      globalDisplay_ = globalDisplay;
      fileDialogs_ = fileDialogs;
      fileTypeRegistry_ = fileTypeRegistry;
      session_ = session;
      fontSizeManager_ = fontSizeManager;
      pPublishPdf_ = pPublishPdf;

      docDisplay_ = docDisplay;
      prefs_ = prefs;
      docDisplay_.addKeyDownHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            NativeEvent ne = event.getNativeEvent();
            int mod = KeyboardShortcut.getModifierValue(ne);
            if ((mod == KeyboardShortcut.META || mod == KeyboardShortcut.CTRL)
                && ne.getKeyCode() == 'F')
            {
               event.preventDefault();
               event.stopPropagation();
               commands_.findReplace().execute();
            }
            else if (mod == KeyboardShortcut.ALT
                     && ne.getKeyCode() == 189) // hyphen
            {
               event.preventDefault();
               event.stopPropagation();
               docDisplay_.insertCode(" <- ", false);
            }
            else if (mod == KeyboardShortcut.CTRL
                     && ne.getKeyCode() == KeyCodes.KEY_UP
                     && fileType_ == FileTypeRegistry.R)
            {
               event.preventDefault();
               event.stopPropagation();
               jumpToPreviousFunction();
            }
            else if (mod == KeyboardShortcut.CTRL
                     && ne.getKeyCode() == KeyCodes.KEY_DOWN
                     && fileType_ == FileTypeRegistry.R)
            {
               event.preventDefault();
               event.stopPropagation();
               jumpToNextFunction();
            }
         }
      });
   }

   private void jumpToPreviousFunction()
   {
      Position cursor = docDisplay_.getCursorPosition();
      JsArray<FunctionStart> functions = docDisplay_.getFunctionTree();
      FunctionStart jumpTo = findPreviousFunction(functions, cursor);
      docDisplay_.setCursorPosition(jumpTo.getStart());
      docDisplay_.moveCursorNearTop();
   }

   private FunctionStart findPreviousFunction(JsArray<FunctionStart> funcs, Position pos)
   {
      FunctionStart result = null;
      for (int i = 0; i < funcs.length(); i++)
      {
         FunctionStart child = funcs.get(i);
         if (child.getStart().compareTo(pos) >= 0)
            break;
         result = child;
      }

      if (result == null)
         return result;

      FunctionStart descendant = findPreviousFunction(result.getChildren(),
                                                      pos);
      if (descendant != null)
         result = descendant;

      return result;
   }

   private void jumpToNextFunction()
   {
      Position cursor = docDisplay_.getCursorPosition();
      JsArray<FunctionStart> functions = docDisplay_.getFunctionTree();
      FunctionStart jumpTo = findNextFunction(functions, cursor);
      docDisplay_.setCursorPosition(jumpTo.getStart());
      docDisplay_.moveCursorNearTop();
   }

   private FunctionStart findNextFunction(JsArray<FunctionStart> funcs, Position pos)
   {
      for (int i = 0; i < funcs.length(); i++)
      {
         FunctionStart child = funcs.get(i);
         if (child.getStart().compareTo(pos) <= 0)
         {
            FunctionStart descendant = findNextFunction(child.getChildren(), pos);
            if (descendant != null)
               return descendant;
         }
         else
         {
            return child;
         }
      }

      return null;
   }

   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      id_ = document.getId();
      fileContext_ = fileContext;
      fileType_ = (TextFileType) type;
      view_ = new TextEditingTargetWidget(commands_,
                                          docDisplay_,
                                          fileType_,
                                          events_);
      docUpdateSentinel_ = new DocUpdateSentinel(
            server_,
            docDisplay_,
            document,
            globalDisplay_.getProgressIndicator("Save File"),
            dirtyState_,
            events_);

      name_.setValue(getNameFromDocument(document, defaultNameProvider), true);
      docDisplay_.setCode(document.getContents(), false);

      registerPrefs();

      // Initialize sourceOnSave, and keep it in sync
      view_.getSourceOnSave().setValue(document.sourceOnSave(), false);
      view_.getSourceOnSave().addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            docUpdateSentinel_.setSourceOnSave(
                  event.getValue(),
                  globalDisplay_.getProgressIndicator("Error Saving Setting"));
         }
      });

      dirtyState_.setValue(document.isDirty(), false);
      docDisplay_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> event)
         {
            dirtyState_.setValue(true, true);
         }
      });

      docDisplay_.addFocusHandler(new FocusHandler()
      {
         public void onFocus(FocusEvent event)
         {
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
            {
               public boolean execute()
               {
                  if (view_.isAttached())
                     checkForExternalEdit();
                  return false;
               }
            }, 500);
         }
      });

      if (fileType_.canCompilePDF()
          && !session_.getSessionInfo().isTexInstalled())
      {
         server_.isTexInstalled(new ServerRequestCallback<Boolean>()
         {
            @Override
            public void onResponseReceived(Boolean response)
            {
               if (!response)
               {
                  String warning;
                  if (Desktop.isDesktop())
                     warning = "No TeX installation detected. Please install " +
                               "TeX before compiling.";
                  else
                     warning = "This server does not have TeX installed. You " +
                               "may not be able to compile.";
                  view_.showWarningBar(warning);
               }
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }

      releaseOnDismiss_.add(events_.addHandler(
            ChangeFontSizeEvent.TYPE,
            new ChangeFontSizeHandler()
            {
               public void onChangeFontSize(ChangeFontSizeEvent event)
               {
                  view_.setFontSize(event.getFontSize());
               }
            }));
      view_.setFontSize(fontSizeManager_.getSize());

      final String rTypeId = FileTypeRegistry.R.getTypeId();
      releaseOnDismiss_.add(prefs_.softWrapRFiles().addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               public void onValueChange(ValueChangeEvent<Boolean> evt)
               {
                  if (fileType_.getTypeId().equals(rTypeId))
                     view_.adaptToFileType(fileType_);
               }
            }
      ));

      initStatusBar();
   }

   private void initStatusBar()
   {
      statusBar_ = view_.getStatusBar();
      docDisplay_.addCursorChangedHandler(new CursorChangedHandler()
      {
         public void onCursorChanged(CursorChangedEvent event)
         {
            updateStatusBarPosition();
         }
      });
      updateStatusBarPosition();
      updateStatusBarLanguage();

      statusBarFileTypes_ = new TextFileType[] {
            FileTypeRegistry.R,
            FileTypeRegistry.TEXT,
            FileTypeRegistry.SWEAVE,
            FileTypeRegistry.RD,
            FileTypeRegistry.TEX,
      };

      for (TextFileType fileType : statusBarFileTypes_)
      {
         statusBar_.getLanguage().addOptionValue(fileType.getLabel());
      }

      statusBar_.getLanguage().addSelectionHandler(new SelectionHandler<String>()
      {
         public void onSelection(SelectionEvent<String> event)
         {
            String item = event.getSelectedItem();
            for (TextFileType fileType : statusBarFileTypes_)
            {
               if (fileType.getLabel().equals(item))
               {
                  docUpdateSentinel_.changeFileType(
                        fileType.getTypeId(),
                        new SaveProgressIndicator(null, fileType, null));
                  break;
               }
            }
         }
      });

      statusBar_.getFunction().addMouseDownHandler(new MouseDownHandler()
      {
         public void onMouseDown(MouseDownEvent event)
         {
            // Unlike the other status bar elements, the function outliner
            // needs its menu built on demand
            JsArray<FunctionStart> tree = docDisplay_.getFunctionTree();
            final StatusBarPopupMenu menu = new StatusBarPopupMenu();
            menu.getElement().getStyle().setProperty(
                  "fontFamily", ThemeFonts.getFixedWidthFont());
            MenuItem defaultItem = addFunctionsToMenu(
                  menu, tree, "", docDisplay_.getCurrentFunction());
            if (defaultItem != null)
            {
               menu.selectItem(defaultItem);
               Scheduler.get().scheduleFinally(new RepeatingCommand()
               {
                  public boolean execute()
                  {
                     menu.ensureSelectedIsVisible();
                     return false;
                  }
               });
            }
            menu.showRelativeToUpward((UIObject) statusBar_.getFunction());
         }
      });
   }

   private MenuItem addFunctionsToMenu(StatusBarPopupMenu menu,
                                       final JsArray<FunctionStart> funcs,
                                       String indent,
                                       FunctionStart defaultFunction)
   {
      MenuItem defaultMenuItem = null;

      for (int i = 0; i < funcs.length(); i++)
      {
         final FunctionStart func = funcs.get(i);
         SafeHtmlBuilder labelBuilder = new SafeHtmlBuilder();
         labelBuilder.appendHtmlConstant(indent);
         labelBuilder.appendEscaped(func.getLabel());

         final MenuItem menuItem = new MenuItem(
               labelBuilder.toSafeHtml(),
               new Command()
               {
                  public void execute()
                  {
                     docDisplay_.setCursorPosition(func.getStart());
                     docDisplay_.moveCursorNearTop();
                     docDisplay_.focus();
                  }
               });
         menu.addItem(menuItem);

         if (defaultFunction != null && defaultMenuItem == null &&
             func.getLabel().equals(defaultFunction.getLabel()) &&
             func.getStart().getRow() == defaultFunction.getStart().getRow() &&
             func.getStart().getColumn() == defaultFunction.getStart().getColumn())
         {
            defaultMenuItem = menuItem;
         }

         MenuItem childDefaultMenuItem = addFunctionsToMenu(
               menu,
               func.getChildren(),
               indent + "&nbsp;&nbsp;",
               defaultMenuItem == null ? defaultFunction : null);
         if (childDefaultMenuItem != null)
            defaultMenuItem = childDefaultMenuItem;
      }

      return defaultMenuItem;
   }

   private void updateStatusBarLanguage()
   {
      statusBar_.getLanguage().setValue(fileType_.getLabel());
      boolean isR = fileType_ == FileTypeRegistry.R;
      statusBar_.setFunctionVisible(isR);
      if (isR)
         updateCurrentFunction();
   }

   private void updateStatusBarPosition()
   {
      Position pos = docDisplay_.getCursorPosition();
      statusBar_.getPosition().setValue((pos.getRow() + 1) + ":" +
                                        (pos.getColumn() + 1));
      updateCurrentFunction();
   }

   private void updateCurrentFunction()
   {
      Scheduler.get().scheduleFinally(
            new RepeatingCommand()
            {
               public boolean execute()
               {
                  FunctionStart function = docDisplay_.getCurrentFunction();
                  String label = function != null
                                ? function.getLabel()
                                : null;
                  statusBar_.getFunction().setValue(label);
                  return false;
               }
            });
   }

   private void registerPrefs()
   {
      releaseOnDismiss_.add(prefs_.highlightSelectedLine().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay_.setHighlightSelectedLine(arg);
               }}));
      releaseOnDismiss_.add(prefs_.showLineNumbers().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay_.setShowLineNumbers(arg);
               }}));
      releaseOnDismiss_.add(prefs_.useSpacesForTab().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay_.setUseSoftTabs(arg);
               }}));
      releaseOnDismiss_.add(prefs_.numSpacesForTab().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay_.setTabSize(arg);
               }}));
      releaseOnDismiss_.add(prefs_.showMargin().bind(
            new CommandWithArg<Boolean>() {
               public void execute(Boolean arg) {
                  docDisplay_.setShowPrintMargin(arg);
               }}));
      releaseOnDismiss_.add(prefs_.printMarginColumn().bind(
            new CommandWithArg<Integer>() {
               public void execute(Integer arg) {
                  docDisplay_.setPrintMarginColumn(arg);
               }}));
   }

   private String getNameFromDocument(SourceDocument document,
                                      Provider<String> defaultNameProvider)
   {
      if (document.getPath() != null)
         return FileSystemItem.getNameFromPath(document.getPath());

      String name = document.getProperties().getString("tempName");
      if (!StringUtil.isNullOrEmpty(name))
         return name;

      String defaultName = defaultNameProvider.get();
      docUpdateSentinel_.setProperty("tempName", defaultName, null);
      return defaultName;
   }

   public long getFileSizeLimit()
   {
      return 5 * 1024 * 1024;
   }

   public long getLargeFileSize()
   {
      return Desktop.isDesktop() ? 1024 * 1024 : 512 * 1024;
   }

   public void insertCode(String source, boolean blockMode)
   {
      docDisplay_.insertCode(source, blockMode);
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      return fileType_.getSupportedCommands(commands_);
   }

   public void focus()
   {
      docDisplay_.focus();
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return view_.addEnsureVisibleHandler(handler);
   }

   public HandlerRegistration addCloseHandler(CloseHandler<java.lang.Void> handler)
   {
      return handlers_.addHandler(CloseEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public void onActivate()
   {
      // If we're already hooked up for some reason, unhook. 
      // This shouldn't happen though.
      if (commandHandlerReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandHandlerReg_.removeHandler();
         commandHandlerReg_ = null;
      }
      commandHandlerReg_ = commandBinder.bind(commands_, this);

      Scheduler.get().scheduleFinally(new ScheduledCommand()
      {
         public void execute()
         {
            // This has to be executed in a scheduleFinally because
            // Source.manageCommands gets called after this.onActivate,
            // and if we're going from a non-editor (like data view) to
            // an editor, setEnabled(true) will be called on the command
            // in manageCommands. 
            commands_.reopenSourceDocWithEncoding().setEnabled(
                  docUpdateSentinel_.getPath() != null);
         }
      });

      view_.onActivate();
   }

   public void onDeactivate()
   {
      externalEditCheckInvalidation_.invalidate();

      commandHandlerReg_.removeHandler();
      commandHandlerReg_ = null;
   }

   public boolean onBeforeDismiss()
   {
      promptForSave(new Command() {
         public void execute()
         {
            CloseEvent.fire(TextEditingTarget.this, null);
         }
      });
      return false;
   }

   private void promptForSave(final Command command)
   {
      if (dirtyState_.getValue())
      {
         view_.ensureVisible();
         globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_WARNING,
                         "Unsaved Changes",
                         "This document has unsaved changes!\n\n" +
                         "Do you want to save changes before continuing?",
                         true,
                         new Operation() {
                            public void execute() { saveThenExecute(null, command); }
                         },
                         new Operation() {
                            public void execute() { command.execute(); }
                         },
                         null,
                         "Save",
                         "Don't Save",
                         true);
      }
      else
      {
         command.execute();
      }
   }

   private void saveThenExecute(String encodingOverride, final Command command)
   {
      final String path = docUpdateSentinel_.getPath();
      if (path == null)
      {
         saveNewFile(null, encodingOverride, command);
         return;
      }

      withEncodingRequiredUnlessAscii(
            encodingOverride,
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  docUpdateSentinel_.save(path,
                                          null,
                                          encoding,
                                          new SaveProgressIndicator(
                                                FileSystemItem.createFile(path),
                                                null,
                                                command
                                          ));
               }
            });
         }

   private void saveNewFile(final String suggestedPath,
                            String encodingOverride,
                            final Command executeOnSuccess)
   {
      withEncodingRequiredUnlessAscii(
            encodingOverride,
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  saveNewFileWithEncoding(suggestedPath,
                                          encoding,
                                          executeOnSuccess);
               }
            });
   }

   private void withEncodingRequiredUnlessAscii(
         final String encodingOverride,
         final CommandWithArg<String> command)
   {
      final String encoding = StringUtil.firstNotNullOrEmpty(new String[] {
            encodingOverride,
            docUpdateSentinel_.getEncoding(),
            prefs_.defaultEncoding().getValue()
      });

      if (StringUtil.isNullOrEmpty(encoding))
      {
         if (docUpdateSentinel_.isAscii())
         {
            // Don't bother asking when it's just ASCII
            command.execute(null);
         }
         else
         {
            withChooseEncoding(session_.getSessionInfo().getSystemEncoding(),
                               new CommandWithArg<String>()
            {
               public void execute(String newEncoding)
               {
                  command.execute(newEncoding);
               }
            });
         }
      }
      else
      {
         command.execute(encoding);
      }
   }

   private void withChooseEncoding(final String defaultEncoding,
                                   final CommandWithArg<String> command)
   {
      server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
      {
         @Override
         public void onResponseReceived(IconvListResult response)
         {
            // Stupid compiler. Use this Value shim to make the dialog available
            // in its own handler.
            final HasValue<ChooseEncodingDialog> d = new Value<ChooseEncodingDialog>(null);
            d.setValue(new ChooseEncodingDialog(
                  response.getCommon(),
                  response.getAll(),
                  defaultEncoding,
                  false,
                  true,
                  new OperationWithInput<String>()
                  {
                     public void execute(String newEncoding)
                     {
                        if (newEncoding == null)
                           return;

                        if (d.getValue().isSaveAsDefault())
                        {
                           prefs_.defaultEncoding().setValue(newEncoding);
                           server2_.setUiPrefs(
                                 session_.getSessionInfo().getUiPrefs(), 
                                 new SimpleRequestCallback<Void>("Error Saving Preference"));
                        }

                        command.execute(newEncoding);
                     }
                  }));
            d.getValue().showModal();
         }
      });

   }

   private void saveNewFileWithEncoding(String suggestedPath,
                                        final String encoding,
                                        final Command executeOnSuccess)
   {
      FileSystemItem fsi = suggestedPath != null
                           ? FileSystemItem.createFile(suggestedPath)
                           : null;
      fileDialogs_.saveFile(
            "Save File",
            fileContext_,
            fsi,
            new FilenameTransform()
            {
               public String transform(String filename)
               {
                  // if there is no extension then we need to add one
                  String ext = FileSystemItem.getExtensionFromPath(filename);
                  if (ext.length() == 0)
                     return filename + fileType_.getDefaultExtension();
                  else
                     return filename;
               }
            },
            new ProgressOperationWithInput<FileSystemItem>()
            {
               public void execute(final FileSystemItem input,
                                   ProgressIndicator indicator)
               {
                  if (input == null)
                     return;

                  try
                  {
                     FileSystemItem saveItem = input;
                     TextFileType fileType =
                           fileTypeRegistry_.getTextTypeForFile(saveItem);

                     docUpdateSentinel_.save(
                           saveItem.getPath(),
                           fileType.getTypeId(),
                           encoding,
                           new SaveProgressIndicator(saveItem,
                                                             fileType,
                                                             executeOnSuccess));

                     events_.fireEvent(
                           new SourceFileSavedEvent(saveItem.getPath()));
                  }
                  catch (Exception e)
                  {
                     indicator.onError(e.toString());
                     return;
                  }

                  indicator.onCompleted();
               }
            });
   }


   public void onDismiss()
   {
      docUpdateSentinel_.stop();
      
      removePublishPdfHandler();

      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();
   }

   public HasValue<Boolean> dirtyState()
   {
      return dirtyState_;
   }

   public Widget toWidget()
   {
      return (Widget) view_;
   }

   public String getId()
   {
      return id_;
   }

   public HasValue<String> getName()
   {
      return name_; 
   }

   public String getPath()
   {
      return docUpdateSentinel_.getPath();
   }

   public ImageResource getIcon()
   {
      return fileType_.getDefaultIcon();
   }

   public String getTabTooltip()
   {
      return getPath();
   }

   @Handler
   void onReopenSourceDocWithEncoding()
   {
      withChooseEncoding(
            docUpdateSentinel_.getEncoding(),
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  docUpdateSentinel_.reopenWithEncoding(encoding);
               }
            });
   }

   @Handler
   void onSaveSourceDoc()
   {
      saveThenExecute(null, sourceOnSaveCommandIfApplicable());
   }

   @Handler
   void onSaveSourceDocAs()
   {
      saveNewFile(docUpdateSentinel_.getPath(),
                  null,
                  sourceOnSaveCommandIfApplicable());
   }

   @Handler
   void onSaveSourceDocWithEncoding()
   {
      withChooseEncoding(
            StringUtil.firstNotNullOrEmpty(new String[] {
                  docUpdateSentinel_.getEncoding(),
                  session_.getSessionInfo().getSystemEncoding()
            }),
            new CommandWithArg<String>()
            {
               public void execute(String encoding)
               {
                  saveThenExecute(encoding, sourceOnSaveCommandIfApplicable());
               }
            });
   }

   @Handler
   void onPrintSourceDoc()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            docDisplay_.print();
         }
      });
   }

   @Handler
   void onExtractFunction()
   {
      // Stops the console from thinking it has focus, and thus stealing it
      Element activeEl = DomUtils.getActiveElement();
      if (activeEl != null)
         activeEl.blur();
      docDisplay_.focus();

      String initialSelection = docDisplay_.getSelectionValue();
      if (initialSelection == null || initialSelection.trim().length() == 0)
      {
         globalDisplay_.showErrorMessage("Extract Function",
                                         "Please select the code to extract " +
                                         "into a function.");
         return;
      }

      docDisplay_.fitSelectionToLines(false);

      final String code = docDisplay_.getSelectionValue();
      if (code == null || code.trim().length() == 0)
      {
         globalDisplay_.showErrorMessage("Extract Function",
                                         "Please select the code to extract " +
                                         "into a function.");
         return;
      }

      Pattern leadingWhitespace = Pattern.create("^(\\s*)");
      Match match = leadingWhitespace.match(code, 0);
      final String indentation = match == null ? "" : match.getGroup(1);

      server_.detectFreeVars(code, new ServerRequestCallback<JsArrayString>()
      {
         @Override
         public void onResponseReceived(final JsArrayString response)
         {
            doExtract(response);
         }

         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showYesNoMessage(
                  GlobalDisplay.MSG_WARNING,
                  "Extract Function",
                  "The selected code could not be " +
                  "parsed.\n\n" +
                  "Are you sure you want to continue?",
                  new Operation()
                  {
                     public void execute()
                     {
                        doExtract(null);
                     }
                  },
                  false);
         }

         private void doExtract(final JsArrayString response)
         {
            globalDisplay_.promptForText(
                  "Extract Function",
                  "Function Name",
                  "",
                  new OperationWithInput<String>()
                  {
                     public void execute(String input)
                     {
                        String prefix = docDisplay_.getSelectionOffset(true) == 0
                              ? "" : "\n";
                        String args = response != null ? response.join(", ")
                                                       : "";
                        docDisplay_.replaceSelection(
                              prefix
                              + indentation
                              + input.trim()
                              + " <- "
                              + "function (" + args + ") {\n"
                              + StringUtil.indent(code, "  ")
                              + "\n"
                              + indentation
                              + "}");
                     }
                  });
         }
      });
   }

   @Handler
   void onCommentUncomment()
   {
      docDisplay_.fitSelectionToLines(true);
      String selection = docDisplay_.getSelectionValue();

      // If any line's first non-whitespace character is not #, then the whole
      // selection should be commented. Exception: If the whole selection is
      // whitespace, then we comment out the whitespace.
      Match match = Pattern.create("^\\s*[^#\\s]").match(selection, 0);
      boolean uncomment = match == null && selection.trim().length() != 0;
      if (uncomment)
         selection = selection.replaceAll("((^|\\n)\\s*)# ?", "$1");
      else
      {
         selection = "# " + selection.replaceAll("\\n", "\n# ");

         // If the selection ends at the very start of a line, we don't want
         // to comment out that line. This enables Shift+DownArrow to select
         // one line at a time.
         if (selection.endsWith("\n# "))
            selection = selection.substring(0, selection.length() - 2);
      }

      docDisplay_.replaceSelection(selection);
   }

   @Handler
   void onExecuteCode()
   {
      // Stops the console from thinking it has focus, and thus stealing it
      Element activeEl = DomUtils.getActiveElement();
      if (activeEl != null)
         activeEl.blur();
      docDisplay_.focus();

      boolean focusConsole = false;
      String code = docDisplay_.getSelectionValue();
      if (code == null || code.length() == 0)
      {
         code = docDisplay_.getCurrentLine();
         docDisplay_.moveSelectionToNextLine(true);
      }
      else
      {
         focusConsole = true;
      }

      if (code != null && code.trim().length() > 0)
      {
         events_.fireEvent(new SendToConsoleEvent(code, true));
         if (focusConsole)
            commands_.activateConsole().execute();
      }
   }

   @Handler
   void onExecuteAllCode()
   {
      // Stops the console from thinking it has focus, and thus stealing it
      Element activeEl = DomUtils.getActiveElement();
      if (activeEl != null)
         activeEl.blur();
      docDisplay_.focus();

      String code = docDisplay_.getCode();

      if (fileType_.canCompilePDF())
      {
         code = stangle(code);
      }

      code = code.replaceAll("^[ \t\n]*\n", "");
      code = code.replaceAll("\n[ \t\n]*$", "");

      events_.fireEvent(new SendToConsoleEvent(code, true));
   }

   @Handler
   void onJumpToFunction()
   {
      statusBar_.getFunction().click();
   }

   private static String stangle(String sweaveStr)
   {
      StringBuilder code = new StringBuilder();
      Pattern pStart = Pattern.create("^<<.*>>=", "mg");
      Pattern pNewLine = Pattern.create("\n");
      Pattern pEnd = Pattern.create("\n@");
      int pos = 0;
      Match mStart;
      while (null != (mStart = pStart.match(sweaveStr, pos)))
      {
         Match mNewLine = pNewLine.match(sweaveStr, mStart.getIndex());
         if (mNewLine == null)
            break;

         Match mEnd = pEnd.match(sweaveStr, mNewLine.getIndex() + 1);
         if (mEnd == null)
            break;

         code.append(sweaveStr, mNewLine.getIndex() + 1, mEnd.getIndex() + 1);
         pos = mEnd.getIndex() + 2;
      }
      sweaveStr = code.toString();
      return sweaveStr;
   }

   @Handler
   void onSourceActiveDocument()
   {
      // Stops the console from thinking it has focus, and thus stealing it
      Element activeEl = DomUtils.getActiveElement();
      if (activeEl != null)
         activeEl.blur();

      String code = docDisplay_.getCode();
      if (code != null && code.trim().length() > 0)
      {
         boolean sweave = fileType_.canCompilePDF();

         server_.saveActiveDocument(code, sweave, new SimpleRequestCallback<Void>() {
            @Override
            public void onResponseReceived(Void response)
            {
               events_.fireEvent(new SendToConsoleEvent(
                     createSourceCommand("~/.active-rstudio-document", "UTF-8"),
                     true));
            }
         });
      }
   }

   private String normalizeEncoding(String str)
   {
      return StringUtil.notNull(str).replaceAll("[- ]", "").toLowerCase();
   }

   private String createSourceCommand(String path, String encoding)
   {
      boolean isAscii = true;
      String code = docDisplay_.getCode();
      for (int i = 0; i < code.length(); i++)
      {
         if (code.charAt(i) > 127)
         {
            isAscii = false;
            break;
         }
      }

      String systemEncoding = session_.getSessionInfo().getSystemEncoding();
      boolean isSystemEncoding =
            normalizeEncoding(encoding).equals(normalizeEncoding(systemEncoding));

      String escapedPath = "'" +
                           path.replace("\\", "\\\\").replace("'", "\\'") +
                           "'";

      if (isAscii || isSystemEncoding)
         return "source(" + escapedPath + ")";
      else
      {
         return "source.with.encoding(" + escapedPath + ", encoding='" +
                (!StringUtil.isNullOrEmpty(encoding) ? encoding : "UTF-8") +
                "')";
      }
   }

   @Handler
   void onPublishPDF()
   {   
      // setup handler for PublishPdfEvent
      removePublishPdfHandler();
      publishPdfReg_ = events_.addHandler(PublishPdfEvent.TYPE,
                                          new PublishPdfHandler() {
         public void onPublishPdf(PublishPdfEvent event)
         {
            // if this event applies to our document then handle it
            if ((docUpdateSentinel_ != null) &&
                 event.getPath().equals(docUpdateSentinel_.getPath()))
            {
               PublishPdfDialog pdfDialog = new PublishPdfDialog();
               pdfDialog.showModal();
               PublishPdf publishPdf = pPublishPdf_.get();
               publishPdf.publish(id_, 
                                  docDisplay_, 
                                  docUpdateSentinel_, 
                                  pdfDialog);
            }


         }
      });
      
      // send publish to console
      handlePdfCommand("publishPdf");
   }
   
   private void removePublishPdfHandler()
   {
      if (publishPdfReg_ != null)
      {
         publishPdfReg_.removeHandler();
         publishPdfReg_ = null;
      }
   }

   @Handler
   void onCompilePDF()
   {
      handlePdfCommand("compilePdf");
   }

   @Handler
   void onFindReplace()
   {
      view_.showFindReplace();
   }
   
   void handlePdfCommand(final String function)
   {
      saveThenExecute(null, new Command() {
         public void execute()
         {
            String path = docUpdateSentinel_.getPath();
            if (path != null)
               sendPdfFunctionToConsole(function, path);
         }  
      });
   }
   
   private void sendPdfFunctionToConsole(String function, String path)
   {
      // first validate the path to make sure it doesn't contain spaces
      FileSystemItem file = FileSystemItem.createFile(path);
      if (file.getName().indexOf(' ') != -1)
      {
         globalDisplay_.showErrorMessage(
               "Invalid Filename",
               "The file '" + file.getName() + "' cannot be compiled to " +
               "a PDF because TeX does not understand paths with spaces. " +
               "If you rename the file to remove spaces then " +
               "PDF compilation will work correctly.");
        
         return;
      }
      
      // build the SendToConsoleEvent and fire it
      String code = function + "(\"" + path + "\")";
      final SendToConsoleEvent event = new SendToConsoleEvent(code, true);
      events_.fireEvent(event);
   }
   
   private Command sourceOnSaveCommandIfApplicable()
   {
      return new Command()
      {
         public void execute()
         {
            if (fileType_.canSourceOnSave() && docUpdateSentinel_.sourceOnSave())
            {
               String path = docUpdateSentinel_.getPath();
               String code = createSourceCommand(
                     path, docUpdateSentinel_.getEncoding());
               events_.fireEvent(new SendToConsoleEvent(code, true));
            }
         }
      };
   }

   public void checkForExternalEdit()
   {
      if (externalEditCheckInterval_.hasElapsed())
         return;
      externalEditCheckInterval_.reset();

      externalEditCheckInvalidation_.invalidate();
      final Token token = externalEditCheckInvalidation_.getInvalidationToken();

      server_.checkForExternalEdit(
            id_,
            new ServerRequestCallback<CheckForExternalEditResult>()
            {
               @Override
               public void onResponseReceived(CheckForExternalEditResult response)
               {
                  if (token.isInvalid())
                     return;
                  
                  if (response.isDeleted())
                  {
                     if (ignoreDeletes_)
                        return;

                     globalDisplay_.showYesNoMessage(
                           GlobalDisplay.MSG_WARNING,
                           "File Deleted",
                           "The file " + name_.getValue() + " has been " +
                           "deleted. Do you want to close this file now?",
                           false,
                           new Operation()
                           {
                              public void execute()
                              {
                                 CloseEvent.fire(TextEditingTarget.this, null);
                              }
                           },
                           new Operation()
                           {
                              public void execute()
                              {
                                 externalEditCheckInterval_.reset();
                                 ignoreDeletes_ = true;
                                 dirtyState_.setValue(true, true);
                              }
                           },
                           false);
                  }
                  else if (response.isModified())
                  {
                     ignoreDeletes_ = false; // Now we know it exists

                     // Use StringUtil.formatDate(response.getLastModified())?

                     if (!dirtyState_.getValue())
                     {
                        docUpdateSentinel_.revert();
                     }
                     else
                     {
                        externalEditCheckInterval_.reset();
                        globalDisplay_.showYesNoMessage(
                              GlobalDisplay.MSG_WARNING,
                              "File Changed",
                              "The file " + name_.getValue() + " has changed " +
                              "on disk. Do you want to reload the file from " +
                              "disk and discard your unsaved changes?",
                              false,
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    docUpdateSentinel_.revert();
                                 }
                              },
                              new Operation()
                              {
                                 public void execute()
                                 {
                                    externalEditCheckInterval_.reset();
                                    docUpdateSentinel_.ignoreExternalEdit();
                                    // Should already be dirty, but whatever,
                                    // we'll just make extra sure.
                                    dirtyState_.setValue(true, true);
                                 }
                              },
                              true);
                     }
                  }
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }

   private StatusBar statusBar_;
   private TextFileType[] statusBarFileTypes_;
   private DocDisplay docDisplay_;
   private final UIPrefs prefs_;
   private Display view_;
   private final Commands commands_;
   private SourceServerOperations server_;
   private final WorkbenchServerOperations server2_;
   private EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final FileDialogs fileDialogs_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private final FontSizeManager fontSizeManager_;
   private DocUpdateSentinel docUpdateSentinel_;
   private Value<String> name_ = new Value<String>(null);
   private TextFileType fileType_;
   private String id_;
   private HandlerRegistration commandHandlerReg_;
   private HandlerRegistration publishPdfReg_;
   private ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
   private final Value<Boolean> dirtyState_ = new Value<Boolean>(false);
   private HandlerManager handlers_ = new HandlerManager(this);
   private FileSystemContext fileContext_;
   private final Provider<PublishPdf> pPublishPdf_;
   private boolean ignoreDeletes_;

   // Allows external edit checks to supercede one another
   private final Invalidation externalEditCheckInvalidation_ =
         new Invalidation();
   // Prevents external edit checks from happening too soon after each other
   private final IntervalTracker externalEditCheckInterval_ =
         new IntervalTracker(1000, true);
}
