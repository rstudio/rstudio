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

}

} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

