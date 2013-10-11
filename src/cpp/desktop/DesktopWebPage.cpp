/*
 * DesktopWebPage.cpp
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

#include "DesktopWebPage.hpp"

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <QWidget>
#include <QtDebug>
#include "DesktopNetworkAccessManager.hpp"
#include "DesktopWindowTracker.hpp"
#include "DesktopSatelliteWindow.hpp"
#include "DesktopSecondaryWindow.hpp"
#include "DesktopMainWindow.hpp"

#include "DesktopUtils.hpp"

extern QString sharedSecret;

namespace desktop {

namespace {

WindowTracker s_windowTracker;

} // anonymous namespace

WebPage::WebPage(QUrl baseUrl, QWidget *parent) :
      QWebPage(parent),
      baseUrl_(baseUrl),
      navigated_(false)
{
   settings()->setAttribute(QWebSettings::DeveloperExtrasEnabled, true);
   settings()->setAttribute(QWebSettings::JavascriptCanOpenWindows, true);
   setNetworkAccessManager(new NetworkAccessManager(sharedSecret, parent));
   defaultSaveDir_ = QDir::home();
}

void WebPage::setBaseUrl(const QUrl& baseUrl)
{
   baseUrl_ = baseUrl;
}

void WebPage::activateSatelliteWindow(QString name)
{
   BrowserWindow* pSatellite = s_windowTracker.getWindow(name);
   if (pSatellite)
      desktop::raiseAndActivateWindow(pSatellite);
}

void WebPage::prepareForSatelliteWindow(
                              const PendingSatelliteWindow& pendingWnd)
{
   pendingSatelliteWindow_ = pendingWnd;
}

QWebPage* WebPage::createWindow(QWebPage::WebWindowType)
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
         return pSatellite->webView()->webPage();
      }
   }
   else
   {
      SecondaryWindow* pWindow = new SecondaryWindow(baseUrl_);
      pWindow->show();
      return pWindow->webView()->webPage();
   }
}


bool WebPage::shouldInterruptJavaScript()
{
   return false;
}

void WebPage::javaScriptConsoleMessage(const QString& message, int /*lineNumber*/, const QString& /*sourceID*/)
{
   qDebug() << message;
}

QString WebPage::userAgentForUrl(const QUrl &url) const
{
   return this->QWebPage::userAgentForUrl(url) + QString::fromAscii(" Qt/") + QString::fromAscii(qVersion());
}

bool WebPage::acceptNavigationRequest(QWebFrame* pWebFrame,
                                       const QNetworkRequest& request,
                                       NavigationType navType)
{
   QUrl url = request.url();

   if (url.toString() == QString::fromAscii("about:blank"))
      return true;

   if (url.scheme() != QString::fromAscii("http")
       && url.scheme() != QString::fromAscii("https")
       && url.scheme() != QString::fromAscii("mailto")
       && url.scheme() != QString::fromAscii("data"))
   {
      qDebug() << url.toString();
      return false;
   }

   // determine if this is a local request (handle internally only if local)
   std::string host = url.host().toStdString();
   bool isLocal = host == "localhost" || host == "127.0.0.1";

   if ((baseUrl_.isEmpty() && isLocal) ||
       (url.scheme() == baseUrl_.scheme()
        && url.host() == baseUrl_.host()
        && url.port() == baseUrl_.port()))
   {
      navigated_ = true;
      return true;
   }
   // allow local viewer urls to be handled internally by Qt
   else if (isLocal && !viewerUrl_.isEmpty() &&
            url.toString().startsWith(viewerUrl_))
   {
      navigated_ = true;
      return true;
   }
   else
   {
      if (url.scheme() == QString::fromUtf8("data") &&
          (navType == QWebPage::NavigationTypeLinkClicked ||
           navType == QWebPage::NavigationTypeFormSubmitted))
      {
         handleBase64Download(pWebFrame, url);
      }
      else
      {
         desktop::openUrl(url);
      }

      if (!navigated_)
         this->view()->window()->deleteLater();

      return false;
   }
}

void WebPage::handleBase64Download(QWebFrame* pWebFrame, QUrl url)
{
   // look for beginning of base64 data
   QString base64 = QString::fromUtf8("base64,");
   int pos = url.path().indexOf(base64);
   if (pos == -1)
   {
      LOG_ERROR_MESSAGE("Base64 designator not found in data url");
      return;
   }

   // extract the base64 data from the uri
   QString base64Data = url.path();
   base64Data.remove(0, pos + base64.length());
   QByteArray base64ByteArray(base64Data.toStdString().c_str());
   QByteArray byteArray = QByteArray::fromBase64(base64ByteArray);

   // find the a tag in the page with this href
   QWebElement aTag;
   QString urlString = url.toString(QUrl::None);
   QWebElementCollection aElements = pWebFrame->findAllElements(
                                             QString::fromUtf8("a"));
   for (int i=0; i<aElements.count(); i++)
   {
      QWebElement a = aElements.at(i);
      QString href = a.attribute(QString::fromUtf8("href"));
      href.replace(QChar::fromAscii('\n'), QString::fromUtf8(""));
      if (href == urlString)
      {
         aTag = a;
         break;
      }
   }

   // if no a tag was found then bail
   if (aTag.isNull())
   {
      LOG_ERROR_MESSAGE("Unable to finding matching a tag for data url");
      return;
   }

   // get the download attribute (default filename)
   QString download = aTag.attribute(QString::fromUtf8("download"));
   QString defaultFilename = defaultSaveDir_.absoluteFilePath(download);

   // prompt for filename
   QString filename = QFileDialog::getSaveFileName(
                                            NULL,
                                            tr("Download File"),
                                            defaultFilename,
                                            defaultSaveDir_.absolutePath(),
                                            0,
                                            standardFileDialogOptions());
   if (!filename.isEmpty())
   {
      // write the file
      QFile file(filename);
      if (file.open(QIODevice::WriteOnly))
      {
         if (file.write(byteArray) == -1)
         {
            showFileError(QString::fromUtf8("writing"),
                          filename,
                          file.errorString());
         }

         file.close();
      }
      else
      {
         showFileError(QString::fromUtf8("writing"),
                       filename,
                       file.errorString());
      }

      // persist the defaultSaveDir
      defaultSaveDir_ = QFileInfo(file).dir();
   }
}

}
