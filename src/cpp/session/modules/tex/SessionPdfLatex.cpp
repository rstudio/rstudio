/*
 * SessionPdfLatex.cpp
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

#include "SessionPdfLatex.hpp"

#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionTexUtils.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace pdflatex {

namespace {

shell_utils::ShellArgs shellArgs(const PdfLatexOptions& options)
{
   shell_utils::ShellArgs args;

   if (options.fileLineError)
      args << kFileLineErrorOption;
   if (options.syncTex)
      args << kSynctexOption;

   args << "-interaction=nonstopmode";

   return args;
}

} // anonymous namespace

#ifdef _WIN32
const char * const kFileLineErrorOption = "-c-style-errors";
#else
const char * const kFileLineErrorOption = "-file-line-error";
#endif

const char * const kSynctexOption = "-synctex=-1";

bool isInstalled()
{
   return !module_context::findProgram("pdflatex").empty();
}


core::Error texToPdf(const PdfLatexOptions& options,
                     const core::FilePath& texFilePath,
                     core::system::ProcessResult* pResult)
{
   FilePath pdfLatexPath;
   std::string pdfLatexEnv = core::system::getenv("PDFLATEX");
   if (!pdfLatexEnv.empty())
   {
      pdfLatexPath = FilePath(pdfLatexEnv);
   }
   else
   {
      pdfLatexPath = module_context::findProgram("pdflatex");
      if (pdfLatexPath.empty())
         return core::fileNotFoundError("pdflatex", ERROR_LOCATION);
   }

   return utils::runTexCompile(pdfLatexPath,
                               utils::rTexInputsEnvVars(),
                               shellArgs(options),
                               texFilePath,
                               pResult);
}


} // namespace pdflatex
} // namespace tex
} // namespace modules
} // namesapce session

