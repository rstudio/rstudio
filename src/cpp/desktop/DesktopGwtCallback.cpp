/*
 * DesktopGwtCallback.cpp
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

#include "DesktopGwtCallback.hpp"

#include <algorithm>

#ifdef _WIN32
#include <shlobj.h>
#endif

#include <boost/foreach.hpp>

#include <QtGui/QFileDialog>
#include <QFileDialog>
#include <QMessageBox>

#include <core/FilePath.hpp>
#include <core/DateTime.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopAboutDialog.hpp"
#include "DesktopOptions.hpp"
#include "DesktopBrowserWindow.hpp"
#include "DesktopWindowTracker.hpp"
#include "DesktopInputDialog.hpp"
#include "DesktopSecondaryWindow.hpp"
#include "DesktopRVersion.hpp"
#include "DesktopMainWindow.hpp"
#include "DesktopUtils.hpp"
#include "DesktopSynctex.hpp"
#include "DesktopUpdateAvailableDialog.hpp"

#ifdef __APPLE__
#include <Carbon/Carbon.h>
#endif

using namespace core;

namespace desktop {

extern QString scratchPath;

GwtCallback::GwtCallback(MainWindow* pMainWindow, GwtCallbackOwner* pOwner)
   : pMainWindow_(pMainWindow),
     pOwner_(pOwner),
     pSynctex_(NULL),
     pendingQuit_(PendingQuitNone)
{
}

Synctex& GwtCallback::synctex()
{
   if (pSynctex_ == NULL)
      pSynctex_ = Synctex::create(pMainWindow_);

   return *pSynctex_;
}

void GwtCallback::browseUrl(QString url)
{
   QUrl qurl = QUrl::fromEncoded(url.toAscii());

#ifdef Q_WS_MAC
   if (qurl.scheme() == QString::fromAscii("file"))
   {
      QProcess open;
      QStringList args;
      // force use of Preview for PDFs (Adobe Reader 10.01 crashes)
      if (url.toLower().endsWith(QString::fromAscii(".pdf")))
      {
         args.append(QString::fromAscii("-a"));
         args.append(QString::fromAscii("Preview"));
         args.append(url);
      }
      else
      {
         args.append(url);
      }
      open.start(QString::fromAscii("open"), args);
      open.waitForFinished(5000);
      if (open.exitCode() != 0)
      {
         // Probably means that the file doesn't have a registered
         // application or something.
         QProcess reveal;
         reveal.startDetached(QString::fromAscii("open"), QStringList() << QString::fromAscii("-R") << url);
      }
      return;
   }
#endif

   desktop::openUrl(qurl);
}

namespace {

FilePath userHomePath()
{
   return core::system::userHomePath("R_USER|HOME");
}

QString createAliasedPath(const QString& path)
{
   std::string aliased = FilePath::createAliasedPath(
         FilePath(path.toUtf8().constData()), userHomePath());
   return QString::fromUtf8(aliased.c_str());
}

QString resolveAliasedPath(const QString& path)
{
   FilePath resolved(FilePath::resolveAliasedPath(path.toUtf8().constData(),
                                                  userHomePath()));
   return QString::fromUtf8(resolved.absolutePath().c_str());
}

} // anonymous namespace

QString GwtCallback::getOpenFileName(const QString& caption,
                                    const QString& dir,
                                    const QString& filter)
{
   QString resolvedDir = resolveAliasedPath(dir);
   QString result = QFileDialog::getOpenFileName(pOwner_->asWidget(),
                                                 caption,
                                                 resolvedDir,
                                                 filter,
                                                 0,
                                                 standardFileDialogOptions());

   activateAndFocusOwner();
   return createAliasedPath(result);
}

QString GwtCallback::getSaveFileName(const QString& caption,
                                     const QString& dir,
                                     const QString& defaultExtension,
                                     bool forceDefaultExtension)
{
   QString resolvedDir = resolveAliasedPath(dir);

   while (true)
   {
      QString result = QFileDialog::getSaveFileName(pOwner_->asWidget(), caption, resolvedDir,
                                                    QString(), 0, standardFileDialogOptions());
      activateAndFocusOwner();
      if (result.isEmpty())
         return result;

      if (!defaultExtension.isEmpty())
      {
         FilePath fp(result.toUtf8().constData());
         if (fp.extension().empty() ||
            (forceDefaultExtension &&
            (fp.extension() != defaultExtension.toStdString())))
         {
            result += defaultExtension;
            FilePath newExtPath(result.toUtf8().constData());
            if (newExtPath.exists())
            {
               std::string message = "\"" + newExtPath.filename() + "\" already "
                                     "exists. Do you want to overwrite it?";
               if (QMessageBox::Cancel == QMessageBox::warning(
                                        pOwner_->asWidget(),
                                        QString::fromUtf8("Save File"),
                                        QString::fromUtf8(message.c_str()),
                                        QMessageBox::Ok | QMessageBox::Cancel,
                                        QMessageBox::Ok))
               {
                  resolvedDir = result;
                  continue;
               }
            }
         }
      }

      return createAliasedPath(result);
   }
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
      wchar_t szDir[MAX_PATH];
      BROWSEINFOW bi;
      bi.hwndOwner = pOwner_->asWidget()->winId();
      bi.pidlRoot = NULL;
      bi.pszDisplayName = szDir;
      bi.lpszTitle = L"Select a folder:";
      bi.ulFlags = BIF_RETURNONLYFSDIRS;
      bi.lpfn = NULL;
      bi.lpfn = 0;
      bi.iImage = -1;
      LPITEMIDLIST pidl = SHBrowseForFolderW(&bi);
      if (!pidl || !SHGetPathFromIDListW(pidl, szDir))
         result = QString();
      else
         result = QString::fromWCharArray(szDir);
   }
   else
   {
      result = QFileDialog::getExistingDirectory(pOwner_->asWidget(), caption, resolvedDir,
                                                 QFileDialog::ShowDirsOnly | standardFileDialogOptions());
   }
#else
   result = QFileDialog::getExistingDirectory(pOwner_->asWidget(), caption, resolvedDir,
                                              QFileDialog::ShowDirsOnly | standardFileDialogOptions());
#endif

   activateAndFocusOwner();
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
   pOwner_->postWebViewEvent(keyEvent);
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
      desktop::openUrl(QUrl::fromLocalFile(dir.absolutePath()));
   }
}

void GwtCallback::showFile(QString path)
{
   if (path.isNull() || path.isEmpty())
      return;

   path = resolveAliasedPath(path);

   desktop::openUrl(QUrl::fromLocalFile(path));
}


QString GwtCallback::getRVersion()
{
#ifdef Q_OS_WIN32
   bool defaulted = options().rBinDir().isEmpty();
   QString rDesc = defaulted
                   ? QString::fromUtf8("[Default] ") + autoDetect().description()
                   : RVersion(options().rBinDir()).description();
   return rDesc;
#else
   return QString();
#endif
}

QString GwtCallback::chooseRVersion()
{
#ifdef Q_OS_WIN32
   RVersion rVersion = desktop::detectRVersion(true, pOwner_->asWidget());
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

bool GwtCallback::isRetina()
{
   return desktop::isRetina(pMainWindow_);
}

void GwtCallback::openMinimalWindow(QString name,
                                    QString url,
                                    int width,
                                    int height)
{
   static WindowTracker windowTracker;

   bool named = !name.isEmpty() && name != QString::fromAscii("_blank");

   BrowserWindow* browser = NULL;
   if (named)
      browser = windowTracker.getWindow(name);

   if (!browser)
   {
      browser = new BrowserWindow(false, true);
      browser->setAttribute(Qt::WA_DeleteOnClose);
      browser->setAttribute(Qt::WA_QuitOnClose, false);
      browser->connect(browser->webView(), SIGNAL(onCloseWindowShortcut()),
                       browser, SLOT(onCloseRequested()));
      if (named)
         windowTracker.addWindow(name, browser);
   }

   browser->webView()->load(QUrl(url));
   browser->resize(width, height);
   browser->show();
   browser->activateWindow();
}

void GwtCallback::prepareForSatelliteWindow(QString name,
                                            int width,
                                            int height)
{
   pOwner_->webPage()->prepareForSatelliteWindow(
                PendingSatelliteWindow(name, pMainWindow_, width, height));
}

void GwtCallback::activateSatelliteWindow(QString name)
{
   pOwner_->webPage()->activateSatelliteWindow(name);
}

void GwtCallback::copyImageToClipboard(int left, int top, int width, int height)
{
   pOwner_->webPage()->updatePositionDependentActions(
         QPoint(left + (width/2), top + (height/2)));
   pOwner_->triggerPageAction(QWebPage::CopyImageToClipboard);
}

bool GwtCallback::supportsClipboardMetafile()
{
#ifdef Q_OS_WIN32
   return true;
#else
   return false;
#endif
}

namespace {
   QMessageBox::ButtonRole captionToRole(QString caption)
   {
      if (caption == QString::fromAscii("OK"))
         return QMessageBox::AcceptRole;
      else if (caption == QString::fromAscii("Cancel"))
         return QMessageBox::RejectRole;
      else if (caption == QString::fromAscii("Yes"))
         return QMessageBox::YesRole;
      else if (caption == QString::fromAscii("No"))
         return QMessageBox::NoRole;
      else if (caption == QString::fromAscii("Save"))
         return QMessageBox::AcceptRole;
      else if (caption == QString::fromAscii("Don't Save"))
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
   // cancel update checker if it's visible
   DesktopUpdateAvailableDialog* pUpdateDialog =
                  qobject_cast<DesktopUpdateAvailableDialog*>(
                        QApplication::activeModalWidget());
   if (pUpdateDialog != NULL)
      pUpdateDialog->close();

   // cancel other message box if it's visible
   QMessageBox* pMsgBox = qobject_cast<QMessageBox*>(
                        QApplication::activeModalWidget());
   if (pMsgBox != NULL)
      pMsgBox->close();

   QMessageBox msgBox(safeMessageBoxIcon(static_cast<QMessageBox::Icon>(type)),
                       caption,
                       message,
                       QMessageBox::NoButton,
                       pOwner_->asWidget(),
                       Qt::Dialog | Qt::Sheet);
   msgBox.setWindowModality(Qt::WindowModal);
   msgBox.setTextFormat(Qt::PlainText);

   QStringList buttonList = buttons.split(QChar::fromAscii('|'));

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
                                   bool usePasswordMask,
                                   QString extraOptionPrompt,
                                   bool extraOptionByDefault,
                                   bool numbersOnly,
                                   int selectionStart,
                                   int selectionLength)
{
   InputDialog dialog(pOwner_->asWidget());
   dialog.setWindowTitle(title);
   dialog.setCaption(caption);

   if (usePasswordMask)
      dialog.setEchoMode(QLineEdit::Password);

   if (!extraOptionPrompt.isEmpty())
   {
      dialog.setExtraOptionPrompt(extraOptionPrompt);
      dialog.setExtraOption(extraOptionByDefault);
   }

   if (usePasswordMask)
   {
      // password prompts are shown higher up (because they relate to
      // console progress dialogs which are at the top of the screen)
      QRect parentGeom = pOwner_->asWidget()->geometry();
      int x = parentGeom.left() + (parentGeom.width() / 2) - (dialog.width() / 2);
      dialog.move(x, parentGeom.top() + 75);
   }

   if (numbersOnly)
      dialog.setNumbersOnly(true);
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
   {
      QString value = dialog.textValue();
      bool extraOption = dialog.extraOption();
      QString values;
      values += value;
      values += QString::fromAscii("\n");
      values += extraOption ? QString::fromAscii("1") : QString::fromAscii("0");
      return values;
   }
   else
      return QString();
}

void GwtCallback::checkForUpdates()
{
   pMainWindow_->checkForUpdates();
}

bool GwtCallback::supportsFullscreenMode()
{
   return desktop::supportsFullscreenMode(pMainWindow_);
}

void GwtCallback::toggleFullscreenMode()
{
   desktop::toggleFullscreenMode(pMainWindow_);
}

void GwtCallback::showKeyboardShortcutHelp()
{
   FilePath keyboardHelpPath = options().wwwDocsPath().complete("keyboard.htm");
   QString file = QString::fromUtf8(keyboardHelpPath.absolutePath().c_str());
   QUrl url = QUrl::fromLocalFile(file);
   desktop::openUrl(url);
}

void GwtCallback::showAboutDialog()
{
   // WA_DeleteOnClose
   AboutDialog* about = new AboutDialog(pOwner_->asWidget());
   about->setAttribute(Qt::WA_DeleteOnClose);
   about->show();
}

void GwtCallback::bringMainFrameToFront()
{
   desktop::raiseAndActivateWindow(pMainWindow_);
}

QString GwtCallback::filterText(QString text)
{
   // Ace doesn't do well with NFD Unicode text. To repro on
   // Mac OS X, create a folder on disk with accented characters
   // in the name, then create a file in that folder. Do a
   // Get Info on the file and copy the path. Now you'll have
   // an NFD string on the clipboard.
   return text.normalized(QString::NormalizationForm_C);
}

#ifdef __APPLE__

namespace {

template <typename TValue>
class CFReleaseHandle
{
public:
   CFReleaseHandle(TValue value=NULL)
   {
      value_ = value;
   }

   ~CFReleaseHandle()
   {
      if (value_)
         CFRelease(value_);
   }

   TValue& value()
   {
      return value_;
   }

   operator TValue () const
   {
      return value_;
   }

   TValue* operator& ()
   {
      return &value_;
   }

private:
   TValue value_;
};

OSStatus addToPasteboard(PasteboardRef pasteboard,
                         int slot,
                         CFStringRef flavor,
                         const QByteArray& data)
{
   CFReleaseHandle<CFDataRef> dataRef = CFDataCreate(
         NULL,
         reinterpret_cast<const UInt8*>(data.constData()),
         data.length());

   if (!dataRef)
      return memFullErr;

   return ::PasteboardPutItemFlavor(pasteboard,
                                    reinterpret_cast<PasteboardItemID>(slot),
                                    flavor, dataRef, 0);
}

} // anonymous namespace

void GwtCallback::cleanClipboard(bool stripHtml)
{
   CFReleaseHandle<PasteboardRef> clipboard;
   if (::PasteboardCreate(kPasteboardClipboard, &clipboard))
      return;

   ::PasteboardSynchronize(clipboard);

   ItemCount itemCount;
   if (::PasteboardGetItemCount(clipboard, &itemCount) || itemCount < 1)
      return;

   PasteboardItemID itemId;
   if (::PasteboardGetItemIdentifier(clipboard, 1, &itemId))
      return;


   /*
   CFReleaseHandle<CFArrayRef> flavorTypes;
   if (::PasteboardCopyItemFlavors(clipboard, itemId, &flavorTypes))
      return;
   for (int i = 0; i < CFArrayGetCount(flavorTypes); i++)
   {
      CFStringRef flavorType = (CFStringRef)CFArrayGetValueAtIndex(flavorTypes, i);
      char buffer[1000];
      if (!CFStringGetCString(flavorType, buffer, 1000, kCFStringEncodingMacRoman))
         return;
      qDebug() << buffer;
   }
   */

   CFReleaseHandle<CFDataRef> data;
   if (::PasteboardCopyItemFlavorData(clipboard,
                                      itemId,
                                      CFSTR("public.utf16-plain-text"),
                                      &data))
   {
      return;
   }

   CFReleaseHandle<CFDataRef> htmlData;
   OSStatus err;
   if (!stripHtml && (err = ::PasteboardCopyItemFlavorData(clipboard, itemId, CFSTR("public.html"), &htmlData)))
   {
      if (err != badPasteboardFlavorErr)
         return;
   }

   CFIndex len = ::CFDataGetLength(data);
   QByteArray buffer;
   buffer.resize(len);
   ::CFDataGetBytes(data, CFRangeMake(0, len), reinterpret_cast<UInt8*>(buffer.data()));
   QString str = QString::fromUtf16(reinterpret_cast<const ushort*>(buffer.constData()), buffer.length()/2);

   if (::PasteboardClear(clipboard))
      return;
   if (addToPasteboard(clipboard, 1, CFSTR("public.utf8-plain-text"), str.toUtf8()))
      return;

   if (htmlData.value())
   {
      ::PasteboardPutItemFlavor(clipboard,
                                (PasteboardItemID)1,
                                CFSTR("public.html"),
                                htmlData,
                                0);
   }
}
#else

