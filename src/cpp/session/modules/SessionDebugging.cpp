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

#ifdef _WIN32
# include <io.h>
#endif

#ifndef STDOUT_FILENO
# define STDOUT_FILENO 1
#endif

#ifndef STDERR_FILENO
# define STDERR_FILENO 2
#endif

#include <fcntl.h>

#include <core/Exec.hpp>
#include <core/system/Xdg.hpp>

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

const char* debugFilename()
{
#ifdef _WIN32
   static std::string instance = ([]()
   {
      return core::system::xdg::userLogDir()
            .completePath("rstudio-debug.log")
            .getAbsolutePath();
   })();

   return instance.c_str();
#else
   return "/tmp/rstudio-debug.log";
#endif
}

class RedirectOutputScope
{
public:
   RedirectOutputScope(const char* filename)
   {
      ::fflush(stdout);
      ::fflush(stderr);

      // Save references to the stdout, stderr streams.
      stdout_ = ::dup(STDOUT_FILENO);
      stderr_ = ::dup(STDERR_FILENO);
      
      // Open file for writing.
      int fd = ::open(filename, O_WRONLY | O_APPEND | O_CREAT, 0600);
      if (fd == -1)
      {
         LOG_ERROR(LAST_SYSTEM_ERROR());
         return;
      }
      
      // Redirect stdout, stderr to this file.
      ::dup2(fd, STDOUT_FILENO);
      ::dup2(fd, STDERR_FILENO);

      // Disable buffering on stdout and stderr.
      ::setvbuf(stdout, NULL, _IONBF, 0);
      ::setvbuf(stderr, NULL, _IONBF, 0);

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
   Error error = r::exec::RFunction(".rs.debugging.printTraceback")
         .call();
   
   if (error)
      LOG_ERROR(error);
}

} // end anonymous namespace


} // namespace debugging
} // namespace modules
} // namespace session
} // namespace rstudio

extern "C" {

RS_EXPORT void rd_eval(const char* code)
{
   using namespace rstudio::session::modules::debugging;
   RedirectOutputScope scope(debugFilename());
   
   r::sexp::Protect protect;
   SEXP resultSEXP = R_NilValue;
   Error error = r::exec::evaluateString(code, &resultSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   Rf_PrintValue(resultSEXP);
}


RS_EXPORT void rd_traceback()
{
   using namespace rstudio::session::modules::debugging;
   RedirectOutputScope scope(debugFilename());
   printTraceback();
}

} // extern "C"


namespace rstudio {
namespace session {
namespace modules {
namespace debugging {

namespace {

SEXP rs_traceback()
{
   rd_traceback();
   return R_NilValue;
}

SEXP rs_eval(SEXP codeSEXP)
{
   std::string code = r::sexp::asString(codeSEXP);
   rd_eval(code.c_str());
   return R_NilValue;
}

} // end anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   
   RS_REGISTER_CALL_METHOD(rs_traceback);
   RS_REGISTER_CALL_METHOD(rs_eval);

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionDebugging.R"));
   return initBlock.execute();
}

} // namespace debugging
} // namespace modules
} // namespace session
} // namespace rstudio
