/*
 * DesktopGwtCallback.cpp
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

#include "DesktopGwtCallback.hpp"
#include "DesktopGwtWindow.hpp"

#ifdef _WIN32
#include <shlobj.h>
#endif

#include <QFileDialog>
#include <QMessageBox>
#include <QApplication>
#include <QAbstractButton>

#include <QtPrintSupport/QPrinter>
#include <QtPrintSupport/QPrintPreviewDialog>

#include <shared_core/FilePath.hpp>
#include <core/FileUtils.hpp>
#include <core/DateTime.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/RUserData.hpp>

#include "DesktopActivationOverlay.hpp"
#include "DesktopSessionServersOverlay.hpp"
#include "DesktopOptions.hpp"
#include "DesktopUtils.hpp"
#include "DesktopBrowserWindow.hpp"
#include "DesktopWindowTracker.hpp"
#include "DesktopInputDialog.hpp"
#include "DesktopSecondaryWindow.hpp"
#include "DesktopRVersion.hpp"
#include "DesktopMainWindow.hpp"
#include "DesktopSynctex.hpp"
#include "DesktopJobLauncherOverlay.hpp"
#include "RemoteDesktopSessionLauncherOverlay.hpp"

#ifdef __APPLE__
#include <Carbon/Carbon.h>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

namespace {
WindowTracker s_windowTracker;

#ifdef Q_OS_LINUX
QString s_globalMouseSelection;
bool s_ignoreNextClipboardSelectionChange;
#endif

} // end anonymous namespace

extern QString scratchPath;

GwtCallback::GwtCallback(MainWindow* pMainWindow, GwtWindow* pOwner, bool isRemoteDesktop)
   : pMainWindow_(pMainWindow),
     pOwner_(pOwner),
     isRemoteDesktop_(isRemoteDesktop),
     pSynctex_(nullptr),
     pendingQuit_(PendingQuitNone)
{
   initialize();
   
#ifdef Q_OS_LINUX
   // assume light theme on startup (theme will be dynamically updated
   // based on editor theme chosen by user)
   syncToEditorTheme(false);

   // listen for clipboard selection change events (X11 only); allow override in options or 
   // via environment variable. clipboard monitoring enables us to support middle-click paste on
   // Linux, but it causes problems on some systems.
   if (desktop::options().clipboardMonitoring() &&
       core::system::getenv("RSTUDIO_NO_CLIPBOARD_MONITORING").empty())
   {
      QClipboard* clipboard = QApplication::clipboard();
      if (clipboard->supportsSelection())
      {
         QObject::connect(
                  clipboard, &QClipboard::selectionChanged,
                  this, &GwtCallback::onClipboardSelectionChanged,
                  Qt::DirectConnection);

         // initialize the global selection
         const QMimeData* mimeData = clipboard->mimeData(QClipboard::Selection);
         if (mimeData->hasText())
            s_globalMouseSelection = mimeData->text();
      }
   }
#endif
}

#ifndef Q_OS_MAC
void GwtCallback::initialize()
{
}
#endif

Synctex& GwtCallback::synctex()
{
   if (pSynctex_ == nullptr)
      pSynctex_ = Synctex::create(pMainWindow_);

   return *pSynctex_;
}

void GwtCallback::printText(QString text)
{
   QPrinter printer;
   printer.setPageMargins(1.0, 1.0, 1.0, 1.0, QPrinter::Inch);

   QPrintPreviewDialog dialog(&printer);
   dialog.setWindowModality(Qt::WindowModal);

   // QPrintPreviewDialog will call us back to paint the contents
   connect(&dialog, SIGNAL(paintRequested(QPrinter*)),
           this, SLOT(paintPrintText(QPrinter*)));
   connect(&dialog, SIGNAL(finished(int)),
           this, SLOT(printFinished(int)));

   // cache the requested print text to replay for the print preview
   printText_ = text;

   dialog.exec();
}

void GwtCallback::paintPrintText(QPrinter* printer)
{
    QPainter painter;
    painter.begin(printer);

    // look up the system fixed font
    QFont fixedFont = QFontDatabase::systemFont(QFontDatabase::FixedFont);
    fixedFont.setPointSize(10);
    painter.setFont(fixedFont);

    // flags used for drawing text
    int textFlags = Qt::AlignLeft | Qt::AlignTop | Qt::TextWordWrap;

    // y coordinate for new line of text to be drawn
    int y = 0;

    // compute page boundaries (used as bounds when painting on page)
    int pageWidth = printer->pageRect().width();
    int pageHeight = printer->pageRect().height();
    QRect pageRect(0, 0, pageWidth, pageHeight);

    // start drawing line-by-line
    QStringList lines = printText_.split(QStringLiteral("\n"));
    for (auto line : lines)
    {
       // figure out what rectangle is needed to fit
       // (use dummy text for blank lines when computing bounds)
       QRect paintRect = painter.boundingRect(
                pageRect,
                textFlags,
                line.isEmpty() ? QStringLiteral("Z") : line);

       // move the bounding rectangle down
       paintRect.moveTo(0, y);

       // check for page overflow and start a new page if so
       if (paintRect.bottom() > pageHeight)
       {
          printer->newPage();
          paintRect.moveTo(0, 0);
          y = 0;
       }

       // draw the text
       painter.drawText(paintRect, textFlags, line);

       // update y
       y += paintRect.height();
    }

    painter.end();
}

void GwtCallback::printFinished(int result)
{
   // signal emitted by QPrintPreviewDialog when the dialog is dismissed
   printText_.clear();
}

void GwtCallback::browseUrl(QString url)
{
   QUrl qurl = QUrl::fromEncoded(url.toUtf8());

#ifdef Q_OS_MAC
   if (qurl.scheme() == QString::fromUtf8("file"))
   {
      QProcess open;
      QStringList args;
      // force use of Preview for PDFs (Adobe Reader 10.01 crashes)
      if (url.toLower().endsWith(QString::fromUtf8(".pdf")))
      {
         args.append(QString::fromUtf8("-a"));
         args.append(QString::fromUtf8("Preview"));
         args.append(url);
      }
      else
      {
         args.append(url);
      }
      open.start(QString::fromUtf8("open"), args);
      open.waitForFinished(5000);
      if (open.exitCode() != 0)
      {
         // Probably means that the file doesn't have a registered
         // application or something.
         QProcess reveal;
         reveal.startDetached(QString::fromUtf8("open"), QStringList() << QString::fromUtf8("-R") << url);
      }
      return;
   }
#endif

   desktop::openUrl(qurl);
}

#ifndef Q_OS_MAC

QString GwtCallback::getOpenFileName(const QString& caption,
                                     const QString& label,
                                     const QString& dir,
                                     const QString& filter,
                                     bool canChooseDirectories,
                                     bool focusOwner)
{
   QString resolvedDir = desktop::resolveAliasedPath(dir);

   QWidget* owner = focusOwner ? pOwner_->asWidget() : qApp->focusWidget();
   QFileDialog dialog(
            owner,
            caption,
            resolvedDir,
            filter);

   QFileDialog::FileMode mode = (canChooseDirectories)
         ? QFileDialog::AnyFile
         : QFileDialog::ExistingFile;

   dialog.setFileMode(mode);
   dialog.setLabelText(QFileDialog::Accept, label);
   dialog.setWindowModality(Qt::WindowModal);

   // don't resolve links on non-Windows platforms
   // https://github.com/rstudio/rstudio/issues/2476
   // https://github.com/rstudio/rstudio/issues/7327
#ifndef _WIN32
   dialog.setOption(QFileDialog::DontResolveSymlinks, true);
#endif
   
   QString result;
   if (dialog.exec() == QDialog::Accepted)
      result = dialog.selectedFiles().value(0);

   desktop::raiseAndActivateWindow(owner);
   return createAliasedPath(result);
}

namespace {

QString getSaveFileNameImpl(QWidget* pParent,
                            const QString& caption,
                            const QString& label,
                            const QString& dir,
                            QFileDialog::Options options)
{
   // initialize dialog
   QFileDialog dialog(pParent, caption, dir);
   dialog.setOptions(options);
   dialog.setLabelText(QFileDialog::Accept, label);
   dialog.setAcceptMode(QFileDialog::AcceptSave);
   dialog.setWindowModality(Qt::WindowModal);

   // execute dialog and check for success
   if (dialog.exec() == QDialog::Accepted)
      return dialog.selectedFiles().value(0);

   return QString();
}

} // end anonymous namespace

QString GwtCallback::getSaveFileName(const QString& caption,
                                     const QString& label,
                                     const QString& dir,
                                     const QString& defaultExtension,
                                     bool forceDefaultExtension,
                                     bool focusOwner)
{
   QString resolvedDir = desktop::resolveAliasedPath(dir);

   while (true)
   {
      QWidget* owner = focusOwner ? pOwner_->asWidget() : qApp->focusWidget();
      QString result = getSaveFileNameImpl(
                  owner,
                  caption,
                  label,
                  resolvedDir,
                  standardFileDialogOptions());

      desktop::raiseAndActivateWindow(owner);
      if (result.isEmpty())
         return result;

      if (!defaultExtension.isEmpty())
      {
         FilePath fp(result.toUtf8().constData());
         if (fp.getExtension().empty() ||
            (forceDefaultExtension &&
            (fp.getExtension() != defaultExtension.toStdString())))
         {
            result += defaultExtension;
            FilePath newExtPath(result.toUtf8().constData());
            if (newExtPath.exists())
            {
               std::string message = "\"" + newExtPath.getFilename() + "\" already "
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
                                          const QString& label,
                                          const QString& dir,
                                          bool focusOwner)
{
   return desktop::browseDirectory(caption,
                                   label,
                                   dir,
                                   focusOwner ? pOwner_->asWidget() : qApp->focusWidget());
}

#endif

void GwtCallback::onClipboardSelectionChanged()
{
#ifdef Q_OS_LINUX
    // for some reason, Qt can get stalled querying the clipboard
    // while a modal is active, so disable any such behavior here
    if (QApplication::activeModalWidget() != nullptr)
       return;

    // check to see if this was a clipboard change synthesized by us;
    // if so, discard it
    if (s_ignoreNextClipboardSelectionChange)
    {
       s_ignoreNextClipboardSelectionChange = false;
       return;
    }

    // we only care about text-related changes, so bail if we didn't
    // get text in the selection clipboard
    QClipboard* clipboard = QApplication::clipboard();
    const QMimeData* mimeData = clipboard->mimeData(QClipboard::Selection);
    if (mimeData->hasText())
    {
       // extract clipboard selection text
       QString text = mimeData->text();

       // when one clicks on an Ace instance, a hidden length-one selection
       // will sneak in here -- explicitly screen those out
       if (text == QStringLiteral("\x01"))
       {
          // ignore the next clipboard change (just in case modifying it below triggers
          // this slot recursively)
          s_ignoreNextClipboardSelectionChange = true;

          // restore the old global selection
          clipboard->setText(s_globalMouseSelection, QClipboard::Selection);
       }
       else
       {
          // otherwise, update our tracked global selection
          s_globalMouseSelection = text;
       }
    }
#endif
}

void GwtCallback::doAction(const QKeySequence& keys)
{
   int keyCode = keys[0];
   auto modifiers = static_cast<Qt::KeyboardModifier>(keyCode & Qt::KeyboardModifierMask);
   keyCode &= ~Qt::KeyboardModifierMask;

   QKeyEvent* keyEvent = new QKeyEvent(QKeyEvent::KeyPress, keyCode, modifiers);
   pOwner_->postWebViewEvent(keyEvent);
}

void GwtCallback::doAction(QKeySequence::StandardKey key)
{
   QList<QKeySequence> bindings = QKeySequence::keyBindings(key);
   if (bindings.empty())
      return;

   doAction(bindings.first());
}

void GwtCallback::undo()
{
   doAction(QKeySequence::Undo);
}

void GwtCallback::redo()
{
   // NOTE: On Windows, the default redo key sequence is 'Ctrl+Y';
   // however, we bind this to 'yank' and so 'redo' actions executed
   // from the menu will fail. We instead use 'Ctrl+Shift+Z' which is
   // supported across all platforms using Qt.
   static const QKeySequence keys =
         QKeySequence::fromString(QString::fromUtf8("Ctrl+Shift+Z"));

   doAction(keys);
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

void GwtCallback::setClipboardText(QString text)
{
   QClipboard* pClipboard = QApplication::clipboard();
   pClipboard->setText(text, QClipboard::Clipboard);
}

QString GwtCallback::getClipboardText()
{
   QClipboard* pClipboard = QApplication::clipboard();
   return pClipboard->text(QClipboard::Clipboard);
}

QJsonArray GwtCallback::getClipboardUris()
{
   QJsonArray urisJson;
   QClipboard* pClipboard = QApplication::clipboard();
   if (pClipboard->mimeData()->hasUrls())
   {
      // build buffer of urls
      auto urls = pClipboard->mimeData()->urls();
      for (auto url : urls)
      {
         // append (converting file-based urls)
         if (url.scheme() == QString::fromUtf8("file"))
            urisJson.append(QJsonValue(createAliasedPath(url.toLocalFile())));
         else
            urisJson.append(QJsonValue(url.toString()));
      }
   }
   return urisJson;
}

QString GwtCallback::getClipboardImage()
{
   QClipboard* pClipboard = QApplication::clipboard();
   if (pClipboard->mimeData()->hasImage())
   {
      QImage image = qvariant_cast<QImage>(pClipboard->mimeData()->imageData());
      FilePath tempDir = options().scratchTempDir();
      FilePath imagePath = file_utils::uniqueFilePath(tempDir, "paste-", ".png");
      QString imageFile = QString::fromStdString(imagePath.getAbsolutePath());
      if (image.save(imageFile))
         return imageFile;
   }
   return QString();
}

void GwtCallback::setGlobalMouseSelection(QString selection)
{
#ifdef Q_OS_LINUX
   QClipboard* clipboard = QApplication::clipboard();
   if (clipboard->supportsSelection())
      clipboard->setText(selection, QClipboard::Selection);
   s_globalMouseSelection = selection;
#endif
}

QString GwtCallback::getGlobalMouseSelection()
{
#ifdef Q_OS_LINUX
   return s_globalMouseSelection;
#else
   return QString();
#endif
}

QJsonObject GwtCallback::getCursorPosition()
{
   QPoint cursorPosition = QCursor::pos();

   return QJsonObject {
      { QStringLiteral("x"), cursorPosition.x() },
      { QStringLiteral("y"), cursorPosition.y() }
   };
}

bool GwtCallback::doesWindowExistAtCursorPosition()
{
   return qApp->topLevelAt(QCursor::pos()) != nullptr;
}

QString GwtCallback::proportionalFont()
{
   return options().proportionalFont();
}

QString GwtCallback::fixedWidthFont()
{
   return options().fixedWidthFont();
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

   path = desktop::resolveAliasedPath(path);

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

   path = desktop::resolveAliasedPath(path);

   desktop::openUrl(QUrl::fromLocalFile(path));
}

#ifndef Q_OS_MAC

void GwtCallback::showWordDoc(QString path)
{
#ifdef Q_OS_WIN32

   path = desktop::resolveAliasedPath(path);
   Error error = wordViewer_.showItem(path.toStdWString());
   if (error)
   {
      LOG_ERROR(error);
      showFile(path);
   }

#else
   // Invoke default viewer on other platforms
   showFile(path);
#endif
}

void GwtCallback::showPptPresentation(QString path)
{
#ifdef Q_OS_WIN32

   path = desktop::resolveAliasedPath(path);
   Error error = pptViewer_.showItem(path.toStdWString());
   if (error)
   {
      LOG_ERROR(error);
      showFile(path);
   }
#else
   // Invoke default viewer on other platforms
   showFile(path);
#endif
}

#endif

void GwtCallback::showPDF(QString path, int pdfPage)
{
   path = desktop::resolveAliasedPath(path);
   
#ifdef Q_OS_MAC
   desktop::openFile(path);
#else
   synctex().view(path, pdfPage);
#endif
}

void GwtCallback::prepareShowWordDoc()
{
#ifdef Q_OS_WIN32
   Error error = wordViewer_.closeLastViewedItem();
   if (error)
   {
      LOG_ERROR(error);
   }
#endif
}

void GwtCallback::prepareShowPptPresentation()
{
#ifdef Q_OS_WIN32
   Error error = pptViewer_.closeLastViewedItem();
   if (error)
   {
      LOG_ERROR(error);
   }
#endif
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

double GwtCallback::devicePixelRatio()
{
   return desktop::devicePixelRatio(pMainWindow_);
}

void GwtCallback::openMinimalWindow(QString name,
                                    QString url,
                                    int width,
                                    int height)
{
   bool named = !name.isEmpty() && name != QString::fromUtf8("_blank");

   BrowserWindow* browser = nullptr;
   if (named)
      browser = s_windowTracker.getWindow(name);

   if (!browser)
   {
      bool isViewerZoomWindow =
          (name == QString::fromUtf8("_rstudio_viewer_zoom"));

      // create the new browser window; pass along our own base URL so that the new window's
      // WebProfile knows how to apply the appropriate headers
      browser = new BrowserWindow(false, !isViewerZoomWindow, name, 
            pMainWindow_->webView()->baseUrl(), nullptr, pMainWindow_->webPage());
      
      browser->setAttribute(Qt::WA_DeleteOnClose, true);
      browser->setAttribute(Qt::WA_QuitOnClose, true);
      
      // ensure minimal windows can be closed with Ctrl+W (Cmd+W on macOS)
      QAction* closeWindow = new QAction(browser);
      closeWindow->setShortcut(Qt::CTRL + Qt::Key_W);
      connect(closeWindow, &QAction::triggered,
              browser, &BrowserWindow::close);
      browser->addAction(closeWindow);
      
      connect(this, &GwtCallback::destroyed, browser, &BrowserWindow::close);
      
      if (named)
         s_windowTracker.addWindow(name, browser);

      // set title for viewer zoom
      if (isViewerZoomWindow)
         browser->setWindowTitle(QString::fromUtf8("Viewer Zoom"));
   }

   browser->webView()->load(QUrl(url));
   browser->resize(width, height);
   browser->show();
   browser->activateWindow();
}

void GwtCallback::activateMinimalWindow(QString name)
{
   // we can only activate named windows
   bool named = !name.isEmpty() && name != QString::fromUtf8("_blank");
   if (!named)
      return;

   pOwner_->webPage()->activateWindow(name);
}

void GwtCallback::prepareForSatelliteWindow(QString name,
                                            int x,
                                            int y,
                                            int width,
                                            int height)
{
   pOwner_->webPage()->prepareForWindow(
                PendingWindow(name, pMainWindow_, x, y, width, height));
}

void GwtCallback::prepareForNamedWindow(QString name,
                                        bool allowExternalNavigate,
                                        bool showDesktopToolbar)
{
   pOwner_->webPage()->prepareForWindow(
                PendingWindow(name, allowExternalNavigate, showDesktopToolbar));
}

void GwtCallback::closeNamedWindow(QString name)
{
   // close the requested window
   pOwner_->webPage()->closeWindow(name);

   // bring the main window to the front (so we don't lose RStudio context
   // entirely)
   desktop::raiseAndActivateWindow(pMainWindow_);
}

void GwtCallback::activateSatelliteWindow(QString name)
{
   pOwner_->webPage()->activateWindow(name);
}

void GwtCallback::copyPageRegionToClipboard(int left, int top, int width, int height)
{
   auto* view = pMainWindow_->webView();

   double scale = view->zoomFactor();
   left = left * scale;
   top = top * scale;
   width = width * scale;
   height = height * scale;

   QPixmap pixmap = view->grab(QRect(left, top, width, height));
   QApplication::clipboard()->setPixmap(pixmap);
}

void GwtCallback::exportPageRegionToFile(QString targetPath,
                                         QString format,
                                         int left,
                                         int top,
                                         int width,
                                         int height)
{
   // resolve target path
   targetPath = desktop::resolveAliasedPath(targetPath);

   // get the pixmap
#if QT_VERSION < QT_VERSION_CHECK(5, 13, 0)
   QPixmap pixmap = QPixmap::grabWidget(pMainWindow_->webView(),
                                        left,
                                        top,
                                        width,
                                        height);
#else
   QPixmap pixmap = pMainWindow_->webView()->grab(QRect(left, top, width, height));
#endif

   // save the file
   pixmap.save(targetPath, format.toUtf8().constData(), 100);
}


bool GwtCallback::supportsClipboardMetafile()
{
#ifdef Q_OS_WIN32
   return true;
#else
   return false;
#endif
}

#ifndef Q_OS_MAC

namespace {
QMessageBox::ButtonRole captionToRole(const QString& caption)
{
   if (caption == QString::fromUtf8("OK"))
      return QMessageBox::AcceptRole;
   else if (caption == QString::fromUtf8("Cancel"))
      return QMessageBox::RejectRole;
   else if (caption == QString::fromUtf8("Yes"))
      return QMessageBox::YesRole;
   else if (caption == QString::fromUtf8("No"))
      return QMessageBox::NoRole;
   else if (caption == QString::fromUtf8("Save"))
      return QMessageBox::AcceptRole;
   else if (caption == QString::fromUtf8("Don't Save"))
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
   // cancel other message box if it's visible
   auto* pMsgBox = qobject_cast<QMessageBox*>(
                        QApplication::activeModalWidget());
   if (pMsgBox != nullptr)
      pMsgBox->close();

   QMessageBox msgBox(pOwner_->asWidget());
   msgBox.setWindowTitle(caption);
   msgBox.setText(message);
   msgBox.setIcon(safeMessageBoxIcon(static_cast<QMessageBox::Icon>(type)));
   msgBox.setWindowFlags(Qt::Dialog | Qt::Sheet);
   msgBox.setWindowModality(Qt::WindowModal);
   msgBox.setWindowFlag(Qt::WindowContextHelpButtonHint, false);
   msgBox.setTextFormat(Qt::PlainText);

   QStringList buttonList = buttons.split(QChar::fromLatin1('|'));

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

#endif

QString GwtCallback::promptForText(QString title,
                                   QString caption,
                                   QString defaultValue,
                                   int inputType,
                                   QString extraOptionPrompt,
                                   bool extraOptionByDefault,
                                   int selectionStart,
                                   int selectionLength,
                                   QString okButtonCaption)
{
   InputDialog dialog(pOwner_->asWidget());
   dialog.setWindowTitle(title);
   dialog.setCaption(caption);
   InputType type = static_cast<InputType>(inputType);
   bool usePasswordMask = type == InputPassword;
   dialog.setRequired(type == InputRequiredText);

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

   if (type == InputNumeric)
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
      values += QString::fromUtf8("\n");
      values += extraOption ? QString::fromUtf8("1") : QString::fromUtf8("0");
      return values;
   }
   else
      return QString();
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
   FilePath keyboardHelpPath = options().wwwDocsPath().completePath("keyboard.htm");
   QString file = QString::fromUtf8(keyboardHelpPath.getAbsolutePath().c_str());
   QUrl url = QUrl::fromLocalFile(file);
   desktop::openUrl(url);
}

void GwtCallback::bringMainFrameToFront()
{
   desktop::raiseAndActivateWindow(pMainWindow_);
}

void GwtCallback::bringMainFrameBehindActive()
{
   desktop::moveWindowBeneath(QApplication::activeWindow(), pMainWindow_);
}

QString GwtCallback::desktopRenderingEngine()
{
   return desktop::options().desktopRenderingEngine();
}

void GwtCallback::setDesktopRenderingEngine(QString engine)
{
   desktop::options().setDesktopRenderingEngine(engine);
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
   CFReleaseHandle(TValue value=nullptr)
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
         nullptr,
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
   if (!isRemoteDesktop_)
   {
      std::vector<std::string> args;
      args.push_back(desktop::resolveAliasedPath(projectFilePath).toStdString());
      pMainWindow_->launchRStudio(args);
   }
   else
   {
      // start new Remote Desktop RStudio process with the session URL
      pMainWindow_->launchRemoteRStudioProject(projectFilePath);
   }
}

void GwtCallback::openSessionInNewWindow(QString workingDirectoryPath)
{
   if (!isRemoteDesktop_)
   {
      workingDirectoryPath = desktop::resolveAliasedPath(workingDirectoryPath);
      pMainWindow_->launchRStudio(std::vector<std::string>(),
                                  workingDirectoryPath.toStdString());
   }
   else
   {
      // start the new session on the currently connected server
      pMainWindow_->launchRemoteRStudio();
   }
}

void GwtCallback::openTerminal(QString terminalPath,
                               QString workingDirectory,
                               QString extraPathEntries,
                               QString shellType)
{
   // append extra path entries to our path before launching
   std::string path = core::system::getenv("PATH");
   std::string previousPath = path;
   core::system::addToPath(&path, extraPathEntries.toStdString());
   core::system::setenv("PATH", path);

#if defined(Q_OS_MAC)

   // call Terminal.app with an applescript that navigates it
   // to the specified directory. note we don't reference the
   // passed terminalPath because this setting isn't respected
   // on the Mac (we always use Terminal.app)
   FilePath macTermScriptFilePath =
      desktop::options().scriptsPath().completePath("mac-terminal");
   QString macTermScriptPath = QString::fromUtf8(
         macTermScriptFilePath.getAbsolutePath().c_str());
   QStringList args;
   args.append(desktop::resolveAliasedPath(workingDirectory));
   QProcess::startDetached(macTermScriptPath, args);

#elif defined(Q_OS_WIN)

   if (terminalPath.length() == 0)
   {
      terminalPath = QString::fromUtf8("cmd.exe");
      shellType = QString::fromUtf8("win-cmd");
   }

   QStringList args;
   std::string previousHome = core::system::getenv("HOME");

   if (shellType == QString::fromUtf8("win-git-bash") ||
       shellType == QString::fromUtf8("win-wsl-bash"))
   {
      args.append(QString::fromUtf8("--login"));
      args.append(QString::fromUtf8("-i"));
   }

   if (shellType != QString::fromUtf8("win-wsl-bash"))
   {
      // set HOME to USERPROFILE so msys ssh can find our keys
      std::string userProfile = core::system::getenv("USERPROFILE");
      core::system::setenv("HOME", userProfile);
   }

   QProcess process;
   process.setProgram(terminalPath);
   process.setArguments(args);
   process.setWorkingDirectory(desktop::resolveAliasedPath(workingDirectory));
   process.setCreateProcessArgumentsModifier([](QProcess::CreateProcessArguments* cpa)
   {
      cpa->flags |= CREATE_NEW_CONSOLE;
      cpa->startupInfo->dwFlags &= ~STARTF_USESTDHANDLES;
   });
   process.startDetached();

   // revert to previous home
   core::system::setenv("HOME", previousHome);

#elif defined(Q_OS_LINUX)

   // start the auto-detected terminal (or user-specified override)
   if (terminalPath.length() != 0)
   {
      QStringList args;
      QProcess::startDetached(terminalPath,
                              args,
                              desktop::resolveAliasedPath(workingDirectory));
   }
   else
   {
      desktop::showWarning(
         nullptr,
         QString::fromUtf8("Terminal Not Found"),
         QString::fromUtf8(
                  "Unable to find a compatible terminal program to launch"),
         QString());
   }

#endif

   // restore previous path
   core::system::setenv("PATH", previousPath);
}

bool isProportionalFont(const QString& fontFamily)
{
   QFont font(fontFamily, 12);
   return !isFixedWidthFont(font);
}

QString GwtCallback::getFixedWidthFontList()
{
   return desktop::getFixedWidthFontList();
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
   for (double zoomLevel : pMainWindow_->zoomLevels())
   {
      zoomLevels.append(QString::fromStdString(
                           safe_convert::numberToString(zoomLevel)));
   }
   return zoomLevels.join(QString::fromUtf8("\n"));
}

double GwtCallback::getZoomLevel()
{
   return desktopInfo().getZoomLevel();
}

void GwtCallback::setZoomLevel(double zoomLevel)
{
   pOwner_->setZoomLevel(zoomLevel);
}

void GwtCallback::zoomIn()
{
   pOwner_->zoomIn();
}

void GwtCallback::zoomOut()
{
   pOwner_->zoomOut();
}

void GwtCallback::zoomActualSize()
{
   pOwner_->zoomActualSize();
}

void GwtCallback::setBackgroundColor(QJsonArray rgbColor)
{
#if defined(NDEBUG) || !defined(_WIN32)
   // don't set the background color in win32 debug builds because Chromium can crash on a fatal assert in
   // debug builds when the background color is changed.
   // https://bitbucket.org/chromiumembedded/cef/issues/2144
   int red   = rgbColor.at(0).toInt();
   int green = rgbColor.at(1).toInt();
   int blue  = rgbColor.at(2).toInt();
   
   QColor color = QColor::fromRgb(red, green, blue);
   pOwner_->webPage()->setBackgroundColor(color);
   changeTitleBarColor(red, green, blue);
#endif
}

#ifndef Q_OS_MAC

void GwtCallback::changeTitleBarColor(int red, int green, int blue)
{
}

#endif

void GwtCallback::syncToEditorTheme(bool isDark)
{
   if (isGnomeDesktop() || isWindows())
   {
      applyDesktopTheme(pOwner_, isDark);
   }
}

bool GwtCallback::getEnableAccessibility()
{
   return options().enableAccessibility();
}

void GwtCallback::setEnableAccessibility(bool enable)
{
   options().setEnableAccessibility(enable);
}

bool GwtCallback::getClipboardMonitoring()
{
   return options().clipboardMonitoring();
}

void GwtCallback::setClipboardMonitoring(bool monitoring)
{
   options().setClipboardMonitoring(monitoring);
}

bool GwtCallback::getIgnoreGpuBlacklist()
{
   return options().ignoreGpuBlacklist();
}

void GwtCallback::setIgnoreGpuBlacklist(bool ignore)
{
   options().setIgnoreGpuBlacklist(ignore);
}

bool GwtCallback::getDisableGpuDriverBugWorkarounds()
{
   return options().disableGpuDriverBugWorkarounds();
}

void GwtCallback::setDisableGpuDriverBugWorkarounds(bool disable)
{
   options().setDisableGpuDriverBugWorkarounds(disable);
}

void GwtCallback::showLicenseDialog()
{
   activation().showLicenseDialog(false /*showQuitButton*/);
}

