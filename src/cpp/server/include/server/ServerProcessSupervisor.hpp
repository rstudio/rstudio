/*
 * ServerProcessSupervisor.hpp
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

#ifndef SERVER_PROCESS_SUPERVISOR_HPP
#define SERVER_PROCESS_SUPERVISOR_HPP

#include <string>
#include <vector>

#include <core/system/Process.hpp>

namespace  core {
   class Error;
}

namespace server {
namespace process_supervisor {

rscore::Error runProgram(
  const std::string& executable,
  const std::vector<std::string>& args,
  const std::string& input,
  const rscore::system::ProcessOptions& options,
  const boost::function<void(const rscore::system::ProcessResult&)>& onCompleted);

rscore::Error initialize();

} // namespace process_supervisor
} // namespace server

#endif // SERVER_PROCESS_SUPERVISOR_HPP

