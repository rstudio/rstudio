/*
 * ServerLogVars.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SERVER_LOG_VARS_HPP
#define SERVER_LOG_VARS_HPP

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace server {
namespace log_vars {

core::Error initialize();

} // namespace log_vars
} // namespace server
} // namespace rstudio

#endif // SERVER_LOG_VARS_HPP