void GwtCallback::showSessionServerOptionsDialog()
{
   sessionServers().showSessionServerOptionsDialog(pMainWindow_);
}

QString GwtCallback::getInitMessages()
{
   std::string message = activation().currentLicenseStateMessage();
   return QString::fromStdString(message);
}

QString GwtCallback::getLicenseStatusMessage()
{
   std::string message = activation().licenseStatus();
   return QString::fromStdString(message);
}

bool GwtCallback::allowProductUsage()
{
   return activation().allowProductUsage();
}

QString GwtCallback::getDesktopSynctexViewer()
{
    return Synctex::desktopViewerInfo().name;
}

void GwtCallback::externalSynctexPreview(QString pdfPath, int page)
{
   synctex().syncView(desktop::resolveAliasedPath(pdfPath), page);
}

void GwtCallback::externalSynctexView(const QString& pdfFile,
                                      const QString& srcFile,
                                      int line,
                                      int column)
{
   synctex().syncView(desktop::resolveAliasedPath(pdfFile),
                      desktop::resolveAliasedPath(srcFile),
                      QPoint(line, column));
}

void GwtCallback::launchSession(bool reload)
{
   pMainWindow_->launchSession(reload);
}


void GwtCallback::activateAndFocusOwner()
{
   desktop::raiseAndActivateWindow(pOwner_->asWidget());
}

