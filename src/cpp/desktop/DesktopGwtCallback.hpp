/*
 * DesktopGwtCallback.hpp
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

#ifndef DESKTOP_GWT_CALLBACK_HPP
#define DESKTOP_GWT_CALLBACK_HPP

#include <QObject>
#include <QClipboard>
#include <QKeySequence>
#include <QJsonArray>
#include <QJsonObject>
#include <QPrinter>

#include <boost/optional.hpp>

#ifdef Q_OS_WIN32
#include "DesktopWordViewer.hpp"
#include "DesktopPowerpointViewer.hpp"
#endif

namespace rstudio {
namespace desktop {

class MainWindow;
class GwtWindow;
class Synctex;
class JobLauncher;

enum PendingQuit 
{
   PendingQuitNone             = 0,
   PendingQuitAndExit          = 1,
   PendingQuitAndRestart       = 2,
   PendingQuitRestartAndReload = 3
};

enum InputType 
{
   InputRequiredText = 0,
   InputOptionalText = 1,
   InputPassword     = 2,
   InputNumeric      = 3
};

class GwtCallback : public QObject
{
   Q_OBJECT

public:
   GwtCallback(MainWindow* pMainWindow, GwtWindow* pOwner, bool isRemoteDesktop);
   void initialize();
   int collectPendingQuitRequest();

Q_SIGNALS:
   void workbenchInitialized();
   void sessionQuit();

public Q_SLOTS:
   QString proportionalFont();
   QString fixedWidthFont();
   void browseUrl(QString url);

   QString getOpenFileName(const QString& caption,
                           const QString& label,
                           const QString& dir,
                           const QString& filter,
                           bool canChooseDirectories,
                           bool focusOwner);

   QString getSaveFileName(const QString& caption,
                           const QString& label,
                           const QString& dir,
                           const QString& defaultExtension,
                           bool forceDefaultExtension,
                           bool focusOwner);

   QString getExistingDirectory(const QString& caption,
                                const QString& label,
                                const QString& dir,
                                bool focusOwner);

   void onClipboardSelectionChanged();

   void undo();
   void redo();

   void clipboardCut();
   void clipboardCopy();
   void clipboardPaste();

   void setClipboardText(QString text);
   QString getClipboardText();
   QJsonArray getClipboardUris();
   QString getClipboardImage();
   
   void setGlobalMouseSelection(QString selection);
   QString getGlobalMouseSelection();

   QJsonObject getCursorPosition();
   bool doesWindowExistAtCursorPosition();

   void onWorkbenchInitialized(QString scratchPath);
   void showFolder(QString path);
   void showFile(QString path);
   void showWordDoc(QString path);
   void showPptPresentation(QString path);
   void showPDF(QString path, int pdfPage);
   void prepareShowWordDoc();
   void prepareShowPptPresentation();

   // R version selection currently Win32 only
   QString getRVersion();
   QString chooseRVersion();

   double devicePixelRatio();

   void openMinimalWindow(QString name, QString url, int width, int height);
   void activateMinimalWindow(QString name);
   void activateSatelliteWindow(QString name);
   void prepareForSatelliteWindow(QString name, int x, int y, int width,
                                  int height);
   void prepareForNamedWindow(QString name, bool allowExternalNavigate,
                              bool showToolbar);
   void closeNamedWindow(QString name);

   // coordinates are relative to entire containing web page
   void copyPageRegionToClipboard(int left, int top, int width, int height);
   void exportPageRegionToFile(QString targetPath,
                               QString format,
                               int left,
                               int top,
                               int width,
                               int height);

   void printText(QString text);
   void paintPrintText(QPrinter* printer);
   void printFinished(int result);

   bool supportsClipboardMetafile();

   int showMessageBox(int type,
                      QString caption,
                      QString message,
                      QString buttons,
                      int defaultButton,
                      int cancelButton);

   QString promptForText(QString title,
                         QString caption,
                         QString defaultValue,
                         int type,
                         QString rememberPasswordPrompt,
                         bool rememberByDefault,
                         int selectionStart,
                         int selectionLength,
                         QString okButtonCaption);

   void bringMainFrameToFront();
   void bringMainFrameBehindActive();
   
   QString desktopRenderingEngine();
   void setDesktopRenderingEngine(QString engine);

   QString filterText(QString text);

   void cleanClipboard(bool stripHtml);

   void setPendingQuit(int pendingQuit);

   void openProjectInNewWindow(QString projectFilePath);
   void openSessionInNewWindow(QString workingDirectoryPath);

   void openTerminal(QString terminalPath,
                     QString workingDirectory,
                     QString extraPathEntries,
                     QString shellType);

   QString getFixedWidthFontList();
   QString getFixedWidthFont();
   void setFixedWidthFont(QString font);

   QString getZoomLevels();
   double getZoomLevel();
   void setZoomLevel(double zoomLevel);
   
   void zoomIn();
   void zoomOut();
   void zoomActualSize();
   
   void setBackgroundColor(QJsonArray rgbColor);
   void changeTitleBarColor(int red, int green, int blue);
   void syncToEditorTheme(bool isDark);

   bool getEnableAccessibility();
   void setEnableAccessibility(bool enable);

   bool getClipboardMonitoring();
   void setClipboardMonitoring(bool monitoring);
   
   bool getIgnoreGpuBlacklist();
   void setIgnoreGpuBlacklist(bool ignore);
   
   bool getDisableGpuDriverBugWorkarounds();
   void setDisableGpuDriverBugWorkarounds(bool disable);

   void showLicenseDialog();
   void showSessionServerOptionsDialog();
   QString getInitMessages();
   QString getLicenseStatusMessage();
   bool allowProductUsage();

   QString getDesktopSynctexViewer();

   void externalSynctexPreview(QString pdfPath, int page);

   void externalSynctexView(const QString& pdfFile,
                            const QString& srcFile,
                            int line,
                            int column);

   bool supportsFullscreenMode();
   void toggleFullscreenMode();
   void showKeyboardShortcutHelp();

   void launchSession(bool reload);

   void reloadZoomWindow();

   void setTutorialUrl(QString url);
   
   void setViewerUrl(QString url);
   void reloadViewerZoomWindow(QString url);

   void setShinyDialogUrl(QString url);

   QString getScrollingCompensationType();

   bool isMacOS();
   bool isCentOS();

   void setBusy(bool busy);

   void setWindowTitle(QString title);

   void installRtools(QString version, QString installerPath);

   QString getDisplayDpi();

   void onSessionQuit();

   QJsonObject getSessionServer();
   QJsonArray getSessionServers();
   void reconnectToSessionServer(const QJsonValue& sessionServerJson);

   bool setLauncherServer(const QJsonObject& sessionServerJson);
   void connectToLauncherServer();

   QJsonObject getLauncherServer();
   void startLauncherJobStatusStream(QString jobId);
   void stopLauncherJobStatusStream(QString jobId);
   void startLauncherJobOutputStream(QString jobId);
   void stopLauncherJobOutputStream(QString jobId);
   void controlLauncherJob(QString jobId, QString operation);
   void submitLauncherJob(const QJsonObject& job);
   void getJobContainerUser();
   void validateJobsConfig();
   int getProxyPortNumber();

   void signOut();

private:
   Synctex& synctex();
   void activateAndFocusOwner();

private:
   void doAction(const QKeySequence& keys);
   void doAction(QKeySequence::StandardKey key);
   MainWindow* pMainWindow_;
   GwtWindow* pOwner_;
   JobLauncher* pLauncher_;
   bool isRemoteDesktop_;
   Synctex* pSynctex_;
   int pendingQuit_;
   QString printText_;
#ifdef Q_OS_WIN32
   // viewers for Office file formats
   WordViewer wordViewer_;
   PowerpointViewer pptViewer_;
#endif

};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_GWT_CALLBACK_HPP
