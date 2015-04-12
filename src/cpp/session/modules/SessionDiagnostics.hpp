/*
 * SessionDiagnostics.hpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

#ifndef SESSION_LINTER_HPP
#define SESSION_LINTER_HPP

#include <vector>
#include <string>
#include <iostream>
#include <sstream>
#include <map>
#include <set>

#include <core/r_util/RTokenizer.hpp>
#include "SessionRParser.hpp"
#include "SessionRTokenCursor.hpp"
#include <core/collection/Stack.hpp>
#include <core/collection/Position.hpp>
#include <core/StringUtils.hpp>

#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/range/adaptor/map.hpp>

#include <core/Macros.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

core::Error initialize();

} // namespace linter
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_LINTER_HPP
