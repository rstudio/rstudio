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

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {

namespace {

FilePath pdfPathForTexPath(const FilePath& texPath)
{
   return texPath.parent().complete(texPath.stem() + ".pdf");
}

void callSweave(const std::string& rBinDir,
                const std::string& file)
{
   // build R exe path and args
#ifdef _WIN32
   std::string path = FilePath(rBinDir).complete("Rterm.exe").absolutePath();
   core::system::Options args;
   std::string sweaveCmd = "\"Sweave('" + file + "')\"";
   args.push_back(std::make_pair("-e", sweaveCmd));
   args.push_back(std::make_pair("--silent", ""));
#else
   std::string path = FilePath(rBinDir).complete("R").absolutePath();
   core::system::Options args;
   args.push_back(std::make_pair("CMD", "Sweave"));
   args.push_back(std::make_pair(file, ""));
#endif

   // call sweave
   Error error = module_context::executeInterruptableChild(path, args);
   if (error)
      LOG_ERROR(error);
}

SEXP rs_callSweave(SEXP rBinDirSEXP, SEXP fileSEXP)
{
   // call sweave
   callSweave(r::sexp::asString(rBinDirSEXP),
              r::sexp::asString(fileSEXP));

   // check for interrupts (likely since sweave can be long running)
   r::exec::checkUserInterrupt();

   return R_NilValue;
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

} // anonymous namespace
      
Error initialize()
{
   // On Mac OS make sure the path is updated to find MacTeX or
   // MacPorts texlive
#ifdef __APPLE__
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      // get path
      std::string path = core::system::getenv("PATH");

      // do we need to add /opt/local/bin?
      FilePath optLocalBinPath("/opt/local/bin");
      if (!regex_search(path, boost::regex("(^|:)/opt/local/bin/?($|:)"))
          && optLocalBinPath.exists())
      {
         // add opt/local/bin to path (prepend so we find macports texi2dvi
         // first if it is installed there)
         core::system::addToSystemPath(optLocalBinPath, true);
      }

      // do we need to add /usr/texbin?
      FilePath texbinPath("/usr/texbin");
      if (!regex_match(path, boost::regex("(^|:)/usr/texbin/?($|:)"))
          && texbinPath.exists())
      {
         core::system::addToSystemPath(texbinPath);
      }
   }
#endif

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

   // source TeX R helpers
   return module_context::sourceModuleRFile("SessionTeX.R");
}


} // namespace tex
} // namespace modules
} // namesapce session

