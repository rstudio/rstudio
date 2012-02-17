/*
 * SessionRnwWeave.cpp
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

#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionRnwConcordance.hpp"
#include "SessionCompilePdfSupervisor.hpp"

using namespace core;
using namespace session::modules::tex::rnw_concordance;

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

   virtual bool isInstalled() const = 0;

   virtual std::vector<std::string> commandArgs(
                                    const std::string& file) const = 0;

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
         if (boost::regex_match(lines[i], match, re))
            chunkLineNumbers.push_back(i+1);
      }

      // determine chunk number and error message
      boost::regex cre(
         "[\\w]+:[\\s]+chunk[\\s]+(\\d+)[^\n]+\n[^:]+:[\\s]*[\n]?([^\n]+)\n");
      if (boost::regex_search(output, match, cre))
      {
         std::string match1(match[1]);
         std::string match2(match[2]);
         std::size_t chunk = core::safe_convert::stringTo<int>(match1, 0);
         std::string msg = boost::algorithm::trim_copy(match2);
         if (chunk > 0 && chunk <= chunkLineNumbers.size())
         {
            boost::format fmt("(chunk %1%) %2%");
            core::tex::LogEntry logEntry(core::tex::LogEntry::Error,
                                         rnwFilePath,
                                         chunkLineNumbers[chunk-1],
                                         boost::str(fmt % chunk % msg));
            pLogEntries->push_back(logEntry);
         }
      }

      return Success();
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

#ifdef _WIN32
   virtual std::vector<std::string> commandArgs(const std::string& file) const
   {
      std::vector<std::string> args;
      std::string sweaveCmd = "\"Sweave('" + file + "')\"";
      args.push_back("-e");
      args.push_back(sweaveCmd);
      args.push_back("--silent");
      return args;
   }
#else
   virtual std::vector<std::string> commandArgs(const std::string& file) const
   {
      std::vector<std::string> args;
      args.push_back("CMD");
      args.push_back("Sweave");
      args.push_back(file);
      return args;
   }
#endif
};

class RnwExternalWeave : public RnwWeave
{
public:
   RnwExternalWeave(const std::string& name,
                    const std::string& packageName,
                    const std::string& cmdFmt)
     : RnwWeave(name, packageName), cmdFmt_(cmdFmt)
   {
   }

   virtual bool isInstalled() const
   {
      bool installed;
      r::exec::RFunction func(".rs.isPackageInstalled", packageName());
      Error error = func.call(&installed);
      return !error ? installed : false;
   }

   virtual std::vector<std::string> commandArgs(const std::string& file) const
   {
      std::vector<std::string> args;
      args.push_back("--silent");
      args.push_back("-e");
      std::string cmd = boost::str(boost::format(cmdFmt_) % file);
      args.push_back(cmd);
      return args;
   }

private:
   std::string cmdFmt_;
};

class RnwPgfSweave : public RnwExternalWeave
{
public:
   RnwPgfSweave()
      : RnwExternalWeave(
            "pgfSweave",
            "pgfSweave",
            "require(pgfSweave); pgfSweave('%1%', compile.tex = FALSE)")
   {
   }

   virtual bool injectConcordance() const { return false; }
};

class RnwKnitr : public RnwExternalWeave
{
public:
   RnwKnitr()
      : RnwExternalWeave("knitr",
                         "knitr",
                         "require(knitr); knit('%1%')")
   {
   }

   virtual bool injectConcordance() const { return false; }

   virtual core::Error parseOutputForErrors(
                                    const std::string& output,
                                    const core::FilePath& rnwFilePath,
                                    core::tex::LogEntries* pLogEntries) const
   {
      return Success();
   }
};


class RnwWeaveRegistry : boost::noncopyable
{
private:
   RnwWeaveRegistry()
   {
      weaveTypes_.push_back(boost::shared_ptr<RnwWeave>(new RnwSweave()));
      weaveTypes_.push_back(boost::shared_ptr<RnwWeave>(new RnwKnitr()));
      weaveTypes_.push_back(boost::shared_ptr<RnwWeave>(new RnwPgfSweave()));
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
      // see if we can pick errors from the output
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

void runWeave(const core::FilePath& rnwPath,
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

   // run the weave
   if (pRnwWeave)
   {
      std::vector<std::string> args = pRnwWeave->commandArgs(
                                                         rnwPath.filename());

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

json::Array supportedTypes()
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
} // namesapce session

