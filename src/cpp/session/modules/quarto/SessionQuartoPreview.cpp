   /*
 * SessionQuartoPreview.cpp
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

#include "SessionQuartoPreview.hpp"

#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
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

// for 'quarto preview' jobs
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

   FilePath previewTarget()
   {
      return previewTarget_;
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

   int port()
   {
      return port_;
   }

   int controlPort()
   {
      if (controlPort_ > 0)
         return controlPort_;
      else
         return port();
   }

   std::string jobId()
   {
      return pJob_->id();
   }

   std::string viewerType()
   {
      return viewerType_;
   }

   bool render(const core::FilePath& previewfile,  std::string format, const json::Value& editorState)
   {
      // reset state
      previewTarget_ = previewfile;
      slideLevel_= -1;
      editorState_ = editorState;
      outputFile_ = FilePath();
      allOutput_.clear();

      // re-read input file
      readInputFileLines();

      // provide format default
      if (format.empty())
         format = "default";

      // render
      SEXP result;
      r::sexp::Protect rProtect;
      Error error = r::exec::RFunction(".rs.quarto.renderPreview",
         safe_convert::numberToString(controlPort()),
         renderToken_,
         previewTarget().getAbsolutePath(),
         format).call(&result, &rProtect);
      if (error || r::sexp::inherits(result, "error"))
      {
         return false;
      }
      else
      {
         return true;
      }
   }

protected:
   explicit QuartoPreview(const FilePath& previewFile, const std::string& format, const json::Value& editorState)
      : QuartoJob(), previewTarget_(previewFile), format_(format), editorState_(editorState),
                     slideLevel_(-1), port_(0), controlPort_(0), viewerType_(prefs::userPrefs().rmdViewerType())
   {
     renderToken_ = core::system::generateUuid();

     readInputFileLines();
   }

   virtual std::string name()
   {
      return "Preview: " + previewTarget_.getFilename();
   }
   
   virtual std::vector<std::string> args()
   {
      // preview target file
      std::vector<std::string> args = { "preview" };
      if (!previewTarget_.isDirectory())
      {
         args.push_back(string_utils::utf8ToSystem(previewTarget_.getFilename()));

         args.push_back("--to");
         args.push_back(!format_.empty() ? format_ : "default");
      }
      else
      {
         args.push_back("--render");
         args.push_back(!format_.empty() ? format_ : "default");
      }

      // presentation mode if this is reveal
      if (formatIsRevealJs())
         args.push_back("--presentation");

      // no watching inputs and no browser
      args.push_back("--no-watch-inputs");
      args.push_back("--no-browse");

      return args;
   }
   
   virtual void environment(core::system::Options* pEnv)
   {
      // make sure quarto and RStudio are using the same Python instance
      std::string quartoPython = core::system::getenv("QUARTO_PYTHON");
      if (quartoPython.empty())
      {
         std::string pythonPath;
         Error error = r::exec::RFunction(".rs.python.activeInterpreterPath").call(&pythonPath);
         if (error)
            LOG_ERROR(error);

         if (!pythonPath.empty())
            core::system::setenv(pEnv, "QUARTO_PYTHON", pythonPath);
      }

      // set render token
      core::system::setenv(pEnv, "QUARTO_RENDER_TOKEN", renderToken_);

      // if this file isn't in a project then add the QUARTO_CROSSREF_INDEX_PATH
      if (!isFileInSessionQuartoProject(previewTarget_))
      {
         FilePath indexPath;
         Error error = module_context::perFilePathStorage(
            kQuartoCrossrefScope, previewTarget_, false, &indexPath
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
      return previewDir();
   }

private:

   core::FilePath previewDir()
   {
     return previewTarget_.isDirectory() ? previewTarget_ :  previewTarget_.getParent();
   }

   virtual void onStdErr(const std::string& output)
   {
      bool isServer =  session::options().programMode() == kSessionProgramModeServer;

      // accumulate output (used for error scanning)
      allOutput_ += output;

      // always be looking for an output file
      FilePath outputFile =
         module_context::extractOutputFileCreated(previewDir(), output, false);
      if (!outputFile.isEmpty())
      {
         // capture output file
         outputFile_ = outputFile;

         // if we are running on rstudio server and there is a control port then
         // refresh the viewer manually (as whatever livereload scheme is in use
         // won't work via direct port connection)
         if (isServer && (controlPort_ > 0))
         {
           refreshViewer();
         }
      }

      // always be looking for slide-level
      int slideLevel = quartoSlideLevelFromOutput(output);
      if (slideLevel != -1)
      {
         slideLevel_ = slideLevel;
      }

      // always be looking for the control port
      int cPort = quartoControlPortFromOutput(output);
      if (cPort != -1)
      {
         controlPort_ = cPort;
      }

      // detect browse directive
      if (port_ == 0)
      {
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
            if (isServer)
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
      if (!previewTarget_.isDirectory())
         navigateToRenderPreviewError(previewTarget_, previewFileLines_, output, allOutput_);

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
         // get proj dir
         QuartoConfig config = quartoConfig();
         FilePath projDir = module_context::resolveAliasedPath(config.project_dir);

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

         std::string outputFile;
         if (!outputFile_.isEmpty())
            outputFile = module_context::createAliasedPath(outputFile_);
         
         QuartoNavigate quartoNav;
         std::string sourceFile = module_context::createAliasedPath(previewTarget_);
         if ((previewTarget()) == projDir || isFileInSessionQuartoProject((previewTarget())))
         {
            quartoNav = QuartoNavigate::navigate(
                     sourceFile,
                     "",
                     pJob_->id(),
                     true);
         }
         else if (!previewTarget_.isDirectory())
         {
           quartoNav = QuartoNavigate::navigate(
                    sourceFile,
                    outputFile,
                    jobId(),
                    false);
         }

         // route to either viewer or presentation pane (for reveal)
         if (isReveal)
         {
            std::string url = url_ports::mapUrlPorts(viewerUrl());

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

            // if we are dealing with a binary output file then make sure nav is for the file
            // not the project (as would occur for epub, docx in book output)
            if (quartoNav.website &&
                (outputFile_.getExtensionLowerCase() == ".docx" ||
                 outputFile_.getExtensionLowerCase() == ".epub"))
            {
               FilePath indexPath = previewDir().completeChildPath("index.qmd");
               std::string sourceFile = module_context::createAliasedPath(indexPath);
               quartoNav = QuartoNavigate::navigate(sourceFile, outputFile, jobId(), false);
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

   void refreshViewer()
   {
      module_context::scheduleDelayedWork(
         boost::posix_time::milliseconds(1000),
            []() {
               json::Object data;
               data["command"] = "viewerRefresh";
               data["quiet"] = false;
               ClientEvent event(client_events::kExecuteAppCommand, data);
               module_context::enqueClientEvent(event);
            },
         false);
   }

   std::string rstudioServerPreviewWindowUrl()
   {
      return url_ports::mapUrlPorts(viewerUrl());
   }

   std::string viewerUrl()
   {
      return "http://localhost:" + safe_convert::numberToString(port_) + "/" + path_;
   }

   void readInputFileLines()
   {
      if (!previewTarget_.isDirectory())
      {
         Error error = core::readLinesFromFile(previewTarget_, &previewFileLines_);
         if (error)
            LOG_ERROR(error);
      }
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

   int quartoControlPortFromOutput(const std::string& output)
   {
      boost::regex controlPortRe("Preview service running \\((\\d+)\\)");
      boost::smatch match;
      if (regex_utils::search(output, match, controlPortRe))
      {
         return safe_convert::stringTo<int>(match[1], -1);
      }
      else
      {
         return -1;
      }
   }

private:
   FilePath previewTarget_;
   std::vector<std::string> previewFileLines_;
   FilePath outputFile_;
   std::string allOutput_;
   std::string format_;
   std::string renderToken_;
   json::Value editorState_;
   int slideLevel_;
   int port_;
   int controlPort_;
   std::string path_;
   std::string viewerType_;
};

// preview singleton
boost::shared_ptr<QuartoPreview> s_pPreview;

// stop any running preview
bool stopPreview()
{
   if (s_pPreview)
   {
      // stop the job if it's running
      if (s_pPreview->isRunning())
      {
         // cooperative termination
         SEXP result;
         r::sexp::Protect rProtect;
         r::exec::RFunction(".rs.quarto.terminatePreview",
            safe_convert::numberToString(s_pPreview->controlPort())).call(&result, &rProtect);

         // job manager termination
         s_pPreview->stop();
      }

      // remove the job (will be replaced by a new quarto serve)
      s_pPreview->remove();

      return true;
   }
   else
   {
      return false;
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

   // we can always preview
   pResponse->setResult(true);

   // if this is a full project render then do that
   if (previewFilePath.isDirectory())
   {
      return createPreview(previewFilePath, format, editorState);
   }
   else
   {
      // see if there is a running preview w/ the same viewer type we can target
      if (s_pPreview && s_pPreview->isRunning() && (s_pPreview->port() > 0) &&
          (s_pPreview->viewerType() == prefs::userPrefs().rmdViewerType()))
      {
         json::Object eventJson;
         eventJson["id"] = s_pPreview->jobId();
         module_context::enqueClientEvent(ClientEvent(client_events::kJobsActivate, eventJson));
         // can we render in-place?
         if  (s_pPreview->render(previewFilePath, format, editorState))
         {
           return Success();
         }
         // create a new preview
         else
         {
           return createPreview(previewFilePath, format, editorState);
         }
      }
      // create a new preview
      else
      {
         return createPreview(previewFilePath, format, editorState);
      }
   }
}

void onSourceDocRemoved(const std::string&, const std::string& path)
{
   // resolve source database path
   FilePath resolvedPath = module_context::resolveAliasedPath(path);

   // if this is our active preview then terminate it
   if (s_pPreview && s_pPreview->isRunning() &&
       (s_pPreview->previewTarget() == resolvedPath))
   {
      stopPreview();
   }

}

void onAllSourceDocsRemoved()
{
   stopPreview();
}

#ifdef WIN32
void onQuit()
{
   stopPreview();
}

void onSuspend(Settings*)
{
   stopPreview();
}

void onResume(const Settings&)
{
}
#endif

} // anonymous namespace



Error initialize()
{
   source_database::events().onDocRemoved.connect(onSourceDocRemoved);
   source_database::events().onRemoveAll.connect(onAllSourceDocsRemoved);

#ifdef WIN32
   // Windows has issues with the Quarto background process running when the session shuts down
   // It requires that the Quarto process is terminated first
   module_context::events().onQuit.connect(onQuit);
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));
#endif

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
