/*
 * NotebookDocQueue.cpp
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

#include "NotebookDocQueue.hpp"

#include <gsl/gsl>

#include "NotebookChunkDefs.hpp"

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>

using namespace rstudio::core;

#define kDocQueueId             "doc_id"
#define kDocQueueJobDesc        "job_desc"
#define kDocQueuePixelWidth     "pixel_width"
#define kDocQueueCharWidth      "char_width"
#define kDocQueueUnits          "units"
#define kDocQueueMaxUnits       "max_units"
#define kDocQueueCommitMode     "commit_mode"
#define kDocQueueCompletedUnits "completed_units"
#define kDocQueueWorkingDir     "working_dir"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

NotebookDocQueue::NotebookDocQueue(const std::string& docId, 
      const std::string& jobDesc, const std::string& workingDir, 
      CommitMode commitMode, int pixelWidth, int charWidth, int maxUnits) :
      docId_(docId),
      jobDesc_(jobDesc),
      commitMode_(commitMode),
      pixelWidth_(pixelWidth),
      charWidth_(charWidth),
      maxUnits_(maxUnits),
      workingDirSource_(DefaultDir)
{
   source_database::getPath(docId_, &docPath_);

   // read the default knit options for this document (this is expected to fail
   // if these options don't exist)
   json::Object vals;
   Error error = getChunkValues(docPath_, docId_, &vals);
   if (error)
      return;

   json::readObject(vals, kChunkDefaultOptions, defaultOptions_);
   
   // read the default working dir; if it specifies a valid directory, use it
   // as the working directory for executing chunks in this document
   std::string docWorkingDir;
   json::readObject(vals, kChunkWorkingDir, docWorkingDir);
   if (!docWorkingDir.empty())
   {
      // working directory set in setup chunk (i.e. knitr root.dir) takes
      // precedence
      setWorkingDir(docWorkingDir, SetupChunkDir);
   }
   else if (!workingDir.empty())
   {
      // working directory given by IDE (current dir or project dir)
      setWorkingDir(workingDir, GlobalDir);
   }

   // read external code chunk contents
   json::readObject(vals, kChunkExternals, externalChunks_);
}

boost::shared_ptr<NotebookQueueUnit> NotebookDocQueue::firstUnit()
{
   if (queue_.empty())
      return boost::shared_ptr<NotebookQueueUnit>();
   return *queue_.begin();
}

json::Object NotebookDocQueue::toJson() const
{
   // serialize all the queue units 
   json::Array units;
   for (const boost::shared_ptr<NotebookQueueUnit>& unit : queue_)
   {
      units.push_back(unit->toJson());
   }

   // form JSON object for client
   json::Object queue;
   queue[kDocQueueId]             = docId_;
   queue[kDocQueueJobDesc]        = jobDesc_;
   queue[kDocQueueWorkingDir]     = workingDir_.getAbsolutePath();
   queue[kDocQueueCommitMode]     = commitMode_;
   queue[kDocQueuePixelWidth]     = pixelWidth_;
   queue[kDocQueueCharWidth]      = charWidth_;
   queue[kDocQueueUnits]          = units;
   queue[kDocQueueMaxUnits]       = maxUnits_;
   queue[kDocQueueCompletedUnits] = json::Array();

   return queue;
}

core::Error NotebookDocQueue::fromJson(const core::json::Object& source, 
   boost::shared_ptr<NotebookDocQueue>* pQueue)
{
   // extract contained unit for manipulation
   json::Array units;
   int commitMode = 0, pixelWidth = 0, charWidth = 0, maxUnits = 0;
   std::string docId, jobDesc, workingDir;
   Error error = json::readObject(source, 
         kDocQueueId,         docId,
         kDocQueueJobDesc,    jobDesc,
         kDocQueueWorkingDir, workingDir,
         kDocQueueCommitMode, commitMode,
         kDocQueuePixelWidth, pixelWidth,
         kDocQueueCharWidth,  charWidth,
         kDocQueueUnits,      units,
         kDocQueueMaxUnits,   maxUnits);
   if (error)
      return error;

   *pQueue = boost::make_shared<NotebookDocQueue>(docId, jobDesc, workingDir,
         static_cast<CommitMode>(commitMode), pixelWidth, charWidth,
         maxUnits);

   // populate the queue units
   for (const json::Value val : units)
   {
      // ignore non-objects
      if (!val.isObject())
         continue;

      boost::shared_ptr<NotebookQueueUnit> pUnit = 
         boost::make_shared<NotebookQueueUnit>();
      Error error = NotebookQueueUnit::fromJson(val.getObject(), &pUnit);
      if (error)
         LOG_ERROR(error);
      else
         (*pQueue)->queue_.push_back(pUnit);
   }

   return Success();
}

bool chunkIdEquals(const boost::shared_ptr<NotebookQueueUnit> unit,
                   const std::string &chunkId)
{
   return unit->chunkId() == chunkId;
}

Error NotebookDocQueue::update(const boost::shared_ptr<NotebookQueueUnit> unit, 
      QueueOperation op, const std::string& before)
{
   std::list<boost::shared_ptr<NotebookQueueUnit> >::iterator it;

   switch(op)
   {
      case QueueAdd:
         // find insertion position
         it = std::find_if(queue_.begin(), queue_.end(), 
               boost::bind(chunkIdEquals, _1, before));
         queue_.insert(it, unit);
         maxUnits_ = std::max(maxUnits_, gsl::narrow_cast<int>(queue_.size()));
         break;

      case QueueUpdate:
         it = std::find_if(queue_.begin(), queue_.end(), 
               boost::bind(chunkIdEquals, _1, unit->chunkId()));
         if (it == queue_.end())
         {
            // no matching chunk ID in queue
            break;
         }
         (*it)->updateFrom(*unit);

         break;

      case QueueDelete:
         queue_.remove_if(boost::bind(chunkIdEquals, _1, unit->chunkId()));
         break;
   }
   return Success();
}

std::string NotebookDocQueue::docId() const
{
   return docId_;
}

int NotebookDocQueue::pixelWidth() const
{
   return pixelWidth_;
}

int NotebookDocQueue::charWidth() const
{
   return charWidth_;
}

bool NotebookDocQueue::complete() const
{
   return queue_.empty();
}

CommitMode NotebookDocQueue::commitMode() const
{
   return commitMode_;
}

json::Object NotebookDocQueue::defaultChunkOptions() const
{
   return defaultOptions_;
}

int NotebookDocQueue::remainingUnits() const 
{
   return gsl::narrow_cast<int>(queue_.size());
}

int NotebookDocQueue::maxUnits() const
{
   return maxUnits_;
}

core::FilePath NotebookDocQueue::workingDir() const 
{
   return workingDir_;
}

void NotebookDocQueue::setDefaultChunkOptions(const json::Object& options)
{
   defaultOptions_ = options;
}

void NotebookDocQueue::setWorkingDir(const std::string& workingDir, WorkingDirSource source)
{
   core::FilePath dir;
   if (workingDir.empty())
   {
      // no directory specified, use an empty path
      dir = FilePath();
   }
   else if (workingDir.at(0) == '~')
   {
      // resolve home directory if necessary
      dir = module_context::resolveAliasedPath(workingDir);
   }
   else if (FilePath::isRootPath(workingDir))
   {
      // use absolute paths as-is
      dir = FilePath(workingDir);
   }
   else
   {
      // this is a relative path; resolve against the document path if we
      // have one, and the current directory if we don't
      core::FilePath docParentPath = docPath_.empty() ? 
         FilePath::safeCurrentPath(module_context::userHomePath()) :
         module_context::resolveAliasedPath(docPath_).getParent();
      dir = docParentPath.completeChildPath(workingDir);
   }

   // remove any trailing / or .
   if (!dir.isEmpty() && (dir.getStem().empty() || dir.getStem() == "."))
      dir = dir.getParent();

   // if this is a real directory, use it; otherwise, use an empty path, which
   // causes use to use the document's path as the working directory
   if (dir.exists())
      workingDir_ = dir;
   else
      workingDir_ = FilePath();

   // record source of the working directory
   workingDirSource_ = source;
}

WorkingDirSource NotebookDocQueue::getWorkingDirSource()
{
   return workingDirSource_;
}

void NotebookDocQueue::setExternalChunks(const json::Object& chunks)
{
   externalChunks_ = chunks;
}

std::string NotebookDocQueue::externalChunk(const std::string& label) const
{
   json::Object::Iterator it = externalChunks_.find(label);
   std::string code;
   if (it == externalChunks_.end())
   {
      // no chunk with this label 
      return code;
   }
   else if (!(*it).getValue().isArray())
   {
      // the JSON object representing the external chunks should contain an
      // array of strings representing the lines of code in the chunk
      return code;
   }
   else
   {
      // extract each line of code
      const json::Array& lines = (*it).getValue().getArray();
      for (size_t i = 0; i < lines.getSize(); i++)
      {
         if (lines.getValueAt(i).isString())
            code.append(lines.getValueAt(i).getString());
         if (i < lines.getSize() - 1)
            code.append("\n");
      }
   }
   return code;
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

