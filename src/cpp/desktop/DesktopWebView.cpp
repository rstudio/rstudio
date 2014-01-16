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

namespace desktop {

WebView::WebView(QUrl baseUrl, QWidget *parent) :
    QWebView(parent),
    baseUrl_(baseUrl),
    dpiZoomScaling_(1.0)
{
#ifdef Q_WS_X11
   if (!core::system::getenv("KDE_FULL_SESSION").empty())
      setStyle(new QPlastiqueStyle());
#endif
   pWebPage_ = new WebPage(baseUrl, this);
   setPage(pWebPage_);

   page()->setForwardUnsupportedContent(true);
   if (desktop::options().webkitDevTools())
      page()->settings()->setAttribute(QWebSettings::DeveloperExtrasEnabled, true);

   connect(page(), SIGNAL(downloadRequested(QNetworkRequest)),
           this, SLOT(downloadRequested(QNetworkRequest)));
   connect(page(), SIGNAL(unsupportedContent(QNetworkReply*)),
           this, SLOT(unsupportedContent(QNetworkReply*)));

#ifdef _WIN32
   // On Windows, check for high DPI; if present, scale the zoom factors
   // accordingly.
   HDC defaultDC = GetDC(NULL);
   int dpi = GetDeviceCaps(defaultDC, LOGPIXELSX);
   if (dpi >= 192)
   {
      // Corresponds to 200% scaling (introduced in Windows 8.1)
      dpiZoomScaling_ = 1.5;
   }
   else if (dpi >= 144)
   {
      // Corresponds to 150% scaling
      dpiZoomScaling_ = 1.2;
   }
   ReleaseDC(NULL, defaultDC);
#endif
}

void WebView::setBaseUrl(const QUrl& baseUrl)
{
   baseUrl_ = baseUrl;
   pWebPage_->setBaseUrl(baseUrl_);
}

void WebView::activateSatelliteWindow(QString name)
{
   pWebPage_->activateSatelliteWindow(name);
}

void WebView::prepareForSatelliteWindow(
                              const PendingSatelliteWindow& pendingWnd)
{
   pWebPage_->prepareForSatelliteWindow(pendingWnd);
}

QString WebView::promptForFilename(const QNetworkRequest& request,
                                   QNetworkReply* pReply = NULL)
{
   QString defaultFileName = QFileInfo(request.url().path()).fileName();

   // Content-Disposition's filename parameter should be used as the
   // default, if present.
   if (pReply && pReply->hasRawHeader("content-disposition"))
   {
      QString headerValue = QString::fromAscii(pReply->rawHeader("content-disposition"));
      QRegExp regexp(QString::fromAscii("filename=(.+)"), Qt::CaseInsensitive);
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
#ifdef Q_WS_MAC
      Qt::KeyboardModifier modifier = Qt::MetaModifier;
#else
      Qt::KeyboardModifier modifier = Qt::ControlModifier;
#endif

      // check modifier and emit signal
      if (pEv->modifiers() & modifier)
         onCloseWindowShortcut();
   }
#endif

   // Work around bugs in QtWebKit that result in numpad key
   // presses resulting in keyCode=0 in the DOM's keydown events.
   // This is due to some missing switch cases in the case
   // where the keypad modifier bit is on, so we turn it off.
  
   Qt::KeyboardModifiers modifiers;
  
#ifdef Q_WS_MAC
   if ((pEv->nativeModifiers() & 0x40101) == 0x40101) {
      modifiers &= ~Qt::MetaModifier;
      modifiers |= Qt::ControlModifier;
   } else if ((pEv->nativeModifiers() & 0x100108) == 0x100108) {
      modifiers &= ~Qt::ControlModifier;
      modifiers |= Qt::MetaModifier;
   } else {
#else
   {
#endif     
     modifiers = pEv->modifiers();
   }

   QKeyEvent newEv(pEv->type(),    
                   pEv->key(),
                   modifiers & ~Qt::KeypadModifier,
                   pEv->text(),
                   pEv->isAutoRepeat(),
                   pEv->count());
  
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
   if (contentType.contains(QRegExp(QString::fromAscii("^\\s*application/pdf($|;)"),
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
         pDownloadHelper = new DownloadHelper(pReply, fileName);
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
#ifdef Q_WS_MAC
   if (fileName.toLower().endsWith(QString::fromAscii(".pdf")))
   {
      QStringList args;
      args.append(QString::fromAscii("-a"));
      args.append(QString::fromAscii("Preview"));
      args.append(fileName);
      QProcess::startDetached(QString::fromAscii("open"), args);
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
   zoomFactor() / dpiZoomScaling_;
}

} // namespace desktop
