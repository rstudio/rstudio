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

#include "SessionRmdNotebook.hpp"
#include "NotebookQueue.hpp"
#include "NotebookQueueUnit.hpp"
#include "NotebookExec.hpp"
#include "NotebookDocQueue.hpp"
#include "NotebookCache.hpp"

#include "../../SessionClientEventService.hpp"

#include <boost/foreach.hpp>

#include <r/RInterface.hpp>

#include <core/Exec.hpp>
#include <core/Thread.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionClientEvent.hpp>
#include <session/http/SessionRequest.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

enum ChunkExecState
{
   ChunkExecStarted   = 0,
   ChunkExecFinished  = 1,
   ChunkExecCancelled = 2
};

// represents the global queue of work 
class NotebookQueue
{
public:
   NotebookQueue() 
   {
      // launch a thread to process console input
      thread::safeLaunchThread(boost::bind(
               &NotebookQueue::consoleThreadMain, this), &console_);
   }

   ~NotebookQueue()
   {
      // clean up thread
      console_.detach();

      // unregister handlers
      BOOST_FOREACH(boost::signals::connection connection, handlers_)
      {
         connection.disconnect();
      }
   }

   bool complete()
   {
      return queue_.empty();
   }

   Error process()
   {
      // if list is empty, we're done
      if (queue_.empty())
         return Success();

      // defer if R is currently executing code (we'll initiate processing when
      // the console continues)
      if (r::getGlobalContext()->nextcontext != NULL)
         return Success();

      // if we have a currently executing unit, execute it; otherwise, pop the
      // next unit off the stack
      if (execUnit_)
      {
         if (execContext_ && execContext_->hasErrors())
         {
            // when an error occurs, see what the chunk options say; if they
            // have error = TRUE we can keep going, but in all other
            // circumstances we should stop right away
            const json::Object& options = execContext_->options();
            bool error = false;
            json::readObject(options, "error", &error);
            if (!error)
            {
               clear();
               return Success();
            }
         }
         if (execUnit_->complete())
         {
            // unit has finished executing; remove it from the queue
            if (queue_.size() > 0)
            {
               boost::shared_ptr<NotebookDocQueue> docQueue = *queue_.begin();
               docQueue->update(execUnit_, QueueDelete, "");
               if (docQueue->complete())
               {
                  queue_.pop_front();
               }
            }

            // notify client
            enqueueExecStateChanged(ChunkExecFinished, execContext_->options());

            // clean up current exec unit 
            execContext_->disconnect();
            execUnit_ = boost::shared_ptr<NotebookQueueUnit>();
         }
         else
            return executeCurrentUnit();
      }

      return executeNextUnit();
   }

   Error update(boost::shared_ptr<NotebookQueueUnit> pUnit, QueueOperation op, 
      const std::string& before)
   {
      // find the document queue corresponding to this unit
      BOOST_FOREACH(const boost::shared_ptr<NotebookDocQueue> queue, queue_)
      {
         if (queue->docId() == pUnit->docId())
         {
            queue->update(pUnit, op, before);
            break;
         }
      }

      return Success();
   }

   void add(boost::shared_ptr<NotebookDocQueue> pQueue)
   {
      queue_.push_back(pQueue);
   }

   void clear()
   {
      // clean up any active execution context
      if (execContext_)
         execContext_->disconnect();

      // remove all document queues
      queue_.clear();
   }

   json::Value getDocQueue(const std::string& docId)
   {
      BOOST_FOREACH(boost::shared_ptr<NotebookDocQueue> pQueue, queue_)
      {
         if (pQueue->docId() == docId)
            return pQueue->toJson();
      }
      return json::Value();
   }

   void onConsolePrompt(const std::string& prompt)
   {
      process();
   }

private:

   // execute the next line or expression in the current execution unit
   Error executeCurrentUnit()
   {
      // ensure we have a unit to execute 
      if (!execUnit_)
         return Success();

      json::Array arr;
      ExecRange range(0, 0);
      arr.push_back(execUnit_->popExecRange(&range));
      arr.push_back(execUnit_->chunkId());

      // formulate request body
      json::Object rpc;
      rpc["method"] = "console_input";
      rpc["params"] = arr;
      rpc["clientId"] = clientEventService().clientId();

      // serialize RPC body and send it to helper thread for submission
      std::ostringstream oss;
      json::write(rpc, oss);
      input_.enque(oss.str());

      // let client know the range has been sent to R
      json::Object exec;
      exec["doc_id"]     = execUnit_->docId();
      exec["chunk_id"]   = execUnit_->chunkId();
      exec["exec_range"] = range.toJson();
      module_context::enqueClientEvent(
            ClientEvent(client_events::kNotebookRangeExecuted, exec));

      return Success();
   }

