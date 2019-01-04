/*
 * DesktopWebView.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include "DesktopWebView.hpp"

#include <QApplication>
#include <QClipboard>
#include <QMenu>
#include <QNetworkReply>
#include <QStyleFactory>

#include <QWebEngineContextMenuData>
#include <QWebEngineSettings>
#include <QWebEngineHistory>

#include <core/system/Environment.hpp>

#include "DesktopBrowserWindow.hpp"

#ifdef _WIN32
#include <windows.h>
#include <wingdi.h>
#endif

namespace rstudio {
namespace desktop {

namespace {

#if QT_VERSION >= QT_VERSION_CHECK(5, 11, 0)

class DevToolsWindow : public QMainWindow
{
public:
   
   DevToolsWindow()
      : webPage_(new QWebEnginePage(this)),
        webView_(new QWebEngineView(this))
   {
      webView_->setPage(webPage_);
      
      QScreen* screen = QApplication::primaryScreen();
      QRect geometry = screen->geometry();
      resize(geometry.width() * 0.7, geometry.height() * 0.7);
      
      setWindowTitle(QStringLiteral("RStudio DevTools"));
      setAttribute(Qt::WA_DeleteOnClose, true);
      setAttribute(Qt::WA_QuitOnClose, true);
      
      setCentralWidget(webView_);
   }
   
   QWebEnginePage* webPage() { return webPage_; }
   QWebEngineView* webView() { return webView_; }
   
private:
   QWebEnginePage* webPage_;
   QWebEngineView* webView_;
};

std::map<QWebEnginePage*, DevToolsWindow*> s_devToolsWindows;

#endif

} // end anonymous namespace

WebView::WebView(QUrl baseUrl, QWidget *parent, bool allowExternalNavigate) :
    QWebEngineView(parent),
    baseUrl_(baseUrl)
{
#ifdef Q_OS_LINUX
   if (!core::system::getenv("KDE_FULL_SESSION").empty())
   {
      QString fusion = QString::fromUtf8("fusion");
      if (QStyleFactory::keys().contains(fusion))
         setStyle(QStyleFactory::create(fusion));
   }
#endif
   pWebPage_ = new WebPage(baseUrl, this, allowExternalNavigate);
   setPage(pWebPage_);
}

void WebView::setBaseUrl(const QUrl& baseUrl)
{
   baseUrl_ = baseUrl;
   pWebPage_->setBaseUrl(baseUrl_);
}

QUrl WebView::baseUrl()
{
   return baseUrl_;
}

void WebView::activateSatelliteWindow(QString name)
{
   pWebPage_->activateWindow(name);
}

void WebView::prepareForWindow(const PendingWindow& pendingWnd)
{
   pWebPage_->prepareForWindow(pendingWnd);
}

QString WebView::promptForFilename(const QNetworkRequest& request,
                                   QNetworkReply* pReply = nullptr)
{
   QString defaultFileName = QFileInfo(request.url().path()).fileName();

   // Content-Disposition's filename parameter should be used as the
   // default, if present.
   if (pReply && pReply->hasRawHeader("content-disposition"))
   {
      QString headerValue = QString::fromUtf8(pReply->rawHeader("content-disposition"));
      QRegExp regexp(QString::fromUtf8("filename=\"?([^\"]+)\"?"), Qt::CaseInsensitive);
      if (regexp.indexIn(headerValue) >= 0)
      {
         defaultFileName = regexp.cap(1);
      }
   }

   QString fileName = QFileDialog::getSaveFileName(this,
                                                   tr("Download File"),
                                                   defaultFileName,
                                                   QString(),
                                                   nullptr,
                                                   standardFileDialogOptions());
   return fileName;
}

void WebView::keyPressEvent(QKeyEvent* pEvent)
{
#ifdef Q_OS_MAC
   if (pEvent->key() == Qt::Key_W &&
       pEvent->modifiers() == Qt::CTRL)
   {
      // on macOS, intercept Cmd+W and emit the window close signal
      onCloseWindowShortcut();
   }
   else
   {
      // pass other key events through to WebEngine
      QWebEngineView::keyPressEvent(pEvent);
   }
#else

   QWebEngineView::keyPressEvent(pEvent);
   
#endif
}

void WebView::openFile(QString fileName)
{
   // force use of Preview for PDFs on the Mac (Adobe Reader 10.01 crashes)
#ifdef Q_OS_MAC
   if (fileName.toLower().endsWith(QString::fromUtf8(".pdf")))
   {
      QStringList args;
      args.append(QString::fromUtf8("-a"));
      args.append(QString::fromUtf8("Preview"));
      args.append(fileName);
      QProcess::startDetached(QString::fromUtf8("open"), args);
      return;
   }
#endif

   QDesktopServices::openUrl(QUrl::fromLocalFile(fileName));
}

bool WebView::event(QEvent* event)
{
   if (event->type() == QEvent::ShortcutOverride)
   {
      // take a first crack at shortcuts
      keyPressEvent(static_cast<QKeyEvent*>(event));
      return true;
   }
   return this->QWebEngineView::event(event);
}

void WebView::closeEvent(QCloseEvent*)
{
}

namespace {

QString label(QString label)
{
#ifdef Q_OS_MAC
   
   static const QChar ampersand = QChar::fromLatin1('&');
   static const QString space = QStringLiteral(" ");
   
   QStringList words = label.split(space);
   for (QString& word : words)
   {
      int index = 0;
      if (word[index] == ampersand)
         index = 1;
      
      word[index] = word[index].toUpper();
   }
   
   return words.join(space);
   
#else
   
   return label;
   
#endif
}

} // end anonymous namespace

void WebView::contextMenuEvent(QContextMenuEvent* event)
{
   QMenu* menu = new QMenu(this);
   
   const auto& data = webPage()->contextMenuData();
   
   bool canNavigateHistory =
         webPage()->history()->canGoBack() ||
         webPage()->history()->canGoForward();
   
   if (data.selectedText().isEmpty() && canNavigateHistory)
   {
      auto* back    = menu->addAction(label(tr("&Back")),    [&]() { webPage()->history()->back(); });
      auto* forward = menu->addAction(label(tr("&Forward")), [&]() { webPage()->history()->forward(); });
      
      back->setEnabled(webPage()->history()->canGoBack());
      forward->setEnabled(webPage()->history()->canGoForward());
      
      menu->addSeparator();
   }
   
   if (data.mediaUrl().isValid())
   {
      switch (data.mediaType())
      {
      case QWebEngineContextMenuData::MediaTypeImage:
         
         menu->addAction(label(tr("Sa&ve image as...")),   [&]() { triggerPageAction(QWebEnginePage::DownloadImageToDisk); });
         menu->addAction(label(tr("Cop&y image")),         [&]() { triggerPageAction(QWebEnginePage::CopyImageToClipboard); });
         menu->addAction(label(tr("C&opy image address")), [&]() { triggerPageAction(QWebEnginePage::CopyImageUrlToClipboard); });
         break;
         
      case QWebEngineContextMenuData::MediaTypeAudio:
         
#if QT_VERSION >= QT_VERSION_CHECK(5, 11, 0)
         if (data.mediaFlags().testFlag(QWebEngineContextMenuData::MediaPaused))
            menu->addAction(label(tr("&Play")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });
         else
            menu->addAction(label(tr("&Pause")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });
#endif

         menu->addAction(label(tr("&Loop")),            [&]() { triggerPageAction(QWebEnginePage::ToggleMediaLoop); });
         menu->addAction(label(tr("Toggle &controls")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaControls); });
         menu->addSeparator();
         
         menu->addAction(label(tr("Sa&ve audio as...")),   [&]() { triggerPageAction(QWebEnginePage::DownloadMediaToDisk); });
         menu->addAction(label(tr("C&opy audio address")), [&]() { triggerPageAction(QWebEnginePage::CopyMediaUrlToClipboard); });
         break;
         
      case QWebEngineContextMenuData::MediaTypeVideo:
         
#if QT_VERSION >= QT_VERSION_CHECK(5, 11, 0)
         if (data.mediaFlags().testFlag(QWebEngineContextMenuData::MediaPaused))
            menu->addAction(label(tr("&Play")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });
         else
            menu->addAction(label(tr("&Pause")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });
#endif

         menu->addAction(label(tr("&Loop")),            [&]() { triggerPageAction(QWebEnginePage::ToggleMediaLoop); });
         menu->addAction(label(tr("Toggle &controls")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaControls); });
         menu->addSeparator();
         
         menu->addAction(label(tr("Sa&ve video as...")),   [&]() { triggerPageAction(QWebEnginePage::DownloadMediaToDisk); });
         menu->addAction(label(tr("C&opy video address")), [&]() { triggerPageAction(QWebEnginePage::CopyMediaUrlToClipboard); });
         break;
         
      case QWebEngineContextMenuData::MediaTypeFile:
         menu->addAction(label(tr("Sa&ve file as...")),   [&]() { triggerPageAction(QWebEnginePage::DownloadLinkToDisk); });
         menu->addAction(label(tr("C&opy link address")), [&]() { triggerPageAction(QWebEnginePage::CopyMediaUrlToClipboard); });
         break;
         
      default:
         break;
      }
   }
   else if (data.linkUrl().isValid())
   {
      menu->addAction(label(tr("Open link in &browser")), [&]() { desktop::openUrl(data.linkUrl()); });
      menu->addAction(label(tr("Save lin&k as...")),      [&]() { triggerPageAction(QWebEnginePage::DownloadLinkToDisk); });
      menu->addAction(label(tr("Copy link addr&ess")),    [&]() { triggerPageAction(QWebEnginePage::CopyLinkToClipboard); });
   }
   else
   {
      // always show cut / copy / paste, but only enable cut / copy if there
      // is some selected text, and only enable paste if there is something
      // on the clipboard. note that this isn't perfect -- the highlighted
      // text may not correspond to the context menu click target -- but
      // in general users who want to copy text will right-click on the
      // selection, rather than elsewhere on the screen.
      auto* cut       = webPage()->action(QWebEnginePage::Cut);
      auto* copy      = webPage()->action(QWebEnginePage::Copy);
      auto* paste     = webPage()->action(QWebEnginePage::Paste);
      auto* selectAll = webPage()->action(QWebEnginePage::SelectAll);
      
      cut->setText(label(tr("Cu&t")));
      copy->setText(label(tr("&Copy")));
      paste->setText(label(tr("&Paste")));
      selectAll->setText(label(tr("Select &all")));
      
      cut->setEnabled(data.isContentEditable() && !data.selectedText().isEmpty());
      copy->setEnabled(!data.selectedText().isEmpty());
      paste->setEnabled(QApplication::clipboard()->mimeData()->hasText());
      
      menu->addAction(cut);
      menu->addAction(copy);
      menu->addAction(paste);
      menu->addAction(selectAll);
   }
   
#if QT_VERSION >= QT_VERSION_CHECK(5, 11, 0)
   menu->addSeparator();
   menu->addAction(label(tr("&Reload")), [&]() { triggerPageAction(QWebEnginePage::Reload); });
   menu->addAction(label(tr("I&nspect element")), [&]() {
      
      QWebEnginePage* devToolsPage = webPage()->devToolsPage();
      if (devToolsPage == nullptr)
      {
         DevToolsWindow* devToolsWindow = new DevToolsWindow();
         devToolsPage = devToolsWindow->webPage();
         webPage()->setDevToolsPage(devToolsPage);
         
         s_devToolsWindows[webPage()] = devToolsWindow;
      }
      
      // make sure the devtools window is showing and focused
      DevToolsWindow* devToolsWindow = s_devToolsWindows[webPage()];
      
      devToolsWindow->show();
      devToolsWindow->raise();
      devToolsWindow->setFocus();
      
      // we have a window; invoke Inspect Element now
      webPage()->triggerAction(QWebEnginePage::InspectElement);
   });
#else
   
# ifndef NDEBUG
   
   menu->addAction(label(tr("&Reload")), [&]() { triggerPageAction(QWebEnginePage::Reload); });
   menu->addSeparator();
   menu->addAction(label(tr("I&nspect element")), [&]() { triggerPageAction(QWebEnginePage::InspectElement); });
   
# endif
   
#endif
   
   menu->setAttribute(Qt::WA_DeleteOnClose, true);
   
   menu->exec(event->globalPos());
}

} // namespace desktop
} // namespace rstudio
