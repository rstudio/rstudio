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
                      const std::string& binding)
      : name_(name), package_(package), title_(title),
        description_(description), binding_(binding)
   {
   }
   
   const std::string& getName() const { return name_; }
   const std::string& getPackage() const { return package_; }
   const std::string& getTitle() const { return title_; }
   const std::string& getDescription() const { return description_; }
   const std::string& getBinding() const { return binding_; }
   
   json::Object toJson()
   {
      json::Object object;
      
      object["name"] = name_;
      object["package"] = package_;
      object["title"] = title_;
      object["description"] = description_;
      object["binding"] = binding_;
      
      return object;
   }
   
private:
   std::string name_;
   std::string package_;
   std::string title_;
   std::string description_;
   std::string binding_;
};

class AddinRegistry : boost::noncopyable
{
public:
   
   void add(const std::string& package, const AddinSpecification& spec)
   {
      addins_[constructKey(package, spec.getBinding())] = spec;
   }
   
   bool contains(const std::string& package, const std::string& name)
   {
      return addins_.count(constructKey(package, name));
   }

   const AddinSpecification& get(const std::string& package, const std::string& name)
   {
      return addins_[constructKey(package, name)];
   }
   
   json::Object toJson()
   {
      json::Object object;
      
      BOOST_FOREACH(const std::string& key, addins_ | boost::adaptors::map_keys)
      {
         object[key] = addins_[key].toJson();
      }
      
      return object;
   }

   std::size_t size() const { return addins_.size(); }
   
private:
   
   static std::string constructKey(const std::string& package, const std::string& name)
   {
      return package + "::" + name;
   }

   std::map<std::string, AddinSpecification> addins_;
};

AddinRegistry& addinRegistry()
{
   static AddinRegistry instance;
   return instance;
}

// TODO: This probably belongs in a separate module and could be exported
std::vector<FilePath> getLibPaths()
{
   std::vector<std::string> libPathsString;
   r::exec::RFunction rfLibPaths(".libPaths");
   Error error = rfLibPaths.call(&libPathsString);
   if (error)
      LOG_ERROR(error);
   
   std::vector<FilePath> libPaths(libPathsString.size());
   BOOST_FOREACH(const std::string& path, libPathsString)
   {
      libPaths.push_back(module_context::resolveAliasedPath(path));
   }
   
   return libPaths;
}

std::map<std::string, std::string> parseAddinDcf(const std::string& contents)
{
   // read and parse the DCF file
   std::map<std::string, std::string> fields;
   std::string errMsg;
   Error error = text::parseDcfFile(contents, true, &fields, &errMsg);
   if (error)
      LOG_ERROR(error);
   
   return fields;
}

void registerAddin(const std::string& pkgName,
                   std::map<std::string, std::string>& fields)
{
   addinRegistry().add(pkgName, AddinSpecification(
         fields["Name"],
         pkgName,
         fields["Title"],
         fields["Description"],
         fields["Binding"]));
}

void registerAddins(const std::string& pkgName, const FilePath& addinPath)
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
      registerAddin(pkgName, fields);
   }
}

class AddinIndexer : public boost::noncopyable
{
public:
   
   void initialize(const std::vector<FilePath>& libPaths)
   {
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
   
   bool pending()
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
         return pending();
      }
      
      std::string pkgName = pkgPath.filename();
      registerAddins(pkgName, addinPath);
      
      ++index_;
      return pending();
   }
   
private:
   std::vector<FilePath> children_;
   std::size_t n_;
   std::size_t index_;
};

AddinIndexer& addinIndexer()
{
   static AddinIndexer instance;
   return instance;
}

void indexLibraryPaths(const std::vector<FilePath>& libPaths)
{
   addinIndexer().initialize(libPaths);
   module_context::scheduleIncrementalWork(
            boost::posix_time::milliseconds(10),
            boost::bind(&AddinIndexer::work, &addinIndexer()),
            true);
}

void onDeferredInit(bool newSession)
{
   indexLibraryPaths(getLibPaths());
}

bool waitForGetRAddins(json::JsonRpcFunctionContinuation continuation)
{
   if (addinIndexer().pending())
      return true;
   
   json::JsonRpcResponse response;
   response.setResult(addinRegistry().toJson());
   continuation(Success(), &response);
   return false;
}

void getRAddins(const json::JsonRpcRequest& request,
                const json::JsonRpcFunctionContinuation& continuation)
{
   module_context::schedulePeriodicWork(
            boost::posix_time::milliseconds(20),
            boost::bind(waitForGetRAddins, continuation),
            true);
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

