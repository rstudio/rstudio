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


struct Texi2DviInfo
{
   bool empty() const { return programFilePath.empty(); }

   FilePath programFilePath;
   const std::string versionInfo;
};

Texi2DviInfo texi2DviInfo()
{
   // get the path to the texi2dvi binary
   FilePath programFilePath = module_context::findProgram("texi2dvi");
   if (programFilePath.empty())
      return Texi2DviInfo();

   // this is enough to return so setup the return structure
   Texi2DviInfo t2dviInfo;
   t2dviInfo.programFilePath = programFilePath;

   // try to get version info from it
   core::system::ProcessResult result;
   Error error = core::system::runProgram(
                  string_utils::utf8ToSystem(programFilePath.absolutePath()),
                  core::shell_utils::ShellArgs() << "--version",
                  "",
                  core::system::ProcessOptions(),
                  &result);
   if (error)
      LOG_ERROR(error);
   else if (result.exitStatus != EXIT_SUCCESS)
      LOG_ERROR_MESSAGE("Error probing for texi2dvi version: "+ result.stdErr);

   // return what we have
   return t2dviInfo;
}

// set of environment variables to customize pdflatex invocation
// includes both the core PDFLATEX command (which maps to the location
// of the custom rstudio-pdflatex script) as well as environment
// variables required to pass options to the script
core::system::Options pdfLatexEnvVars(
                           const tex::pdflatex::PdfLatexOptions& options)
{
   core::system::Options envVars;

   // executable
   FilePath pdfLatexPath;
   std::string pdfLatexEnv = core::system::getenv("PDFLATEX");
   if (!pdfLatexEnv.empty())
   {
      pdfLatexPath = FilePath(pdfLatexEnv);
   }
   else
   {
      pdfLatexPath = module_context::findProgram("pdflatex");
   }
   envVars.push_back(std::make_pair("RS_PDFLATEX",
                     string_utils::utf8ToSystem(pdfLatexPath.absolutePath())));

   // options
   boost::format fmt("RS_PDFLATEX_OPTION_%1%");
   int n = 1;
   if (options.fileLineError)
   {
      envVars.push_back(std::make_pair(boost::str(fmt % n++),
                                       pdflatex::kFileLineErrorOption));
   }
   if (options.syncTex)
   {
      envVars.push_back(std::make_pair(boost::str(fmt % n++),
                                       pdflatex::kSynctexOption));
   }

   // rstudio-pdflatex script
   FilePath texScriptsPath = session::options().texScriptsPath();
   FilePath scriptPath = texScriptsPath.complete("rstudio-pdflatex" +
                                                   std::string(kScriptEx));
   std::string path = string_utils::utf8ToSystem(scriptPath.absolutePath());
   envVars.push_back(std::make_pair("PDFLATEX", path));

   // return envVars
   return envVars;
}


core::system::Options environmentVars(
                           const std::string& versionInfo,
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
   core::system::Options pdfLatexVars = pdfLatexEnvVars(pdfLatexOptions);
   std::copy(pdfLatexVars.begin(),
             pdfLatexVars.end(),
             std::back_inserter(envVars));

   return envVars;
}

shell_utils::ShellArgs shellArgs(const std::string& texVersionInfo)
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
   if (texVersionInfo.find("MiKTeX") != std::string::npos)
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


Error texToPdf(const tex::pdflatex::PdfLatexOptions& options,
               const FilePath& texFilePath,
               core::system::ProcessResult* pResult)
{
   Texi2DviInfo t2dviInfo = texi2DviInfo();
   if (t2dviInfo.empty())
      return core::fileNotFoundError("texi2dvi", ERROR_LOCATION);

   return utils::runTexCompile(t2dviInfo.programFilePath,
                               environmentVars(t2dviInfo.versionInfo, options),
                               shellArgs(t2dviInfo.versionInfo),
                               texFilePath,
                               pResult);
}

} // namespace texi2dvi
} // namespace tex
} // namespace modules
} // namesapce session

