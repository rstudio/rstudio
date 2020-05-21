/*
 * NotebookDocQueue.hpp
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

#ifndef SESSION_NOTEBOOK_DOC_QUEUE_HPP
#define SESSION_NOTEBOOK_DOC_QUEUE_HPP

#include "NotebookQueue.hpp"
#include "NotebookQueueUnit.hpp"
#include "SessionRmdNotebook.hpp"

#include <shared_core/json/Json.hpp>
#include <shared_core/FilePath.hpp>
#include <list>

namespace rstudio {

namespace core {
   class Error;
   class FilePath;
}

namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

// possible sources for specifying the working directory in a notebook execution queue
enum WorkingDirSource
{
   DefaultDir    = 0,    // working directory was unspecified (default)
   GlobalDir     = 1,    // working dir was specified by the IDE (globally)
   SetupChunkDir = 2     // working dir was specified by the doc's setup chunk
};

class NotebookDocQueue : boost::noncopyable
{
public:
   NotebookDocQueue(const std::string& docId, const std::string& jobDesc,
         const std::string& workingDir, CommitMode commitMode, int pixelWith, 
         int charWidth, int maxUnits);

   static core::Error fromJson(const core::json::Object& source,
      boost::shared_ptr<NotebookDocQueue>* pQueue);
   core::json::Object toJson() const;

   core::Error update(const boost::shared_ptr<NotebookQueueUnit> unit,
      QueueOperation op, const std::string& before);
   boost::shared_ptr<NotebookQueueUnit> firstUnit();

   core::json::Object defaultChunkOptions() const;
   void setDefaultChunkOptions(const core::json::Object& options);
   void setWorkingDir(const std::string& workingDir, WorkingDirSource source);
   WorkingDirSource getWorkingDirSource();
   void setExternalChunks(const core::json::Object& chunks);

   // accessors
   std::string docId() const;
   int pixelWidth() const;
   int charWidth() const;
   bool complete() const;
   CommitMode commitMode() const;
   int maxUnits() const;
   int remainingUnits() const;
   core::FilePath workingDir() const;
   std::string externalChunk(const std::string& label) const;

private:
   std::string docId_;
   std::string jobDesc_;
   CommitMode commitMode_;
   int pixelWidth_;
   int charWidth_;
   int maxUnits_;

   // the document path and its default knit chunk options
   std::string docPath_;
   core::json::Object defaultOptions_;

   // external code chunks
   core::json::Object externalChunks_;

   // the working directory in which to execute chunks (note that this will be
   // empty unless manually specified)
   core::FilePath workingDir_;
   WorkingDirSource workingDirSource_;

   // the queue of chunks to be executed 
   std::list<boost::shared_ptr<NotebookQueueUnit> > queue_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
