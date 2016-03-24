/*
 * NotebookExec.cpp
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
#include "NotebookExec.hpp"
#include "NotebookOutput.hpp"
#include "NotebookPlots.hpp"
#include "NotebookHtmlWidgets.hpp"
#include "NotebookCache.hpp"

#include <boost/foreach.hpp>

#include <core/text/CsvParser.hpp>
#include <core/FileSerializer.hpp>

#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionSourceDatabase.hpp>

#include <iostream>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

bool moveLibFile(const FilePath& from, const FilePath& to, 
      const FilePath& path)
{
   std::string relativePath = path.relativePath(from);
   FilePath target = to.complete(relativePath);

   Error error = path.isDirectory() ?
                     target.ensureDirectory() :
                     path.move(target);
   if (error)
      LOG_ERROR(error);
   return true;
}

} // anonymous namespace

ChunkExecContext::ChunkExecContext(const std::string& docId, 
      const std::string& chunkId, const std::string& options, int width):
   docId_(docId), 
   chunkId_(chunkId),
   width_(width),
   prevWidth_(0),
   connected_(false)
{
}

ChunkExecContext::~ChunkExecContext()
{
   if (connected_)
      disconnect();
}

std::string ChunkExecContext::chunkId()
{
   return chunkId_;
}

std::string ChunkExecContext::docId()
{
   return docId_;
}

bool ChunkExecContext::connected()
{
   return connected_;
}

void ChunkExecContext::connect()
{
   FilePath outputPath = chunkOutputPath(docId_, chunkId_);
   Error error = outputPath.ensureDirectory();
   if (error)
   {
      // if we don't have a place to put the output, don't register any handlers
      // (will end in tears)
      LOG_ERROR(error);
      return;
   }

   // begin capturing plots 
   connections_.push_back(events().onPlotOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, FilePath(), 
                     kChunkOutputPlot)));

   error = beginPlotCapture(outputPath);
   if (error)
      LOG_ERROR(error);

   // begin capturing HTML input
   connections_.push_back(events().onHtmlOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2,
                     kChunkOutputHtml)));

   error = beginWidgetCapture(outputPath, 
         outputPath.parent().complete(kChunkLibDir));
   if (error)
      LOG_ERROR(error);

   // reset width
   prevWidth_ = r::options::getOptionWidth();
   r::options::setOptionWidth(width_);

   // begin capturing console text
   connections_.push_back(module_context::events().onConsolePrompt.connect(
         boost::bind(&ChunkExecContext::onConsolePrompt, this, _1)));
   connections_.push_back(module_context::events().onConsoleOutput.connect(
         boost::bind(&ChunkExecContext::onConsoleOutput, this, _1, _2)));
   connections_.push_back(module_context::events().onConsoleInput.connect(
         boost::bind(&ChunkExecContext::onConsoleInput, this, _1)));

   connected_ = true;
}

void ChunkExecContext::onConsolePrompt(const std::string& )
{
   if (connected_)
      disconnect();
}

void ChunkExecContext::onFileOutput(const FilePath& file, 
      const FilePath& metadata, int outputType)
{
   OutputPair pair = lastChunkOutput(docId_, chunkId_);
   pair.ordinal++;
   pair.outputType = outputType;
   FilePath target = chunkOutputFile(docId_, chunkId_, pair);
   Error error = file.move(target);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // check to see if the file has an accompanying library folder; if so, move
   // it to the global library folder
   FilePath fileLib = file.parent().complete(kChunkLibDir);
   if (fileLib.exists())
   {
      std::string docPath;
      source_database::getPath(docId_, &docPath);
      error = fileLib.childrenRecursive(boost::bind(moveLibFile, fileLib,
            chunkCacheFolder(docPath, docId_)
                           .complete(kChunkLibDir), _2));
      if (error)
         LOG_ERROR(error);
      error = fileLib.remove();
      if (error)
         LOG_ERROR(error);
   }

   // if JSON metadata was provided, write it out
   if (!metadata.empty())
   {
      metadata.move(target.parent().complete(
               target.stem() + metadata.extension()));
   }

   enqueueChunkOutput(docId_, chunkId_, outputType, target);
   updateLastChunkOutput(docId_, chunkId_, pair);
}

void ChunkExecContext::onConsoleText(int type, const std::string& output, 
      bool truncate)
{
   if (output.empty())
      return;

   FilePath outputCsv = chunkOutputFile(docId_, chunkId_, kChunkOutputText);

   std::vector<std::string> vals; 
   vals.push_back(safe_convert::numberToString(type));
   vals.push_back(output);
   Error error = core::writeStringToFile(outputCsv, 
         text::encodeCsvLine(vals) + "\n", 
         string_utils::LineEndingPassthrough, truncate);
   if (error)
   {
      LOG_ERROR(error);
   }

   events().onChunkConsoleOutput(docId_, chunkId_, type, output);
}

void ChunkExecContext::disconnect()
{
   // restore width value
   r::options::setOptionWidth(prevWidth_);

   // unhook all our event handlers
   BOOST_FOREACH(const boost::signals::connection connection, connections_) 
   {
      connection.disconnect();
   }

   connected_ = false;

   events().onChunkExecCompleted(docId_, chunkId_, notebookCtxId());

}

void ChunkExecContext::onConsoleOutput(module_context::ConsoleOutputType type, 
      const std::string& output)
{
   if (type == module_context::ConsoleOutputNormal)
      onConsoleText(kChunkConsoleOutput, output, false);
   else
      onConsoleText(kChunkConsoleError, output, false);
}

void ChunkExecContext::onConsoleInput(const std::string& input)
{
   onConsoleText(kChunkConsoleInput, input, false);
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

