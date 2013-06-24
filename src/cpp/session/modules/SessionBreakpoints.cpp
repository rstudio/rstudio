/*
 * SessionBreakpoints.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/utility.hpp>
#include <boost/foreach.hpp>


#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

using namespace core ;
using namespace r::sexp;
using namespace r::exec;

namespace session {
namespace modules {
namespace breakpoints {

namespace {

Error getFunctionSteps(
      const json::JsonRpcRequest& request,
      json::JsonRpcResponse* pResponse)
{
   json::Array lineNumbers;
   Error error = json::readParams(request.params, &lineNumbers);

   json::Value foo;
   BOOST_FOREACH(json::Value lineNumber, lineNumbers)
   {
      std::cerr << "checking for line " << lineNumber.get_int() << std::endl;
   }

   return error;
}

} // anonymous namespace

Error initialize()
{
   // subscribe to events
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_function_steps", getFunctionSteps))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}


} // namepsace dirty
} // namespace modules
} // namesapce session


