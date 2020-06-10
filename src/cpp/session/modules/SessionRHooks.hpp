/*
 * SessionRHooks.hpp
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

#ifndef SESSION_RHOOKS_HPP
#define SESSION_RHOOKS_HPP

#define kSessionInitHook "rstudio.sessionInit"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules { 
namespace rhooks {

core::Error initialize();

// thin wrapper around hook invocation 
template<typename ParamType> 
core::Error invokeHook(const std::string& hookName, const ParamType& param)
{
   return r::exec::RFunction(".rs.invokeHook", hookName, param).call();
}

} // namespace packrat
} // namespace modules
} // namespace rhooks
} // namespace rstudio

#endif // SESSION_RHOOKS_HPP

