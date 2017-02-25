/*
 * DesktopFrame.java
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
   boolean isCocoa();
   void browseUrl(String url);
   String getOpenFileName(String caption,
                          String dir,
                          String filter,
                          boolean canChooseDirectories);
   String getSaveFileName(String caption, 
                          String dir, 
                          String defaultExtension, 
                          boolean forceDefaultExtension);
   String getExistingDirectory(String caption, String dir);
   void undo(boolean forAce);
   void redo(boolean forAce);
   void clipboardCut();
   void clipboardCopy();
   void clipboardPaste();
   String getUriForPath(String path);
   void onWorkbenchInitialized(String scratchDir);
   void showFolder(String path);
   void showFile(String path);
   void showWordDoc(String path);
   void showPDF(String path, int pdfPage);
   void prepareShowWordDoc();
   void openMinimalWindow(String name, String url, int width, int height);
   void activateMinimalWindow(String name);
   void activateSatelliteWindow(String name);
   void prepareForSatelliteWindow(String name, int x, int y, int width,
                                  int height);
   void prepareForNamedWindow(String name, boolean allowExternalNavigation,
         boolean showDesktopToolbar);
   void closeNamedWindow(String name);
   
   // interface for plot export where coordinates are specified relative to
   // the iframe where the image is located within
   void copyImageToClipboard(int clientLeft,
                             int clientTop,
                             int clientWidth,
                             int clientHeight);
   
   void copyPageRegionToClipboard(int left, int top, int width, int height);
   
   void exportPageRegionToFile(String targetPath, 
                               String format, 
                               int left, 
                               int top, 
                               int width, 
                               int height);
   
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
                        boolean usePasswordMask,
                        String rememberPasswordPrompt,
                        boolean rememberByDefault,
                        boolean numbersOnly,
                        int selectionStart,
                        int selectionLength, String okButtonCaption);

   void showAboutDialog();
   void bringMainFrameToFront();
   void bringMainFrameBehindActive();

   String getRVersion();
   String chooseRVersion();
   boolean canChooseRVersion();

   double devicePixelRatio();
   int getDisplayDpi();
   
   void cleanClipboard();
   
   public static final int PENDING_QUIT_NONE = 0;
   public static final int PENDING_QUIT_AND_EXIT = 1;
   public static final int PENDING_QUIT_AND_RESTART = 2;
   public static final int PENDING_QUIT_RESTART_AND_RELOAD = 3;
   
   void setPendingQuit(int pendingQuit);
   void setPendingProject(String projectFilePath);
   void launchSession(boolean reload);
   
   void openProjectInNewWindow(String projectFilePath);
   void openSessionInNewWindow(String workingDirectoryPath);
   
   void openTerminal(String terminalPath,
                     String workingDirectory,
                     String extraPathEntries);

   String getFixedWidthFontList();
   String getFixedWidthFont();
   void setFixedWidthFont(String font);
   
   String getZoomLevels();
   double getZoomLevel();
   void setZoomLevel(double zoomLevel);
   
   // mac-specific zoom calls
   void macZoomActualSize();
   void macZoomIn();
   void macZoomOut();
   
   String getDesktopSynctexViewer();
   
   void externalSynctexPreview(String pdfPath, int page);
   
   void externalSynctexView(String pdfFile, 
                            String srcFile, 
                            int line,
                            int column);
   
   boolean supportsFullscreenMode();
   void toggleFullscreenMode();
   void showKeyboardShortcutHelp();
   
   void reloadZoomWindow();
   
   void setViewerUrl(String url);
   void reloadViewerZoomWindow(String url);
   
   void setShinyDialogUrl(String url);
   
   boolean isOSXMavericks();
   boolean isCentOS();

   String getScrollingCompensationType();
   
   void setBusy(boolean busy);
   
   void setWindowTitle(String title);
   
   void installRtools(String version, String installerPath);
}
