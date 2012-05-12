/*
 * SessionRPubs.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionRPubs.hpp"

#include <boost/utility.hpp>
#include <boost/format.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Error.hpp>
#include <core/system/Process.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace rpubs {

namespace {

class RPubsUpload : boost::noncopyable,
                    public boost::enable_shared_from_this<RPubsUpload>
{
public:
   static boost::shared_ptr<RPubsUpload> create(const std::string& title,
                                                const FilePath& htmlFile,
                                                const std::string& id)
   {
      boost::shared_ptr<RPubsUpload> pUpload(new RPubsUpload());
      pUpload->start(title, htmlFile, id);
      return pUpload;
   }

   virtual ~RPubsUpload()
   {
   }

   void terminate()
   {
      terminationRequested_ = true;
   }

private:
   RPubsUpload()
      : terminationRequested_(false)
   {
   }

   void start(const std::string& title,
              const FilePath& htmlFile,
              const std::string& id)
   {
      using namespace core::string_utils;
      using namespace module_context;

      // R binary
      FilePath rProgramPath;
      Error error = rScriptPath(&rProgramPath);
      if (error)
      {
         terminateWithError(error);
         return;
      }

      // args
      std::vector<std::string> args;
      args.push_back("--slave");
      args.push_back("--vanilla");
      args.push_back("-e");

      boost::format fmt(
               "source(\"%1%\"); "
               "result <- rpubsUpload(\"%2%\", \"%3%\", %4%); "
               "utils::write.csv(as.data.frame(result), row.names=FALSE);");

      FilePath modulesPath = session::options().modulesRSourcePath();;
      std::string scriptPath = utf8ToSystem(
                        modulesPath.complete("SessionRPubs.R").absolutePath());
      boost::replace_all(scriptPath, "\\", "\\\\");

      std::string htmlPath = utf8ToSystem(htmlFile.absolutePath());
      boost::replace_all(htmlPath, "\\", "\\\\");

      std::string cmd = boost::str(fmt %
                                   scriptPath %
                                   title %
                                   htmlPath %
                                   (!id.empty() ? "\"" + id + "\"" : "NULL"));
      args.push_back(cmd);

      // options
      core::system::ProcessOptions options;
      options.terminateChildren = true;
      options.workingDir = htmlFile.parent();

      // callbacks
      core::system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&RPubsUpload::onContinue,
                                  RPubsUpload::shared_from_this());
      cb.onStdout = boost::bind(&RPubsUpload::onStdOut,
                                RPubsUpload::shared_from_this(), _2);
      cb.onStderr = boost::bind(&RPubsUpload::onStdErr,
                                RPubsUpload::shared_from_this(), _2);
      cb.onExit =  boost::bind(&RPubsUpload::onCompleted,
                                RPubsUpload::shared_from_this(), _1);

      // execute
      processSupervisor().runProgram(rProgramPath.absolutePath(),
                                     args,
                                     options,
                                     cb);
   }

   bool onContinue()
   {
      return !terminationRequested_;
   }

   void onStdOut(const std::string& output)
   {
      output_.append(output);
   }

   void onStdErr(const std::string& error)
   {
      error_.append(error);
   }

   void onCompleted(int exitStatus)
   {
      if (exitStatus == EXIT_SUCCESS)
      {
         module_context::consoleWriteOutput(output_);
      }
      else
      {
         module_context::consoleWriteError("Error: " + error_);
      }
   }

   void terminateWithError(const Error& error)
   {

   }


private:
   bool terminationRequested_;
   std::string output_;
   std::string error_;
};

boost::shared_ptr<RPubsUpload> s_pCurrentUpload;

// log warning message from R
SEXP rs_rpubsUpload(SEXP titleSEXP, SEXP htmlFileSEXP, SEXP idSEXP)
{
   std::string title = r::sexp::asString(titleSEXP);
   std::string htmlFile = r::sexp::asString(htmlFileSEXP);
   std::string id = r::sexp::asString(idSEXP);

   s_pCurrentUpload = RPubsUpload::create(title, FilePath(htmlFile), id);




   return R_NilValue;
}


} // anonymous namespace


Error initialize()
{
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_rpubsUpload" ;
   methodDef.fun = (DL_FUNC) rs_rpubsUpload ;
   methodDef.numArgs = 3;
   r::routines::addCallMethod(methodDef);

   return Success();

}
   
   
} // namespace rpubs
} // namespace modules
} // namesapce session