void GwtCallback::cleanClipboard(bool stripHtml)
{
}

#endif

void GwtCallback::setPendingQuit(int pendingQuit)
{
   pendingQuit_ = pendingQuit;
}

int GwtCallback::collectPendingQuitRequest()
{
   if (pendingQuit_ != PendingQuitNone)
   {
      int pendingQuit = pendingQuit_;
      pendingQuit_ = PendingQuitNone;
      return pendingQuit;
   }
   else
   {
      return PendingQuitNone;
   }
}

void GwtCallback::openProjectInNewWindow(QString projectFilePath)
{
   launchProjectInNewInstance(resolveAliasedPath(projectFilePath));
}

void GwtCallback::openTerminal(QString terminalPath,
                               QString workingDirectory,
                               QString extraPathEntries)
{
   // append extra path entries to our path before launching
   std::string path = core::system::getenv("PATH");
   std::string previousPath = path;
   core::system::addToPath(&path, extraPathEntries.toStdString());
   core::system::setenv("PATH", path);

#if defined(Q_WS_MACX)

   // call Terminal.app with an applescript that navigates it
   // to the specified directory. note we don't reference the
   // passed terminalPath because this setting isn't respected
   // on the Mac (we always use Terminal.app)
   FilePath macTermScriptFilePath =
      desktop::options().scriptsPath().complete("mac-terminal");
   QString macTermScriptPath = QString::fromUtf8(
         macTermScriptFilePath.absolutePath().c_str());
   QStringList args;
   args.append(resolveAliasedPath(workingDirectory));
   QProcess::startDetached(macTermScriptPath, args);

#elif defined(Q_WS_WIN)

   // git bash
   if (terminalPath.length() > 0)
   {
      QStringList args;
      args.append(QString::fromAscii("--login"));
      args.append(QString::fromAscii("-i"));
      QProcess::startDetached(terminalPath,
                              args,
                              resolveAliasedPath(workingDirectory));
   }
   else
   {
      // set HOME to USERPROFILE so msys ssh can find our keys
      std::string previousHome = core::system::getenv("HOME");
      std::string userProfile = core::system::getenv("USERPROFILE");
      core::system::setenv("HOME", userProfile);

      // run the process
      QProcess::startDetached(QString::fromAscii("cmd.exe"),
                              QStringList(),
                              resolveAliasedPath(workingDirectory));

      // revert to previous home
      core::system::setenv("HOME", previousHome);
   }


#elif defined(Q_WS_X11)

   // start the auto-detected terminal (or user-specified override)
   if (!terminalPath.length() == 0)
   {
      QStringList args;
      QProcess::startDetached(terminalPath,
                              args,
                              resolveAliasedPath(workingDirectory));
   }
   else
   {
      desktop::showWarning(
         NULL,
         QString::fromAscii("Terminal Not Found"),
         QString::fromAscii(
                  "Unable to find a compatible terminal program to launch"));
   }

#endif

   // restore previous path
   core::system::setenv("PATH", previousPath);
}

