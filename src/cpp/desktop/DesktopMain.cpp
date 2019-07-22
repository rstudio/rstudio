/*
 * DesktopMain.cpp
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

#include <QtGui>
#include <QDebug>
#include <QPushButton>
#include <QQuickWindow>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

#include <core/CrashHandler.hpp>
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
#include "RemoteDesktopSessionLauncherOverlay.hpp"
#include "DesktopProgressActivator.hpp"
#include "DesktopNetworkProxyFactory.hpp"
#include "DesktopActivationOverlay.hpp"
#include "DesktopSessionServersOverlay.hpp"

#ifdef _WIN32
#include <core/system/RegistryKey.hpp>
#include <Windows.h>
#endif

#ifdef Q_OS_LINUX
#include <core/system/PosixSystem.hpp>
#endif

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

   double diff = ::difftime(::time(nullptr), lockFilePath.lastWriteTime());
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

bool useRemoteDevtoolsDebugging()
{
#if QT_VERSION >= QT_VERSION_CHECK(5, 11, 0)
   // don't need remote debugging for newer Qt
   return false;
#else
   
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
   
#endif
}

#ifdef Q_OS_MAC

QString inferDefaultRenderingEngine()
{
   return QStringLiteral("auto");
}

#endif

#ifdef Q_OS_WIN

namespace {

bool isRemoteSession()
{
   if (::GetSystemMetrics(SM_REMOTESESSION))
      return true;
   
   core::system::RegistryKey key;
   Error error = key.open(
            HKEY_LOCAL_MACHINE,
            "SYSTEM\\CurrentControlSet\\Control\\Terminal Server\\",
            KEY_READ);
   
   if (error)
      return false;
   
   DWORD dwGlassSessionId;
   DWORD cbGlassSessionId = sizeof(dwGlassSessionId);
   DWORD dwType;

   LONG lResult = RegQueryValueEx(
            key.handle(),
            "GlassSessionId",
            NULL, // lpReserved
            &dwType,
            (BYTE*) &dwGlassSessionId,
            &cbGlassSessionId);

   if (lResult != ERROR_SUCCESS)
      return false;
   
   DWORD dwCurrentSessionId;
   if (ProcessIdToSessionId(GetCurrentProcessId(), &dwCurrentSessionId))
      return dwCurrentSessionId != dwGlassSessionId;
   
   return false;
   
}

} // end anonymous namespace

QString inferDefaultRenderingEngine()
{
   if (isRemoteSession())
      return QStringLiteral("software");

   // prefer software rendering for certain graphics cards
   std::vector<std::string> blacklist = {
      "Intel(R) HD Graphics 520",
      "Intel(R) HD Graphics 530",
      "Intel(R) HD Graphics 620",
      "Intel(R) HD Graphics 630",
   };

   DISPLAY_DEVICE device;
   device.cb = sizeof(DISPLAY_DEVICE);

   DWORD i = 0;
   while (::EnumDisplayDevices(nullptr, i++, &device, 0))
   {
      // skip non-primary device
      if ((device.StateFlags & DISPLAY_DEVICE_PRIMARY_DEVICE) == 0)
         continue;

      // check for unsupported device
      std::string deviceString(device.DeviceString);
      for (auto&& item : blacklist)
      {
         if (deviceString.find(item) != std::string::npos)
         {
            QCoreApplication::setAttribute(Qt::AA_DisableShaderDiskCache, true);
            return QStringLiteral("software");
         }
      }
   }

   return QStringLiteral("auto");
}

#endif

#ifdef Q_OS_LINUX

QString inferDefaultRenderingEngine()
{
   return QStringLiteral("auto");
}

#endif

void initializeRenderingEngine(std::vector<char*>* pArguments)
{
   QString engine = desktop::options().desktopRenderingEngine();

   if (engine.isEmpty() || engine == QStringLiteral("auto"))
   {
      engine = inferDefaultRenderingEngine();
   }

   if (engine == QStringLiteral("desktop"))
   {
      QCoreApplication::setAttribute(Qt::AA_UseDesktopOpenGL);
      QQuickWindow::setSceneGraphBackend(QSGRendererInterface::OpenGL);
   }
   else if (engine == QStringLiteral("gles"))
   {
      QCoreApplication::setAttribute(Qt::AA_UseOpenGLES);
      QQuickWindow::setSceneGraphBackend(QSGRendererInterface::OpenGL);
   }
   else if (engine == QStringLiteral("software"))
   {
      QCoreApplication::setAttribute(Qt::AA_UseSoftwareOpenGL);
      QQuickWindow::setSceneGraphBackend(QSGRendererInterface::Software);
      
      // allow WebGL rendering with the software renderer
      static char enableWebglSoftwareRendering[] = "--enable-webgl-software-rendering";
      pArguments->push_back(enableWebglSoftwareRendering);
   }
   
   // tell Chromium to ignore the GPU blacklist if requested
   bool ignore = desktop::options().ignoreGpuBlacklist();
   if (ignore)
   {
      static char ignoreGpuBlacklist[] = "--ignore-gpu-blacklist";
      pArguments->push_back(ignoreGpuBlacklist);
   }
   
   // also disable driver workarounds if requested
   bool disable = desktop::options().disableGpuDriverBugWorkarounds();
   if (disable)
   {
      static char disableGpuDriverBugWorkarounds[] = "--disable-gpu-driver-bug-workarounds";
      pArguments->push_back(disableGpuDriverBugWorkarounds);
   }
}

boost::optional<SessionServer> getLaunchServerFromUrl(const std::string& url)
{
   auto iter = std::find_if(sessionServerSettings().servers().begin(),
                            sessionServerSettings().servers().end(),
                            [&](const SessionServer& server) { return server.url() == url; });

   if (iter != sessionServerSettings().servers().end())
   {
      SessionServer server = *iter;
      return boost::optional<SessionServer>(server);
   }

   return boost::optional<SessionServer>();
}

} // anonymous namespace

int main(int argc, char* argv[])
{
   core::system::initHook();

   try
   {
      static std::vector<char*> arguments(argv, argv + argc);

#ifndef RSTUDIO_PACKAGE_BUILD
      // find the build directory root
      QDir dir = QDir::current();
      do
      {
         QString path = dir.filePath(QStringLiteral("CMakeCache.txt"));
         if (QFile(path).exists())
         {
            QDir::setCurrent(dir.path());
            break;
         }

      } while (dir.cdUp());
#endif
      
      initializeLang();
      initializeRenderingEngine(&arguments);
      
      if (useRemoteDevtoolsDebugging())
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

      // catch unhandled exceptions
      error = core::crash_handler::initialize(core::crash_handler::ProgramMode::Desktop);
      if (error)
         LOG_ERROR(error);

      // attempt to remove stale lockfiles, as they can impede
      // application startup
      error = removeStaleOptionsLockfile();
      if (error)
         LOG_ERROR(error);

      // set application attributes
      QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);

      // don't synthesize mouse events for unhandled tablet events,
      // as this causes tablet clicks to be duplicated (effectively
      // leading to single clicks registering as double clicks)
      //
      // https://bugreports.qt.io/browse/QTBUG-76347
      QCoreApplication::setAttribute(Qt::AA_SynthesizeMouseForUnhandledTabletEvents, false);
      
      // enable viewport meta (allows us to control / restrict
      // certain touch gestures)
      static char enableViewport[] = "--enable-viewport";
      arguments.push_back(enableViewport);
      
#if QT_VERSION < QT_VERSION_CHECK(5, 11, 0)
      
#ifndef NDEBUG
      // disable web security for development builds (so we can
      // get access to sourcemaps)
      static char disableWebSecurity[] = "--disable-web-security";
      arguments.push_back(disableWebSecurity);
#endif
      
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
      
      // disable GPU features for certain machine configurations. see e.g.
      //
      // https://bugs.chromium.org/p/chromium/issues/detail?id=773705
      // https://github.com/rstudio/rstudio/issues/2093
      // https://github.com/rstudio/rstudio/issues/3148
      //
      // because the issue seems to only affect certain video cards on macOS
      // High Sierra, we scope that change to that particular configuration
      // for now (we can expand this list if more users report issues)
      {
         core::system::ProcessResult processResult;
         core::system::runCommand(
                  "/usr/sbin/system_profiler SPDisplaysDataType",
                  core::system::ProcessOptions(),
                  &processResult);

         std::string stdOut = processResult.stdOut;
         if (!stdOut.empty())
         {
            // NOTE: temporarily backed out as it appears the rasterization
            // issues do not occur anymore with Qt 5.12.1; re-enable if we
            // receive more reports in the wild.
            //
            // https://github.com/rstudio/rstudio/issues/2176
            
            /*
            std::vector<std::string> rasterBlacklist = {
               "NVIDIA GeForce GT 650M",
               "NVIDIA GeForce GT 750M",
               "Intel Iris Graphics 6100"
            };

            for (const std::string& entry : rasterBlacklist)
            {
               if (stdOut.find(entry) != std::string::npos)
               {
                  static char disableGpuRasterization[] = "--disable-gpu-rasterization";
                  arguments.push_back(disableGpuRasterization);
                  break;
               }
            }
            */
            
            std::vector<std::string> gpuBlacklist = {
#if QT_VERSION < QT_VERSION_CHECK(5, 12, 0)
               "AMD FirePro"
#endif
            };
            
            for (const std::string& entry : gpuBlacklist)
            {
               if (stdOut.find(entry) != std::string::npos)
               {
                  static char disableGpu[] = "--disable-gpu";
                  arguments.push_back(disableGpu);
                  break;
               }
            }
         }
      }
