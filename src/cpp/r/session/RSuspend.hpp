/*
 * RSuspend.hpp
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

#ifndef R_SESSION_SUSPEND_HPP
#define R_SESSION_SUSPEND_HPP

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace r {
namespace session {

struct RSuspendOptions;

void setSuspendPaths(const core::FilePath& sessionPath, 
      const core::FilePath& clientStatePath, 
      const core::FilePath& projectClientStatePath);
core::FilePath suspendedSessionPath();
bool suspend(const RSuspendOptions& options,
             const core::FilePath& suspendedSessionPath,
             bool disableSaveCompression,
             bool force);
bool suspended();

void saveClientState(ClientStateCommitType commitType);

class SerializationCallbackScope : boost::noncopyable
{
public:
   SerializationCallbackScope(int action, const core::FilePath& targetPath = core::FilePath());
   ~SerializationCallbackScope();
};

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_SUSPEND_HPP 
