/*
 * DesktopSessionLauncher.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include "DesktopSessionLauncher.hpp"

#include <iostream>

#include <boost/bind.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/TcpIpBlockingClient.hpp>

#include <core/WaitUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/r_util/RUserData.hpp>

#include <QPushButton>

#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopActivationOverlay.hpp"

#define RUN_DIAGNOSTICS_LOG(message) if (desktop::options().runDiagnostics()) \
             std::cout << (message) << std::endl;

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

namespace {

std::string s_launcherToken;

void launchProcess(const std::string& absPath,
                   const QStringList& argList,
                   QProcess** ppProc)
{
   QProcess* process = new QProcess();
   process->setProgram(QString::fromStdString(absPath));
   process->setArguments(argList);
   if (options().runDiagnostics())
      process->setProcessChannelMode(QProcess::ForwardedChannels);

#ifdef _WIN32
   process->setCreateProcessArgumentsModifier([](QProcess::CreateProcessArguments* cpa)
   {
      cpa->flags |= CREATE_NEW_PROCESS_GROUP;
   });
#endif

   process->start();
   *ppProc = process;
}

FilePath abendLogPath()
{
   return desktop::userLogPath().complete("rsession_abort_msg.log");
}

void logEnvVar(const std::string& name)
{
   std::string value = core::system::getenv(name);
   if (!value.empty())
      RUN_DIAGNOSTICS_LOG("  " + name + "=" + value);
}

} // anonymous namespace

void SessionLauncher::launchFirstSession(const core::FilePath& installPath,
                                         bool devMode,
                                         const QStringList& arguments)
{
   connect(&activation(), &DesktopActivation::launchFirstSession,
           this, &SessionLauncher::onLaunchFirstSession, Qt::QueuedConnection);
   connect(&activation(), &DesktopActivation::launchError,
           this, &SessionLauncher::onLaunchError, Qt::QueuedConnection);
   activation().getInitialLicense(arguments, installPath, devMode);
}

Error SessionLauncher::launchFirstSession()
{
   // build a new new launch context
   QString host, port;
   QStringList argList;
   QUrl url;
   buildLaunchContext(&host, &port, &argList, &url);

   // show help home on first run
   argList.push_back(QString::fromUtf8("--show-help-home"));
   argList.push_back(QString::fromUtf8("1"));

   RUN_DIAGNOSTICS_LOG("\nAttempting to launch R session...");
   logEnvVar("RSTUDIO_WHICH_R");
   logEnvVar("R_HOME");
   logEnvVar("R_DOC_DIR");
   logEnvVar("R_INCLUDE_DIR");
   logEnvVar("R_SHARE_DIR");
   logEnvVar("R_LIBS");
   logEnvVar("R_LIBS_USER");
   logEnvVar("DYLD_LIBRARY_PATH");
   logEnvVar("LD_LIBRARY_PATH");
   logEnvVar("PATH");
   logEnvVar("HOME");
   logEnvVar("R_USER");

   // launch the process
   Error error = launchSession(argList, &pRSessionProcess_);
   if (error)
     return error;

   RUN_DIAGNOSTICS_LOG("\nR session launched, "
                           "attempting to connect on port "
                           + port.toStdString() +
                           "...");

   pMainWindow_ = new MainWindow(url);
   pMainWindow_->setAttribute(Qt::WA_DeleteOnClose);
   pMainWindow_->setSessionLauncher(this);
   pMainWindow_->setSessionProcess(pRSessionProcess_);
   pMainWindow_->setAppLauncher(pAppLaunch_);
   pAppLaunch_->setActivationWindow(pMainWindow_);

   desktop::options().restoreMainWindowBounds(pMainWindow_);

   RUN_DIAGNOSTICS_LOG("\nConnected to R session, attempting to initialize...\n");

   // one-time workbench initialized hook for startup file association
   if (!filename_.isNull() && !filename_.isEmpty())
   {
      StringSlotBinder* filenameBinder = new StringSlotBinder(filename_);
      pMainWindow_->connect(pMainWindow_,
                            SIGNAL(firstWorkbenchInitialized()),
                            filenameBinder,
                            SLOT(trigger()));
      pMainWindow_->connect(filenameBinder,
                            SIGNAL(triggered(QString)),
                            pMainWindow_,
                            SLOT(openFileInRStudio(QString)));
   }

   pMainWindow_->connect(pAppLaunch_,
                         SIGNAL(openFileRequest(QString)),
                         pMainWindow_,
                         SLOT(openFileInRStudio(QString)));

   pMainWindow_->connect(pRSessionProcess_,
                         SIGNAL(finished(int,QProcess::ExitStatus)),
                         this, SLOT(onRSessionExited(int,QProcess::ExitStatus)));

   pMainWindow_->connect(&activation(),
                         SIGNAL(licenseLost(QString)),
                         pMainWindow_,
                         SLOT(onLicenseLost(QString)));

   pMainWindow_->connect(&activation(), &DesktopActivation::updateLicenseWarningBar,
                         pMainWindow_, &MainWindow::onUpdateLicenseWarningBar);

   // show the window (but don't if we are doing a --run-diagnostics)
   if (!options().runDiagnostics())
   {
      finalPlatformInitialize(pMainWindow_);
      pMainWindow_->show();
      desktop::activation().setMainWindow(pMainWindow_);
      pAppLaunch_->activateWindow();
      pMainWindow_->loadUrl(url);
   }
   qApp->setQuitOnLastWindowClosed(true);
   return Success();
}

void SessionLauncher::closeAllSatellites()
{
   QWidgetList topLevels = QApplication::topLevelWidgets();
   for (auto pWindow : topLevels)
   {
      if (pWindow != pMainWindow_)
        pWindow->close();
   }
}


void SessionLauncher::onRSessionExited(int, QProcess::ExitStatus)
{
   // if this is a verify-installation session then just quit
   if (options().runDiagnostics())
   {
      pMainWindow_->quit();
      return;
   }

   int pendingQuit = pMainWindow_->collectPendingQuitRequest();

   // if there was no pending quit set then this is a crash
   if (pendingQuit == PendingQuitNone)
   {
      closeAllSatellites();
      pMainWindow_->webView()->webPage()->runJavaScript(
               QString::fromUtf8("window.desktopHooks.notifyRCrashed()"));

      if (abendLogPath().exists())
      {
         showMessageBox(QMessageBox::Critical,
                        pMainWindow_,
                        desktop::activation().editionName(),
                        launchFailedErrorMessage(), QString());
      }
   }

   // quit and exit means close the main window
   else if (pendingQuit == PendingQuitAndExit)
   {
      pMainWindow_->quit();
   }

   // otherwise this is a restart so we need to launch the next session
   else
   {
      if (!activation().allowProductUsage())
      {
         std::string message = "Unable to obtain a license. Please restart RStudio to try again.";
         std::string licenseMessage = activation().currentLicenseStateMessage();
         if (licenseMessage.empty())
            licenseMessage = "None Available";
         message += "\n\nDetails: ";
         message += licenseMessage;
         showMessageBox(QMessageBox::Critical,
                        pMainWindow_,
                        desktop::activation().editionName(),
                        QString::fromUtf8(message.c_str()), QString());
         closeAllSatellites();
         pMainWindow_->quit();
         return;
      }

      // close all satellite windows if we are reloading
      bool reload = (pendingQuit == PendingQuitRestartAndReload);
      if (reload)
         closeAllSatellites();

      // launch next session
      Error error = launchNextSession(reload);
      if (error)
      {
         LOG_ERROR(error);

         showMessageBox(QMessageBox::Critical,
                        pMainWindow_,
                        desktop::activation().editionName(),
                        launchFailedErrorMessage(), QString());

         pMainWindow_->quit();
      }
   }
}

namespace {

core::WaitResult serverReady(const std::string& host, const std::string& port)
{
   core::http::Request request;
   request.setMethod("GET");
   request.setHost("host");
   request.setUri("/");
   request.setHeader("Accept", "*/*");
   request.setHeader("Connection", "close");
   
   core::http::Response response;
   Error error = core::http::sendRequest(host, port, request, &response);
   if (error)
      return WaitResult(WaitContinue, Success());
   else
      return WaitResult(WaitSuccess, Success());
}

} // end anonymous namespace

