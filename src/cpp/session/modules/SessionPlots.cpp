/*
 * SessionPlots.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/system/Environment.hpp>

#include <core/text/TemplateFilter.hpp>

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
   r::session::graphics::Display& display = r::session::graphics::display();

   if (display.plotCount() < 1)
   {
      return Error(core::json::errc::ParamInvalid, ERROR_LOCATION);
   }
   else if (display.plotCount() == 1)
   {
      r::session::graphics::display().clear();
      return Success();
   }
   else
   {
      int activePlot = display.activePlotIndex();
      return display.removePlot(activePlot);
   }
} 
   
   
Error clearPlots(const json::JsonRpcRequest& request, 
                 json::JsonRpcResponse* pResponse)
{
   r::session::graphics::display().clear();
   return Success();
}

Error refreshPlot(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   r::session::graphics::display().refresh();
   return Success();
}

json::Object boolObject(bool value)
{
   json::Object boolObject ;
   boolObject["value"] = value;
   return boolObject;
}

Error savePlotAs(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // get args
   std::string path, format;
   int width, height;
   bool overwrite;
   Error error = json::readParams(request.params,
                                  &path,
                                  &format,
                                  &width,
                                  &height,
                                  &overwrite);
   if (error)
      return error;

   // resolve path
   FilePath plotPath = module_context::resolveAliasedPath(path);

   // if it already exists and we aren't ovewriting then return false
   if (plotPath.exists() && !overwrite)
   {
      pResponse->setResult(boolObject(false));
      return Success();
   }

   // save plot
   using namespace r::session::graphics;
   Display& display = r::session::graphics::display();
   error = display.savePlotAsImage(plotPath, format, width, height);
   if (error)
   {
       LOG_ERROR(error);
       return error;
   }


   // set success result
   pResponse->setResult(boolObject(true));
   return Success();
}


Error savePlotAsPdf(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // get args
   std::string path;
   double width, height;
   bool useCairoPdf, overwrite;
   Error error = json::readParams(request.params,
                                  &path,
                                  &width,
                                  &height,
                                  &useCairoPdf,
                                  &overwrite);
   if (error)
      return error;

   // resolve path
   FilePath plotPath = module_context::resolveAliasedPath(path);

   // if it already exists and we aren't ovewriting then return false
   if (plotPath.exists() && !overwrite)
   {
      pResponse->setResult(boolObject(false));
      return Success();
   }

   // save plot
   using namespace r::session::graphics;
   Display& display = r::session::graphics::display();
   error = display.savePlotAsPdf(plotPath, width, height, useCairoPdf);
   if (error)
   {
      LOG_ERROR_MESSAGE(r::endUserErrorMessage(error));
      return error;
   }

   // set success result
   pResponse->setResult(boolObject(true));
   return Success();
}

Error copyPlotToClipboardMetafile(const json::JsonRpcRequest& request,
                                  json::JsonRpcResponse* pResponse)
{
   // get args
   int width, height;
   Error error = json::readParams(request.params, &width, &height);
   if (error)
      return error;

#if _WIN32

   // create temp file to write to
   FilePath targetFile = module_context::tempFile("clipboard", "emf");

   // save as metafile
   using namespace r::session::graphics;
   Display& display = r::session::graphics::display();
   error = display.savePlotAsMetafile(targetFile, width, height);
   if (error)
      return error;

   // copy to clipboard
   error = system::copyMetafileToClipboard(targetFile);
   if (error)
      return error;

   // remove temp file
   error = targetFile.remove();
   if (error)
      LOG_ERROR(error);

   return Success();

#else
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
#endif
}



bool hasStem(const FilePath& filePath, const std::string& stem)
{
   return filePath.stem() == stem;
}

json::Object plotExportFormat(const std::string& name,
                              const std::string& extension)
{
   json::Object formatJson;
   formatJson["name"] = name;
   formatJson["extension"] = extension;
   return formatJson;
}

Error uniqueSavePlotStem(const FilePath& directoryPath, std::string* pStem)
{
   // determine unique file name
   std::vector<FilePath> children;
   Error error = directoryPath.children(&children);
   if (error)
      return error;

   // search for unique stem
   int i = 0;
   *pStem = "Rplot";
   while(true)
   {
      // seek stem
      std::vector<FilePath>::const_iterator it = std::find_if(
                                                children.begin(),
                                                children.end(),
                                                boost::bind(hasStem, _1, *pStem));
      // break if not found
      if (it == children.end())
         break;

      // update stem and search again
      boost::format fmt("Rplot%1%");
      *pStem = boost::str(fmt % boost::io::group(std::setfill('0'),
                                                 std::setw(2),
                                                 ++i));
   }

   return Success();
}

Error getUniqueSavePlotStem(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   // get directory arg and convert to path
   std::string directory;
   Error error = json::readParam(request.params, 0, &directory);
   if (error)
      return error;
   FilePath directoryPath = module_context::resolveAliasedPath(directory);

   // get stem
   std::string stem;
   error = uniqueSavePlotStem(directoryPath, &stem);
   if (error)
      return error;

   // set resposne
   pResponse->setResult(stem);
   return Success();
}

bool supportsSvg()
{
   bool supportsSvg = false;
   Error error = r::exec::RFunction("capabilities", "cairo").call(&supportsSvg);
   if (error)
      LOG_ERROR(error);
   return supportsSvg;
}

Error getSavePlotContext(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // get directory arg
   std::string directory;
   Error error = json::readParam(request.params, 0, &directory);
   if (error)
      return error;

   // context
   json::Object contextJson;

   // get supported formats
    using namespace r::session::graphics;
   json::Array formats;
   formats.push_back(plotExportFormat("PNG", kPngFormat));
   formats.push_back(plotExportFormat("JPEG", kJpegFormat));
   formats.push_back(plotExportFormat("TIFF", kTiffFormat));
   formats.push_back(plotExportFormat("BMP", kBmpFormat));
#if _WIN32
   formats.push_back(plotExportFormat("Metafile", kMetafileFormat));
#endif
   if(supportsSvg())
      formats.push_back(plotExportFormat("SVG", kSvgFormat));
   formats.push_back(plotExportFormat("EPS", kPostscriptFormat));
   contextJson["formats"] = formats;

   // get directory path
   FilePath directoryPath = module_context::resolveAliasedPath(directory);

   // reflect directory back to caller
   contextJson["directory"] = module_context::createFileSystemItem(directoryPath);

   // get unique stem
   std::string stem;
   error = uniqueSavePlotStem(directoryPath, &stem);
   if (error)
      return error;
   contextJson["uniqueFileStem"] = stem;

   pResponse->setResult(contextJson);

   return Success();
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
   Error error = pResponse->setBody(imageFilePath);
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

   // fire off the plot zoom size changed event to notify the client
   // that a new default size should be established
   json::Object dataJson;
   dataJson["width"] = width;
   dataJson["height"] = height;
   ClientEvent event(client_events::kPlotsZoomSizeChanged, dataJson);
   module_context::enqueClientEvent(event);

   // get the scale parameter
   int scale = request.queryParamValue("scale", 1);

   // define template
   std::stringstream templateStream;
   templateStream <<
      "<html>"
         "<head>"
            "<title>Plot Zoom</title>"
            "<script type=\"text/javascript\">"

               "window.onresize = function() {"

                  "var plotEl = document.getElementById('plot');"
                  "if (plotEl && (#scale#==1) ) {"
                     "plotEl.style.width='100%';"
                     "plotEl.style.height='100%';"
                  "}"

                  "if(window.activeTimer)"
                     "clearTimeout(window.activeTimer);"

                  "window.activeTimer = setTimeout( function() { "

                     "window.location.href = "
                        "\"plot_zoom?width=\" + document.body.clientWidth "
                              " + \"&height=\" + document.body.clientHeight "
                              " + \"&scale=\" + #scale#;"
                   "}, 300);"
               "}"
            "</script>"
         "</head>"
         "<body style=\"margin: 0; overflow: hidden\">"
            "<img id=\"plot\" src=\"plot_zoom_png?width=#width#&height=#height#\"/>"
         "</body>"
      "</html>";

   // define variables
   std::map<std::string,std::string> variables;
   variables["width"] = safe_convert::numberToString(width);
   variables["height"] = safe_convert::numberToString(height);
   variables["scale"] = safe_convert::numberToString(scale);;
   text::TemplateFilter filter(variables);

   pResponse->setNoCacheHeaders();
   pResponse->setBody(templateStream, filter);
   pResponse->setContentType("text/html");
}
   
void handleZoomPngRequest(const http::Request& request,
                          http::Response* pResponse)
{
   using namespace r::session;

   // get the width and height parameters
   int width, height;
   if (!extractSizeParams(request, 100, 5000, &width, &height, pResponse))
     return ;

   // generate the file
   using namespace r::session::graphics;
   FilePath imagePath = module_context::tempFile("plot", "png");
   Error saveError = graphics::display().savePlotAsImage(imagePath,
                                                         kPngFormat,
                                                         width,
                                                         height);
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

void handlePngRequest(const http::Request& request, 
                      http::Response* pResponse)
{
   // get the width and height parameters
   int width, height;
   if (!extractSizeParams(request, 100, 5000, &width, &height, pResponse))
      return ;

   // generate the image
   using namespace r::session;
   FilePath imagePath = module_context::tempFile("plot", "png");
   Error error = graphics::display().savePlotAsImage(imagePath,
                                                      graphics::kPngFormat,
                                                      width,
                                                      height);
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
// on the requested png results in a redirect to the currently active
// plot's png). to handle this redirection we should always maintain an
// entry point with these semantics. if we wish to have an entry point
// for obtaining arbitrary pngs then it should be separate from this.
   
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
 
   // calculate the path to the png
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
      // redirect to png for currently active plot
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
   
void renderGraphicsOutput(bool activatePlots, bool showManipulator)
{
   using namespace r::session;
   if (graphics::display().hasOutput())
   {
      graphics::display().render(
         boost::bind(enquePlotsChanged, _1, activatePlots, showManipulator));
   }
}


void onClientInit()
{
   // if we have output make sure the client knows about it
   renderGraphicsOutput(false, false);
}

void detectChanges(bool activatePlots)
{
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

void onDetectChanges(module_context::ChangeSource source)
{
   bool activatePlots = source == module_context::ChangeSourceREPL;
   detectChanges(activatePlots);
}

void onSysSleep()
{
   detectChanges(true);
}

void onBeforeExecute()
{
   r::session::graphics::display().onBeforeExecute();
}

void onShowManipulator()
{
   // render changes and show manipulator
   renderGraphicsOutput(true, true);
}

Error setManipulatorValues(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   // read the params
   json::Object jsObject;
   Error error = json::readParam(request.params, 0, &jsObject);
   if (error)
      return error;

   // set them
   using namespace r::session;
   graphics::display().setPlotManipulatorValues(jsObject);

   // render
   renderGraphicsOutput(true, false);

   return Success();
}

Error manipulatorPlotClicked(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // read the params
   int x, y;
   Error error = json::readParams(request.params, &x, &y);
   if (error)
      return error;

   // notify the device
   using namespace r::session;
   graphics::display().manipulatorPlotClicked(x, y);

   // render
   renderGraphicsOutput(true, false);

   return Success();
}



} // anonymous namespace  
   
bool haveCairoPdf()
{
   // make sure there is a real x server running on osx
#ifdef __APPLE__
   std::string display = core::system::getenv("DISPLAY");
   if (display.empty() || (display == ":0"))
      return false;
#endif

   SEXP functionSEXP = R_NilValue;
   r::sexp::Protect rProtect;
   r::exec::RFunction f(".rs.getPackageFunction", "cairo_pdf", "grDevices");
   Error error = f.call(&functionSEXP, &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return functionSEXP != R_NilValue;
}

Error initialize()
{
   // subscribe to events
   using boost::bind;
   module_context::events().onClientInit.connect(bind(onClientInit));
   module_context::events().onDetectChanges.connect(bind(onDetectChanges, _1));
   module_context::events().onBeforeExecute.connect(bind(onBeforeExecute));
   module_context::events().onSysSleep.connect(bind(onSysSleep));

   // connect to onShowManipulator
   using namespace r::session;
   graphics::display().onShowManipulator().connect(bind(onShowManipulator));
   
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "next_plot", nextPlot))
      (bind(registerRpcMethod, "previous_plot", previousPlot))
      (bind(registerRpcMethod, "remove_plot", removePlot))
      (bind(registerRpcMethod, "clear_plots", clearPlots))
      (bind(registerRpcMethod, "refresh_plot", refreshPlot))
      (bind(registerRpcMethod, "save_plot_as", savePlotAs))
      (bind(registerRpcMethod, "save_plot_as_pdf", savePlotAsPdf))
      (bind(registerRpcMethod, "copy_plot_to_clipboard_metafile", copyPlotToClipboardMetafile))
      (bind(registerRpcMethod, "get_unique_save_plot_stem", getUniqueSavePlotStem))
      (bind(registerRpcMethod, "get_save_plot_context", getSavePlotContext))
      (bind(registerRpcMethod, "set_manipulator_values", setManipulatorValues))
      (bind(registerRpcMethod, "manipulator_plot_clicked", manipulatorPlotClicked))
      (bind(registerUriHandler, kGraphics "/plot_zoom_png", handleZoomPngRequest))
      (bind(registerUriHandler, kGraphics "/plot_zoom", handleZoomRequest))
      (bind(registerUriHandler, kGraphics "/plot.png", handlePngRequest))
      (bind(registerUriHandler, kGraphics, handleGraphicsRequest));
   return initBlock.execute();
}
         
} // namespace plots
} // namespace modules   
} // namespace session
  
