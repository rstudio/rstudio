/*
 * SessionPdfLatex.hpp
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

#ifndef SESSION_MODULES_TEX_PDFLATEX_HPP
#define SESSION_MODULES_TEX_PDFLATEX_HPP

#include <core/FilePath.hpp>

#include <core/json/Json.hpp>

#include <core/tex/TexMagicComment.hpp>

#include <core/system/Types.hpp>
#include <core/system/Process.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace pdflatex {

// NOTE: other potential command line parameters to support:
//  -interaction=batch (but will that work with texi2dvi?)
//  -halt-on-error (same question, does it work with texi2dvi)
//

// NOTE: synctex can be called from the command line as in:
//  sync­tex view –i 25:15:filename.tex –o filename.pdf
//

extern const char * const kFileLineErrorOption;
extern const char * const kCStyleErrorsOption;
extern const char * const kShellEscapeOption;
extern const char * const kEnableWrite18Option;
extern const char * const kSynctexOption;

struct PdfLatexOptions
{
   PdfLatexOptions()
      : fileLineError(false), syncTex(false), shellEscape(false)
   {
   }

   bool isMikTeX() const
   {
      return versionInfo.find("MiKTeX") != std::string::npos;
   }

   bool fileLineError;
   bool syncTex;
   bool shellEscape;
   std::string versionInfo;
};

core::Error texToPdf(const core::FilePath& texProgramPath,
                     const core::FilePath& texFilePath,
                     const tex::pdflatex::PdfLatexOptions& options,
                     core::system::ProcessResult* pResult);

bool isInstalled();

core::json::Array supportedTypes();

bool latexProgramForFile(const core::tex::TexMagicComments& magicComments,
                         core::FilePath* pTexProgramPath,
                         std::string* pUserErrMsg);

} // namespace pdflatex
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_TEX_PDFLATEX_HPP
