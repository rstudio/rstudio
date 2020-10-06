/*
 * RRestartContext.hpp
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

#ifndef R_SESSION_RESTART_CONTEXT_HPP
#define R_SESSION_RESTART_CONTEXT_HPP

#include <boost/noncopyable.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace r {
namespace session {

// singleton
class RestartContext;
RestartContext& restartContext();

class RestartContext : boost::noncopyable
{
private:
   RestartContext();
   friend RestartContext& restartContext();

public:

   void initialize(const core::FilePath& scopePath,
                   const std::string& contextId);

   bool hasSessionState() const;

   bool rProfileOnRestore() const;

   core::FilePath sessionStatePath() const;

   void removeSessionState();

   static core::FilePath createSessionStatePath(
                                            const core::FilePath& scopePath,
                                            const std::string& contextId);

private:
   core::FilePath sessionStatePath_;
};


} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_RESTART_CONTEXT_HPP

