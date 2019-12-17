/*
 * SessionTutorial.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
#include <core/text/TemplateFilter.hpp>
#include <core/YamlUtil.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/RSexp.hpp>

#include <session/SessionPackageProvidedExtension.hpp>
#include <session/SessionModuleContext.hpp>

const char * const kTutorialLocation = "/tutorial";

const char * const kTutorialClientEventStarted = "started";
const char * const kTutorialClientEventIndexingCompleted = "indexing_completed";

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace tutorial {

namespace {

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
                                  "name",        &info.name,
                                  "file",        &info.file,
                                  "title",       &info.title,
                                  "description", &info.description);

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

std::string htmlFormatTutorialName(const std::string& packageName,
                                   const TutorialInfo& tutorial)
{
   using namespace string_utils;
   
   std::stringstream href;
   href << "/tutorial/run"
        << "?package=" + htmlEscape(packageName)
        << "&name=" + htmlEscape(tutorial.name);
   
   std::stringstream ss;
   ss << "<a class=\"rstudio-tutorials-link\" href=\"" << href.str() << "\">"
      << "<code>" << htmlEscape(tutorial.name) << "</code>"
      << "</a>";
   
   return ss.str();
}

void handleTutorialRunRequest(const http::Request& request,
                              http::Response* pResponse)
{
   std::string package = request.queryParamValue("package");
   std::string name = request.queryParamValue("name");
   
   json::Object event;
   event["package"] = package;
   event["name"] = name;
   enqueueTutorialClientEvent(kTutorialClientEventStarted, event);
   
   // TODO: Check tutorial pre-requisites first!
   Error error = r::exec::RFunction(".rs.tutorial.runTutorial")
         .addParam(name)
         .addParam(package)
         .call();
   
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setStatusCode(http::status::NotFound);
   }
   
   pResponse->setStatusCode(http::status::Ok);
}

void handleTutorialHomeRequest(const http::Request& request,
                               http::Response* pResponse)
{
   using namespace string_utils;
   
   std::stringstream ss;
   
   if (tutorialIndex().empty())
   {
      ss << "<p>No tutorials are currently available.</p>";
      if (!module_context::isPackageInstalled("learnr"))
      {
         ss << "<p>Please install the learnr package to enable tutorials for RStudio.</p>";
      }
      else
      {
         ss << "<p>Please wait while RStudio finishes indexing...</p>";
      }
   }
   
   for (auto entry : tutorialIndex())
   {
      std::string pkgName = entry.first;
      std::vector<TutorialInfo> tutorials = entry.second;
      if (tutorials.empty())
         continue;

      ss << "<h2 class=\"rstudio-tutorials-package\">" << htmlFormatTitle(pkgName) << "</h2>";
      ss << "<hr class=\"rstudio-tutorials-separator\">";

      for (auto tutorial : tutorials)
      {
         ss << "<div>";
         
         ss << "<div class=\"rstudio-tutorials-label-container\">";
         
         ss << "<span class=\"rstudio-tutorials-label\">"
            << htmlEscape(tutorial.title)
            << "</span>";
         
         ss << "<span class=\"rstudio-tutorials-name\">"
            << htmlFormatTutorialName(pkgName, tutorial)
            << "</span>";
         
         ss << "</div>";
         
         if (tutorial.description.empty())
         {
            ss << "<div class=\"rstudio-tutorials-description rstudio-tutorials-description-empty\">"
               << "[No description available.]"
               << "</div>";
         }
         else
         {
            ss << "<div class=\"rstudio-tutorials-description\">"
               << tutorial.description
               << "</div>";
         }
         
         ss << "</div>";
         ss << "<hr class=\"rstudio-tutorials-separator\">";
      }
   }
   
   std::map<std::string, std::string> vars;
   
   std::string tutorialsHtml = ss.str();
   vars["tutorials"] = tutorialsHtml;
   
   FilePath homePath = resourcesPath().completePath("index.html");
   pResponse->setFile(homePath, request, text::TemplateFilter(vars));
}

void handleTutorialRequest(const http::Request& request,
                           http::Response* pResponse)
{
   std::string path = http::util::pathAfterPrefix(request, kTutorialLocation);
   if (path == "/run")
      handleTutorialRunRequest(request, pResponse);
   else if (path == "/home")
      handleTutorialHomeRequest(request, pResponse);
   else
   {
      LOG_ERROR_MESSAGE("Unhandled tutorial URI '" + path + "'");
      pResponse->setStatusCode(http::status::NotFound);
   }
}
   

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;

   ppe::indexer().addWorker(tutorialWorker());
   
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
