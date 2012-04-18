/*
 * DesktopGwtCallback.hpp
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

#ifndef DESKTOP_GWT_CALLBACK_HPP
#define DESKTOP_GWT_CALLBACK_HPP

#include <QObject>
#include <QtWebKit>

#include "DesktopGwtCallbackOwner.hpp"

namespace desktop {

class MainWindow;
class BrowserWindow;

class GwtCallback : public QObject
{
   Q_OBJECT
   Q_PROPERTY(QString proportionalFont READ proportionalFont)
   Q_PROPERTY(QString fixedWidthFont READ fixedWidthFont)

public:
   GwtCallback(MainWindow* pMainWindow, GwtCallbackOwner* pOwner)
      : pMainWindow_(pMainWindow),
        pOwner_(pOwner),
        switchToProjectPending_(false)
   {
   }

   QString proportionalFont();
   QString fixedWidthFont();

   bool collectPendingSwitchToProjectRequest();

signals:
   void workbenchInitialized();

public slots:
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

   QString getRVersion();
   QString chooseRVersion();
   bool canChooseRVersion();

   void openMinimalWindow(QString name, QString url, int width, int height);
   void activateSatelliteWindow(QString name);
   void prepareForSatelliteWindow(QString name, int width, int height);


   // Image coordinates are relative to the window contents
   void copyImageToClipboard(int left, int top, int width, int height);

   bool supportsClipboardMetafile();

   int showMessageBox(int type,
                      QString caption,
                      QString message,
                      QString buttons,
                      int defaultButton,
                      int cancelButton);

   QVariant promptForText(QString title,
                         QString caption,
                         QString defaultValue,
                         bool usePasswordMask,
                         QString rememberPasswordPrompt,
                         bool rememberByDefault,
                         bool numbersOnly,
                         int selectionStart,
                         int selectionLength);

   void checkForUpdates();
   void showAboutDialog();
   void bringMainFrameToFront();
   bool suppressSyntaxHighlighting();
   QString filterText(QString text);

   void cleanClipboard(bool stripHtml);

   void setSwitchToProjectPending(bool switchPending);

   void openProjectInNewWindow(QString projectFilePath);

   void openTerminal(QString terminalPath,
                     QString workingDirectory,
                     QString extraPathEntries);

   QVariant getFontList(bool fixedWidthOnly);
   QString getFixedWidthFont();
   void setFixedWidthFont(QString font);

   bool forceFastScrollFactor();

private:
   void doAction(QKeySequence::StandardKey key);
   MainWindow* pMainWindow_;
   GwtCallbackOwner* pOwner_;
   bool switchToProjectPending_;

};

} // namespace desktop

#endif // DESKTOP_GWT_CALLBACK_HPP
