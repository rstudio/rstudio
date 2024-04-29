/*
 * SessionAutomation.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionAutomation.hpp"

#include <shared_core/Error.hpp>

#include <r/RRoutines.hpp>
#include <r/session/REventLoop.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace automation {

namespace {

SEXP rs_processEvents()
{
   r::session::event_loop::processEvents();
   return R_NilValue;
}

} // end anonymous namespace

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_processEvents);
   return Success();
}

} // end namespace automation
} // end namespace modules
} // end namespace session
} // end namespace rstudio
