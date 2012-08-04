/*
 * DesktopFrame.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application;

import com.google.gwt.core.client.JsArrayString;
import org.rstudio.core.client.js.BaseExpression;
import org.rstudio.core.client.js.JavaScriptPassthrough;
import org.rstudio.core.client.js.JsObject;

/**
 * This is an interface straight through to a C++ object that lives
 * in the Qt desktop frame.
 */
@BaseExpression("$wnd.desktop")
public interface DesktopFrame extends JavaScriptPassthrough
{
   void browseUrl(String url);
   String getOpenFileName(String caption, String dir, String filter);
   String getSaveFileName(String caption, 
                          String dir, 
                          String defaultExtension, 
                          boolean forceDefaultExtension);
   String getExistingDirectory(String caption, String dir);
   void undo();
   void redo();
   void clipboardCut();
   void clipboardCopy();
   void clipboardPaste();
   String getUriForPath(String path);
   void onWorkbenchInitialized(String scratchDir);
   void showFolder(String path);
   void showFile(String path);
   void openMinimalWindow(String name, String url, int width, int height);
   void activateSatelliteWindow(String name);
   void prepareForSatelliteWindow(String name, int width, int height);
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

   JsObject promptForText(String title,
                        String label,
                        String initialValue,
                        boolean usePasswordMask,
                        String rememberPasswordPrompt,
                        boolean rememberByDefault,
                        boolean numbersOnly,
                        int selectionStart,
                        int selectionLength, String okButtonCaption);

   void checkForUpdates();
   void showAboutDialog();
   void bringMainFrameToFront();

   boolean suppressSyntaxHighlighting();

   String getRVersion();
   String chooseRVersion();
   boolean canChooseRVersion();

   void cleanClipboard();
   
   public static final int PENDING_RESTART_NONE = 0;
   public static final int PENDING_RESTART_ONLY = 1;
   public static final int PENDING_RESTART_AND_RELOAD = 2;
   
   void setPendingRestart(int pendingRestart);
   
   void openProjectInNewWindow(String projectFilePath);
   
   void openTerminal(String terminalPath,
                     String workingDirectory,
                     String extraPathEntries);

   JsArrayString getFontList(boolean fixedWidthOnly);
   String getFixedWidthFont();
   void setFixedWidthFont(String font);
   
   String getDesktopSynctexViewer();
   
   void externalSynctexPreview(String pdfPath, int page);
   
   void externalSynctexView(String pdfFile, 
                            String srcFile, 
                            int line,
                            int column);
}
