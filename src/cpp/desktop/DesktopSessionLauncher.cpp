/*
 * DesktopSessionLauncher.cpp
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

#include "DesktopSessionLauncher.hpp"

#include <iostream>

#include <boost/bind.hpp>

#include <core/WaitUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ParentProcessMonitor.hpp>

#include <QProcess>
#include <QtNetwork/QTcpSocket>

#include "DesktopUtils.hpp"
#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopGwtCallback.hpp"

#define RUN_DIAGNOSTICS_LOG(message) if (desktop::options().runDiagnostics()) \
             std::cout << (message) << std::endl;

using namespace core;

namespace desktop {

namespace {

void launchProcess(std::string absPath,
                   QStringList argList,
                   QProcess** ppProc)
{
   QProcess* pProcess = new QProcess();
   if (options().runDiagnostics())
      pProcess->setProcessChannelMode(QProcess::ForwardedChannels);
   else
      pProcess->setProcessChannelMode(QProcess::SeparateChannels);
   pProcess->start(QString::fromUtf8(absPath.c_str()), argList);
   *ppProc = pProcess;
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


Error SessionLauncher::launchFirstSession(const QString& filename,
                                          ApplicationLaunch* pAppLaunch)
{
   // save reference to app launch
   pAppLaunch_ = pAppLaunch;

   // build a new new launch context
   QString host, port;
   QStringList argList;
   QUrl url;
   buildLaunchContext(&host, &port, &argList, &url);

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

   // jcheng 03/16/2011: Due to crashing caused by authenticating
   // proxies, bypass all proxies from Qt until we can get the problem
   // completely solved. This is only expected to affect CRAN mirror
   // selection (which falls back to local mirror list) and update
   // checking.
   //NetworkProxyFactory* pProxyFactory = new NetworkProxyFactory();
   //QNetworkProxyFactory::setApplicationProxyFactory(pProxyFactory);

   pMainWindow_ = new MainWindow(url);
   pMainWindow_->setSessionLauncher(this);
   pMainWindow_->setSessionProcess(pRSessionProcess_);
   pAppLaunch->setActivationWindow(pMainWindow_);

   desktop::options().restoreMainWindowBounds(pMainWindow_);

   RUN_DIAGNOSTICS_LOG("\nConnected to R session, attempting to initialize...\n");

   // one-time workbench intiailized hook for startup file association
   if (!filename.isNull() && !filename.isEmpty())
   {
      StringSlotBinder* filenameBinder = new StringSlotBinder(filename);
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


   // show the window (but don't if we are doing a --run-diagnostics)
   if (!options().runDiagnostics())
   {
      pMainWindow_->show();
      pAppLaunch->activateWindow();
      pMainWindow_->loadUrl(url);
   }

   return Success();
}

void SessionLauncher::closeAllSatillites()
{
   QWidgetList topLevels = QApplication::topLevelWidgets();
   for (int i = 0; i < topLevels.size(); i++)
   {
      QWidget* pWindow = topLevels.at(i);
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
      closeAllSatillites();

      pMainWindow_->evaluateJavaScript(
               QString::fromAscii("window.desktopHooks.notifyRCrashed()"));

      if (abendLogPath().exists())
      {
         showMessageBox(QMessageBox::Critical,
                        pMainWindow_,
                        QString::fromUtf8("RStudio"),
                        launchFailedErrorMessage());
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
      // close all satellite windows if we are reloading
      bool reload = (pendingQuit == PendingQuitRestartAndReload);
      if (reload)
         closeAllSatillites();

      // launch next session
      Error error = launchNextSession(reload);
      if (error)
      {
         LOG_ERROR(error);

         showMessageBox(QMessageBox::Critical,
                        pMainWindow_,
                        QString::fromUtf8("RStudio"),
                        launchFailedErrorMessage());

         pMainWindow_->quit();
      }
   }
}

Error SessionLauncher::launchNextSession(bool reload)
{
   // disconnect the firstWorkbenchInitialized event so it doesn't occur
   // again when we launch the next session
   pMainWindow_->disconnect(SIGNAL(firstWorkbenchInitialized()));

   // delete the old process object
   pMainWindow_->setSessionProcess(NULL);
   if (pRSessionProcess_)
   {
      delete pRSessionProcess_;
      pRSessionProcess_ = NULL;
   }

   // build a new launch context -- re-use the same port if we aren't reloading
   QString port = !reload ? options().portNumber() : QString::fromAscii("");
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
      // load url -- use a delay because on occation we've seen the
      // mac client crash during switching of projects and this could
      // be some type of timing related issue
      nextSessionUrl_ = url;
      QTimer::singleShot(100, this, SLOT(onReloadFrameForNextSession()));
   }

   return Success();
}

void SessionLauncher::onReloadFrameForNextSession()
{
   pMainWindow_->loadUrl(nextSessionUrl_);
   nextSessionUrl_.clear();
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
      errMsg.append(QString::fromAscii("\n\n").append(abendLogMessage));

   // check for stderr
   if (pRSessionProcess_)
   {
      QString errmsgs = QString::fromLocal8Bit(
                              pRSessionProcess_->readAllStandardError());
      if (errmsgs.size())
      {
         errMsg = errMsg.append(
                           QString::fromAscii("\n\n")).append(errmsgs);
      }
   }

   return errMsg;
}


void SessionLauncher::cleanupAtExit()
{
   if (pMainWindow_)
      desktop::options().saveMainWindowBounds(pMainWindow_);
}


void SessionLauncher::buildLaunchContext(QString* pHost,
                                         QString* pPort,
                                         QStringList* pArgList,
                                         QUrl* pUrl) const
{
   *pHost = QString::fromAscii("127.0.0.1");
   if (pPort->isEmpty())
      *pPort = desktop::options().newPortNumber();
   *pUrl = QUrl(QString::fromAscii("http://") + *pHost +
                QString::fromAscii(":") + *pPort + QString::fromAscii("/"));

   if (!confPath_.empty())
   {
      *pArgList << QString::fromAscii("--config-file") <<
                   QString::fromUtf8(confPath_.absolutePath().c_str());
   }
   else
   {
      // explicitly pass "none" so that rsession doesn't read an
      // /etc/rstudio/rsession.conf file which may be sitting around
      // from a previous configuratin or install
      *pArgList << QString::fromAscii("--config-file") <<
                   QString::fromAscii("none");
   }

   *pArgList << QString::fromAscii("--program-mode") <<
                QString::fromAscii("desktop");

   *pArgList << QString::fromAscii("--www-port") << *pPort;

   if (options().runDiagnostics())
      *pArgList << QString::fromAscii("--verify-installation") <<
                   QString::fromAscii("1");
}


} // namespace desktop
