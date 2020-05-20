/*
 * SessionEnvironment.hpp
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

#ifndef SESSION_SESSION_ENVIRONMENT_HPP
#define SESSION_SESSION_ENVIRONMENT_HPP

#include <shared_core/json/Json.hpp>

#include "EnvironmentConstants.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules {      
namespace environment {

core::json::Value environmentStateAsJson();

bool monitoring();

bool isSuspendable();

core::Error initialize();
   
} // namespace environment
} // namepace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_ENVIRONMENT_HPP
