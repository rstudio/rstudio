/*
 * NotebookQueueUnit.hpp
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

#ifndef SESSION_NOTEBOOK_QUEUE_UNIT_HPP
#define SESSION_NOTEBOOK_QUEUE_UNIT_HPP

#include "SessionRmdNotebook.hpp"

#include <shared_core/json/Json.hpp>

#include <list>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

struct ExecRange 
{
   ExecRange():
      start(0),
      stop(0)
   {}
   ExecRange(int startIn, int stopIn):
      start(startIn),
      stop(stopIn)
   {}
   static core::Error fromJson(
         const core::json::Object& source, 
         ExecRange *pRange);
   void extendTo(const ExecRange& other);
   bool empty();
   core::json::Object toJson() const;
   int start;
   int stop;
};

enum ExpressionMode
{
   ExprModeNew          = 0,
   ExprModeContinuation = 1
};

class NotebookQueueUnit : boost::noncopyable
{
public:
   // serialization/deserialization
   static core::Error fromJson(
         const core::json::Object& source,
         boost::shared_ptr<NotebookQueueUnit>* pUnit);
   core::json::Object toJson() const;

   core::Error parseOptions(core::json::Object* pOptions);
   std::string popExecRange(ExecRange* pRange, ExpressionMode mode);
   bool complete() const;
   core::Error innerCode(std::string* pCode);
   void updateFrom(const NotebookQueueUnit& other);
   bool hasPendingRanges();
   void replaceCode(const std::string& code);

   // accessors
   std::string docId() const;
   std::string chunkId() const;
   ExecMode execMode() const;
   ExecScope execScope() const;
   std::wstring code() const;
   std::string executingCode() const;

private:
   std::string docId_;
   std::string chunkId_;
   ExecMode execMode_;
   ExecScope execScope_;
   std::wstring code_;
   std::list<ExecRange> completed_;
   std::list<ExecRange> pending_;
   ExecRange executing_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
