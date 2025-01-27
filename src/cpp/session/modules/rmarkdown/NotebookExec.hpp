/*
 * NotebookExec.hpp
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

#ifndef SESSION_NOTEBOOK_EXEC_HPP
#define SESSION_NOTEBOOK_EXEC_HPP

#include <session/SessionModuleContext.hpp>

#include <core/BoostSignals.hpp>
#include <core/FileLock.hpp>
#include <shared_core/json/Json.hpp>

#include <r/RSexp.hpp>

#include "NotebookCapture.hpp"
#include "NotebookOutput.hpp"
#include "NotebookChunkOptions.hpp"

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

void flushPendingChunkConsoleOutputs(bool clear);

core::Error copyLibDirForOutput(const core::FilePath& file,
   const std::string& docId, const std::string& nbCtxId);

class ChunkExecContext : public NotebookCapture
{
public:
   // initialize a new execution context
   ChunkExecContext(const std::string& docId,
                    const std::string& chunkId,
                    const std::string& chunkCode,
                    const std::string& chunkLabel,
                    const std::string& nbCtxId,
                    const std::string& engine,
                    ExecScope execScope,
                    const core::FilePath& workingDir,
                    const ChunkOptions& options,
                    int pixelWidth,
                    int charWidth);

   // return execution context from events
   std::string chunkId();
   std::string docId();
   std::string engine();
   ExecScope execScope();
   const ChunkOptions& options();

   // inject console input/output manually
   void onConsoleInput(const std::string& input);
   void onConsoleOutput(module_context::ConsoleOutputType type,
                        const std::string& output);

   // invoked to indicate that an expression has finished evaluating
   void onExprComplete();

   bool hasErrors();

   void connect();
   void disconnect();

private:
   void onConsoleText(int type, const std::string& output, bool truncate, bool pending);
   void onConsolePrompt(const std::string&);
   void onFileOutput(const core::FilePath& file, const core::FilePath& sidecar,
        const core::json::Value& metadata, ChunkOutputType outputType, 
        unsigned ordinal);
   void onError(const core::json::Object& err);
   bool onCondition(Condition condition, const std::string &message);
   void initializeOutput();

   std::string docId_;
   std::string chunkId_;
   std::string chunkCode_;
   std::string chunkLabel_;
   std::string nbCtxId_;
   std::string engine_;
   std::string pendingInput_;
   core::FilePath outputPath_;
   core::FilePath workingDir_;
   ChunkOptions options_;

   int pixelWidth_;
   int charWidth_;
   int prevCharWidth_;
   int lastOutputType_;
   ExecScope execScope_;
   
   // we save both the previous R warning level,
   // as well as the chunk warning level, so that
   // we can detect if users try to set options(warn = 2)
   // within a chunk directly (affecting global state)
   r::sexp::PreservedSEXP rGlobalWarningLevel_;
   r::sexp::PreservedSEXP rChunkWarningLevel_;
   
   core::FilePath consoleChunkOutputFile_;
   bool hasOutput_;
   bool hasErrors_;

   std::vector<std::unique_ptr<NotebookCapture>> captures_;
   std::vector<std::unique_ptr<core::ScopedFileLock>> locks_;
   std::vector<RSTUDIO_BOOST_CONNECTION> connections_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_NOTEBOOK_EXEC_HPP
