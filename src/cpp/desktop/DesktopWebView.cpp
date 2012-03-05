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
#include <core/system/Environment.hpp>
#include "DesktopSecondaryWindow.hpp"
#include "DesktopSatelliteWindow.hpp"
#include "DesktopDownloadHelper.hpp"
#include "DesktopOptions.hpp"
#include "DesktopWebPage.hpp"
#include "DesktopWindowTracker.hpp"
#include "DesktopUtils.hpp"
#include "DesktopMainWindow.hpp"

namespace desktop {

namespace {

WindowTracker s_windowTracker;

} // anonymous namespace

WebView::WebView(QUrl baseUrl, QWidget *parent) :
    QWebView(parent),
    baseUrl_(baseUrl)
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

   // Use a timer to reduce the granularity of mouse events on Mac
   pMouseWheelTimer_ = new QTimer(this);
   pMouseWheelTimer_->setInterval(20);
   connect(pMouseWheelTimer_, SIGNAL(timeout()),
           this, SLOT(mouseWheelTimerFired()));
}

void WebView::setBaseUrl(const QUrl& baseUrl)
{
   baseUrl_ = baseUrl;
   pWebPage_->setBaseUrl(baseUrl_);
}


void WebView::activateSatelliteWindow(QString name)
{
   BrowserWindow* pSatellite = s_windowTracker.getWindow(name);
   if (pSatellite)
      desktop::raiseAndActivateWindow(pSatellite);
}

void WebView::prepareForSatelliteWindow(
                              const PendingSatelliteWindow& pendingWnd)
{
   pendingSatelliteWindow_ = pendingWnd;
}



QWebView* WebView::createWindow(QWebPage::WebWindowType)
{
   // check if this is a satellite window
   if (!pendingSatelliteWindow_.isEmpty())
   {
      // capture pending window params then clear them (one time only)
      QString name = pendingSatelliteWindow_.name;
      MainWindow* pMainWindow = pendingSatelliteWindow_.pMainWindow;
      int width = pendingSatelliteWindow_.width;
      int height = pendingSatelliteWindow_.height;
      pendingSatelliteWindow_ = PendingSatelliteWindow();

      // check for an existing window of this name
      BrowserWindow* pSatellite = s_windowTracker.getWindow(name);
      if (pSatellite)
      {
         // activate the browser then return NULL to indicate
         // we didn't create a new WebView
         desktop::raiseAndActivateWindow(pSatellite);
         return NULL;
      }
      // create a new window if we didn't find one
      else
      {
         // create and size
         pSatellite = new SatelliteWindow(pMainWindow);
         pSatellite->resize(width, height);

         // try to tile the window (but leave pdf window alone
         // since it is so large)
         if (name != QString::fromAscii("pdf"))
         {
            // calculate location to move to

            // y always attempts to be 25 pixels above then faults back
            // to 25 pixels below if that would be offscreen
            const int OVERLAP = 25;
            int moveY = pMainWindow->y() - OVERLAP;
            if (moveY < 0)
               moveY = pMainWindow->y() + OVERLAP;

            // x is based on centering over main window
            int moveX = pMainWindow->x() +
                        (pMainWindow->width() / 2) -
                        (width / 2);

            // perform movve
            pSatellite->move(moveX, moveY);
         }

         // add to tracker
         s_windowTracker.addWindow(name, pSatellite);

         // show and return the browser
         pSatellite->show();
         return pSatellite->webView();
      }
   }
   else
   {
      SecondaryWindow* pWindow = new SecondaryWindow(baseUrl_);
      pWindow->show();
      return pWindow->webView();
   }
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
                                                   defaultFileName);
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
   if ((pEv->nativeModifiers() & 0x40101) == 0x40101){
     modifiers = pEv->modifiers() & ~Qt::MetaModifier | Qt::ControlModifier;
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

void WebView::wheelEvent (QWheelEvent* event)
{
#ifdef Q_WS_MAC
   if (event->orientation() != Qt::Vertical)
   {
      this->QWebView::wheelEvent(event);
      return;
   }

   this->mouseWheelEvents_.push_back(*event);
   if (!pMouseWheelTimer_->isActive())
   {
      pMouseWheelTimer_->start();
   }
#else
   this->QWebView::wheelEvent(event);
#endif
}

void WebView::mouseWheelTimerFired()
{
   pMouseWheelTimer_->stop();

   if (mouseWheelEvents_.empty())
      return;

   int totalDelta = 0;
   for (int i = 0; i < mouseWheelEvents_.length(); i++)
   {
      totalDelta += mouseWheelEvents_.at(i).delta();
   }
   QWheelEvent event = mouseWheelEvents_.last();
   mouseWheelEvents_.clear();

   while (totalDelta != 0)
   {
      // If the absolute delta value exceeds 5000, Ace editor
      // goes into a super fine granularity mode that we need
      // to avoid
      const int MAX_ABS_DELTA_VALUE = 5000;

      int thisDelta = totalDelta;
      if (thisDelta > MAX_ABS_DELTA_VALUE)
      {
         thisDelta = MAX_ABS_DELTA_VALUE;
      }
      else if (thisDelta < -MAX_ABS_DELTA_VALUE)
      {
         thisDelta = -MAX_ABS_DELTA_VALUE;
      }
      totalDelta -= thisDelta;

      QWheelEvent totalEvent(event.pos(),
                             event.globalPos(),
                             thisDelta,
                             event.buttons(),
                             event.modifiers(),
                             event.orientation());
      this->QWebView::wheelEvent(&totalEvent);
   }
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

} // namespace desktop
