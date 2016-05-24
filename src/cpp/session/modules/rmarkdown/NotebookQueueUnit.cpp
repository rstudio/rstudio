/*
 * NotebookQueueUnit.cpp
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

#include "NotebookQueueUnit.hpp"

#include <session/SessionModuleContext.hpp>
#include "../../SessionClientEventQueue.hpp"

#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>

#define kQueueUnitCode       "code"
#define kQueueUnitDocId      "doc_id"
#define kQueueUnitChunkId    "chunk_id"
#define kQueueUnitCompleted  "completed"
#define kQueueUnitPending    "pending"
#define kQueueUnitRangeStart "start"
#define kQueueUnitRangeStop  "stop"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

Error fillExecRange(const json::Array& in, std::list<ExecRange>* pOut)
{
   // process arrays
   BOOST_FOREACH(const json::Value val, in) 
   {
      // ignore non-value types
      if (val.type() != json::ObjectType)
         continue;

      ExecRange range(0, 0);
      Error error = json::readObject(val.get_obj(),
            kQueueUnitRangeStart, &range.start,
            kQueueUnitRangeStop,  &range.stop);
      if (error)
         return error;

      pOut->push_back(range);
   }
   return Success();
}

void fillJsonRange(const std::list<ExecRange>& in, json::Array* pOut)
{
   BOOST_FOREACH(const ExecRange range, in)
   {
      json::Object jsonRange;
      jsonRange[kQueueUnitRangeStart] = range.start;
      jsonRange[kQueueUnitRangeStop] = range.stop;
      pOut->push_back(jsonRange);
   }
}

} // anonymous namespace

Error NotebookQueueUnit::fromJson(const json::Object& source, 
      boost::shared_ptr<NotebookQueueUnit>* pUnit)
{
   // extract contained unit for manipulation
   NotebookQueueUnit& unit = *pUnit->get();

   // extract top-level values
   json::Array completed, pending;
   Error error = json::readObject(source, 
         kQueueUnitCode,      &unit.code_,
         kQueueUnitDocId,     &unit.docId_,
         kQueueUnitChunkId,   &unit.chunkId_,
         kQueueUnitCompleted, &completed,
         kQueueUnitPending,   &pending);
   if (error)
      LOG_ERROR(error);

   // process arrays 
   error = fillExecRange(completed, &unit.completed_);
   if (error)
      LOG_ERROR(error);
   error = fillExecRange(pending, &unit.pending_);
   if (error)
      LOG_ERROR(error);
   return Success();
}

Error NotebookQueueUnit::parseOptions(json::Object* pOptions)
{
   // evaluate this chunk's options in R
   r::sexp::Protect protect;
   SEXP sexpOptions = R_NilValue;
   Error error = r::exec::RFunction(".rs.evaluateChunkOptions", code_)
                                   .call(&sexpOptions, &protect);
   if (error)
      return error;

   // convert to JSON 
   json::Value jsonOptions;
   error = r::json::jsonValueFromList(sexpOptions, &jsonOptions);
   if (jsonOptions.type() != json::ObjectType)
      return Error(json::errc::ParseError, ERROR_LOCATION);
   
   *pOptions = jsonOptions.get_obj();
   return Success();
}

json::Object NotebookQueueUnit::toJson() const
{
   // process arrays
   json::Array completed, pending;
   fillJsonRange(completed_, &completed);
   fillJsonRange(pending_, &pending);

   // emit top-level values
   json::Object unit;
   unit[kQueueUnitCode]      = code_;
   unit[kQueueUnitDocId]     = docId_;
   unit[kQueueUnitChunkId]   = chunkId_;
   unit[kQueueUnitCompleted] = completed;
   unit[kQueueUnitPending]   = pending;

   return json::Object();
}

Error NotebookQueueUnit::execute()
{
   // extract next range to execute
   ExecRange& range = *pending_.begin();
   int start = range.start;
   int stop = range.stop;

   // use the first line of the range if it's multi-line
   size_t idx = code_.find('\n', start);
   if (idx != std::string::npos && static_cast<int>(idx) < stop)
   {
      stop = idx;

      // adjust the range to account for the code we're about to send
      range.start = idx;
   }
   else
   {
      // not multi line, remove range entirely
      pending_.pop_front();
   }

   // mark completed and extract from code
   completed_.push_back(ExecRange(start, stop));
   std::string code = code_.substr(start, stop);

   // send to R 
      
   return Success();
}

std::string NotebookQueueUnit::docId() const
{
   return docId_;
}

std::string NotebookQueueUnit::chunkId() const
{
   return chunkId_;
}

std::string NotebookQueueUnit::code() const
{
   return code_;
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