void GwtCallback::reloadZoomWindow()
{
   BrowserWindow* pBrowser = s_windowTracker.getWindow(
                     QString::fromUtf8("_rstudio_zoom"));
   if (pBrowser)
      pBrowser->webView()->reload();
}

void GwtCallback::setTutorialUrl(QString url)
{
   pOwner_->webPage()->setTutorialUrl(url);
}

void GwtCallback::setViewerUrl(QString url)
{
   pOwner_->webPage()->setViewerUrl(url);
}

void GwtCallback::setShinyDialogUrl(QString url)
{
   pOwner_->webPage()->setShinyDialogUrl(url);
}

void GwtCallback::reloadViewerZoomWindow(QString url)
{
   BrowserWindow* pBrowser = s_windowTracker.getWindow(
                     QString::fromUtf8("_rstudio_viewer_zoom"));
   if (pBrowser)
      pBrowser->webView()->setUrl(url);
}

bool GwtCallback::isMacOS()
{
   return desktop::isMacOS();
}

bool GwtCallback::isCentOS()
{
   return desktop::isCentOS();
}

QString GwtCallback::getScrollingCompensationType()
{
#if defined(Q_OS_MAC)
   return QString::fromUtf8("Mac");
#elif defined(Q_OS_WIN)
   return QString::fromUtf8("Win");
#else
   return QString::fromUtf8("None");
#endif
}

