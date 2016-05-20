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

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

std::list<NotebookQueueUnit> s_queue;

enum QueueOperation
{
   QueueAdd    = 0,
   QueueUpdate = 1,
   QueueDelete = 2
};

Error updateExecQueue(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   json::Object unitJson;
   int op = 0;
   std::string before;
   Error error = json::readParams(request.params, &unitJson, &op, &before);
   if (error)
      return error;

   NotebookQueueUnit unit(unitJson);
   std::list<NotebookQueueUnit>::iterator it;

   switch(op)
   {
      case QueueAdd:
         // find insertion position
         for (s_queue.begin(); it != s_queue.end(); it++)
         {
            if (it->docId() == unit.docId() && 
                it->chunkId() == before)
               break;
         }
         s_queue.insert(it, unit);
         break;

      case QueueUpdate:
         break;

      case QueueDelete:
         break;
   }

   return Success();
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
