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
#include <core/Debug.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>

#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/SessionPackageProvidedExtension.hpp>

#define kProjectTemplateLocal "(local)"
#define kRStudioProjectTemplatesPath "rstudio/templates/project"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace projects {
namespace templates {

namespace errors {

inline Error protocolError(const std::string& description,
                           const FilePath& resourcePath,
                           const ErrorLocation& location)
{
   Error error = systemError(boost::system::errc::protocol_error, location);
   error.addProperty("resource", resourcePath);
   error.addProperty("description", description);
   return error;
}

Error unexpectedWidgetType(const std::string& type,
                           const FilePath& resourcePath,
                           const ErrorLocation& location)
{
   return protocolError(
            "unexpected widget type '" + type + "'",
            resourcePath,
            location);
}

Error emptyField(const std::string& field, 
                 const FilePath& resourcePath,
                 const ErrorLocation& location)
{
   return protocolError(
            "empty field for property '" + field + "'",
            resourcePath,
            location);
}

} // end namespace errors

Error fromJson(
      const json::Object& object,
      ProjectTemplateWidgetDescription* pDescription)
{
   ProjectTemplateWidgetDescription description;

   core::Error error = core::json::readObject(
            object,
            "parameter", &description.parameter,
            "type",      &description.type,
            "label",     &description.label,
            "default",   &description.defaultValue,
            "position",  &description.position,
            "fields",    &description.fields);

   if (error)
      return error;
   
   *pDescription = description;
   return Success();
}

json::Value ProjectTemplateWidgetDescription::toJson() const
{
   core::json::Object object;

   object["parameter"] = parameter;
   object["type"]      = type;
   object["label"]     = label;
   object["default"]   = defaultValue;
   object["position"]  = position;
   object["fields"]    = core::json::toJsonArray(fields);

   return object;
}

Error fromJson(
      const json::Object& object,
      ProjectTemplateDescription* pDescription)
{
   Error error;
   ProjectTemplateDescription description;

   error = json::readObject(
            object,
            "package",    &description.package,
            "binding",    &description.binding,
            "title",      &description.title,
            "subtitle",   &description.subtitle,
            "caption",    &description.caption,
            "icon",       &description.icon,
            "open_files", &description.openFiles);
   
   if (error)
      return error;
   
   json::Array array;
   error = json::readObject(object, "widgets", &array);
   if (error)
      return error;
   
   BOOST_FOREACH(const json::Value& value, array)
   {
      if (!json::isType<json::Object>(value))
         return json::errors::typeMismatch(
                  value,
                  json::ObjectType,
                  ERROR_LOCATION);
      
      ProjectTemplateWidgetDescription widget;
      error = fromJson(value.get_obj(), &widget);
      if (error)
         return error;
      
      description.widgets.push_back(widget);
   }
   
   *pDescription = description;
   return Success();
}

json::Value ProjectTemplateDescription::toJson() const
{
   core::json::Object object;

   object["package"]    = package;
   object["binding"]    = binding;
   object["title"]      = title;
   object["subtitle"]   = subtitle;
   object["caption"]    = caption;
   object["icon"]       = icon;
   object["open_files"] = json::toJsonArray(openFiles);

   core::json::Array widgetsJson;
   BOOST_FOREACH(const ProjectTemplateWidgetDescription& widgetDescription, widgets)
   {
      widgetsJson.push_back(widgetDescription.toJson());
   }
   object["widgets"] = widgetsJson;

   return object;
}

namespace {

Error parseCsvField(const std::string& key,
                    const std::string& value,
                    const FilePath& resourcePath,
                    std::vector<std::string>* pEntries)
{
   std::vector<std::string> entries;
   text::parseCsvLine(value.begin(), value.end(), true, &entries);
   if (entries.empty())
      return errors::emptyField(key, resourcePath, ERROR_LOCATION);

   for (std::size_t i = 0, n = entries.size(); i < n; ++i)
      entries[i] = string_utils::trimWhitespace(entries[i]);

   *pEntries = entries;
   return Success();
}

Error parseWidgetType(const std::string& widget,
                      const FilePath& resourcePath,
                      std::string* pWidgetType)
{
   static std::vector<std::string> kWidgetTypes;
   if (kWidgetTypes.empty())
   {
      kWidgetTypes.push_back(kProjectTemplateWidgetTypeCheckBoxInput);
      kWidgetTypes.push_back(kProjectTemplateWidgetTypeFileInput);
      kWidgetTypes.push_back(kProjectTemplateWidgetTypeSelectInput);
      kWidgetTypes.push_back(kProjectTemplateWidgetTypeTextInput);
   }
   
   std::vector<std::string>::const_iterator it =
         std::find(kWidgetTypes.begin(),
                   kWidgetTypes.end(),
                   string_utils::toLower(widget));
   
   if (it != kWidgetTypes.end())
   {
      pWidgetType->assign(*it);
      return Success();
   }
   
   return errors::unexpectedWidgetType(widget, resourcePath, ERROR_LOCATION);
}

template <typename T>
core::Error populate(
      const core::FilePath& resourcePath,
      const T& map,
      ProjectTemplateDescription* pDescription)
{
   ProjectTemplateWidgetDescription widget;
   for (typename T::const_iterator it = map.begin();
        it != map.end();
        ++it)
   {
      const std::string& key   = it->first;
      const std::string& value = it->second;
      
      // populate primary keys
      if (key == "Binding")
         pDescription->binding = value;
      else if (key == "Title")
         pDescription->title = value;
      else if (key == "Subtitle")
         pDescription->subtitle = value;
      else if (key == "Caption")
         pDescription->caption = value;
      else if (key == "Icon")
      {
         // read icon file from disk
         FilePath iconPath = resourcePath.parent().complete(value);
         
         // skip if the file is too large
         uintmax_t fileSize = iconPath.size();
         if (fileSize > 1024 * 1024)
         {
            return systemError(
                     boost::system::errc::file_too_large,
                     ERROR_LOCATION);
         }

         // encode file contents as base64
         std::string encoded;
         Error error = base64::encode(iconPath, &encoded);
         if (error)
            return error;

         // send up to client as base64-encoded blob
         pDescription->icon = encoded;
      }
      else if (key == "OpenFiles")
      {
         Error error = parseCsvField(key, value, resourcePath, &pDescription->openFiles);
         if (error)
            return error;
      }

      // populate widget
      else if (key == "Parameter")
         widget.parameter = value;
      else if (key == "Label")
         widget.label = value;
      else if (key == "Default")
         widget.defaultValue = value;
      else if (key == "Position")
         widget.position = value;
      else if (key == "Widget")
      {
         Error error = parseWidgetType(value, resourcePath, &widget.type);
         if (error)
            return error;
      }
      else if (key == "Fields")
      {
         Error error = parseCsvField(key, value, resourcePath, &widget.fields);
         if (error)
            return error;
      }
   }

   // if we discovered a widget here, add it to the description
   if (!widget.parameter.empty())
      pDescription->widgets.push_back(widget);

   return Success();
}

class ProjectTemplateRegistry : boost::noncopyable
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

boost::shared_ptr<ProjectTemplateRegistry>& projectTemplateRegistry()
{
   static boost::shared_ptr<ProjectTemplateRegistry> instance =
         boost::make_shared<ProjectTemplateRegistry>();
   
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
      
      // attempt to parse as DCF -- multiple newlines used to separate records
      boost::regex reSeparator("\\n{2,}");
      boost::sregex_token_iterator it(contents.begin(), contents.end(), reSeparator, -1);
      boost::sregex_token_iterator end;
      
      for (; it != end; ++it)
      {
         // invoke parser on current record
         std::map<std::string, std::string> fields;
         std::string errorMessage;
         error = text::parseDcfFile(*it, true, &fields, &errorMessage);
         if (error)
            return error;
         
         // populate project template description based on fields
         error = populate(resourcePath, fields, pDescription);
         if (error)
            return error;
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
         // skip files that don't have a dcf extension
         if (childPath.extension() != ".dcf")
            continue;
         
         ProjectTemplateDescription description;
         Error error = parseResourceFile(pkgName, childPath, &description);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }
         
         description.package = pkgName;
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
      projectTemplateRegistry() = pRegistry_;
      
      // add known project templates
      addKnownProjectTemplates();
      
      // execute any callbacks waiting for indexing to complete
      executeCallbacks();
   }
   
private:
   
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
            projectTemplateRegistry()->get(package);
      
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
      projectTemplateRegistry()->add(package, ptd);
   }
   
