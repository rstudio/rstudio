/*
 * NotebookOutput.hpp
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


#ifndef SESSION_NOTEBOOK_OUTPUT_HPP
#define SESSION_NOTEBOOK_OUTPUT_HPP

#include <string>

#define kChunkOutputPath   "chunk_output"

#define kChunkOutputNone  0
#define kChunkOutputText  1
#define kChunkOutputPlot  2
#define kChunkOutputHtml  3
#define kChunkOutputError 4

#define kChunkConsoleInput  0
#define kChunkConsoleOutput 1
#define kChunkConsoleError  2

#define kChunkLibDir "lib"

namespace rstudio {
namespace core {
   class FilePath;
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

// forms for requesting the chunk output folder
enum ChunkOutputContext
{
   ContextExact = 0,   // always use the requested context
   ContextSaved        // fall back to saved context if exact doesn't exist
};

struct OutputPair
{
   OutputPair() :
      outputType(kChunkOutputNone), ordinal(1)
   {}
   OutputPair(unsigned type, unsigned ord):
      outputType(type), ordinal(ord) 
   {}
   unsigned outputType;
   unsigned ordinal;
};

// given a document ID and a chunk ID, discover the last output the chunk had
OutputPair lastChunkOutput(const std::string& docId, 
                           const std::string& chunkId,
                           const std::string& nbCtxId);

void updateLastChunkOutput(const std::string& docId, 
                           const std::string& chunkId,
                           const OutputPair& pair);

// compute chunk output folder paths for specific contexts
core::FilePath chunkOutputPath(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId,
      ChunkOutputContext ctxType);
core::FilePath chunkOutputPath(const std::string& docId, 
      const std::string& chunkId, const std::string& nbCtxId, 
      ChunkOutputContext ctxType);
core::FilePath chunkOutputPath(const std::string& docId, 
      const std::string& chunkId, ChunkOutputContext ctxType);

// compute individual chunk output unit paths
core::FilePath chunkOutputFile(const std::string& docId, 
      const std::string& chunkId, const std::string& nbCtxId,
      const OutputPair& output);

core::FilePath chunkOutputFile(const std::string& docId, 
      const std::string& chunkId, const std::string& nbCtxId,
      unsigned outputType);

core::Error cleanChunkOutput(const std::string& docId, 
      const std::string& chunkId, const std::string& nbCtxId, 
      bool preserveFolder);

// helper for emitting console data to file
core::Error appendConsoleOutput(int chunkConsoleOutputType,
                                const std::string& output,
                                const core::FilePath& filePath);

// send chunk output to client
void enqueueChunkOutput(const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId, 
      int outputType, const core::FilePath& path);
core::Error enqueueChunkOutput(
      const std::string& docPath, const std::string& docId,
      const std::string& chunkId, const std::string& nbCtxId,
      const std::string& requestId);

core::Error initOutput();

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
