/*
 * DesktopMain.cpp
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

#include <QtGui>
#include <QProcess>
#include <QtWebKit>
#include <QtNetwork/QTcpSocket>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/SafeConvert.hpp>
#include <core/WaitUtils.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/system/System.hpp>

#include "DesktopApplicationLaunch.hpp"
#include "DesktopMainWindow.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopDetectRHome.hpp"
#include "DesktopOptions.hpp"
#include "DesktopNetworkProxyFactory.hpp"

QProcess* pRSessionProcess;
QString sharedSecret;

using namespace core;
using namespace desktop;

namespace {

core::WaitResult serverReady(QString host, QString port)
{
   QTcpSocket socket;
   socket.connectToHost(host, port.toInt());
   return WaitResult(socket.waitForConnected() ? WaitSuccess : WaitContinue,
                     Success());
}

void initializeSharedSecret()
{
   sharedSecret = QString::number(rand())
                  + QString::number(rand())
                  + QString::number(rand());
   std::string value = sharedSecret.toStdString();
   core::system::setenv("RS_SHARED_SECRET", value);
}

void launchProcess(std::string absPath, QStringList argList, QProcess** ppProc)
{
   QProcess* pProcess = new QProcess();
   pProcess->setProcessChannelMode(QProcess::SeparateChannels);
   pProcess->start(absPath.c_str(), argList);
   *ppProc = pProcess;
}

QString verifyAndNormalizeFilename(QString filename)
{
   if (filename.isNull() || filename.isEmpty())
      return QString();

   QFileInfo fileInfo(filename);
   if (fileInfo.exists() && fileInfo.isFile())
      return fileInfo.absoluteFilePath();
   else
      return QString();
}

} // anonymous namespace

int main(int argc, char* argv[])
{
   core::system::initHook();

   try
   {
      // initialize log
      FilePath userHomePath = core::system::userHomePath("R_USER");
      FilePath logPath = core::system::userSettingsPath(
            userHomePath,
            "RStudio-Desktop").childPath("log");
      core::system::initializeLog("rdesktop",
                                  core::system::kLogLevelWarning,
                                  logPath);


      boost::scoped_ptr<QApplication> pApp;
      boost::scoped_ptr<ApplicationLaunch> pAppLaunch;
      ApplicationLaunch::init("RStudio",
                              argc,
                              argv,
                              &pApp,
                              &pAppLaunch);

      QString filename;
      if (pApp->arguments().size() > 1)
      {
         filename = verifyAndNormalizeFilename(pApp->arguments().last());
      }

      if (pAppLaunch->sendMessage(filename))
         return 0;

      pApp->setAttribute(Qt::AA_MacDontSwapCtrlAndMeta);

      initializeSharedSecret();

      Options& options = desktop::options();
      if (!prepareEnvironment(options))
         return 1;

      // get install path
      FilePath installPath;
      Error error = core::system::installPath("..", argc, argv, &installPath);
      if (error)
      {
         LOG_ERROR(error);
         return EXIT_FAILURE;
      }

#ifdef _WIN32
      RVersion version = detectRVersion(false);
#endif

      // calculate paths to config file and rsession
      FilePath confPath, sessionPath;
      confPath = installPath.complete("conf/rdesktop-dev.conf");
      if (confPath.exists()) // dev/debug configuration
      {
         sessionPath = installPath.complete("session/rsession");
#ifdef _WIN32
         if (version.architecture() == ArchX64)
            sessionPath = installPath.complete("x64/rsession");
#endif
      }
      else // release configuration
      {
         // no config file in release mode
         confPath = FilePath();

         // default session path (then tweak)
         sessionPath = installPath.complete("bin/rsession");

         // check for win64 binary on windows
#ifdef _WIN32
         if (version.architecture() == ArchX64)
            sessionPath = installPath.complete("bin/x64/rsession");
#endif

         // check for running in a bundle on OSX
#ifdef __APPLE__
         if (installPath.complete("Info.plist").exists())
            sessionPath = installPath.complete("MacOS/rsession");
#endif
      }
      core::system::fixupExecutablePath(&sessionPath);

      QString host("127.0.0.1");
      QString port = options.portNumber();
      QUrl url("http://" + host + ":" + port + "/");

      QStringList argList;
      if (!confPath.empty())
      {
          argList << "--config-file" << confPath.absolutePath().c_str();
       }
      else
      {
          // explicitly pass "none" so that rsession doesn't read an
          // /etc/rstudio/rsession.conf file which may be sitting around
          // from a previous configuratin or install
          argList << "--config-file" << "none";
       }

      argList << "--program-mode" << "desktop";

      argList << "--www-port" << port;

      if (!options.defaultCRANmirrorURL().isEmpty())
         argList << "--r-cran-repos" << options.defaultCRANmirrorURL();

      switch (options.saveWorkspaceOnExit())
      {
      case SAVE_YES:
         argList << "--r-save-workspace" << "yes";
         break;
      case SAVE_NO:
         argList << "--r-save-workspace" << "no";
         break;
      }

      error = parent_process_monitor::wrapFork(
            boost::bind(launchProcess,
                        sessionPath.absolutePath(),
                        argList,
                        &pRSessionProcess));
      if (error)
      {
         LOG_ERROR(error);
         return EXIT_FAILURE;
      }

      NetworkProxyFactory proxyFactory;
      QNetworkProxyFactory::setApplicationProxyFactory(&proxyFactory);

      MainWindow* browser = new MainWindow(url);
      pAppLaunch->setActivationWindow(browser);

      options.restoreMainWindowBounds(browser);

      error = waitWithTimeout(boost::bind(serverReady, host, port),
                              50, 25, 10);
      int result;
      if (!error)
      {
         if (!filename.isNull() && !filename.isEmpty())
         {
            StringSlotBinder* filenameBinder = new StringSlotBinder(filename);
            browser->connect(browser, SIGNAL(workbenchInitialized()),
                             filenameBinder, SLOT(trigger()));
            browser->connect(filenameBinder, SIGNAL(triggered(QString)),
                             browser, SLOT(openFileInRStudio(QString)));
         }

         browser->connect(pAppLaunch.get(), SIGNAL(openFileRequest(QString)),
                          browser, SLOT(openFileInRStudio(QString)));

         browser->connect(pRSessionProcess, SIGNAL(finished(int,QProcess::ExitStatus)),
                          browser, SLOT(quit()));

         browser->show();
         browser->loadUrl(url);

         result = pApp->exec();

         options.saveMainWindowBounds(browser);
         options.cleanUpScratchTempDir();
      }
      else
      {
         QString errorMessage = "The R session failed to start.";

         // These calls to processEvents() seem to be necessary to get
         // readAllStandardError to work.
         pApp->processEvents();
         pApp->processEvents();
         pApp->processEvents();

         if (pRSessionProcess)
         {
            QString errmsgs = (pRSessionProcess->readAllStandardError());
            if (errmsgs.size())
            {
               errorMessage = errorMessage.append("\n\n").append(errmsgs);
            }
         }

         result = boost::system::errc::timed_out;
         QMessageBox errorMsg(QMessageBox::Critical,
                              "RStudio",
                              errorMessage,
                              QMessageBox::Ok);
         errorMsg.show();
         pApp->exec();
      }

      //pRSessionProcess->kill();
      //pRSessionProcess->waitForFinished(1000);

      return result;
   }
   CATCH_UNEXPECTED_EXCEPTION
}
