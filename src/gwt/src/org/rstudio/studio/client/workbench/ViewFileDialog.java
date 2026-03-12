/*
 * ViewFileDialog.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.core.client.GWT;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ViewFileDialog extends ModalDialogBase
{
   public ViewFileDialog(String filePath)
   {
      super(Roles.getDialogRole());
      filePath_ = filePath;

      FileSystemItem fsi = FileSystemItem.createFile(filePath);
      setText(fsi.getName());

      setThemeAware(true);

      CoreClientConstants constants = GWT.create(CoreClientConstants.class);
      ThemedButton closeButton = new ThemedButton(constants.closeText(), event -> closeDialog());
      addCancelButton(closeButton);
   }

   @Override
   protected Widget createMainWidget()
   {
      editor_ = new AceEditor();
      editor_.setReadOnly(true);

      FileTypeRegistry registry = RStudioGinjector.INSTANCE.getFileTypeRegistry();
      FileSystemItem fsi = FileSystemItem.createFile(filePath_);
      FileType fileType = registry.getTypeForFile(fsi);
      if (fileType instanceof TextFileType)
         editor_.setFileType((TextFileType) fileType);

      SimplePanel panel = new SimplePanel();
      Size size = DomMetrics.adjustedElementSize(new Size(700, 500), null, 70, 100);
      panel.setWidth(size.width + "px");
      panel.setHeight(size.height + "px");
      panel.setWidget(editor_.asWidget());

      RStudioGinjector.INSTANCE.getServer().getFileContents(
         filePath_,
         "UTF-8",
         new ServerRequestCallback<String>()
         {
            @Override
            public void onResponseReceived(String contents)
            {
               editor_.setCode(contents, false);
            }

            @Override
            public void onError(ServerError error)
            {
               editor_.setCode("Error reading file: " + error.getUserMessage(), false);
            }
         });

      return panel;
   }

   private final String filePath_;
   private AceEditor editor_;
}
