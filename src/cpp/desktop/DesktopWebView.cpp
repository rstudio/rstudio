/*
 * DesktopWebView.cpp
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

#include "DesktopWebView.hpp"
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QTemporaryFile>
#include <QStyleFactory>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include "DesktopDownloadHelper.hpp"
#include "DesktopOptions.hpp"
#include "DesktopWebPage.hpp"
#include "DesktopUtils.hpp"

#ifdef _WIN32
#include <windows.h>
#include <wingdi.h>
#endif

namespace rstudio {
namespace desktop {

WebView::WebView(QUrl baseUrl, QWidget *parent, bool allowExternalNavigate) :
    QWebView(parent),
    baseUrl_(baseUrl),
    pWebInspector_(NULL),
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

   // QWebView can create its own QWebInspector instance, but it doesn't always
   // destroy it correctly if the inspector is open when the associated browser
   // window is closed (see case 3889), leading to a crash. To work around this,
   // we create our own unbound web inspector, and clean it up manually when the
   // WebView closes.
   pWebInspector_ = new QWebInspector();
   pWebInspector_->setVisible(false);
   pWebInspector_->setPage(pWebPage_);

   page()->setForwardUnsupportedContent(true);

   connect(page(), SIGNAL(downloadRequested(QNetworkRequest)),
           this, SLOT(downloadRequested(QNetworkRequest)));
   connect(page(), SIGNAL(unsupportedContent(QNetworkReply*)),
           this, SLOT(unsupportedContent(QNetworkReply*)));
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
                                   QNetworkReply* pReply = NULL)
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
                                                   0,
                                                   standardFileDialogOptions());
   return fileName;
}

void WebView::keyPressEvent(QKeyEvent* pEv)
{
   // emit close window shortcut signal if appropriate
#ifndef _WIN32
   if (pEv->key() == 'W')
   {
      // check modifier and emit signal
      if (pEv->modifiers() & Qt::ControlModifier)
         onCloseWindowShortcut();
   }
#endif

  // flip control and meta on the mac
#ifdef Q_OS_MAC
   Qt::KeyboardModifiers modifiers = pEv->modifiers();
   if (modifiers & Qt::MetaModifier && !(modifiers & Qt::ControlModifier))
   {
      modifiers &= ~Qt::MetaModifier;
      modifiers |= Qt::ControlModifier;
   }
   else if (modifiers & Qt::ControlModifier && !(modifiers & Qt::MetaModifier))
   {
      modifiers &= ~Qt::ControlModifier;
      modifiers |= Qt::MetaModifier;
   }
#endif

   // Work around bugs in QtWebKit that result in numpad key
   // presses resulting in keyCode=0 in the DOM's keydown events.
   // This is due to some missing switch cases in the case
   // where the keypad modifier bit is on, so we turn it off.
   QKeyEvent newEv(pEv->type(),    
                   pEv->key(),
                   pEv->modifiers() & ~Qt::KeypadModifier,
                   pEv->text(),
                   pEv->isAutoRepeat(),
                   pEv->count());
  
   // delegate to base
   this->QWebView::keyPressEvent(&newEv);
}

void WebView::downloadRequested(const QNetworkRequest& request)
{
   QString fileName = promptForFilename(request);
   if (fileName.isEmpty())
      return;

   // Ask the network manager to download
   // the file and connect to the progress
   // and finished signals.
   QNetworkRequest newRequest = request;

   QNetworkAccessManager* pNetworkManager = page()->networkAccessManager();
   QNetworkReply* pReply = pNetworkManager->get(newRequest);
   // DownloadHelper frees itself when downloading is done
   new DownloadHelper(pReply, fileName);
}

void WebView::unsupportedContent(QNetworkReply* pReply)
{
   bool closeAfterDownload = false;
   if (this->page()->history()->count() == 0)
   {
      /* This is for the case where a new browser window was launched just
         to show a PDF or save a file. Otherwise we would have an empty
         browser window with no history hanging around. */
      window()->hide();
      closeAfterDownload = true;
   }

   DownloadHelper* pDownloadHelper = NULL;

   QString contentType =
         pReply->header(QNetworkRequest::ContentTypeHeader).toString();
   if (contentType.contains(QRegExp(QString::fromUtf8("^\\s*application/pdf($|;)"),
                                    Qt::CaseInsensitive)))
   {
      core::FilePath dir(options().scratchTempDir());

      QTemporaryFile pdfFile(QString::fromUtf8(
            dir.childPath("rstudio-XXXXXX.pdf").absolutePath().c_str()));
      pdfFile.setAutoRemove(false);
      pdfFile.open();
      pdfFile.close();

      if (pReply->isFinished())
      {
         DownloadHelper::handleDownload(pReply, pdfFile.fileName());
         openFile(pdfFile.fileName());
      }
      else
      {
         // DownloadHelper frees itself when downloading is done
         pDownloadHelper = new DownloadHelper(pReply, pdfFile.fileName());
         connect(pDownloadHelper, SIGNAL(downloadFinished(QString)),
                 this, SLOT(openFile(QString)));
      }
   }
   else
   {
      QString fileName = promptForFilename(pReply->request(), pReply);
      if (fileName.isEmpty())
      {
         pReply->abort();
         if (closeAfterDownload)
            window()->close();
      }
      else
      {
         // DownloadHelper frees itself when downloading is done
         if (pReply->isFinished())
         {
            DownloadHelper::handleDownload(pReply, fileName);
         }
         else
         {
            pDownloadHelper = new DownloadHelper(pReply, fileName);
         }
      }
   }

   if (closeAfterDownload && pDownloadHelper)
   {
      connect(pDownloadHelper, SIGNAL(downloadFinished(QString)),
              window(), SLOT(close()));
   }
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

// QWebView doesn't respect the system DPI and always renders as though
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

void WebView::closeEvent(QCloseEvent*)
{
   // When the webview closes, preemptively destroy the associated web
   // inspector, if we have one.
   if (pWebInspector_ != NULL)
   {
      pWebInspector_->setVisible(false);
      pWebInspector_->disconnect();
      pWebInspector_->deleteLater();
      pWebInspector_ = NULL;
   }
}

} // namespace desktop
} // namespace rstudio
