/*
 * NotebookExec.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookExec.hpp"
#include "NotebookOutput.hpp"
#include "NotebookPlots.hpp"
#include "NotebookHtmlWidgets.hpp"
#include "NotebookCache.hpp"
#include "NotebookData.hpp"
#include "NotebookErrors.hpp"
#include "NotebookWorkingDir.hpp"
#include "NotebookConditions.hpp"

#include <shared_core/Error.hpp>
#include <core/text/CsvParser.hpp>
#include <core/FileSerializer.hpp>

#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>

#include <iostream>

using namespace rstudio::core;

#define kRStudioNotebookExecuting ("rstudio.notebook.executing")

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

FilePath getNextOutputFile(const std::string& docId, const std::string& chunkId,
   const std::string& nbCtxId, ChunkOutputType outputType, unsigned *pOrdinal)
{
   OutputPair pair = lastChunkOutput(docId, chunkId, nbCtxId);
   pair.ordinal++;
   pair.outputType = outputType;
   if (pOrdinal)
      *pOrdinal = pair.ordinal;
   FilePath target = chunkOutputFile(docId, chunkId, nbCtxId, pair);
   updateLastChunkOutput(docId, chunkId, pair);
   return target;
}

} // anonymous namespace

core::Error copyLibDirForOutput(const core::FilePath& file,
   const std::string& docId, const std::string& nbCtxId)
{
   Error error = Success();

   FilePath fileLib = file.getParent().completePath(kChunkLibDir);
   if (fileLib.exists())
   {
      std::string docPath;
      source_database::getPath(docId, &docPath);
      error = mergeLib(fileLib, chunkCacheFolder(docPath, docId, nbCtxId)
         .completePath(kChunkLibDir));
      if (error)
         LOG_ERROR(error);

      error = fileLib.remove();
      if (error)
         LOG_ERROR(error);
   }

   return error;
}

ChunkExecContext::ChunkExecContext(const std::string& docId,
                                   const std::string& chunkId,
                                   const std::string& chunkCode,
                                   const std::string& chunkLabel,
                                   const std::string& nbCtxId,
                                   const std::string& engine,
                                   ExecScope execScope,
                                   const core::FilePath& workingDir,
                                   const ChunkOptions& options,
                                   int pixelWidth,
                                   int charWidth)
   : docId_(docId),
     chunkId_(chunkId),
     chunkCode_(chunkCode),
     chunkLabel_(chunkLabel),
     nbCtxId_(nbCtxId),
     engine_(engine),
     workingDir_(workingDir),
     options_(options),
     pixelWidth_(pixelWidth),
     charWidth_(charWidth),
     prevCharWidth_(0),
     lastOutputType_(kChunkConsoleInput),
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

std::string ChunkExecContext::engine()
{
   return engine_;
}

const ChunkOptions& ChunkExecContext::options() 
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

   // capture conditions
   boost::shared_ptr<ConditionCapture> pConditionCapture = 
      boost::make_shared<ConditionCapture>();
   pConditionCapture->connect();
   captures_.push_back(pConditionCapture);
   connections_.push_back(events().onCondition.connect(
         boost::bind(&ChunkExecContext::onCondition, this, _1, _2)));

   // extract knitr figure options if present
   double figWidth = options_.getOverlayOption("fig.width", 0.0);
   double figHeight = options_.getOverlayOption("fig.height", 0.0);
   
   // if 'fig.asp' is set, then use that to override 'fig.height'
   double figAsp = options_.getOverlayOption("fig.asp", 0.0);
   if (figAsp != 0.0)
   {
      // if figWidth is unset, default to 7.0
      if (figWidth == 0.0)
         figWidth = 7.0;
      
      figHeight = figWidth * figAsp;
   }

   // begin capturing plots 
   connections_.push_back(events().onPlotOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2, 
                     _3, ChunkOutputPlot, _4)));

   boost::shared_ptr<PlotCapture> pPlotCapture = 
      boost::make_shared<PlotCapture>();
   captures_.push_back(pPlotCapture);

   if (figWidth > 0 || figHeight > 0)
   {
      // user specified plot size, use it
      error = pPlotCapture->connectPlots(docId_, chunkId_, nbCtxId_, 
            figHeight, figWidth, PlotSizeManual, outputPath_);
   }
   else
   {
      // user didn't specify plot size, use the width of the editor surface
      error = pPlotCapture->connectPlots(docId_, chunkId_, nbCtxId_,
            0, pixelWidth_, PlotSizeAutomatic, outputPath_);
   }
   if (error)
      LOG_ERROR(error);

   // begin capturing HTML input
   connections_.push_back(events().onHtmlOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2, _3, 
                     ChunkOutputHtml, 0)));

   boost::shared_ptr<HtmlCapture> pHtmlCapture = 
      boost::make_shared<HtmlCapture>();
   captures_.push_back(pHtmlCapture);

   error = pHtmlCapture->connectHtmlCapture(
            outputPath_,
            outputPath_.getParent().completePath(kChunkLibDir),
            options_.chunkOptions());
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
      bool warning = options_.getOverlayOption("warning", true);
      error = r::options::setOption<int>("warn", warning ? 1 : -1);
      if (error)
         LOG_ERROR(error);
   }

   // broadcast that we're executing in a Notebook
   r::options::setOption(kRStudioNotebookExecuting, true);
   
   // reset width
   prevCharWidth_ = r::options::getOptionWidth();
   r::options::setOptionWidth(charWidth_);

   boost::shared_ptr<DirCapture> pDirCapture = boost::make_shared<DirCapture>();
   error = pDirCapture->connectDir(docId_, workingDir_);
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

   // begin capturing data
   connections_.push_back(events().onDataOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2, _3, 
                     ChunkOutputData, 0)));

   boost::shared_ptr<DataCapture> pDataCapture = 
      boost::make_shared<DataCapture>();
   captures_.push_back(pDataCapture);

   error = pDataCapture->connectDataCapture(
            outputPath_,
            options_.mergedOptions());
   if (error)
      LOG_ERROR(error);

   NotebookCapture::connect();
}

bool ChunkExecContext::onCondition(Condition condition,
      const std::string& message)
{
   // skip if the user has asked us to suppress this kind of condition
   if (condition == ConditionMessage && 
       !options_.getOverlayOption("message", true))
   {
      return false;
   }
   if (condition == ConditionWarning && 
       !options_.getOverlayOption("warning", true))
   {
      return false;
   }

   // give each capturing module a chance to handle the condition
   for (boost::shared_ptr<NotebookCapture> pCapture : captures_)
   {
      if (pCapture->onCondition(condition, message))
         return true;
   }

   // none of them did; treat it as ordinary output
   onConsoleOutput(module_context::ConsoleOutputError, message);
   module_context::enqueClientEvent(
      ClientEvent(client_events::kConsoleWriteError, message));

   return true;
}

void ChunkExecContext::onFileOutput(const FilePath& file, 
      const FilePath& sidecar, const core::json::Value& metadata, 
      ChunkOutputType outputType, unsigned ordinal)
{
   // set up folder to receive output if necessary
   initializeOutput();

   // put the file in sequence inside the host directory
   FilePath target;
   if (ordinal == 0)
   {
      // unspecified ordinal, generate one
      target = getNextOutputFile(docId_, chunkId_, nbCtxId_, outputType,
            &ordinal);
   }
   else
   {
      // known ordinal, use it (this can occur for out of sequence events, such
      // as plots)
      OutputPair pair(outputType, ordinal);
      target = chunkOutputFile(docId_, chunkId_, nbCtxId_, pair);
   }

   // preserve original extension; some output types, such as plots, don't
   // have a canonical extension
   target = target.getParent().completePath(target.getStem() + file.getExtension());

   Error error = file.move(target);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // check to see if the file has an accompanying library folder; if so, move
   // it to the global library folder
   copyLibDirForOutput(file, docId_, nbCtxId_);

   // if output sidecar file was provided, write it out
   if (!sidecar.isEmpty())
   {
      sidecar.move(target.getParent().completePath(
               target.getStem() + sidecar.getExtension()));
   }

   // serialize metadata if provided
   if (!metadata.isNull())
   {
      error = writeStringToFile(target.getParent().completePath(
               target.getStem() + ".metadata"), metadata.write());
   }

   enqueueChunkOutput(docId_, chunkId_, nbCtxId_, ordinal, outputType, target,
         metadata);
}

void ChunkExecContext::onError(const core::json::Object& err)
{
   // set up folder to receive output if necessary
   initializeOutput();

   // mark error state
   hasErrors_ = true;

   // write the error to a file 
   unsigned ordinal;
   FilePath target = getNextOutputFile(docId_, chunkId_, nbCtxId_, 
         ChunkOutputError, &ordinal);
   std::shared_ptr<std::ostream> pOfs;
   Error error = target.openForWrite(pOfs, true);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   err.write(*pOfs);
   
   pOfs->flush();
   pOfs.reset();

   // send to client
   enqueueChunkOutput(docId_, chunkId_, nbCtxId_, ordinal, ChunkOutputError, 
                      target);
}

void ChunkExecContext::onConsoleText(int type, const std::string& output, 
      bool truncate)
{
   // if we haven't received any actual output yet, don't push input into the
   // file yet
   if (type == kChunkConsoleInput && !hasOutput_) 
   {
      pendingInput_.append(output + "\n");
      return;
   }

   // blank lines aren't permitted following output, only input
   if (lastOutputType_ != kChunkConsoleInput && type == kChunkConsoleInput && output.empty())
      return;
   lastOutputType_ = type;

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
         ChunkOutputText);
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

   // if we got some real output, fire event for it
   if (!output.empty())
      events().onChunkConsoleOutput(docId_, chunkId_, type, output);
}

void ChunkExecContext::disconnect()
{
   Error error;

   // clean up capturing modules (includes plots, errors, and HTML widgets)
   for (boost::shared_ptr<NotebookCapture> pCapture : captures_)
   {
      pCapture->disconnect();
   }

   // clean up staging folder
   error = outputPath_.removeIfExists();
   if (error)
      LOG_ERROR(error);

   // broadcast that we're done with notebook execution
   r::options::setOption(kRStudioNotebookExecuting, false);

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
   for (const RSTUDIO_BOOST_CONNECTION& connection : connections_)
   {
      connection.disconnect();
   }

   NotebookCapture::disconnect();

   events().onChunkExecCompleted(docId_, chunkId_, chunkCode_, chunkLabel_, nbCtxId_);
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
   for (boost::shared_ptr<NotebookCapture> pCapture : captures_)
   {
      pCapture->onExprComplete();
   }
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

