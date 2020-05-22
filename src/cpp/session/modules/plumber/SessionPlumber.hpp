/*
 * SessionPlumber.hpp
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

#ifndef SESSION_PLUMBER_HPP
#define SESSION_PLUMBER_HPP

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace plumber {

enum class PlumberFileType
{
   PlumberNone,
   PlumberApi
};

PlumberFileType plumberTypeFromExtendedType(const std::string& extendedType);

core::Error initialize();
                       
} // namespace plumber
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PLUMBER_HPP