void GwtCallback::setBusy(bool)
{
#if defined(Q_OS_MAC)
   // call AppNap apis for Mac (we use Cocoa on the Mac though so
   // this codepath will never be hit)
#endif
}

void GwtCallback::setWindowTitle(QString title)
{
   pMainWindow_->setWindowTitle(title + QString::fromUtf8(" - ") + desktop::activation().editionName());
}

#ifdef Q_OS_WIN
void GwtCallback::installRtools(QString version, QString installerPath)
{
   // silent install
   QStringList args;
   args.push_back(QString::fromUtf8("/SP-"));
   args.push_back(QString::fromUtf8("/SILENT"));

   // custom install directory
   std::string systemDrive = core::system::getenv("SYSTEMDRIVE");
   if (!systemDrive.empty() && FilePath(systemDrive).exists())
   {
      std::string dir = systemDrive + "\\RBuildTools\\" + version.toStdString();
      std::string dirArg = "/DIR=" + dir;
      args.push_back(QString::fromStdString(dirArg));
   }

   // launch installer
   QProcess::startDetached(installerPath, args);
}
#else
void GwtCallback::installRtools(QString version, QString installerPath)
{
}
#endif

QString GwtCallback::getDisplayDpi()
{
   return QString::fromStdString(
            safe_convert::numberToString(getDpi()));
}

