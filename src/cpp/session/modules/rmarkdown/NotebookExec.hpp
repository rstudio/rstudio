/*
 * NotebookExec.hpp
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

#ifndef SESSION_NOTEBOOK_EXEC_HPP
#define SESSION_NOTEBOOK_EXEC_HPP

#include <session/SessionModuleContext.hpp>

#include <boost/signal.hpp>

#include <core/json/Json.hpp>

#include <r/RSexp.hpp>

#include "NotebookCapture.hpp"
#include "NotebookOutput.hpp"
#include "NotebookChunkOptions.hpp"

#define kStagingSuffix "_t"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

class ChunkExecContext : public NotebookCapture
{
public:
   // initialize a new execution context
   ChunkExecContext(const std::string& docId, const std::string& chunkId,
         const std::string& nbCtxId, ExecScope execScope, 
         const core::FilePath& workingDir, const ChunkOptions& options, 
         int pixelWidth, int charWidth);

   // return execution context from events
   std::string chunkId();
   std::string docId();
   ExecScope execScope();
   const ChunkOptions& options();

   // inject console input manually
   void onConsoleInput(const std::string& input);

   // invoked to indicate that an expression has finished evaluating
   void onExprComplete();

   bool hasErrors();

   void connect();
   void disconnect();

private:
   void onConsoleOutput(module_context::ConsoleOutputType type, 
         const std::string& output);
   void onConsoleText(int type, const std::string& output, bool truncate);
   void onConsolePrompt(const std::string&);
   void onFileOutput(const core::FilePath& file, const core::FilePath& sidecar,
        const core::json::Value& metadata, ChunkOutputType outputType, 
        unsigned ordinal);
   void onError(const core::json::Object& err);
   bool onCondition(Condition condition, const std::string &message);
   void initializeOutput();

   std::string docId_;
   std::string chunkId_;
   std::string nbCtxId_;
   std::string pendingInput_;
   core::FilePath outputPath_;
   core::FilePath workingDir_;
   ChunkOptions options_;

   int pixelWidth_;
   int charWidth_;
   int prevCharWidth_;
   ExecScope execScope_;
   r::sexp::PreservedSEXP prevWarn_;

   bool hasOutput_;
   bool hasErrors_;

   std::vector<boost::shared_ptr<NotebookCapture> > captures_;
   std::vector<boost::signals::connection> connections_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_NOTEBOOK_EXEC_HPP
