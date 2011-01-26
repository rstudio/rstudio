/*
 * SessionPlots.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionPlots.hpp"

#include <boost/format.hpp>
#include <boost/iostreams/filter/regex.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/Predicate.hpp>
#include <core/FilePath.hpp>
#include <core/BoostErrors.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RGraphics.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules {
namespace plots {
  
namespace {

// locations
#define kGraphics "/graphics"
   
Error setActivePlot(const json::JsonRpcRequest& request, 
                    json::JsonRpcResponse* pResponse)
{   
   // params
   int index ;
   Error error = json::readParam(request.params, 0, &index);
   if (error)
      return error ;
   
   return r::session::graphics::display().setActivePlot(index);   
} 
   
Error nextPlot(const json::JsonRpcRequest& request, 
               json::JsonRpcResponse* pResponse)
{   
   r::session::graphics::Display& display = r::session::graphics::display();
   return display.setActivePlot(display.activePlotIndex() + 1);
}     
   
Error previousPlot(const json::JsonRpcRequest& request, 
                   json::JsonRpcResponse* pResponse)
{   
   r::session::graphics::Display& display = r::session::graphics::display();
   return display.setActivePlot(display.activePlotIndex() - 1);
}  

   
Error removePlot(const json::JsonRpcRequest& request, 
                 json::JsonRpcResponse* pResponse)
{   
   // params
   int index ;
   Error error = json::readParam(request.params, 0, &index);
   if (error)
      return error ;
   
   return r::session::graphics::display().removePlot(index);
} 
   
   
Error clearPlots(const json::JsonRpcRequest& request, 
                 json::JsonRpcResponse* pResponse)
{
   r::session::graphics::display().clear();
   return Success();
}

Error loadPlot(const json::JsonRpcRequest& request, 
               json::JsonRpcResponse* pResponse)
{   
   // params
   std::string varName ;
   Error error = json::readParam(request.params, 0, &varName);
   if (error)
      return error;
   
   // execute replay plot
   boost::format replayFmt("replayPlot(%1%)");
   return r::exec::executeString(boost::str(replayFmt % varName));
}

Error refreshPlot(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   r::session::graphics::display().refresh();
   return Success();
}

Error exportPlot(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string path;
   int width, height;
   json::readParams(request.params, &path, &width, &height);
   return r::session::graphics::display().savePlotAsPng(FilePath(path),
                                                        width,
                                                        height);
}
   
template <typename T>
bool extractSizeParams(const http::Request& request,
                       T min, 
                       T max,
                       T* pWidth,
                       T* pHeight,
                       http::Response* pResponse)
{      
   // get the width and height parameters
   if (!request.queryParamValue("width", 
                                predicate::range(min, max), 
                                pWidth))
   {
      pResponse->setError(http::status::BadRequest, "invalid width");
      return false;
   }
   if (!request.queryParamValue("height", 
                                predicate::range(min, max),
                                pHeight))
   {
      pResponse->setError(http::status::BadRequest, "invalid height");
      return false;
   }
   
   // got two valid params
   return true;
}
   
void setImageFileResponse(const FilePath& imageFilePath,
                          const http::Request& request, 
                          http::Response* pResponse)
{
   // set content type
   pResponse->setContentType(imageFilePath.mimeContentType());
   
   // attempt gzip
   if (request.acceptsEncoding(http::kGzipEncoding))
      pResponse->setContentEncoding(http::kGzipEncoding);
   
   // set file
   Error error;
   if (imageFilePath.extensionLowerCase() == ".svg")
   {
      // filter for making sure width and height are 100%
      boost::iostreams::basic_regex_filter<char> sizeFilter(
            boost::regex("width=\"[0-9].*pt\" height=\"[0-9].*pt\" viewBox="),
            "width=\"100%\" height=\"100%\" viewBox=");
   
      error = pResponse->setBody(imageFilePath, sizeFilter);
   }
   else
   {
      error = pResponse->setBody(imageFilePath);
   }
   
   // report error to client if one occurred
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setError(http::status::InternalServerError,
                          error.code().message());
   }
}

void setTemporaryFileResponse(const FilePath& filePath, 
                              const http::Request& request, 
                              http::Response* pResponse)
{
   // no cache (dynamic content)
   pResponse->setNoCacheHeaders();
   
   // return the file
   pResponse->setFile(filePath, request);
   
   // delete the file
   Error error = filePath.remove();
   if (error)
      LOG_ERROR(error);
}   
   
void handleZoomRequest(const http::Request& request, http::Response* pResponse)
{
   using namespace r::session;

   // get the width and height parameters
   int width, height;
   if (!extractSizeParams(request, 100, 3000, &width, &height, pResponse))
     return ;

   // get the type (force png if the back end doesn't support svg)
   std::string type;
   if (!graphics::display().supportsSvg())
   {
      type = "png";
   }
   else
   {
      type = request.queryParamValue("type");
      if (type != "png")
         type = "svg";
   }

   // generate the file
   FilePath imagePath = module_context::tempFile("plot", type);
   Error saveError;
   if (type == "png")
   {
      saveError = graphics::display().savePlotAsPng(imagePath, width, height);
   }
   else // svg 
   {
      // scale the size down by 20% so we provide some "zoom" effect (larger
      // typefaces and other plot features)
      width -= (width/5);
      height -= (height/5);
      
      // generate the file
      saveError = graphics::display().savePlotAsSvg(imagePath, width, height);
   }
   
   // check for error
   if (saveError)
   {
      pResponse->setError(http::status::InternalServerError, 
                          saveError.code().message());
      return;
   }
   
   // send it back
   setImageFileResponse(imagePath, request, pResponse);
   
   // delete the temp file
   Error error = imagePath.remove();
   if (error)
      LOG_ERROR(error);
}
   
void handlePrintRequest(const http::Request& request, http::Response* pResponse)
{
   // get the width and height parameters
   double width, height;
   if (!extractSizeParams(request, 3.0, 30.0, &width, &height, pResponse))
      return ;
 
   // generate the pdf
   FilePath pdfPath = module_context::tempFile("plot", "pdf");
   using namespace r::session;
   Error error = graphics::display().savePlotAsPdf(pdfPath, width, height);
   if (error)
   {
      pResponse->setError(http::status::InternalServerError,
                          error.code().message());
      return;
   }
   
   // return it
   setTemporaryFileResponse(pdfPath, request, pResponse);
}


   
void handlePngRequest(const http::Request& request, 
                      http::Response* pResponse)
{
   // get the width and height parameters
   int width, height;
   if (!extractSizeParams(request, 100, 2000, &width, &height, pResponse))
      return ;

   // generate the image
   using namespace r::session;
   FilePath imagePath = module_context::tempFile("plot", "png");
   Error error = graphics::display().savePlotAsPng(imagePath, width, height);
   if (error)
   {
      pResponse->setError(http::status::InternalServerError,
                          error.code().message());
      return;
   }

   // check for attachment flag and set content-disposition
   bool attachment = request.queryParamValue("attachment") == "1";
   if (attachment)
   {
      pResponse->setHeader("Content-Disposition",
                           "attachment; filename=rstudio-plot" +
                           imagePath.extension());
   }

   // return it
   setTemporaryFileResponse(imagePath, request, pResponse);
}


// NOTE: this function assumes it is retreiving the image for the currently
// active plot (the assumption is implied by the fact that file not found
// on the requested svg results in a redirect to the currently active
// plot's svg). to handle this redirection we should always maintain an
// entry point with these semantics. if we wish to have an entry point
// for obtaining arbitrary svgs then it should be separate from this.
   
void handleGraphicsRequest(const http::Request& request, 
                           http::Response* pResponse)
{    
   // extract plot key from request (take everything after the last /)
   std::string uri = request.uri();
   std::size_t lastSlashPos = uri.find_last_of('/');
   if (lastSlashPos == std::string::npos || 
       lastSlashPos == (uri.length() - 1))
   {
      std::string errmsg = "invalid graphics uri: " + uri;
      LOG_ERROR_MESSAGE(errmsg);
      pResponse->setError(http::status::NotFound, errmsg);
      return ;
   }
   std::string filename = uri.substr(lastSlashPos+1);
 
   // calculate the path to the svg
   using namespace r::session;
   FilePath imagePath = graphics::display().imagePath(filename);
      
   // if it exists then return it
   if (imagePath.exists())
   {
      // strong named - cache permanently (in user's browser only)
      pResponse->setPrivateCacheForeverHeaders();

      // set the file
      setImageFileResponse(imagePath, request, pResponse);   
   }
   else
   {
      // redirect to svg for currently active plot
      if (graphics::display().hasOutput())
      {
         // calculate location of current image
         std::string imageFilename = graphics::display().imageFilename();
         std::string imageLocation = std::string(kGraphics "/") +
                                     imageFilename;
                                 
         // redirect to it
         pResponse->setMovedTemporarily(request, imageLocation);
      }
      else
      {
         // not found error
         pResponse->setError(http::status::NotFound, 
                             request.uri() + " not found");
      }
   }
}

   
void enquePlotsChanged(const r::session::graphics::DisplayState& displayState,
                       bool activatePlots, bool showManipulator)
{
   // build graphics output event
   json::Object jsonPlotsState;
   jsonPlotsState["filename"] = displayState.imageFilename;
   jsonPlotsState["manipulator"] = displayState.manipulatorJson;
   jsonPlotsState["width"] = displayState.width;
   jsonPlotsState["height"] = displayState.height;
   jsonPlotsState["plotIndex"] = displayState.activePlotIndex;
   jsonPlotsState["plotCount"] = displayState.plotCount;
   jsonPlotsState["activatePlots"] = activatePlots;
   jsonPlotsState["showManipulator"] = showManipulator;
   ClientEvent plotsStateChangedEvent(client_events::kPlotsStateChanged, 
                                      jsonPlotsState);
      
   // fire it
   module_context::enqueClientEvent(plotsStateChangedEvent);
  
}
   
void onClientInit()
{
   // if we have output make sure the client knows about it
   using namespace r::session;
   if (graphics::display().hasOutput())
   {
      graphics::display().render(boost::bind(enquePlotsChanged, _1, false, false));
   }
   
}

void onDetectChanges(module_context::ChangeSource source)
{
   // activate the plots tab if this came from the user
   bool activatePlots = source == module_context::ChangeSourceREPL;
   
   // check for changes
   using namespace r::session;
   if (graphics::display().hasChanges())
   {
      graphics::display().render(boost::bind(enquePlotsChanged,
                                             _1,
                                             activatePlots,
                                             false));
   }
}


void onShowManipulator()
{
   // render changes and show manipulator
   using namespace r::session;
   if (graphics::display().hasOutput())
      graphics::display().render(boost::bind(enquePlotsChanged, _1, true, true));
}

Error setManipulatorValues(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{



   using namespace r::session;
   if (graphics::display().hasOutput())
      graphics::display().render(boost::bind(enquePlotsChanged, _1, true, true));

   return Success();
}

} // anonymous namespace  
   
Error initialize()
{
   // subscribe to events
   using boost::bind;
   module_context::events().onClientInit.connect(bind(onClientInit));
   module_context::events().onDetectChanges.connect(bind(onDetectChanges, _1));

   using namespace r::session;
   graphics::display().onShowManipulator().connect(bind(onShowManipulator));
   
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_active_plot", setActivePlot))
      (bind(registerRpcMethod, "next_plot", nextPlot))
      (bind(registerRpcMethod, "previous_plot", previousPlot))
      (bind(registerRpcMethod, "remove_plot", removePlot))
      (bind(registerRpcMethod, "clear_plots", clearPlots))
      (bind(registerRpcMethod, "load_plot", loadPlot))
      (bind(registerRpcMethod, "refresh_plot", refreshPlot))
      (bind(registerRpcMethod, "export_plot", exportPlot))
      (bind(registerRpcMethod, "set_manipulator_values", setManipulatorValues))
      (bind(registerUriHandler, kGraphics "/plot_zoom", handleZoomRequest))
      (bind(registerUriHandler, kGraphics "/plot.pdf", handlePrintRequest))
      (bind(registerUriHandler, kGraphics "/plot.png", handlePngRequest))
      (bind(registerUriHandler, kGraphics, handleGraphicsRequest));
   return initBlock.execute();
}
         
} // namespace plots
} // namespace modules   
} // namespace session
  
