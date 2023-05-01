/*
 * SessionCopilot.cpp
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

#include "SessionCopilot.hpp"

#include <shared_core/Error.hpp>

#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace copilot {

namespace {

void onDocAdded(const std::string& id)
{
   Error error = r::exec::RFunction(".rs.copilot.onDocAdded")
         .addParam(id)
         .call();

   if (error)
      LOG_ERROR(error);
}

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error = r::exec::RFunction(".rs.copilot.onDocUpdated")
         .addParam(pDoc->id())
         .addParam(pDoc->contents())
         .call();

   if (error)
      LOG_ERROR(error);
}

void onDocRemoved(const std::string& id, const std::string& path)
{
   Error error = r::exec::RFunction(".rs.copilot.onDocRemoved")
         .addParam(id)
         .call();

   if (error)
      LOG_ERROR(error);
}

void onDeferredInit(bool newSession)
{
   source_database::events().onDocAdded.connect(onDocAdded);
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocRemoved.connect(onDocRemoved);
}

} // end anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   events().onDeferredInit.connect(onDeferredInit);

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionCopilot.R"));
   return initBlock.execute();

}

} // end namespace copilot
} // end namespace modules
} // end namespace session
} // end namespace rstudio
