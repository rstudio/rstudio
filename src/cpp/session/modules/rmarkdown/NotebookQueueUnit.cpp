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

#include <boost/foreach.hpp>

#include <core/json/JsonRpc.hpp>

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

Error fillExecRangeArray(const json::Array& in, std::vector<ExecRange>* pOut)
{
   // process arrays
   BOOST_FOREACH(const json::Value val, in) 
   {
      // ignore non-value types
      if (val.type() != json::ObjectType)
         continue;

      ExecRange range;
      Error error = json::readObject(val.get_obj(),
            kQueueUnitRangeStart, &range.start,
            kQueueUnitRangeStop,  &range.stop);
      if (error)
         return error;

      pOut->push_back(range);
   }
   return Success();
}

void fillJsonRangeArray(const std::vector<ExecRange>& in, json::Array* pOut)
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
      NotebookQueueUnit* pUnit)
{
   // extract top-level values
   json::Array completed, pending;
   Error error = json::readObject(source, 
         kQueueUnitCode,      &pUnit->code_,
         kQueueUnitDocId,     &pUnit->docId_,
         kQueueUnitChunkId,   &pUnit->chunkId_,
         kQueueUnitCompleted, &completed,
         kQueueUnitPending,   &pending);
   if (error)
      LOG_ERROR(error);

   // process arrays 
   error = fillExecRangeArray(completed, &pUnit->completed_);
   if (error)
      LOG_ERROR(error);
   error = fillExecRangeArray(pending, &pUnit->pending_);
   if (error)
      LOG_ERROR(error);
   return Success();
}

json::Object NotebookQueueUnit::toJson()
{
   // process arrays
   json::Array completed, pending;
   fillJsonRangeArray(completed_, &completed);
   fillJsonRangeArray(pending_, &pending);

   // emit top-level values
   json::Object unit;
   unit[kQueueUnitCode]      = code_;
   unit[kQueueUnitDocId]     = docId_;
   unit[kQueueUnitChunkId]   = chunkId_;
   unit[kQueueUnitCompleted] = completed;
   unit[kQueueUnitPending]   = pending;

   return json::Object();
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

