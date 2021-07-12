/*
 * SessionQuartoPreview.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "SessionQuartoPreview.hpp"

#include <string>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/RegexUtils.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionQuartoJob.hpp"

using namespace rstudio::core;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {
namespace preview {

namespace {

class QuartoPreview : public QuartoJob

{
public:
   static Error create(const core::FilePath& previewFile,
                       boost::shared_ptr<QuartoPreview>* ppPreview)
   {
      ppPreview->reset(new QuartoPreview(previewFile));
      return (*ppPreview)->start();
   }

   virtual ~QuartoPreview()
   {
   }

   FilePath previewFile()
   {
      return previewFile_;
   }

   long port()
   {
      return port_;
   }

   Error render()
   {
      return r::exec::RFunction(".rs.quarto.renderPreview",
                                safe_convert::numberToString(port())).call();
   }

protected:
   explicit QuartoPreview(const FilePath& previewFile)
      : QuartoJob(), previewFile_(previewFile), port_(0)
   {
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

      // no automatic render and no browser
      args.push_back("--no-render");
      args.push_back("--no-browse");

      return args;
   }

   virtual core::FilePath workingDir()
   {
      return previewFile_.getParent();
   }


private:

   virtual void onStdErr(const std::string& output)
   {
      // detect browse directive
      if (port_ == 0) {
         auto location = quartoServerLocationFromOutput(output);
         if (location.port > 0)
         {
            // save port and path
            port_ = location.port;
            path_ = location.path;

            // show in viewer
            showInViewer();

            // restore the console tab after render
            activateConsole();

            // emit filtered output if we are on rstudio server
            if (session::options().programMode() == kSessionProgramModeServer)
            {
               QuartoJob::onStdErr(location.filteredOutput);
            }
            else
            {
               QuartoJob::onStdErr(output);
            }
            return;
         }
      }

      if (output.find("Watching files for changes") != std::string::npos)
      {
         // activate the console
         activateConsole();

         // if the viewer is already on the site just activate it
         if (boost::algorithm::starts_with(
                module_context::viewerCurrentUrl(false), viewerUrl()))
         {
            module_context::activatePane("viewer");
         }
         else
         {
            showInViewer();
         }
      }

      // standard output forwarding
      QuartoJob::onStdErr(output);
   }

   void activateConsole()
   {
      ClientEvent activateConsoleEvent(client_events::kConsoleActivate, false);
      module_context::enqueClientEvent(activateConsoleEvent);
   }

   void showInViewer()
   {
      module_context::viewer(viewerUrl(), false,  -1);
   }

   std::string viewerUrl()
   {
      return "http://localhost:" + safe_convert::numberToString(port_) + "/" + path_;
   }

private:
   FilePath previewFile_;
   long port_;
   std::string path_;
};


// keep a list of previews so we can re-render, re-activate
std::vector<boost::shared_ptr<QuartoPreview>> s_previews;



Error quartoPreviewRpc(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse*)
{
   // read params
   std::string previewFile;
   Error error = json::readParams(request.params, &previewFile);
   if (error)
      return error;
   FilePath previewFilePath = module_context::resolveAliasedPath(previewFile);

   // see if there is an existing preview we should re-render
   auto preview = std::find_if(s_previews.begin(), s_previews.end(),
                               [&previewFilePath](boost::shared_ptr<QuartoPreview> pPreview) {
      return (pPreview->previewFile() == previewFilePath) && pPreview->isRunning();
   });
   if (preview != s_previews.end())
   {
      module_context::enqueClientEvent(ClientEvent(client_events::kJobsActivate));
      return (*preview)->render();
   }
   else
   {
      boost::shared_ptr<QuartoPreview> pPreview;
      error = QuartoPreview::create(previewFilePath, &pPreview);
      if (error)
         return error;
      s_previews.push_back(pPreview);
      return Success();
   }


}


} // anonymous namespace



Error initialize()
{
   // register rpc functions
  ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_preview", quartoPreviewRpc))
   ;
   return initBlock.execute();
}

} // namespace prevew
} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
