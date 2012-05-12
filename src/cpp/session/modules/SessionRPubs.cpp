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

#include <boost/bind.hpp>
#include <boost/utility.hpp>
#include <boost/format.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/text/CsvParser.hpp>
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

   bool isRunning() const { return isRunning_; }

   void terminate()
   {
      terminationRequested_ = true;
   }

private:
   RPubsUpload()
      : terminationRequested_(false), isRunning_(false)
   {
   }

   void start(const std::string& title,
              const FilePath& htmlFile,
              const std::string& id)
   {
      using namespace core::string_utils;
      using namespace module_context;

      isRunning_ = true;

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
      std::string htmlPath = utf8ToSystem(htmlFile.absolutePath());

      std::string escapedScriptPath = string_utils::jsLiteralEscape(scriptPath);
      std::string escapedTitle = string_utils::jsLiteralEscape(title);
      std::string escapedHtmlPath = string_utils::jsLiteralEscape(htmlPath);
      std::string escapedId = string_utils::jsLiteralEscape(id);

      std::string cmd = boost::str(fmt %
                    escapedScriptPath % escapedTitle % escapedHtmlPath %
                    (!escapedId.empty() ? "\"" + escapedId + "\"" : "NULL"));
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
         Result result = parseOutput(output_);
         if (!result.empty())
            terminateWithResult(result);
         else
            terminateWithError("Unexpected output from upload: " + output_);
      }
      else
      {
         terminateWithError(error_);
      }
   }

   void terminateWithError(const Error& error)
   {
      terminateWithError(error.summary());
   }

   void terminateWithError(const std::string& error)
   {
      isRunning_ = false;


   }


   struct Result
   {
      Result()
      {
      }

      Result(const std::string& error)
         : error(error)
      {
      }

      Result(const std::string& id, const std::string& continueUrl)
         : id(id), continueUrl(continueUrl)
      {
      }

      bool empty() const { return id.empty() && error.empty();  }

      std::string id;
      std::string continueUrl;
      std::string error;
   };
   void terminateWithResult(const Result& result)
   {
      isRunning_ = false;



   }

   Result parseOutput(const std::string& output)
   {
      std::pair<std::vector<std::string>, std::string::const_iterator>
                  line = text::parseCsvLine(output.begin(), output.end());
      if (!line.first.empty())
      {
         std::vector<std::string> headers = line.first;

         line = text::parseCsvLine(line.second, output.end());
         if (!line.first.empty())
         {
            std::vector<std::string> data = line.first;

            if (headers.size() == 1 &&
                data.size() == 1 &&
                headers[0] == "error")
            {
               return Result(data[0]);
            }
            else if (headers.size() == 2 &&
                     data.size() == 2 &&
                     headers[0] == "id" &&
                     headers[1] == "continueUrl")
            {
               return Result(data[0], data[1]);
            }
         }
      }

      return Result();
   }

private:
   bool terminationRequested_;
   bool isRunning_;
   std::string output_;
   std::string error_;
};

boost::shared_ptr<RPubsUpload> s_pCurrentUpload;

bool isUploadRunning()
{
   return s_pCurrentUpload && s_pCurrentUpload->isRunning();
}

Error rpubsUpload(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string title, htmlFile;
   Error error = json::readParams(request.params, &title, &htmlFile);
   if (error)
      return error;

   if (isUploadRunning())
   {
      pResponse->setResult(false);
   }
   else
   {
      FilePath filePath = module_context::resolveAliasedPath(htmlFile);
      s_pCurrentUpload = RPubsUpload::create(title, filePath, "");
      pResponse->setResult(true);
   }

   return Success();
}

Error terminateRpubsUpload(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   if (isUploadRunning())
      s_pCurrentUpload->terminate();

   return Success();
}




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

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "rpubs_upload", rpubsUpload))
      (bind(registerRpcMethod, "terminate_rpubs_upload", terminateRpubsUpload))
   ;
   return initBlock.execute();
   return Success();

}
   
   
} // namespace rpubs
} // namespace modules
} // namesapce session

