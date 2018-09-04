/*
 * SessionAsyncRProcesss.hpp
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


#ifndef SESSION_ASYNC_R_PROCESS_HPP
#define SESSION_ASYNC_R_PROCESS_HPP

#include <boost/enable_shared_from_this.hpp>

#include <core/system/Types.hpp>
#include <core/system/Process.hpp>

namespace core
{
   class FilePath;
}

namespace rstudio {
namespace session {
namespace async_r {

enum AsyncRProcessOptions
{
   R_PROCESS_NORMAL         = 1 << 0,
   R_PROCESS_REDIRECTSTDERR = 1 << 1,
   R_PROCESS_VANILLA        = 1 << 2,
   R_PROCESS_AUGMENTED      = 1 << 3,
   R_PROCESS_NO_RDATA       = 1 << 4
};

inline AsyncRProcessOptions operator | (AsyncRProcessOptions lhs,
                                        AsyncRProcessOptions rhs)
{
   return static_cast<AsyncRProcessOptions>(
            static_cast<int>(lhs) | static_cast<int>(rhs));
}

class AsyncRProcess :
      boost::noncopyable,
      public boost::enable_shared_from_this<AsyncRProcess>
{
public:
   AsyncRProcess();
   virtual ~AsyncRProcess();

   void start(const std::string& rCommand,
              const core::FilePath& workingDir,
              AsyncRProcessOptions rOptions,
              std::vector<core::FilePath> rSourceFiles = {},
              const std::string &input = {})
   {
      start(rCommand, core::system::Options(), workingDir, rOptions, 
            rSourceFiles, input);
   }

   void start(const std::string& command,
              core::system::Options environment,
              const core::FilePath& workingDir,
              AsyncRProcessOptions rOptions,
              std::vector<core::FilePath> rSourceFiles = {},
              const std::string& input = {});

   bool isRunning();
   void terminate();
   void markCompleted();
   bool terminationRequested();

protected:
   // NOTE: implementing these methods is optional; deriving classes
   // do not need to call the associated base-class method
   virtual void onStarted(core::system::ProcessOperations& operations);
   virtual bool onContinue();
   virtual void onStdout(const std::string& output);
   virtual void onStderr(const std::string& output);
   virtual void onCompleted(int exitStatus);

private:
   void onProcessStarted(core::system::ProcessOperations& operations);
   bool onProcessContinue();
   void onProcessStdout(const std::string& output);
   void onProcessStderr(const std::string& output);
   void onProcessCompleted(int exitStatus);
   
   bool isRunning_;
   bool terminationRequested_;
   std::string input_;
   core::FilePath scriptPath_;
};

} // namespace async_r
} // namespace session
} // namespace rstudio

#endif