   void normalize(ProjectTemplateDescription* pDescription)
   {
      std::string title = pDescription->title;
      
      if (pDescription->subtitle.empty())
         pDescription->subtitle = "Create a new " + title;
      
      if (pDescription->caption.empty())
         pDescription->caption = "Create " + title;
   }
   
   void executeCallbacks()
   {
      for (std::size_t i = 0, n = callbacks_.size(); i < n; ++i)
         callbacks_[i]();
      callbacks_.clear();
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

void notifyClientProjectTemplateRegistryUpdated()
{
   json::Value data = projectTemplateRegistry()->toJson();
   ClientEvent event(client_events::kProjectTemplateRegistryUpdated, data);
   module_context::enqueClientEvent(event);
}

void reindex(bool notifyClient)
{
   if (module_context::disablePackages())
      return;
   
   if (notifyClient)
   {
      projectTemplateIndexer().addIndexingFinishedCallback(
               boost::bind(notifyClientProjectTemplateRegistryUpdated));
   }
   
   projectTemplateIndexer().start();
}

void onDeferredInit(bool)
{
   reindex(true);
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
                  boost::bind(reindex, true),
                  true);
      }
   }
}

void respondWithProjectTemplateRegistry(const json::JsonRpcFunctionContinuation& continuation)
{
   json::JsonRpcResponse response;
   response.setResult(projectTemplateRegistry()->toJson());
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

SEXP rs_getProjectTemplateRegistry()
{
   json::Value registryJson = projectTemplateRegistry()->toJson();
   r::sexp::Protect protect;
   return r::sexp::create(registryJson, &protect);
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   using boost::bind;
   
   events().onDeferredInit.connect(onDeferredInit);
   events().onConsoleInput.connect(onConsoleInput);
   
   RS_REGISTER_CALL_METHOD(rs_getProjectTemplateRegistry, 0);
   
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