void GwtCallback::onSessionQuit()
{
   sessionQuit();
}

QJsonObject GwtCallback::getSessionServer()
{
   if (pMainWindow_->getRemoteDesktopSessionLauncher() != nullptr)
      return pMainWindow_->getRemoteDesktopSessionLauncher()->sessionServer().toJson();
   else
      return QJsonObject();
}

QJsonObject GwtCallback::getLauncherServer()
{
   SessionServer server = pMainWindow_->getJobLauncher()->getLauncherServer();
   return server.toJson();
}

QJsonArray GwtCallback::getSessionServers()
{
   QJsonArray serversArray;
   for (const SessionServer& server : sessionServerSettings().servers())
   {
      serversArray.append(server.toJson());
   }

   return serversArray;
}

void GwtCallback::reconnectToSessionServer(const QJsonValue& sessionServerJson)
{
   SessionServer server = !sessionServerJson.isNull() ?
            SessionServer::fromJson(sessionServerJson.toObject()) :
            SessionServer("", "");
   sessionServers().setPendingSessionServerReconnect(server);
   pMainWindow_->close();
}

bool GwtCallback::setLauncherServer(const QJsonObject& sessionServerJson)
{
   SessionServer server = SessionServer::fromJson(sessionServerJson);
   return pMainWindow_->getJobLauncher()->setSessionServer(server);
}

