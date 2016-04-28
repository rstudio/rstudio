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
#include "NotebookErrors.hpp"

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

FilePath getNextOutputFile(const std::string& docId, const std::string& chunkId,
   int outputType)
{
   OutputPair pair = lastChunkOutput(docId, chunkId);
   pair.ordinal++;
   pair.outputType = outputType;
   FilePath target = chunkOutputFile(docId, chunkId, pair);
   updateLastChunkOutput(docId, chunkId, pair);
   return target;
}

} // anonymous namespace

ChunkExecContext::ChunkExecContext(const std::string& docId, 
      const std::string& chunkId, const std::string& options, int pixelWidth,
      int charWidth):
   docId_(docId), 
   chunkId_(chunkId),
   prevWorkingDir_(""),
   pixelWidth_(pixelWidth),
   charWidth_(charWidth),
   prevCharWidth_(0),
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

   error = beginWidgetCapture(pixelWidth_, outputPath,
         outputPath.parent().complete(kChunkLibDir));
   if (error)
      LOG_ERROR(error);

   // log warnings immediately (unless user's changed the default warning
   // level)
   r::sexp::Protect protect;
   SEXP warnSEXP;
   error = r::exec::RFunction("getOption", "warn").call(&warnSEXP, &protect);
   if (!error)
   {
      prevWarn_.set(warnSEXP);
      error = r::options::setOption<int>("warn", 1);
      if (error)
         LOG_ERROR(error);
   }
   
   // reset width
   prevCharWidth_ = r::options::getOptionWidth();
   r::options::setOptionWidth(charWidth_);

   // reset working directory to doc path, if it has one
   std::string docPath;
   source_database::getPath(docId_, &docPath);
   if (!docPath.empty())
   {
      FilePath targetDir = module_context::resolveAliasedPath(docPath).parent();
      FilePath currentDir = FilePath::safeCurrentPath(targetDir);
      if (currentDir != targetDir)
      {
         error = FilePath::makeCurrent(targetDir.absolutePath());
         if (error)
            LOG_ERROR(error);
         else
            prevWorkingDir_ = currentDir.absolutePath();
      }
   }

   // begin capturing errors
   connections_.push_back(events().onErrorOutput.connect(
         boost::bind(&ChunkExecContext::onError, this, _1)));
   beginErrorCapture();

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
   FilePath target = getNextOutputFile(docId_, chunkId_, outputType);
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

   enqueueChunkOutput(docId_, chunkId_, notebookCtxId(), outputType, target);
}

void ChunkExecContext::onError(const core::json::Object& err)
{
   // write the error to a file 
   FilePath target = getNextOutputFile(docId_, chunkId_, kChunkOutputError);
   boost::shared_ptr<std::ostream> pOfs;
   Error error = target.open_w(&pOfs, true);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   json::write(err, *pOfs);
   
   pOfs->flush();
   pOfs.reset();

   // send to client
   enqueueChunkOutput(docId_, chunkId_, notebookCtxId(), kChunkOutputError, 
                      target);
}

void ChunkExecContext::onConsoleText(int type, const std::string& output, 
      bool truncate)
{
   if (output.empty())
      return;

   // determine output filename and ensure it exists
   FilePath outputCsv = chunkOutputFile(docId_, chunkId_, kChunkOutputText);
   Error error = outputCsv.ensureFile();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   std::vector<std::string> vals; 
   vals.push_back(safe_convert::numberToString(type));
   vals.push_back(output);
   error = core::writeStringToFile(outputCsv, 
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
   Error error;

   // restore width value
   r::options::setOptionWidth(prevCharWidth_);

   // restore preserved warning level, if any
   if (!prevWarn_.isNil())
   {
      error = r::options::setOption("warn", prevWarn_.get());
      if (error)
         LOG_ERROR(error);
   }

   // restore working directory, if we saved one
   if (!prevWorkingDir_.empty())
   {
      error = FilePath::makeCurrent(prevWorkingDir_);
      if (error)
         LOG_ERROR(error);
   }

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

