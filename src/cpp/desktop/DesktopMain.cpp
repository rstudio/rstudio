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
#include "DesktopUtils.hpp"

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
   std::string value = sharedSecret.toUtf8().constData();
   core::system::setenv("RS_SHARED_SECRET", value);
}

void initializeWorkingDirectory(int argc,
                                char* argv[],
                                const QString& filename)
{
   // calculate what our initial working directory should be
   std::string workingDir;

   // if there is a filename passed to us then use it's path
   if (filename != QString())
   {
      FilePath filePath(filename.toUtf8().constData());
      if (filePath.exists())
      {
         if (filePath.isDirectory())
            workingDir = filePath.absolutePath();
         else
            workingDir = filePath.parent().absolutePath();
      }
   }

   // do additinal detection if necessary
   if (workingDir.empty())
   {
      // get current path
      FilePath currentPath = FilePath::safeCurrentPath(
                                       core::system::userHomePath());

#if defined(_WIN32) || defined(__APPLE__)

      // detect whether we were launched from the system application menu
      // (e.g. Dock, Program File icon, etc.). we do this by checking
      // whether the executable path is within the current path. if we
      // weren't launched from the system app menu that set the initial
      // wd to the current path

      FilePath exePath;
      Error error = core::system::executablePath(argc, argv, &exePath);
      if (!error)
      {
         if (!exePath.isWithin(currentPath))
            workingDir = currentPath.absolutePath();
      }
      else
      {
         LOG_ERROR(error);
      }

#else

      // on linux we take the current working dir if we were launched
      // from within a terminal
      if (core::system::stdoutIsTerminal())
      {
         workingDir = currentPath.absolutePath();
      }

#endif

   }

   // set the working dir if we have one
   if (!workingDir.empty())
      core::system::setenv("RS_INITIAL_WD", workingDir);
}

void initializeStartupEnvironment(QString* pFilename)
{
   // if the filename ends with .RData or .rda then this is an
   // environment file. if it ends with .Rproject then it is
   // a project file. we handle both cases by setting an environment
   // var and then resetting the pFilename so it isn't processed
   // using the standard open file logic
   FilePath filePath(pFilename->toUtf8().constData());
   if (filePath.exists())
   {
      std::string ext = filePath.extensionLowerCase();
      if (ext == ".rdata" || ext == ".rda")
      {
         core::system::setenv("RS_INITIAL_ENV", filePath.absolutePath());
         pFilename->clear();
      }
      else if (ext == ".rproject")
      {
         core::system::setenv("RS_INITIAL_PROJECT", filePath.absolutePath());
         pFilename->clear();
      }
   }
}

void launchProcess(std::string absPath, QStringList argList, QProcess** ppProc)
{
   QProcess* pProcess = new QProcess();
   pProcess->setProcessChannelMode(QProcess::SeparateChannels);
   pProcess->start(QString::fromUtf8(absPath.c_str()), argList);
   *ppProc = pProcess;
}

QString verifyAndNormalizeFilename(QString filename)
{
   if (filename.isNull() || filename.isEmpty())
      return QString();

   QFileInfo fileInfo(filename);
   if (fileInfo.exists())
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
      QTextCodec::setCodecForCStrings(QTextCodec::codecForName("UTF-8"));

      // initialize log
      FilePath userHomePath = core::system::userHomePath("R_USER|HOME");
      FilePath logPath = core::system::userSettingsPath(
            userHomePath,
            "RStudio-Desktop").childPath("log");
      core::system::initializeLog("rdesktop",
                                  core::system::kLogLevelWarning,
                                  logPath);


      boost::scoped_ptr<QApplication> pApp;
      boost::scoped_ptr<ApplicationLaunch> pAppLaunch;
      ApplicationLaunch::init(QString::fromAscii("RStudio"),
                              argc,
                              argv,
                              &pApp,
                              &pAppLaunch);

      // determine the filename that was passed to us
      QString filename;
#ifdef __APPLE__
      // get filename from OpenFile apple-event (pump to ensure delivery)
      pApp->processEvents();
      filename = verifyAndNormalizeFilename(
                              pAppLaunch->startupOpenFileRequest());
#else
      // get filename from command line arguments
      if (pApp->arguments().size() > 1)
         filename = verifyAndNormalizeFilename(pApp->arguments().last());
#endif

      // try to activate existing instance...exit if we do
      if (pAppLaunch->sendMessage(filename))
         return 0;

      pApp->setAttribute(Qt::AA_MacDontSwapCtrlAndMeta);

      initializeSharedSecret();
      initializeWorkingDirectory(argc, argv, filename);
      initializeStartupEnvironment(&filename);

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

      // check for debug configuration
#ifndef NDEBUG
      FilePath currentPath = FilePath::safeCurrentPath(installPath);
      if (currentPath.complete("conf/rdesktop-dev.conf").exists())
      {
         confPath = currentPath.complete("conf/rdesktop-dev.conf");
         sessionPath = currentPath.complete("session/rsession");
#ifdef _WIN32
         if (version.architecture() == ArchX64)
            sessionPath = installPath.complete("x64/rsession");
#endif
      }
#endif

      // if there is no conf path then release mode
      if (confPath.empty())
      {
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

      QString host = QString::fromAscii("127.0.0.1");
      QString port = options.portNumber();
      QUrl url(QString::fromAscii("http://") + host + QString::fromAscii(":") + port + QString::fromAscii("/"));

      QStringList argList;
      if (!confPath.empty())
      {
         argList << QString::fromAscii("--config-file") << QString::fromUtf8(confPath.absolutePath().c_str());
       }
      else
      {
         // explicitly pass "none" so that rsession doesn't read an
         // /etc/rstudio/rsession.conf file which may be sitting around
         // from a previous configuratin or install
         argList << QString::fromAscii("--config-file") << QString::fromAscii("none");
      }

      argList << QString::fromAscii("--program-mode") << QString::fromAscii("desktop");

      argList << QString::fromAscii("--www-port") << port;

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

      // jcheng 03/16/2011: Due to crashing caused by authenticating
      // proxies, bypass all proxies from Qt until we can get the problem
      // completely solved. This is only expected to affect CRAN mirror
      // selection (which falls back to local mirror list) and update
      // checking.
      //NetworkProxyFactory* pProxyFactory = new NetworkProxyFactory();
      //QNetworkProxyFactory::setApplicationProxyFactory(pProxyFactory);

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
         QString errorMessage = QString::fromUtf8("The R session failed to start.");

         // These calls to processEvents() seem to be necessary to get
         // readAllStandardError to work.
         pApp->processEvents();
         pApp->processEvents();
         pApp->processEvents();

         if (pRSessionProcess)
         {
            QString errmsgs = QString::fromLocal8Bit(pRSessionProcess->readAllStandardError());
            if (errmsgs.size())
            {
               errorMessage = errorMessage.append(QString::fromAscii("\n\n")).append(errmsgs);
            }
         }

         result = boost::system::errc::timed_out;
         QMessageBox errorMsg(safeMessageBoxIcon(QMessageBox::Critical),
                              QString::fromUtf8("RStudio"),
                              errorMessage);
         errorMsg.addButton(new QPushButton(QString::fromUtf8("OK")), QMessageBox::AcceptRole);
         errorMsg.show();
         pApp->exec();
      }

      //pRSessionProcess->kill();
      //pRSessionProcess->waitForFinished(1000);

      return result;
   }
   CATCH_UNEXPECTED_EXCEPTION
}
