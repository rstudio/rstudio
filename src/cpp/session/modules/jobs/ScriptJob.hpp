/*
 * ScriptJob.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef SESSION_SCRIPT_JOB_HPP
#define SESSION_SCRIPT_JOB_HPP

#include <session/jobs/Job.hpp>
#include <session/SessionAsyncRProcess.hpp>

#include "AsyncRJobManager.hpp"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

class ScriptLaunchSpec 
{
public:
   // A script consisting of a named code snippet
   ScriptLaunchSpec(
         const std::string& name,
         const std::string& code,
         const core::FilePath& workingDir,
         bool importEnv,
         const std::string& exportEnv);

   // A script consisting of an R file on disk
   ScriptLaunchSpec(
         const std::string& name,
         const core::FilePath& path,
         const std::string& encoding,
         const core::FilePath& workingDir,
         bool importEnv,
         const std::string& exportEnv);

   std::string name() const;
   std::string code() const;
   core::FilePath path() const;
   std::string encoding() const;
   core::FilePath workingDir() const;
   bool importEnv() const;
   std::string exportEnv() const;
   void setProcOptions(async_r::AsyncRProcessOptions options);
   boost::optional<async_r::AsyncRProcessOptions> procOptions();
private:
   std::string name_;
   std::string code_;
   core::FilePath path_;
   std::string encoding_;
   core::FilePath workingDir_;
   bool importEnv_;
   std::string exportEnv_;
   boost::optional<async_r::AsyncRProcessOptions> procOptions_;
};


class ScriptJob : public AsyncRJob
{
public:
   static boost::shared_ptr<ScriptJob> create(
         const ScriptLaunchSpec& spec);
   void start();

private:
   ScriptJob(const ScriptLaunchSpec& spec);
   void onStdout(const std::string& output);
   void onCompleted(int exitStatus);
   void onProgress(const std::string& cat, const std::string& argument);

   ScriptLaunchSpec spec_;
   core::FilePath import_;
   core::FilePath export_;
   core::FilePath tempCode_;
};

core::Error startScriptJob(const ScriptLaunchSpec& spec, 
      std::string *pId);

core::Error startScriptJob(const ScriptLaunchSpec& spec, 
      boost::function<void()> onComplete,
      std::string *pId);

core::Error stopScriptJob(const std::string& id);

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
