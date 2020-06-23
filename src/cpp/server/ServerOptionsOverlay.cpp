/*
 * ServerOptionsOverlay.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <server/ServerOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {

void Options::addOverlayOptions(
                       boost::program_options::options_description* pVerify,
                       boost::program_options::options_description* pServer,
                       boost::program_options::options_description* pWWW,
                       boost::program_options::options_description* pRSession,
                       boost::program_options::options_description* pDatabase,
                       boost::program_options::options_description* pAuth,
                       boost::program_options::options_description* pMonitor)
{
}

bool Options::validateOverlayOptions(std::string* pErrMsg,
                                     std::ostream& osWarnings)
{
   return true;
}

void Options::resolveOverlayOptions()
{
}

std::string Options::gwtPrefix() const
{
   return std::string();
}

void sessionProcessConfigOverlay(core::system::Options* pArgs,
                                 core::system::Options* pEnvironment)
{
}

std::string Options::rsessionExecutable() const
{
   return "rsession";
}

} // namespace server
} // namespace rstudio
