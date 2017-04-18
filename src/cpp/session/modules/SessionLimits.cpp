/*
 * SessionLimits.cpp
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


#include "SessionLimits.hpp"

#include <boost/format.hpp>
#include <boost/bind.hpp>
#include <boost/function.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/system/System.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace limits {

namespace {
      
void onBeforeExecute()
{
   // enforce a limit if there is one
   if (session::options().limitCpuTimeMinutes() > 0)
   {
      // calculate seconds
      int seconds = session::options().limitCpuTimeMinutes() * 60;
   
      // call setTimeLimit
      r::exec::RFunction setTimeLimit("setTimeLimit");
      setTimeLimit.addParam("cpu", seconds);
      Error error = setTimeLimit.call();
      if (error)
         LOG_ERROR(error);
   }
}    
   
} // anonymous namespace
   
Error initialize()
{  
   // subscribe to onBeforeExecute so we can set a cpu time limit for
   // top level computations
   module_context::events().onBeforeExecute.connect(boost::bind(
                                                            onBeforeExecute));

   return Success();
}
   


} // namespace limits
} // namespace modules
} // namespace session
} // namespace rstudio

