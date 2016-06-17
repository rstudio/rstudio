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
#include "NotebookWorkingDir.hpp"

#include <boost/foreach.hpp>

#include <core/text/CsvParser.hpp>
#include <core/FileSerializer.hpp>

#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include <iostream>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

FilePath getNextOutputFile(const std::string& docId, const std::string& chunkId,
   const std::string& nbCtxId, int outputType)
{
   OutputPair pair = lastChunkOutput(docId, chunkId, nbCtxId);
   pair.ordinal++;
   pair.outputType = outputType;
   FilePath target = chunkOutputFile(docId, chunkId, nbCtxId, pair);
   updateLastChunkOutput(docId, chunkId, pair);
   return target;
}

} // anonymous namespace

ChunkExecContext::ChunkExecContext(const std::string& docId, 
      const std::string& chunkId, const std::string& nbCtxId, 
      ExecScope execScope, const json::Object& options, int pixelWidth, 
      int charWidth):
   docId_(docId), 
   chunkId_(chunkId),
   nbCtxId_(nbCtxId),
   prevWorkingDir_(""),
   options_(options),
   pixelWidth_(pixelWidth),
   charWidth_(charWidth),
   prevCharWidth_(0),
   execScope_(execScope),
   hasOutput_(false),
   hasErrors_(false)
{
}

std::string ChunkExecContext::chunkId()
{
   return chunkId_;
}

std::string ChunkExecContext::docId()
{
   return docId_;
}

json::Object ChunkExecContext::options() 
{
   return options_;
}

void ChunkExecContext::connect()
{
   outputPath_ = chunkOutputPath(docId_, chunkId_ + kStagingSuffix, nbCtxId_,
         ContextExact);
   Error error = outputPath_.ensureDirectory();
   if (error)
   {
      // if we don't have a place to put the output, don't register any handlers
      // (will end in tears)
      LOG_ERROR(error);
      return;
   }

   // if executing the whole chunk, initialize output right away (otherwise we
   // wait until we actually have output)
   if (execScope_ == ExecScopeChunk)
      initializeOutput();

   // extract knitr figure options if present
   double figWidth = 0;
   double figHeight = 0;
   json::readObject(options_, "fig.width",  &figWidth);
   json::readObject(options_, "fig.height", &figHeight);

   // begin capturing plots 
   connections_.push_back(events().onPlotOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2, 
                     kChunkOutputPlot)));

   boost::shared_ptr<PlotCapture> pPlotCapture = 
      boost::make_shared<PlotCapture>();
   captures_.push_back(pPlotCapture);

   if (figWidth > 0 || figHeight > 0)
   {
      // user specified plot size, use it
      error = pPlotCapture->connectPlots(figHeight, figWidth, PlotSizeManual, 
            outputPath_);
   }
   else
   {
      // user didn't specify plot size, use the width of the editor surface
      error = pPlotCapture->connectPlots(0, pixelWidth_, PlotSizeAutomatic, 
            outputPath_);
   }
   if (error)
      LOG_ERROR(error);

   // begin capturing HTML input
   connections_.push_back(events().onHtmlOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2,
                     kChunkOutputHtml)));

   boost::shared_ptr<HtmlCapture> pHtmlCapture = 
      boost::make_shared<HtmlCapture>();
   captures_.push_back(pHtmlCapture);

   error = pHtmlCapture->connectHtmlCapture(
            outputPath_,
            outputPath_.parent().complete(kChunkLibDir),
            options_);
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

      // default warning setting is 1 (log immediately), but if the warning
      // option is set to FALSE, we want to set it to -1 (ignore warnings)
      bool warning = true;
      json::readObject(options_, "warning",  &warning);
      error = r::options::setOption<int>("warn", warning ? 1 : -1);
      if (error)
         LOG_ERROR(error);
   }
   
   // reset width
   prevCharWidth_ = r::options::getOptionWidth();
   r::options::setOptionWidth(charWidth_);

   boost::shared_ptr<DirCapture> pDirCapture = boost::make_shared<DirCapture>();
   error = pDirCapture->connectDir(docId_);
   if (error)
      LOG_ERROR(error);
   else
      captures_.push_back(pDirCapture);

   // begin capturing errors
   boost::shared_ptr<ErrorCapture> pErrorCapture = 
      boost::make_shared<ErrorCapture>();
   pErrorCapture->connect();
   captures_.push_back(pErrorCapture);

   connections_.push_back(events().onErrorOutput.connect(
         boost::bind(&ChunkExecContext::onError, this, _1)));

   // begin capturing console text
   connections_.push_back(module_context::events().onConsoleOutput.connect(
         boost::bind(&ChunkExecContext::onConsoleOutput, this, _1, _2)));
   connections_.push_back(module_context::events().onConsoleInput.connect(
         boost::bind(&ChunkExecContext::onConsoleInput, this, _1)));

   NotebookCapture::connect();
}

