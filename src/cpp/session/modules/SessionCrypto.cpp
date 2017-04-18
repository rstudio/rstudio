/*
 * SessionCrypto.cpp
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

#include "SessionCrypto.hpp"

#include <string>

#include <boost/bind.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/Crypto.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace crypto {

namespace {

Error getPublicKey(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(publicKeyInfoJson());

   return Success();
}

} // anonymous namespace


json::Object publicKeyInfoJson()
{
   std::string exponent;
   std::string modulo;
   core::system::crypto::rsaPublicKey(&exponent, &modulo);

   json::Object result;
   result["exponent"] = exponent;
   result["modulo"] = modulo;
   return result;
}

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_public_key", getPublicKey));
   return initBlock.execute();
}


} // namespace crypto
} // namespace modules
} // namespace session
} // namespace rstudio

