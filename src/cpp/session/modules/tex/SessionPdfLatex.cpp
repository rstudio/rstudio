/*
 * SessionPdfLatex.cpp
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

#include "SessionPdfLatex.hpp"

#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <core/system/Environment.hpp>
#include <core/FileSerializer.hpp>

#include <session/projects/SessionProjects.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionTexUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace pdflatex {

namespace {

class LatexProgramTypes : boost::noncopyable
{
public:
   LatexProgramTypes()
   {
      types_.push_back("pdfLaTeX");
      types_.push_back("XeLaTeX");
   }

   const std::vector<std::string>& allTypes() const
   {
      return types_;
   }

   json::Array allTypesAsJson() const
   {
      json::Array typesJson;
      std::transform(types_.begin(),
                     types_.end(),
                     std::back_inserter(typesJson),
                     json::toJsonString);
      return typesJson;
   }

   bool isValidTypeName(const std::string& name) const
   {
      BOOST_FOREACH(const std::string& type, types_)
      {
         if (boost::algorithm::iequals(name, type))
            return true;
      }

      return false;
   }

   std::string printableTypeNames() const
   {
      if (types_.size() == 1)
         return types_[0];
      else if (types_.size() == 2)
         return types_[0] + " and " + types_[1];
      else
      {
         std::string str;
         for (std::size_t i=0; i<types_.size(); i++)
         {
            str.append(types_[i]);
            if (i != (types_.size() - 1))
               str.append(", ");
            if (i == (types_.size() - 2))
               str.append("and ");
         }
         return str;
      }
   }

private:
   std::vector<std::string> types_;
};

const LatexProgramTypes& programTypes()
{
   static LatexProgramTypes instance;
   return instance;
}

std::string latexProgramMagicComment(
                     const core::tex::TexMagicComments& magicComments)
{
   BOOST_FOREACH(const core::tex::TexMagicComment& mc, magicComments)
   {
      if (boost::algorithm::iequals(mc.scope(), "tex") &&
          (boost::algorithm::iequals(mc.variable(), "program") ||
           boost::algorithm::iequals(mc.variable(), "ts-program")))
      {
         return mc.value();
      }
   }

   return std::string();
}

void setInvalidProgramTypeMessage(const std::string& program,
                                  std::string* pUserErrMsg)
{
   *pUserErrMsg = "Unknown LaTeX program type '" + program +
                  "' specified (valid types are " +
                  programTypes().printableTypeNames() + ")";
}


bool validateLatexProgram(const std::string& program,
                          FilePath* pTexProgramPath,
                          std::string* pUserErrMsg)
{
   // convert to lower case for finding
   std::string programName = string_utils::toLower(program);

   // try to find on the path
   *pTexProgramPath = module_context::findProgram(programName);
   if (pTexProgramPath->empty())
   {
      *pUserErrMsg = "Unabled to find specified LaTeX program '" +
                     program + "' on the system path";
      return false;
   }
   else
   {
      return true;
   }
}


bool validateLatexProgramType(const std::string& programType,
                              std::string* pUserErrMsg)
{
   if (!programTypes().isValidTypeName(programType))
   {
      setInvalidProgramTypeMessage(programType, pUserErrMsg);
      return false;
   }
   else
   {
      return true;
   }
}

void appendEnvVarNotice(std::string* pUserErrMsg)
{
   pUserErrMsg->append(" (the program was specified using the "
                       "RSTUDIO_PDFLATEX environment variable)");
}

shell_utils::ShellArgs shellArgs(const PdfLatexOptions& options)
{
   shell_utils::ShellArgs args;

   if (options.fileLineError)
   {
      if (options.isMikTeX())
         args << kCStyleErrorsOption;
      else
         args << kFileLineErrorOption;
   }
   if (options.syncTex)
   {
      args << kSynctexOption;
   }
   if (options.shellEscape)
   {
      if (options.isMikTeX())
         args << kEnableWrite18Option;
      else
         args << kShellEscapeOption;
   }
   args << "-interaction=nonstopmode";

   return args;
}

FilePath programPath(const std::string& name, const std::string& envOverride)
{
   std::string envProgram = core::system::getenv(envOverride);
   std::string program = envProgram.empty() ? name : envProgram;
   return module_context::findProgram(program);
}



bool lineIncludes(const std::string& line, const boost::regex& regex)
{
    boost::smatch match;
    return regex_utils::search(line, match, regex);
}

int countCitationMisses(const FilePath& logFilePath)
{
   // read the log file
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(logFilePath, &lines);
   if (error)
   {
      LOG_ERROR(error);
      return 0;
   }

   // look for misses
   boost::regex missRegex("Warning:.*Citation.*undefined");
   int misses = std::count_if(lines.begin(),
                              lines.end(),
                              boost::bind(lineIncludes, _1, missRegex));
   return misses;
}

bool logIncludesRerun(const FilePath& logFilePath)
{
   std::string logContents;
   Error error = core::readStringFromFile(logFilePath, &logContents);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return logContents.find("Rerun to get") != std::string::npos;
}

} // anonymous namespace

const char * const kFileLineErrorOption = "-file-line-error";
const char * const kCStyleErrorsOption = "-c-style-errors";
const char * const kShellEscapeOption = "-shell-escape";
const char * const kEnableWrite18Option = "-enable-write18";
const char * const kSynctexOption = "-synctex=1";

bool isInstalled()
{
   return module_context::isPdfLatexInstalled();
}


core::json::Array supportedTypes()
{
   return programTypes().allTypesAsJson();
}


bool latexProgramForFile(const core::tex::TexMagicComments& magicComments,
                         FilePath* pTexProgramPath,
                         std::string* pUserErrMsg)
{
   // get (optional) magic comments and environment variable override
   std::string latexProgramMC = latexProgramMagicComment(magicComments);
   std::string pdflatexEnv = core::system::getenv("RSTUDIO_PDFLATEX");

   // magic comment always takes highest priority
   if (!latexProgramMC.empty())
   {
      // validate magic comment
      if (!validateLatexProgramType(latexProgramMC, pUserErrMsg))
      {
         return false;
      }
      else
      {
         return validateLatexProgram(latexProgramMC,
                                     pTexProgramPath,
                                     pUserErrMsg);
      }
   }

   // next is environment variable
   else if (!pdflatexEnv.empty())
   {
      if (FilePath::isRootPath(pdflatexEnv))
      {
         FilePath texProgramPath(pdflatexEnv);
         if (texProgramPath.exists())
         {
            *pTexProgramPath = texProgramPath;
            return true;
         }
         else
         {
            *pUserErrMsg = "Unabled to find specified LaTeX program " +
                           pdflatexEnv;
            appendEnvVarNotice(pUserErrMsg);
            return false;
         }
      }
      else
      {
         bool validated = validateLatexProgram(pdflatexEnv,
                                               pTexProgramPath,
                                               pUserErrMsg);

         if (!validated)
            appendEnvVarNotice(pUserErrMsg);

         return validated;
      }
   }

   // project or global default setting
   else
   {
      std::string defaultProgram = projects::projectContext().hasProject() ?
                projects::projectContext().config().defaultLatexProgram :
                userSettings().defaultLatexProgram();

      if (!validateLatexProgramType(defaultProgram, pUserErrMsg))
      {
         return false;
      }
      else
      {
         return validateLatexProgram(defaultProgram,
                                     pTexProgramPath,
                                     pUserErrMsg);
      }
   }
}

// this function provides an "emulated" version of texi2dvi for when the
// user has texi2dvi disabled. For example to workaround this bug:
//
//  http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=534458
//
// this code is a port of the simillar logic which exists in the
// tools::texi2dvi function (but the regex for detecting citation
// warnings was made a bit more liberal)
//
core::Error texToPdf(const core::FilePath& texProgramPath,
                     const core::FilePath& texFilePath,
                     const tex::pdflatex::PdfLatexOptions& options,
                     core::system::ProcessResult* pResult)
{
   // input file paths
   FilePath baseFilePath = texFilePath.parent().complete(texFilePath.stem());
   FilePath idxFilePath(baseFilePath.absolutePath() + ".idx");
   FilePath logFilePath(baseFilePath.absolutePath() + ".log");

   // bibtex and makeindex program paths
   FilePath bibtexProgramPath = programPath("bibtex", "BIBTEX");
   FilePath makeindexProgramPath = programPath("makeindex", "MAKEINDEX");

   // args and process options for running bibtex and makeindex
   core::shell_utils::ShellArgs bibtexArgs;
   bibtexArgs << string_utils::utf8ToSystem(baseFilePath.filename());
   core::shell_utils::ShellArgs makeindexArgs;
   makeindexArgs << string_utils::utf8ToSystem(idxFilePath.filename());
   core::system::ProcessOptions procOptions;
   procOptions.environment = utils::rTexInputsEnvVars();
   procOptions.workingDir = texFilePath.parent();

   // run the initial compile
   Error error = utils::runTexCompile(texProgramPath,
                                      utils::rTexInputsEnvVars(),
                                      shellArgs(options),
                                      texFilePath,
                                      pResult);
   if (error)
      return error;

   // count misses
   int misses = countCitationMisses(logFilePath);
   int previousMisses = 0;

   // resolve citation misses and index
   for (int i=0; i<10; i++)
   {
      // run bibtex if necessary
      if (misses > 0 && !bibtexProgramPath.empty())
      {
         core::system::ProcessResult result;
         Error error = core::system::runProgram(
               string_utils::utf8ToSystem(bibtexProgramPath.absolutePath()),
               bibtexArgs,
               "",
               procOptions,
               pResult);
         if (error)
            LOG_ERROR(error);
         else if (pResult->exitStatus != EXIT_SUCCESS)
            return Success(); // pass error state on to caller
      }
      previousMisses = misses;

      // run makeindex if necessary
      if (idxFilePath.exists() && !makeindexProgramPath.empty())
      {
         Error error = core::system::runProgram(
               string_utils::utf8ToSystem(makeindexProgramPath.absolutePath()),
               makeindexArgs,
               "",
               procOptions,
               pResult);
         if (error)
            LOG_ERROR(error);
         else if (pResult->exitStatus != EXIT_SUCCESS)
            return Success(); // pass error state on to caller
      }

      // re-run latex
      Error error = utils::runTexCompile(texProgramPath,
                                         utils::rTexInputsEnvVars(),
                                         shellArgs(options),
                                         texFilePath,
                                         pResult);
      if (error)
         return error;

      // count misses
      misses = countCitationMisses(logFilePath);

      // if there is no change in misses and there is no "Rerun to get"
      // in the log file then break
      if ((misses == previousMisses) && !logIncludesRerun(logFilePath))
         break;
   }

   return Success();
}


} // namespace pdflatex
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

