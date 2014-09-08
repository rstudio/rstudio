/*
 * SessionPackrat.hpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#ifndef SESSION_PACKRAT_HPP
#define SESSION_PACKRAT_HPP

#include <core/json/Json.hpp>
#include <r/RSexp.hpp>
#include <session/SessionModuleContext.hpp>

namespace rstudiocore {
   class Error;
}

namespace session {
namespace modules { 
namespace packrat {

rstudiocore::Error initialize();

// return the current Packrat context
rstudiocore::json::Object contextAsJson();

// annotate a JSON object with pending Packrat actions
void annotatePendingActions(rstudiocore::json::Object *pJson, bool useCached);

// return the given Packrat context
rstudiocore::json::Object contextAsJson(const module_context::PackratContext& context);

// get Packrat options (as an SEXP)
rstudiocore::Error getPackratOptions(SEXP* pOptionsSEXP, r::sexp::Protect* pRProtect);

} // namespace packrat
} // namespace modules
} // namespace session

#endif // SESSION_PACKRAT_HPP