   Error executeNextUnit()
   {
      // no work to do if we have no documents
      if (queue_.empty())
         return Success();

      // get the next execution unit from the current queue
      boost::shared_ptr<NotebookDocQueue> docQueue = *queue_.begin();
      if (docQueue->complete())
         return Success();

      boost::shared_ptr<NotebookQueueUnit> unit = docQueue->firstUnit();

      // establish execution context for the unit
      json::Object options;
      Error error = unit->parseOptions(&options);
      if (error)
         return error;

      // in batch mode, make sure unit should be evaluated -- note that
      // eval=FALSE units generally do not get sent up in the first place, so
      // if we're here it's because the unit has eval=<expr>
      if (unit->execMode() == ExecModeBatch)
      {
         bool eval = true;
         json::readObject(options, "eval", &eval);
         if (!eval)
         {
            execUnit_ = unit;
            enqueueExecStateChanged(ChunkExecCancelled, options);
            docQueue->update(execUnit_, QueueDelete, "");
            return executeNextUnit();
         }
      }

      std::string ctx = docQueue->commitMode() == ModeCommitted ?
         kSavedCtx : notebookCtxId();
      execContext_ = boost::make_shared<ChunkExecContext>(
         unit->docId(), unit->chunkId(), ctx, unit->execScope(), options,
         docQueue->pixelWidth(), docQueue->charWidth());
      execContext_->connect();

      // notify client
      execUnit_ = unit;
      enqueueExecStateChanged(ChunkExecStarted, options);

      executeCurrentUnit();

      return Success();
   }

   // main function for thread which receives console input
   void consoleThreadMain()
   {
      std::string input;
      while (input_.deque(&input, boost::posix_time::not_a_date_time))
      {
         // loop back console input request to session -- this allows us to treat 
         // notebook console input exactly as user console input
         core::http::Response response;
         Error error = session::http::sendSessionRequest(
               "/rpc/console_input", input, &response);
         if (error)
            LOG_ERROR(error);
      }
   }

   void enqueueExecStateChanged(ChunkExecState state, 
         const json::Object& options)
   {
      json::Object event;
      event["doc_id"]     = execUnit_->docId();
      event["chunk_id"]   = execUnit_->chunkId();
      event["exec_state"] = state;
      event["options"]    = options;
      module_context::enqueClientEvent(ClientEvent(
               client_events::kChunkExecStateChanged, event));
   }

   // the documents with active queues
   std::list<boost::shared_ptr<NotebookDocQueue> > queue_;

   // the execution context for the currently executing chunk
   boost::shared_ptr<NotebookQueueUnit> execUnit_;
   boost::shared_ptr<ChunkExecContext> execContext_;

   // registered signal handlers
   std::vector<boost::signals::connection> handlers_;

   // the thread which submits console input, and the queue which feeds it
   boost::thread console_;
   core::thread::ThreadsafeQueue<std::string> input_;
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

   boost::shared_ptr<NotebookQueueUnit> pUnit = 
      boost::make_shared<NotebookQueueUnit>();
   error = NotebookQueueUnit::fromJson(unitJson, &pUnit);
   if (error)
      return error;
   if (!s_queue)
      return Success();

   return s_queue->update(pUnit, static_cast<QueueOperation>(op), before);
}

Error executeNotebookChunks(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   json::Object docObj;
   Error error = json::readParams(request.params, &docObj);

   boost::shared_ptr<NotebookDocQueue> pQueue = 
      boost::make_shared<NotebookDocQueue>();
   error = NotebookDocQueue::fromJson(docObj, &pQueue);
   if (error)
      return error;

   // create queue if it doesn't exist
   if (!s_queue)
      s_queue = boost::make_shared<NotebookQueue>();

   // add the queue and process immediately
   s_queue->add(pQueue);
   s_queue->process();

   return Success();
}

void onConsolePrompt(const std::string& prompt)
{
   if (s_queue)
   {
      s_queue->onConsolePrompt(prompt);
   }

   // clean up queue if it's finished executing
   if (s_queue && s_queue->complete())
   {
      s_queue.reset();
   }
}

void onUserInterrupt()
{
   if (s_queue)
   {
      s_queue->clear();
      s_queue.reset();
   }
}

} // anonymous namespace

json::Value getDocQueue(const std::string& docId)
{
   if (!s_queue)
      return json::Value();

   return s_queue->getDocQueue(docId);
}

Error initQueue()
{
   using boost::bind;
   using namespace module_context;

   module_context::events().onConsolePrompt.connect(onConsolePrompt);
   module_context::events().onUserInterrupt.connect(onUserInterrupt);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "update_notebook_exec_queue", updateExecQueue))
      (bind(registerRpcMethod, "execute_notebook_chunks", executeNotebookChunks));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
