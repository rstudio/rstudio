/*
 * FileSystemDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.files.filedialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.aria.client.DialogRole;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

public abstract class FileSystemDialog extends ModalDialogBase
      implements ProgressIndicator,
                 FileSystemContext.Callbacks,
                 FileBrowserWidget.Host
{
   private class NewFolderHandler implements ClickHandler,
                                             ProgressOperationWithInput<String>
   {
      public void onClick(ClickEvent event)
      {
         context_.messageDisplay().promptForText(constants_.newFolderTitle(),
                                                 constants_.folderNameLabel(),
                                                 null,
                                                 this);
      }

      public void execute(final String input, final ProgressIndicator progress)
      {
         context_.mkdir(input, new ProgressIndicator()
         {
            public void onProgress(String message)
            {
               onProgress(message, null);
            }

            public void onProgress(String message, Operation onCancel)
            {
               progress.onProgress(message, onCancel);
            }

            public void clearProgress()
            {
               progress.clearProgress();
            }

            public void onCompleted()
            {
               progress.onCompleted();
               context_.cd(input);
            }

            public void onError(String message)
            {
               progress.onError(message);
            }
         });
      }
   }

   static
   {
      FileDialogResources.INSTANCE.styles().ensureInjected();
   }

   public FileSystemDialog(String title,
                           String caption,
                           DialogRole role,
                           String buttonName,
                           FileSystemContext context,
                           String filter,
                           boolean allowFolderCreation,
                           ProgressOperationWithInput<FileSystemItem> operation)
   {
      super(role);
      setThemeAware(true);
      context_ = context;
      operation_ = operation;
      context_.setCallbacks(this);
      filterExtensions_ = extractFilterExtensions(filter);

      setTitle(caption);
      setText(title);

      if (allowFolderCreation)
      {
         addLeftButton(new ThemedButton(constants_.newFolderTitle(), new NewFolderHandler()),
               ElementIds.FILE_NEW_FOLDER_BUTTON);
      }

      ThemedButton okButton = new ThemedButton(buttonName, event -> maybeAccept());
      addOkButton(okButton,
            ElementIds.FILE_ACCEPT_BUTTON + "_" + ElementIds.idSafeString(buttonName));

      ThemedButton cancelButton =
         new ThemedButton(constants_.cancelLabel(),
         event -> {
            if (invokeOperationEvenOnCancel_)
            {
               operation_.execute(null, FileSystemDialog.this);
            }
            closeDialog();
         });
      addCancelButton(cancelButton,
            ElementIds.FILE_CANCEL_BUTTON + "_" + ElementIds.idSafeString(buttonName));

      addDomHandler(event ->
         {
               if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
               {
                  event.stopPropagation();
                  event.preventDefault();
                  maybeAccept();
               }
         },
         KeyDownEvent.getType());

      progress_ = addProgressIndicator();
   }

   /**
    * Subclasses that pass true for the promptOnOverwrite constructor arg
    * must override this method. If the user elects to overwrite, the
    * subclass should call accept() directly (this will cause all validation
    * to be bypassed).
    */
   protected void showOverwritePrompt()
   {
      assert false :
            "Subclasses should override showOvewritePrompt() " +
            "if promptOnOverwrite is true";
   }

   public void onSelectionCommit(SelectionCommitEvent<FileSystemItem> event)
   {
      FileSystemItem item = event.getSelectedItem();
      if (item != null && item.isDirectory())
         browser_.cd(item);
      else
         maybeAccept();
   }

   /**
    * Accept if validation passes. While a directory listing is in flight
    * (the dialog just opened, or a cd() is pending) the accept is deferred
    * until the listing arrives, since the input can only be resolved against
    * the listed directory: accepting against a missing listing produced a
    * null item, which callers treat as a cancel (#18922).
    */
   @Override
   public final void maybeAccept()
   {
      if (browser_.isNavigating())
      {
         acceptOnNavigated_ = true;
         return;
      }

      if (shouldAccept())
         accept();
   }

   protected boolean shouldAccept()
   {
      return true;
   }

   protected void accept()
   {
      accept(getSelectedItem());
   }

   protected void accept(FileSystemItem item)
   {
      operation_.execute(item, this);
   }

   protected abstract FileSystemItem getSelectedItem();

   @Override
   public void showModal()
   {
      super.showModal();
      Scheduler.get().scheduleDeferred(() -> center());
   }

   /**
    * If non-null, the filename textbox will be inserted at the top
    * of the dialog with the given label shown next to it.
    *
    * If null, the filename textbox will not be created.
    */
   @Override
   public abstract String getFilenameLabel();

   /**
    * Set the contents of the filename box
    */
   public void setFilename(String filename)
   {
      if (browser_ != null)
         browser_.setFilename(filename);
      else
         initialFilename_ = filename;
   }

   /**
    * Subclasses that adjust the filename on navigation must do so BEFORE
    * calling super.onNavigated(), which may run a deferred accept.
    */
   @Override
   public void onNavigated()
   {
      browser_.onNavigated();

      if (acceptOnNavigated_)
      {
         acceptOnNavigated_ = false;
         maybeAccept();
      }
   }

   /**
    * Closing the dialog (Cancel, Escape) does not detach it from the context,
    * so a listing that arrives afterwards still reaches onNavigated(). Drop
    * any deferred accept so it cannot run the operation from a closed dialog.
    */
   @Override
   protected void onUnload()
   {
      acceptOnNavigated_ = false;
      super.onUnload();
   }

   @Override
   public void onDirectoryCreated(FileSystemItem directory)
   {
      browser_.onDirectoryCreated(directory);
   }

   @Override
   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN
          && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER)
      {
         // Let directory browser handle its own Enter
         return;
      }
      super.onPreviewNativeEvent(event);    //To change body of overridden methods use File | Settings | File Templates.
   }

   public void onSelection(SelectionEvent<FileSystemItem> event)
   {
   }

   public void onProgress(String message)
   {
      onProgress(message, null);
   }

   public void onProgress(String message, Operation onCancel)
   {
      progress_.onProgress(message);
   }

   public void clearProgress()
   {
      progress_.clearProgress();
   }

   public void onCompleted()
   {
      progress_.onCompleted();
   }

   /**
    * Reports a failed navigation (from the context callbacks) or a failed
    * operation (as the ProgressIndicator passed to it).
    */
   @Override
   public void onError(String errorMessage)
   {
      // a failed navigation leaves the browser in the directory it was already
      // showing: drop any deferred accept and end its loading state, so the
      // dialog stays usable. This bypasses the subclass onNavigated() hooks,
      // which react to arriving somewhere (e.g. by discarding typed input).
      if (browser_.isNavigating())
      {
         acceptOnNavigated_ = false;
         browser_.onNavigated();
      }

      progress_.onError(errorMessage);
   }

   protected void showError(String errorMessage)
   {
      context_.messageDisplay().showErrorMessage(constants_.errorCaption(), errorMessage);
   }

   /**
    * If true, hitting the Cancel button will result in the action operation
    * (passed in the constructor) to be invoked with a null value, rather
    * than having the dialog just close.
    */
   public void setInvokeOperationEvenOnCancel(boolean invoke)
   {
      invokeOperationEvenOnCancel_ = invoke;
   }

   @Override
   public Widget createMainWidget()
   {
      browser_ = new FileBrowserWidget(context_, this);
      if (initialFilename_ != null)
         browser_.setFilename(initialFilename_);
      return browser_;
   }

   @Override
   public FileSystemItem[] ls()
   {
      return listFilesWithExtensions(context_, filterExtensions_);
   }

   public static FileSystemItem[] listFilesWithExtension(
         FileSystemContext context, String extension)
   {
      List<String> filterExtensions = new ArrayList<>();
      if (StringUtil.isNullOrEmpty(extension))
         filterExtensions.add(extension);
      return listFilesWithExtensions(context, filterExtensions);
   }

   private static FileSystemItem[] listFilesWithExtensions(
         FileSystemContext context, List<String> filterExtensions)
   {
      FileSystemItem[] items = context.ls();
      if (items == null)
         return new FileSystemItem[0];

      ArrayList<FileSystemItem> filtered = new ArrayList<>();
      for (int i = 0; i < items.length; i++)
      {
         if (items[i].isDirectory())
            filtered.add(items[i]);
         else if (filterExtensions.isEmpty())
            filtered.add(items[i]);
         else if (extensionMatchesFilters(items[i].getExtension(), filterExtensions))
            filtered.add(items[i]);
      }

      Collections.sort(filtered, (o1, o2) -> o1.compareTo(o2));
      FileSystemItem[] clone = new FileSystemItem[filtered.size()];
      return filtered.toArray(clone);
   }

   // NOTE: web mode only supports a single one-extension filter (whereas
   // desktop mode supports full multi-filetype, multi-extension filtering).
   // to support more sophisticated filtering we'd need to both add the
   // UI as well as update this function to extract a list of filters
   private List<String> extractFilterExtensions(String filter)
   {
      List<String> filters = new ArrayList<>();
      if (!StringUtil.isNullOrEmpty(filter))
      {
         Pattern listPattern = Pattern.create("\\(([^)]+)\\)");
         Pattern singlePattern = Pattern.create("\\*([^\\s;]+)");
         Match listMatch = listPattern.match(filter, 0);
         while (listMatch != null)
         {
            Match singleMatch = singlePattern.match(listMatch.getGroup(1), 0);
            while (singleMatch != null)
            {
               filters.add(singleMatch.getGroup(1));
               singleMatch = singleMatch.nextMatch();
            }
            listMatch = listMatch.nextMatch();
         }
      }
      return filters;
   }

   private static boolean extensionMatchesFilters(String extension, List<String> filterExtensions)
   {
      if (filterExtensions.isEmpty())
         return true;

      for (String filter: filterExtensions)
         if (filter.equalsIgnoreCase(extension))
            return true;

      return false;
   }

   protected final FileSystemContext context_;
   private final ProgressOperationWithInput<FileSystemItem> operation_;
   private List<String> filterExtensions_;
   private boolean invokeOperationEvenOnCancel_;
   private final ProgressIndicator progress_;
   protected FileBrowserWidget browser_;
   protected boolean acceptOnNavigated_ = false;
   private String initialFilename_;
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
