/*
 * DirectoryChooserTextBox.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;

import com.google.gwt.user.client.ui.Focusable;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;

public class DirectoryChooserTextBox extends TextBoxWithButton
{
   public DirectoryChooserTextBox(String label, ElementIds.TextBoxButtonId uniqueId)
   {
      this(label, "", uniqueId, null); 
   }

   public DirectoryChooserTextBox(String label, 
                                  String emptyLabel, 
                                  ElementIds.TextBoxButtonId uniqueId,
                                  Focusable focusAfter)
   {
      this(label, 
           emptyLabel,
           uniqueId,
           focusAfter,
           RStudioGinjector.INSTANCE.getFileDialogs(),
           RStudioGinjector.INSTANCE.getRemoteFileSystemContext());
   }

   public DirectoryChooserTextBox(String label,
                                  ElementIds.TextBoxButtonId uniqueId,
                                  boolean buttonDisabled,
                                  Focusable focusAfter)
   {
      this(label,
            "",
            uniqueId,
            buttonDisabled,
            focusAfter,
            RStudioGinjector.INSTANCE.getFileDialogs(),
            RStudioGinjector.INSTANCE.getRemoteFileSystemContext());
   }

   public DirectoryChooserTextBox(String label, ElementIds.TextBoxButtonId uniqueId, Focusable focusAfter)
   {
      this(label, "", uniqueId, focusAfter);
   }

   public DirectoryChooserTextBox(String label, 
                                  ElementIds.TextBoxButtonId uniqueId,
                                  Focusable focusAfter,
                                  FileDialogs fileDialogs,
                                  FileSystemContext fsContext)
   {
      this(label, "", uniqueId, focusAfter, fileDialogs, fsContext);
   }

   public DirectoryChooserTextBox(String label, 
                                  String emptyLabel,
                                  ElementIds.TextBoxButtonId uniqueId,
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      this(label, emptyLabel, uniqueId, false, focusAfter, fileDialogs, fsContext);
   }

   public DirectoryChooserTextBox(String label,
                                  String emptyLabel,
                                  ElementIds.TextBoxButtonId uniqueId,
                                  boolean buttonDisabled,
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      this(label, emptyLabel, constants_.browseLabel(), uniqueId, buttonDisabled, focusAfter, fileDialogs, fsContext);
   }

   public DirectoryChooserTextBox(String label, 
                                  String emptyLabel,
                                  String browseLabel,
                                  ElementIds.TextBoxButtonId uniqueId,
                                  boolean buttonDisabled,
                                  final Focusable focusAfter,
                                  final FileDialogs fileDialogs,
                                  final FileSystemContext fsContext)
   {
      super(label, emptyLabel, browseLabel, null, uniqueId, true, null);

      if (buttonDisabled)
      {
         getButton().setEnabled(false);
         setReadOnly(false);

         getTextBox().addChangeHandler(event -> setText(getTextBox().getText()));
      }

      addClickHandler(event -> fileDialogs.chooseFolder(
            constants_.directoryLabel(),
            fsContext,
            FileSystemItem.createDir(getText()),
            (input, indicator) ->
            {
               if (input == null)
                  return;

               setText(input.getPath());
               indicator.onCompleted();
               if (focusAfter != null)
                  focusAfter.setFocus(true);
            })
      );
   }
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

}
