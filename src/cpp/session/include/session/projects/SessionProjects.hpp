/*
 * SessionProjects.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_PROJECTS_PROJECTS_HPP
#define SESSION_PROJECTS_PROJECTS_HPP


#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>

#include <core/FilePath.hpp>

#include <core/r_util/RProjectFile.hpp>

#include <core/r_util/RSourceIndex.hpp>
 
namespace session {
namespace projects {

class ProjectContext : boost::noncopyable
{
public:
   ProjectContext() {}
   virtual ~ProjectContext() {}

   core::Error initialize(const core::FilePath& projectFile,
                          std::string* pUserErrMsg);

public:
   bool hasProject() const { return !file_.empty(); }

   const core::FilePath& file() const { return file_; }
   const core::FilePath& directory() const { return directory_; }
   const core::FilePath& scratchPath() const { return scratchPath_; }

   const core::r_util::RProjectConfig& config() const { return config_; }
   void setConfig(const core::r_util::RProjectConfig& config)
   {
      config_ = config;
   }

public:
   static core::r_util::RProjectConfig defaultConfig();

private:
   core::FilePath file_;
   core::FilePath directory_;
   core::FilePath scratchPath_;
   core::r_util::RProjectConfig config_;
};

const ProjectContext& projectContext();

} // namespace projects
} // namesapce session

#endif // SESSION_PROJECTS_PROJECTS_HPP
