/*
 * SessionProjectTemplate.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
#include <session/SessionProjectTemplate.hpp>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/function.hpp>
#include <boost/range/adaptors.hpp>

#include <core/Algorithm.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/SessionPackageProvidedExtension.hpp>

#define kProjectTemplateLocal "(local)"
#define kRStudioProjectTemplatesPath "rstudio/templates"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace projects {
namespace templates {

namespace {

class ProjectTemplateRegistry
{
public:
   
   void add(const std::string& pkgName, const ProjectTemplateDescription& description)
   {
      registry_[pkgName].push_back(description);
   }
   
   std::vector<ProjectTemplateDescription>& get(const std::string& pkgName)
   {
      return registry_[pkgName];
   }
   
   std::size_t size()
   {
      return registry_.size();
   }
   
   json::Value toJson()
   {
      json::Object object;
      
      BOOST_FOREACH(const std::string& pkgName, registry_ | boost::adaptors::map_keys)
      {
         json::Array array;
         BOOST_FOREACH(const ProjectTemplateDescription& description, registry_[pkgName])
         {
            array.push_back(description.toJson());
         }
         object[pkgName] = array;
      }
      
      return object;
   }

   std::map<
      std::string,
      std::vector<ProjectTemplateDescription>
   > registry_;
};

ProjectTemplateRegistry& projectTemplateRegistry()
{
   static ProjectTemplateRegistry instance;
   return instance;
}

class ProjectTemplateIndexer : public ppe::Indexer
{
public:
   
   explicit ProjectTemplateIndexer(const std::string& resourcePath)
      : ppe::Indexer(resourcePath)
   {
   }
   
   void addIndexingFinishedCallback(boost::function<void()> callback)
   {
      callbacks_.push_back(callback);
   }
   
   Error parseResourceFile(const std::string& pkgName,
                           const FilePath& resourcePath,
                           ProjectTemplateDescription* pDescription)
   {
      Error error;
      
      // read dcf contents
      std::string contents;
      error = core::readStringFromFile(resourcePath, &contents, string_utils::LineEndingPosix);
      if (error)
         return error;
      
      return parseResourceContents(pkgName, contents, pDescription);
   }
   
   Error parseResourceContents(const std::string& pkgName,
                               const std::string& contents,
                               ProjectTemplateDescription* pDescription)
   {
      Error error;
      
      // attempt to parse as DCF -- multiple newlines used to separate records
      boost::regex reSeparator("\\n{2,}");
      boost::sregex_token_iterator it(contents.begin(), contents.end(), reSeparator, -1);
      boost::sregex_token_iterator end;
      
      for (; it != end; ++it) {
         // invoke parser on current record
         std::map<std::string, std::string> fields;
         std::string errorMessage;
         error = text::parseDcfFile(*it, true, &fields, &errorMessage);
         if (error)
            continue;
         
         // populate project template description based on fields
         error = ProjectTemplateDescription::populate(fields, pDescription);
         if (error)
            continue;
      }
      
      return error;
   }
   
private:
   void onIndexingStarted()
   {
      pRegistry_ = boost::make_shared<ProjectTemplateRegistry>();
   }

   void onWork(const std::string& pkgName, const core::FilePath& resourcePath)
   {
      Error error;
      
      // loop over discovered files and attempt to read template descriptions
      std::vector<FilePath> children;
      error = resourcePath.children(&children);
      if (error)
         LOG_ERROR(error);
      
      BOOST_FOREACH(const FilePath& childPath, children)
      {
         ProjectTemplateDescription description;
         Error error = parseResourceFile(pkgName, childPath, &description);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }
         
         normalize(&description);
         
         pRegistry_->add(pkgName, description);
      }
   }

   void onIndexingCompleted()
   {
      // index a project template file in the RStudio options folder
      FilePath localTemplatesPath =
            module_context::resolveAliasedPath("~/.R/" kRStudioProjectTemplatesPath);
      
      if (localTemplatesPath.exists())
         onWork(kProjectTemplateLocal, localTemplatesPath);
      
      // update global registry
      projectTemplateRegistry() = *pRegistry_;
      
      // add known project templates
      addKnownProjectTemplates();
      
      // execute any callbacks waiting for indexing to complete
      executeCallbacks();
      
      // notify client
      notifyClient();
   }
   
private:
   Error validateFields(std::map<std::string, std::string>& fields,
                        const ErrorLocation& location)
   {
      std::vector<std::string> missingFields;
      const char* expected[] = {"binding", "title", "subtitle", "caption"};
      for (std::size_t i = 0; i < sizeof(expected); ++i)
      {
         const char* field = expected[i];
         if (!fields.count(field) || fields[field].empty())
            missingFields.push_back(field);
      }
      
      if (missingFields.empty())
         return Success();
      
      std::string reason =
            "invalid project template description: missing or empty fields "
            "[" + core::algorithm::join(missingFields, ",") + "]";
      
      
      Error error = systemError(boost::system::errc::protocol_error, reason, location);
      return error;
   }
   
   void addKnownProjectTemplates()
   {
      addProjectTemplate("devtools", "create", "R Package using devtools");
      addProjectTemplate("Rcpp", "Rcpp.package.skeleton", "R Package using Rcpp");
      addProjectTemplate("RcppArmadillo", "RcppArmadillo.package.skeleton", "R Package using RcppArmadillo");
      addProjectTemplate("RcppEigen", "RcppEigen.package.skeleton", "R Package using RcppEigen");
   }
   
   void addProjectTemplate(const std::string& package,
                           const std::string& binding,
                           const std::string& title)
   {
      // if we already have a project template registered for this
      // package with this binding, bail (this allows R packages to
      // override the default settings provided by RStudio if desired)
      std::vector<ProjectTemplateDescription>& templates =
            projectTemplateRegistry().get(package);
      
      for (std::size_t i = 0, n = templates.size(); i < n; ++i)
         if (templates[i].binding == binding)
            return;
      
      // generate appropriate subtitle, caption from title
      std::string subtitle = "Create a new " + title;
      std::string caption  = "Create " + title;
      
      // add a new project template
      ProjectTemplateDescription ptd;
      ptd.package  = package;
      ptd.binding  = binding;
      ptd.title    = title;
      ptd.subtitle = subtitle;
      ptd.caption  = caption;
      projectTemplateRegistry().add(package, ptd);
   }
   
   void normalize(ProjectTemplateDescription* pDescription)
   {
      std::string title = pDescription->title;
      
      if (pDescription->subtitle.empty())
         pDescription->subtitle = "Create a new " + title;
      
      if (pDescription->subtitle.empty())
         pDescription->caption = "Create " + title;
   }
   
   void executeCallbacks()
   {
      for (std::size_t i = 0, n = callbacks_.size(); i < n; ++i)
         callbacks_[i]();
      callbacks_.clear();
   }
   
   void notifyClient()
   {
      json::Value data = projectTemplateRegistry().toJson();
      ClientEvent event(client_events::kProjectTemplateRegistryUpdated, data);
      module_context::enqueClientEvent(event);
   }
   
private:
   std::vector< boost::function<void()> > callbacks_;
   boost::shared_ptr<ProjectTemplateRegistry> pRegistry_;
};

ProjectTemplateIndexer& projectTemplateIndexer()
{
   static ProjectTemplateIndexer instance(kRStudioProjectTemplatesPath);
   return instance;
}

void reindex()
{
   projectTemplateIndexer().start();
}

void onDeferredInit(bool)
{
   if (module_context::disablePackages())
      return;
   
   projectTemplateIndexer().start();
}

void onConsoleInput(const std::string& input)
{
   if (module_context::disablePackages())
      return;
   
   const char* const commands[] = {
      "install.packages",
      "remove.packages",
      "devtools::install_github",
      "install_github",
      "devtools::load_all",
      "load_all"
   };
   
   std::string inputTrimmed = boost::algorithm::trim_copy(input);
   BOOST_FOREACH(const char* command, commands)
   {
      if (boost::algorithm::starts_with(inputTrimmed, command))
      {
         // we need to give R a chance to actually process the package library
         // mutating command before we update the index. schedule delayed work
         // with idleOnly = true so that it waits until the user has returned
         // to the R prompt
         module_context::scheduleDelayedWork(
                  boost::posix_time::seconds(1),
                  reindex,
                  true);
      }
   }
}

void respondWithProjectTemplateRegistry(const json::JsonRpcFunctionContinuation& continuation)
{
   json::JsonRpcResponse response;
   response.setResult(projectTemplateRegistry().toJson());
   continuation(Success(), &response);
}

void withProjectTemplateRegistry(boost::function<void()> callback)
{
   if (projectTemplateIndexer().running())
      projectTemplateIndexer().addIndexingFinishedCallback(callback);
   else
      callback();
}

void getProjectTemplateRegistry(const json::JsonRpcRequest& request,
                                const json::JsonRpcFunctionContinuation& continuation)
{
   withProjectTemplateRegistry(
            boost::bind(respondWithProjectTemplateRegistry, boost::cref(continuation)));
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   events().onDeferredInit.connect(onDeferredInit);
   events().onConsoleInput.connect(onConsoleInput);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionProjectTemplate.R"))
         (bind(registerAsyncRpcMethod, "get_project_template_registry", getProjectTemplateRegistry));
   return initBlock.execute();
}

} // end namespace templates
} // end namespace projects
} // end namespace modules
} // end namespace session
} // end namespace rstudio
