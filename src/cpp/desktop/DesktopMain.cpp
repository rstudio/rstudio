/*
 * DesktopMain.cpp
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

// TODO: open project in new window command

// TODO: open -a RStudio on the Mac (reboot / test on other system)

// TODO: periodic crashing when switching projects on ubuntu (enable cores)

#include <QtGui>
#include <QtWebKit>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

#include <core/Log.hpp>

#include <core/system/FileScanner.hpp>
#include <core/Error.hpp>
#include <core/FileInfo.hpp>
#include <core/FilePath.hpp>
#include <core/SafeConvert.hpp>
#include <core/StringUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/RProjectFile.hpp>

#include "DesktopApplicationLaunch.hpp"
#include "DesktopSlotBinders.hpp"
#include "DesktopDetectRHome.hpp"
#include "DesktopOptions.hpp"
#include "DesktopUtils.hpp"
#include "DesktopSessionLauncher.hpp"

QProcess* pRSessionProcess;
QString sharedSecret;

using namespace core;
using namespace desktop;

namespace {

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

void setInitialProject(const FilePath& projectFile, QString* pFilename)
{
   core::system::setenv("RS_INITIAL_PROJECT", projectFile.absolutePath());
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
         core::system::setenv("RS_INITIAL_ENV", filePath.absolutePath());
         pFilename->clear();
      }

   }
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

bool isNonProjectFilename(QString filename)
{
   if (filename.isNull() || filename.isEmpty())
      return false;

   FilePath filePath(filename.toUtf8().constData());
   return filePath.exists() && filePath.extensionLowerCase() != ".rproj";
}

} // anonymous namespace

int main(int argc, char* argv[])
{
   core::system::initHook();

   try
   {
      QTextCodec::setCodecForCStrings(QTextCodec::codecForName("UTF-8"));

      // initialize log
      core::system::initializeLog("rdesktop",
                                  core::system::kLogLevelWarning,
                                  desktop::userLogPath());

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

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
            QString arg = pApp->arguments().last();
            if (arg != QString::fromAscii(kVerifyInstallationOption))
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

      // reset log if we are in verify-installation mode
      if (desktop::options().verifyInstallation())
      {
         initializeStderrLog("rdesktop", core::system::kLogLevelWarning);
      }

      pApp->setAttribute(Qt::AA_MacDontSwapCtrlAndMeta);

      initializeSharedSecret();
      initializeWorkingDirectory(argc, argv, filename);
      initializeStartupEnvironment(&filename);

      Options& options = desktop::options();
      if (!prepareEnvironment(options))
         return 1;

      // get install path
      FilePath installPath;
      error = core::system::installPath("..", argc, argv, &installPath);
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

      // check for debug configuration
#ifndef NDEBUG
      FilePath currentPath = FilePath::safeCurrentPath(installPath);
      if (currentPath.complete("conf/rdesktop-dev.conf").exists())
      {
         confPath = currentPath.complete("conf/rdesktop-dev.conf");
         sessionPath = currentPath.complete("session/rsession");
         scriptsPath = currentPath.complete("desktop");
#ifdef _WIN32
         if (version.architecture() == ArchX64)
            sessionPath = installPath.complete("x64/rsession");
#endif
      }
#endif

      // if there is no conf path then release mode
      if (confPath.empty())
      {
         // default paths (then tweak)
         sessionPath = installPath.complete("bin/rsession");
         scriptsPath = installPath.complete("bin");

         // check for win64 binary on windows
#ifdef _WIN32
         if (version.architecture() == ArchX64)
            sessionPath = installPath.complete("bin/x64/rsession");
#endif

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

      // set the scripts path in options
      desktop::options().setScriptsPath(scriptsPath);

      // launch session
      SessionLauncher sessionLauncher(sessionPath, confPath);
      error = sessionLauncher.launchFirstSession(filename, pAppLaunch.get());
      if (!error)
      {
         int result = pApp->exec();

         sessionLauncher.cleanupAtExit();

         options.cleanUpScratchTempDir();

         return result;
      }
      else
      {
         LOG_ERROR(error);

         // These calls to processEvents() seem to be necessary to get
         // readAllStandardError to work.
         pApp->processEvents();
         pApp->processEvents();
         pApp->processEvents();

         QMessageBox errorMsg(safeMessageBoxIcon(QMessageBox::Critical),
                              QString::fromUtf8("RStudio"),
                              sessionLauncher.launchFailedErrorMessage());
         errorMsg.addButton(new QPushButton(QString::fromUtf8("OK")),
                            QMessageBox::AcceptRole);
         errorMsg.show();

         pApp->exec();

         return EXIT_FAILURE;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}
