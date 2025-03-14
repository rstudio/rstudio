/*
 * SessionRAddins.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionRAddins.hpp"

#include <gsl/gsl-lite.hpp>

#include <core/Macros.hpp>
#include <core/Algorithm.hpp>
#include <core/Debug.hpp>
#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>

#include <boost/regex.hpp>
#include <boost/bind/bind.hpp>
#include <boost/range/adaptor/map.hpp>
#include <boost/system/error_code.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RVersionInfo.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionPackageProvidedExtension.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace r_addins {

namespace {

RSTUDIO_BOOST_CONNECTION s_consolePromptHandler;

bool isDevtoolsLoadAllActive()
{
   std::vector<std::string> search;
   Error error = r::exec::RFunction("search").call(&search);
   if (error)
      return false;
   
   return std::find(search.begin(), search.end(), "devtools_shims") != search.end();
}

class AddinSpecification
{
public:
   
   AddinSpecification() {}
   
   AddinSpecification(const std::string& name,
                      const std::string& package,
                      const std::string& title,
                      const std::string& description,
                      bool interactive,
                      const std::string& binding,
                      int ordinal)
      : name_(name), package_(package), title_(title),
        description_(description), interactive_(interactive),
        binding_(binding), ordinal_(ordinal)
   {
   }
   
   const std::string& getName() const { return name_; }
   const std::string& getPackage() const { return package_; }
   const std::string& getTitle() const { return title_; }
   const std::string& getDescription() const { return description_; }
   bool isInteractive() const { return interactive_; }
   const std::string& getBinding() const { return binding_; }
   int getOrdinal() const { return ordinal_; }
   
   json::Object toJson() const
   {
      json::Object object;
      
      object["name"] = name_;
      object["package"] = package_;
      object["title"] = title_;
      object["description"] = description_;
      object["interactive"] = interactive_;
      object["binding"] = binding_;
      object["ordinal"] = ordinal_;
      
      return object;
   }
   
private:
   std::string name_;
   std::string package_;
   std::string title_;
   std::string description_;
   bool interactive_;
   std::string binding_;
   int ordinal_;
};

class AddinRegistry : boost::noncopyable
{
public:
   
   void saveToFile(const core::FilePath& filePath) const
   {
      std::shared_ptr<std::ostream> pStream;
      Error error = filePath.openForWrite(pStream);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      toJson().writeFormatted(*pStream);
   }

   void loadFromFile(const core::FilePath& filePath)
   {
      addins_.clear();

      if (!filePath.exists())
         return;

      std::string contents;
      Error error = core::readStringFromFile(filePath, &contents);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // check but don't log for unexpected input because we are the only ones
      // that write this file
      json::Value parsedJson;
      if (!parsedJson.parse(contents) &&
          json::isType<json::Object>(parsedJson))
      {
         const json::Object& addinsJson = parsedJson.getObject();

         for(const json::Object::Member& member : addinsJson)
         {
            const std::string& key = member.getName();
            const json::Value& valueJson = member.getValue();
            if (json::isType<json::Object>(valueJson))
            {
               bool interactive;
               std::string name, package, title, description, binding;
               Error error = json::readObject(valueJson.getObject(),
                                              "name", name,
                                              "package", package,
                                              "title", title,
                                              "description", description,
                                              "interactive", interactive,
                                              "binding", binding);
               if (error)
               {
                  LOG_ERROR(error);
                  continue;
               }
               
               // attempt read to ordinal (note that this was not persisted
               // as part of older addin databases so we read it separately;
               // we don't log errors as they're rather noisy and otherwise
               // harmless)
               int ordinal = 0;
               json::readObject(valueJson.getObject(), "ordinal", ordinal);
               addins_[key] = AddinSpecification(name,
                                                 package,
                                                 title,
                                                 description,
                                                 interactive,
                                                 binding,
                                                 ordinal);
            }
         }

      }

   }

   void add(const std::string& package, const AddinSpecification& spec)
   {
      addins_[constructKey(package, spec.getBinding())] = spec;
   }

   void add(const std::string& pkgName,
            std::map<std::string, std::string>& fields)
   {
      // if the 'interactive' field is not specified, default to 'true'
      bool interactive = true;
      if (fields.count("Interactive"))
         interactive = isTruthy(fields["Interactive"]);
      
      add(pkgName, AddinSpecification(
            fields["Name"],
            pkgName,
            fields["Title"],
            fields["Description"],
            interactive,
            fields["Binding"],
            gsl::narrow_cast<int>(addins_.size() + 1)));
   }

   void add(const std::string& pkgName, const FilePath& addinPath)
   {
      static const boost::regex reSeparator("\\n{2,}");

      std::string contents;
      Error error = core::readStringFromFile(addinPath, &contents, string_utils::LineEndingPosix);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      try
      {
         boost::sregex_token_iterator it(contents.begin(), contents.end(), reSeparator, -1);
         boost::sregex_token_iterator end;

         for (; it != end; ++it)
         {
            std::map<std::string, std::string> fields = parseAddinDcf(addinPath, *it);
            add(pkgName, fields);
         }
      }
      CATCH_UNEXPECTED_EXCEPTION;
   }
   
   bool contains(const std::string& package, const std::string& name)
   {
      return addins_.count(constructKey(package, name));
   }

   const AddinSpecification& get(const std::string& package, const std::string& name)
   {
      return addins_[constructKey(package, name)];
   }
   
   json::Object toJson() const
   {
      json::Object object;
      
      for (const std::string& key : addins_ | boost::adaptors::map_keys)
      {
         object[key] = addins_.at(key).toJson();
      }
      
      return object;
   }

   std::size_t size() const { return addins_.size(); }
   
private:
   
   static std::map<std::string, std::string> parseAddinDcf(
                                          const FilePath& addinPath,
                                          const std::string& contents)
   {
      // read and parse the DCF file
      std::map<std::string, std::string> fields;
      std::string errMsg;
      Error error = text::parseDcfFile(contents, true, &fields, &errMsg);
      if (error)
      {
         error.addProperty("path", addinPath.getAbsolutePath());
         LOG_ERROR(error);
      }

      return fields;
   }


   static std::string constructKey(const std::string& package, const std::string& name)
   {
      return package + "::" + name;
   }
   
   static bool isTruthy(const std::string& string)
   {
      std::string lower = string_utils::trimWhitespace(
               boost::algorithm::to_lower_copy(string));
      
      return lower == "true";
   }

   std::map<std::string, AddinSpecification> addins_;
};

// maintain single "active" registry that is updatable as a result of indexing
// and which is persisted to disk

boost::shared_ptr<AddinRegistry> s_pCurrentRegistry =
                                    boost::make_shared<AddinRegistry>();

FilePath addinRegistryPath()
{
   return module_context::userScratchPath().completeChildPath("addin_registry");
}

void updateAddinRegistry(boost::shared_ptr<AddinRegistry> pRegistry)
{
   // update registry in memory + on disk
   s_pCurrentRegistry = pRegistry;
   s_pCurrentRegistry->saveToFile(addinRegistryPath());
   
   // notify client
   json::Value data = s_pCurrentRegistry->toJson();
   ClientEvent event(client_events::kAddinRegistryUpdated, data);
   module_context::enqueClientEvent(event);
}

void loadAddinRegistry()
{
   s_pCurrentRegistry->loadFromFile(addinRegistryPath());
}

AddinRegistry& addinRegistry()
{
   return *s_pCurrentRegistry;
}

class AddinWorker : public ppe::Worker
{
   void onIndexingStarted()
   {
      // initialize registry
      pRegistry_ = boost::make_shared<AddinRegistry>();
      
      // discover path to user config directory
      if (r::version_info::currentRVersion().versionMajor() >= 4)
      {
         Error error = r::exec::RFunction("tools:::R_user_dir")
               .addParam("package", "")
               .addParam("which", "config")
               .call(&userConfigPath_);
         if (error)
            LOG_ERROR(error);
      }
   }
   
   void onWork(const std::string& pkgName, const FilePath& pkgPath)
   {
      // first, check for bundled addins
      FilePath bundledAddinsPath = pkgPath.completeChildPath("rstudio/addins.dcf");
      if (bundledAddinsPath.exists())
         pRegistry_->add(pkgName, bundledAddinsPath);
      
      // next, check for addins in R_user_dir() folder
      if (!userConfigPath_.isEmpty())
      {
         FilePath configAddinsPath = userConfigPath_.completeChildPath(pkgName + "/rstudio/addins.dcf");
         if (configAddinsPath.exists())
            pRegistry_->add(pkgName, configAddinsPath);
      }
   }
   
   void onIndexingCompleted(json::Object* pPayload)
   {
      // finalize by indexing current package
      if (isDevtoolsLoadAllActive())
      {
         FilePath pkgPath = projects::projectContext().buildTargetPath();
         FilePath addinPath = pkgPath.completeChildPath("inst/rstudio/addins.dcf");
         if (addinPath.exists())
         {
            std::string pkgName = projects::projectContext().packageInfo().name();
            pRegistry_->add(pkgName, addinPath);
         }
      }

      // update the addin registry
      updateAddinRegistry(pRegistry_);

      // handle pending continuations
      json::Object registryJson = addinRegistry().toJson();
      for (json::JsonRpcFunctionContinuation continuation : continuations_)
      {
         json::JsonRpcResponse response;
         response.setResult(registryJson);
         continuation(Success(), &response);
      }
      
      // provide registry as json
      (*pPayload)["addin_registry"] = registryJson;

      // reset
      continuations_.clear();
      pRegistry_.reset();
   }

public:
   
   AddinWorker() : ppe::Worker() {}
   
   void addContinuation(json::JsonRpcFunctionContinuation continuation)
   {
      continuations_.push_back(continuation);
   }

private:
   boost::shared_ptr<AddinRegistry> pRegistry_;
   std::vector<json::JsonRpcFunctionContinuation> continuations_;
   FilePath userConfigPath_;
};

boost::shared_ptr<AddinWorker>& addinWorker()
{
   static boost::shared_ptr<AddinWorker> instance(new AddinWorker);
   return instance;
}

void indexLibraryPathsWithContinuation(
      json::JsonRpcFunctionContinuation continuation)
{
   if (continuation)
      addinWorker()->addContinuation(continuation);
}

void getRAddins(const json::JsonRpcRequest& request,
                const json::JsonRpcFunctionContinuation& continuation)
{
   // read params
   bool reindex = false;
   Error error = json::readParams(request.params, &reindex);
   if (error)
   {
      json::JsonRpcResponse response;
      continuation(error, &response);
      return;
   }

   // if we aren't reindexing or if the packages pane is disabled (indicating
   // that the user has told us they don't want aggressive crawling of the
   // package libraries) then just return what's in the cache. note that in
   // the case of a disabled packages pane we'll still get the benefit of
   // the library crawl that is done at startup.
   if (!reindex || module_context::disablePackages())
   {
      json::JsonRpcResponse response;
      response.setResult(addinRegistry().toJson());
      continuation(Success(), &response);
   }
   // otherwise re-index and arrange for the reply to occur once
   // re-indexing is completed
   else
   {
      indexLibraryPathsWithContinuation(continuation);
   }
}

Error noSuchAddin(const ErrorLocation& errorLocation)
{
   return systemError(boost::system::errc::invalid_argument, errorLocation);
}

Error executeRAddin(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string commandId;
   Error error;
   
   error = json::readParams(request.params, &commandId);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   std::vector<std::string> splat = core::algorithm::split(commandId, "::");
   if (splat.size() != 2)
   {
      LOG_ERROR_MESSAGE("unexpected command id '" + commandId + "'");
      return Success();
   }
   
   std::string pkgName = splat[0];
   std::string cmdName = splat[1];
   
   if (!addinRegistry().contains(pkgName, cmdName))
   {
      std::string message = "no addin with id '" + commandId + "' registered";
      pResponse->setError(noSuchAddin(ERROR_LOCATION), json::Value(message));
      return Success();
   }
   
   SEXP fnSEXP = r::sexp::findFunction(cmdName, pkgName);
   if (fnSEXP == R_UnboundValue)
   {
      std::string message =
            "no function '" + cmdName + "' found in package '" + pkgName + "'";
      pResponse->setError(noSuchAddin(ERROR_LOCATION), json::Value(message));
      return Success();
   }
   
    error = r::exec::RFunction(fnSEXP).call();
    if (error)
    {
       LOG_ERROR(error);
       return error;
    }
   
   return Success();
}

void onConsolePrompt(const std::string&)
{
   Error error = r::exec::RFunction(".rs.addins.removeShinyResponseFilter").call();
   if (error)
      LOG_ERROR(error);

   s_consolePromptHandler.disconnect();
}

SEXP rs_registerAddinConsolePromptHandler()
{
   using namespace module_context;

   s_consolePromptHandler = events().onConsolePrompt.connect(onConsolePrompt);

   return R_NilValue;
}

} // end anonymous namespace

core::json::Value addinRegistryAsJson()
{
   return addinRegistry().toJson();
}
  
Error initialize()
{
   using boost::bind;
   using namespace module_context;
   
   // load cached registry
   loadAddinRegistry();
   
   // register worker
   ppe::indexer().addWorker(addinWorker());

   // register call methods
   RS_REGISTER_CALL_METHOD(rs_registerAddinConsolePromptHandler);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRAddins.R"))
         (bind(registerAsyncRpcMethod, "get_r_addins", getRAddins))
         (bind(registerRpcMethod, "execute_r_addin", executeRAddin));
   
   return initBlock.execute();
}

} // namespace r_addins
} // namespace modules
} // namespace session
} // namespace rstudio

