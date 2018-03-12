/*
 * SessionJobs.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <string>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "Job.hpp"
#include "SessionJobs.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {

namespace {

// map of job ID to jobs
std::map<std::string, boost::shared_ptr<Job> > s_jobs;

SEXP rs_addJob(SEXP name, SEXP status, SEXP progressUnits, SEXP actions,
      SEXP estimate, SEXP estimateRemaining, SEXP running, SEXP autoRemove,
      SEXP group) 
{
   return R_NilValue;
}
      
Error getJobs(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{

   return Success();
}

} // anonymous namespace

core::Error initialize()
{
   // register API handlers
   RS_REGISTER_CALL_METHOD(rs_addJob, 9);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_jobs", getJobs))
      (bind(module_context::sourceModuleRFile, "SessionJobs.R"));

   return initBlock.execute();
}

} // namepsace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

