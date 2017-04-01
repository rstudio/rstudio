/*
 * SessionConnectionsWorker.cpp
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
#include <r/RRoutines.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionPackageProvidedExtension.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {

bool isDevtoolsLoadAllActive()
{
   std::vector<std::string> search;
   Error error = r::exec::RFunction("search").call(&search);
   if (error)
      return false;
   
   return std::find(search.begin(), search.end(), "devtools_shims") != search.end();
}

class ConnectionsIndexEntry
{
public:
   
   ConnectionsIndexEntry() {}
   
   ConnectionsIndexEntry(const std::string& name,
                         const std::string& package,
                         const std::string& shinyapp,
                         const std::string& help,
                         const std::string& icon)
      : name_(name), package_(package), shinyapp_(shinyapp), help_(help), icon_(icon)
   {
   }
   
   const std::string& getName() const { return name_; }
   const std::string& getPackage() const { return package_; }
   const std::string& getShinyApp() const { return shinyapp_; }
   const std::string& getHelp() const { return help_; }
   const std::string& getIcon() const { return icon_; }

   json::Object toJson() const
   {
      json::Object object;
      
      object["name"] = name_;
      object["package"] = package_;
      object["shinyapp"] = shinyapp_;
      object["help"] = help_;
      object["icon"] = icon_;
      
      return object;
   }
   
private:
   std::string name_;
   std::string package_;
   std::string shinyapp_;
   std::string help_;
   std::string icon_;
};

class ConnectionsRegistry : boost::noncopyable
{
public:

   void add(const std::string& package, const ConnectionsIndexEntry& spec)
   {
      connections_[constructKey(package, spec.getName())] = spec;
   }

   void add(const std::string& pkgName,
            std::map<std::string, std::string>& fields)
   {  
      add(pkgName, ConnectionsIndexEntry(
         fields["Name"],
         pkgName,
         fields["ShinyApp"],
         fields["HelpUrl"],
         fields["Icon"]));
   }

   void add(const std::string& pkgName, const FilePath& connectionExtensionPath)
   {
      static const boost::regex reSeparator("\\n{2,}");

      std::string contents;
      Error error = core::readStringFromFile(connectionExtensionPath, &contents, string_utils::LineEndingPosix);
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
            std::map<std::string, std::string> fields = parseConnectionsDcf(*it);
            add(pkgName, fields);
         }
      }
      CATCH_UNEXPECTED_EXCEPTION;
   }
   
   bool contains(const std::string& package, const std::string& name)
   {
      return connections_.count(constructKey(package, name));
   }

   const ConnectionsIndexEntry& get(const std::string& package, const std::string& name)
   {
      return connections_[constructKey(package, name)];
   }
   
   json::Object toJson() const
   {
      json::Object object;
      
      BOOST_FOREACH(const std::string& key, connections_ | boost::adaptors::map_keys)
      {
         object[key] = connections_.at(key).toJson();
      }
      
      return object;
   }

   std::size_t size() const { return connections_.size(); }
   
private:
   
   static std::map<std::string, std::string> parseConnectionsDcf(
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

   std::map<std::string, ConnectionsIndexEntry> connections_;
};

boost::shared_ptr<ConnectionsRegistry> s_pCurrentConnectionsRegistry =
    boost::make_shared<ConnectionsRegistry>();

ConnectionsRegistry& connectionsRegistry()
{
   return *s_pCurrentConnectionsRegistry;
}

void updateConnectionsRegistry(boost::shared_ptr<ConnectionsRegistry> pRegistry)
{
   s_pCurrentConnectionsRegistry = pRegistry;
}

class ConnectionsWorker : public ppe::Worker
{
   void onIndexingStarted()
   {
      pRegistry_ = boost::make_shared<ConnectionsRegistry>();
   }
   
   void onWork(const std::string& pkgName, const FilePath& connectionExtensionPath)
   {
      pRegistry_->add(pkgName, connectionExtensionPath);
   }
   
   void onIndexingCompleted(json::Object* pPayload)
   {
      // finalize by indexing current package
      if (isDevtoolsLoadAllActive())
      {
         FilePath pkgPath = projects::projectContext().buildTargetPath();
         FilePath extensionPath = pkgPath.childPath("inst/rstudio/connections.dcf");
         if (extensionPath.exists())
         {
            std::string pkgName = projects::projectContext().packageInfo().name();
            pRegistry_->add(pkgName, extensionPath);
         }
      }

      // update the connections registry
      updateConnectionsRegistry(pRegistry_);

      // handle pending continuations
      json::Object registryJson = connectionsRegistry().toJson();
      BOOST_FOREACH(json::JsonRpcFunctionContinuation continuation, continuations_)
      {
         json::JsonRpcResponse response;
         response.setResult(registryJson);
         continuation(Success(), &response);
      }
      
      // provide registry as json
      (*pPayload)["connections_registry"] = registryJson;

      // reset
      continuations_.clear();
      pRegistry_.reset();
   }

public:
   
   ConnectionsWorker() : ppe::Worker("rstudio/connections.dcf") {}
   
   void addContinuation(json::JsonRpcFunctionContinuation continuation)
   {
      continuations_.push_back(continuation);
   }

private:
   boost::shared_ptr<ConnectionsRegistry> pRegistry_;
   std::vector<json::JsonRpcFunctionContinuation> continuations_;
};

boost::shared_ptr<ConnectionsWorker>& connectionsWorker()
{
   static boost::shared_ptr<ConnectionsWorker> instance(new ConnectionsWorker);
   return instance;
}

} // end anonymous namespace

core::json::Value connectionsRegistryAsJson()
{
   return connectionsRegistry().toJson();
}

void registerConnectionsWorker()
{
   ppe::indexer().addWorker(connectionsWorker());
}

} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

