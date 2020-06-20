/*
 * Job.hpp
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

#ifndef SESSION_JOBS_JOB_HPP
#define SESSION_JOBS_JOB_HPP

#include <string>
#include <shared_core/json/Json.hpp>
#include <r/RSexp.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

enum JobState {
   // invalid state sentry
   JobInvalid    = 0,

   // valid job states
   JobIdle       = 1,
   JobRunning    = 2,
   JobSucceeded  = 3,
   JobCancelled  = 4,
   JobFailed     = 5,

   // min/max valid state sentries
   JobStateMin   = JobIdle,
   JobStateMax   = JobFailed
};

enum JobType {
   JobTypeUnknown = 0,
   JobTypeSession = 1, // local job, child of rsession
   JobTypeLauncher = 2 // cluster job via job launcher
};

class Job
{
public:
   Job();

   Job(const std::string& id,
       time_t recorded,
       time_t started,
       time_t completed,
       const std::string& name,
       const std::string& status,
       const std::string& group,
       int progress, 
       int max,
       JobState state,
       JobType type,
       const std::string& cluster,
       bool autoRemove,
       SEXP actions,
       bool show,
       bool saveOutput,
       const std::vector<std::string>& tags);

   // job ID (machine-generated)
   std::string id() const;

   // name of job (user-defined)
   std::string name() const;

   // job status; updated throughout its lifetime
   std::string status() const;

   // group job belongs to
   std::string group() const;

   // the number of progress units that have been completed so far
   int progress() const;

   // the total number of progress units
   int max() const;

   // the current state of the job
   JobState state() const;

   // type of job
   JobType type() const;

   // cluster for launcher jobs
   std::string cluster() const;

   // job tags
   std::vector<std::string> tags() const;

   // whether the job is complete
   bool complete() const;
   static bool completedState(JobState state);

   // whether the job should be cleaned up automatically when complete
   bool autoRemove() const;

   // whether the client is listening to the job
   void setListening(bool listening);

   // execute a custom (user-defined) action
   core::Error executeAction(const std::string& name);

   // add and retrieve output
   void addOutput(const std::string& output, bool error);
   core::json::Array output(int position);

   // whether the job pane should should be shown at start
   bool show() const;
   
   // whether the job should persist its output
   bool saveOutput() const;

   // timing
   time_t recorded() const;
   time_t started() const;
   time_t completed() const;

   void setProgress(int units);
   void setProgressMax(int units);
   void setStatus(const std::string& status);
   void setState(JobState state);

   // convert job to/from JSON
   core::json::Object toJson() const;
   static core::Error fromJson(const core::json::Object& src, 
                               boost::shared_ptr<Job>* pJob);

   static std::string stateAsString(JobState state);
   static JobState stringAsState (const std::string& state);

   // remove any persisted state
   void cleanup();

private:
   core::FilePath jobCacheFolder();
   core::FilePath outputCacheFile();

   std::string id_;
   std::string name_;
   std::string status_;
   std::string group_;

   JobState state_;
   JobType type_;
   std::string cluster_;

   int progress_;
   int max_;

   time_t recorded_;   // when the job was added
   time_t started_;    // when the job began executing
   time_t completed_;  // when the job completed executing

   bool autoRemove_;
   bool listening_;
   bool saveOutput_;
   bool show_;

   r::sexp::PreservedSEXP actions_;

   std::vector<std::string> tags_;
};



} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_JOBS_JOB_HPP
