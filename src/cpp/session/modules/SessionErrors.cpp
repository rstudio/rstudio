/*
 * SessionErrors.cpp
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

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>

#include <boost/bind.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules {
namespace errors {
namespace {

Error setErrManagement(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   int type = 0;
   Error error = json::readParams(request.params, &type);
   if (error)
      return error;

   // clear the previous error handler; if we don't do this, the error handler
   // we set will be unset by DisableErrorHandlerScope during call evaluation
   r::options::setErrorOption(R_NilValue);

   return r::exec::RFunction(".rs.setErrorManagementType", type)
           .call();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_error_management_type", setErrManagement))
      (bind(sourceModuleRFile, "SessionErrors.R"));

   return initBlock.execute();
}


} // namepsace errors
} // namespace modules
} // namesapce session