#endif

#if defined(Q_OS_LINUX) 

      static char noSandbox[] = "--no-sandbox";

#if (QT_VERSION == QT_VERSION_CHECK(5, 10, 1))
      // workaround for Qt 5.10.1 bug "Could not find QtWebEngineProcess"
      // https://bugreports.qt.io/browse/QTBUG-67023
      // https://bugreports.qt.io/browse/QTBUG-66346
      arguments.push_back(noSandbox);

#else
      // is this root? if so, we need --no-sandbox on Linux. 
      // see https://crbug.com/638180.
      if (core::system::effectiveUserIsRoot())
      {
         arguments.push_back(noSandbox);
      }
#endif

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
      // run an event loop for a short period of time just to ensure
      // that the OpenFile startup event (if any) gets pumped
      QEventLoop loop;
      QTimer::singleShot(100, &loop, &QEventLoop::quit);
      loop.exec();

      // grab the startup file request (if any)
      filename = verifyAndNormalizeFilename(pAppLaunch->startupOpenFileRequest());
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

      // get install path
      FilePath installPath;
      error = core::system::installPath("..", argv[0], &installPath);
      if (error)
      {
         LOG_ERROR(error);
         return EXIT_FAILURE;
      }

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

      // set the scripts path in options
      desktop::options().setScriptsPath(scriptsPath);

      Options& options = desktop::options();
      if (!prepareEnvironment(options))
         return 1;

