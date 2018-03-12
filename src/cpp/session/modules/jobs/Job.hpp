/*
 * Job.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
#include <core/json/Json.hpp>

namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

class Job
{
public:
   Job();

   // job ID (machine-generated)
   std::string id();

   // name of job (user-defined)
   std::string name();

   // job status; updated throughout its lifetime
   std::string status();

   // group job belongs to
   std::string group();

   // the number of progress units that have been completed so far
   int progress();

   // the total number of progress units
   int max();

   // whether the job is currently executing
   bool running();

   // whether the job has completed
   bool completed();

   // convert job to/from JSON
   core::json::Object toJson();
   static Job fromJson(const core::json::Object& src);

private:
   std::string id_;
   std::string name_;
   std::string status_;
   std::string group_;

   int max_;
   int progress_;

   bool running_;
   bool completed_;
};


} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_JOBS_JOB_HPP
