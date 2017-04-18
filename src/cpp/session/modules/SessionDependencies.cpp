/*
 * SessionDependencies.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "SessionDependencies.hpp"

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/system/Environment.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

struct EmbeddedPackage
{
   bool empty() const { return archivePath.empty(); }

   std::string name;
   std::string version;
   std::string sha1;
   std::string archivePath;
};

EmbeddedPackage embeddedPackageInfo(const std::string& name)
{
   // determine location of archives
   FilePath archivesDir = session::options().sessionPackageArchivesPath();
   std::vector<FilePath> children;
   Error error = archivesDir.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return EmbeddedPackage();
   }

   // we saw the regex with explicit character class ranges fail to match
   // on a windows 8.1 system so we are falling back to a simpler regex
   //
   // (note see below for another approach involving setting the locale
   // of the regex directly -- this assumes that the matching issue is
   // somehow related to locales)
   boost::regex re(name + "_([^_]+)_([^\\.]+)\\.tar\\.gz");

   /* another approach (which we didn't try) based on setting the regex locale
   boost::regex re;
   re.imbue(std::locale("en_US.UTF-8"));
   re.assign(name + "_([0-9]+\\.[0-9]+\\.[0-9]+)_([\\d\\w]+)\\.tar\\.gz");
   */

   BOOST_FOREACH(const FilePath& child, children)
   {
      boost::smatch match;
      std::string filename = child.filename();
      if (regex_utils::match(filename, match, re))
      {
         EmbeddedPackage pkg;
         pkg.name = name;
         pkg.version = match[1];
         pkg.sha1 = match[2];
         pkg.archivePath = string_utils::utf8ToSystem(child.absolutePath());
         return pkg;
      }
   }

   // none found
   return EmbeddedPackage();
}

} // anonymous namespace

namespace modules {
namespace dependencies {

namespace {

const int kCRANPackageDependency = 0;
const int kEmbeddedPackageDependency = 1;

struct Dependency
{
   Dependency() : type(0), source(false), versionSatisfied(true) {}

   bool empty() const { return name.empty(); }

