/*
 * ScriptJob.cpp
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

#include <boost/asio.hpp>

#include <r/RExec.hpp>

#include <core/Algorithm.hpp>
#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/jobs/JobsApi.hpp>

#include "ScriptJob.hpp"

#define kProgressDelim  "__"
#define kProgressStart  kProgressDelim "rs_progress_0" kProgressDelim " " 
#define kProgressEnd    " " kProgressDelim "rs_progress_1" kProgressDelim
#define kProgressMiddle " " kProgressDelim " "

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

namespace {

class ScriptJob : public async_r::AsyncRProcess 
{
public:
   static boost::shared_ptr<ScriptJob> create(
         const ScriptLaunchSpec& spec,
         boost::function<void()> onComplete)
   {
      boost::shared_ptr<ScriptJob> pJob(new ScriptJob(spec, onComplete));
      pJob->start();
      return pJob;
   }
   
   std::string id()
   {
      if (job_)
         return job_->id();
      return "";
   }
   
private:
   ScriptJob(const ScriptLaunchSpec& spec, 
         boost::function<void()> onComplete):
      spec_(spec),
      completed_(false),
      onComplete_(onComplete)
   {
   }

   void start()
   {
      Error error;
      r::sexp::Protect protect;

      // create the actions list
      SEXP actions = R_NilValue;
      error = r::exec::RFunction(".rs.scriptActions").call(&actions, &protect);
      if (error)
         LOG_ERROR(error);

      // add the job -- currently idle until we get some content from it
      job_ = addJob(spec_.name(), "", "", 0, JobIdle, JobTypeSession, false, actions, true, {});
      
      std::string importRdata = "NULL";
      std::string exportRdata = "NULL";

      if (spec_.importEnv())
      {
         // create temporary file to save/load the data
         FilePath::tempFilePath(&import_);
         importRdata = "'" + string_utils::utf8ToSystem(import_.absolutePath()) + "'";

         // prepare the environment for the script by exporting the current env
         setJobStatus(job_, "Preparing environment");
         r::exec::RFunction save("save.image");
         save.addParam("file", string_utils::utf8ToSystem(import_.absolutePath()));
         save.addParam("safe", false); // no need to write a temp file
         error = save.call();
         if (error)
         {
            job_->addOutput("Error importing environment: " + error.summary() + "\n", true);
         }

         // clear status in preparation for execution
         setJobStatus(job_, "");
      }

      if (!spec_.exportEnv().empty())
      {
         // if exporting, create a file to host the exported values
         FilePath::tempFilePath(&export_);
         exportRdata = "'" + string_utils::utf8ToSystem(export_.absolutePath()) + "'";
      }

      std::string path;
      if (spec_.code().empty())
      {
         // no code specified to run, so we're running an R script
         path = spec_.path().absolutePath();
      }
      else
      {
         // code specified; inject it into a temporary file
         error = FilePath::tempFilePath(&tempCode_);
         if (!error)
         {
            error = writeStringToFile(tempCode_, spec_.code());
         }

         if (error)
         {
            // emit the error to the job, and allow it to run with no code (so the only result
            // will be this error)
            job_->addOutput("Error writing code to file " + tempCode_.absolutePath() + ": " +
                            error.summary() + "\n", true);
         }
         path = tempCode_.absolutePath();
      }

      // determine encoding
      std::string encoding = "UTF-8";
      if (!spec_.encoding().empty())
          encoding = spec_.encoding();
      
      // form the command to send to R
      std::string cmd = "source('" +
         string_utils::utf8ToSystem(
            string_utils::singleQuotedStrEscape(
                  session::options().modulesRSourcePath()
                                    .complete("SourceWithProgress.R").absolutePath())) + 
         "'); sourceWithProgress(script = '" +
         string_utils::utf8ToSystem(
               string_utils::singleQuotedStrEscape(path)) + "', "
         "encoding = '" + encoding + "', "
         "con = stdout(), "
         "importRdata = " + importRdata + ", "
         "exportRdata = " + exportRdata + ");";
        
      core::system::Options environment;
      async_r::AsyncRProcess::start(cmd.c_str(), environment, spec_.workingDir(),
                                    async_r::R_PROCESS_NO_RDATA);
   }

   void onStdout(const std::string& output)
   {
      // split into lines
      std::vector<std::string> lines = core::algorithm::split(output, "\n");

      // examine each line and look for progress markers; for performance we do this using
      // piecewise string indexing rather than regular expressions
      for (size_t i = 0; i < lines.size(); i++)
      {
         std::string line = lines.at(i);
         
         // initialize string positions (where we found the progress marker)
         size_t start = std::string::npos, 
                middle = std::string::npos, 
                end = std::string::npos;

         // look for the start of the progress marker
         start = line.find(kProgressStart);
         if (start != std::string::npos)
         {
            // ... and for the middle ...
            middle = line.find(kProgressMiddle, 
                               start + std::strlen(kProgressStart));
            if (middle != std::string::npos)
            {
               // ... and finally the end.
               size_t end = line.find(kProgressEnd, middle + std::strlen(kProgressMiddle));
               if (end != std::string::npos)
               {
                  // we found all three, so split up the string accordingly.
                  size_t begin = start + std::strlen(kProgressStart);
                  std::string cat = line.substr(begin, middle - begin);
                  size_t mid = middle + std::strlen(kProgressMiddle);
                  std::string arg = line.substr(mid, end - mid);

                  // invoke the progress handler with our discoveries
                  onProgress(cat, arg);
               }
            }
         }
         if (job_)
         {
            std::string output;
            
            if (start != std::string::npos)
            {
               // emit the output that occurred before the start marker
               output = line.substr(0, start);
            }
            if (end != std::string::npos)
            {
               // emit the output that occurred after the end marker
               output.append(line.substr(end + std::strlen(kProgressEnd)));
            }
            if (start == std::string::npos && end == std::string::npos)
            {
               // no progress markers were found; just emit the whole line
               output.append(line);
            }
            
            if (!output.empty())
            {
               // if this isn't the final line in the set, add a newline
               if (i < lines.size() - 1)
               {
                  output.append("\n");
               }
               
               job_->addOutput(output, false);
            }
         }
      }
   }

   void onStderr(const std::string& output)
   {
      if (job_)
         job_->addOutput(output, true);
   }

   void onCompleted(int exitStatus)
   {
      Error error;
      r::sexp::Protect protect;
      
      // export results if requested
      if (!spec_.exportEnv().empty() && export_.exists())
      {
         setJobStatus(job_, "Loading results");
         r::exec::RFunction load("load");
         load.addParam("file", string_utils::utf8ToSystem(export_.absolutePath()));
         if (spec_.exportEnv() == "R_GlobalEnv")
         {
            // user requested that results be loaded into the global environment
            load.addParam("envir", R_GlobalEnv);
         }
         else
         {
            // user requested a named local environment
            SEXP localEnv = R_NilValue;
            error = r::exec::evaluateString(
                  "`" + spec_.exportEnv() + "` <- new.env(parent = emptyenv())",
                  &localEnv, &protect);
            load.addParam("envir", localEnv);
         }
         if (!error)
         {
            error = load.call();
         }
         if (error)
         {
            job_->addOutput("Error loading results: " + error.summary() + "\n", true);
         }
         else
         {
            // act as though the user had just invoked load() at the R console; this will cause the
            // environment monitor to pick up the new values, if any
            module_context::events().onDetectChanges(module_context::ChangeSourceREPL);
         }
         setJobStatus(job_, "");
      }

      // mark job state
      if (job_)
         setJobState(job_, exitStatus == 0 && completed_ ? JobSucceeded : JobFailed);

      // clean up temporary files, if we used them
      import_.removeIfExists();
      export_.removeIfExists();
      tempCode_.removeIfExists();

      // run caller-provided completion function
      onComplete_();
   }

   void onProgress(const std::string& cat, const std::string& argument)
   {
      // process argument according to category
      if (cat == "count")
      {
         // record job as started
         int count = safe_convert::stringTo<int>(argument, 0);
         job_->setProgressMax(count);

         // set state to running
         setJobState(job_, JobRunning);
      }
      else if (cat == "statement" && job_)
      {
         int statement = safe_convert::stringTo<int>(argument, 0);
         setJobProgress(job_, statement);
      }
      else if (cat == "completed")
      {
         completed_ = true;

         // clear the progress text
         setJobStatus(job_, "");
      }
      else if (cat == "section" && job_)
      {
         setJobStatus(job_, argument);
      }
   }

   boost::shared_ptr<Job> job_;
   ScriptLaunchSpec spec_;
   bool completed_;
   FilePath import_;
   FilePath export_;
   FilePath tempCode_;
   boost::function<void()> onComplete_;
};


std::vector<boost::shared_ptr<ScriptJob> > s_scripts;

} // anonymous namespace

ScriptLaunchSpec::ScriptLaunchSpec(
      const std::string& name,
      const core::FilePath& path,
      const std::string& encoding,
      const core::FilePath& workingDir,
      bool importEnv,
      const std::string& exportEnv):
   name_(name),
   path_(path),
   encoding_(encoding),
   workingDir_(workingDir),
   importEnv_(importEnv),
   exportEnv_(exportEnv)
{
}

ScriptLaunchSpec::ScriptLaunchSpec(
      const std::string& name,
      const std::string& code,
      const core::FilePath& workingDir,
      bool importEnv,
      const std::string& exportEnv):
   name_(name),
   code_(code),
   workingDir_(workingDir),
   importEnv_(importEnv),
   exportEnv_(exportEnv)
{
}

FilePath ScriptLaunchSpec::path()
{
   return path_;
}

FilePath ScriptLaunchSpec::workingDir()
{
   return workingDir_;
}

std::string ScriptLaunchSpec::exportEnv()
{
   return exportEnv_;
}

std::string ScriptLaunchSpec::code()
{
   return code_;
}

std::string ScriptLaunchSpec::name()
{
   return name_;
}

bool ScriptLaunchSpec::importEnv()
{
   return importEnv_;
}

std::string ScriptLaunchSpec::encoding()
{
   return encoding_;
}

Error startScriptJob(const ScriptLaunchSpec& spec, std::string* pId)
{
   boost::shared_ptr<ScriptJob> job = ScriptJob::create(spec,
         [&]() 
         { 
            // remove the script from the list of those running
            s_scripts.erase(std::remove(s_scripts.begin(), s_scripts.end(), job), s_scripts.end());
         });

   if (pId != nullptr)
   {
      *pId = job->id();
   }

   s_scripts.push_back(job);
   return Success();
}

Error stopScriptJob(const std::string& id)
{
   for (auto script: s_scripts)
   {
      if (script->id() == id)
      {
         // request terminate
         script->terminate();
         return Success();
      }
   }

   // indicate that we didn't find the job
   Error error = systemError(boost::system::errc::no_such_file_or_directory, 
         ERROR_LOCATION);
   error.addProperty("id", id);
   return error;
}

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

