/*
 * SessionTeX.cpp
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

#include "SessionTeX.hpp"

#include <string>

#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/tex/TexMagicComment.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {

namespace {

std::string sweaveEngineForFile(const FilePath& rnwPath)
{
   // first see if the file contains an rnw weave magic comment
   std::string rnwWeaveDirective;
   std::vector<core::tex::TexMagicComment> magicComments;
   Error error = core::tex::parseMagicComments(rnwPath, &magicComments);
   if (!error)
   {
      BOOST_FOREACH(const core::tex::TexMagicComment& mc, magicComments)
      {
         if (boost::algorithm::iequals(mc.scope(), "rnw") &&
             boost::algorithm::iequals(mc.variable(), "weave"))
         {
            if (boost::algorithm::iequals(mc.value(), "sweave"))
               rnwWeaveDirective = "Sweave";
            else if (boost::algorithm::iequals(mc.value(), "knitr"))
               rnwWeaveDirective = "knitr";
            else
               rnwWeaveDirective = mc.value();

            break;
         }
      }
   }
   else
   {
      LOG_ERROR(error);
   }

   // return the correct sweave engine
   if (!rnwWeaveDirective.empty())
      return rnwWeaveDirective;
   else if (projects::projectContext().hasProject())
      return projects::projectContext().config().defaultSweaveEngine;
   else
      return userSettings().defaultSweaveEngine();
}

FilePath pdfPathForTexPath(const FilePath& texPath)
{
   return texPath.parent().complete(texPath.stem() + ".pdf");
}

#ifdef _WIN32

std::vector<std::string> sweaveArgs(const std::string& file)
{
   std::vector<std::string> args;
   std::string sweaveCmd = "\"Sweave('" + file + "')\"";
   args.push_back("-e");
   args.push_back(sweaveCmd);
   args.push_back("--silent");
   return args;
}

#else

std::vector<std::string> sweaveArgs(const std::string& file)
{
   std::vector<std::string> args;
   args.push_back("CMD");
   args.push_back("Sweave");
   args.push_back(file);
   return args;
}

#endif

std::vector<std::string> knitrArgs(const std::string& file)
{
   std::vector<std::string> args;
   std::string knitrCmd = "require(knitr); knit('" + file + "')";
   args.push_back("--silent");
   args.push_back("-e");
   args.push_back(knitrCmd);
   return args;
}


bool callSweave(const std::string& rBinDir,
                const std::string& file)
{
   // R exe path differs by platform
#ifdef _WIN32
   std::string path = FilePath(rBinDir).complete("Rterm.exe").absolutePath();
#else
   std::string path = FilePath(rBinDir).complete("R").absolutePath();
#endif

   // calculate the full path to the file then use it to determine
   // the active sweave engine
   FilePath rnwPath = module_context::resolveAliasedPath(file);
   std::string sweaveEngine = sweaveEngineForFile(rnwPath);

   // args differ by back-end
   std::vector<std::string> args;
   if (sweaveEngine == "Sweave")
   {
      args = sweaveArgs(file);
   }
   else if (sweaveEngine == "knitr")
   {
      args = knitrArgs(file);
   }
   else
   {
      throw r::exec::RErrorException(
            "Unknown Rnw weave method: " + sweaveEngine + " (valid values " +
            "are Sweave and knitr)");
   }

   // call back-end
   int exitStatus;
   Error error = module_context::executeInterruptableChild(path,
                                                           args,
                                                           &exitStatus);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else if (exitStatus != EXIT_SUCCESS)
   {
      return false;
   }
   else
   {
      return true;
   }
}

SEXP rs_callSweave(SEXP rBinDirSEXP, SEXP fileSEXP)
{
   // call sweave
   bool success = false;
   try
   {
      success = callSweave(r::sexp::asString(rBinDirSEXP),
                           r::sexp::asString(fileSEXP));
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   // check for interrupts (likely since sweave can be long running)
   r::exec::checkUserInterrupt();

   r::sexp::Protect rProtect;
   return r::sexp::create(success, &rProtect);
}

SEXP rs_validateTexFile(SEXP texFileSEXP)
{
   // used to protect return value
   r::sexp::Protect rProtect;

   // get path to TeX file
   std::string texFile = r::sexp::asString(texFileSEXP);

   // false if no file passed
   if (texFile.empty())
      return r::sexp::create(false, &rProtect);

   // read its contents
   FilePath texFilePath = module_context::safeCurrentPath().complete(texFile);
   if (!texFilePath.exists())
      return r::sexp::create(false, &rProtect);

   std::string contents;
   Error error = readStringFromFile(texFilePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return r::sexp::create(false, &rProtect);
   }

   // return based on whether it contains an \end{document}
   bool hasEndDoc = contents.find("\\end{document}") != std::string::npos;
   return r::sexp::create(hasEndDoc, &rProtect);
}

SEXP rs_viewPdf(SEXP texPathSEXP)
{
   FilePath pdfPath = pdfPathForTexPath(FilePath(r::sexp::asString(texPathSEXP)));
   module_context::showFile(pdfPath, "_rstudio_compile_pdf");
   return R_NilValue;
}


Error getTexCapabilities(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(tex::capabilitiesAsJson());
   return Success();
}

} // anonymous namespace

json::Object capabilitiesAsJson()
{
   json::Object obj;

   bool texInstalled;
   Error error = r::exec::RFunction(".rs.is_tex_installed").call(&texInstalled);
   obj["tex_installed"] = !error ? texInstalled : false;

   bool knitrInstalled;
   error = r::exec::RFunction(".rs.is_knitr_installed").call(&knitrInstalled);
   obj["knitr_installed"] = !error ? knitrInstalled : false;

   return obj;
}

Error initialize()
{
   // install core Sweave/TeX routines
   R_CallMethodDef callSweaveMethodDef;
   callSweaveMethodDef.name = "rs_callSweave" ;
   callSweaveMethodDef.fun = (DL_FUNC) rs_callSweave ;
   callSweaveMethodDef.numArgs = 2;
   r::routines::addCallMethod(callSweaveMethodDef);

   R_CallMethodDef validateTexFileMethodDef;
   validateTexFileMethodDef.name = "rs_validateTexFile" ;
   validateTexFileMethodDef.fun = (DL_FUNC) rs_validateTexFile ;
   validateTexFileMethodDef.numArgs = 1;
   r::routines::addCallMethod(validateTexFileMethodDef);

   R_CallMethodDef viewPdfMethodDef ;
   viewPdfMethodDef.name = "rs_viewPdf" ;
   viewPdfMethodDef.fun = (DL_FUNC) rs_viewPdf ;
   viewPdfMethodDef.numArgs = 1;
   r::routines::addCallMethod(viewPdfMethodDef);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_tex_capabilities", getTexCapabilities))
      (bind(sourceModuleRFile, "SessionTeX.R"))
      ;
  return initBlock.execute();
}


} // namespace tex
} // namespace modules
} // namesapce session

