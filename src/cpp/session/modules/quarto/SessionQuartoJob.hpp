/*
 * SessionQuartoJob.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_QUARTO_JOB_HPP
#define SESSION_QUARTO_JOB_HPP

#include <string>
#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Types.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/Process.hpp>

#include <session/jobs/JobsApi.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {

class QuartoJob : boost::noncopyable,
                      public boost::enable_shared_from_this<QuartoJob>
{

public:

   virtual ~QuartoJob()
   {
   }

   bool isRunning()
   {
      return pJob_->state() == jobs::JobRunning;
   }

   void stop()
   {
      stopRequested_ = true;

      // on windows we need to be a bit more aggressive (as we've seen cases where
      // the 'stop' doesn't actually work esp. when deno is running a web server
#ifdef _WIN32
      using namespace core::shell_utils;
      if (pid_ > 0)
      {
         ShellCommand cmd("taskkill");
         cmd << "/F" << "/T" << "/PID" << core::safe_convert::numberToString(pid_);
         core::system::ProcessOptions options;
         core::system::ProcessResult result;
         core::Error error = core::system::runCommand(cmd, options, &result);
         if (error)
         {
            LOG_ERROR(error);
         }
         else if (result.exitStatus != EXIT_SUCCESS)
         {
            LOG_ERROR_MESSAGE("Error killing quarto job: " + result.stdErr);
         }
      }
#endif
   }

   void remove()
   {
      // only remove if we are still in the list
      boost::shared_ptr<jobs::Job> pJob;
      if (jobs::lookupJob(pJob_->id(), &pJob))
         jobs::removeJob(pJob_);
   }

protected:
   explicit QuartoJob()
      : stopRequested_(false), pid_(0)
   {
   }

   virtual std::string name()
   {
      return "";
   }

   virtual std::vector<std::string> args()
   {
      return std::vector<std::string>();
   }

   virtual void environment(core::system::Options*)
   {
   }

   virtual core::FilePath workingDir()
   {
      return core::FilePath();
   }

   virtual core::Error start();

   virtual void onStarted(core::system::ProcessOperations& process)
   {
      pid_ = process.getPid();
   }

   virtual bool onContinue()
   {
      return !stopRequested_;
   }

   virtual void onStdOut(const std::string& output)
   {
      pJob_->addOutput(output, false);

   }

   virtual void onStdErr(const std::string& error)
   {
      pJob_->addOutput(error, false);
   }

   virtual void onCompleted(int exitStatus)
   {
      if (stopRequested_)
      {
         setJobState(pJob_, jobs::JobCancelled);
         remove();
      }
      else if (exitStatus == EXIT_SUCCESS)
      {
         setJobState(pJob_, jobs::JobSucceeded);
      }
      else
      {
         setJobState(pJob_, jobs::JobFailed);
      }
   }

protected:
   boost::shared_ptr<jobs::Job> pJob_;

private:
   bool stopRequested_;
   PidType pid_;
};

struct ParsedServerLocation
{
   ParsedServerLocation(int port, const std::string& path, const std::string& filteredOutput)
      : port(port), path(path), filteredOutput(filteredOutput)
   {
   }
   explicit ParsedServerLocation(const std::string& filteredOutput = "")
      : port(0), filteredOutput(filteredOutput)
   {
   }
   int port;
   std::string path;
   std::string filteredOutput;
};


ParsedServerLocation quartoServerLocationFromOutput(const std::string& output);

   
} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_QUARTO_JOB_HPP
