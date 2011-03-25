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
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.HasValue;
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
import org.rstudio.core.client.widget.*;
import org.rstudio.core.client.widget.FontSizer.Size;
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
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.FileTypeChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PublishPdfEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PublishPdfHandler;
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
      void setFontSize(FontSizer.Size size);

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
      boolean moveSelectionToNextLine();
      ChangeTracker getChangeTracker();

      void fitSelectionToLines(boolean expand);
      int getSelectionOffset(boolean start);

      // Fix bug 964
      void onActivate();

      void setFontSize(Size size);

      void onVisibilityChanged(boolean visible);

      void setHighlightSelectedLine(boolean on);
      void setShowLineNumbers(boolean on);
      void setUseSoftTabs(boolean on);
      void setTabSize(int tabSize);
      void setShowPrintMargin(boolean on);
      void setPrintMarginColumn(int column);
   }
   private class ExplicitSaveProgressIndicator implements ProgressIndicator
   {

      public ExplicitSaveProgressIndicator(FileSystemItem file,
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
         ignoreDeletes_ = false;
         name_.setValue(file_.getName(), true);
         // Make sure tooltip gets updated, even if name hasn't changed
         name_.fireChangeEvent(); 
         dirtyState_.setValue(false, true);
         if (newFileType_ != null)
         {
            fileType_ = newFileType_;
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
         }
      });
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
                            public void execute() { saveThenExecute(command); }
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

   private void saveThenExecute(Command command)
   {
      String path = docUpdateSentinel_.getPath();
      if (path == null)
      {
         saveNewFile(null, command);
         return;
      }

      docUpdateSentinel_.save(path,
                              null,
                              new ExplicitSaveProgressIndicator(
                                    FileSystemItem.createFile(path),
                                    null,
                                    command
                              ));
   }

   private void saveNewFile(String suggestedPath,
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
                           new ExplicitSaveProgressIndicator(saveItem,
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
      String path = docUpdateSentinel_.getPath();
      if (path != null)
         return fileTypeRegistry_.getIconForFile(FileSystemItem.createFile(path));
      else
         return fileType_.getDefaultIcon();
   }

   public String getTabTooltip()
   {
      return getPath();
   }

   @Handler
   void onReopenSourceDocWithEncoding()
   {
      server_.iconvlist(new SimpleRequestCallback<IconvListResult>()
      {
         @Override
         public void onResponseReceived(IconvListResult response)
         {
            String currentEncoding = docUpdateSentinel_.getEncoding();

            new ChooseEncodingDialog(
                  response.getCommon(), response.getAll(), currentEncoding,
                  new OperationWithInput<String>()
                  {
                     public void execute(String encoding)
                     {
                        if (encoding == null)
                           return;

                        docUpdateSentinel_.reopenWithEncoding(encoding);
                     }
                  }).showModal();
         }
      });
   }

   @Handler
   void onSaveSourceDoc()
   {
      saveThenExecute(sourceOnSaveCommandIfApplicable());
   }

   @Handler
   void onSaveSourceDocAs()
   {
      saveNewFile(docUpdateSentinel_.getPath(),
                  sourceOnSaveCommandIfApplicable());
   }

   @Handler
   void onPrintSourceDoc()
   {
      DeferredCommand.addCommand(new Command()
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
         docDisplay_.moveSelectionToNextLine();
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

      String code = docDisplay_.getCode();
      if (code != null && code.trim().length() > 0)
      {
         boolean sweave = fileType_.canCompilePDF();

         server_.saveActiveDocument(code, sweave, new SimpleRequestCallback<Void>() {
            @Override
            public void onResponseReceived(Void response)
            {
               events_.fireEvent(new SendToConsoleEvent(
                                             "source(\"~/.active.document\")",
                                             true));
            }
         });
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
      saveThenExecute(new Command() {
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
               String code = "source('"
                             + path.replace("\\", "\\\\").replace("'", "\\'")
                             + "')";
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

   private DocDisplay docDisplay_;
   private final UIPrefs prefs_;
   private Display view_;
   private final Commands commands_;
   private SourceServerOperations server_;
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
