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

#include <core/tex/TexMagicComment.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace rnw_weave {

namespace {

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

   virtual bool isInstalled() const = 0;

   virtual std::vector<std::string> commandArgs(
                                    const std::string& file) const = 0;

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
      : RnwExternalWeave("pgfSweave",
                         "pgfSweave",
                         "require(pgfSweave); pgfSweave('%1%')")
   {
   }
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

std::string weaveTypeForFile(const FilePath& rnwPath)
{
   // first see if the file contains an rnw weave magic comment
   std::vector<core::tex::TexMagicComment> magicComments;
   Error error = core::tex::parseMagicComments(rnwPath, &magicComments);
   if (!error)
   {
      BOOST_FOREACH(const core::tex::TexMagicComment& mc, magicComments)
      {
         if (boost::algorithm::iequals(mc.scope(), "rnw") &&
             boost::algorithm::iequals(mc.variable(), "weave"))
         {
            return mc.value();
         }
      }
   }
   else
   {
      LOG_ERROR(error);
   }

   // if we didn't find a directive then inspect project & global config
   if (projects::projectContext().hasProject())
      return projects::projectContext().config().defaultSweaveEngine;
   else
      return userSettings().defaultSweaveEngine();
}

bool callSweave(const std::string& rBinDir,
                const std::string& file)
{
   // R exe path differs by platform
#ifdef _WIN32
   std::string path = FilePath(rBinDir).complete("Rterm.exe").absolutePath();
#else
   std::string path = FilePath(rBinDir).complete("R").absolutePath();
#endif

   // calculate the full path to the file then use it to determine
   // the active sweave engine
   FilePath rnwPath = module_context::resolveAliasedPath(file);
   std::string weaveType = weaveTypeForFile(rnwPath);
   boost::shared_ptr<RnwWeave> pRnwWeave = weaveRegistry()
                                             .findTypeIgnoreCase(weaveType);

   // run the weave
   if (pRnwWeave)
   {
      std::vector<std::string> args = pRnwWeave->commandArgs(file);

      // call back-end
      int exitStatus;
      Error error = module_context::executeInterruptableChild(path,
                                                              args,
                                                              &exitStatus);
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }
      else if (exitStatus != EXIT_SUCCESS)
      {
         return false;
      }
      else
      {
         return true;
      }
   }
   else
   {
      throw r::exec::RErrorException(
         "Unknown Rnw weave method '" + weaveType + "' specified (valid " +
         "values are " + weaveRegistry().printableTypeNames() + ")");

      // keep compiler happy
      return false;
   }
}

SEXP rs_callSweave(SEXP rBinDirSEXP, SEXP fileSEXP)
{
   // call sweave
   bool success = false;
   try
   {
      success = callSweave(r::sexp::asString(rBinDirSEXP),
                           r::sexp::asString(fileSEXP));
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   // check for interrupts (likely since sweave can be long running)
   r::exec::checkUserInterrupt();

   r::sexp::Protect rProtect;
   return r::sexp::create(success, &rProtect);
}




} // anonymous namespace


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


Error initialize()
{
   R_CallMethodDef callSweaveMethodDef;
   callSweaveMethodDef.name = "rs_callSweave" ;
   callSweaveMethodDef.fun = (DL_FUNC) rs_callSweave ;
   callSweaveMethodDef.numArgs = 2;
   r::routines::addCallMethod(callSweaveMethodDef);



   return Success();
}


} // namespace rnw_weave
} // namespace tex
} // namespace modules
} // namesapce session