void GwtCallback::connectToLauncherServer()
{
   pMainWindow_->getJobLauncher()->signIn();
}

void GwtCallback::startLauncherJobStatusStream(QString jobId)
{
   pMainWindow_->getJobLauncher()->startLauncherJobStatusStream(jobId.toStdString());
}

void GwtCallback::stopLauncherJobStatusStream(QString jobId)
{
   pMainWindow_->getJobLauncher()->stopLauncherJobStatusStream(jobId.toStdString());
}

void GwtCallback::startLauncherJobOutputStream(QString jobId)
{
   pMainWindow_->getJobLauncher()->startLauncherJobOutputStream(jobId.toStdString());
}

void GwtCallback::stopLauncherJobOutputStream(QString jobId)
{
   pMainWindow_->getJobLauncher()->stopLauncherJobOutputStream(jobId.toStdString());
}

void GwtCallback::controlLauncherJob(QString jobId, QString operation)
{
   pMainWindow_->getJobLauncher()->controlLauncherJob(jobId.toStdString(), operation.toStdString());
}

void GwtCallback::submitLauncherJob(const QJsonObject& job)
{
   // convert qt json object to core json object
   QJsonDocument doc(job);
   std::string jsonStr = doc.toJson().toStdString();

   json::Value val;
   Error error = val.parse(jsonStr);
   if (error)
   {
      // a parse error should not occur here - if it does it indicates a programmer
      // error while invoking this method - as such, forward on the invalid job
      // so the appropriate error event is eventually delivered
      log::logError(error, ERROR_LOCATION);
   }

   json::Object obj;
   if (val.isObject())
      obj = val.getObject();

   if (obj.isEmpty())
   {
      // same as above - if this is not a valid object, it indicates a programmer error
      LOG_ERROR_MESSAGE("Empty job object submitted");
   }

   pMainWindow_->getJobLauncher()->submitLauncherJob(obj);
}

void GwtCallback::getJobContainerUser()
{
   pMainWindow_->getJobLauncher()->getJobContainerUser();
}

void GwtCallback::validateJobsConfig()
{
   pMainWindow_->getJobLauncher()->validateJobsConfig();
}

int GwtCallback::getProxyPortNumber()
{
   return pMainWindow_->getJobLauncher()->getProxyPortNumber();
}

void GwtCallback::signOut()
{
   // set the currently connected session server as a pending reload
   // this ensure that once we sign out, we can reconnect to the server
   // with a completely clean state
   if (pMainWindow_->getRemoteDesktopSessionLauncher() != nullptr)
   {
      sessionServers().setPendingSessionServerReconnect(
               pMainWindow_->getRemoteDesktopSessionLauncher()->sessionServer());
      pMainWindow_->getRemoteDesktopSessionLauncher()->closeOnSignOut();
   }
}

} // namespace desktop
} // namespace rstudio
