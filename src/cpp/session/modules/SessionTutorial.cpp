/*
 * SessionTutorial.cpp
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

#include "SessionTutorial.hpp"

#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>
#include <core/markdown/Markdown.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RSexp.hpp>

#include <session/projects/SessionProjects.hpp>

#include <session/SessionPackageProvidedExtension.hpp>
#include <session/SessionModuleContext.hpp>

const char * const kTutorialLocation = "/tutorial";

const char * const kTutorialClientEventIndexingCompleted = "indexing_completed";

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace tutorial {

namespace {

FilePath tutorialResourcePath(const std::string& fileName = std::string())
{
   return module_context::scopedScratchPath()
         .completePath("tutorial/" + fileName);
}

struct TutorialInfo
{
   std::string name;
   std::string file;
   std::string title;
   std::string description;
};

using TutorialIndex = std::map<std::string, std::vector<TutorialInfo>>;

TutorialIndex& tutorialIndex()
{
   static TutorialIndex instance;
   return instance;
}

void enqueueTutorialClientEvent(const std::string& type,
                                const json::Value& data)
{
   using namespace module_context;
   
   json::Object eventJson;
   eventJson["type"] = type;
   eventJson["data"] = data;
   
   ClientEvent event(client_events::kTutorialCommand, eventJson);
   enqueClientEvent(event);
}

class TutorialWorker : public ppe::Worker
{
private:
   void onIndexingStarted() override
   {
      index_.clear();
   }
   
   void onIndexingCompleted(core::json::Object* pPayload) override
   {
      tutorialIndex() = index_;
      
      enqueueTutorialClientEvent(
               kTutorialClientEventIndexingCompleted,
               json::Value());
   }
   
   void onWork(const std::string& pkgName,
               const core::FilePath& resourcePath) override
   {
      r::sexp::Protect protect;
      SEXP tutorialsSEXP;
      
      Error error = r::exec::RFunction(".rs.tutorial.findTutorials")
            .addParam(resourcePath.getAbsolutePath())
            .call(&tutorialsSEXP, &protect);
      
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      json::Value tutorialsJson;
      error = r::json::jsonValueFromList(tutorialsSEXP, &tutorialsJson);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      if (!tutorialsJson.isArray())
      {
         Error error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
         LOG_ERROR(error);
      }
      
      for (auto tutorialJson : tutorialsJson.getArray())
      {
         if (!tutorialJson.isObject())
         {
            Error error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
            LOG_ERROR(error);
         }
         
         TutorialInfo info;
         error = json::readObject(tutorialJson.getObject(),
                                  "name",        info.name,
                                  "file",        info.file,
                                  "title",       info.title,
                                  "description", info.description);

         if (error)
         {
            LOG_ERROR(error);
            return;
         }

         index_[pkgName].push_back(info);
         
      }
   }
   
private:
   TutorialIndex index_;
};

boost::shared_ptr<TutorialWorker>& tutorialWorker()
{
   static boost::shared_ptr<TutorialWorker> instance(new TutorialWorker);
   return instance;
}

FilePath resourcesPath()
{
   return options().rResourcesPath().completePath("tutorial_resources");
}

void handleTutorialRunRequest(const http::Request& request,
                              http::Response* pResponse)
{
   std::string package = request.queryParamValue("package");
   std::string name = request.queryParamValue("name");
   
   Error error = r::exec::RFunction(".rs.tutorial.runTutorial")
         .addParam(name)
         .addParam(package)
         .call();
   
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setStatusCode(http::status::NotFound);
      return;
   }
   
   FilePath loadingPath = resourcesPath().completePath("loading.html");
   pResponse->setFile(loadingPath, request);
}

void handleTutorialHomeRequest(const http::Request& request,
                               http::Response* pResponse)
{
   using namespace string_utils;
   
   std::stringstream ss;
   
   if (tutorialIndex().empty())
   {
      ss << "<div class=\"rstudio-tutorials-section\">";
      
      if (!module_context::isPackageInstalled("learnr"))
      {
         std::stringstream clickHere;
         clickHere << "<a"
                   << " aria-label\"Install learnr\""
                   << " class=\"rstudio-tutorials-install-learnr-link\""
                   << " href=\"javascript:void(0)\""
                   << " onclick=\"window.parent.tutorialInstallLearnr(); return false;\">"
                   << "click here"
                   << "</a>";
         
         ss << "<p>The <code>learnr</code> package is required to run tutorials for RStudio.</p>"
            << "<p>Please " << clickHere.str() << " to install the <code>learnr</code> package.</p>";
      }
      else
      {
         ss << "<p>Please wait while RStudio finishes indexing available tutorials...</p>";
      }
      
      ss << "</div>";
   }
   
   for (auto entry : tutorialIndex())
   {
      std::string pkgName = entry.first;
      std::vector<TutorialInfo> tutorials = entry.second;
      if (tutorials.empty())
         continue;

      for (auto tutorial : tutorials)
      {
         ss << "<div class=\"rstudio-tutorials-section\">";
         
         ss << "<div class=\"rstudio-tutorials-label-container\">";
         
         std::string title = (tutorial.title.empty())
               ? "[Untitled tutorial]"
               : htmlEscape(tutorial.title);
         
         ss << "<span role=\"heading\" aria-level=\"2\" class=\"rstudio-tutorials-label\">"
            << title
            << "</span>";
         
         ss << "<span class=\"rstudio-tutorials-run-container\">"
            
            << "<button"
            << " class=\"rstudio-tutorials-run-button\""
            << " aria-label=\"Start tutorial '" << htmlEscape(tutorial.name, true) << "' from package '" << htmlEscape(pkgName, true) << "'\""
            << " onclick=\"window.parent.tutorialRun('" << htmlEscape(tutorial.name, true) << "', '" << htmlEscape(pkgName, true) << "')\""
            << ">"
               
            << "<span class=\"rstudio-tutorials-run-button-label\">Start Tutorial</span>"
            << "<span class=\"rstudio-tutorials-run-button-icon\">&#x25b6</span>"
            << "</button>"
            << "</span>";
         
         ss << "</div>";
         
         ss << "<div class=\"rstudio-tutorials-sublabel\">"
            << pkgName << ": " << htmlEscape(tutorial.name)
            << "</div>";
         
         if (tutorial.description.empty())
         {
            ss << "<div class=\"rstudio-tutorials-description rstudio-tutorials-description-empty\">"
               << "<p>[No description available.]</p>"
               << "</div>";
         }
         else
         {
            std::string descriptionHtml;
            Error error = core::markdown::markdownToHTML(
                     tutorial.description,
                     core::markdown::Extensions(),
                     core::markdown::HTMLOptions(),
                     &descriptionHtml);
            
            if (error)
            {
               LOG_ERROR(error);
               descriptionHtml = tutorial.description;
            }
            
            ss << "<div class=\"rstudio-tutorials-description\">"
               << descriptionHtml
               << "</div>";
         }
         
         ss << "</div>";
         
      }
      
   }
   
   std::map<std::string, std::string> vars;
   
   std::string tutorialsHtml = ss.str();
   vars["tutorials"] = tutorialsHtml;
   
   FilePath homePath = resourcesPath().completePath("index.html");
   pResponse->setFile(homePath, request, text::TemplateFilter(vars));
}

void handleTutorialFileRequest(const http::Request& request,
                               http::Response* pResponse)
{
   FilePath resourcesPath =
         options().rResourcesPath().completePath("tutorial_resources");
   
   std::string path = http::util::pathAfterPrefix(request, "/tutorial/");
   if (path.empty())
   {
      pResponse->setStatusCode(http::status::NotFound);
      return;
   }
   
   pResponse->setCacheableFile(resourcesPath.completePath(path), request);
}

void handleTutorialRequest(const http::Request& request,
                           http::Response* pResponse)
{
   std::string path = http::util::pathAfterPrefix(request, kTutorialLocation);
   if (path == "/run")
      handleTutorialRunRequest(request, pResponse);
   else if (path == "/home")
      handleTutorialHomeRequest(request, pResponse);
   else if (boost::algorithm::ends_with(path, ".png"))
      handleTutorialFileRequest(request, pResponse);
   else
   {
      LOG_ERROR_MESSAGE("Unhandled tutorial URI '" + path + "'");
      pResponse->setStatusCode(http::status::NotFound);
   }
}

void onDeferredInit(bool newSession)
{
   static bool s_launched = false;
   if (s_launched)
      return;
   
   using namespace projects;
   if (!projectContext().hasProject())
      return;
   
   std::string defaultTutorial = projectContext().config().defaultTutorial;
   if (defaultTutorial.empty())
      return;
   
   std::vector<std::string> parts = core::algorithm::split(defaultTutorial, "::");
   if (parts.size() != 2)
   {
      LOG_WARNING_MESSAGE("Unexpected DefaultTutorial field: " + defaultTutorial);
      return;
   }
   
   json::Object data;
   data["package"] = parts[0];
   data["name"] = parts[1];
   
   ClientEvent event(client_events::kTutorialLaunch, data);
   module_context::enqueClientEvent(event);
   
   s_launched = true;
}

void onSuspend(const r::session::RSuspendOptions& suspendOptions,
               Settings* pSettings)
{
   Error error = r::exec::RFunction(".rs.tutorial.onSuspend")
         .addParam(tutorialResourcePath("tutorial-state").getAbsolutePath())
         .call();
   
   if (error)
      LOG_ERROR(error);
}

void onResume(const Settings& settings)
{
   Error error = r::exec::RFunction(".rs.tutorial.onResume")
         .addParam(tutorialResourcePath("tutorial-state").getAbsolutePath())
         .call();
   
   if (error)
      LOG_ERROR(error);
}
   

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;

   ppe::indexer().addWorker(tutorialWorker());
 
   events().onDeferredInit.connect(onDeferredInit);
   
   addSuspendHandler(SuspendHandler(onSuspend, onResume));
   
   Error error = tutorialResourcePath().ensureDirectory();
   if (error)
      LOG_ERROR(error);
   
   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(registerUriHandler, kTutorialLocation, handleTutorialRequest))
         (bind(sourceModuleRFile, "SessionTutorial.R"));

   return initBlock.execute();
}

} // end namespace tutorial
} // end namespace modules
} // end namespace session
} // end namespace rstudio
