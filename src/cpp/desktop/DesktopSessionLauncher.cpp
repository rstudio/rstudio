/*
 * DesktopSessionLauncher.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopSessionLauncher.hpp"

#include <boost/bind.hpp>

#include <core/WaitUtils.hpp>
#include <core/system/ParentProcessMonitor.hpp>

#include <QProcess>
#include <QtNetwork/QTcpSocket>

#include "DesktopUtils.hpp"
#include "DesktopOptions.hpp"
#include "DesktopSlotBinders.hpp"

using namespace core;

namespace desktop {

namespace {

void launchProcess(std::string absPath,
                   QStringList argList,
                   QProcess** ppProc)
{
   QProcess* pProcess = new QProcess();
   pProcess->setProcessChannelMode(QProcess::SeparateChannels);
   pProcess->start(QString::fromUtf8(absPath.c_str()), argList);
   *ppProc = pProcess;
}


core::WaitResult serverReady(QString host, QString port)
{
   QTcpSocket socket;
   socket.connectToHost(host, port.toInt());
   return WaitResult(socket.waitForConnected() ? WaitSuccess : WaitContinue,
                     Success());
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

   // launch the process
   Error error = launchSession(argList, &pRSessionProcess_);
   if (error)
     return error;

   // jcheng 03/16/2011: Due to crashing caused by authenticating
   // proxies, bypass all proxies from Qt until we can get the problem
   // completely solved. This is only expected to affect CRAN mirror
   // selection (which falls back to local mirror list) and update
   // checking.
   //NetworkProxyFactory* pProxyFactory = new NetworkProxyFactory();
   //QNetworkProxyFactory::setApplicationProxyFactory(pProxyFactory);

   pMainWindow_ = new MainWindow(url);
   pMainWindow_->setSessionProcess(pRSessionProcess_);
   pAppLaunch->setActivationWindow(pMainWindow_);

   desktop::options().restoreMainWindowBounds(pMainWindow_);

   error = waitForSession(host, port);
   if (error)
      return error;

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
                         this, SLOT(onRSessionExited()));



   pMainWindow_->show();
   pAppLaunch->activateWindow();
   pMainWindow_->loadUrl(url);

   return Success();
}


void SessionLauncher::onRSessionExited()
{
   if (pMainWindow_->collectPendingSwitchToProjectRequest())
   {
      // close all satellite windows
      QWidgetList topLevels = QApplication::topLevelWidgets();
      for (int i = 0; i < topLevels.size(); i++)
      {
         QWidget* pWindow = topLevels.at(i);
         if (pWindow != pMainWindow_)
           pWindow->close();
      }

      // launch next session
      Error error = launchNextSession();
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
   else
   {
      pMainWindow_->quit();
   }
}

Error SessionLauncher::launchNextSession()
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

   // build a new new launch context
   QString host, port;
   QStringList argList;
   QUrl url;
   buildLaunchContext(&host, &port, &argList, &url);

   // launch the process
   Error error = launchSession(argList, &pRSessionProcess_);
   if (error)
     return error;

   // update the main window's reference to the process object
   pMainWindow_->setSessionProcess(pRSessionProcess_);

   // wait for it to be available
   error = waitForSession(host, port);
   if (error)
      return error;

   // connect to quit event
   pMainWindow_->connect(pRSessionProcess_,
                         SIGNAL(finished(int,QProcess::ExitStatus)),
                         this, SLOT(onRSessionExited()));

   // laod url -- use a delay because on occation we've seen the
   // mac client crash during switching of projects and this could
   // be some type of timing related issue
   nextSessionUrl_ = url;
   QTimer::singleShot(100, this, SLOT(onReloadFrameForNextSession()));

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
   return  parent_process_monitor::wrapFork(
         boost::bind(launchProcess,
                     sessionPath_.absolutePath(),
                     argList,
                     ppRSessionProcess));
}

Error SessionLauncher::waitForSession(const QString& host,
                                      const QString& port)
{
   return waitWithTimeout(boost::bind(serverReady, host, port),
                          50, 25, 10);
}


QString SessionLauncher::launchFailedErrorMessage() const
{
   QString errMsg = QString::fromUtf8("The R session failed to start.");

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
}


} // namespace desktop
