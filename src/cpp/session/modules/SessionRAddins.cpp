/*
 * SessionRAddins.cpp.in
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

#include <core/Macros.hpp>
#include <core/Algorithm.hpp>
#include <core/Debug.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/DcfParser.hpp>

#include <boost/regex.hpp>
#include <boost/foreach.hpp>
#include <boost/bind.hpp>
#include <boost/range/adaptor/map.hpp>
#include <boost/system/error_code.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace r_addins {

namespace {

class AddinSpecification
{
public:
   
   AddinSpecification() {}
   
   AddinSpecification(const std::string& name,
                      const std::string& package,
                      const std::string& title,
                      const std::string& description,
                      bool interactive,
                      const std::string& binding)
      : name_(name), package_(package), title_(title),
        description_(description), interactive_(interactive),
        binding_(binding)
   {
   }
   
   const std::string& getName() const { return name_; }
   const std::string& getPackage() const { return package_; }
   const std::string& getTitle() const { return title_; }
   const std::string& getDescription() const { return description_; }
   bool isInteractive() const { return interactive_; }
   const std::string& getBinding() const { return binding_; }
   
   json::Object toJson() const
   {
      json::Object object;
      
      object["name"] = name_;
      object["package"] = package_;
      object["title"] = title_;
      object["description"] = description_;
      object["interactive"] = interactive_;
      object["binding"] = binding_;
      
      return object;
   }
   
private:
   std::string name_;
   std::string package_;
   std::string title_;
   std::string description_;
   bool interactive_;
   std::string binding_;
};

class AddinRegistry : boost::noncopyable
{
public:
   
   void saveToFile(const core::FilePath& filePath) const
   {
      boost::shared_ptr<std::ostream> pStream;
      Error error = filePath.open_w(&pStream);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      json::writeFormatted(toJson(), *pStream);
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
      if (json::parse(contents, &parsedJson) &&
          json::isType<json::Object>(parsedJson))
      {
         json::Object addinsJson = parsedJson.get_obj();

         BOOST_FOREACH(const std::string& key, addinsJson | boost::adaptors::map_keys)
         {
            json::Value valueJson = addinsJson.at(key);
            if(json::isType<json::Object>(valueJson))
            {
               bool interactive;
               std::string name, package, title, description, binding;
               Error error = json::readObject(valueJson.get_obj(),
                                              "name", &name,
                                              "package", &package,
                                              "title", &title,
                                              "description", &description,
                                              "interactive", &interactive,
                                              "binding", &binding);
               if (error)
               {
                  LOG_ERROR(error);
                  continue;
               }

               addins_[key] = AddinSpecification(name,
                                                 package,
                                                 title,
                                                 description,
                                                 interactive,
                                                 binding);
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
            fields["Binding"]));
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

      boost::sregex_token_iterator it(contents.begin(), contents.end(), reSeparator, -1);
      boost::sregex_token_iterator end;

      for (; it != end; ++it)
      {
         std::map<std::string, std::string> fields = parseAddinDcf(*it);
         add(pkgName, fields);
      }
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
      
      BOOST_FOREACH(const std::string& key, addins_ | boost::adaptors::map_keys)
      {
         object[key] = addins_.at(key).toJson();
      }
      
      return object;
   }

   std::size_t size() const { return addins_.size(); }
   
private:
   
   static std::map<std::string, std::string> parseAddinDcf(
                                          const std::string& contents)
   {
      // read and parse the DCF file
      std::map<std::string, std::string> fields;
      std::string errMsg;
      Error error = text::parseDcfFile(contents, true, &fields, &errMsg);
      if (error)
         LOG_ERROR(error);

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
   return module_context::userScratchPath().childPath("addin_registry");
}

void updateAddinRegistry(boost::shared_ptr<AddinRegistry> pRegistry)
{
   s_pCurrentRegistry = pRegistry;
   s_pCurrentRegistry->saveToFile(addinRegistryPath());
}

void loadAddinRegistry()
{
   s_pCurrentRegistry->loadFromFile(addinRegistryPath());
}

AddinRegistry& addinRegistry()
{
   return *s_pCurrentRegistry;
}

class AddinIndexer : public boost::noncopyable
{
public:
   
   AddinIndexer()
   {
      clear();
   }

   void start(const std::vector<FilePath>& libPaths)
   {
      // reset instance data
      clear();

      // prime work list
      std::vector<FilePath> pkgPaths;
      BOOST_FOREACH(const FilePath& libPath, libPaths)
      {
         if (!libPath.exists())
            continue;
         
         pkgPaths.clear();
         Error error = libPath.children(&pkgPaths);
         if (error)
            LOG_ERROR(error);
         children_.insert(
                  children_.end(),
                  pkgPaths.begin(),
                  pkgPaths.end());
      }
      
      n_ = children_.size();
   }

   void addContinuation(json::JsonRpcFunctionContinuation continuation)
   {
      continuations_.push_back(continuation);
   }

   bool running()
   {
      return index_ != n_;
   }
   
   bool work()
   {
      // std::cout << "Job " << index_ + 1 << " of " << n_ << "\n";
      // ::usleep(10000);
      
      const FilePath& pkgPath = children_[index_];
      FilePath addinPath = pkgPath.childPath("rstudio/addins.dcf");
      if (!addinPath.exists())
      {
         ++index_;
         return maybeFinishRunning();
      }
      
      std::string pkgName = pkgPath.filename();
      pRegistry_->add(pkgName, addinPath);
      
      ++index_;
      return maybeFinishRunning();
   }

private:

   // reset all instance data
   void clear()
   {
      children_.clear();
      n_ = 0;
      index_ = 0;
      pRegistry_ = boost::make_shared<AddinRegistry>();
      continuations_.clear();
   }

   // check for still running, if we aren't still running then complete
   // our work and return false, otherwise return true
   bool maybeFinishRunning()
   {
      if (!running())
      {
         // update the addin registry
         updateAddinRegistry(pRegistry_);

         // handle pending continuations
         json::Object registryJson = addinRegistry().toJson();
         BOOST_FOREACH(json::JsonRpcFunctionContinuation continuation, continuations_)
         {
            json::JsonRpcResponse response;
            response.setResult(registryJson);
            continuation(Success(), &response);
         }

         // clear instance data and return false
         clear();
         return false;
      }
      else
      {
         return true;
      }
   }
   
private:
   std::vector<FilePath> children_;
   std::size_t n_;
   std::size_t index_;
   boost::shared_ptr<AddinRegistry> pRegistry_;
   std::vector<json::JsonRpcFunctionContinuation> continuations_;
};

AddinIndexer& addinIndexer()
{
   static AddinIndexer instance;
   return instance;
}

void indexLibraryPaths(
   const std::vector<FilePath>& libPaths,
   json::JsonRpcFunctionContinuation continuation = json::JsonRpcFunctionContinuation())
{
   // start if we arent' already running
   if (!addinIndexer().running())
   {
      // start indexer
      addinIndexer().start(libPaths);

      // register continuation if provided
      if (continuation)
         addinIndexer().addContinuation(continuation);

      // schedule work
      module_context::scheduleIncrementalWork(
               boost::posix_time::milliseconds(300),
               boost::posix_time::milliseconds(20),
               boost::bind(&AddinIndexer::work, &addinIndexer()),
               false);
   }
   else
   {
      // register continuation if provided
      if (continuation)
         addinIndexer().addContinuation(continuation);
   }
}

void onDeferredInit(bool newSession)
{
   // load cached registry
   loadAddinRegistry();

   // re-index
   indexLibraryPaths(module_context::getLibPaths());
}

void getRAddins(const json::JsonRpcRequest& request,
                const json::JsonRpcFunctionContinuation& continuation)
{
   // read params
   bool reindex;
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
      indexLibraryPaths(module_context::getLibPaths(), continuation);
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
      pResponse->setError(noSuchAddin(ERROR_LOCATION), message);
      return Success();
   }
   
   SEXP fnSEXP = r::sexp::findFunction(cmdName, pkgName);
   if (fnSEXP == R_UnboundValue)
   {
      std::string message =
            "no function '" + cmdName + "' found in package '" + pkgName + "'";
      pResponse->setError(noSuchAddin(ERROR_LOCATION), message);
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

} // end anonymous namespace
  
Error initialize()
{
   using boost::bind;
   using namespace module_context;
   
   events().onDeferredInit.connect(onDeferredInit);
   
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

