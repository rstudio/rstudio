/*
 * RSessionState.hpp
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

#ifndef R_R_SESSION_STATE_HPP
#define R_R_SESSION_STATE_HPP

#include <string>

#include <boost/function.hpp>

#include <core/Error.hpp>

namespace core {
   class FilePath;
}

namespace r {
namespace session {
namespace state {
        
bool save(const core::FilePath& statePath,
          bool serverMode,
          bool disableSaveCompression);
   
bool restore(const core::FilePath& statePath, 
             bool serverMode,
             boost::function<core::Error()>* pDeferredRestoreAction,
             std::string* pErrorMessages); 
   
bool destroy(const core::FilePath& statePath);
     
} // namespace state
} // namespace session
} // namespace r

#endif // R_R_SESSION_STATE_HPP