Error SessionLauncher::launchNextSession(bool reload)
{
   // unset the initial project environment variable it this doesn't
   // pollute future sessions
   core::system::unsetenv(kRStudioInitialProject);

   // disconnect the firstWorkbenchInitialized event so it doesn't occur
   // again when we launch the next session
   pMainWindow_->disconnect(SIGNAL(firstWorkbenchInitialized()));

   // delete the old process object
   pMainWindow_->setSessionProcess(nullptr);
   if (pRSessionProcess_)
   {
      delete pRSessionProcess_;
      pRSessionProcess_ = nullptr;
   }

   // build a new launch context -- re-use the same port if we aren't reloading
   QString port = !reload ? options().portNumber() : QString::fromUtf8("");
   QString host;
   QStringList argList;
   QUrl url;
   buildLaunchContext(&host, &port, &argList, &url);

   // launch the process
   Error error = launchSession(argList, &pRSessionProcess_);
   if (error)
     return error;

   // update the main window's reference to the process object
   pMainWindow_->setSessionProcess(pRSessionProcess_);

   // connect to quit event
   pMainWindow_->connect(pRSessionProcess_,
                         SIGNAL(finished(int,QProcess::ExitStatus)),
                         this, SLOT(onRSessionExited(int,QProcess::ExitStatus)));

   if (reload)
   {
      Error error = core::waitWithTimeout(
               boost::bind(serverReady, host.toStdString(), port.toStdString()),
               50,
               25,
               10);
      if (error)
         LOG_ERROR(error);
      
      nextSessionUrl_ = url;
      QTimer::singleShot(0, this, SLOT(onReloadFrameForNextSession()));
   }

   return Success();
}

