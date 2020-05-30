/*
 * RGraphicsUtils.hpp
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

#ifndef R_SESSION_GRAPHICS_UTILS_HPP
#define R_SESSION_GRAPHICS_UTILS_HPP

#include <boost/shared_ptr.hpp>

namespace rstudio {
namespace core {
   class Error;
   class ErrorLocation;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

void setCompatibleEngineVersion(int version);
bool validateRequirements(std::string* pMessage = nullptr);

class RestorePreviousGraphicsDeviceScope
{
public:
   RestorePreviousGraphicsDeviceScope();
   virtual ~RestorePreviousGraphicsDeviceScope();
   
private:
   struct Impl;
   boost::shared_ptr<Impl> pImpl_;
};

void reportError(const core::Error& error);

void logAndReportError(const core::Error& error,
                       const core::ErrorLocation& location);

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_SESSION_GRAPHICS_UTILS_HPP 

