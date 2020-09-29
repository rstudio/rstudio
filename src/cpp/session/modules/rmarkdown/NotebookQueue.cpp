/*
 * NotebookQueue.cpp
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

#if defined(_WIN32)
// Necessary to avoid compile error on Win x64
#include <winsock2.h>
#endif


#include "SessionRmdNotebook.hpp"
#include "SessionRMarkdown.hpp"
#include "NotebookQueue.hpp"
#include "NotebookQueueUnit.hpp"
#include "NotebookChunkDefs.hpp"
#include "NotebookExec.hpp"
#include "NotebookDocQueue.hpp"
#include "NotebookCache.hpp"
#include "NotebookAlternateEngines.hpp"
#include "NotebookChunkOptions.hpp"

#include <r/RCntxtUtils.hpp>
#include <r/RInterface.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RSexp.hpp>

#include <core/Exec.hpp>
#include <core/Thread.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionClientEvent.hpp>
#include <session/SessionClientEventService.hpp>
#include <session/http/SessionRequest.hpp>

#define kThreadQuitCommand "thread_quit"

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
class NotebookQueue : boost::noncopyable
{
public:
   NotebookQueue() 
   {
      // launch a thread to process console input
      pInput_ = 
         boost::make_shared<core::thread::ThreadsafeQueue<std::string> >();
      thread::safeLaunchThread(boost::bind(
               &NotebookQueue::consoleThreadMain, this), &console_);

      // register handler for chunk exec complete
      handlers_.push_back(events().onChunkExecCompleted.connect(
               boost::bind(&NotebookQueue::onChunkExecCompleted, this, _1, _2, _3)));
   }

   ~NotebookQueue()
   {
      // let thread clean up asynchronously
      pInput_->enque(kThreadQuitCommand);

      // unregister handlers
      for (RSTUDIO_BOOST_CONNECTION& connection : handlers_)
      {
         connection.disconnect();
      }

      // clear queue state and any active execution contexts
      clear();
   }

   bool complete()
   {
      return queue_.empty();
   }

   Error process(ExpressionMode mode)
   {
      // if list is empty, we're done
      if (queue_.empty())
         return Success();

      // defer if R is currently executing code (we'll initiate processing when
      // the console continues)
      if (!module_context::isPythonReplActive() && r::context::globalContext().nextcontext())
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
            if (!execContext_->options().getOverlayOption("error", false))
            {
               clear();
               return Success();
            }
         }
         if (execUnit_->complete())
         {
            bool incomplete = false;

            if (mode == ExprModeContinuation &&
                execUnit_->execScope() == ExecScopeChunk)
            {
               // if we're still in continuation mode but we're at the end of
               // the chunk, generate an error
               incomplete = true;
               sendIncompleteError(execUnit_);
            }
            else if (mode == ExprModeNew && execContext_)
            {
               // otherwise let the context know that the last expression 
               // finished evaluation
               execContext_->onExprComplete();
            }

            // unit has finished executing; remove it from the queue
            popUnit(execUnit_);

            // notify client
            enqueueExecStateChanged(ChunkExecFinished, execContext_ ?
                  execContext_->options().chunkOptions() : json::Object());

            // clean up current exec unit 
            if (execContext_)
            {
               execContext_->disconnect();
               execContext_.reset();
            }
            execUnit_.reset();

            // if the unit was incomplete, we need to wait for the interrupt
            // to complete before we execute more code
            if (incomplete)
               return Success();
         }
         else
         {
            // if unit is executing code, execute more code
            if (!execUnit_->executingCode().empty())
               return executeCurrentUnit(mode);
            else
               return Success();
         }
      }

      return executeNextUnit(mode);
   }

   Error update(boost::shared_ptr<NotebookQueueUnit> pUnit, QueueOperation op, 
      const std::string& before)
   {
      // find the document queue corresponding to this unit
      for (const boost::shared_ptr<NotebookDocQueue>& queue : queue_)
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
      if (execUnit_)
         execUnit_.reset();

      // remove all document queues
      queue_.clear();
   }

   json::Value getDocQueue(const std::string& docId)
   {
      for (boost::shared_ptr<NotebookDocQueue> pQueue : queue_)
      {
         if (pQueue->docId() == docId)
            return pQueue->toJson();
      }
      return json::Value();
   }

   void onConsolePrompt(const std::string& prompt)
   {
      Error error = process(prompt == "+ " ? ExprModeContinuation : 
                                             ExprModeNew);
      if (error)
         LOG_ERROR(error);
   }

private:

   void onChunkExecCompleted(const std::string& docId, 
         const std::string& chunkId, const std::string& nbCtxId)
   {
      if (!execUnit_)
         return;

      // if this is the currently executing chunk but it doesn't have an R
      // execution context, it must be executing with an alternate engine; 
      // this event signals that the alternate engine is finished, so move to
      // the next document in the queue
      if (execUnit_->docId() == docId && 
          execUnit_->chunkId() == chunkId &&
          execUnit_->execScope() != ExecScopeInline &&
          !execContext_)
      {
         // remove from the queue
         popUnit(execUnit_);

         // signal client
         enqueueExecStateChanged(ChunkExecFinished, json::Object());
         
         // execute the next chunk, if any
         execUnit_.reset();
         process(ExprModeNew);
      }

      if (execContext_)
      {
         // get the chunk label to see if this is the setup chunk 
         std::string label;
         json::readObject(execContext_->options().chunkOptions(), "label", label);
         if (label == "setup")
            saveSetupContext();
      }
   }

   // execute the next line or expression in the current execution unit
   Error executeCurrentUnit(ExpressionMode mode)
   {
      // ensure we have a unit to execute 
      if (!execUnit_)
         return Success();

      // if this isn't the continuation of an expression, perform any
      // post-expression operations
      if (mode == ExprModeNew && execContext_)
         execContext_->onExprComplete();
         
      ExecRange range;
      std::string code = execUnit_->popExecRange(&range, mode);
      if (code.empty())
      {
         // no code to evaluate--skip this unit
         skipUnit();
      }
      else 
      {
         // if we're switching the console between languages, call the
         // appropriate R code to make that happen
         std::string prefix;

         bool isPythonActive = module_context::isPythonReplActive();
         if (isPythonActive && execContext_->engine() != "python")
         {
            // switching from Python -> R: deactivate the Python REPL
            prefix = "quit\n";
         }
         else if (!isPythonActive && execContext_->engine() == "python")
         {
            // switching from R -> Python: activate the Python REPL
            prefix = "reticulate::repl_python()\n";
         }

         // send code to console 
         sendConsoleInput(execUnit_->chunkId(), json::Value(prefix + code));

         // let client know the range has been sent to R
         json::Object exec;
         exec["doc_id"]     = execUnit_->docId();
         exec["chunk_id"]   = execUnit_->chunkId();
         exec["exec_range"] = range.toJson();
         exec["expr_mode"]  = mode;
         exec["code"]       = code;
         module_context::enqueClientEvent(
               ClientEvent(client_events::kNotebookRangeExecuted, exec));
      }

      return Success();
   }

   void sendConsoleInput(const std::string& chunkId, const json::Value& input)
   {
      json::Array arr;
      ExecRange range(0, 0);
      arr.push_back(input);
      arr.push_back(chunkId);

      // formulate request body
      json::Object rpc;
      rpc["method"] = json::Value("console_input");
      rpc["params"] = arr;
      rpc["clientId"] = json::Value(clientEventService().clientId());

      // serialize RPC body and send it to helper thread for submission
      pInput_->enque(rpc.write());
   }

   Error executeNextUnit(ExpressionMode mode)
   {
      Error error;

      // no work to do if we have no documents
      if (queue_.empty())
         return Success();

      // get the next execution unit from the current queue
      boost::shared_ptr<NotebookDocQueue> docQueue = *queue_.begin();
      if (docQueue->complete())
         return Success();

      // if this is the first unit in the queue, evaluate document-wide 
      // knit parameters if appropriate
      if (docQueue->maxUnits() == docQueue->remainingUnits())
      {
         error = evaluateRmdParams(docQueue->docId());
         if (error)
            LOG_ERROR(error);
      }

      boost::shared_ptr<NotebookQueueUnit> unit = docQueue->firstUnit();

      // extract the default chunk options, then augment with the unit's 
      // chunk-specific options
      json::Object chunkOptions;
      Error optionsError = unit->parseOptions(&chunkOptions);
      ChunkOptions options(docQueue->defaultChunkOptions(), chunkOptions);

      // establish execution context for the unit

      // in batch mode, make sure unit should be evaluated -- note that
      // eval=FALSE units generally do not get sent up in the first place, so
      // if we're here it's because the unit has eval=<expr>
      if (unit->execMode() == ExecModeBatch &&
         !options.getOverlayOption("eval", true))
      {
         return skipUnit();
      }

      // compute context
      std::string ctx = docQueue->commitMode() == ModeCommitted ?
         kSavedCtx : notebookCtxId();

      // if this is the setup chunk, prepare for its execution by switching
      // knitr chunk defaults
      std::string label;
      json::readObject(chunkOptions, "label", label);
      if (label == "setup")
         prepareSetupContext();

      // is there external code for this chunk? if so, replace the chunk's
      // code with the code from the external file; see:
      // http://yihui.name/knitr/demo/externalization/
      if (unit->execScope() == ExecScopeChunk)
      {
         std::string external = docQueue->externalChunk(label);
         if (!external.empty())
            unit->replaceCode(external);
      }

      // skip unit if it has no code to execute
      if (!unit->hasPendingRanges())
      {
         return skipUnit();
      }

      // compute engine
      std::string engine = options.getOverlayOption("engine", std::string("r"));

      // normalize engine case - if the chunk doesn't have an engine specified
      // it will receive the knitr default of "R"
      if (engine == "R")
         engine = "r";

      if (engine == "r" || engine == "python")
      {
         // establish execution context unless we're an inline chunk
         if (unit->execScope() != ExecScopeInline)
         {
            // look up the working directory of the document as long as we
            // aren't in the setup chunk, which always computes paths relative
            // to the document so that relative paths in the setup chunk, such
            // as those used in root.dir, have a reliable origin
            core::FilePath workingDir;
            if (label != "setup")
               workingDir = docQueue->workingDir();

            std::string codeString = string_utils::wideToUtf8(unit->code());
            execContext_ = boost::make_shared<ChunkExecContext>(
               unit->docId(), unit->chunkId(), codeString, label, ctx, engine,
               unit->execScope(), workingDir, options,
               docQueue->pixelWidth(), docQueue->charWidth());
            execContext_->connect();

            // if there was an error parsing the options for the chunk, display
            // that as an error inside the chunk itself
            if (optionsError)
            {
                execContext_->onConsoleOutput(module_context::ConsoleOutputError,
                                              optionsError.getSummary());
            }
         }
         execUnit_ = unit;
         enqueueExecStateChanged(ChunkExecStarted, options.chunkOptions());
      }
      else
      {
         // execute with alternate engine
         std::string innerCode;
         error = unit->innerCode(&innerCode);
         if (error)
         {
            LOG_ERROR(error);
         }
         else
         {
            execUnit_ = unit;

            enqueueExecStateChanged(ChunkExecStarted, options.chunkOptions());

            // actually execute the chunk with the alternate engine; store the error separately
            // and log if necessary
            Error execError = executeAlternateEngineChunk(
               unit->docId(), unit->chunkId(), label, ctx, docQueue->workingDir(),
               engine, innerCode, options, execUnit_->execScope(),
               docQueue->pixelWidth(), docQueue->charWidth());
            if (execError)
            {
               LOG_ERROR(execError);
            }
         }
      }

      // if there was an error, skip the chunk
      if (error)
         return skipUnit();

      if (engine == "r" || engine == "python")
      {
         error = executeCurrentUnit(ExprModeNew);
         if (error)
            LOG_ERROR(error);
      }

      return Success();
   }

   // main function for thread which receives console input
   void consoleThreadMain()
   {
      // create our own reference to the threadsafe queue (this prevents it 
      // from getting cleaned up when the parent detaches)
      boost::shared_ptr<core::thread::ThreadsafeQueue<std::string> > pInput = 
         pInput_;

      std::string input;
      while (pInput->deque(&input, boost::posix_time::not_a_date_time))
      {
         // if we were asked to quit, stop processing now
         if (input == kThreadQuitCommand)
            return;

         // loop back console input request to session -- this allows us to treat 
         // notebook console input exactly as user console input
         core::http::Response response;
         Error error = session::http::sendSessionRequest(
               "/rpc/console_input", input, &response);

         if (error)
            LOG_ERROR(error);
         else
         {
            // log warning if the response was not successful
            if (response.statusCode() != core::http::status::Ok)
            {
               std::stringstream oss;
               oss << "Received unexpected response when submitting console input: "
                   << response;
               LOG_WARNING_MESSAGE(oss.str());
            }
         }
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

   Error skipUnit()
   {
      if (queue_.empty())
         return Success();

      boost::shared_ptr<NotebookDocQueue> docQueue = *queue_.begin();
      if (docQueue->complete())
         return Success();

      boost::shared_ptr<NotebookQueueUnit> unit = docQueue->firstUnit();
      popUnit(unit);

      execUnit_ = unit;
      enqueueExecStateChanged(ChunkExecCancelled, json::Object());

      execUnit_.reset();

      return executeNextUnit(ExprModeNew);
   }

   void popUnit(boost::shared_ptr<NotebookQueueUnit> pUnit)
   {
      if (queue_.empty())
         return;

      // remove this unit from the queue
      boost::shared_ptr<NotebookDocQueue> docQueue = *queue_.begin();
      docQueue->update(pUnit, QueueDelete, "");

      // advance if queue is complete
      if (docQueue->complete())
         queue_.pop_front();
   }

   void sendIncompleteError(boost::shared_ptr<NotebookQueueUnit> unit)
   {
      // raise an error
      r::exec::error("Incomplete expression: " + unit->executingCode());

      // send an interrupt to the console to abort the unterminated 
      // expression
      sendConsoleInput(execUnit_->chunkId(), json::Value());
   }

   // invoked prior to executing the setup chunk; we use this to set notebook-
   // specific defaults for knitr chunk options (since they are persisted after
   // the setup chunk completes)
   void prepareSetupContext()
   {
      Error error = r::exec::RFunction(".rs.setDefaultChunkOptions").call();
      if (error)
         LOG_ERROR(error);
   }

   // invoked when the current execContext_ represents a completed setup chunk
   // execution; persists knitr options specified in the chunk into storage
   void saveSetupContext()
   {
      std::string docPath;
      source_database::getPath(execContext_->docId(), &docPath);

      r::sexp::Protect protect;
      SEXP resultSEXP = R_NilValue;

      // record the contents of external code chunks
      Error error = r::exec::evaluateString(
            "knitr:::knit_code$get()", &resultSEXP, &protect);
      if (error)
         LOG_ERROR(error);
      else if (r::sexp::isList(resultSEXP))
      {
         json::Value externals;
         r::json::jsonValueFromList(resultSEXP, &externals);
         if (externals.isObject())
         {
            error = setChunkValue(docPath, execContext_->docId(), 
                  kChunkExternals, externals.getObject());
            if (error)
               LOG_ERROR(error);

            if (!queue_.empty())
               queue_.front()->setExternalChunks(externals.getObject());
         }
      }

      // record the root directory
      error = r::exec::evaluateString(
            "knitr::opts_knit$get(\"root.dir\")", &resultSEXP, &protect);
      if (error)
         LOG_ERROR(error);
      if (TYPEOF(resultSEXP) != NILSXP)
      {
         std::string workingDir = r::sexp::safeAsString(resultSEXP, "");

         // write working dir to the cache (just use unresolved string; the
         // string will be resolved to a path in setWorkingDir)
         Error error = setChunkValue(docPath, execContext_->docId(), 
               kChunkWorkingDir, workingDir);
         if (error)
            LOG_ERROR(error);

         // update running queue if present
         if (!queue_.empty() && !workingDir.empty())
            queue_.front()->setWorkingDir(workingDir, SetupChunkDir);
      }
      else if (!error)
      {
         // we succeeded in checking root.dir but it was set to NULL; clear
         // the root directory stored in the cache
         error = setChunkValue(docPath, execContext_->docId(), 
               kChunkWorkingDir, json::Value());
         if (error)
            LOG_ERROR(error);

         // if the running queue got its working directory from the setup chunk, clear it
         if (!queue_.empty() && queue_.front()->getWorkingDirSource() == SetupChunkDir)
            queue_.front()->setWorkingDir("", SetupChunkDir);
      }

      error = r::exec::RFunction(".rs.defaultChunkOptions")
                                      .call(&resultSEXP, &protect);
      if (error)
         LOG_ERROR(error);
      else
      {
         json::Value defaults;
         r::json::jsonValueFromList(resultSEXP, &defaults);
         if (defaults.isObject())
         {
            // write default chunk options to cache
            Error error = setChunkValue(docPath, execContext_->docId(), 
                  kChunkDefaultOptions, defaults.getObject());
            if (error)
               LOG_ERROR(error);

            // update running queue if present
            if (!queue_.empty())
               queue_.front()->setDefaultChunkOptions(defaults.getObject());
         }
      }
   }

   // the documents with active queues
   std::list<boost::shared_ptr<NotebookDocQueue> > queue_;

   // the execution context for the currently executing chunk
   boost::shared_ptr<NotebookQueueUnit> execUnit_;
   boost::shared_ptr<ChunkExecContext> execContext_;

   // registered signal handlers
   std::vector<RSTUDIO_BOOST_CONNECTION> handlers_;

   // the thread which submits console input, and the queue which feeds it
   boost::thread console_;
   boost::shared_ptr<core::thread::ThreadsafeQueue<std::string> > pInput_;
};

// NOTE: we previously used a shared pointer here but this caused
// issues with deletion of the static object during program shutdown;
// since we already manage the lifetime of the queue appropriately
// we use a raw pointer and let it leak if the user attempts to quit R
// while the notebook queue is running
static NotebookQueue* s_queue = nullptr;

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

   // deserialize queue from json
   boost::shared_ptr<NotebookDocQueue> pQueue;
   error = NotebookDocQueue::fromJson(docObj, &pQueue);
   if (error)
      return error;

   // create queue if it doesn't exist
   if (!s_queue)
      s_queue = new NotebookQueue;

   // add the queue and process after the RPC returns 
   s_queue->add(pQueue);
   pResponse->setAfterResponse(
         boost::bind(&NotebookQueue::process, s_queue, ExprModeNew));

   return Success();
}

void onConsolePrompt(const std::string& prompt)
{
   // Ignore debug prompts
   if (r::context::inBrowseContext())
      return;

   if (s_queue)
   {
      s_queue->onConsolePrompt(prompt);
   }

   // clean up queue if it's finished executing
   if (s_queue && s_queue->complete())
   {
      delete s_queue;
      s_queue = nullptr;
   }
}

void onUserInterrupt()
{
   if (s_queue)
   {
      s_queue->clear();
      delete s_queue;
      s_queue = nullptr;
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
