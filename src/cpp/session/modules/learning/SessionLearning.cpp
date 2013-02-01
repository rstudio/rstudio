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
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/HtmlUtils.hpp>
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

SEXP rs_showLearningPane(SEXP paneCaptionSEXP, SEXP dirSEXP)
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
         learning::state::init(r::sexp::asString(paneCaptionSEXP), dir);

         // notify the client
         ClientEvent event(client_events::kShowLearningPane,
                           learning::state::asJson());
         module_context::enqueClientEvent(event);
      }
      else
      {
         throw r::exec::RErrorException("Learning pane is not supported "
                                        "in desktop mode.");
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
private:
   ResourceFiles() {}

public:
   std::string get(const std::string& path)
   {
      return module_context::resourceFileAsString(path);
   }

private:
   friend ResourceFiles& resourceFiles();
};

ResourceFiles& resourceFiles()
{
   static ResourceFiles instance;
   return instance;
}


std::string mathjaxIfRequired(const std::string& contents)
{
   if (markdown::isMathJaxRequired(contents))
      return resourceFiles().get("learning/mathjax.html");
   else
      return std::string();
}

void handleRangeRequest(const FilePath& targetFile,
                        const http::Request& request,
                        http::Response* pResponse)
{
   // read the file in from disk
   std::string contents;
   Error error = core::readStringFromFile(targetFile, &contents);
   if (error)
      pResponse->setError(error);

   // set content type
   pResponse->setContentType(targetFile.mimeContentType());

   // parse the range field
   std::string range = request.headerValue("Range");
   boost::regex re("bytes=(\\d*)\\-(\\d*)");
   boost::smatch match;
   if (boost::regex_match(range, match, re))
   {
      // specify partial content
      pResponse->setStatusCode(http::status::PartialContent);

      // determine the byte range
      const size_t kNone = -1;
      size_t begin = safe_convert::stringTo<size_t>(match[1], kNone);
      size_t end = safe_convert::stringTo<size_t>(match[2], kNone);
      size_t total = contents.length();

      if (end == kNone)
      {
         end = total-1;
      }
      if (begin == kNone)
      {
         begin = total - end;
         end = total-1;
      }

      // set the byte range
      pResponse->addHeader("Accept-Ranges", "bytes");
      boost::format fmt("bytes %1%-%2%/%3%");
      std::string range = boost::str(fmt % begin % end % contents.length());
      pResponse->addHeader("Content-Range", range);

      // always attempt gzip
      if (request.acceptsEncoding(http::kGzipEncoding))
         pResponse->setContentEncoding(http::kGzipEncoding);

      // set body
      if (begin == 0 && end == (contents.length()-1))
         pResponse->setBody(contents);
      else
         pResponse->setBody(contents.substr(begin, end-begin));
   }
   else
   {
      pResponse->setStatusCode(http::status::RangeNotSatisfiable);
      boost::format fmt("bytes */%1%");
      std::string range = boost::str(fmt % contents.length());
      pResponse->addHeader("Content-Range", range);
   }
}

void handleLearningPaneRequest(const http::Request& request,
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
      learning::SlideDeck slideDeck;
      Error error = slideDeck.readSlides(slidesFile);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setError(http::status::InternalServerError,
                             error.summary());
         return;
      }

      // render the slides
      std::string slides, revealConfig, initCommands, slideCommands;
      error = learning::renderSlides(slideDeck,
                                     &slides,
                                     &revealConfig,
                                     &initCommands,
                                     &slideCommands);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setError(http::status::InternalServerError,
                             error.summary());
         return;
      }

      // get user css if it exists
      std::string userSlidesCss;
      FilePath cssPath = learning::state::directory().complete("slides.css");
      if (cssPath.exists())
      {
         userSlidesCss = "<link rel=\"stylesheet\" href=\"slides.css\">\n";
      }

      // build template variables
      std::map<std::string,std::string> vars;
      vars["title"] = slideDeck.title();
      vars["user_slides_css"] = userSlidesCss;
      vars["preamble"] = slideDeck.preamble();
      vars["slides"] = slides;
      vars["slide_commands"] = slideCommands;
      vars["slides_css"] =  resourceFiles().get("learning/slides.css");
      vars["r_highlight"] = resourceFiles().get("r_highlight.html");
      vars["mathjax"] = mathjaxIfRequired(slides);
      vars["slides_js"] = resourceFiles().get("learning/slides.js");
      vars["reveal_config"] = revealConfig;
      vars["init_commands"] = initCommands;

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

   // special handling for mathjax assets
   else if (boost::algorithm::starts_with(path, "mathjax/"))
   {
      FilePath filePath =
            session::options().mathjaxPath().parent().childPath(path);
      pResponse->setFile(filePath, request);
   }


   // serve the file back
   else
   {
      FilePath targetFile = learning::state::directory().childPath(path);
      if (!request.headerValue("Range").empty())
      {
         handleRangeRequest(targetFile, request, pResponse);
      }
      else
      {
         // indicate that we accept byte range requests
         pResponse->addHeader("Accept-Ranges", "bytes");

         // return the file
         pResponse->setFile(targetFile, request);
      }
   }
}


// we save the most recent /help/learning/&file=parameter so we
// can resolve relative file references against it. we do this
// separately from learning::state::directory so that the help
// urls can be available within the help pane (and history)
// independent of the duration of the learning tab
FilePath s_learningHelpDir;


} // anonymous namespace

void handleLearningHelpRequest(const core::http::Request& request,
                               const std::string& jsCallbacks,
                               core::http::Response* pResponse)
{
   // check if this is a root request
   std::string file = request.queryParamValue("file");
   if (!file.empty())
   {
      // ensure file exists
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (!filePath.exists())
      {
         pResponse->setError(http::status::NotFound, request.uri());
         return;
      }

      // save the file's directory (for resolving other resources)
      s_learningHelpDir = filePath.parent();


      // read in the file (process markdown)
      std::string helpDoc;
      Error error = markdown::markdownToHTML(filePath,
                                             markdown::Extensions(),
                                             markdown::HTMLOptions(),
                                             &helpDoc);
      if (error)
      {
         pResponse->setError(error);
         return;
      }

      // process the template
      std::map<std::string,std::string> vars;
      vars["title"] = html_utils::defaultTitle(helpDoc);
      vars["styles"] = resourceFiles().get("learning/helpdoc.css");
      vars["r_highlight"] = resourceFiles().get("r_highlight.html");
      vars["mathjax"] = mathjaxIfRequired(helpDoc);
      vars["content"] = helpDoc;
      vars["js_callbacks"] = jsCallbacks;
      pResponse->setNoCacheHeaders();
      pResponse->setBody(resourceFiles().get("learning/helpdoc.html"),
                         text::TemplateFilter(vars));
   }

   // it's a relative file reference
   else
   {
      // make sure the directory exists
      if (!s_learningHelpDir.exists())
      {
         pResponse->setError(http::status::NotFound,
                             "Directory not found: " +
                             s_learningHelpDir.absolutePath());
         return;
      }

      // resolve the file reference
      std::string path = http::util::pathAfterPrefix(request,
                                                     "/help/learning/");

      // serve the file back
      pResponse->setFile(s_learningHelpDir.complete(path), request);
   }
}



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
      methodDefShowLearningPane.numArgs = 2;
      r::routines::addCallMethod(methodDefShowLearningPane);

      using boost::bind;
      using namespace session::module_context;
      ExecBlock initBlock ;
      initBlock.addFunctions()
         (bind(registerUriHandler, "/learning", handleLearningPaneRequest))
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

