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

#include <core/system/Environment.hpp>

#ifdef _WIN32
#include <windows.h>
#include <wingdi.h>
#endif

namespace rstudio {
namespace desktop {

WebView::WebView(QUrl baseUrl, QWidget *parent, bool allowExternalNavigate) :
    QWebEngineView(parent),
    baseUrl_(baseUrl),
    dpiZoomScaling_(getDpiZoomScaling())
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

// QWebEngineView doesn't respect the system DPI and always renders as though
// it were at 96dpi. To work around this, we take the user-specified zoom level
// and scale it by a DPI-determined constant before applying it to the view.
// See: https://bugreports.qt-project.org/browse/QTBUG-29571
void WebView::setDpiAwareZoomFactor(qreal factor)
{
   setZoomFactor(factor * dpiZoomScaling_);
}

qreal WebView::dpiAwareZoomFactor()
{
   return zoomFactor() / dpiZoomScaling_;
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

void WebView::contextMenuEvent(QContextMenuEvent* event)
{
   QMenu* menu = new QMenu(this);
   const auto& data = webPage()->contextMenuData();
 
   if (data.mediaUrl().isValid())
   {
      switch (data.mediaType())
      {
      case QWebEngineContextMenuData::MediaTypeImage:
         menu->addAction(webPage()->action(QWebEnginePage::DownloadImageToDisk));
         menu->addAction(webPage()->action(QWebEnginePage::CopyImageUrlToClipboard));
         menu->addAction(webPage()->action(QWebEnginePage::CopyImageToClipboard));
         break;
      case QWebEngineContextMenuData::MediaTypeAudio:
      case QWebEngineContextMenuData::MediaTypeVideo:
         menu->addAction(webPage()->action(QWebEnginePage::DownloadMediaToDisk));
         menu->addAction(webPage()->action(QWebEnginePage::CopyMediaUrlToClipboard));
         menu->addAction(webPage()->action(QWebEnginePage::ToggleMediaPlayPause));
         menu->addAction(webPage()->action(QWebEnginePage::ToggleMediaLoop));
         break;
      case QWebEngineContextMenuData::MediaTypeFile:
         menu->addAction(webPage()->action(QWebEnginePage::DownloadLinkToDisk));
         menu->addAction(webPage()->action(QWebEnginePage::CopyMediaUrlToClipboard));
         break;
      default:
         break;
      }
   }
   else
   {
      // always show cut / copy / paste, but only enable cut / copy if there
      // is some selected text, and only enable paste if there is something
      // on the clipboard. note that this isn't perfect -- the highlighted
      // text may not correspond to the context menu click target -- but
      // in general users who want to copy text will right-click on the
      // selection, rather than elsewhere on the screen.
      auto* cut   = webPage()->action(QWebEnginePage::Cut);
      auto* copy  = webPage()->action(QWebEnginePage::Copy);
      auto* paste = webPage()->action(QWebEnginePage::Paste);
      
      cut->setEnabled(data.isContentEditable() && !data.selectedText().isEmpty());
      copy->setEnabled(!data.selectedText().isEmpty());
      paste->setEnabled(QApplication::clipboard()->mimeData()->hasText());
      
      menu->addAction(cut);
      menu->addAction(copy);
      menu->addAction(paste);
      menu->addSeparator();
      menu->addAction(webPage()->action(QWebEnginePage::SelectAll));
   }
   
#ifndef NDEBUG
   menu->addSeparator();
   menu->addAction(tr("&Reload"), [&]() { triggerPageAction(QWebEnginePage::Reload); });
   menu->addAction(webPage()->action(QWebEnginePage::InspectElement));
#endif
   
   menu->setAttribute(Qt::WA_DeleteOnClose, true);
   
   menu->exec(event->globalPos());
}

} // namespace desktop
} // namespace rstudio