   int type;
   std::string name;
   std::string version;
   bool source;
   std::string availableVersion;
   bool versionSatisfied;
};

std::string nameFromDep(const Dependency& dep)
{
   return dep.name;
}

std::vector<std::string> packageNames(const std::vector<Dependency> deps)
{
   std::vector<std::string> names;
   std::transform(deps.begin(),
                  deps.end(),
                  std::back_inserter(names),
                  nameFromDep);
   return names;
}

std::vector<Dependency> dependenciesFromJson(const json::Array& depsJson)
{
   std::vector<Dependency> deps;
   BOOST_FOREACH(const json::Value& depJsonValue, depsJson)
   {
      if (json::isType<json::Object>(depJsonValue))
      {
         Dependency dep;
         json::Object depJson = depJsonValue.get_obj();
         Error error = json::readObject(depJson,
                                        "type", &(dep.type),
                                        "name", &(dep.name),
                                        "version", &(dep.version),
                                        "source", &(dep.source));
         if (!error)
         {
            deps.push_back(dep);
         }
         else
         {
            LOG_ERROR(error);
         }
      }
   }
   return deps;
}

json::Array dependenciesToJson(const std::vector<Dependency>& deps)
{
   json::Array depsJson;
   BOOST_FOREACH(const Dependency& dep, deps)
   {
      json::Object depJson;
      depJson["type"] = dep.type;
      depJson["name"] = dep.name;
      depJson["version"] = dep.version;
      depJson["source"] = dep.source;
      depJson["available_version"] = dep.availableVersion;
      depJson["version_satisfied"] = dep.versionSatisfied;
      depsJson.push_back(depJson);
   }
   return depsJson;
}



bool embeddedPackageRequiresUpdate(const EmbeddedPackage& pkg)
{
   // if this package came from the rstudio ide then check if it needs
   // an update (i.e. has a different SHA1)
   r::exec::RFunction func(".rs.rstudioIDEPackageRequiresUpdate",
                           pkg.name, pkg.sha1);
   bool requiresUpdate = false;
   Error error = func.call(&requiresUpdate);
   if (error)
      LOG_ERROR(error);

   return requiresUpdate;
}

void silentUpdateEmbeddedPackage(const EmbeddedPackage& pkg)
{
   // suppress output which occurs during silent update
   r::session::utils::SuppressOutputInScope suppressOutput;

   Error error = r::exec::RFunction(".rs.updateRStudioIDEPackage",
                             pkg.name, pkg.archivePath).call();
   if (error)
      LOG_ERROR(error);
}


Error unsatisfiedDependencies(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   // get list of dependencies and silentUpdate flag
   json::Array depsJson;
   bool silentUpdate;
   Error error = json::readParams(request.params, &depsJson, &silentUpdate);
   if (error)
      return error;
   std::vector<Dependency> deps = dependenciesFromJson(depsJson);

   // build the list of unsatisifed dependencies
   using namespace module_context;
   std::vector<Dependency> unsatisfiedDeps;
   BOOST_FOREACH(Dependency& dep, deps)
   {
      switch(dep.type)
      {
      case kCRANPackageDependency:
         if (!isPackageVersionInstalled(dep.name, dep.version))
         {
            // presume package is available unless we can demonstrate otherwise
            // (we don't want to block installation attempt unless we're
            // reasonably confident it will not result in a viable version)
            r::sexp::Protect protect;
            SEXP versionInfo = R_NilValue;

            // find the version that will be installed from CRAN
            error = r::exec::RFunction(".rs.packageCRANVersionAvailable", 
                  dep.name, dep.version, dep.source).call(&versionInfo, &protect);
            if (error) {
               LOG_ERROR(error);
            } else {
               // if these fail, we'll fall back on defaults set above
               r::sexp::getNamedListElement(versionInfo, "version", 
                     &dep.availableVersion);
               r::sexp::getNamedListElement(versionInfo, "satisfied", 
                     &dep.versionSatisfied);
            }

            unsatisfiedDeps.push_back(dep);
         }
         break;

      case kEmbeddedPackageDependency:
         EmbeddedPackage pkg = embeddedPackageInfo(dep.name);

         // package isn't installed so report that it reqires installation
         if (!isPackageInstalled(dep.name))
         {
            unsatisfiedDeps.push_back(dep);
         }
         // silent update if necessary (as long as we aren't packified)
         else if (silentUpdate && !packratContext().packified)
         {
            // package installed was from IDE but is out of date
            if (embeddedPackageRequiresUpdate(pkg))
            {
               silentUpdateEmbeddedPackage(pkg);
            }
            // package installed wasn't from the IDE but is older than
            // the version we currently have embedded
            else if (!isPackageVersionInstalled(pkg.name, pkg.version))
            {
               silentUpdateEmbeddedPackage(pkg);
            }
            else
            {
               // the only remaining case is a newer version of the package is
               // already installed (e.g. directly from github). in this case
               // we do nothing
            }
         }

         break;
      }
   }

   // return unsatisfied dependencies
   pResponse->setResult(dependenciesToJson(unsatisfiedDeps));
   return Success();
}

Error installDependencies(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // get list of dependencies
   json::Array depsJson;
   Error error = json::readParams(request.params, &depsJson);
   if (error)
      return error;
   std::vector<Dependency> deps = dependenciesFromJson(depsJson);

   // Ensure we have a writeable user library
   error = r::exec::RFunction(".rs.ensureWriteableUserLibrary").call();
   if (error)
      return error;

   // force unload as necessary
   std::vector<std::string> names = packageNames(deps);
   error = r::exec::RFunction(".rs.forceUnloadForPackageInstall", names).call();
   if (error)
      LOG_ERROR(error);

   // R binary
   FilePath rProgramPath;
   error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;

   // options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.redirectStdErrToStdOut = true;

   // build lists of cran packages and archives
   std::vector<std::string> cranPackages;
   std::vector<std::string> cranSourcePackages;
   std::vector<std::string> embeddedPackages;
   BOOST_FOREACH(const Dependency& dep, deps)
   {
      switch(dep.type)
      {
      case kCRANPackageDependency:
         if (dep.source)
            cranSourcePackages.push_back("'" + dep.name + "'");
         else
            cranPackages.push_back("'" + dep.name + "'");
         break;

      case kEmbeddedPackageDependency:
         EmbeddedPackage pkg = embeddedPackageInfo(dep.name);
         if (!pkg.empty())
            embeddedPackages.push_back(pkg.archivePath);
         break;
      }
   }

   // build install command
   std::string cmd("{ " + module_context::CRANDownloadOptions() + "; ");
   if (!cranPackages.empty())
   {
      std::string pkgList = boost::algorithm::join(cranPackages, ",");
      cmd += "utils::install.packages(c(" + pkgList + "), " +
             "repos = '"+ module_context::CRANReposURL() + "'";
      cmd += ");";
   }
   if (!cranSourcePackages.empty())
   {
      std::string pkgList = boost::algorithm::join(cranSourcePackages, ",");
      cmd += "utils::install.packages(c(" + pkgList + "), " +
             "repos = '"+ module_context::CRANReposURL() + "', ";
      cmd += "type = 'source');";
   }
   BOOST_FOREACH(const std::string& pkg, embeddedPackages)
   {
      cmd += "utils::install.packages('" + pkg + "', "
                                      "repos = NULL, type = 'source');";
   }
   cmd += "}";

   // build args
   std::vector<std::string> args;
   args.push_back("--slave");

   // for packrat projects we execute the profile and set the working
   // directory to the project directory; for other contexts we just
   // propagate the R_LIBS
   if (module_context::packratContext().modeOn)
   {
      options.workingDir = projects::projectContext().directory();
   }
   else
   {
      args.push_back("--vanilla");
      core::system::Options childEnv;
      core::system::environment(&childEnv);
      std::string libPaths = module_context::libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&childEnv, "R_LIBS", libPaths);
      options.environment = childEnv;
   }

   // for windows we need to forward setInternet2
#ifdef _WIN32
   if (!r::session::utils::isR3_3() && userSettings().useInternet2())
      args.push_back("--internet2");
#endif

   args.push_back("-e");
   args.push_back(cmd);

   boost::shared_ptr<console_process::ConsoleProcessInfo> pCPI =
         boost::make_shared<console_process::ConsoleProcessInfo>(
            "Installing Packages", console_process::InteractionNever);

   // create and execute console process
   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   pCP = console_process::ConsoleProcess::create(
            string_utils::utf8ToSystem(rProgramPath.absolutePath()),
            args,
            options,
            pCPI);

   // return console process
   pResponse->setResult(pCP->toJson());
   return Success();
}


} // anonymous namespace


Error initialize()
{         
   // install handlers
   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "unsatisfied_dependencies", unsatisfiedDependencies))
      (bind(registerRpcMethod, "install_dependencies", installDependencies));
   return initBlock.execute();
}
   

} // namepsace dependencies
} // namespace modules

namespace module_context {

Error installEmbeddedPackage(const std::string& name)
{
   EmbeddedPackage pkg = embeddedPackageInfo(name);
   return module_context::installPackage(pkg.archivePath);
}

} // anonymous namespace

} // namespace session
} // namespace rstudio

