/*
 * SessionLauncher.hpp
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

#ifndef DESKTOP_SESSION_LAUNCHER_HPP
#define DESKTOP_SESSION_LAUNCHER_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

namespace desktop {

// singleton
class SessionLauncher;
SessionLauncher& sessionLauncher();

class SessionLauncher : boost::noncopyable
{
private:
   SessionLauncher() {}
   friend SessionLauncher& sessionLauncher();
   
public:
   void init(const core::FilePath& sessionPath,
             const core::FilePath& confPath);
   
   core::Error launchFirstSession(const std::string& filename);
   
   std::string launchFailedErrorMessage();
   
   void cleanupAtExit();
   
private:
   core::FilePath confPath_;
   core::FilePath sessionPath_;
};
   

   
} // namespace desktop

#endif // DESKTOP_SESSION_LAUNCHER_HPP