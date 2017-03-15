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

#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RSession.hpp>

#define kQueueUnitCode       "code"
#define kQueueUnitDocId      "doc_id"
#define kQueueUnitChunkId    "chunk_id"
#define kQueueUnitCompleted  "completed"
#define kQueueUnitPending    "pending"
#define kQueueUnitExecuting  "executing"
#define kQueueUnitExecMode   "exec_mode"
#define kQueueUnitExecScope  "exec_scope"
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
      Error error = ExecRange::fromJson(val.get_obj(), &range);
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
      pOut->push_back(range.toJson());
   }
}

} // anonymous namespace

Error ExecRange::fromJson(const json::Object& source,
                          ExecRange* pRange)
{
   return json::readObject(source,
         kQueueUnitRangeStart, &pRange->start,
         kQueueUnitRangeStop,  &pRange->stop);
}

json::Object ExecRange::toJson() const
{
   json::Object jsonRange;
   jsonRange[kQueueUnitRangeStart] = start;
   jsonRange[kQueueUnitRangeStop] = stop;
   return jsonRange;
}

void ExecRange::extendTo(const ExecRange& other)
{
   start = std::min(start, other.start);
   stop  = std::max(stop, other.stop);
}

bool ExecRange::empty()
{
   return start == 0 && stop == 0;
}

Error NotebookQueueUnit::fromJson(const json::Object& source, 
      boost::shared_ptr<NotebookQueueUnit>* pUnit)
{
   // extract contained unit for manipulation
   NotebookQueueUnit& unit = *pUnit->get();

   // extract top-level values
   json::Array completed, pending;
   int execMode, execScope;
   std::string code;
   Error error = json::readObject(source, 
         kQueueUnitCode,      &code,
         kQueueUnitDocId,     &unit.docId_,
         kQueueUnitChunkId,   &unit.chunkId_,
         kQueueUnitCompleted, &completed,
         kQueueUnitPending,   &pending,
         kQueueUnitExecMode,  &execMode,
         kQueueUnitExecScope, &execScope);
   if (error)
      LOG_ERROR(error);

   // convert enums
   unit.execMode_ = static_cast<ExecMode>(execMode);
   unit.execScope_ = static_cast<ExecScope>(execScope);

   // convert code to wide chars (so we don't have to do UTF-8 math when
   // processing execution ranges)
   unit.code_ = string_utils::utf8ToWide(code);

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
   // inline chunks have no options
   if (execScope_ == ExecScopeInline)
   {
      *pOptions = json::Object();
      return Success();
   }

   // evaluate this chunk's options in R
   r::sexp::Protect protect;
   SEXP sexpOptions = R_NilValue;
   Error error = r::exec::RFunction(".rs.evaluateChunkOptions", 
               string_utils::wideToUtf8(code_)).call(&sexpOptions, &protect);
   if (error)
      return error;

   // convert to JSON 
   json::Value jsonOptions;
   error = r::json::jsonValueFromList(sexpOptions, &jsonOptions);
   if (jsonOptions.type() == json::ArrayType && 
       jsonOptions.get_array().empty())
   {
      // treat empty array as empty object
      *pOptions = json::Object();
   }
   else if (jsonOptions.type() != json::ObjectType)
   {
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }
   else 
   {
      *pOptions = jsonOptions.get_obj();
   }

   return Success();
}

Error NotebookQueueUnit::innerCode(std::string* pCode)
{
   return r::exec::RFunction(".rs.extractChunkInnerCode", 
         string_utils::wideToUtf8(code_)).call(pCode);
}

bool NotebookQueueUnit::hasPendingRanges()
{
   return !pending_.empty();
}

void NotebookQueueUnit::updateFrom(const NotebookQueueUnit& other)
{
   // replace code
   code_ = other.code();

   // we don't support removing or changing executable ranges, so process only
   // additions
   std::list<ExecRange>::iterator i = pending_.begin(); 
   for (std::list<ExecRange>::const_iterator o = other.pending_.begin();
        o != other.pending_.end();
        o ++)
   {
      if (i == pending_.end())
      {
         // past end of our own range, so begin inserting elements from the 
         // other unit's range
         pending_.insert(i, *o);
      }
      else
      {
         i++;
      }
   }
}

json::Object NotebookQueueUnit::toJson() const
{
   // process arrays
   json::Array completed, pending;
   fillJsonRange(completed_, &completed);
   fillJsonRange(pending_, &pending);

   // emit top-level values
   json::Object unit;
   unit[kQueueUnitCode]      = string_utils::wideToUtf8(code_);
   unit[kQueueUnitDocId]     = docId_;
   unit[kQueueUnitChunkId]   = chunkId_;
   unit[kQueueUnitCompleted] = completed;
   unit[kQueueUnitPending]   = pending;
   unit[kQueueUnitExecMode]  = execMode_;
   unit[kQueueUnitExecScope] = execScope_;
   unit[kQueueUnitExecuting] = executing_.toJson();

   return unit;
}

std::string NotebookQueueUnit::popExecRange(ExecRange* pRange, 
      ExpressionMode mode)
{
   // do we have any unevaluated code in this execution unit?
   if (pending_.empty())
      return "";

   // inline chunks always execute all their code at once
   if (execScope_ == ExecScopeInline)
   {
      pending_.clear();
      return string_utils::wideToUtf8(code_);
   }

   // extract next range to execute
   ExecRange& range = *pending_.begin();
   int start = range.start;
   int stop = range.stop;

   // use the first line of the range if it's multi-line
   size_t idx = code_.find('\n', start + 1);
   if (idx != std::string::npos && static_cast<int>(idx) < (stop - 1))
   {
      stop = idx;

      // adjust the range to account for the code we're about to send
      range.start = idx + 1;
   }
   else
   {
      // not multi line, remove range entirely
      pending_.pop_front();
   }

   // mark completed and extract from code
   if (mode == ExprModeNew || executing_.empty())
      executing_ = ExecRange(start, stop);
   else
      executing_.extendTo(ExecRange(start, stop));

   completed_.push_back(executing_);
   std::wstring code = code_.substr(start, stop - start);
   
   // return values to caller
   if (pRange)
      *pRange = executing_;

   return string_utils::wideToUtf8(code);
}

std::string NotebookQueueUnit::docId() const
{
   return docId_;
}

std::string NotebookQueueUnit::chunkId() const
{
   return chunkId_;
}

std::wstring NotebookQueueUnit::code() const
{
   return code_;
}

bool NotebookQueueUnit::complete() const
{
   return pending_.empty();
}

ExecScope NotebookQueueUnit::execScope() const
{
   return execScope_;
}

ExecMode NotebookQueueUnit::execMode() const
{
   return execMode_;
}

std::string NotebookQueueUnit::executingCode() const
{
   return string_utils::wideToUtf8(code_.substr(
            executing_.start, executing_.stop - executing_.start));
}

void NotebookQueueUnit::replaceCode(const std::string& code)
{
   // replace the entire body of the code
   code_ = string_utils::utf8ToWide(code);

   // replace the pending queue with one that executes exactly the code given
   pending_.clear();
   pending_.push_back(ExecRange(0, code_.length()));
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