void SessionLauncher::onReloadFrameForNextSession()
{
   pMainWindow_->loadUrl(nextSessionUrl_);
   nextSessionUrl_.clear();
}

void SessionLauncher::onLaunchFirstSession()
{
   Error error = launchFirstSession();
   if (error)
   {
      LOG_ERROR(error);
      activation().emitLaunchError(launchFailedErrorMessage());
   }
}

Error SessionLauncher::launchSession(const QStringList& argList,
                                     QProcess** ppRSessionProcess)
{
   // always remove the abend log path before launching
   Error error = abendLogPath().removeIfExists();
   if (error)
      LOG_ERROR(error);

   return  parent_process_monitor::wrapFork(
         boost::bind(launchProcess,
                     sessionPath_.absolutePath(),
                     argList,
                     ppRSessionProcess));
}

void SessionLauncher::onLaunchError(QString message)
{
   qApp->setQuitOnLastWindowClosed(true);
   if (!message.isEmpty())
   {
      QMessageBox errorMsg(safeMessageBoxIcon(QMessageBox::Critical),
                           desktop::activation().editionName(), message);
      errorMsg.addButton(QMessageBox::Close);
      errorMsg.setWindowFlag(Qt::WindowContextHelpButtonHint, false);
      errorMsg.exec();
   }

   if (pMainWindow_)
      pMainWindow_->quit();
   else
      qApp->exit(EXIT_FAILURE);
}

QString SessionLauncher::collectAbendLogMessage() const
{
   std::string contents;
   FilePath abendLog = abendLogPath();
   if (abendLog.exists())
   {
      Error error = core::readStringFromFile(abendLog, &contents);
      if (error)
         LOG_ERROR(error);

      error = abendLog.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }

   return QString::fromStdString(contents);
}

QString SessionLauncher::launchFailedErrorMessage() const
{
   QString errMsg = QString::fromUtf8("The R session had a fatal error.");

   // check for abend log
   QString abendLogMessage = collectAbendLogMessage();

   // check for R version mismatch
   if (abendLogMessage.contains(
                    QString::fromUtf8("arguments passed to .Internal")))
   {
      errMsg.append(QString::fromUtf8("\n\nThis error was very likely caused "
                    "by R attempting to load packages from a different "
                    "incompatible version of R on your system. Please remove "
                    "other versions of R and/or remove environment variables "
                    "that reference libraries from other versions of R "
                    "before proceeding."));
   }

   if (!abendLogMessage.isEmpty())
      errMsg.append(QString::fromUtf8("\n\n").append(abendLogMessage));

   // check for stderr
   if (pRSessionProcess_)
   {
      QString errmsgs = QString::fromLocal8Bit(
                              pRSessionProcess_->readAllStandardError());
      if (errmsgs.size())
      {
         errMsg = errMsg.append(
                           QString::fromUtf8("\n\n")).append(errmsgs);
      }
   }

   return errMsg;
}


void SessionLauncher::buildLaunchContext(QString* pHost,
                                         QString* pPort,
                                         QStringList* pArgList,
                                         QUrl* pUrl) const
{
   *pHost = QString::fromUtf8("127.0.0.1");
   if (pPort->isEmpty())
      *pPort = desktop::options().newPortNumber();
   *pUrl = QUrl(QString::fromUtf8("http://") + *pHost +
                QString::fromUtf8(":") + *pPort + QString::fromUtf8("/"));

   if (!confPath_.empty())
   {
      *pArgList << QString::fromUtf8("--config-file") <<
                   QString::fromUtf8(confPath_.absolutePath().c_str());
   }
   else
   {
      // explicitly pass "none" so that rsession doesn't read an
      // /etc/rstudio/rsession.conf file which may be sitting around
      // from a previous configuration or install
      *pArgList << QString::fromUtf8("--config-file") <<
                   QString::fromUtf8("none");
   }

   *pArgList << QString::fromUtf8("--program-mode") <<
                QString::fromUtf8("desktop");

   *pArgList << QString::fromUtf8("--www-port") << *pPort;

   // create launch token if we haven't already
   if (s_launcherToken.empty())
      s_launcherToken = core::system::generateShortenedUuid();
   *pArgList << QString::fromUtf8("--launcher-token") <<
                QString::fromUtf8(s_launcherToken.c_str());

   if (options().runDiagnostics())
      *pArgList << QString::fromUtf8("--verify-installation") <<
                   QString::fromUtf8("1");
}


} // namespace desktop
} // namespace rstudio
