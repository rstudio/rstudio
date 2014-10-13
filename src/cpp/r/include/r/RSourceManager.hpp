/*
 * RSourceManager.hpp
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

#ifndef R_SOURCE_MANAGER_HPP
#define R_SOURCE_MANAGER_HPP

#include <time.h>

#include <string>
#include <vector>

#include <boost/utility.hpp>
#include <boost/unordered_map.hpp>

#include <core/FilePath.hpp>

namespace rstudiocore {
   class Error ;
}

namespace r {

// singleton
class SourceManager ;
SourceManager& sourceManager();

class SourceManager : boost::noncopyable
{
private:
   SourceManager() : autoReload_(false) {}
   friend SourceManager& sourceManager();
   // COPYING: boost::noncopyable
   
public:
   
   bool autoReload() const { return autoReload_; }
   void setAutoReload(bool autoReload) { autoReload_ = autoReload; }
   
   rstudiocore::Error sourceTools(const rstudiocore::FilePath& filePath);
   void ensureToolsLoaded();

   rstudiocore::Error sourceLocal(const rstudiocore::FilePath& filePath);
   
   void reloadIfNecessary();
   
private:   
   // data types
   struct SourcedFileInfo
   {
      SourcedFileInfo() : lastWriteTime((time_t)-1), local(true) {}
      SourcedFileInfo(time_t lastWriteTime, bool local)
      :  lastWriteTime(lastWriteTime), 
      local(local)
      {
      }
      time_t lastWriteTime;
      bool local;
   };   
   typedef boost::unordered_map<std::string, SourcedFileInfo> SourcedFileMap;
   
   // helper functions
   rstudiocore::Error source(const rstudiocore::FilePath& filePath, bool local);
   void reSourceTools(const rstudiocore::FilePath& filePath);
   void recordSourcedFile(const rstudiocore::FilePath& filePath, bool local);
   void reloadSourceIfNecessary(const SourcedFileMap::value_type& value);
   
   // members
   bool autoReload_ ;
   SourcedFileMap sourcedFiles_ ;
   std::vector<rstudiocore::FilePath> toolsFilePaths_;
};
   
} // namespace r


#endif // R_SOURCE_MANAGER_HPP 

