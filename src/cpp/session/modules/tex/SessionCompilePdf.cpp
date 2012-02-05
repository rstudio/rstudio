/*
 * SessionCompilePdf.cpp
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

#include "SessionCompilePdf.hpp"

#include <core/FilePath.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionPdfLatex.hpp"
#include "SessionTexi2Dvi.hpp"

// TODO: investigate other texi2dvi and pdflatex options
//         -- shell-escape
//         -- clean
//         -- alternative output file location

// TODO: emulate texi2dvi on linux to workaround debian tilde
//       escaping bug (http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=534458)

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace compile_pdf {

namespace {

SEXP rs_texToPdf(SEXP filePathSEXP)
{
   FilePath filePath = module_context::resolveAliasedPath(
                           r::sexp::asString(filePathSEXP));

   pdflatex::PdfLatexOptions options;
   options.fileLineError = true;
   options.syncTex = true;

#if defined(_WIN32) || defined(__APPLE__)
   Error error = tex::texi2dvi::texToPdf(options, filePath);
#else
   Error error = tex::texi2dvi::texToPdf(options, filePath);
#endif

  if (error)
      r::exec::warning("Unable to compile pdf: " + error.summary());

   return R_NilValue;
}


FilePath pdfPathForTexPath(const FilePath& texPath)
{
   return texPath.parent().complete(texPath.stem() + ".pdf");
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
   R_CallMethodDef runTexToPdfMethodDef;
   runTexToPdfMethodDef.name = "rs_texToPdf" ;
   runTexToPdfMethodDef.fun = (DL_FUNC) rs_texToPdf ;
   runTexToPdfMethodDef.numArgs = 1;
   r::routines::addCallMethod(runTexToPdfMethodDef);


   R_CallMethodDef viewPdfMethodDef ;
   viewPdfMethodDef.name = "rs_viewPdf" ;
   viewPdfMethodDef.fun = (DL_FUNC) rs_viewPdf ;
   viewPdfMethodDef.numArgs = 1;
   r::routines::addCallMethod(viewPdfMethodDef);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionCompilePdf.R"));
   return initBlock.execute();

}


} // namespace compile_pdf
} // namespace tex
} // namespace modules
} // namesapce session