void ChunkExecContext::onFileOutput(const FilePath& file, 
      const FilePath& metadata, int outputType)
{
   // set up folder to receive output if necessary
   initializeOutput();

   // put the file in sequence inside the host directory
   FilePath target = getNextOutputFile(docId_, chunkId_, nbCtxId_, outputType);
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
      error = mergeLib(fileLib, chunkCacheFolder(docPath, docId_, nbCtxId_)
                                   .complete(kChunkLibDir));
      if (error)
         LOG_ERROR(error);
      error = fileLib.remove();
      if (error)
         LOG_ERROR(error);
   }

   // if output metadata was provided, write it out
   if (!metadata.empty())
   {
      metadata.move(target.parent().complete(
               target.stem() + metadata.extension()));
   }

   enqueueChunkOutput(docId_, chunkId_, nbCtxId_, outputType, target);
}

void ChunkExecContext::onError(const core::json::Object& err)
{
   // set up folder to receive output if necessary
   initializeOutput();

   // mark error state
   hasErrors_ = true;

   // write the error to a file 
   FilePath target = getNextOutputFile(docId_, chunkId_, nbCtxId_, 
         kChunkOutputError);
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
   enqueueChunkOutput(docId_, chunkId_, nbCtxId_, kChunkOutputError, 
                      target);
}

void ChunkExecContext::onConsoleText(int type, const std::string& output, 
      bool truncate)
{
   if (output.empty())
      return;

   // if we haven't received any actual output yet, don't push input into the
   // file yet
   if (type == kChunkConsoleInput && !hasOutput_) 
   {
      pendingInput_.append(output + "\n");
      return;
   }

   // set up folder to receive output if necessary
   initializeOutput();

   // flush any buffered pending input
   if (!pendingInput_.empty())
   {
      std::string input = pendingInput_;
      pendingInput_.clear();
      onConsoleText(kChunkConsoleInput, input, true);
   }

   // determine output filename and ensure it exists
   FilePath outputCsv = chunkOutputFile(docId_, chunkId_, nbCtxId_, 
         kChunkOutputText);
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

   // clean up capturing modules (includes plots, errors, and HTML widgets)
   BOOST_FOREACH(boost::shared_ptr<NotebookCapture> pCapture, captures_)
   {
      pCapture->disconnect();
   }

   // clean up staging folder
   error = outputPath_.removeIfExists();
   if (error)
      LOG_ERROR(error);

   // restore width value
   r::options::setOptionWidth(prevCharWidth_);

   // restore preserved warning level, if any
   if (!prevWarn_.isNil())
   {
      error = r::options::setOption("warn", prevWarn_.get());
      if (error)
         LOG_ERROR(error);
   }

   // unhook all our event handlers
   BOOST_FOREACH(const boost::signals::connection connection, connections_) 
   {
      connection.disconnect();
   }

   NotebookCapture::disconnect();

   events().onChunkExecCompleted(docId_, chunkId_, nbCtxId_);
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

void ChunkExecContext::initializeOutput()
{
   // if we already have output, do nothing
   if (hasOutput_)
      return;
   
   // if we don't have output yet, clean existing output before adding new 
   // output
   Error error = cleanChunkOutput(docId_, chunkId_, nbCtxId_, true);
   if (error)
      LOG_ERROR(error);

   // ensure that the output folder exists
   error = chunkOutputPath(docId_, chunkId_, nbCtxId_, ContextExact)
      .ensureDirectory();
   if (error)
      LOG_ERROR(error);

   hasOutput_ = true;
}

bool ChunkExecContext::hasErrors()
{
   return hasErrors_;
}

ExecScope ChunkExecContext::execScope()
{
   return execScope_;
}

void ChunkExecContext::onExprComplete()
{
   // notify capturing submodules
   BOOST_FOREACH(boost::shared_ptr<NotebookCapture> pCapture, captures_)
   {
      pCapture->onExprComplete();
   }
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

