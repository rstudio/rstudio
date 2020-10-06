/*
 * DesktopMainWindow.cpp
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

#include "DesktopMainWindow.hpp"

#include <QToolBar>
#include <QWebChannel>

#include <boost/bind.hpp>
#include <boost/format.hpp>

#include <core/FileSerializer.hpp>
#include <core/Macros.hpp>
#include <core/text/TemplateFilter.hpp>

#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopSessionLauncher.hpp"
#include "DesktopJobLauncherOverlay.hpp"
#include "RemoteDesktopSessionLauncherOverlay.hpp"
#include "DockTileView.hpp"
#include "DesktopActivationOverlay.hpp"
#include "DesktopSessionServersOverlay.hpp"
#include "DesktopRCommandEvaluator.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

namespace {

#ifdef _WIN32

void CALLBACK onDialogStart(HWINEVENTHOOK hook, DWORD event, HWND hwnd,
                            LONG idObject, LONG idChild,
                            DWORD dwEventThread, DWORD dwmsEventTime)
{
   ::BringWindowToTop(hwnd);
}

#endif

} // end anonymous namespace

MainWindow::MainWindow(QUrl url,
                       bool isRemoteDesktop) :
      GwtWindow(false, false, QString(), url, nullptr, nullptr, isRemoteDesktop),
      isRemoteDesktop_(isRemoteDesktop),
      menuCallback_(this),
      gwtCallback_(this, this, isRemoteDesktop),
      pSessionLauncher_(nullptr),
      pRemoteSessionLauncher_(nullptr),
      pLauncher_(new JobLauncher(this)),
      pCurrentSessionProcess_(nullptr)
{
   RCommandEvaluator::setMainWindow(this);
   pToolbar_->setVisible(false);

#ifdef _WIN32
   eventHook_ = nullptr;
#endif

   // bind GWT callbacks
   auto* channel = webPage()->webChannel();
   channel->registerObject(QStringLiteral("desktop"), &gwtCallback_);
   if (isRemoteDesktop_)
   {
      // since the object registration is asynchronous, during the GWT setup code
      // there is a race condition where the initialization can happen before the
      // remoteDesktop object is registered, making the GWT application think that
      // it should use regular desktop objects - to circumvent this, we use a custom
      // user agent string that the GWT code can detect with 100% success rate to
      // get around this race condition
      QString userAgent = webPage()->profile()->httpUserAgent().append(
               QStringLiteral("; RStudio Remote Desktop"));
      webPage()->profile()->setHttpUserAgent(userAgent);
      channel->registerObject(QStringLiteral("remoteDesktop"), &gwtCallback_);
   }
   channel->registerObject(QStringLiteral("desktopMenuCallback"), &menuCallback_);

   // Dummy menu bar to deal with the fact that
   // the real menu bar isn't ready until well
   // after startup.
#ifndef Q_OS_MAC
   auto* pMainMenuStub = new QMenuBar(this);
   pMainMenuStub->addMenu(QString::fromUtf8("File"));
   pMainMenuStub->addMenu(QString::fromUtf8("Edit"));
   pMainMenuStub->addMenu(QString::fromUtf8("Code"));
   pMainMenuStub->addMenu(QString::fromUtf8("View"));
   pMainMenuStub->addMenu(QString::fromUtf8("Plots"));
   pMainMenuStub->addMenu(QString::fromUtf8("Session"));
   pMainMenuStub->addMenu(QString::fromUtf8("Build"));
   pMainMenuStub->addMenu(QString::fromUtf8("Debug"));
   pMainMenuStub->addMenu(QString::fromUtf8("Profile"));
   pMainMenuStub->addMenu(QString::fromUtf8("Tools"));
   pMainMenuStub->addMenu(QString::fromUtf8("Help"));
   setMenuBar(pMainMenuStub);
#endif
   
   connect(&menuCallback_, SIGNAL(menuBarCompleted(QMenuBar*)),
           this, SLOT(setMenuBar(QMenuBar*)));
   connect(&menuCallback_, SIGNAL(commandInvoked(QString)),
           this, SLOT(invokeCommand(QString)));

   connect(&menuCallback_, SIGNAL(zoomActualSize()), this, SLOT(zoomActualSize()));
   connect(&menuCallback_, SIGNAL(zoomIn()), this, SLOT(zoomIn()));
   connect(&menuCallback_, SIGNAL(zoomOut()), this, SLOT(zoomOut()));

   connect(&gwtCallback_, SIGNAL(workbenchInitialized()),
           this, SIGNAL(firstWorkbenchInitialized()));
   connect(&gwtCallback_, SIGNAL(workbenchInitialized()),
           this, SLOT(onWorkbenchInitialized()));
   connect(&gwtCallback_, SIGNAL(sessionQuit()),
           this, SLOT(onSessionQuit()));

   connect(webView(), SIGNAL(onCloseWindowShortcut()),
           this, SLOT(onCloseWindowShortcut()));

   connect(webView(), &WebView::urlChanged,
           this, &MainWindow::onUrlChanged);

   connect(webView(), &WebView::loadFinished,
           this, &MainWindow::onLoadFinished);

   connect(webPage(), &QWebEnginePage::loadFinished,
           &menuCallback_, &MenuCallback::cleanUpActions);

   connect(&desktopInfo(), &DesktopInfo::fixedWidthFontListChanged, [this]() {
      QString js = QStringLiteral(
         "if (typeof window.onFontListReady === 'function') window.onFontListReady()");
      this->webPage()->runJavaScript(js);
   });

   connect(qApp, SIGNAL(commitDataRequest(QSessionManager&)),
           this, SLOT(commitDataRequest(QSessionManager&)),
           Qt::DirectConnection);

   setWindowIcon(QIcon(QString::fromUtf8(":/icons/RStudio.ico")));

   setWindowTitle(desktop::activation().editionName());

#ifdef Q_OS_MAC
   auto* pDefaultMenu = new QMenuBar(this);
   pDefaultMenu->addMenu(new WindowMenu());
#endif

   desktop::enableFullscreenMode(this, true);

   Error error = pLauncher_->initialize();
   if (error)
   {
      LOG_ERROR(error);
      showError(nullptr,
                QStringLiteral("Initialization error"),
                QStringLiteral("Could not initialize Job Launcher"),
                QString());
      ::_exit(EXIT_FAILURE);
   }
}

bool MainWindow::isRemoteDesktop() const
{
   return isRemoteDesktop_;
}

QString MainWindow::getSumatraPdfExePath()
{
   return desktopInfo().getSumatraPdfExePath();
}

void MainWindow::launchSession(bool reload)
{
   // we're about to start another session, so clear the workbench init flag
   // (will get set again once the new session has initialized the workbench)
   workbenchInitialized_ = false;

   Error error = pSessionLauncher_->launchNextSession(reload);
   if (error)
   {
      LOG_ERROR(error);

      showMessageBox(QMessageBox::Critical,
                     this,
                     desktop::activation().editionName(),
                     QString::fromUtf8("The R session failed to start."),
                     QString());

      quit();
   }
}

void MainWindow::launchRStudio(const std::vector<std::string> &args,
                               const std::string& initialDir)
{
    pAppLauncher_->launchRStudio(args, initialDir);
}

void MainWindow::saveRemoteAuthCookies(const boost::function<QList<QNetworkCookie>()>& loadCookies,
                                       const boost::function<void(QList<QNetworkCookie>)>& saveCookies,
                                       bool saveSessionCookies)
{   
   std::map<std::string, QNetworkCookie> cookieMap;
   auto addCookie = [&](const QNetworkCookie& cookie)
   {
      // ensure we don't save expired cookies
      // due to how cookie domains are fluid, it's possible
      // we could continue to save expired cookies if we don't filter them out
      if (!cookie.isSessionCookie() && cookie.expirationDate().toUTC() <= QDateTime::currentDateTimeUtc())
         return;

      // also do not save the cookie if it is a session cookie and we were not told to explicitly save them
      if (!saveSessionCookies && cookie.isSessionCookie())
         return;

      for (const auto& sessionServer : sessionServerSettings().servers())
      {
         if (sessionServer.cookieBelongs(cookie))
         {
            cookieMap[sessionServer.label() + "." + cookie.name().toStdString()] = cookie;
         }
      }
   };

   // merge with existing cookies on disk
   QList<QNetworkCookie> cookies = loadCookies();
   for (const QNetworkCookie& cookie : cookies)
   {
      addCookie(cookie);
   }

   if (pRemoteSessionLauncher_)
   {
      std::map<std::string, QNetworkCookie> remoteCookies = pRemoteSessionLauncher_->getCookies();

      if (!remoteCookies.empty())
      {
         for (const auto& pair : remoteCookies)
         {
            addCookie(pair.second);
         }
      }
      else
      {
         // cookies were empty, meaning they needed to be cleared (for example, due to sign out)
         // ensure that all cookies for the domain are cleared out
         for (auto it = cookieMap.cbegin(); it != cookieMap.cend();)
         {
            if (pRemoteSessionLauncher_->sessionServer().cookieBelongs(it->second))
            {
               it = cookieMap.erase(it);
            }
            else
            {
               ++it;
            }
         }
      }
   }

   if (pLauncher_)
   {
      std::map<std::string, QNetworkCookie> cookies = pLauncher_->getCookies();
      for (const auto& pair : cookies)
      {
         addCookie(pair.second);
      }
   }

   QList<QNetworkCookie> mergedCookies;
   for (const auto& pair: cookieMap)
   {
      mergedCookies.push_back(pair.second);
   }

   saveCookies(mergedCookies);
}

void MainWindow::launchRemoteRStudio()
{
   saveRemoteAuthCookies(boost::bind(&Options::tempAuthCookies, &options()),
                         boost::bind(&Options::setTempAuthCookies, &options(), _1),
                         true);

   std::vector<std::string> args;
   args.push_back(kSessionServerOption);
   args.push_back(pRemoteSessionLauncher_->sessionServer().url());
   args.push_back(kTempCookiesOption);

   pAppLauncher_->launchRStudio(args);
}

void MainWindow::launchRemoteRStudioProject(const QString& projectUrl)
{
   saveRemoteAuthCookies(boost::bind(&Options::tempAuthCookies, &options()),
                         boost::bind(&Options::setTempAuthCookies, &options(), _1),
                         true);

   std::vector<std::string> args;
   args.push_back(kSessionServerOption);
   args.push_back(pRemoteSessionLauncher_->sessionServer().url());
   args.push_back(kSessionServerUrlOption);
   args.push_back(projectUrl.toStdString());
   args.push_back(kTempCookiesOption);

   pAppLauncher_->launchRStudio(args);
}

bool MainWindow::workbenchInitialized()
{
    return workbenchInitialized_;
}

void MainWindow::onWorkbenchInitialized()
{
   //QTimer::singleShot(300, this, SLOT(resetMargins()));

   // reset state (in case this occurred in response to a manual reload
   // or reload for a new project context)
   quitConfirmed_ = false;
   geometrySaved_ = false;
   workbenchInitialized_ = true;

   webPage()->runJavaScript(
            QStringLiteral("window.desktopHooks.getActiveProjectDir()"),
            [&](QVariant qProjectDir)
   {
      QString projectDir = qProjectDir.toString();

      if (projectDir.length() > 0)
      {
         setWindowTitle(tr("%1 - %2").arg(projectDir).arg(desktop::activation().editionName()));
         DockTileView::setLabel(projectDir);
      }
      else
      {
         setWindowTitle(desktop::activation().editionName());
         DockTileView::setLabel(QString());
      }

      avoidMoveCursorIfNecessary();
   });

#ifdef Q_OS_MAC
   webView()->setFocus();
#endif
}

void MainWindow::resetMargins()
{
   setContentsMargins(0, 0, 0, 0);
}

// this notification occurs when windows or X11 is shutting
// down -- in this case we want to be a good citizen and just
// exit right away so we notify the gwt callback that a legit
// quit and exit is on the way and we set the quitConfirmed_
// flag so no prompting occurs (note that source documents
// have already been auto-saved so will be restored next time
// the current project context is opened)
void MainWindow::commitDataRequest(QSessionManager &manager)
{
   gwtCallback_.setPendingQuit(PendingQuitAndExit);
   quitConfirmed_ = true;
}

void MainWindow::loadUrl(const QUrl& url)
{
   webView()->setBaseUrl(url);
   webView()->load(url);
}

void MainWindow::loadRequest(const QWebEngineHttpRequest& request)
{
   webView()->setBaseUrl(request.url());
   webView()->load(request);
}

void MainWindow::loadHtml(const QString& html)
{
   webView()->setHtml(html);
}

QWebEngineProfile* MainWindow::getPageProfile()
{
   return webView()->page()->profile();
}

void MainWindow::quit()
{
   RCommandEvaluator::setMainWindow(nullptr);
   quitConfirmed_ = true;
   close();
}

void MainWindow::invokeCommand(QString commandId)
{
#ifdef Q_OS_MAC
   QString fmt = QStringLiteral(R"EOF(
var wnd;
try {
   wnd = window.$RStudio.last_focused_window;
} catch (e) {
   wnd = window;
}
(wnd || window).desktopHooks.invokeCommand('%1');
)EOF");
#else
   QString fmt = QStringLiteral("window.desktopHooks.invokeCommand('%1')");
#endif
   
   QString command = fmt.arg(commandId);
   webPage()->runJavaScript(command);
}

void MainWindow::runJavaScript(QString script)
{
   webPage()->runJavaScript(script);
}

namespace {

void closeAllSatellites(QWidget* mainWindow)
{
   for (auto window: QApplication::topLevelWidgets())
      if (window != mainWindow)
         window->close();
}

} // end anonymous namespace

void MainWindow::onSessionQuit()
{
   if (isRemoteDesktop_)
   {
      int pendingQuit = collectPendingQuitRequest();
      if (pendingQuit == PendingQuitAndExit || quitConfirmed_)
      {
         closeAllSatellites(this);
         quit();
      }
   }
}

void MainWindow::closeEvent(QCloseEvent* pEvent)
{
#ifdef _WIN32
   if (eventHook_)
      ::UnhookWinEvent(eventHook_);
#endif

   desktopInfo().onClose();
   saveRemoteAuthCookies(boost::bind(&Options::authCookies, &options()),
                         boost::bind(&Options::setAuthCookies, &options(), _1),
                         false);

   if (!geometrySaved_)
   {
      desktop::options().saveMainWindowBounds(this);
      geometrySaved_ = true;
   }

   CloseServerSessions close = sessionServerSettings().closeServerSessionsOnExit();

   if (quitConfirmed_ ||
       (!isRemoteDesktop_ && pCurrentSessionProcess_ == nullptr) ||
       (!isRemoteDesktop_ && pCurrentSessionProcess_->state() != QProcess::Running))
   {
      closeAllSatellites(this);
      pEvent->accept();
      return;
   }

   auto quit = [this]()
   {
      closeAllSatellites(this);
      this->quit();
   };

   pEvent->ignore();
   webPage()->runJavaScript(
            QStringLiteral("!!window.desktopHooks"),
            [=](QVariant hasQuitR) {

      if (!hasQuitR.toBool())
      {
         LOG_ERROR_MESSAGE("Main window closed unexpectedly");

         // exit to avoid user having to kill/force-close the application
         quit();
      }
      else
      {
         if (!isRemoteDesktop_ ||
             close == CloseServerSessions::Always)
         {
            webPage()->runJavaScript(
                     QStringLiteral("window.desktopHooks.quitR()"),
                     [=](QVariant ignored)
            {
               quitConfirmed_ = true;
            });
         }
         else if (close == CloseServerSessions::Never)
         {
            quit();
         }
         else
         {
            webPage()->runJavaScript(
                     QStringLiteral("window.desktopHooks.promptToQuitR()"),
                     [=](QVariant ignored)
            {
               quitConfirmed_ = true;
            });
         }
      }
   });
}

void MainWindow::setMenuBar(QMenuBar *pMenubar)
{
   delete menuBar();
   this->QMainWindow::setMenuBar(pMenubar);
}

void MainWindow::openFileInRStudio(QString path)
{
   QFileInfo fileInfo(path);
   if (!fileInfo.isAbsolute() || !fileInfo.exists() || !fileInfo.isFile())
      return;

   path = path.replace(QString::fromUtf8("\\"), QString::fromUtf8("\\\\"))
         .replace(QString::fromUtf8("\""), QString::fromUtf8("\\\""))
         .replace(QString::fromUtf8("\n"), QString::fromUtf8("\\n"));

   webView()->page()->runJavaScript(
            QString::fromUtf8("window.desktopHooks.openFile(\"") + path + QString::fromUtf8("\")"));
}

void MainWindow::onPdfViewerClosed(QString pdfPath)
{
   webView()->page()->runJavaScript(
            QString::fromUtf8("window.synctexNotifyPdfViewerClosed(\"") +
            pdfPath + QString::fromUtf8("\")"));
}

void MainWindow::onPdfViewerSyncSource(QString srcFile, int line, int column)
{
   boost::format fmt("window.desktopSynctexInverseSearch(\"%1%\", %2%, %3%)");
   std::string js = boost::str(fmt % srcFile.toStdString() % line % column);
   webView()->page()->runJavaScript(QString::fromStdString(js));
}

void MainWindow::onLicenseLost(QString licenseMessage)
{
   webView()->page()->runJavaScript(
            QString::fromUtf8("window.desktopHooks.licenseLost('") + licenseMessage +
            QString::fromUtf8("');"));
}

void MainWindow::onUpdateLicenseWarningBar(QString message)
{
   webView()->page()->runJavaScript(
            QString::fromUtf8("window.desktopHooks.updateLicenseWarningBar('") + message +
            QString::fromUtf8("');"));
}

// private interface for SessionLauncher

void MainWindow::setSessionLauncher(SessionLauncher* pSessionLauncher)
{
   pSessionLauncher_ = pSessionLauncher;
}

RemoteDesktopSessionLauncher* MainWindow::getRemoteDesktopSessionLauncher()
{
   return pRemoteSessionLauncher_;
}

boost::shared_ptr<JobLauncher> MainWindow::getJobLauncher()
{
   return pLauncher_;
}

void MainWindow::setRemoteDesktopSessionLauncher(RemoteDesktopSessionLauncher* pSessionLauncher)
{
   pRemoteSessionLauncher_ = pSessionLauncher;
}

void MainWindow::setSessionProcess(QProcess* pSessionProcess)
{
   pCurrentSessionProcess_ = pSessionProcess;

   // when R creates dialogs (e.g. through utils::askYesNo), their first
   // invocation might show behind the RStudio window. this allows us
   // to detect when those Windows are opened and focused, and raise them
   // to the front.
#ifdef _WIN32
   if (eventHook_)
      ::UnhookWinEvent(eventHook_);

   if (pSessionProcess)
   {
      eventHook_ = ::SetWinEventHook(
               EVENT_SYSTEM_DIALOGSTART, EVENT_SYSTEM_DIALOGSTART,
               nullptr,
               onDialogStart,
               pSessionProcess->processId(),
               0,
               WINEVENT_OUTOFCONTEXT);
   }
#endif

}

void MainWindow::setAppLauncher(ApplicationLaunch *pAppLauncher)
{
    pAppLauncher_ = pAppLauncher;
}

// allow SessionLauncher to collect restart requests from GwtCallback
int MainWindow::collectPendingQuitRequest()
{
   return gwtCallback_.collectPendingQuitRequest();
}

bool MainWindow::desktopHooksAvailable()
{
   return desktopInfo().desktopHooksAvailable();
}

void MainWindow::onActivated()
{
}

void MainWindow::onUrlChanged(QUrl url)
{
   urlChanged(url);
}

void MainWindow::onLoadFinished(bool ok)
{
   if (ok || pRemoteSessionLauncher_)
      return;

   RS_CALL_ONCE();
   
   std::map<std::string,std::string> vars;
   vars["url"] = webView()->url().url().toStdString();
   std::ostringstream oss;
   Error error = text::renderTemplate(options().resourcesPath().completePath("html/connect.html"),
                                      vars, oss);
   if (error)
      LOG_ERROR(error);
   else
      loadHtml(QString::fromStdString(oss.str()));
}

WebView* MainWindow::getWebView()
{
   return webView();
}

} // namespace desktop
} // namespace rstudio
