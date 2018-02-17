/*
 * DesktopMainWindow.cpp
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

#include "DesktopMainWindow.hpp"

#include <QToolBar>
#include <QWebChannel>
#include <QWebEngineScript>
#include <QWebEngineScriptCollection>

#include <boost/bind.hpp>
#include <boost/format.hpp>

#include <core/FileSerializer.hpp>

#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopSessionLauncher.hpp"
#include "DockTileView.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

MainWindow::MainWindow(QUrl url) :
      GwtWindow(false, false, QString(), url, nullptr),
      menuCallback_(this),
      gwtCallback_(this, this),
      pSessionLauncher_(nullptr),
      pCurrentSessionProcess_(nullptr)
{
   pToolbar_->setVisible(false);

   // create web channel and bind GWT callbacks
   auto* channel = new QWebChannel(this);
   channel->registerObject(QStringLiteral("desktop"), &gwtCallback_);
   channel->registerObject(QStringLiteral("desktopInfo"), &desktopInfo());
   channel->registerObject(QStringLiteral("desktopMenuCallback"), &menuCallback_);
   webPage()->setWebChannel(channel);

   // load qwebchannel.js
   QFile webChannelJsFile(QStringLiteral(":/qtwebchannel/qwebchannel.js"));
   if (!webChannelJsFile.open(QFile::ReadOnly))
      qDebug() << "Failed to open qwebchannel.js!";

   QString webChannelJs = QString::fromUtf8(webChannelJsFile.readAll());
   webChannelJsFile.close();

   // append our WebChannel initialization code
   const char* webChannelInit = R"EOF(
      new QWebChannel(qt.webChannelTransport, function(channel) {

         // export channel objects to the main window
         for (var key in channel.objects) {
            window[key] = channel.objects[key];
         }

         // notify that we're finished initialization and load
         // GWT sources if necessary
         window.qt.webChannelReady = true;
         if (typeof window.rstudioDelayLoadApplication == "function") {
            window.rstudioDelayLoadApplication();
            window.rstudioDelayLoadApplication = null;
         }
      });
   )EOF";

   webChannelJs.append(QString::fromUtf8(webChannelInit));

   QWebEngineScript script;
   script.setName(QStringLiteral("qwebchannel"));
   script.setInjectionPoint(QWebEngineScript::DocumentCreation);
   script.setWorldId(QWebEngineScript::MainWorld);
   script.setSourceCode(webChannelJs);
   webPage()->scripts().insert(script);

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

   connect(webView(), SIGNAL(onCloseWindowShortcut()),
           this, SLOT(onCloseWindowShortcut()));

   connect(qApp, SIGNAL(commitDataRequest(QSessionManager&)),
           this, SLOT(commitDataRequest(QSessionManager&)),
           Qt::DirectConnection);

   setWindowIcon(QIcon(QString::fromUtf8(":/icons/RStudio.ico")));

   setWindowTitle(QString::fromUtf8("RStudio"));

#ifdef Q_OS_MAC
   auto* pDefaultMenu = new QMenuBar(this);
   pDefaultMenu->addMenu(new WindowMenu());
#endif

   desktop::enableFullscreenMode(this, true);
}

QString MainWindow::getSumatraPdfExePath()
{
   return desktopInfo().getSumatraPdfExePath();
}

void MainWindow::launchSession(bool reload)
{
   Error error = pSessionLauncher_->launchNextSession(reload);
   if (error)
   {
      LOG_ERROR(error);

      showMessageBox(QMessageBox::Critical,
                     this,
                     QString::fromUtf8("RStudio"),
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

void MainWindow::onCloseWindowShortcut()
{
   webPage()->runJavaScript(
            QStringLiteral("window.desktopHooks.isCommandEnabled('closeSourceDoc')"),
            [&](QVariant closeSourceDocEnabled)
   {
      if (!closeSourceDocEnabled.toBool())
         close();
   });
}

void MainWindow::onWorkbenchInitialized()
{
   //QTimer::singleShot(300, this, SLOT(resetMargins()));

   // reset state (in case this occurred in response to a manual reload
   // or reload for a new project context)
   quitConfirmed_ = false;
   geometrySaved_ = false;

   webPage()->runJavaScript(
            QStringLiteral("window.desktopHooks.getActiveProjectDir()"),
            [&](QVariant qProjectDir)
   {
      QString projectDir = qProjectDir.toString();

      if (projectDir.length() > 0)
      {
         setWindowTitle(projectDir + QString::fromUtf8(" - RStudio"));
         DockTileView::setLabel(projectDir);
      }
      else
      {
         setWindowTitle(QString::fromUtf8("RStudio"));
         DockTileView::setLabel(QString());
      }

      avoidMoveCursorIfNecessary();
   });
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

void MainWindow::quit()
{
   quitConfirmed_ = true;
   close();
}

void MainWindow::invokeCommand(QString commandId)
{
   QString command =
         QString::fromUtf8("window.desktopHooks.invokeCommand('") +
         commandId +
         QString::fromUtf8("');");

   webPage()->runJavaScript(command);
}

void MainWindow::closeEvent(QCloseEvent* pEvent)
{
   if (!webPage())
   {
       pEvent->accept();
       return;
   }

   if (!geometrySaved_)
   {
      desktop::options().saveMainWindowBounds(this);
      geometrySaved_ = true;
   }

   if (quitConfirmed_ ||
       pCurrentSessionProcess_ == nullptr ||
       pCurrentSessionProcess_->state() != QProcess::Running)
   {
      pEvent->accept();
      return;
   }

   pEvent->ignore();
   webPage()->runJavaScript(
            QStringLiteral("!!window.desktopHooks"),
            [&](QVariant hasQuitR) {

      if (!hasQuitR.toBool())
      {
         LOG_ERROR_MESSAGE("Main window closed unexpectedly");

         // exit to avoid user having to kill/force-close the application
         QApplication::quit();
      }
      else
      {
         webPage()->runJavaScript(
                  QStringLiteral("window.desktopHooks.quitR()"),
                  [&](QVariant ignored)
         {
         });
      }
   });
}

double MainWindow::getZoomLevel()
{
   return options().zoomLevel();
}

void MainWindow::setZoomLevel(double zoomLevel)
{
   options().setZoomLevel(zoomLevel);
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

// private interface for SessionLauncher

void MainWindow::setSessionLauncher(SessionLauncher* pSessionLauncher)
{
   pSessionLauncher_ = pSessionLauncher;
}

void MainWindow::setSessionProcess(QProcess* pSessionProcess)
{
   pCurrentSessionProcess_ = pSessionProcess;
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

} // namespace desktop
} // namespace rstudio
