/*
 * SessionTexi2Dvi.cpp
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

#include "SessionTexi2Dvi.hpp"

#include <boost/format.hpp>

#include <core/system/ShellUtils.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionPdfLatex.hpp"
#include "SessionTexUtils.hpp"


// platform specific constants
#ifdef _WIN32
const char * const kScriptEx = ".cmd";
#else
const char * const kScriptEx = ".sh";
#endif


using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace texi2dvi {

namespace {


FilePath texi2DviPath()
{
   return module_context::findProgram("texi2dvi");
}

// set of environment variables to customize pdflatex invocation
// includes both the core PDFLATEX command (which maps to the location
// of the custom rstudio-pdflatex script) as well as environment
// variables required to pass options to the script
core::system::Options pdfLatexEnvVars(
                           const core::FilePath& texProgramPath,
                           const tex::pdflatex::PdfLatexOptions& options)
{
   core::system::Options envVars;

   envVars.push_back(
         std::make_pair("RS_PDFLATEX",
         string_utils::utf8ToSystem(texProgramPath.absolutePath())));

   // options
   boost::format fmt("RS_PDFLATEX_OPTION_%1%");
   int n = 1;
   if (options.fileLineError)
   {
      std::string option = options.isMikTeX() ? pdflatex::kCStyleErrorsOption :
                                                pdflatex::kFileLineErrorOption;
      envVars.push_back(std::make_pair(boost::str(fmt % n++), option));
   }
   if (options.syncTex)
   {
      envVars.push_back(std::make_pair(boost::str(fmt % n++),
                                       pdflatex::kSynctexOption));
   }
   if (options.shellEscape)
   {
      std::string option = options.isMikTeX() ? pdflatex::kEnableWrite18Option :
                                                pdflatex::kShellEscapeOption;
      envVars.push_back(std::make_pair(boost::str(fmt % n++), option));
   }

   // rspdflatex binary
   FilePath pdflatexPath(session::options().rspdflatexPath());
   std::string path = string_utils::utf8ToSystem(pdflatexPath.absolutePath());
   envVars.push_back(std::make_pair("PDFLATEX", path));

   // return envVars
   return envVars;
}


core::system::Options environmentVars(
                           const core::FilePath& texProgramPath,
                           const pdflatex::PdfLatexOptions& pdfLatexOptions)
{
   // start with inputs (TEXINPUTS, BIBINPUTS, BSTINPUTS)
   core::system::Options envVars = utils::rTexInputsEnvVars();

   // The tools::texi2dvi function sets these environment variables (on posix)
   // so they are presumably there as workarounds-- it would be good to
   // understand exactly why they are defined and consequently whether we also
   // need to define them
#ifndef _WIN32
   envVars.push_back(std::make_pair("TEXINDY", "false"));
   envVars.push_back(std::make_pair("LC_COLLATE", "C"));
#endif

   // env vars required to customize invocation of pdflatex
   core::system::Options pdfLatexVars = pdfLatexEnvVars(texProgramPath,
                                                        pdfLatexOptions);
   std::copy(pdfLatexVars.begin(),
             pdfLatexVars.end(),
             std::back_inserter(envVars));

   return envVars;
}

shell_utils::ShellArgs shellArgs(
                     const pdflatex::PdfLatexOptions& pdfLatexOptions)
{
   shell_utils::ShellArgs args;

   args << "--pdf";
   args << "--quiet";

#ifdef _WIN32
   // This emulates two behaviors found in tools::texi2dvi:
   //
   //   (1) Detecting MikTeX and in that case passing TEXINPUTS and
   //       BSTINPUTS (but not BIBINPUTS) on the texi2devi command line
   //
   //   (2) Substituting any instances of \ in the paths with /
   //
   if (pdfLatexOptions.isMikTeX())
   {
      utils::RTexmfPaths texmfPaths = utils::rTexmfPaths();
      if (!texmfPaths.empty())
      {
         std::string texInputs = string_utils::utf8ToSystem(
                                    texmfPaths.texInputsPath.absolutePath());
         boost::algorithm::replace_all(texInputs, "\\", "/");
         args << "-I" << texInputs;

         std::string bstInputs = string_utils::utf8ToSystem(
                                    texmfPaths.bstInputsPath.absolutePath());
         boost::algorithm::replace_all(bstInputs, "\\", "/");
         args << "-I" << bstInputs;
      }
   }
#endif

   return args;
}

} // anonymous namespace


bool isAvailable()
{
   return !module_context::findProgram("texi2dvi").empty();
}

core::Error texToPdf(
         const core::FilePath& texProgramPath,
         const core::FilePath& texFilePath,
         const tex::pdflatex::PdfLatexOptions& options,
         const boost::function<void(int,const std::string&)>& onExited)
{
   FilePath texi2DviProgramFilePath = texi2DviPath();
   if (texi2DviProgramFilePath.empty())
      return core::fileNotFoundError("texi2dvi", ERROR_LOCATION);

   return utils::runTexCompile(texi2DviProgramFilePath,
                               environmentVars(texProgramPath,
                                               options),
                               shellArgs(options),
                               texFilePath,
                               onExited);
}

} // namespace texi2dvi
} // namespace tex
} // namespace modules
} // namesapce session

