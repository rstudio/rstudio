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

#include <QFileDialog>
#include <QWebEngineSettings>

#include "DesktopInfo.hpp"
#include "DesktopDownloadItemHelper.hpp"
#include "DesktopMainWindow.hpp"
#include "DesktopSatelliteWindow.hpp"
#include "DesktopSecondaryWindow.hpp"
#include "DesktopWebProfile.hpp"
#include "DesktopWindowTracker.hpp"

using namespace rstudio::core;

extern QString sharedSecret;

namespace rstudio {
namespace desktop {

namespace {

WindowTracker s_windowTracker;
std::mutex s_mutex;

// NOTE: This variable is static and hence shared by all of the web pages
// created by RStudio. This is intentional as it's possible that the
// request interceptor from one page will handle a request, and a _different_
// page will handle the new navigation attempt -- so both pages will need
// access to the same data.
std::map<QUrl, int> s_subframes;

void onDownloadRequested(QWebEngineDownloadItem* downloadItem)
{
   // request directory from user
   QString downloadPath = QFileDialog::getSaveFileName(
            nullptr,
            QStringLiteral("Save File"),
            downloadItem->path());
   
   if (downloadPath.isEmpty())
      return;
   
   DownloadHelper::manageDownload(downloadItem, downloadPath);
}

} // anonymous namespace

WebPage::WebPage(QUrl baseUrl, QWidget *parent, bool allowExternalNavigate) :
      QWebEnginePage(new WebProfile(baseUrl), parent),
      baseUrl_(baseUrl),
      allowExternalNav_(allowExternalNavigate)
{
   settings()->setAttribute(QWebEngineSettings::PluginsEnabled, true);
   settings()->setAttribute(QWebEngineSettings::FullScreenSupportEnabled, true);
   settings()->setAttribute(QWebEngineSettings::JavascriptCanOpenWindows, true);
   settings()->setAttribute(QWebEngineSettings::JavascriptCanAccessClipboard, true);
   settings()->setAttribute(QWebEngineSettings::LocalStorageEnabled, true);
   
   defaultSaveDir_ = QDir::home();
   connect(this, SIGNAL(windowCloseRequested()), SLOT(closeRequested()));
   connect(profile(), &QWebEngineProfile::downloadRequested, onDownloadRequested);
   connect(profile(), &WebProfile::urlIntercepted, this, &WebPage::onUrlIntercepted, Qt::DirectConnection);
}

void WebPage::setBaseUrl(const QUrl& baseUrl)
{
   profile()->setBaseUrl(baseUrl);
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
      pWindow = new SatelliteWindow(pMainWindow, name, this);
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

void WebPage::onUrlIntercepted(QUrl url, int type)
{
   if (type == QWebEngineUrlRequestInfo::ResourceTypeSubFrame)
   {
      s_subframes[url] = type;
   }
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

// NOTE: NavigationType is unreliable, as per:
//
//    https://bugreports.qt.io/browse/QTBUG-56805
//
// so we avoid querying it here.
bool WebPage::acceptNavigationRequest(const QUrl &url,
                                      NavigationType /* navType*/,
                                      bool isMainFrame)
{
   if (url.toString() == QStringLiteral("about:blank"))
      return true;

   if (url.scheme() != QStringLiteral("http") &&
       url.scheme() != QStringLiteral("https") &&
       url.scheme() != QStringLiteral("mailto") &&
       url.scheme() != QStringLiteral("data"))
   {
      qDebug() << url.toString();
      return false;
   }
   
   // determine if this is a local request (handle internally only if local)
   std::string host = url.host().toStdString();
   bool isLocal = host == "localhost" || host == "127.0.0.1" || host == "::1";
   
   // accept Chrome Developer Tools navigation attempts
#ifndef NDEBUG
   if (isLocal && url.port() == desktopInfo().getChromiumDevtoolsPort())
      return true;
#endif
   
   // if this appears to be an attempt to load an external page within
   // an iframe, don't load it
   if (!isLocal && s_subframes.count(url))
   {
      s_subframes.erase(url);
      return false;
   }

   if ((baseUrl_.isEmpty() && isLocal) ||
       (url.scheme() == baseUrl_.scheme() &&
        url.host() == baseUrl_.host() &&
        url.port() == baseUrl_.port()))
   {
      return true;
   }
   // allow viewer urls to be handled internally by Qt. note that the client is responsible for 
   // ensuring that non-local viewer urls are appropriately sandboxed.
   else if (!viewerUrl().isEmpty() &&
            url.toString().startsWith(viewerUrl()))
   {
      return true;
   }
   // allow shiny dialog urls to be handled internally by Qt
   else if (isLocal && !shinyDialogUrl_.isEmpty() &&
            url.toString().startsWith(shinyDialogUrl_))
   {
      return true;
   }
   else
   {
      bool navigated = false;
      
      if (allowExternalNav_)
      {
         // if allowing external navigation, follow this (even if a link click)
         return true;
      }
      else if (boost::algorithm::ends_with(host, ".youtube.com") ||
               boost::algorithm::ends_with(host, ".vimeo.com")   ||
               boost::algorithm::ends_with(host, ".c9.ms"))
      {
         return true;
      }
      else
      {
         // when not allowing external navigation, open an external browser
         // to view the URL
         desktop::openUrl(url);
         navigated = true;
      }

      if (!navigated)
         this->view()->window()->deleteLater();

      return false;
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

QString WebPage::viewerUrl()
{
   if (viewerUrl_.isEmpty())
   {
      // if we don't know the viewer URL ourselves but we're a child window, ask our parent
      BrowserWindow *parent = dynamic_cast<BrowserWindow*>(view()->window());
      if (parent != nullptr && parent->opener() != nullptr)
      {
         return parent->opener()->viewerUrl();
      }
   }

   // return our own viewer URL
   return viewerUrl_;
}

void WebPage::setShinyDialogUrl(const QString &shinyDialogUrl)
{
   shinyDialogUrl_ = shinyDialogUrl;
}

void WebPage::triggerAction(WebAction action, bool checked)
{
   QWebEnginePage::triggerAction(action, checked);
}

} // namespace desktop
} // namespace rstudio
