/*
 * DesktopFrame.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.SessionServer;
import org.rstudio.core.client.js.BaseExpression;
import org.rstudio.core.client.js.JavaScriptPassthrough;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;

/**
 * This is an interface straight through to a C++ object that lives
 * in the Qt desktop frame.
 * 
 * String arguments must not be null.
 */
@BaseExpression("$wnd.desktop")
public interface DesktopFrame extends JavaScriptPassthrough
{
   void browseUrl(String url);
   
   void getOpenFileName(String caption,
                        String label,
                        String dir,
                        String filter,
                        boolean canChooseDirectories,
                        boolean focusOpener,
                        CommandWithArg<String> callback);
   
   void getSaveFileName(String caption,
                        String label,
                        String dir, 
                        String defaultExtension, 
                        boolean forceDefaultExtension,
                        boolean focusOpener,
                        CommandWithArg<String> callback);
   
   void getExistingDirectory(String caption,
                             String label,
                             String dir,
                             boolean focusOpener,
                             CommandWithArg<String> callback);
   
   void undo();
   void redo();
   
   void clipboardCut();
   void clipboardCopy();
   void clipboardPaste();
   
   void setClipboardText(String text);
   void getClipboardText(CommandWithArg<String> callback);
   void getClipboardUris(CommandWithArg<JsArrayString> callback);
   void getClipboardImage(CommandWithArg<String> callback);
   
   void setGlobalMouseSelection(String selection);
   void getGlobalMouseSelection(CommandWithArg<String> callback);
   
   void doesWindowExistAtCursorPosition(CommandWithArg<Boolean> callback);
   void getCursorPosition(CommandWithArg<Point> callback);
   
   void onWorkbenchInitialized(String scratchDir);
   void showFolder(String path);
   void showFile(String path);
   void showWordDoc(String path);
   void showPDF(String path, int pdfPage);
   void prepareShowWordDoc();
   void prepareShowPptPresentation();
   void showPptPresentation(String path);
   void openMinimalWindow(String name, String url, int width, int height);
   void activateMinimalWindow(String name);
   void activateSatelliteWindow(String name);
   void prepareForSatelliteWindow(String name, int x, int y, int width,
                                  int height, Command onPrepared);
   void prepareForNamedWindow(String name, boolean allowExternalNavigation,
         boolean showDesktopToolbar, Command onPrepared);
   void closeNamedWindow(String name);
   
   void copyPageRegionToClipboard(int left, int top, int width, int height,
                                  Command onCopied);
   
   void exportPageRegionToFile(String targetPath, 
                               String format, 
                               int left, 
                               int top, 
                               int width, 
                               int height);

   void printText(String text);
   
   void supportsClipboardMetafile(CommandWithArg<Boolean> callback);

   void showMessageBox(int type,
                       String caption,
                       String message,
                       String buttons,
                       int defaultButton,
                       int cancelButton,
                       CommandWithArg<Double> callback);

   void promptForText(String title,
                      String label,
                      String initialValue,
                      int type,
                      String rememberPasswordPrompt,
                      boolean rememberByDefault,
                      int selectionStart,
                      int selectionLength,
                      String okButtonCaption,
                      CommandWithArg<String> callback);

   void bringMainFrameToFront();
   void bringMainFrameBehindActive();
   
   void desktopRenderingEngine(CommandWithArg<String> callback);
   void setDesktopRenderingEngine(String engine);

   // R version selection currently Win32 only
   void getRVersion(CommandWithArg<String> callback);
   void chooseRVersion(CommandWithArg<String> callback);

   void getDisplayDpi(CommandWithArg<String> callback);

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
                     String extraPathEntries,
                     String shellType);

   void setFixedWidthFont(String font);
   void setZoomLevel(double zoomLevel);
   
   void zoomIn();
   void zoomOut();
   void zoomActualSize();
   
   void setBackgroundColor(JsArrayInteger rgbColor);
   void changeTitleBarColor(int r, int g, int b);
   void syncToEditorTheme(boolean isDark);
   
   void getEnableAccessibility(CommandWithArg<Boolean> callback);
   void setEnableAccessibility(boolean enable);
   
   void getClipboardMonitoring(CommandWithArg<Boolean> callback);
   void setClipboardMonitoring(boolean monitoring);
   
   void getIgnoreGpuBlacklist(CommandWithArg<Boolean> callback);
   void setIgnoreGpuBlacklist(boolean ignore);
   
   void getDisableGpuDriverBugWorkarounds(CommandWithArg<Boolean> callback);
   void setDisableGpuDriverBugWorkarounds(boolean disable);
   
   void showLicenseDialog();
   void getInitMessages(CommandWithArg<String> callback);
   void getLicenseStatusMessage(CommandWithArg<String> callback);
   void allowProductUsage(CommandWithArg<Boolean> callback);
   
   void externalSynctexPreview(String pdfPath, int page);
   
   void externalSynctexView(String pdfFile, 
                            String srcFile, 
                            int line,
                            int column);
   
   void toggleFullscreenMode();
   void showKeyboardShortcutHelp();
   
   void reloadZoomWindow();
   
   void setViewerUrl(String url);
   void reloadViewerZoomWindow(String url);
   
   void setTutorialUrl(String url);
   
   void setShinyDialogUrl(String url);
   
   void setBusy(boolean busy);
   
   void setWindowTitle(String title);
   
   void installRtools(String version, String installerPath);

   void showSessionServerOptionsDialog();

   void onSessionQuit();

   void getSessionServer(CommandWithArg<SessionServer> callback);
   void getSessionServers(CommandWithArg<JsArray<SessionServer>> callback);
   void reconnectToSessionServer(SessionServer server);

   void setLauncherServer(SessionServer server, CommandWithArg<Boolean> callback);
   void connectToLauncherServer();

   void getLauncherServer(CommandWithArg<SessionServer> callback);
   void startLauncherJobStatusStream(String jobId);
   void stopLauncherJobStatusStream(String jobId);
   void startLauncherJobOutputStream(String jobId);
   void stopLauncherJobOutputStream(String jobId);
   void controlLauncherJob(String jobId, String operation);
   void getJobContainerUser();
   void validateJobsConfig();
   void getProxyPortNumber(CommandWithArg<Double> callback);

   void signOut();
}