#ifdef _WIN32
      RVersion version = detectRVersion(false);
      if (devMode)
      {
         if (version.architecture() == ArchX86 &&
             installPath.complete("session/x86").exists())
         {
            sessionPath = installPath.complete("session/x86/rsession");
         }
      }
      else
      {
         // check for win32 binary on windows
          if (version.architecture() == ArchX86 &&
             installPath.complete("bin/x86").exists())
         {
            sessionPath = installPath.complete("bin/x86/rsession");
         }
      }
#endif

      core::system::fixupExecutablePath(&sessionPath);

      auto* pProxyFactory = new NetworkProxyFactory();
      QNetworkProxyFactory::setApplicationProxyFactory(pProxyFactory);

      // determine where the session should be launched
      boost::optional<SessionServer> launchServer;
      bool forceSessionServerLaunch = false;
      if (!desktop::options().sessionServer().empty())
      {
         forceSessionServerLaunch = true;

         // launched with a specific session server selected
         // such as opening a new session in another window
         launchServer = getLaunchServerFromUrl(desktop::options().sessionServer());
         if (!launchServer)
         {
            // if we don't have an entry for the server URL, something is horribly wrong
            // just show an error and exit
            showError(nullptr,
                      QString::fromUtf8("Invalid session server"),
                      QString::fromStdString("Session server " + desktop::options().sessionServer() +
                                                " does not exist"),
                      QString::null);
            return EXIT_FAILURE;
         }
      }

      bool forceShowSessionLocationDialog = (qApp->queryKeyboardModifiers() & Qt::AltModifier);

      while (true)
      {
         SessionLocation location = forceShowSessionLocationDialog ?
                  SessionLocation::Ask :
                  sessionServerSettings().sessionLocation();

         if (!forceSessionServerLaunch)
         {
            switch (location)
            {
               case SessionLocation::Server:
                  if (sessionServerSettings().servers().size() != 0)
                  {
                     // remote launch on default server
                     auto iter = std::find_if(sessionServerSettings().servers().begin(),
                                              sessionServerSettings().servers().end(),
                                              [](const SessionServer& server) { return server.isDefault(); });

                     if (iter == sessionServerSettings().servers().end())
                     {
                        // somehow, no default was specified
                        // log an error and launch session on the first server in the list
                        launchServer = sessionServerSettings().servers().at(0);
                        LOG_ERROR_MESSAGE("No default session server specified. Using first server");
                     }
                     else
                        launchServer = *iter;
                  }
                  break;

               case SessionLocation::Ask:
                  if (sessionServerSettings().servers().size() != 0)
                  {
                     // display session location chooser dialog
                     LaunchLocationResult result = sessionServers().showSessionLaunchLocationDialog();
                     if (result.dialogResult == QDialog::Rejected)
                        return EXIT_SUCCESS;

                     launchServer = result.sessionServer;
                  }
                  break;

               case SessionLocation::Locally:
               default:
                  break;
            }
         }

         // keep the launcher object alive for the program's duration
         boost::shared_ptr<void> pSessionLauncher;

         if (!launchServer)
         {
            // launch a local session
            SessionLauncher* pLauncher = new SessionLauncher(sessionPath, confPath, filename, pAppLaunch.get());
            pLauncher->launchFirstSession(installPath, devMode, pApp->arguments());
            pSessionLauncher.reset(pLauncher);
         }
         else
         {
            // launch a remote session
            // first, check to make sure the server is reachable/valid
            Error error = launchServer->test();
            if (error)
            {
               bool continueConnecting =
                     showYesNoDialog(QMessageBox::Warning,
                                     nullptr,
                                     QString::fromUtf8("Server Error"),
                                     QString::fromUtf8("There was an error while attempting to run "
                                                       "prelaunch diagnostics for this server. "
                                                       "Do you want to attempt to connect anyway?"),
                                     QString::fromStdString(error.getProperty("description")),
                                     false);
               if (!continueConnecting)
               {
                  if (forceSessionServerLaunch)
                     return EXIT_FAILURE;

                  forceShowSessionLocationDialog = true;
                  continue;
               }
            }

            RemoteDesktopSessionLauncher* pLauncher;

            std::string sessionUrl = desktop::options().sessionUrl();
            if (sessionUrl.empty())
            {
               pLauncher = new RemoteDesktopSessionLauncher(launchServer.get(),
                                                            pAppLaunch.get(),
                                                            forceSessionServerLaunch);
            }
            else
            {
               pLauncher = new RemoteDesktopSessionLauncher(launchServer.get(),
                                                            pAppLaunch.get(),
                                                            sessionUrl);
            }

            pLauncher->launchFirstSession();
            pSessionLauncher.reset(pLauncher);
         }

         ProgressActivator progressActivator;

         int result = pApp->exec();

         desktop::activation().releaseLicense();
         options.cleanUpScratchTempDir();

         return result;
      }
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
