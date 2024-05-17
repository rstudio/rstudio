/*
 * SessionDebugging.cpp
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

#include "SessionDebugging.hpp"

#include <fcntl.h>

#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace debugging {

namespace {

class RedirectOutputScope
{
public:
   RedirectOutputScope(const char* filename)
   {
      // Save references to the stdout, stderr streams.
      stdout_ = ::dup(STDOUT_FILENO);
      stderr_ = ::dup(STDERR_FILENO);
      
      // Open file for writing.
      int fd = ::open(filename, O_WRONLY | O_APPEND | O_CREAT, 0600);
      
      // Redirect stdout, stderr to this file.
      ::dup2(fd, STDOUT_FILENO);
      ::dup2(fd, STDERR_FILENO);
      
      // Remove reference.
      ::close(fd);
      
      // Also redirect R output streams.
      Error error = r::exec::RFunction(".rs.debugging.beginSinkOutput")
            .addParam("file", filename)
            .call();
      
      if (error)
         LOG_ERROR(error);
   }
   
   ~RedirectOutputScope()
   {
      // Restore R output streams.
      Error error = r::exec::RFunction(".rs.debugging.endSinkOutput")
            .call();
      
      if (error)
         LOG_ERROR(error);
      
      // Restore stdout, stderr
      ::dup2(stdout_, STDOUT_FILENO);
      ::close(stdout_);
      
      ::dup2(stderr_, STDERR_FILENO);
      ::close(stderr_);
   }
   
private:
   int stdout_;
   int stderr_;
};

void printTraceback()
{
   RedirectOutputScope scope("/tmp/rstudio-debug.log");
   
   Error error = r::exec::RFunction(".rs.debugging.printTraceback")
         .call();
   
   if (error)
      LOG_ERROR(error);
}

extern "C" SEXP rs_traceback()
{
   printTraceback();
   return R_NilValue;
}

} // end anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   
   RS_REGISTER_CALL_METHOD(rs_traceback);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionDebugging.R"));
   return initBlock.execute();
}

} // namespace debugging
} // namespace modules
} // namespace session
} // namespace rstudio
