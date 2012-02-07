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

#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <core/system/Environment.hpp>
#include <core/FileSerializer.hpp>

#include <session/projects/SessionProjects.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionTexUtils.hpp"

using namespace core;

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

FilePath programPath(const std::string& name, const std::string& envOverride)
{
   std::string envProgram = core::system::getenv(envOverride);
   std::string program = envProgram.empty() ? name : envProgram;
   return module_context::findProgram(program);
}



bool lineIncludes(const std::string& line, const boost::regex& regex)
{
    boost::smatch match;
    return boost::regex_search(line, match, regex);
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


core::json::Array supportedTypes()
{
   return programTypes().allTypesAsJson();
}


bool latexProgramForFile(const core::tex::TexMagicComments& magicComments,
                         FilePath* pTexProgramPath,
                         std::string* pUserErrMsg)
{
   // magic comment always takes highest priority
   std::string latexProgramMC = latexProgramMagicComment(magicComments);
   if (!latexProgramMC.empty())
   {
      // validate magic comment
      if (!programTypes().isValidTypeName(latexProgramMC))
      {
         *pUserErrMsg =
            "Unknown LaTeX program type '" + latexProgramMC +
            "' specified (valid types are " +
            programTypes().printableTypeNames() + ")";

         return false;
      }
      else
      {
         return validateLatexProgram(latexProgramMC,
                                     pTexProgramPath,
                                     pUserErrMsg);
      }
   }

   // project level setting next
   else if (projects::projectContext().hasProject())
   {
      return validateLatexProgram(
                  projects::projectContext().config().defaultLatexProgram,
                  pTexProgramPath,
                  pUserErrMsg);
   }

   // finally global setting if we aren't in a project
   else
   {
      return validateLatexProgram(
                  userSettings().defaultLatexProgram(),
                  pTexProgramPath,
                  pUserErrMsg);
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
               &result);
         if (error)
            LOG_ERROR(error);
         else if (result.exitStatus != EXIT_SUCCESS)
            LOG_ERROR_MESSAGE(result.stdErr);
      }
      previousMisses = misses;

      // run makeindex if necessary
      if (idxFilePath.exists() && !makeindexProgramPath.empty())
      {
         core::system::ProcessResult result;
         Error error = core::system::runProgram(
               string_utils::utf8ToSystem(makeindexProgramPath.absolutePath()),
               makeindexArgs,
               "",
               procOptions,
               &result);
         if (error)
            LOG_ERROR(error);
         else if (result.exitStatus != EXIT_SUCCESS)
            LOG_ERROR_MESSAGE(result.stdErr);
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
} // namesapce session

