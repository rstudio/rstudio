/*
 * NotebookQueueUnit.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "NotebookQueueUnit.hpp"

#include <gsl/gsl-lite.hpp>

#include <session/SessionModuleContext.hpp>

#include <boost/algorithm/string.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RSession.hpp>

#define kQueueUnitCode       "code"
#define kQueueUnitDocId      "doc_id"
#define kQueueUnitChunkId    "chunk_id"
#define kQueueUnitDocType    "doc_type"
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
   for (const json::Value val : in)
   {
      // ignore non-value types
      if (!val.isObject())
         continue;

      ExecRange range(0, 0);
      Error error = ExecRange::fromJson(val.getObject(), &range);
      if (error)
         return error;

      pOut->push_back(range);
   }
   return Success();
}

void fillJsonRange(const std::list<ExecRange>& in, json::Array* pOut)
{
   for (const ExecRange& range : in)
   {
      pOut->push_back(range.toJson());
   }
}

} // anonymous namespace

Error ExecRange::fromJson(const json::Object& source,
                          ExecRange* pRange)
{
   return json::readObject(source,
         kQueueUnitRangeStart, pRange->start,
         kQueueUnitRangeStop,  pRange->stop);
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

Error NotebookQueueUnit::fromJson(
      const json::Object& source, 
      boost::shared_ptr<NotebookQueueUnit>* pUnit)
{
   // extract contained unit for manipulation
   NotebookQueueUnit& unit = *pUnit->get();

   // extract top-level values
   json::Array completed, pending;
   int execMode, execScope;
   std::string code;
   Error error = json::readObject(source, 
         kQueueUnitCode,      code,
         kQueueUnitDocId,     unit.docId_,
         kQueueUnitChunkId,   unit.chunkId_,
         kQueueUnitCompleted, completed,
         kQueueUnitPending,   pending,
         kQueueUnitExecMode,  execMode,
         kQueueUnitExecScope, execScope);
   if (error)
      LOG_ERROR(error);

   // extract document type separately -- we've seen it missing
   // in some scenarios
   std::string docType;
   error = json::readObject(source, kQueueUnitDocType, docType);
   if (error)
   {
      // try reading the source document from the database to recover the type
      boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument());
      Error dbError = source_database::get(unit.docId_, pDoc);
      if (dbError)
      {
         LOG_ERROR(error);
         LOG_ERROR(dbError);
      }
      else
      {
         docType = pDoc->type();
      }
   }
   
   // assume an R Markdown document type if we couldn't infer one
   unit.docType_ = docType.empty() ? kSourceDocumentTypeRMarkdown : docType;
   
   // convert enums
   unit.execMode_ = static_cast<ExecMode>(execMode);
   unit.execScope_ = static_cast<ExecScope>(execScope);
   unit.code_ = code;

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
   Error error = r::exec::RFunction(".rs.evaluateChunkOptions")
         .addUtf8Param(docId_)
         .addUtf8Param(docType_)
         .addUtf8Param(code_)
         .call(&sexpOptions, &protect);

   if (error)
      return error;

   // convert to JSON 
   json::Value jsonOptions;
   error = r::json::jsonValueFromList(sexpOptions, &jsonOptions);
   if (jsonOptions.isArray() &&
       jsonOptions.getArray().isEmpty())
   {
      // treat empty array as empty object
      *pOptions = json::Object();
   }
   else if (!jsonOptions.isObject())
   {
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }
   else 
   {
      *pOptions = jsonOptions.getObject();
   }
   
   // if this chunk defines a 'file' option, then we'll read
   // the contents of that file and use it for code. note that
   // any existing code in the chunk will be ignored.
   std::string file;
   Error readError = core::json::readObject(*pOptions, "file", file);
   if (!readError)
   {
      FilePath resolvedPath = module_context::resolveAliasedPath(file);
      
      std::string code;
      Error error = core::readStringFromFile(resolvedPath, &code);
      if (!error)
         replaceCode(code);
   }

   return Success();
}

Error NotebookQueueUnit::innerCode(std::string* pCode)
{
   return r::exec::RFunction(".rs.extractChunkInnerCode")
       .addUtf8Param(code_)
       .callUtf8(pCode);
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
   unit[kQueueUnitCode]      = code_;
   unit[kQueueUnitDocId]     = docId_;
   unit[kQueueUnitDocType]   = docType_;
   unit[kQueueUnitChunkId]   = chunkId_;
   unit[kQueueUnitCompleted] = completed;
   unit[kQueueUnitPending]   = pending;
   unit[kQueueUnitExecMode]  = execMode_;
   unit[kQueueUnitExecScope] = execScope_;
   unit[kQueueUnitExecuting] = executing_.toJson();

   return unit;
}

std::string NotebookQueueUnit::popExecRange(ExecRange* pRange, 
                                            ExpressionMode mode,
                                            const std::string& engine)
{
   // do we have any unevaluated code in this execution unit?
   if (pending_.empty())
      return "";

   // inline chunks always execute all their code at once
   if (execScope_ == ExecScopeInline)
   {
      pending_.clear();
      return code_;
   }

   // extract next range to execute
   ExecRange& range = *pending_.begin();
   int start = range.start;
   int stop = range.stop;

   // for python chunks, use the entire range, even when it's multi-line
   // otherwise, use the first line of the range if it's multi-line
   size_t idx = code_.find('\n', start + 1);
   if (engine == "python")
   {
      code_ = code_ + "\n"; // add a newline to terminate the multiline chunk
      stop = stop + 1;
      pending_.pop_front();
   }
   else if (idx != std::string::npos && gsl::narrow_cast<int>(idx) < (stop - 1))
   {
      stop = gsl::narrow_cast<int>(idx);

      // adjust the range to account for the code we're about to send
      range.start = gsl::narrow_cast<int>(idx) + 1;
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
   
   // return values to caller
   if (pRange)
      *pRange = executing_;

   std::string code = code_.substr(start, stop - start);
   return code;
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
   return code_.substr(executing_.start, executing_.stop - executing_.start);
}

void NotebookQueueUnit::replaceCode(const std::string& code)
{
   // replace the entire body of the code
   code_ = code;

   // replace the pending queue with one that executes exactly the code given
   pending_.clear();
   pending_.push_back(ExecRange(0, gsl::narrow_cast<int>(code_.length())));
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

