/*
 * SessionRHooks.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
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

#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

#include <boost/bind/bind.hpp>

#include "SessionRHooks.hpp"

namespace rstudio {
namespace session {
namespace modules { 
namespace rhooks {

using namespace rstudio::core;
using namespace boost::placeholders;

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionRHooks.R"));
   return initBlock.execute();
}

} // namespace rhooks
} // namespace modules
} // namespace session
} // namespace rstudio

