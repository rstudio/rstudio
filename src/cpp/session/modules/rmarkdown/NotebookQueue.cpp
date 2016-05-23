/*
 * NotebookQueue.cpp
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

#include "NotebookQueue.hpp"
#include "NotebookQueueUnit.hpp"
#include "NotebookExec.hpp"

#include <r/RInterface.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

// represents the global queue of work 
class NotebookQueue
{
public:
   Error process()
   {
      // no work if list is empty
      if (queue_.empty())
         return Success();

      // defer if R is currently executing code
      if (r::getGlobalContext()->nextcontext != NULL)
         return Success();

      // pop front off execution queue
      const NotebookQueueUnit& unit = *queue_.begin();
      queue_.pop_front();

      return Success();
   }

   Error update(const NotebookQueueUnit& unit, QueueOperation op, 
         const std::string& before)
   {
      std::list<NotebookQueueUnit>::iterator it;

      switch(op)
      {
         case QueueAdd:
            // find insertion position
            for (it = queue_.begin(); it != queue_.end(); it++)
            {
               if (it->docId()   == unit.docId() && 
                   it->chunkId() == before)
                  break;
            }
            queue_.insert(it, unit);
            break;

         case QueueUpdate:
            break;

         case QueueDelete:
            break;
      }
      return Success();
   }

private:
   // the queue of chunks to be executed 
   std::list<NotebookQueueUnit> queue_;

   // the execution context for the currently executing chunk
   boost::shared_ptr<ChunkExecContext> execContext_;
};

static boost::shared_ptr<NotebookQueue> s_queue;

Error updateExecQueue(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   json::Object unitJson;
   int op = 0;
   std::string before;
   Error error = json::readParams(request.params, &unitJson, &op, &before);
   if (error)
      return error;

   NotebookQueueUnit unit;
   error = NotebookQueueUnit::fromJson(unitJson, &unit);
   if (error)
      return error;

   return s_queue->update(unit, static_cast<QueueOperation>(op), before);
}

} // anonymous namespace

Error initQueue()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "update_notebook_exec_queue", updateExecQueue));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
