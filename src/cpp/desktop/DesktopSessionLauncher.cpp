/*
 * DesktopSessionLauncher.cpp
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

#include "DesktopSessionLauncher.hpp"

#include <boost/bind.hpp>

#include <core/WaitUtils.hpp>
#include <core/system/ParentProcessMonitor.hpp>

#include <QProcess>
#include <QtNetwork/QTcpSocket>

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
   // build a new new launch context
   QString host, port;
   QStringList argList;
   QUrl url;
   buildLaunchContext(&host, &port, &argList, &url);

   // launch the process
   Error error = parent_process_monitor::wrapFork(
         boost::bind(launchProcess,
                     sessionPath_.absolutePath(),
                     argList,
                     &pRSessionProcess_));
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

   error = waitWithTimeout(boost::bind(serverReady, host, port),
                           50, 25, 10);
   if (error)
      return error;

   if (!filename.isNull() && !filename.isEmpty())
   {
      StringSlotBinder* filenameBinder = new StringSlotBinder(filename);
      pMainWindow_->connect(pMainWindow_,
                            SIGNAL(workbenchInitialized()),
                            filenameBinder,
                            SLOT(trigger()));
      pMainWindow_->connect(filenameBinder,
                            SIGNAL(triggered(QString)),
                            pMainWindow_,
                            SLOT(openFileInRStudio(QString)));
   }

   pMainWindow_->connect(pAppLaunch,
                         SIGNAL(openFileRequest(QString)),
                         pMainWindow_,
                         SLOT(openFileInRStudio(QString)));

   pMainWindow_->connect(pRSessionProcess_,
                         SIGNAL(finished(int,QProcess::ExitStatus)),
                         pMainWindow_, SLOT(quit()));



   pMainWindow_->show();
   pMainWindow_->loadUrl(url);

   return Success();
}


QString SessionLauncher::readFailedLaunchStandardError() const
{
   QString processStderr;
   if (pRSessionProcess_)
   {
      QString errmsgs = QString::fromLocal8Bit(
                              pRSessionProcess_->readAllStandardError());
      if (errmsgs.size())
      {
         processStderr = processStderr.append(
                           QString::fromAscii("\n\n")).append(errmsgs);
      }
   }
   return processStderr;
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
