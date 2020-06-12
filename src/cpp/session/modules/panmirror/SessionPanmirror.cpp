/*
 * SessionPanmirror.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionPanmirror.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/system/Process.hpp>
#include <core/json/JsonRpc.hpp>


#include "SessionPanmirrorPandoc.hpp"
#include "SessionPanmirrorCrossref.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {

std::string errorMessage(const core::Error& error)
{
   std::string msg = error.getMessage();
   if (msg.length() == 0)
   {
      msg = error.getProperty("category");
   }
   if (msg.length() == 0)
   {
      msg = error.getName();
   }
   return msg;
}

void setErrorResponse(const core::Error& error, core::json::JsonRpcResponse* pResponse)
{
   LOG_ERROR(error);
   pResponse->setError(error, errorMessage(error));
}

void setProcessErrorResponse(const core::system::ProcessResult& result,
                             core::json::JsonRpcResponse* pResponse)
{
   Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, ERROR_LOCATION);
   LOG_ERROR(error);
   pResponse->setError(error, result.stdErr);
}


Error initialize()
{
   core::ExecBlock initBlock;
   initBlock.addFunctions()
      (pandoc::initialize)
      (crossref::initialize)
    ;
   return initBlock.execute();
}

} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
