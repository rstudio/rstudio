/*
 * DesktopWebView.cpp
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

#include "DesktopWebView.hpp"
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QTemporaryFile>
#include <core/system/System.hpp>
#include "DesktopSecondaryWindow.hpp"
#include "DesktopDownloadHelper.hpp"
#include "DesktopOptions.hpp"
#include "DesktopWebPage.hpp"

namespace desktop {

WebView::WebView(QUrl baseUrl, QWidget *parent) :
    QWebView(parent),
    baseUrl_(baseUrl)
{
#ifdef Q_WS_X11
   if (!core::system::getenv("KDE_FULL_SESSION").empty())
      setStyle(new QPlastiqueStyle());
#endif
   setPage(new WebPage(baseUrl, this));

   page()->setForwardUnsupportedContent(true);
   //page()->settings()->setAttribute(QWebSettings::DeveloperExtrasEnabled, true);

   connect(page(), SIGNAL(downloadRequested(QNetworkRequest)),
           this, SLOT(downloadRequested(QNetworkRequest)));
   connect(page(), SIGNAL(unsupportedContent(QNetworkReply*)),
           this, SLOT(unsupportedContent(QNetworkReply*)));
}

QWebView* WebView::createWindow(QWebPage::WebWindowType)
{
   SecondaryWindow* pWindow = new SecondaryWindow(baseUrl_);
   pWindow->show();
   return pWindow->webView();
}

QString WebView::promptForFilename(const QNetworkRequest& request,
                                   QNetworkReply* pReply = NULL)
{
   QString defaultFileName = QFileInfo(request.url().path()).fileName();

   // Content-Disposition's filename parameter should be used as the
   // default, if present.
   if (pReply && pReply->hasRawHeader("content-disposition"))
   {
      QString headerValue = pReply->rawHeader("content-disposition");
      QRegExp regexp("filename=(.+)", Qt::CaseInsensitive);
      if (regexp.indexIn(headerValue) >= 0)
      {
         defaultFileName = regexp.cap(1);
      }
   }

   QString fileName = QFileDialog::getSaveFileName(this,
                                                   tr("Download File"),
                                                   defaultFileName);
   return fileName;
}

void WebView::keyPressEvent(QKeyEvent* pEv)
{
   // Work around bug in Qt 4.6 and earlier where arrow keys
   // on Mac result in keyCode=0 in the DOM's keydown events.
   // This is due to some missing switch cases in the case
   // where the keypad modifier bit is on, so we turn it off.

   Qt::KeyboardModifiers modifiers(pEv->modifiers());
   switch (pEv->key())
   {
   case Qt::Key_Up:
   case Qt::Key_Down:
   case Qt::Key_Left:
   case Qt::Key_Right:
      modifiers &= ~Qt::KeypadModifier;
   }

   QKeyEvent newEv(pEv->type(),
                   pEv->key(),
                   modifiers,
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
   if (contentType.contains(QRegExp("^\\s*application/pdf($|;)",
                                    Qt::CaseInsensitive)))
   {
      core::FilePath dir(options().scratchTempDir());

      QTemporaryFile pdfFile(QString::fromStdString(
            dir.childPath("rstudio-XXXXXX.pdf").absolutePath()));
      pdfFile.open();
      pdfFile.close();
      // DownloadHelper frees itself when downloading is done
      pDownloadHelper = new DownloadHelper(pReply, pdfFile.fileName());
      connect(pDownloadHelper, SIGNAL(downloadFinished(QString)),
              this, SLOT(openFile(QString)));
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
   QDesktopServices::openUrl(QUrl::fromLocalFile(fileName));
}

} // namespace desktop
