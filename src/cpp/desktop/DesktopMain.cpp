/*
 * DesktopMain.cpp
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

#include <QtGui>
#include <QDebug>
#include <QPushButton>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

#include <core/Log.hpp>

#include <core/Version.hpp>
#include <core/system/FileScanner.hpp>
#include <core/SafeConvert.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RUserData.hpp>

#include "DesktopApplicationLaunch.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopDetectRHome.hpp"
#include "DesktopUtils.hpp"
#include "DesktopSessionLauncher.hpp"
#include "DesktopProgressActivator.hpp"
#include "DesktopNetworkProxyFactory.hpp"
#include "DesktopActivationOverlay.hpp"

QProcess* pRSessionProcess;
QString sharedSecret;

using namespace rstudio;
using namespace rstudio::core;
using namespace rstudio::desktop;

namespace {

void augmentCommandLineArguments(std::vector<char*>* arguments)
{
   std::string user = core::system::getenv("RSTUDIO_CHROMIUM_ARGUMENTS");
   if (user.empty())
      return;
   
   std::vector<std::string> pieces = core::algorithm::split(user, " ");
   for (auto& piece : pieces)
   {
      // NOTE: we intentionally leak the memory here just so the command
      // line arguments can persist through lifetime of application
      char* argument = (char*) ::malloc(piece.size() + 1);
      ::memcpy(argument, piece.c_str(), piece.size() + 1);
      arguments->push_back(argument);
   }
}

// attempt to remove stale lockfiles that might inhibit
// RStudio startup (currently Windows only). returns
// an error only when a stale lockfile exists, but
// we could not successfully remove it
Error removeStaleOptionsLockfile()
{
#ifndef Q_OS_WIN32
   return Success();
#else
   std::string appData = core::system::getenv("APPDATA");
   if (appData.empty())
      return Success();

   FilePath appDataPath(appData);
   if (!appDataPath.exists())
      return Success();

   FilePath lockFilePath = appDataPath.childPath("RStudio/desktop.ini.lock");
   if (!lockFilePath.exists())
      return Success();

   double diff = ::difftime(::time(NULL), lockFilePath.lastWriteTime());
   if (diff < 10)
      return Success();

   return lockFilePath.remove();
#endif
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
   // bail if we've already got a working directory as a result of
   // a call to openSessionInNewWindow
   if (!core::system::getenv(kRStudioInitialWorkingDir).empty())
      return;

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
      Error error = core::system::executablePath(argv[0], &exePath);
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
      if (core::system::stdoutIsTerminal() &&
         (currentPath != core::system::userHomePath()))
      {
         workingDir = currentPath.absolutePath();
      }

#endif

   }

   // set the working dir if we have one
   if (!workingDir.empty())
      core::system::setenv(kRStudioInitialWorkingDir, workingDir);
}

void setInitialProject(const FilePath& projectFile, QString* pFilename)
{
   core::system::setenv(kRStudioInitialProject, projectFile.absolutePath());
   pFilename->clear();
}

void initializeStartupEnvironment(QString* pFilename)
{
   // if the filename ends with .RData or .rda then this is an
   // environment file. if it ends with .Rproj then it is
   // a project file. we handle both cases by setting an environment
   // var and then resetting the pFilename so it isn't processed
   // using the standard open file logic
   FilePath filePath(pFilename->toUtf8().constData());
   if (filePath.exists())
   {
      std::string ext = filePath.extensionLowerCase();

      // if it is a directory or just an .rdata file then we can see
      // whether there is a project file we can automatically attach to
      if (filePath.isDirectory())
      {
         FilePath projectFile = r_util::projectFromDirectory(filePath);
         if (!projectFile.empty())
         {
            setInitialProject(projectFile, pFilename);
         }
      }
      else if (ext == ".rproj")
      {
         setInitialProject(filePath, pFilename);
      }
      else if (ext == ".rdata" || ext == ".rda")
      {
         core::system::setenv(kRStudioInitialEnvironment, filePath.absolutePath());
         pFilename->clear();
      }

   }
}

QString verifyAndNormalizeFilename(const QString &filename)
{
   if (filename.isNull() || filename.isEmpty())
      return QString();

   QFileInfo fileInfo(filename);
   if (fileInfo.exists())
      return fileInfo.absoluteFilePath();
   else
      return QString();
}

bool isNonProjectFilename(const QString &filename)
{
   if (filename.isNull() || filename.isEmpty())
      return false;

   FilePath filePath(filename.toUtf8().constData());
   return filePath.exists() && filePath.extensionLowerCase() != ".rproj";
}

bool useChromiumDevtools()
{
   // disable by default due to security concerns
   // https://bugreports.qt.io/browse/QTBUG-50725
   bool useDevtools = false;

#ifndef NDEBUG
   // but enable by default for development builds
   useDevtools = true;
#endif

   // enable when environment variable is set
   if (!core::system::getenv("RSTUDIO_USE_CHROMIUM_DEVTOOLS").empty())
   {
      useDevtools = true;
   }

   return useDevtools;
}

} // anonymous namespace

int main(int argc, char* argv[])
{
   core::system::initHook();

   try
   {
      initializeLang();
      
      if (useChromiumDevtools())
      {
         // use QTcpSocket to find an open port. this is unfortunately a bit racey
         // but AFAICS there isn't a better solution for port selection
         QByteArray port;
         QTcpSocket* pSocket = new QTcpSocket();
         if (pSocket->bind())
         {
            quint16 port = pSocket->localPort();
            desktopInfo().setChromiumDevtoolsPort(port);
            core::system::setenv("QTWEBENGINE_REMOTE_DEBUGGING", safe_convert::numberToString(port));
            pSocket->close();
         }
      }

      // initialize log
      core::system::initializeLog("rdesktop",
                                  core::system::kLogLevelWarning,
                                  desktop::userLogPath());

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // attempt to remove stale lockfiles, as they can impede
      // application startup
      error = removeStaleOptionsLockfile();
      if (error)
         LOG_ERROR(error);

      // set application attributes
      QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
      
      // prepare command line arguments
      static std::vector<char*> arguments(argv, argv + argc);
      
      // enable viewport meta (allows us to control / restrict
      // certain touch gestures)
      static char enableViewport[] = "--enable-viewport";
      arguments.push_back(enableViewport);
      
#ifndef NDEBUG
      // disable web security for development builds (so we can
      // get access to sourcemaps)
      static char disableWebSecurity[] = "--disable-web-security";
      arguments.push_back(disableWebSecurity);
#endif
      
      // disable chromium renderer accessibility by default (it can cause
      // slowdown when used in conjunction with some applications; see e.g.
      // https://github.com/rstudio/rstudio/issues/1990)
      bool accessibility = desktop::options().enableAccessibility();

      if (!accessibility && core::system::getenv("RSTUDIO_ACCESSIBILITY").empty())
      {
         // only disable if (a) pref indicates we should, and (b) override env var is not set
         static char disableRendererAccessibility[] = "--disable-renderer-accessibility";
         arguments.push_back(disableRendererAccessibility);
      }

#ifdef Q_OS_MAC
      // don't prefer compositing to LCD text rendering. when enabled, this causes the compositor to
      // be used too aggressively on Retina displays on macOS, with the side effect that the
      // scrollbar doesn't auto-hide because a compositor layer is present.
      // https://github.com/rstudio/rstudio/issues/1953
      static char disableCompositorPref[] = "--disable-prefer-compositing-to-lcd-text";
      arguments.push_back(disableCompositorPref);
      
      // disable gpu rasterization for certain display configurations.
      // this works around some of the rendering issues seen with RStudio.
      //
      // https://bugs.chromium.org/p/chromium/issues/detail?id=773705
      // https://github.com/rstudio/rstudio/issues/2093
      //
      // because the issue seems to only affect certain video cards on macOS
      // High Sierra, we scope that change to that particular configuration
      // for now (we can expand this list if more users report issues)
      core::Version macVersion(QSysInfo::productVersion().toStdString());
      if (macVersion.versionMajor() == 10 &&
          macVersion.versionMinor() == 13)
      {
         core::system::ProcessResult processResult;
         core::system::runCommand(
                  "/usr/sbin/system_profiler SPDisplaysDataType",
                  core::system::ProcessOptions(),
                  &processResult);

         std::string stdOut = processResult.stdOut;
         if (!stdOut.empty())
         {
            std::vector<std::string> blackList = {
               "NVIDIA GeForce GT 650M",
               "NVIDIA GeForce GT 750M",
               "Intel Iris Graphics 6100"
            };

            for (const std::string& entry : blackList)
            {
               if (stdOut.find(entry) != std::string::npos)
               {
                  static char disableGpuRasterization[] = "--disable-gpu-rasterization";
                  arguments.push_back(disableGpuRasterization);
                  break;
               }
            }
         }
      }
#endif
      
      // allow users to supply extra command-line arguments
      augmentCommandLineArguments(&arguments);

      // re-assign command line arguments
      argc = (int) arguments.size();
      argv = &arguments[0];

      // prepare application for launch
      boost::scoped_ptr<QApplication> pApp;
      boost::scoped_ptr<ApplicationLaunch> pAppLaunch;
      ApplicationLaunch::init(QString::fromUtf8("RStudio"),
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
#endif
      // allow all platforms (including OSX) to check the command line.
      // we include OSX because the way Qt handles apple events is to
      // re-route them to the first instance to register for events. in
      // this case (for projects) we use this to initiate a launch
      // of the application with the project filename on the command line
      if (filename.isEmpty())
      {
         // get filename from command line arguments
         if (pApp->arguments().size() > 1)
         {
            QString arg = pApp->arguments().at(1);
            if (arg != QString::fromUtf8(kRunDiagnosticsOption))
               filename = verifyAndNormalizeFilename(arg);
         }
      }

      // if we have a filename and it is NOT a project file then see
      // if we can open it within an existing instance
      if (isNonProjectFilename(filename))
      {
         if (pAppLaunch->sendMessage(filename))
            return 0;
      }
      else
      {
         // try to register ourselves as a peer for others
         pAppLaunch->attemptToRegisterPeer();
      }

      // init options from command line
      desktop::options().initFromCommandLine(pApp->arguments());

      // reset log if we are in run-diagnostics mode
      if (desktop::options().runDiagnostics())
      {
         desktop::reattachConsoleIfNecessary();
         initializeStderrLog("rdesktop", core::system::kLogLevelWarning);
      }

      initializeSharedSecret();
      initializeWorkingDirectory(argc, argv, filename);
      initializeStartupEnvironment(&filename);

      Options& options = desktop::options();
      if (!prepareEnvironment(options))
         return 1;

      // get install path
      FilePath installPath;
      error = core::system::installPath("..", argv[0], &installPath);
      if (error)
      {
         LOG_ERROR(error);
         return EXIT_FAILURE;
      }

#ifdef _WIN32
      RVersion version = detectRVersion(false);
#endif

      // calculate paths to config file, rsession, and desktop scripts
      FilePath confPath, sessionPath, scriptsPath;
      bool devMode = false;

      // check for debug configuration
      FilePath currentPath = FilePath::safeCurrentPath(installPath);
      if (currentPath.complete("conf/rdesktop-dev.conf").exists())
      {
         confPath = currentPath.complete("conf/rdesktop-dev.conf");
         sessionPath = currentPath.complete("session/rsession");
         scriptsPath = currentPath.complete("desktop");
         devMode = true;
      }

      // if there is no conf path then release mode
      if (confPath.empty())
      {
         // default paths (then tweak)
         sessionPath = installPath.complete("bin/rsession");
         scriptsPath = installPath.complete("bin");

         // check for running in a bundle on OSX
#ifdef __APPLE__
         if (installPath.complete("Info.plist").exists())
         {
            sessionPath = installPath.complete("MacOS/rsession");
            scriptsPath = installPath.complete("MacOS");
         }
#endif
      }
      core::system::fixupExecutablePath(&sessionPath);

      auto* pProxyFactory = new NetworkProxyFactory();
      QNetworkProxyFactory::setApplicationProxyFactory(pProxyFactory);

      // set the scripts path in options
      desktop::options().setScriptsPath(scriptsPath);

      // launch session
      SessionLauncher sessionLauncher(sessionPath, confPath, filename, pAppLaunch.get());
      sessionLauncher.launchFirstSession(installPath, devMode, pApp->arguments());

      ProgressActivator progressActivator;

      int result = pApp->exec();

      desktop::activation().releaseLicense();
      options.cleanUpScratchTempDir();

      return result;
   }
   CATCH_UNEXPECTED_EXCEPTION
}

#ifdef _WIN32
int WINAPI WinMain(HINSTANCE hInstance,
                   HINSTANCE hPrevInstance,
                   LPSTR lpCmdLine,
                   int nShowCmd)
{
   return main(__argc, __argv);
}
#endif