bool isProportionalFont(QString fontFamily)
{
   QFont font(fontFamily, 12);
   return !isFixedWidthFont(font);
}

QString GwtCallback::getFontList(bool fixedWidthOnly)
{
   QFontDatabase db;
   QStringList families = db.families();

   if (fixedWidthOnly)
   {
      QStringList::iterator it = std::remove_if(
            families.begin(), families.end(), isProportionalFont);
      families.erase(it, families.end());
   }

   return families.join(QString::fromAscii("\n"));
}

QString GwtCallback::getFixedWidthFont()
{
   return options().fixedWidthFont();
}

void GwtCallback::setFixedWidthFont(QString font)
{
   options().setFixedWidthFont(font);
}

QString GwtCallback::getZoomLevels()
{
   QStringList zoomLevels;
   BOOST_FOREACH(double zoomLevel, pMainWindow_->zoomLevels())
   {
      zoomLevels.append(QString::fromStdString(
                           safe_convert::numberToString(zoomLevel)));
   }
   return zoomLevels.join(QString::fromAscii("\n"));
}

double GwtCallback::getZoomLevel()
{
   return options().zoomLevel();
}

void GwtCallback::setZoomLevel(double zoomLevel)
{
   options().setZoomLevel(zoomLevel);
}


QString GwtCallback::getDesktopSynctexViewer()
{
    return Synctex::desktopViewerInfo().name;
}

