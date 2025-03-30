/*
 * NotebookExec.cpp
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

#include <memory>
#include <iostream>

#include <shared_core/Error.hpp>

#include <core/text/CsvParser.hpp>
#include <core/FileSerializer.hpp>

#include <r/ROptions.hpp>
#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

#define kRStudioNotebookExecuting ("rstudio.notebook.executing")

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

std::recursive_mutex s_consoleMutex;

struct PendingChunkConsoleOutput
{
   int type;
   std::string output;
};

std::map<FilePath, std::vector<PendingChunkConsoleOutput>> s_pendingChunkOutput;

void flushPendingChunkConsoleOutput(const FilePath& outputCsv)
{
   auto&& chunkOutputs = s_pendingChunkOutput[outputCsv];
   if (chunkOutputs.empty())
      return;
   
   std::stringstream ss;
   for (auto&& chunkOutput : chunkOutputs)
   {
      std::vector<std::string> values;
      values.push_back(safe_convert::numberToString(chunkOutput.type));
      values.push_back(chunkOutput.output);
      ss << text::encodeCsvLine(values) << "\n";
   }
   
   Error error = core::writeStringToFile(outputCsv, ss.str());
   if (error)
      LOG_ERROR(error);
}
   
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

void flushPendingChunkConsoleOutputs(bool clear)
{
   std::lock_guard<std::recursive_mutex> guard(s_consoleMutex);

   for (auto&& entry : s_pendingChunkOutput)
      flushPendingChunkConsoleOutput(entry.first);

   if (clear)
      s_pendingChunkOutput.clear();
}

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

   // leave an execution lock in this folder so it won't be moved if the notebook
   // is saved while executing
   auto lock = boost::make_unique<ScopedFileLock>(
       FileLock::createDefault(),
       outputPath_.completePath(kExecutionLock));
   locks_.push_back(std::move(lock));

   // if executing the whole chunk, initialize output right away (otherwise we
   // wait until we actually have output)
   if (execScope_ == ExecScopeChunk)
      initializeOutput();

   // capture conditions
   auto pConditionCapture = boost::make_unique<ConditionCapture>();
   pConditionCapture->connect();
   captures_.push_back(std::move(pConditionCapture));
   connections_.push_back(events().onCondition.connect(
         boost::bind(&ChunkExecContext::onCondition, this, _1, _2)));

   // extract knitr figure options if present
   double figWidth = options_.getOverlayOption("fig.width", 0.0);
   double figHeight = options_.getOverlayOption("fig.height", 0.0);
   double figDpi = options_.getOverlayOption("dpi", -1.0);
   
   // the knitr 'dev' option, if set, may override the default graphics device backend
   std::string chunkGraphicsBackend = options_.getOverlayOption("dev", std::string("png"));

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

   auto pPlotCapture = boost::make_unique<PlotCapture>();

   if (figWidth > 0 || figHeight > 0)
   {
      // user specified plot size, use it
      error = pPlotCapture->connectPlots(
               docId_, chunkId_, nbCtxId_,
               figWidth, figHeight, figDpi,
               PlotSizeManual, outputPath_, chunkGraphicsBackend);
   }
   else
   {
      // user didn't specify plot size, use the width of the editor surface
      error = pPlotCapture->connectPlots(
               docId_, chunkId_, nbCtxId_,
               pixelWidth_, 0, figDpi,
               PlotSizeAutomatic, outputPath_, chunkGraphicsBackend);
   }
   if (error)
      LOG_ERROR(error);

   captures_.push_back(std::move(pPlotCapture));

   // begin capturing HTML input
   connections_.push_back(events().onHtmlOutput.connect(
         boost::bind(&ChunkExecContext::onFileOutput, this, _1, _2, _3, 
                     ChunkOutputHtml, 0)));

   auto pHtmlCapture = boost::make_unique<HtmlCapture>();
   error = pHtmlCapture->connectHtmlCapture(
            outputPath_,
            outputPath_.getParent().completePath(kChunkLibDir),
            options_.chunkOptions());
   if (error)
      LOG_ERROR(error);
   captures_.push_back(std::move(pHtmlCapture));

   // log warnings immediately
   // (unless user's changed the default warning level)
   int rWarningLevel = 1;
   error = r::exec::RFunction("getOption", "warn").call(&rWarningLevel);
   if (!error)
   {
      // save the current warning level
      rGlobalWarningLevel_.set(Rf_ScalarInteger(rWarningLevel));

      // default warning setting is 1 (log immediately), but if the warning
      // option is set to FALSE, we want to set it to -1 (ignore warnings)
      int chunkWarningLevel;
      if (options_.hasOverlayOption("warning"))
      {
         bool warningsEnabled = options_.getOverlayOption("warning", true);
         chunkWarningLevel = warningsEnabled ? 1 : -1;
      }
      
      // ensure that warnings are shown by default
      else if (rWarningLevel == 0)
      {
         chunkWarningLevel = 1;
      }
      
      // otherwise, just preserve the current warning level
      else
      {
         chunkWarningLevel = rWarningLevel;
      }
      
      // update warning level for this chunk
      if (rWarningLevel != chunkWarningLevel)
      {
         rChunkWarningLevel_.set(Rf_ScalarInteger(chunkWarningLevel));
         SEXP cellSEXP = r::options::getOptionCell("warn");
         SETCAR(cellSEXP, rChunkWarningLevel_.get());
      }
   }

   // broadcast that we're executing in a Notebook
   r::options::setOption(kRStudioNotebookExecuting, true);
   
   // reset width
   prevCharWidth_ = r::options::getOptionWidth();
   r::options::setOptionWidth(charWidth_);

   auto pDirCapture = boost::make_unique<DirCapture>();
   error = pDirCapture->connectDir(docId_, workingDir_);
   if (error)
      LOG_ERROR(error);
   captures_.push_back(std::move(pDirCapture));

   // begin capturing errors
   auto pErrorCapture = boost::make_unique<ErrorCapture>();
   pErrorCapture->connect();
   captures_.push_back(std::move(pErrorCapture));

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

   auto pDataCapture = boost::make_unique<DataCapture>();
   error = pDataCapture->connectDataCapture(
            outputPath_,
            options_.mergedOptions());
   if (error)
      LOG_ERROR(error);
   captures_.push_back(std::move(pDataCapture));

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
   for (auto&& pCapture : captures_)
   {
      if (pCapture->onCondition(condition, message))
         return true;
   }

   // add to event queue
   session::clientEventQueue().add(
      ClientEvent(client_events::kConsoleWriteError, message));

   // force events to be flushed, so output is displayed
   session::clientEventQueue().flush();

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
      bool truncate, bool pending)
{
   std::lock_guard<std::recursive_mutex> guard(s_consoleMutex);

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
      if (pending)
      {
         // guard against any possibility of runaway recursion by discarding pending input if we are
         // already processing it (no clear codepath leads to this but we've seen behavior that
         // looks like it in the wild)
         LOG_WARNING_MESSAGE("Discarding pending notebook text '" + input + "'");
      }
      else
      {
         onConsoleText(kChunkConsoleInput, input, true, true);
      }
   }

   // determine output filename and ensure it exists
   FilePath outputCsv = chunkOutputFile(docId_, chunkId_, nbCtxId_, ChunkOutputText);
   if (outputCsv != consoleChunkOutputFile_)
   {
      consoleChunkOutputFile_ = outputCsv;
      Error error = outputCsv.ensureFile();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   // truncate if necessary
   if (truncate)
      s_pendingChunkOutput.erase(outputCsv);
   
   // add pending chunk output
   PendingChunkConsoleOutput chunkOutput;
   chunkOutput.type = type;
   chunkOutput.output = output;
   s_pendingChunkOutput[outputCsv].push_back(chunkOutput);

   // if we got some real output, fire event for it
   if (!output.empty())
      events().onChunkConsoleOutput(docId_, chunkId_, type, output);
}

void ChunkExecContext::disconnect()
{
   Error error;

   // flush any pending chunk output
   flushPendingChunkConsoleOutputs(true);
   
   // clean up capturing modules (includes plots, errors, and HTML widgets)
   for (auto&& pCapture : captures_)
   {
      pCapture->disconnect();
   }

   // clear all execution locks
   locks_.clear();

   // clean up staging folder
   error = outputPath_.removeIfExists();
   if (error)
      LOG_ERROR(error);

   // broadcast that we're done with notebook execution
   r::options::setOption(kRStudioNotebookExecuting, false);

   // restore width value
   r::options::setOptionWidth(prevCharWidth_);

   // restore warn (if it wasn't changed in the chunk)
   // note that we intentionally compare the pointers here;
   // we only want to take action if the SEXP pointer returned
   // via 'getOption()' has changed
   SEXP warningSEXP = r::options::getOption("warn");
   if (warningSEXP == rChunkWarningLevel_.get())
   {
      error = r::options::setOption("warn", rGlobalWarningLevel_.get());
      if (error)
         LOG_ERROR(error);
   }

   // unhook all our event handlers
   for (const RSTUDIO_BOOST_CONNECTION& connection : connections_)
   {
      connection.disconnect();
   }

   NotebookCapture::disconnect();

   // check to see whether we need to migrate the output folder to another location;
   // this addresses the case where the output folder location changed during execution,
   // i.e. if the notebook was saved while running
   FilePath migrationFile = chunkOutputPath(docId_, chunkId_, nbCtxId_, ContextExact)
      .completePath(kMigrationTarget);
   if (migrationFile.exists())
   {
      std::string path;
      error = readStringFromFile(migrationFile, &path);
      if (error)
      {
         error.addProperty("description", "Unable to read notebook output migration file");
         LOG_ERROR(error);
      }
      else
      {
         // clean up migration file so it doesn't get moved
         error = migrationFile.remove();
         if (error)
         {
            error.addProperty("description", "Unable to clean up output migration file");
            LOG_ERROR(error);
         }

         // perform the migration
         FilePath target(path);
         error = target.removeIfExists();
         if (error)
         {
            error.addProperty("description", "Unable to remove old notebook output folder");
            LOG_ERROR(error);
         }
         else
         {
            error = migrationFile.getParent().move(target);
            if (error)
            {
               error.addProperty("description", "Unable to move notebook output folder");
               LOG_ERROR(error);
            }
         }
      }
   }

   events().onChunkExecCompleted(docId_, chunkId_, chunkCode_, chunkLabel_, nbCtxId_);
}

void ChunkExecContext::onConsoleOutput(
      module_context::ConsoleOutputType type,
      const std::string& output)
{
   if (type == module_context::ConsoleOutputNormal)
      onConsoleText(kChunkConsoleOutput, output, false, false);
   else
      onConsoleText(kChunkConsoleError, output, false, false);
}

void ChunkExecContext::onConsoleInput(const std::string& input)
{
   onConsoleText(kChunkConsoleInput, input, false, false);
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
   FilePath outputPath = chunkOutputPath(docId_, chunkId_, nbCtxId_, ContextExact);
   error = outputPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);

   // leave an execution lock in this folder so it won't be moved if the notebook
   // is saved while executing
   auto lock = boost::make_unique<ScopedFileLock>(
       FileLock::createDefault(),
       outputPath.completePath(kExecutionLock));
   locks_.push_back(std::move(lock));

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
   for (auto&& pCapture : captures_)
   {
      pCapture->onExprComplete();
   }
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

