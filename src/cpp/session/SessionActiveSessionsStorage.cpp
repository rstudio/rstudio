/*
 * SessionActiveSessionsStorage.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <session/SessionActiveSessionsStorage.hpp>

#include <shared_core/system/User.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionServerRpc.hpp>

using namespace rstudio::core;
using namespace rstudio::core::r_util;

namespace rstudio {
namespace session {
namespace storage {

InvokeRpc getSessionRpcInvoker()
{
   return [](const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
   {
      return server_rpc::invokeServerRpc(request, pResponse);
   };
}

Error activeSessionsStorage(std::shared_ptr<IActiveSessionsStorage>* pStorage) 
{
   FilePath storagePath = options().userScratchPath();

#ifdef _WIN32
   pStorage->reset(new FileActiveSessionsStorage(storagePath));
#else
   if (options().sessionUseFileStorage())
      pStorage->reset(new FileActiveSessionsStorage(storagePath));
   else
   {
      system::User user;
      Error error = system::User::getCurrentUser(user);
      if (error)
      {
         LOG_ERROR(error);
         return error;
      }

      pStorage->reset(new RpcActiveSessionsStorage(
            user,
            storagePath,
            getSessionRpcInvoker()));
   }
#endif

   return Success();
}

} // namespace storage
} // namespace session
} // namespace rstudio
