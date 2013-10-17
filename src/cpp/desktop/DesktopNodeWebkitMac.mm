/*
 * DesktopNodeWebkitMac.cpp
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

#include "DesktopNodeWebkit.hpp"

#include <iostream>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/SafeConvert.hpp>
#include <core/Thread.hpp>
#include <core/system/System.hpp>

#include <mach-o/dyld.h>
#include <Security/AuthSession.h>
#include <CoreServices/CoreServices.h>
#include <Foundation/NSString.h>
#include <Foundation/NSDictionary.h>

#include <QProcess>
#include <QTcpSocket>
#include <QDir>
#include <QFileInfo>

#ifdef __clang__
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#endif

// TODO: quit protection

/*
var gui = require('nw.gui');
var win = gui.Window.get();
win.on('close', function() {
  this.hide(); // Pretend to be closed already
  console.log("We're closing...");
  this.close(true);
});
*/

// TODO: test multi-session within OS (user switch)


using namespace core;

namespace desktop {

namespace {

Error errorForStatus(OSStatus status, const ErrorLocation& location)
{
   return systemError(boost::system::errc::protocol_error,
                      ::GetMacOSStatusCommentString(status),
                      location);
}

int sessionPort()
{
   // get the unique session id
   SecuritySessionId sessionId;
   SessionAttributeBits sessionInfo;
   OSStatus error = ::SessionGetInfo(callerSecuritySession,
                                     &sessionId,
                                     &sessionInfo);
   if (error != noErr)
   {
      LOG_ERROR(errorForStatus(error, ERROR_LOCATION));
      return -1;
   }

   // compute a port from it
   return (sessionId % 40000) + 8080;
}

bool isServerRunning(int port)
{
   boost::scoped_ptr<QTcpSocket> pSocket(new QTcpSocket());
   pSocket->connectToHost(QString::fromUtf8("localhost"), port);
   bool isRunning = pSocket->waitForConnected(200);
   pSocket->close();
   return isRunning;
}

FilePath executablePath()
{
   // get path to current executable
   uint32_t buffSize = 2048;
   std::vector<char> buffer(buffSize);
   if (_NSGetExecutablePath(&(buffer[0]), &buffSize) == -1)
   {
      buffer.resize(buffSize);
      _NSGetExecutablePath(&(buffer[0]), &buffSize);
   }

   // return it
   return FilePath(&(buffer[0]));
}

QString rserverPath()
{
   // check for rserver in the bundle
   FilePath macOSPath = executablePath().parent();
   FilePath rserverPath = macOSPath.childPath("rserver");
   if (rserverPath.exists())
   {
      return QString::fromStdString(rserverPath.absolutePath());
   }
   else // development mode
   {
      return QString::fromStdString(
                macOSPath.complete("../../../../rserver-dev").absolutePath());
   }
}

QString nodeWebkitPath()
{
   FilePath exePath = executablePath();
   FilePath frameworksPath = exePath.parent().parent().childPath("Frameworks");
   FilePath nodewebkitPath = frameworksPath.childPath("RStudioIDE.app");
   if (nodewebkitPath.exists())
   {
      return QString::fromStdString(nodewebkitPath.absolutePath());
   }
   else // development mode
   {
      FilePath srcPath = exePath.complete("../../../../../..");
      return QString::fromStdString(
            srcPath.complete("cpp/desktop/node-webkit/RStudioIDE.app").absolutePath());
   }
}

bool isOSXMavericks()
{
   NSDictionary *systemVersionDictionary =
       [NSDictionary dictionaryWithContentsOfFile:
           @"/System/Library/CoreServices/SystemVersion.plist"];

   NSString *systemVersion =
       [systemVersionDictionary objectForKey:@"ProductVersion"];

   std::string version(
         [systemVersion cStringUsingEncoding:NSASCIIStringEncoding]);

   return boost::algorithm::starts_with(version, "10.9");
}

} // anonymous namespace


bool useNodeWebkit()
{
   // check configuration
   FilePath appSupportPath =
     system::userHomePath().childPath("Library/Application Support/RStudio");
   bool useServerMode = appSupportPath.childPath("use-server-mode").exists();
   bool useDesktopMode = appSupportPath.childPath("use-desktop-mode").exists();

   if (useServerMode)
   {
      return true;
   }
   else if (isOSXMavericks() && !useDesktopMode)
   {
      return true;
   }
   else
   {
      return false;
   }
}


int runWithNodeWebkit()
{
   // get the session port
   int port = sessionPort();
   if (port == -1)
      return EXIT_FAILURE;

   // start the server if we need to
   if (!isServerRunning(port))
   {
      QStringList args;
      args.push_back(QString::fromUtf8("--server-on-desktop=1"));
      args.push_back(QString::fromUtf8("--www-port"));
      args.push_back(QString::number(port));
      QString exePath = rserverPath();
      bool started = QProcess::startDetached(exePath, args);
      if (!started)
      {
         LOG_ERROR_MESSAGE("Unable to start rserver process at " +
                           exePath.toStdString());
         return EXIT_FAILURE;
      }

      // sleep to give the server a chance to start
      boost::this_thread::sleep(boost::posix_time::milliseconds(200));
   }

   // launch the client (will do a reactivation if it's already running)
   QStringList args;
   args.push_back(QString::fromUtf8("-a"));
   QString exePath = nodeWebkitPath();
   args.push_back(exePath);
   args.push_back(QString::fromUtf8("--args"));
   QString urlArg = QString::fromUtf8("--url=http://localhost:%1");
   args.push_back(urlArg.arg(port));
   bool started = QProcess::startDetached(QString::fromUtf8("open"), args);
   if (!started)
   {
      LOG_ERROR_MESSAGE("Unabled to start node-webkit process at " +
                        exePath.toStdString());
      return EXIT_FAILURE;
   }
   else
   {
      return EXIT_SUCCESS;
   }
}

} // namespace desktop
