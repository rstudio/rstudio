/*
 * SessionConnectionsIndexer.cpp
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
#include <r/RExec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionPackageProvidedExtension.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {


namespace {


class SessionConnectionsIndexEntry
{
public:
   
   SessionConnectionsIndexEntry() {}
   
   SessionConnectionsIndexEntry(const std::string& name,
                                const std::string& package)
      : name_(name), package_(package)
   {
   }
   
   const std::string& getName() const { return name_; }
   const std::string& getPackage() const { return package_; }

   json::Object toJson() const
   {
      json::Object object;
      
      object["name"] = name_;
      object["package"] = package_;
      
      return object;
   }
   
private:
   std::string name_;
   std::string package_;
};

class SessionConnectionsIndexRegistry : boost::noncopyable
{
public:

   void add(const std::string& package, const SessionConnectionsIndexEntry& spec)
   {
      connections_[constructKey(package, spec.getName())] = spec;
   }

   void add(const std::string& pkgName,
            std::map<std::string, std::string>& fields)
   {  
      add(pkgName, SessionConnectionsIndexEntry(
            fields["Name"],
            pkgName));
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

      boost::sregex_token_iterator it(contents.begin(), contents.end(), reSeparator, -1);
      boost::sregex_token_iterator end;

      for (; it != end; ++it)
      {
         std::map<std::string, std::string> fields = parseConnectionsDcf(*it);
         add(pkgName, fields);
      }
   }
   
   bool contains(const std::string& package, const std::string& name)
   {
      return connections_.count(constructKey(package, name));
   }

   const SessionConnectionsIndexEntry& get(const std::string& package, const std::string& name)
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

   std::map<std::string, SessionConnectionsIndexEntry> connections_;
};

}

} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

