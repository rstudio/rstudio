/*
 * DesktopGwtCallback.hpp
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

#ifndef DESKTOP_GWT_CALLBACK_HPP
#define DESKTOP_GWT_CALLBACK_HPP

#include <QObject>
#include <QtWebKit>

#include "DesktopGwtCallbackOwner.hpp"

#ifdef Q_OS_WIN32
#include "DesktopWordViewer.hpp"
#endif

namespace rstudio {
namespace desktop {

class MainWindow;
class BrowserWindow;
class Synctex;

enum PendingQuit {
   PendingQuitNone = 0,
   PendingQuitAndExit = 1,
   PendingQuitAndRestart = 2,
   PendingQuitRestartAndReload = 3
};

class GwtCallback : public QObject
{
   Q_OBJECT

public:
   GwtCallback(MainWindow* pMainWindow, GwtCallbackOwner* pOwner);

   int collectPendingQuitRequest();

signals:
   void workbenchInitialized();

public slots:
   QString proportionalFont();
   QString fixedWidthFont();
   bool isCocoa();
   void browseUrl(QString url);
   QString getOpenFileName(const QString& caption,
                           const QString& dir,
                           const QString& filter);
   QString getSaveFileName(const QString& caption,
                           const QString& dir,
                           const QString& defaultExtension,
                           bool forceDefaultExtension);
   QString getExistingDirectory(const QString& caption,
                                const QString& dir);
   void undo();
   void redo();
   void clipboardCut();
   void clipboardCopy();
   void clipboardPaste();
   QString getUriForPath(QString path);
   void onWorkbenchInitialized(QString scratchPath);
   void showFolder(QString path);
   void showFile(QString path);
   void showWordDoc(QString path);
   void showPDF(QString path, int pdfPage);
   void prepareShowWordDoc();

   QString getRVersion();
   QString chooseRVersion();
   bool canChooseRVersion();

   double devicePixelRatio();

   void openMinimalWindow(QString name, QString url, int width, int height);
   void activateMinimalWindow(QString name);
   void activateSatelliteWindow(QString name);
   void prepareForSatelliteWindow(QString name, int width, int height);
   void prepareForNamedWindow(QString name, bool allowExternalNavigate,
                              bool showToolbar);
   void closeNamedWindow(QString name);

   // Image coordinates are relative to the window contents
   void copyImageToClipboard(int left, int top, int width, int height);

   // coordinates are relative to entire containing web page
   void copyPageRegionToClipboard(int left, int top, int width, int height);
   void exportPageRegionToFile(QString targetPath,
                               QString format,
                               int left,
                               int top,
                               int width,
                               int height);

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
                         bool usePasswordMask,
                         QString rememberPasswordPrompt,
                         bool rememberByDefault,
                         bool numbersOnly,
                         int selectionStart,
                         int selectionLength);

   void showAboutDialog();
   void bringMainFrameToFront();

   QString filterText(QString text);

   void cleanClipboard(bool stripHtml);

   void setPendingQuit(int pendingQuit);

   void openProjectInNewWindow(QString projectFilePath);

   void openTerminal(QString terminalPath,
                     QString workingDirectory,
                     QString extraPathEntries);

   QString getFixedWidthFontList();
   QString getFixedWidthFont();
   void setFixedWidthFont(QString font);

   QString getZoomLevels();
   double getZoomLevel();
   void setZoomLevel(double zoomLevel);

   void macZoomActualSize();
   void macZoomIn();
   void macZoomOut();

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

   void setViewerUrl(QString url);
   void reloadViewerZoomWindow(QString url);

   QString getScrollingCompensationType();

   bool isOSXMavericks();

   void setBusy(bool busy);

   void setWindowTitle(QString title);

   void installRtools(QString version, QString installerPath);

   int getDisplayDpi();

private:
   Synctex& synctex();

   void activateAndFocusOwner();

private:
   void doAction(QKeySequence::StandardKey key);
   MainWindow* pMainWindow_;
   GwtCallbackOwner* pOwner_;
   Synctex* pSynctex_;
   int pendingQuit_;
#ifdef Q_OS_WIN32
   WordViewer wordViewer_;
#endif

};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_GWT_CALLBACK_HPP
