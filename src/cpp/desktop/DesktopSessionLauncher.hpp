/*
 * DesktopSessionLauncher.hpp
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

#ifndef DESKTOP_SESSION_LAUNCHER_HPP
#define DESKTOP_SESSION_LAUNCHER_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include "DesktopApplicationLaunch.hpp"
#include "DesktopMainWindow.hpp"

namespace desktop {

class SessionLauncher : boost::noncopyable
{
public:
   SessionLauncher(const core::FilePath& sessionPath,
                   const core::FilePath& confPath)
      : confPath_(confPath),
        sessionPath_(sessionPath),
        pMainWindow_(NULL),
        pRSessionProcess_(NULL)
   {
   }

   core::Error launchFirstSession(const QString& filename,
                                  ApplicationLaunch* pAppLaunch);


   QString readFailedLaunchStandardError() const;

   void cleanupAtExit();

private:
   void buildLaunchContext(QString* pHost,
                           QString* pPort,
                           QStringList* pArgList,
                           QUrl* pUrl) const;


private:
   core::FilePath confPath_;
   core::FilePath sessionPath_;
   MainWindow* pMainWindow_;
   QProcess* pRSessionProcess_;
};

} // namespace desktop

#endif // DESKTOP_SESSION_LAUNCHER_HPP
