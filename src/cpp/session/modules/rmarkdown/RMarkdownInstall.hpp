/*
 * RMarkdownInstall.hpp
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

#ifndef SESSION_SESSION_RMARKDOWN_INSTALL_HPP
#define SESSION_SESSION_RMARKDOWN_INSTALL_HPP

#include <boost/shared_ptr.hpp>

namespace core {
   class Error;
}

namespace session {

namespace console_process {
class ConsoleProcess;
} // namespace console_process

namespace modules {      
namespace rmarkdown {
namespace install {

core::Error initialize();

enum Status
{
   NotInstalled,
   Installed,
   InstalledRequiresUpdate
};

Status status();

bool haveRequiredVersion();

core::Error installWithProgress(
               boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

core::Error silentUpdate();

} // namespace install
} // namespace rmarkdown
} // namepace modules
} // namesapce session

#endif // SESSION_SESSION_RMARKDOWN_INSTALL_HPP
