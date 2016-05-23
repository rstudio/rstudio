/*
 * NotebookQueueUnit.hpp
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

#ifndef SESSION_NOTEBOOK_QUEUE_UNIT_HPP
#define SESSION_NOTEBOOK_QUEUE_UNIT_HPP

#include <core/json/Json.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

struct ExecRange 
{
   int start;
   int stop;
};

class NotebookQueueUnit
{
public:
   // serialization/deserialization
   static core::Error fromJson (
         const core::json::Object& source,
         boost::shared_ptr<NotebookQueueUnit>* pUnit);
   core::json::Object toJson() const;

   core::Error parseOptions(core::json::Object* pOptions);

   // accessors
   std::string docId() const;
   std::string chunkId() const;
   std::string code() const;

private:
   std::string docId_;
   std::string chunkId_;
   std::string code_;
   std::vector<ExecRange> completed_;
   std::vector<ExecRange> pending_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
