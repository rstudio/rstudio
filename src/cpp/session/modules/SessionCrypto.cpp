/*
 * SessionCrypto.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace crypto {

Error getPublicKey(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   std::string exponent;
   std::string modulo;
   core::system::crypto::rsaPublicKey(&exponent, &modulo);

   json::Object result;
   result["exponent"] = exponent;
   result["modulo"] = modulo;
   pResponse->setResult(result);

   return Success();
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
} // namesapce session

