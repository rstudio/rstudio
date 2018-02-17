/*
 * DesktopWebPage.cpp
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

#include "DesktopWebPage.hpp"

#include <boost/algorithm/string.hpp>

#include <core/Thread.hpp>

#include <QWebEngineSettings>

#include "DesktopWindowTracker.hpp"
#include "DesktopSatelliteWindow.hpp"
#include "DesktopSecondaryWindow.hpp"
#include "DesktopMainWindow.hpp"
#include "DesktopWebProfile.hpp"

using namespace rstudio::core;

extern QString sharedSecret;

namespace rstudio {
namespace desktop {

namespace {

WindowTracker s_windowTracker;
std::mutex s_mutex;

} // anonymous namespace

WebPage::WebPage(QUrl baseUrl, QWidget *parent, bool allowExternalNavigate) :
      QWebEnginePage(new WebProfile, parent),
      baseUrl_(baseUrl),
      navigated_(false),
      allowExternalNav_(allowExternalNavigate)
{
   settings()->setAttribute(QWebEngineSettings::FullScreenSupportEnabled, true);
   settings()->setAttribute(QWebEngineSettings::JavascriptCanOpenWindows, true);
   settings()->setAttribute(QWebEngineSettings::JavascriptCanAccessClipboard, true);
   settings()->setAttribute(QWebEngineSettings::LocalStorageEnabled, true);
   
#ifdef Q_OS_MAC
   // disable scrollbars entirely for now as otherwise they persist indefinitely
   // and look terribly ugly (pending a better fix / workaround)
   settings()->setAttribute(QWebEngineSettings::ShowScrollBars, false);
#endif

   defaultSaveDir_ = QDir::home();
   connect(this, SIGNAL(windowCloseRequested()), SLOT(closeRequested()));
}

void WebPage::setBaseUrl(const QUrl& baseUrl)
{
   baseUrl_ = baseUrl;
}

void WebPage::activateWindow(QString name)
{
   BrowserWindow* pWindow = s_windowTracker.getWindow(name);
   if (pWindow)
      desktop::raiseAndActivateWindow(pWindow);
}

void WebPage::closeWindow(QString name)
{
   BrowserWindow* pWindow = s_windowTracker.getWindow(name);
   if (pWindow)
      desktop::closeWindow(pWindow);
}

void WebPage::prepareForWindow(const PendingWindow& pendingWindow)
{
   std::lock_guard<std::mutex> lock(s_mutex);
   
   pendingWindows_.push(pendingWindow);
}

QWebEnginePage* WebPage::createWindow(QWebEnginePage::WebWindowType type)
{
   std::lock_guard<std::mutex> lock(s_mutex);

   QString name;
   bool isSatellite = false;
   bool showToolbar = true;
   bool allowExternalNavigate = false;
   int width = 0;
   int height = 0;
   int x = -1;
   int y = -1;
   MainWindow* pMainWindow = nullptr;
   BrowserWindow* pWindow = nullptr;
   bool show = true;

   // check if this is target="_blank";
   if (type == QWebEnginePage::WebWindowType::WebBrowserTab)
   {
      // QtWebEngine behavior is to open a new browser window and send it the 
      // acceptNavigationRequest we use to redirect to system browser; don't want
      // that to be visible
      show = false;
      name = tr("_blank_redirector");

      // check for an existing hidden window
      pWindow = s_windowTracker.getWindow(name);
      if (pWindow)
      {
         return pWindow->webView()->webPage();
      }
   }

   // check if we have a satellite window waiting to come up
   if (!pendingWindows_.empty())
   {
      // retrieve the window
      PendingWindow pendingWindow = pendingWindows_.front();
      pendingWindows_.pop();

      // capture pending window params then clear them (one time only)
      name = pendingWindow.name;
      isSatellite = pendingWindow.isSatellite;
      showToolbar = pendingWindow.showToolbar;
      pMainWindow = pendingWindow.pMainWindow;
      allowExternalNavigate = pendingWindow.allowExternalNavigate;

      // get width and height, and adjust for high DPI
      double dpiZoomScaling = getDpiZoomScaling();
      width = pendingWindow.width * dpiZoomScaling;
      height = pendingWindow.height * dpiZoomScaling;
      x = pendingWindow.x;
      y = pendingWindow.y;

      // check for an existing window of this name
      pWindow = s_windowTracker.getWindow(name);
      if (pWindow)
      {
         // activate the browser then return NULL to indicate
         // we didn't create a new WebView
         desktop::raiseAndActivateWindow(pWindow);
         return nullptr;
      }
   }

   if (isSatellite)
   {
      // create and size
      pWindow = new SatelliteWindow(pMainWindow, name);
      pWindow->resize(width, height);

      if (x >= 0 && y >= 0)
      {
         // if the window specified its location, use it
         pWindow->move(x, y);
      }
      else if (name != QString::fromUtf8("pdf"))
      {
         // window location was left for us to determine; try to tile the window
         // (but leave pdf window alone since it is so large)

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

         // perform move
         pWindow->move(moveX, moveY);
      }
   }
   else
   {
      pWindow = new SecondaryWindow(showToolbar, name, baseUrl_, nullptr, this,
                                    allowExternalNavigate);
   }

   // if we have a name set, start tracking this window
   if (!name.isEmpty())
   {
      s_windowTracker.addWindow(name, pWindow);
   }

   // show and return the browser
   if (show)
      pWindow->show();
   return pWindow->webView()->webPage();
}

void WebPage::closeRequested()
{
   // invoked when close is requested via script (i.e. window.close()); honor
   // this request by closing the window in which the view is hosted
   view()->window()->close();
}

bool WebPage::shouldInterruptJavaScript()
{
   return false;
}

void WebPage::javaScriptConsoleMessage(JavaScriptConsoleMessageLevel /*level*/, const QString& message,
                                       int /*lineNumber*/, const QString& /*sourceID*/)
{
   qDebug() << message;
}

