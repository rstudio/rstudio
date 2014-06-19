/*
 * SessionAsyncRProcesss.hpp
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


#ifndef SESSION_ASYNC_R_PROCESS_HPP
#define SESSION_ASYNC_R_PROCESS_HPP

namespace core
{
   class FilePath;
}

namespace session {   
namespace async_r {

enum AsyncRProcessOptions
{
   R_PROCESS_NORMAL         = 1 << 0,
   R_PROCESS_REDIRECTSTDERR = 1 << 1,
   R_PROCESS_VANILLA        = 1 << 2
};
    
class AsyncRProcess :
      boost::noncopyable,
      public boost::enable_shared_from_this<AsyncRProcess>
{
public:
   AsyncRProcess();
   virtual ~AsyncRProcess();

   void start(const char* rCommand, const core::FilePath& workingDir,
              AsyncRProcessOptions rOptions);
   bool isRunning();
   void terminate();
   void markCompleted();

protected:
   virtual bool onContinue();
   virtual void onStdout(const std::string& output);
   virtual void onStderr(const std::string& output);

   virtual void onCompleted(int exitStatus) = 0;

private:
   void onProcessCompleted(int exitStatus);
   bool isRunning_;
   bool terminationRequested_;
};

} // namespace async_r
} // namespace session

#endif
