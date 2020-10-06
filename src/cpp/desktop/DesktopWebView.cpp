/*
 * DesktopWebView.cpp
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


class MouseNavigateSourceEventFilter : public QObject
{
public:
   explicit MouseNavigateSourceEventFilter(WebView* pParent)
      : QObject(pParent)
   {
   }

protected:
   // Handler for mouse back/forward button for source history navigation.
   // This is needed for Desktop because QtWebEngine doesn't receive these button clicks. In the
   // web browser/Server scenario this is handled by Source.java::handleMouseButtonNavigations().
   bool eventFilter(QObject* pObject, QEvent* pEvent) override
   {
      if (pEvent->type() == QEvent::MouseButtonPress)
      {
         QMouseEvent* pMouseEvent = static_cast<QMouseEvent*>(pEvent);
         if (pMouseEvent->button() == Qt::ForwardButton ||
             pMouseEvent->button() == Qt::BackButton)
         {
            WebView* pWebView = qobject_cast<WebView*>(parent());
            if (pWebView)
            {
               pWebView->mouseNavigateButtonClick(pMouseEvent);
               return true;
            }
         }
      }

      return QObject::eventFilter(pObject, pEvent);
   }
};


} // end anonymous namespace

WebView::WebView(QUrl baseUrl, QWidget *parent, bool allowExternalNavigate) :
    QWebEngineView(parent),
    baseUrl_(baseUrl)
{

   pWebPage_ = new WebPage(baseUrl, this, allowExternalNavigate);
   init();
}

WebView::WebView(QWebEngineProfile *profile,
                 QUrl baseUrl,
                 QWidget *parent,
                 bool allowExternalNavigate) :
   QWebEngineView(parent),
   baseUrl_(baseUrl)
{
   pWebPage_ = new WebPage(profile, baseUrl, this, allowExternalNavigate);
   init();
}

void WebView::init()
{
#ifdef Q_OS_LINUX
   if (!core::system::getenv("KDE_FULL_SESSION").empty())
   {
      QString fusion = QString::fromUtf8("fusion");
      if (QStyleFactory::keys().contains(fusion))
         setStyle(QStyleFactory::create(fusion));
   }
#endif

   setPage(pWebPage_);
   setAcceptDrops(true);
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
   // on macOS, intercept Cmd+W and emit the window close signal
   if (pEvent->key() == Qt::Key_W && pEvent->modifiers() == Qt::CTRL)
   {
      onCloseWindowShortcut();
      return;
   }
#endif
 
   // use default behavior otherwise
   QWebEngineView::keyPressEvent(pEvent);
   
}

void WebView::dragEnterEvent(QDragEnterEvent *pEvent)
{
   // notify GWT context of drag start
   QString command = QString::fromUtf8(
     "if (window.desktopHooks) "
     "  window.desktopHooks.onDragStart();");
   webPage()->runJavaScript(command);

   // delegate to default
   QWebEngineView::dragEnterEvent(pEvent);
}

void WebView::dropEvent(QDropEvent *pEvent)
{
   // notify GWT context of dropped urls (as in the JS layer
   // the dataTransfer object comes up empty)
   if (pEvent->mimeData()->hasUrls())
   {
      // build buffer of urls
      QString urlsBuffer;
      auto urls = pEvent->mimeData()->urls();
      for (auto url : urls)
      {
         // append (converting file-based urls)
         if (url.scheme() == QString::fromUtf8("file"))
            urlsBuffer.append(createAliasedPath(url.toLocalFile()));
         else
            urlsBuffer.append(url.toString());

         // append unique separator
         const char * const kUrlSeparator = "26D63FFA-995F-4E9A-B4AA-04DA9F93B538";
         urlsBuffer.append(QString::fromUtf8(kUrlSeparator));
      }

      // notify desktop of dropped urls
      QString command = QStringLiteral(
        "if (window.desktopHooks) "
        "  window.desktopHooks.onUrlsDropped(\"%1\");")
           .arg(urlsBuffer);
      webPage()->runJavaScript(command);
   }

   // delegate to default (will end up in standard drag/drop
   // handling but w/ empty dataTransfer)
   QWebEngineView::dropEvent(pEvent);
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
   onClose();
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
         
         if (data.mediaFlags().testFlag(QWebEngineContextMenuData::MediaPaused))
            menu->addAction(label(tr("&Play")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });
         else
            menu->addAction(label(tr("&Pause")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });

         menu->addAction(label(tr("&Loop")),            [&]() { triggerPageAction(QWebEnginePage::ToggleMediaLoop); });
         menu->addAction(label(tr("Toggle &controls")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaControls); });
         menu->addSeparator();
         
         menu->addAction(label(tr("Sa&ve audio as...")),   [&]() { triggerPageAction(QWebEnginePage::DownloadMediaToDisk); });
         menu->addAction(label(tr("C&opy audio address")), [&]() { triggerPageAction(QWebEnginePage::CopyMediaUrlToClipboard); });
         break;
         
      case QWebEngineContextMenuData::MediaTypeVideo:
         
         if (data.mediaFlags().testFlag(QWebEngineContextMenuData::MediaPaused))
            menu->addAction(label(tr("&Play")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });
         else
            menu->addAction(label(tr("&Pause")), [&]() { triggerPageAction(QWebEnginePage::ToggleMediaPlayPause); });

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

   menu->setAttribute(Qt::WA_DeleteOnClose, true);
   
   menu->exec(event->globalPos());
}

void WebView::childEvent(QChildEvent *event)
{
   if (event->added() && event->child()->inherits("QWidget"))
   {
      event->child()->installEventFilter(new MouseNavigateSourceEventFilter(this));
   }
}

void WebView::mouseNavigateButtonClick(QMouseEvent* pMouseEvent)
{
   QString command =  QStringLiteral(
      "if (window.desktopHooks) "
      "  window.desktopHooks.mouseNavigateButtonClick(%1, %2, %3);")
         .arg(pMouseEvent->button() == Qt::ForwardButton ? QStringLiteral("true") : QStringLiteral("false"))
         .arg(pMouseEvent->x())
         .arg(pMouseEvent->y());

   webPage()->runJavaScript(command);
}


} // namespace desktop
} // namespace rstudio
