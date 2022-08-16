   /*
 * SessionQuartoPreview.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionQuartoPreview.hpp"

#include <string>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/RegexUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionQuarto.hpp>
#include <session/SessionUrlPorts.hpp>

#include <session/prefs/UserPrefs.hpp>

#include "SessionQuartoJob.hpp"

using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {

using namespace quarto;

namespace modules {
namespace quarto {
namespace preview {

namespace {

class QuartoPreview : public QuartoJob

{
public:
   static Error create(const core::FilePath& previewFile,
                       const std::string& format,
                       const json::Value& editorState,
                       boost::shared_ptr<QuartoPreview>* ppPreview)
   {
      ppPreview->reset(new QuartoPreview(previewFile, format, editorState));
      return (*ppPreview)->start();
   }

   virtual ~QuartoPreview()
   {
   }

   FilePath previewFile()
   {
      return previewFile_;
   }

   std::string format()
   {
      return format_;
   }

   json::Value editorState()
   {
      return editorState_;
   }

   int slideLevel()
   {
      return slideLevel_;
   }

   bool hasModifiedProject()
   {
      if (!projectFile_.empty())
      {
         return FilePath(projectFile_.absolutePath()).getLastWriteTime() >
                projectFile_.lastWriteTime();
      }
      else
      {
         return false;
      }
   }

   int port()
   {
      return port_;
   }

   std::string jobId()
   {
      return pJob_->id();
   }

   std::string viewerType()
   {
      return viewerType_;
   }

   Error render(const json::Value& editorState)
   {
      // reset state
      slideLevel_= -1;
      editorState_ = editorState;
      outputFile_ = FilePath();
      allOutput_.clear();

      // re-read input file
      readInputFileLines();

      // render
      return r::exec::RFunction(".rs.quarto.renderPreview",
                                safe_convert::numberToString(port())).call();
   }

protected:
   explicit QuartoPreview(const FilePath& previewFile, const std::string& format, const json::Value& editorState)
      : QuartoJob(), previewFile_(previewFile), format_(format), editorState_(editorState), slideLevel_(-1), port_(0), viewerType_(prefs::userPrefs().rmdViewerType())
   {
     readInputFileLines();

     FilePath projectFile = quartoProjectConfigFile(previewFile_);
     if (!projectFile.isEmpty())
        projectFile_ = FileInfo(projectFile);
   }

   virtual std::string name()
   {
      return "Preview: " + previewFile_.getFilename();
   }

   virtual std::vector<std::string> args()
   {
      // preview target file
      std::vector<std::string> args({"preview"});
      args.push_back(string_utils::utf8ToSystem(previewFile_.getFilename()));

      // presentation mode if this is reveal
      if (formatIsRevealJs())
         args.push_back("--presentation");

      // format (or default if none specified)
      args.push_back("--to");
      args.push_back(!format_.empty() ? format_ : "default");

      // no watching inputs and no browser
      args.push_back("--no-watch-inputs");
      args.push_back("--no-browse");

      return args;
   }

   virtual void environment(core::system::Options* pEnv)
   {
      // if this file isn't in a project then add the QUARTO_CROSSREF_INDEX_PATH
      if (!isFileInSessionQuartoProject(previewFile_))
      {
         FilePath indexPath;
         Error error = module_context::perFilePathStorage(
            kQuartoCrossrefScope, previewFile_, false, &indexPath
         );
         if (error)
         {
            LOG_ERROR(error);
            return;
         }
         core::system::setenv(pEnv, "QUARTO_CROSSREF_INDEX_PATH", indexPath.getAbsolutePath());
      }
   }

   virtual core::FilePath workingDir()
   {
      return previewFile_.getParent();
   }


private:

   virtual void onStdErr(const std::string& output)
   {
      // accumulate output (used for error scanning)
      allOutput_ += output;

      // always be looking for an output file
      FilePath outputFile =
         module_context::extractOutputFileCreated(previewFile_.getParent(), output);
      if (!outputFile.isEmpty())
         outputFile_ = outputFile;

      // always be looking for slide-level
      int slideLevel = quartoSlideLevelFromOutput(output);
      if (slideLevel != -1)
      {
         slideLevel_ = slideLevel;
      }

      // detect browse directive
      if (port_ == 0) {
         auto location = quartoServerLocationFromOutput(output);
         if (location.port > 0)
         {
            // save port and path
            port_ = location.port;
            path_ = location.path;

            // show preview
            if (viewerType_ != kRmdViewerTypeNone)
            {
               showInViewer();
            }

            // restore the console tab after render
            activateConsole();

            // emit filtered output if we are on rstudio server and using the viewer
            if (session::options().programMode() == kSessionProgramModeServer)
            {
               QuartoJob::onStdErr(location.filteredOutput);
               QuartoJob::onStdErr("Browse at: " +
                                   core::system::getenv("RSTUDIO_HTTP_REFERER") +
                                   rstudioServerPreviewWindowUrl() + "\n");
            }
            else
            {
               QuartoJob::onStdErr(output);
            }
            return;
         }
      }

      if (port_ > 0 && output.find("Watching files for changes") != std::string::npos)
      {
         // activate the console
         activateConsole();

         // if the viewer is already on the site just activate it (however for revealjs go
         // back through standard presentation pane logic)
         if (viewerType_ == kRmdViewerTypePane)
         {
            if (!formatIsRevealJs() &&
                 boost::algorithm::starts_with(module_context::viewerCurrentUrl(false), viewerUrl()))
            {
               module_context::activatePane("viewer");
            }
            else
            {
               showInViewer();
            }
         }
      }

      // look for an error and do source navigation as necessary
      navigateToRenderPreviewError(previewFile_, previewFileLines_, output, allOutput_);

      // standard output forwarding
      QuartoJob::onStdErr(output);
   }

   void activateConsole()
   {
      ClientEvent activateConsoleEvent(client_events::kConsoleActivate, false);
      module_context::enqueClientEvent(activateConsoleEvent);
   }

   bool formatIsRevealJs()
   {
      return boost::algorithm::starts_with(format_, "revealjs");
   }

   void showInViewer()
   {
      if (viewerType_ == kRmdViewerTypePane)
      {
         // format info
         bool isReveal = formatIsRevealJs();
         bool isSlidy = boost::algorithm::starts_with(format_, "slidy");
         bool isBeamer = boost::algorithm::starts_with(format_, "beamer");

         // determine height
         int minHeight = -1; // maximize
         if (isReveal || isSlidy)
         {
            minHeight = 450;
         }
         else if (isBeamer)
         {
             minHeight = 500;
         }

         std::string sourceFile = module_context::createAliasedPath(previewFile_);
         std::string outputFile;
         if (!outputFile_.isEmpty())
            outputFile = module_context::createAliasedPath(outputFile_);
         QuartoNavigate quartoNav = QuartoNavigate::navDoc(sourceFile, outputFile, jobId());

         // route to either viewer or presentation pane (for reveal)
         if (isReveal)
         {
            std::string url = url_ports::mapUrlPorts(viewerUrl());
            if (isFileInSessionQuartoProject(previewFile_))
            {
               url = url + urlPathForQuartoProjectOutputFile(outputFile_);
            }

            json::Object eventData;
            eventData["url"] = url;
            eventData["quarto_navigation"] = module_context::quartoNavigateAsJson(quartoNav);
            eventData["editor_state"] = editorState_;
            eventData["slide_level"] = slideLevel_;
            ClientEvent event(client_events::kPresentationPreview, eventData);
            module_context::enqueClientEvent(event);
         }
         else
         {
            std::string url = viewerUrl();

            if (outputFile_.getExtensionLowerCase() != ".pdf")
            {
               if (isFileInSessionQuartoProject(previewFile_))
                  url = url + urlPathForQuartoProjectOutputFile(outputFile_);
            }

            module_context::viewer(url,  minHeight, quartoNav);
         }
      }
      else if (viewerType_ == kRmdViewerTypeWindow)
      {
         std::string url = rstudioServerPreviewWindowUrl();
         ClientEvent event = browseUrlEvent(url);
         module_context::enqueClientEvent(event);
      }
   }

   std::string rstudioServerPreviewWindowUrl()
   {
      std::string url = url_ports::mapUrlPorts(viewerUrl());
      if (isFileInSessionQuartoProject(previewFile_))
      {
         url = url + urlPathForQuartoProjectOutputFile(outputFile_);
      }
      return url;
   }

   std::string viewerUrl()
   {
      return "http://localhost:" + safe_convert::numberToString(port_) + "/" + path_;
   }

   void readInputFileLines()
   {
      Error error = core::readLinesFromFile(previewFile_, &previewFileLines_);
      if (error)
         LOG_ERROR(error);
   }

   int quartoSlideLevelFromOutput(const std::string& output)
   {
      boost::regex slideLevelRe("\n\\s+slide-level:\\s+(\\d)+\n");
      boost::smatch match;
      if (regex_utils::search(output, match, slideLevelRe))
      {
         return safe_convert::stringTo<int>(match[1], -1);
      }
      else
      {
         return -1;
      }
   }

private:
   FilePath previewFile_;
   FileInfo projectFile_;
   std::vector<std::string> previewFileLines_;
   FilePath outputFile_;
   std::string allOutput_;
   std::string format_;
   json::Value editorState_;
   int slideLevel_;
   int port_;
   std::string path_;
   std::string viewerType_;
};

// preview singleton
boost::shared_ptr<QuartoPreview> s_pPreview;

// stop any running preview
void stopPreview()
{
   if (s_pPreview)
   {
      // stop the job if it's running
      if (s_pPreview->isRunning())
         s_pPreview->stop();

      // remove the job (will be replaced by a new quarto serve)
      s_pPreview->remove();
   }
}

// create a preview job
Error createPreview(const FilePath& previewFilePath, const std::string& format, const json::Value& editorState)
{
   // stop any running preview
   stopPreview();

   Error error = QuartoPreview::create(previewFilePath, format, editorState, &s_pPreview);
   if (error)
      return error;
   return Success();
}


Error quartoPreviewRpc(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   json::Value editorState;
   std::string previewFile, format;
   Error error = json::readParams(request.params, &previewFile, &format, &editorState);
   if (error)
      return error;
   FilePath previewFilePath = module_context::resolveAliasedPath(previewFile);

   // first check to see if this file is in a book project (if so then fail and fall
   // back on normal render)
   bool canPreview = true;
   FilePath quartoConfig = session::quarto::quartoProjectConfigFile(previewFilePath);
   if (!quartoConfig.isEmpty())
   {
      std::string type;
      readQuartoProjectConfig(quartoConfig, &type);
      canPreview = type != session::quarto::kQuartoProjectBook;
   }

   // set result
   pResponse->setResult(canPreview);

   if (canPreview)
   {
      if (s_pPreview && s_pPreview->isRunning() && (s_pPreview->port() > 0) &&
          (s_pPreview->previewFile() == previewFilePath) &&
          (s_pPreview->format() == format && !s_pPreview->hasModifiedProject()) &&
          (s_pPreview->viewerType() == prefs::userPrefs().rmdViewerType()))
      {
         json::Object eventJson;
         eventJson["id"] = s_pPreview->jobId();
         module_context::enqueClientEvent(ClientEvent(client_events::kJobsActivate, eventJson));
         return s_pPreview->render(editorState);
      }
      else
      {
         return createPreview(previewFilePath, format, editorState);
      }
   }
   else
   {
      return Success();
   }
}

void onSourceDocRemoved(const std::string& id, const std::string& path)
{
   // resolve source database path
   FilePath resolvedPath = module_context::resolveAliasedPath(path);

   // if this is our active preview then terminate it
   if (s_pPreview && s_pPreview->isRunning() &&
       (s_pPreview->previewFile() == resolvedPath))
   {
      stopPreview();
   }

}

void onAllSourceDocsRemoved()
{
   stopPreview();
}


} // anonymous namespace



Error initialize()
{
   source_database::events().onDocRemoved.connect(onSourceDocRemoved);
   source_database::events().onRemoveAll.connect(onAllSourceDocsRemoved);


   // register rpc functions
  ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_preview", quartoPreviewRpc))
   ;
   return initBlock.execute();
}

} // namespace preview
} // namespace quarto
} // namespace modules

namespace module_context {

json::Value quartoNavigateAsJson(const QuartoNavigate& quartoNavigate)
{
   json::Value quartoNav;
   if (!quartoNavigate.empty())
   {
      json::Object quartoNavObj;
      quartoNavObj["is_website"] = quartoNavigate.website;
      quartoNavObj["source_file"] = quartoNavigate.source;
      quartoNavObj["output_file"] = quartoNavigate.output;
      quartoNavObj["job_id"] = quartoNavigate.job_id;
      quartoNav = quartoNavObj;
   }
   return quartoNav;
}


} // namespace module_context

} // namespace session
} // namespace rstudio
