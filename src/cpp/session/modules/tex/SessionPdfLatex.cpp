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


#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace pdflatex {

#ifdef _WIN32
const char * const kFileLineErrorOption = "-c-style-errors";
#else
const char * const kFileLineErrorOption = "-file-line-error";
#endif

const char * const kSynctexOption = "-synctex";

bool isInstalled()
{
   return !module_context::findProgram("pdflatex").empty();
}


core::Error texToPdf(const PdfLatexOptions& options,
                     const core::FilePath& texFilePath)
{

   return Success();
}


} // namespace pdflatex
} // namespace tex
} // namespace modules
} // namesapce session