void GwtCallback::externalSynctexPreview(QString pdfPath, int page)
{
   synctex().syncView(resolveAliasedPath(pdfPath), page);
}

void GwtCallback::externalSynctexView(const QString& pdfFile,
                                      const QString& srcFile,
                                      int line,
                                      int column)
{
   synctex().syncView(resolveAliasedPath(pdfFile),
                      resolveAliasedPath(srcFile),
                      QPoint(line, column));
}

void GwtCallback::launchSession(bool reload)
{
   pMainWindow_->launchSession(reload);
}


void GwtCallback::activateAndFocusOwner()
{
   desktop::raiseAndActivateWindow(pOwner_->asWidget());
   pOwner_->webPage()->mainFrame()->setFocus();
}

void GwtCallback::reloadZoomWindow()
{
   QWidgetList topLevels = QApplication::topLevelWidgets();
   for (int i = 0; i < topLevels.size(); i++)
   {
      QWidget* pWindow = topLevels.at(i);
      if (!pWindow->isVisible())
         continue;

      if (pWindow->windowTitle() == QString::fromAscii("Plot Zoom"))
      {
         // do the reload
         BrowserWindow* pBrowserWindow = (BrowserWindow*)pWindow;
         pBrowserWindow->webView()->reload();

         break;
      }
   }
}

void GwtCallback::setViewerUrl(QString url)
{
   pOwner_->webPage()->setViewerUrl(url);
}

bool GwtCallback::isOSXMavericks()
{
   return desktop::isOSXMavericks();
}

QString GwtCallback::getScrollingCompensationType()
{
#if defined(Q_WS_MACX)
   return QString::fromAscii("Mac");
#elif defined(Q_WS_WIN)
   return QString::fromAscii("Win");
#else
   return QString::fromAscii("None");
#endif
}
} // namespace desktop
