/*
 * SessionDependencies.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include "SessionPackages.hpp"

#include <boost/bind.hpp>
#include <boost/algorithm/string/join.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/system/Environment.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "jobs/ScriptJob.hpp"

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
   Error error = archivesDir.getChildren(children);
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

   for (const FilePath& child : children)
   {
      boost::smatch match;
      std::string filename = child.getFilename();
      if (regex_utils::match(filename, match, re))
      {
         EmbeddedPackage pkg;
         pkg.name = name;
         pkg.version = match[1];
         pkg.sha1 = match[2];
         pkg.archivePath = string_utils::utf8ToSystem(child.getAbsolutePath());
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

#define kCRANPackageDependency "cran"
#define kEmbeddedPackageDependency "embedded"

struct Dependency
{
   Dependency() : 
      location(kCRANPackageDependency), 
      source(false), 
      versionSatisfied(true) {}

   // Construct a new Dependency record from an S-expression containing a named list
   Dependency(SEXP sexp)
   {
      // Required fields
      Error error = r::sexp::getNamedListElement(sexp, "name", &name);
      if (error)
         LOG_ERROR(error);
      error = r::sexp::getNamedListElement(sexp, "version", &version);
      if (error)
         LOG_ERROR(error);

      // Establish defaults
      source = false;
      location = "cran";

      // Optional fields
      r::sexp::getNamedListElement(sexp, "source", &source);
      r::sexp::getNamedListElement(sexp, "location", &location);
      r::sexp::getNamedListElement(sexp, "availableVersion", &availableVersion);
      r::sexp::getNamedListElement(sexp, "versionSatisfied", &versionSatisfied);
   }

   bool empty() const { return name.empty(); }

   // Return a version of the dependency as an S-expression for processing in R.
   SEXP asSEXP(r::sexp::Protect *protect) const
   {
      r::sexp::ListBuilder list(protect);
      list.add("name", name);
      list.add("location", location);
      list.add("version", version);
      list.add("source", source);
      list.add("availableVersion", availableVersion);
      list.add("versionSatisfied", versionSatisfied);
      return r::sexp::create(list, protect);
   }

   std::string location;
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
   for (const json::Value& depJsonValue : depsJson)
   {
      if (json::isType<json::Object>(depJsonValue))
      {
         Dependency dep;
         const json::Object& depJson = depJsonValue.getObject();
         Error error = json::readObject(depJson,
                                        "location", dep.location,
                                        "name", dep.name,
                                        "version", dep.version,
                                        "source", dep.source);
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
   for (const Dependency& dep : deps)
   {
      json::Object depJson;
      depJson["location"] = dep.location;
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

   // build the list of unsatisfied dependencies
   using namespace module_context;
   std::vector<Dependency> unsatisfiedDeps;
   for (Dependency& dep : deps)
   {
      if (dep.location == kCRANPackageDependency)
      {
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
      }
      else if (dep.location == kEmbeddedPackageDependency)
      {
         EmbeddedPackage pkg = embeddedPackageInfo(dep.name);

         // package isn't installed so report that it requires installation
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
      }
   }

   // return unsatisfied dependencies
   pResponse->setResult(dependenciesToJson(unsatisfiedDeps));
   return Success();
}

// Builds an installation script which will install all the dependencies at once. 
std::string buildCombinedInstallScript(const std::vector<Dependency>& deps)
{
   bool isRenv = module_context::isRenvActive();
   std::vector<std::string> cranPackages;
   std::vector<std::string> cranSourcePackages;
   std::vector<std::string> embeddedPackages;
   std::string cmd;

   // Sort the dependencies into CRAN packages installed with defaults, CRAN packages explicitly
   // installed as source, and embedded packages.
   for (const Dependency& dep: deps)
   {
      if (dep.location == kCRANPackageDependency)
      {
         if (dep.source)
            cranSourcePackages.push_back("'" + dep.name + "'");
         else
            cranPackages.push_back("'" + dep.name + "'");
      }
      else if (dep.location == kEmbeddedPackageDependency)
      {
         EmbeddedPackage pkg = embeddedPackageInfo(dep.name);
         if (!pkg.empty())
            embeddedPackages.push_back(pkg.archivePath);
      }
   }

   // Install the CRAN packages with a single call
   if (!cranPackages.empty())
   {
      std::string pkgList = boost::algorithm::join(cranPackages, ",");
      
      if (isRenv)
      {
         cmd += "renv::install(c(" + pkgList + "))\n\n";
      }
      else
      {
         cmd += "utils::install.packages(c(" + pkgList + "), " +
                "repos = '"+ module_context::CRANReposURL() + "'";
         cmd += ")\n\n";
      }
   }

   // Install the CRAN source packages with a single call
   if (!cranSourcePackages.empty())
   {
      std::string pkgList = boost::algorithm::join(cranSourcePackages, ",");

      if (isRenv)
      {
         cmd += "options(pkgType = 'source'); renv::install(c(" + pkgList + "))\n\n";
      }
      else
      {
         cmd += "utils::install.packages(c(" + pkgList + "), " +
                "repos = '"+ module_context::CRANReposURL() + "', ";
         cmd += "type = 'source')\n\n";
      }
   }

   // Install any requested embedded packages
   for (const std::string& pkg: embeddedPackages)
   {
      cmd += "utils::install.packages('" + pkg + "', "
                                      "repos = NULL, type = 'source')\n\n";
   }

   return cmd;
}

// Builds an installation script which will install the packages one at a time. This is desirable
// because it allows us to give much better progress and feedback during the installation process,
// and also gives us greater control over installation order.
std::string buildIndividualInstallScript(const std::vector<Dependency>& dependencies)
{
   bool isRenv = module_context::isRenvActive();

   // Convert dependencies list to an R list
   r::sexp::Protect protect;
   r::sexp::ListBuilder list(&protect);
   for (const Dependency& dep: dependencies)
   {
      list.add(dep.asSEXP(&protect));
   }

   // Expand (add direct dependencies) and topologically sort the dependencies in R
   SEXP expanded;
   r::exec::RFunction expander(".rs.expandPkgDependencies");
   expander.addParam("dependencies", r::sexp::create(list, &protect));
   Error error = expander.call(&expanded, &protect);
   if (error)
   {
      LOG_ERROR(error);
      // Use simple version of installation script as a fallback
      return buildCombinedInstallScript(dependencies);
   }

   // Create a new list of dependencies from the expanded list returned from R
   std::vector<Dependency> deps;
   for (int idx = 0; idx < r::sexp::length(expanded); idx++)
   {
      deps.push_back(Dependency(VECTOR_ELT(expanded, idx)));
   }

   std::string script;

   if (isRenv)
   {
      // For renv, remember default setting for package type
      script += "defaultPkgType <- getOption('pkgType')\n";
   }

   // Loop over each dependency and add it to the installation script
   for (size_t i = 0; i < deps.size(); i++)
   {
      const Dependency& dep = deps[i];

      // Add a comment header with the name of the package. This will result in better progress
      // treatment, since comment headers are used to show the currently executing section of a
      // script.
      script += "# " + dep.name + " -------------\n\n";

      // If there's more than one dependency, indicate which one is being installed.
      if (deps.size() > 1)
      {
         script += "cat('\\n\\n[" + 
            safe_convert::numberToString(i + 1) + 
            "/" + 
            safe_convert::numberToString(deps.size()) +
            "] Installing " + dep.name + "...\\n\\n')\n";
      }

      if (dep.location == kCRANPackageDependency)
      {
         if (isRenv)
         {
            // renv used the 'pkgType' option to decide whether to install a package
            // from sources or not; set it to source explicitly to force source installs
            if (dep.source)
            {
               script += "options(pkgType = 'source'); ";
            }
            else
            {
               script += "options(pkgType = defaultPkgType); ";
            }
            
            // NOTE: renv will use the repositories as set in the lockfile here;
            // should we override this based on the current repositories set in
            // the session?
            script += "renv::install('" + dep.name + "')";
         }
         else
         {
            // Build install command for CRAN. 
            script += "utils::install.packages('" + dep.name + "', " +
                  "repos = '"+ module_context::CRANReposURL() + "'";

            if (dep.source) 
            {
               // Install from source if requested
               script += ", type = 'source'";
            }

            script += ")";
         }
      }
      else if (dep.location == kEmbeddedPackageDependency)
      {
         EmbeddedPackage pkg = embeddedPackageInfo(dep.name);

         // Build install command for bundled archive
         script += "utils::install.packages('" + pkg.archivePath + 
            "', repos = NULL, type = 'source')";
      }

      script += "\n\n";
   }

   return script;
}

Error installDependencies(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // get list of dependencies
   json::Array depsJson;
   std::string context;
   Error error = json::readParams(request.params, &context, &depsJson);
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

   // Start script with all the CRAN download settings
   std::string script = module_context::CRANDownloadOptions() + "\n\n";

   // Lock during installation since we will be doing this in a background job and want to minimize
   // the risk of competing processes attempting to write to the library (note that this only
   // affects binary installations on Windows and MacOS; source installations are locked with
   // 00LOCK)
   script += "options(install.lock = TRUE)\n\n";

   bool isRenv = module_context::isRenvActive();
   bool isPackrat = module_context::packratContext().modeOn;
   bool isProjectLocal = isPackrat || isRenv;
   
   // Emit a message to the user at the beginning of the script declaring what we're about to do
   if (deps.size() > 1)
   {
      script += "cat('** ";
      if (context.empty()) 
         script += "Installing R Packages: ";
      else
         script += "Installing R Package Dependencies for " + context + ": ";
      for (size_t i = 0; i < deps.size(); i++)
      {
         script += "\\'" + deps[i].name + "\\'";
         if (i < deps.size() - 1)
            script += ", ";
      }
      script += "\\n\\n')\n";
   }
   else
   {
      if (context.empty())
         script += "cat('Installing \\'" + deps[0].name + "\\' ...\\n\\n')\n";
      else
         script += "cat('Installing \\'" + deps[0].name + "\\' for " + context + 
            "...\\n\\n')\n";
   }

   // Build an installation script to install the required packages.
   if (prefs::userPrefs().installPkgDepsIndividually())
   {
      script += buildIndividualInstallScript(deps);
   }
   else
   {
      script += buildCombinedInstallScript(deps);
   }

   // Emit a message indicating that we're done. The script aborts on error, so if we get here we
   // can presume everything installed successfully.
   script += "message('\\n\\n\\u2714 ";  // Heavy checkmark
   if (deps.size() < 2) 
      script += "Package \\'" + deps[0].name + "\\' successfully installed.";
   else
      script += "Packages successfully installed.";
   script += "')\n\n";

   jobs::ScriptLaunchSpec installJob(
         // Supply job name; use context if we have one, otherwise auto-generate from dependency list
         context.empty() ?
            (deps.size() == 1 ? 
                ("Install '" + deps[0].name + "'") :
                ("Install R packages")) :
            context + " Dependencies",

         // Script to run for job
         script,

         // Directory in which to run job
         isProjectLocal ?
            projects::projectContext().directory() :
            FilePath(),

         false, // Import environment
         "");

   // Run in a vanilla session if Packrat mode isn't on (prevents problematic startup scripts/etc
   // from causing install trouble)
   if (!isProjectLocal)
      installJob.setProcOptions(async_r::R_PROCESS_VANILLA);

   std::string jobId;
   error = jobs::startScriptJob(installJob, [&]()
   {
      // When job is finished, update the packages state
      packages::enquePackageStateChanged();
   }, &jobId);

   // Return handle to script job
   pResponse->setResult(jobId);

   return Success();
}


} // anonymous namespace


Error initialize()
{         
   // install handlers
   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionDependencies.R"))
      (bind(registerRpcMethod, "unsatisfied_dependencies", unsatisfiedDependencies))
      (bind(registerRpcMethod, "install_dependencies", installDependencies));
   return initBlock.execute();
}
   

} // namespace dependencies
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

