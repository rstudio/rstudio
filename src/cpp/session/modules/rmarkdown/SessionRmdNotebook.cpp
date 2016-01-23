/*
 * SessionRmdNotebook.cpp
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

#include <r/RJson.hpp>
#include <r/RExec.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

FilePath chunkOutputPath(const FilePath& file, 
                         const std::string& chunkId)
{
   return file.parent().childPath(file.stem() + ".rnb.cached")
                       .childPath(chunkId)
                       .childPath("contents.html");
}

Error executeInlineChunk(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse*)
{
   std::string file, chunkId, options, content;
   Error error = json::readParams(request.params, &file, &chunkId, &options, 
         &content);
   if (error)
      return error;

   // ensure we have a place to put the output
   FilePath chunkOutput = chunkOutputPath(FilePath(file), chunkId);
   error = chunkOutput.parent().ensureDirectory();
   if (error)
      return error;

   // render the contents to the cached folder, then extract the contents
   error = r::exec::RFunction(".rs.executeSingleChunk", options, content,
         chunkOutput.absolutePath()).call();
   if (error)
      return error;
   std::string html;
   error = core::readStringFromFile(chunkOutput, &html);
   if (error)
      return error;

   // this RPC enqueues a client event to show chunk output rather than
   // returning it directly for symmetry with other methods of updating chunk
   // output (such as deserialization from caching and collaborative editing)
   json::Object output;
   output["html"] = html;
   output["chunk_id"] = chunkId;
   output["file"] = file;
   ClientEvent event(client_events::kChunkOutput, output);
   module_context::enqueClientEvent(event);

   return Success();
}

} // anonymous namespace


Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "execute_inline_chunk", executeInlineChunk))
      (bind(module_context::sourceModuleRFile, "SessionRmdNotebook.R"));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

