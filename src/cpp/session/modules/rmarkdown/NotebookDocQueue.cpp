/*
 * NotebookDocQueue.hpp
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

#ifndef SESSION_NOTEBOOK_DOC_QUEUE_HPP
#define SESSION_NOTEBOOK_DOC_QUEUE_HPP

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

class NotebookDocQueue
{
public:
   NotebookDocQueue(const std::string& docId, int execMode, int execScope, 
         int pixelWith, int charWidth);
   Error update(const NotebookQueueUnit& unit, QueueOperation op, 
         const std::string& before);
private:
   // the queue of chunks to be executed 
   std::list<NotebookQueueUnit> queue_;
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
