/*
 * SessionPresentation.cpp
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


// TODO: custom code navigator for presentations

#include "SessionPresentation.hpp"


#include <boost/bind.hpp>

#include <core/Exec.hpp>
#include <core/http/Util.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/TemplateFilter.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "PresentationLog.hpp"
#include "PresentationState.hpp"
#include "SlideRequestHandler.hpp"
#include "SlideNavigationList.hpp"


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

namespace {


void showPresentation(const FilePath& filePath)
{
   // initialize state
   presentation::state::init(filePath);

   // notify the client
   ClientEvent event(client_events::kShowPresentationPane,
                     presentation::state::asJson());
   module_context::enqueClientEvent(event);
}

SEXP rs_showPresentation(SEXP fileSEXP)
{
   try
   {
      // validate path
      FilePath filePath(r::sexp::asString(fileSEXP));
      if (!filePath.exists())
         throw r::exec::RErrorException("File path " + filePath.absolutePath() +
                                        " does not exist.");

      showPresentation(filePath);
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

SEXP rs_showPresentationHelpDoc(SEXP helpDocSEXP)
{
   try
   {
      // verify a presentation is active
      if (!presentation::state::isActive())
      {
         throw r::exec::RErrorException(
                                 "No presentation is currently active");
      }

      // resolve against presentation directory
      std::string helpDoc = r::sexp::asString(helpDocSEXP);
      FilePath helpDocPath = presentation::state::directory().childPath(
                                                                  helpDoc);
      if (!helpDocPath.exists())
      {
         throw r::exec::RErrorException("Path " + helpDocPath.absolutePath()
                                        + " not found.");
      }

      // build url and fire event
      std::string url = "help/presentation/?file=";
      std::string file = module_context::createAliasedPath(helpDocPath);
      url += http::util::urlEncode(file, true);

      ClientEvent event(client_events::kShowHelp, url);
      module_context::enqueClientEvent(event);
   }
   catch(const r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   return R_NilValue;
}

Error setPresentationSlideIndex(const json::JsonRpcRequest& request,
                                json::JsonRpcResponse*)
{
   int index = 0;
   Error error = json::readParam(request.params, 0, &index);
   if (error)
      return error;

   presentation::state::setSlideIndex(index);

   presentation::log().onSlideIndexChanged(index);

   return Success();
}

Error createNewPresentation(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   // get file path
   std::string file;
   Error error = json::readParam(request.params, 0, &file);
   if (error)
      return error;
   FilePath filePath = module_context::resolveAliasedPath(file);

   // process template
   std::map<std::string,std::string> vars;
   vars["name"] = filePath.stem();
   core::text::TemplateFilter filter(vars);

   // read file with template filter
   FilePath templatePath = session::options().rResourcesPath().complete(
                                             "templates/r_presentation.Rpres");
   std::string presContents;
   error = core::readStringFromFile(templatePath, filter, &presContents);
   if (error)
      return error;


   // write file
   return core::writeStringToFile(filePath,
                                  presContents,
                                  string_utils::LineEndingNative);
}

Error showPresentationPane(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   std::string file;
   Error error = json::readParam(request.params, 0, &file);
   if (error)
      return error;

   FilePath filePath = module_context::resolveAliasedPath(file);
   if (!filePath.exists())
      return core::fileNotFoundError(filePath, ERROR_LOCATION);

   showPresentation(filePath);

   return Success();
}

Error closePresentationPane(const json::JsonRpcRequest&,
                            json::JsonRpcResponse*)
{
   presentation::state::clear();

   return Success();
}

Error presentationExecuteCode(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   // get the code
   std::string code;
   Error error = json::readParam(request.params, 0, &code);
   if (error)
      return error;

   // confirm we are active
   if (!presentation::state::isActive())
   {
      pResponse->setError(json::errc::MethodUnexpected);
      return Success();
   }

   // execute within the context of either the tutorial project directory
   // or presentation directory
   RestoreCurrentPathScope restorePathScope(
                                          module_context::safeCurrentPath());
   if (presentation::state::isTutorial() &&
       projects::projectContext().hasProject())
   {
      error = projects::projectContext().directory().makeCurrentPath();
   }
   else
   {
      error = presentation::state::directory().makeCurrentPath();
   }
   if (error)
      return error;


   // actually execute the code (show error in the console)
   error = r::exec::executeString(code);
   if (error)
   {
      std::string errMsg = "Error executing code: " + code + "\n";
      errMsg += r::endUserErrorMessage(error);
      module_context::consoleWriteError(errMsg + "\n");
   }

   return Success();
}

Error setWorkingDirectory(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse*)
{
   // get the path
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error;

   // set current path
   FilePath filePath = module_context::resolveAliasedPath(path);
   return filePath.makeCurrentPath();
}

Error tutorialFeedback(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // get the feedback
   std::string feedback;
   Error error = json::readParam(request.params, 0, &feedback);
   if (error)
      return error;

   // confirm we are active
   if (!presentation::state::isActive())
   {
      pResponse->setError(json::errc::MethodUnexpected);
      return Success();
   }

   // record the feedback
   presentation::log().recordFeedback(feedback);

   return Success();
}

Error tutorialQuizResponse(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // get the params
   int slideIndex, answer;
   bool correct;
   Error error = json::readParams(request.params,
                                  &slideIndex,
                                  &answer,
                                  &correct);
   if (error)
      return error;

   // confirm we are active
   if (!presentation::state::isActive())
   {
      pResponse->setError(json::errc::MethodUnexpected);
      return Success();
   }

   // record the feedback
   presentation::log().recordQuizResponse(slideIndex, answer, correct);

   return Success();
}


Error getSlideNavigation(const std::string& code,
                         const FilePath& baseDir,
                         json::Object* pSlideNavigationJson)
{
   SlideDeck slideDeck;
   Error error = slideDeck.readSlides(code, baseDir);
   if (error)
      return error;

   SlideNavigationList navigationList("slide");
   BOOST_FOREACH(const Slide& slide, slideDeck.slides())
   {
      navigationList.add(slide);
   }

   *pSlideNavigationJson = navigationList.asJson();

   return Success();
}

Error getSlideNavigationForFile(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // get param
   std::string file;
   Error error = json::readParam(request.params, 0, &file);
   if (error)
      return error;
   FilePath filePath = module_context::resolveAliasedPath(file);

   // read code
   std::string code;
   error = core::readStringFromFile(filePath,
                                    &code,
                                    string_utils::LineEndingPosix);
   if (error)
      return error;

   // get slide navigation
   json::Object slideNavigationJson;
   error = getSlideNavigation(code, filePath.parent(), &slideNavigationJson);
   if (error)
      return error;
   pResponse->setResult(slideNavigationJson);

   return Success();
}

Error getSlideNavigationForCode(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // get params
   std::string code, parentDir;
   Error error = json::readParams(request.params, &code, &parentDir);
   if (error)
      return error;
   FilePath parentDirPath = module_context::resolveAliasedPath(parentDir);

   // get slide navigation
   json::Object slideNavigationJson;
   error = getSlideNavigation(code, parentDirPath, &slideNavigationJson);
   if (error)
      return error;
   pResponse->setResult(slideNavigationJson);

   return Success();
}

Error clearPresentationCache(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{

   ErrorResponse errorResponse;
   if (!clearKnitrCache(&errorResponse))
   {
      pResponse->setError(systemError(boost::system::errc::io_error,
                                      ERROR_LOCATION),
                          json::toJsonString(errorResponse.message));
   }

   return Success();
}



Error createStandalonePresentation(const json::JsonRpcRequest& request,
                                   json::JsonRpcResponse* pResponse)
{
   std::string pathParam;
   Error error = json::readParam(request.params, 0, &pathParam);
   if (error)
      return error;
   FilePath targetPath = module_context::resolveAliasedPath(pathParam);

   ErrorResponse errorResponse;
   if (!savePresentationAsStandalone(targetPath, &errorResponse))
   {
      pResponse->setError(systemError(boost::system::errc::io_error,
                                      ERROR_LOCATION),
                          json::toJsonString(errorResponse.message));
   }

   return Success();
}

Error createDesktopViewInBrowserPresentation(
                                   const json::JsonRpcRequest& request,
                                   json::JsonRpcResponse* pResponse)
{
   // save to view in browser path
   FilePath targetPath = presentation::state::viewInBrowserPath();
   ErrorResponse errorResponse;
   if (savePresentationAsStandalone(targetPath, &errorResponse))
   {
      pResponse->setResult(module_context::createAliasedPath(targetPath));
   }
   else
   {
      pResponse->setError(systemError(boost::system::errc::io_error,
                                      ERROR_LOCATION),
                          json::toJsonString(errorResponse.message));
   }

   return Success();
}

Error createPresentationRpubsSource(const json::JsonRpcRequest& request,
                                    json::JsonRpcResponse* pResponse)
{
   // use a stable location in the presentation directory for the Rpubs
   // source file so that update works across sessions
   std::string stem = presentation::state::filePath().stem();
   FilePath filePath = presentation::state::directory().childPath(
                                                      stem + "-rpubs.html");

   ErrorResponse errorResponse;
   if (savePresentationAsRpubsSource(filePath, &errorResponse))
   {
      using namespace module_context;
      json::Object resultJson;
      resultJson["published"] = !previousRpubsUploadId(filePath).empty();
      resultJson["source_file_path"] = module_context::createAliasedPath(
                                                                     filePath);
      pResponse->setResult(resultJson);
   }
   else
   {
      pResponse->setError(systemError(boost::system::errc::io_error,
                                      ERROR_LOCATION),
                          json::toJsonString(errorResponse.message));
   }

   return Success();
}

} // anonymous namespace


json::Value presentationStateAsJson()
{
   return presentation::state::asJson();
}

Error initialize()
{
   // register rs_showPresentation
   R_CallMethodDef methodDefShowPresentation;
   methodDefShowPresentation.name = "rs_showPresentation" ;
   methodDefShowPresentation.fun = (DL_FUNC) rs_showPresentation;
   methodDefShowPresentation.numArgs = 1;
   r::routines::addCallMethod(methodDefShowPresentation);

   // register rs_showPresentationHelpDoc
   R_CallMethodDef methodDefShowHelpDoc;
   methodDefShowHelpDoc.name = "rs_showPresentationHelpDoc" ;
   methodDefShowHelpDoc.fun = (DL_FUNC) rs_showPresentationHelpDoc;
   methodDefShowHelpDoc.numArgs = 1;
   r::routines::addCallMethod(methodDefShowHelpDoc);

   // initialize presentation log
   Error error = log().initialize();
   if (error)
      return error;

   using boost::bind;
   using namespace session::module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/presentation", handlePresentationPaneRequest))
      (bind(registerRpcMethod, "create_standalone_presentation", createStandalonePresentation))
      (bind(registerRpcMethod, "create_desktop_view_in_browser_presentation",
                                createDesktopViewInBrowserPresentation))
      (bind(registerRpcMethod, "create_presentation_rpubs_source", createPresentationRpubsSource))
      (bind(registerRpcMethod, "set_presentation_slide_index", setPresentationSlideIndex))
      (bind(registerRpcMethod, "create_new_presentation", createNewPresentation))
      (bind(registerRpcMethod, "show_presentation_pane", showPresentationPane))
      (bind(registerRpcMethod, "close_presentation_pane", closePresentationPane))
      (bind(registerRpcMethod, "presentation_execute_code", presentationExecuteCode))
      (bind(registerRpcMethod, "set_working_directory", setWorkingDirectory))
      (bind(registerRpcMethod, "tutorial_feedback", tutorialFeedback))
      (bind(registerRpcMethod, "tutorial_quiz_response", tutorialQuizResponse))
      (bind(registerRpcMethod, "get_slide_navigation_for_file", getSlideNavigationForFile))
      (bind(registerRpcMethod, "get_slide_navigation_for_code", getSlideNavigationForCode))
      (bind(registerRpcMethod, "clear_presentation_cache", clearPresentationCache))
      (bind(presentation::state::initialize))
      (bind(sourceModuleRFile, "SessionPresentation.R"));

   return initBlock.execute();
}

} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

