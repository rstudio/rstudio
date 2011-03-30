/*
 * DesktopGwtCallback.cpp
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

#include "DesktopGwtCallback.hpp"

#include <algorithm>

#if _WIN32
#include <shlobj.h>
#endif

#include <QtGui/QFileDialog>
#include <QFileDialog>
#include <QDesktopServices>
#include <QMessageBox>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include "DesktopAboutDialog.hpp"
#include "DesktopCRANMirrorDialog.hpp"
#include "DesktopOptions.hpp"
#include "DesktopBrowserWindow.hpp"
#include "DesktopWindowTracker.hpp"
#include "DesktopInputDialog.hpp"
#include "DesktopSecondaryWindow.hpp"
#include "DesktopRVersion.hpp"
#include "DesktopMainWindow.hpp"
#include "DesktopUtils.hpp"

using namespace core;

namespace desktop {

extern QString scratchPath;

void GwtCallback::browseUrl(QString url)
{
   QUrl qurl(url);

#ifdef Q_WS_MAC
   if (qurl.scheme() == "file")
   {
      QProcess open;
      open.start("open", QStringList() << url);
      open.waitForFinished(5000);
      if (open.exitCode() != 0)
      {
         // Probably means that the file doesn't have a registered
         // application or something.
         QProcess reveal;
         reveal.startDetached("open", QStringList() << "-R" << url);
      }
      return;
   }
#endif

   if (qurl.isRelative())
   {
      // TODO: this should really be handled within GlobalDisplay -- rather
      // than checking for a relative URL here GlobalDisplay should determine
      // that a URL is relative and do the right thing (note this needs to
      // account for our policy regarding /custom/* urls -- below we allow
      // these to be opened in a standard browser window to match the
      // behavior of standard CRAN desktop R).

      // compute local url
      QUrl localUrl;
      localUrl.setScheme("http");
      localUrl.setHost("localhost");
      localUrl.setPort(options().portNumber().toInt());
      localUrl.setEncodedPath(url.toAscii());

      // show it in a browser or a secondary window as appropriate
      if (url.startsWith("custom/"))
      {
         QDesktopServices::openUrl(localUrl);
      }
      else
      {
         SecondaryWindow* pBrowser = new SecondaryWindow(localUrl);
         pBrowser->webView()->load(localUrl);
         pBrowser->show();
         pBrowser->activateWindow();
      }
   }
   else
   {
      QDesktopServices::openUrl(qurl);
   };
}

namespace {

FilePath userHomePath()
{
   return core::system::userHomePath("R_USER|HOME");
}

QString createAliasedPath(const QString& path)
{
   std::string aliased = FilePath::createAliasedPath(
         FilePath(path.toStdString()), userHomePath());
   return QString::fromStdString(aliased);
}

QString resolveAliasedPath(const QString& path)
{
   FilePath resolved(FilePath::resolveAliasedPath(path.toStdString(),
                                                  userHomePath()));
   return QString::fromStdString(resolved.absolutePath());
}

} // anonymous namespace

QString GwtCallback::getOpenFileName(const QString& caption,
                                    const QString& dir)
{
   QString resolvedDir = resolveAliasedPath(dir);
   QString result = QFileDialog::getOpenFileName(pOwnerWindow_, caption, resolvedDir);
   webView()->page()->mainFrame()->setFocus();
   return createAliasedPath(result);
}

QString GwtCallback::getSaveFileName(const QString& caption,
                                    const QString& dir)
{
   QString resolvedDir = resolveAliasedPath(dir);
   QString result = QFileDialog::getSaveFileName(pOwnerWindow_, caption, resolvedDir);
   webView()->page()->mainFrame()->setFocus();
   return createAliasedPath(result);
}

QString GwtCallback::getExistingDirectory(const QString& caption,
                                         const QString& dir)
{
   QString resolvedDir = resolveAliasedPath(dir);

   QString result;
#ifdef _WIN32
   if (dir.isNull())
   {
      // Bug
      char szDir[MAX_PATH];
      BROWSEINFO bi;
      bi.hwndOwner = pOwnerWindow_->winId();
      bi.pidlRoot = NULL;
      bi.pszDisplayName = szDir;
      bi.lpszTitle = "Select a folder:";
      bi.ulFlags = BIF_RETURNONLYFSDIRS;
      bi.lpfn = NULL;
      bi.lpfn = 0;
      bi.iImage = -1;
      LPITEMIDLIST pidl = SHBrowseForFolder(&bi);
      if (!pidl || !SHGetPathFromIDList(pidl, szDir))
         result = QString("");
      else
         result = QString::fromLocal8Bit(szDir);
   }
   else
   {
      result = QFileDialog::getExistingDirectory(pOwnerWindow_, caption, resolvedDir);
   }
#else
   result = QFileDialog::getExistingDirectory(pOwnerWindow_, caption, resolvedDir);
#endif

   webView()->page()->mainFrame()->setFocus();
   return createAliasedPath(result);
}

void GwtCallback::doAction(QKeySequence::StandardKey key)
{
   QList<QKeySequence> bindings = QKeySequence::keyBindings(key);
   if (bindings.size() == 0)
      return;

   QKeySequence seq = bindings.first();

   int keyCode = seq[0];
   Qt::KeyboardModifier modifiers = static_cast<Qt::KeyboardModifier>(keyCode & Qt::KeyboardModifierMask);
   keyCode &= ~Qt::KeyboardModifierMask;

   QKeyEvent* keyEvent = new QKeyEvent(QKeyEvent::KeyPress, keyCode, modifiers);
   QCoreApplication::postEvent(webView(), keyEvent);
}

QWebView* GwtCallback::webView()
{
   return pOwnerWindow_->webView();
}

void GwtCallback::undo()
{
   doAction(QKeySequence::Undo);
}

void GwtCallback::redo()
{
   doAction(QKeySequence::Redo);
}

void GwtCallback::clipboardCut()
{
   doAction(QKeySequence::Cut);
}

void GwtCallback::clipboardCopy()
{
   doAction(QKeySequence::Copy);
}

void GwtCallback::clipboardPaste()
{
   doAction(QKeySequence::Paste);
}

QString GwtCallback::proportionalFont()
{
   return options().proportionalFont();
}

QString GwtCallback::fixedWidthFont()
{
   return options().fixedWidthFont();
}

QString GwtCallback::getUriForPath(QString path)
{
   return QUrl::fromLocalFile(resolveAliasedPath(path)).toString();
}

void GwtCallback::onWorkbenchInitialized(QString scratchPath)
{
   workbenchInitialized();
   desktop::scratchPath = scratchPath;
}

void GwtCallback::showFolder(QString path)
{
   if (path.isNull() || path.isEmpty())
      return;

   path = resolveAliasedPath(path);

   QDir dir(path);
   if (dir.exists())
   {
      QDesktopServices::openUrl(QUrl::fromLocalFile(dir.absolutePath()));
   }
}

QString GwtCallback::getCRANMirror()
{
   return options().defaultCRANmirrorName();
}

QString GwtCallback::chooseCRANmirror()
{
   CRANMirrorDialog dialog(pOwnerWindow_);
   if (dialog.exec() == QDialog::Accepted)
   {
      options().setDefaultCRANmirror(dialog.selectedName(),
                                     dialog.selectedURL());
      return dialog.selectedName();
   }
   return QString();
}

QString GwtCallback::getRVersion()
{
#ifdef Q_OS_WIN32
   bool defaulted = options().rBinDir().isEmpty();
   QString rDesc = defaulted
                   ? QString("[Default] ") + autoDetect().description()
                   : RVersion(options().rBinDir()).description();
   return rDesc;
#else
   return QString();
#endif
}

QString GwtCallback::chooseRVersion()
{
#ifdef Q_OS_WIN32
   RVersion rVersion = desktop::detectRVersion(true, pOwnerWindow_);
   if (rVersion.isValid())
      return getRVersion();
   else
      return QString();
#else
   return QString();
#endif
}

bool GwtCallback::canChooseRVersion()
{
#ifdef Q_OS_WIN32
   return true;
#else
   return false;
#endif
}

void GwtCallback::close()
{
   pOwnerWindow_->close();
}

void GwtCallback::openMinimalWindow(QString name,
                                    QString url,
                                    int width,
                                    int height)
{
   static WindowTracker windowTracker;

   bool named = !name.isEmpty() && name != "_blank";

   BrowserWindow* browser = NULL;
   if (named)
      browser = windowTracker.getWindow(name);

   if (!browser)
   {
      browser = new BrowserWindow(false);
      browser->setAttribute(Qt::WA_DeleteOnClose);
      browser->setAttribute(Qt::WA_QuitOnClose, false);
      if (named)
         windowTracker.addWindow(name, browser);
   }

   browser->webView()->load(QUrl(url));
   browser->setFixedSize(width, height);
   browser->show();
   browser->activateWindow();
}

void GwtCallback::copyImageToClipboard(int left, int top, int width, int height)
{
   pOwnerWindow_->webView()->page()->updatePositionDependentActions(
         QPoint(left + (width/2), top + (height/2)));
   pOwnerWindow_->webView()->triggerPageAction(QWebPage::CopyImageToClipboard);
}

namespace {
   QMessageBox::ButtonRole captionToRole(QString caption)
   {
      if (caption == "OK")
         return QMessageBox::AcceptRole;
      else if (caption == "Cancel")
         return QMessageBox::RejectRole;
      else if (caption == "Yes")
         return QMessageBox::YesRole;
      else if (caption == "No")
         return QMessageBox::NoRole;
      else if (caption == "Save")
         return QMessageBox::AcceptRole;
      else if (caption == "Don't Save")
         return QMessageBox::DestructiveRole;
      else
         return QMessageBox::ActionRole;
   }
} // anonymous namespace

int GwtCallback::showMessageBox(int type,
                                QString caption,
                                QString message,
                                QString buttons,
                                int defaultButton,
                                int cancelButton)
{
   QMessageBox msgBox(safeMessageBoxIcon(static_cast<QMessageBox::Icon>(type)),
                       caption,
                       message,
                       QMessageBox::NoButton,
                       pOwnerWindow_,
                       Qt::Dialog | Qt::Sheet);
   msgBox.setWindowModality(Qt::WindowModal);
   msgBox.setTextFormat(Qt::PlainText);

   QStringList buttonList = buttons.split(QChar('|'));

   for (int i = 0; i != buttonList.size(); i++)
   {
      QPushButton* pBtn = msgBox.addButton(buttonList.at(i),
                                           captionToRole(buttonList.at(i)));
      if (defaultButton == i)
         msgBox.setDefaultButton(pBtn);
   }

   msgBox.exec();

   QAbstractButton* button = msgBox.clickedButton();
   if (!button)
      return cancelButton;

   for (int i = 0; i < buttonList.size(); i++)
      if (buttonList.at(i) == button->text())
         return i;

   return cancelButton;
}

QString GwtCallback::promptForText(QString title,
                                   QString caption,
                                   QString defaultValue,
                                   int selectionStart,
                                   int selectionLength)
{
   InputDialog dialog(pOwnerWindow_);
   dialog.setWindowTitle(title);
   dialog.setCaption(caption);
   if (!defaultValue.isEmpty())
   {
      dialog.setTextValue(defaultValue);
      if (selectionStart >= 0 && selectionLength >= 0)
      {
         dialog.setSelection(selectionStart, selectionLength);
      }
      else
      {
         dialog.setSelection(0, defaultValue.size());
      }
   }

   if (dialog.exec() == QDialog::Accepted)
      return dialog.textValue();
   else
      return QString();
}

void GwtCallback::checkForUpdates()
{
   pOwnerWindow_->checkForUpdates();
}

void GwtCallback::showAboutDialog()
{
   // WA_DeleteOnClose
   AboutDialog* about = new AboutDialog(pOwnerWindow_);
   about->setAttribute(Qt::WA_DeleteOnClose);
   about->show();
}

bool GwtCallback::suppressSyntaxHighlighting()
{
#ifdef Q_WS_X11
   // Fix bug 1228: "on ubuntu 10.04 desktop cursor's position
   // horizontally off by progressively more as line gets longer"
   return QString(qVersion()).startsWith("4.6.");
#else
   return false;
#endif
}

} // namespace desktop
