/*
 * SessionLearning.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


#include "SessionLearning.hpp"

#include <iostream>

#include <boost/utility.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Exec.hpp>
#include <core/markdown/Markdown.hpp>
#include <core/text/TemplateFilter.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

#include "LearningState.hpp"

#include "SlideParser.hpp"
#include "SlideRenderer.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {

FilePath learningResourcesPath()
{
   return session::options().rResourcesPath().complete("learning");
}

SEXP rs_showLearningPane(SEXP dirSEXP)
{
   try
   {
      if (session::options().programMode() == kSessionProgramModeServer)
      {
         // validate path
         FilePath dir(r::sexp::asString(dirSEXP));
         if (!dir.exists())
            throw r::exec::RErrorException("Directory " + dir.absolutePath() +
                                           " does not exist.");

         // initialize learning state
         learning::state::init(dir);

         // notify the client
         ClientEvent event(client_events::kShowLearningPane,
                           learning::state::asJson());
         module_context::enqueClientEvent(event);
      }
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

core::Error setLearningSlideIndex(const json::JsonRpcRequest& request,
                                  json::JsonRpcResponse*)
{
   int index;
   Error error = json::readParam(request.params, 0, &index);
   if (error)
      return error;

   learning::state::setSlideIndex(index);

   return Success();
}

core::Error closeLearningPane(const json::JsonRpcRequest&,
                              json::JsonRpcResponse*)
{
   learning::state::clear();

   return Success();
}

class ResourceFiles : boost::noncopyable
{
public:
   const std::string& get(const std::string& path)
   {
      std::map<std::string, std::string>::const_iterator it =
                                                      cache_.find(path);
      if (it == cache_.end())
         cache_[path] = module_context::resourceFileAsString(path);

      return cache_[path];
   }

private:
   friend ResourceFiles& resourceFiles();
   std::map<std::string, std::string> cache_;
};

ResourceFiles& resourceFiles()
{
   static ResourceFiles instance;
   return instance;
}


std::string mathjaxIfRequired(const std::string& contents)
{
   if (markdown::isMathJaxRequired(contents))
      return resourceFiles().get("mathjax.html");
   else
      return std::string();
}

void handleLearningContentRequest(const http::Request& request,
                                  http::Response* pResponse)
{
   // return not found if learning isn't active
   if (!learning::state::isActive())
   {
      pResponse->setError(http::status::NotFound, request.uri() + " not found");
      return;
   }

   // get the requested path
   std::string path = http::util::pathAfterPrefix(request, "/learning/");

   // special handling for the root (process learning template)
   if (path.empty())
   {
      // look for slides.md
      FilePath slidesFile = learning::state::directory().complete("slides.md");
      if (!slidesFile.exists())
      {
         pResponse->setError(http::status::NotFound,
                             "slides.md file not found in " +
                             learning::state::directory().absolutePath());
         return;
      }

      // parse the slides
      std::string errMsg;
      learning::SlideDeck slideDeck;
      Error error = slideDeck.readSlides(slidesFile, &errMsg);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setError(http::status::InternalServerError, errMsg);
         return;
      }

      // render the slides
      std::string slides;
      error = learning::renderSlides(slideDeck, &slides, &errMsg);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setError(http::status::InternalServerError, errMsg);
         return;
      }

      // build template variables
      std::map<std::string,std::string> vars;
      vars["title"] = slideDeck.title();
      vars["slides"] = slides;
      vars["styles"] =  resourceFiles().get("learning/slides.css");
      vars["r_highlight"] = resourceFiles().get("r_highlight.html");
      vars["mathjax"] = mathjaxIfRequired(slides);

      // process the template
      pResponse->setNoCacheHeaders();
      pResponse->setBody(resourceFiles().get("learning/slides.html"),
                         text::TemplateFilter(vars));
   }

   // special handling for reveal.js assets
   else if (boost::algorithm::starts_with(path, "revealjs/"))
   {
      path = http::util::pathAfterPrefix(request, "/learning/revealjs/");
      FilePath filePath = learningResourcesPath().complete("revealjs/" + path);
      pResponse->setFile(filePath, request);
   }

   // just serve the file back
   else
   {
      pResponse->setFile(learning::state::directory().childPath(path),
                         request);
   }
}

} // anonymous namespace

json::Value learningStateAsJson()
{
   return learning::state::asJson();
}

Error initialize()
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      // register rs_showLearningPane
      R_CallMethodDef methodDefShowLearningPane;
      methodDefShowLearningPane.name = "rs_showLearningPane" ;
      methodDefShowLearningPane.fun = (DL_FUNC) rs_showLearningPane;
      methodDefShowLearningPane.numArgs = 1;
      r::routines::addCallMethod(methodDefShowLearningPane);

      using boost::bind;
      using namespace session::module_context;
      ExecBlock initBlock ;
      initBlock.addFunctions()
         (bind(registerUriHandler, "/learning", handleLearningContentRequest))
         (bind(registerRpcMethod, "set_learning_slide_index", setLearningSlideIndex))
         (bind(registerRpcMethod, "close_learning_pane", closeLearningPane))
         (bind(learning::state::initialize))
         (bind(sourceModuleRFile, "SessionLearning.R"));

      return initBlock.execute();
   }
   else
   {
      return Success();
   }
}

} // namespace learning
} // namespace modules
} // namesapce session

