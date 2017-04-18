/*
 * SessionRnwWeave.cpp
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

#include "SessionRnwWeave.hpp"

#include <boost/utility.hpp>
#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <boost/algorithm/string/split.hpp>

#include <core/FileSerializer.hpp>

#include <core/tex/TexLogParser.hpp>
#include <core/tex/TexMagicComment.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionRnwConcordance.hpp"
#include "SessionCompilePdfSupervisor.hpp"

using namespace rstudio::core;
using namespace rstudio::session::modules::tex::rnw_concordance;

namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace rnw_weave {

namespace {

Error rBinDir(FilePath* pRBinDir)
{
   std::string rHomeBin;
   r::exec::RFunction rHomeBinFunc("R.home", "bin");
   Error error = rHomeBinFunc.call(&rHomeBin);
   if (error)
      return error;

   *pRBinDir = FilePath(rHomeBin);
   return Success();
}


class RnwWeave : boost::noncopyable
{
public:
   explicit RnwWeave(const std::string& name,
                     const std::string& packageName = "")
   {
      name_ = name;
      packageName_ = !packageName.empty() ? packageName : name;
   }

   virtual ~RnwWeave()
   {
   }

   // COPYING: noncopyable (to prevent slicing)

   const std::string& name() const { return name_; }
   const std::string& packageName() const { return packageName_; }

   virtual bool injectConcordance() const = 0;

   virtual bool usesCodeForOptions() const = 0;

   virtual bool forceEchoOnExec() const = 0;

   virtual bool isInstalled() const = 0;

   virtual core::json::Value chunkOptions() const = 0;

   // tangle the passed file (note that the implementation can assume
   // that the working directory is already set to that of the file)
   virtual core::Error tangle(const std::string& file,
                              const std::string& encoding) = 0;

   virtual std::vector<std::string> commandArgs(
                                       const std::string& file,
                                       const std::string& encoding,
                                       const std::string& driver) const
   {
      std::vector<std::string> args;
      args.push_back("--slave");
      args.push_back("--no-save");
      args.push_back("--no-restore");
      args.push_back("-e");
      std::string cmd = "grDevices::pdf.options(useDingbats = FALSE); "
                        + weaveCommand(file, encoding, driver);
      args.push_back(cmd);
      return args;
   }

   virtual std::string weaveCommand(const std::string& file,
                                    const std::string& encoding,
                                    const std::string& driver) const = 0;

   virtual core::Error parseOutputForErrors(
                                    const std::string& output,
                                    const core::FilePath& rnwFilePath,
                                    core::tex::LogEntries* pLogEntries) const
   {
      // split into lines so we can determine the line numbers for the chunks
      // NOTE: will need to read this using global/project encoding if we
      // want to look for text outside of theh orignal error parsing
      // scenario (which only required ascii)
      std::string rnwContents;
      Error error = core::readStringFromFile(rnwFilePath, &rnwContents);
      if (error)
         return error;
      std::vector<std::string> lines;
      boost::algorithm::split(lines, rnwContents, boost::is_any_of("\n"));

      // determine line numbers
      boost::regex re("^<<(.*)>>=.*");
      boost::smatch match;
      std::vector<int> chunkLineNumbers;
      for (std::size_t i=0; i<lines.size(); i++)
      {
         if (regex_utils::match(lines[i], match, re))
            chunkLineNumbers.push_back(i+1);
      }

      // determine chunk number and error message
      boost::regex cre(
         "[\\w]+:[\\s]+chunk[\\s]+(\\d+)[^\n]+\n[^:]+:[\\s]*[\n]?([^\n]+)\n");
      if (regex_utils::search(output, match, cre))
      {
         std::string match1(match[1]);
         std::string match2(match[2]);
         std::size_t chunk = core::safe_convert::stringTo<int>(match1, 0);
         std::string msg = boost::algorithm::trim_copy(match2);
         if (chunk > 0 && chunk <= chunkLineNumbers.size())
         {
            boost::format fmt("(chunk %1%) %2%");
            core::tex::LogEntry logEntry(FilePath(),
                                         -1,
                                         core::tex::LogEntry::Error,
                                         rnwFilePath,
                                         chunkLineNumbers[chunk-1],
                                         boost::str(fmt % chunk % msg));
            pLogEntries->push_back(logEntry);
         }
      }

      return Success();
   }

protected:
   core::json::Value chunkOptions(const std::string& chunkFunction) const
   {
      SEXP optionsSEXP;
      r::sexp::Protect rProtect;
      r::exec::RFunction optionsFunc(chunkFunction);
      Error error = optionsFunc.call(&optionsSEXP, &rProtect);
      if (error)
      {
         LOG_ERROR(error);
         return json::Value();
      }

      core::json::Value optionsJson;
      error = r::json::jsonValueFromList(optionsSEXP, &optionsJson);
      if (error)
         LOG_ERROR(error);

      return optionsJson;
   }

private:
   std::string name_;
   std::string packageName_;
};

class RnwSweave : public RnwWeave
{
public:
   RnwSweave()
      : RnwWeave("Sweave")
   {
   }

   virtual bool isInstalled() const { return true; }

   virtual bool injectConcordance() const { return true; }

   virtual bool usesCodeForOptions() const { return false; }

   virtual bool forceEchoOnExec() const { return false; }

   virtual core::json::Value chunkOptions() const
   {
      return RnwWeave::chunkOptions(".rs.sweaveChunkOptions");
   }

   virtual core::Error tangle(const std::string& file,
                              const std::string& encoding)
   {
      r::exec::RFunction stangle("utils:::Stangle");
      stangle.addParam(file);
      if (!encoding.empty())
         stangle.addParam("encoding", encoding);
      return stangle.call();
   }

   virtual std::string weaveCommand(const std::string& file,
                                    const std::string& encoding,
                                    const std::string& driver) const
   {
      std::string cmd = "utils::Sweave('" + file + "'";

      if (!driver.empty())
         cmd += ", driver = " + driver + "";

      if (!encoding.empty())
         cmd += (", encoding='" + encoding + "'");

      cmd += ")";

      return cmd;
   }
};


class RnwKnitr : public RnwWeave
{
public:
   RnwKnitr()
      : RnwWeave("knitr", "knitr")
   {
   }

   virtual bool injectConcordance() const { return false; }

   virtual bool usesCodeForOptions() const { return true; }

   virtual bool forceEchoOnExec() const { return true; }

   virtual bool isInstalled() const
   {
      return module_context::isPackageInstalled(packageName());
   }

   virtual std::string weaveCommand(const std::string& file,
                                    const std::string& encoding,
                                    const std::string& driver) const
   {
      std::string format = "require(knitr); ";
      if (userSettings().alwaysEnableRnwCorcordance())
         format += "opts_knit$set(concordance = TRUE); ";
      format += "knit('%1%'";
      std::string cmd = boost::str(boost::format(format) % file);

      if (!encoding.empty())
         cmd += (", encoding='" + encoding + "'");

      cmd += ")";

      return cmd;
   }

   virtual core::Error parseOutputForErrors(
                                    const std::string& output,
                                    const core::FilePath& rnwFilePath,
                                    core::tex::LogEntries* pLogEntries) const
   {
      // older error style
      boost::regex errRe("^\\s*Quitting from lines ([0-9]+)-([0-9]+): "
                          "(?:Error in [a-z]+\\([a-z=, ]+\\) : \n?)?"
                          "([^\n]+)$");

      // new error style
      boost::regex newErrRe("^\\s*Quitting from lines ([0-9]+)-([0-9]+) "
                            "\\((.*?)\\)\\s*\\n(.*?)\\n");

      boost::smatch match;
      if (regex_utils::search(output, match, errRe))
      {
         // extract error info
         int lineBegin = safe_convert::stringTo<int>(match[1], -1);
         std::string message = match[3];

         // check to see if there is a parse error which provides more
         // precise pinpointing of the line
         boost::regex parseRe("^\\s*<text>:([0-9]+):[0-9]+: (.+)$");
         if (regex_utils::match(message, match, parseRe))
         {
            lineBegin += safe_convert::stringTo<int>(match[1], -1);
            message = match[2];
         }

         core::tex::LogEntry logEntry(FilePath(),
                                      -1,
                                      core::tex::LogEntry::Error,
                                      rnwFilePath,
                                      lineBegin,
                                      message);
         pLogEntries->push_back(logEntry);
      }

      else if (regex_utils::search(output, match, newErrRe))
      {
         // extract error info
         int lineBegin = safe_convert::stringTo<int>(match[1], -1);
         std::string message = match[4];

         core::tex::LogEntry logEntry(FilePath(),
                                      -1,
                                      core::tex::LogEntry::Error,
                                      rnwFilePath,
                                      lineBegin,
                                      message);
         pLogEntries->push_back(logEntry);
      }

      return Success();
   }

   virtual core::json::Value chunkOptions() const
   {
      if (isInstalled())
         return RnwWeave::chunkOptions(".rs.knitrChunkOptions");
      else
         return json::Value();
   }

   virtual core::Error tangle(const std::string& file,
                              const std::string& encoding)
   {
      r::session::utils::SuppressOutputInScope suppressOutput;
      r::exec::RFunction purlFunc("knitr:::purl");
      purlFunc.addParam("input", file);
      purlFunc.addParam("output", file + ".R");
      if (!encoding.empty())
         purlFunc.addParam("encoding", encoding);
      return purlFunc.call();
   }
};


class RnwWeaveRegistry : boost::noncopyable
{
private:
   RnwWeaveRegistry()
   {
      weaveTypes_.push_back(boost::shared_ptr<RnwWeave>(new RnwSweave()));
      weaveTypes_.push_back(boost::shared_ptr<RnwWeave>(new RnwKnitr()));
   }
   friend const RnwWeaveRegistry& weaveRegistry();

public:
   typedef std::vector<boost::shared_ptr<RnwWeave> > RnwWeaveTypes;


public:
   std::string printableTypeNames() const
   {
      std::string str;
      for (std::size_t i=0; i<weaveTypes_.size(); i++)
      {
         str.append(weaveTypes_[i]->name());
         if (i != (weaveTypes_.size() - 1))
            str.append(", ");
         if (i == (weaveTypes_.size() - 2))
            str.append("and ");
      }
      return str;
   }

   RnwWeaveTypes weaveTypes() const { return weaveTypes_; }

   boost::shared_ptr<RnwWeave> findTypeIgnoreCase(const std::string& name)
                                                                        const
   {
      BOOST_FOREACH(boost::shared_ptr<RnwWeave> weaveType, weaveTypes_)
      {
         if (boost::algorithm::iequals(weaveType->name(), name))
            return weaveType;
      }

      return boost::shared_ptr<RnwWeave>();
   }

private:
   RnwWeaveTypes weaveTypes_;
};


const RnwWeaveRegistry& weaveRegistry()
{
   static RnwWeaveRegistry instance;
   return instance;
}

std::string weaveTypeForFile(const core::tex::TexMagicComments& magicComments)
{
   // first see if the file contains an rnw weave magic comment
   BOOST_FOREACH(const core::tex::TexMagicComment& mc, magicComments)
   {
      if (boost::algorithm::iequals(mc.scope(), "rnw") &&
          boost::algorithm::iequals(mc.variable(), "weave"))
      {
         return mc.value();
      }
   }

   // if we didn't find a directive then inspect project & global config
   if (projects::projectContext().hasProject())
      return projects::projectContext().config().defaultSweaveEngine;
   else
      return userSettings().defaultSweaveEngine();
}

std::string driverForFile(const core::tex::TexMagicComments& magicComments)
{
   BOOST_FOREACH(const core::tex::TexMagicComment& mc, magicComments)
   {
      if (boost::algorithm::iequals(mc.scope(), "rnw") &&
          boost::algorithm::iequals(mc.variable(), "driver"))
      {
         return mc.value();
      }
   }

   return std::string();
}


void onWeaveProcessExit(boost::shared_ptr<RnwWeave> pRnwWeave,
                        int exitCode,
                        const std::string& output,
                        const FilePath& rnwPath,
                        const CompletedFunction& onCompleted)
{
   if (exitCode == EXIT_SUCCESS)
   {
      // pickup concordance if there is any
      rnw_concordance::Concordances concordances;
      Error error = rnw_concordance::readIfExists(rnwPath, &concordances);
      if (error)
         LOG_ERROR(error);

      // return success
      onCompleted(Result::success(concordances));
   }
   else
   {
      // parse for errors
      core::tex::LogEntries entries;
      Error error = pRnwWeave->parseOutputForErrors(output, rnwPath, &entries);
      if (error)
         LOG_ERROR(error);

      if (!entries.empty())
      {
         onCompleted(Result::error(entries));
      }
      else
      {
         // don't return an error message because the process almost
         // certainly already printed something to stderr
         onCompleted(Result::error(std::string()));
      }
   }
}

} // anonymous namespace

void runTangle(const std::string& filePath,
               const std::string& encoding,
               const std::string& rnwWeave)
{
   using namespace module_context;
   boost::shared_ptr<RnwWeave> pWeave =
                         weaveRegistry().findTypeIgnoreCase(rnwWeave);
   if (!pWeave)
   {
      consoleWriteError("Unknown Rnw weave type: " + rnwWeave + "\n");
   }
   else
   {
      Error error = pWeave->tangle(filePath, encoding);
      if (error)
         consoleWriteError(r::endUserErrorMessage(error) + "\n");
   }
}

void runWeave(const core::FilePath& rnwPath,
              const std::string& encoding,
              const core::tex::TexMagicComments& magicComments,
              const boost::function<void(const std::string&)>& onOutput,
              const CompletedFunction& onCompleted)
{
   // remove existing concordance file (if any)
   rnw_concordance::removePrevious(rnwPath);

   // get the R bin dir
   FilePath rBin;
   Error error = rBinDir(&rBin);
   if (error)
   {
      LOG_ERROR(error);
      onCompleted(Result::error(error.summary()));
      return;
   }

   // R exe path differs by platform
#ifdef _WIN32
   FilePath rBinPath = rBin.complete("Rterm.exe");
#else
   FilePath rBinPath = rBin.complete("R");
#endif

   // determine the active sweave engine
   std::string weaveType = weaveTypeForFile(magicComments);
   boost::shared_ptr<RnwWeave> pRnwWeave = weaveRegistry()
                                             .findTypeIgnoreCase(weaveType);

   // determine the driver (if any)
   std::string driver = driverForFile(magicComments);

   // run the weave
   if (pRnwWeave)
   {
      std::vector<std::string> args = pRnwWeave->commandArgs(
                                                         rnwPath.filename(),
                                                         encoding,
                                                         driver);

      // call back-end
      Error error = compile_pdf_supervisor::runProgram(
               rBinPath,
               args,
               core::system::Options(),
               rnwPath.parent(),
               onOutput,
               boost::bind(onWeaveProcessExit,
                                 pRnwWeave, _1, _2, rnwPath, onCompleted));
      if (error)
      {
         LOG_ERROR(error);
         onCompleted(Result::error(error.summary()));
      }
   }
   else
   {
      onCompleted(Result::error(
         "Unknown Rnw weave method '" + weaveType + "' specified (valid " +
         "values are " + weaveRegistry().printableTypeNames() + ")"));
   }
}

core::json::Value chunkOptions(const std::string& weaveType)
{
   boost::shared_ptr<RnwWeave> pRnwWeave = weaveRegistry()
                                             .findTypeIgnoreCase(weaveType);
   if (pRnwWeave)
      return pRnwWeave->chunkOptions();
   else
      return core::json::Value();
}

core::json::Array supportedTypes()
{
   // query for list of supported types
   json::Array array;
   BOOST_FOREACH(boost::shared_ptr<RnwWeave> pRnwWeave,
                 weaveRegistry().weaveTypes())
   {
      json::Object object;
      object["name"] = pRnwWeave->name();
      object["package_name"] = pRnwWeave->packageName();
      object["inject_concordance"] = pRnwWeave->injectConcordance();
      object["uses_code_for_options"] = pRnwWeave->usesCodeForOptions();
      object["force_echo_on_exec"] = pRnwWeave->forceEchoOnExec();
      array.push_back(object);
   }
   return array;
}

void getTypesInstalledStatus(json::Object* pObj)
{
   // query for status of all rnw weave types
   BOOST_FOREACH(boost::shared_ptr<RnwWeave> pRnwWeave,
                 weaveRegistry().weaveTypes())
   {
      std::string n = string_utils::toLower(pRnwWeave->name() + "_installed");
      (*pObj)[n] = pRnwWeave->isInstalled();
   }
}

} // namespace rnw_weave
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