bool WebPage::acceptNavigationRequest(const QUrl &url, NavigationType navType, bool isMainFrame)
{
   if (url.toString() == QString::fromUtf8("about:blank"))
      return true;

   if (url.scheme() != QString::fromUtf8("http")
       && url.scheme() != QString::fromUtf8("https")
       && url.scheme() != QString::fromUtf8("mailto")
       && url.scheme() != QString::fromUtf8("data"))
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
   // allow shiny dialog urls to be handled internally by Qt
   else if (isLocal && !shinyDialogUrl_.isEmpty() &&
            url.toString().startsWith(shinyDialogUrl_))
   {
      navigated_ = true;
      return true;
   }
   else
   {
      if (url.scheme() == QString::fromUtf8("data") &&
          (navType == QWebEnginePage::NavigationTypeLinkClicked ||
           navType == QWebEnginePage::NavigationTypeFormSubmitted))
      {
         handleBase64Download(url);
      }
      else if (allowExternalNav_)
      {
         // if allowing external navigation, follow this (even if a link click)
         navigated_ = true;
         return true;
      }
      else if (navType == QWebEnginePage::NavigationTypeLinkClicked)
      {
         // when not allowing external navigation, open an external browser
         // to view the URL
         desktop::openUrl(url);
         navigated_ = true;
      }
      else if (boost::algorithm::ends_with(host, ".youtube.com") ||
            boost::algorithm::ends_with(host, ".vimeo.com")   ||
            boost::algorithm::ends_with(host, ".c9.ms"))
      {
         navigated_ = true;
         return true;
      }

      if (!navigated_)
         this->view()->window()->deleteLater();

      return false;
   }
}

void WebPage::handleBase64Download(QUrl url)
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

   // TODO: may need to use custom JavaScript here?
   // QWebElement aTag;
   // QString urlString = url.toString(QUrl::None);
   // QWebElementCollection aElements = pWebPage->findAllElements(
   //                                           QString::fromUtf8("a"));
   // for (int i=0; i<aElements.count(); i++)
   // {
   //    QWebElement a = aElements.at(i);
   //    QString href = a.attribute(QString::fromUtf8("href"));
   //    href.replace(QChar::fromLatin1('\n'), QString::fromUtf8(""));
   //    if (href == urlString)
   //    {
   //       aTag = a;
   //       break;
   //    }
   // }

   // // if no a tag was found then bail
   // if (aTag.isNull())
   // {
   //    LOG_ERROR_MESSAGE("Unable to finding matching a tag for data url");
   //    return;
   // }

   // // get the download attribute (default filename)
   // QString download = aTag.attribute(QString::fromUtf8("download"));

   QString download = QString::fromUtf8("");
   QString defaultFilename = defaultSaveDir_.absoluteFilePath(download);

   // prompt for filename
   QString filename = QFileDialog::getSaveFileName(
                                            nullptr,
                                            tr("Download File"),
                                            defaultFilename,
                                            defaultSaveDir_.absolutePath(),
                                            nullptr,
                                            standardFileDialogOptions());
   if (!filename.isEmpty())
   {
      // see if we need to force the extension
      FilePath defaultFilePath(defaultFilename.toUtf8().constData());
      FilePath chosenFilePath(filename.toUtf8().constData());
      if (chosenFilePath.extension().empty() &&
          !defaultFilePath.extension().empty())
      {
         filename += QString::fromStdString(defaultFilePath.extension());
      }

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

void WebPage::setViewerUrl(const QString& viewerUrl)
{
   // record about:blank literally
   if (viewerUrl == QString::fromUtf8("about:blank"))
   {
      viewerUrl_ = viewerUrl;
      return;
   }

   // extract the authority (domain and port) from the URL; we'll agree to
   // serve requests for the viewer pane that match this prefix. 
   QUrl url(viewerUrl);
   viewerUrl_ = url.scheme() + QString::fromUtf8("://") +
                url.authority() + QString::fromUtf8("/");
}


void WebPage::setShinyDialogUrl(const QString &shinyDialogUrl)
{
   shinyDialogUrl_ = shinyDialogUrl;
}

void WebPage::triggerAction(WebAction action, bool checked)
{
   // swallow copy events when the selection is empty
   if (action == QWebEnginePage::Copy || action == QWebEnginePage::Cut)
   {
      runJavaScript(
               QStringLiteral("window.desktopHooks.isSelectionEmpty()"),
               [&](const QVariant& emptySelection)
      {
         if (emptySelection.toBool())
            return;

         QWebEnginePage::triggerAction(action, checked);
      });
   }
   else
   {
      QWebEnginePage::triggerAction(action, checked);
   }
}

} // namespace desktop
} // namespace rstudio
