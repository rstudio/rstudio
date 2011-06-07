/*
 * DesktopFrame.java
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.js.BaseExpression;
import org.rstudio.core.client.js.JavaScriptPassthrough;

/**
 * This is an interface straight through to a C++ object that lives
 * in the Qt desktop frame.
 */
@BaseExpression("$wnd.desktop")
public interface DesktopFrame extends JavaScriptPassthrough
{
   void browseUrl(String url);
   String getOpenFileName(String caption, String dir);
   String getSaveFileName(String caption, String dir, String defaultExtension);
   String getExistingDirectory(String caption, String dir);
   void undo();
   void redo();
   void clipboardCut();
   void clipboardCopy();
   void clipboardPaste();
   String getUriForPath(String path);
   void onWorkbenchInitialized(String scratchDir);
   void showFolder(String path);
   void close();
   void openMinimalWindow(String name, String url, int width, int height);
   void copyImageToClipboard(int clientLeft,
                             int clientTop,
                             int clientWidth,
                             int clientHeight);
   
   boolean supportsClipboardMetafile();

   int showMessageBox(int type,
                      String caption,
                      String message,
                      String buttons,
                      int defaultButton,
                      int cancelButton);

   String promptForText(String title,
                        String label,
                        String initialValue,
                        int selectionStart,
                        int selectionLength, String okButtonCaption);

   void checkForUpdates();
   void showAboutDialog();

   boolean suppressSyntaxHighlighting();

   String getRVersion();
   String chooseRVersion();
   boolean canChooseRVersion();
}
