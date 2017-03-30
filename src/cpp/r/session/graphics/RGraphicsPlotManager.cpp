/*
 * RGraphicsPlotManager.cpp
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

#include "RGraphicsPlotManager.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/foreach.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>

#include <r/RExec.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>
#include <r/RErrorCategory.hpp>
#include <r/session/RSessionUtils.hpp>

#include "RGraphicsUtils.hpp"
#include "RGraphicsDevice.hpp"
#include "RGraphicsPlotManipulatorManager.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {  
namespace graphics {

namespace {

double pixelsToInches(int pixels)
{
   return (double)pixels / 96.0;
}

} // anonymous namespace

const char * const kPngFormat = "png";
const char * const kJpegFormat = "jpeg";
const char * const kBmpFormat = "bmp";
const char * const kTiffFormat = "tiff";
const char * const kMetafileFormat = "emf";
const char * const kSvgFormat = "svg";
const char * const kPostscriptFormat = "eps";

// satisfy r::session::graphics::Display singleton
Display& display()
{
   return graphics::plotManager();
}
   
PlotManager& plotManager()
{
   static PlotManager instance;
   return instance;
}
   
PlotManager::PlotManager()
   :  displayHasChanges_(false), 
      lastChange_(boost::posix_time::not_a_date_time),
      suppressDeviceEvents_(false),
      activePlot_(-1),
      plotInfoRegex_("([A-Za-z0-9\\-]+):([0-9]+),([0-9]+)")
{
   plots_.set_capacity(100);
}
      
Error PlotManager::initialize(const FilePath& graphicsPath,
                              const GraphicsDeviceFunctions& graphicsDevice,
                              GraphicsDeviceEvents* pEvents)
{
   // initialize plot manipulator manager
   Error error = plotManipulatorManager().initialize(graphicsDevice.convert);
   if (error)
      return error;

   // save reference to graphics path and make sure it exists
   graphicsPath_ = graphicsPath ;
   error = graphicsPath_.ensureDirectory();
   if (error)
      return error;

   // save reference to plots state file
   plotsStateFile_ = graphicsPath_.complete("INDEX");
   
   // save reference to graphics device functions
   graphicsDevice_ = graphicsDevice;
   
   // sign up for graphics device events
   using boost::bind;
   pEvents->onNewPage.connect(bind(&PlotManager::onDeviceNewPage, this, _1));
   pEvents->onDrawing.connect(bind(&PlotManager::onDeviceDrawing, this));
   pEvents->onResized.connect(bind(&PlotManager::onDeviceResized, this));
   pEvents->onClosed.connect(bind(&PlotManager::onDeviceClosed, this));

   return Success();
}
      

int PlotManager::plotCount() const
{
   return plots_.size();
}
   
Error PlotManager::plotImageFilename(int index, 
                                     std::string* pImageFilename) const
{
   if (!isValidPlotIndex(index))
   {
      return plotIndexError(index, ERROR_LOCATION);
   }
   else
   {
      *pImageFilename = plots_[index]->imageFilename();
      return Success();
   }
}      
   
int PlotManager::activePlotIndex() const
{
   return activePlot_;
}
       
// NOTE: returns an error if the plot index is invalid. Otherwise will always
// successfully update the active plot state. If any file or rendering errors
// occur while setting the active plot they will be reported but will not
// cause the method to return an error.
Error PlotManager::setActivePlot(int index)
{
   if (!isValidPlotIndex(index))
      return plotIndexError(index, ERROR_LOCATION);
   
   if (activePlot_ != index)
   {
      // if there is already a plot active then release its
      // in-memory resources
      if (hasPlot())
         activePlot().purgeInMemoryResources();

      // set index
      activePlot_ = index;
      
      // render it
      renderActivePlotToDisplay();

      // trip changes flag 
      setDisplayHasChanges(true);
   }
   
   // return success
   return Success();
}

   
// NOTE: returns an error if the plot index is invalid. Otherwise it 
// is guaranteed to have removed the plot. Rendering or file errors which
// occur during the removal or transformation to a new graphics state are
// reported to the user and logged but are not returned (because in these
// cases the actual removal succeeded)
Error PlotManager::removePlot(int index)
{
   if (!isValidPlotIndex(index))
      return plotIndexError(index, ERROR_LOCATION);
   
   // remove the plot files 
   Error removeError = plots_[index]->removeFiles();
   if (removeError)
      logAndReportError(removeError, ERROR_LOCATION);
   
   // erase the plot from the internal list
   plots_.erase(plots_.begin() + index);
   
   // trip changes flag (removing a plot will affect the number of plots
   // and the active plot index so we need a new changed event)
   setDisplayHasChanges(true);

   // fixup active plot as necessary
   
   // case: we just removed the active plot
   if (index == activePlot_)
   {
      // clear active plot 
      activePlot_ = -1;
           
      // try to select the plot after the one removed
      if (isValidPlotIndex(index))
      {
         Error error = setActivePlot(index);
         if (error)
            logAndReportError(error, ERROR_LOCATION);
      }
      // try to select the plot prior to the one removed
      else if (isValidPlotIndex(index-1))
      {
         Error error = setActivePlot(index-1);
         if (error)
            logAndReportError(error, ERROR_LOCATION);
      }
   }
   // case: we removed a plot *prior to* the active plot. this means
   // that the list shrunk by 1 so the active plot's index needs to
   // shrink by 1 as well
   else if (index < activePlot_)
   {
      --activePlot_;
   }
   
   
   return Success();
}

Error PlotManager::savePlotAsFile(const boost::function<Error()>&
                                     deviceCreationFunction)
{
   if (!hasPlot())
      return Error(errc::NoActivePlot, ERROR_LOCATION);
   
   // restore previous device after invoking file device
   RestorePreviousGraphicsDeviceScope restoreScope;
   
   // create the target device
   Error error = deviceCreationFunction();
   if (error)
      return error ;
   
   // copy the current contents of the graphics device to the target device
   graphicsDevice_.copyToActiveDevice();
   
   // close the target device to save the file
   return r::exec::RFunction("dev.off").call();
}

Error PlotManager::savePlotAsFile(const std::string& deviceCreationCode)
{
   return savePlotAsFile(
         boost::bind(r::exec::executeString, deviceCreationCode));
}

Error PlotManager::savePlotAsImage(const FilePath& filePath,
                                   const std::string& format,
                                   int widthPx,
                                   int heightPx,
                                   bool useDevicePixelRatio)
{
   double pixelRatio = useDevicePixelRatio ?
                         r::session::graphics::device::devicePixelRatio() : 1;
   return savePlotAsImage(filePath, format, widthPx, heightPx, pixelRatio);
}


Error PlotManager::savePlotAsImage(const FilePath& filePath,
                                   const std::string& format,
                                   int widthPx,
                                   int heightPx,
                                   double pixelRatio)
{
   if (format == kPngFormat ||
       format == kBmpFormat ||
       format == kJpegFormat ||
       format == kTiffFormat)
   {
      return savePlotAsBitmapFile(filePath, format, widthPx, heightPx, pixelRatio);
   }
   else if (format == kSvgFormat)
   {
      return savePlotAsSvg(filePath, widthPx, heightPx);
   }
   else if (format == kMetafileFormat)
   {
      return savePlotAsMetafile(filePath, widthPx, heightPx);
   }
   else if (format == kPostscriptFormat)
   {
      return savePlotAsPostscript(filePath, widthPx, heightPx);
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }
}

Error PlotManager::savePlotAsBitmapFile(const FilePath& targetPath,
                                        const std::string& bitmapFileType,
                                        int width,
                                        int height,
                                        double pixelRatio)
{
   // default res
   int res = 96;

   // adjust for device pixel ratio
   width = width * pixelRatio;
   height = height * pixelRatio;
   res = res * pixelRatio;

   // optional format specific extra params
   std::string extraParams;

   // add extra quality parameter for jpegs
   if (bitmapFileType == kJpegFormat)
      extraParams = ", quality = 100";

   // add extra bitmap params
   extraParams += r::session::graphics::extraBitmapParams();

   // generate code for creating bitmap file device
   boost::format fmt(
      "{ require(grDevices, quietly=TRUE); "
      "  %1%(filename=\"%2%\", width=%3%, height=%4%, res = %5% %6%); }");
   std::string deviceCreationCode = boost::str(fmt % bitmapFileType %
                                                     string_utils::utf8ToSystem(targetPath.absolutePath()) %
                                                     width %
                                                     height %
                                                     res %
                                                     extraParams);

   // save the file
   return savePlotAsFile(deviceCreationCode);
}

Error PlotManager::savePlotAsPdf(const FilePath& filePath, 
                                 double widthInches,
                                 double heightInches,
                                 bool useCairoPdf)
{
   // generate code for creating pdf file device
   std::string code("{ require(grDevices, quietly=TRUE); ");
   if (useCairoPdf)
      code += "cairo_pdf(file=\"%1%\", width=%2%, height=%3%); }";
   else
      code += " pdf(file=\"%1%\", width=%2%, height=%3%, "
             "      useDingbats=FALSE); }";
   boost::format fmt(code);
   std::string deviceCreationCode = boost::str(fmt % string_utils::utf8ToSystem(filePath.absolutePath()) %
                                                     widthInches % 
                                                     heightInches);
   
   // save the file
   return savePlotAsFile(deviceCreationCode);
}

Error PlotManager::savePlotAsSvg(const FilePath& targetPath,
                                 int width,
                                 int height)
{
   // calculate size in inches
   double widthInches = pixelsToInches(width);
   double heightInches = pixelsToInches(height);

   // generate code for creating svg device
   boost::format fmt("{ require(grDevices, quietly=TRUE); "
                     "  svg(filename=\"%1%\", width=%2%, height=%3%, "
                     "      antialias = \"subpixel\"); }");
   std::string deviceCreationCode = boost::str(fmt % string_utils::utf8ToSystem(targetPath.absolutePath()) %
                                                     widthInches %
                                                     heightInches);

   return savePlotAsFile(deviceCreationCode);
}

Error PlotManager::savePlotAsPostscript(const FilePath& targetPath,
                                        int width,
                                        int height)
{
   // calculate size in inches
   double widthInches = pixelsToInches(width);
   double heightInches = pixelsToInches(height);

   // generate code for creating postscript device
   boost::format fmt("{ require(grDevices, quietly=TRUE); "
                     "  postscript(file=\"%1%\", width=%2%, height=%3%, "
                     "             onefile = FALSE, "
                     "             paper = \"special\", "
                     "             horizontal = FALSE); }");
   std::string deviceCreationCode = boost::str(fmt % string_utils::utf8ToSystem(targetPath.absolutePath()) %
                                                     widthInches %
                                                     heightInches);

   return savePlotAsFile(deviceCreationCode);
}


Error PlotManager::savePlotAsMetafile(const core::FilePath& filePath,
                                      int widthPx,
                                      int heightPx)
{ 
#ifdef _WIN32
   // calculate size in inches
   double widthInches = pixelsToInches(widthPx);
   double heightInches = pixelsToInches(heightPx);

   // generate code for creating metafile device
   boost::format fmt("{ require(grDevices, quietly=TRUE); "
                     "  win.metafile(filename=\"%1%\", width=%2%, height=%3%, "
                     "               restoreConsole=FALSE); }");
   std::string deviceCreationCode = boost::str(fmt % string_utils::utf8ToSystem(filePath.absolutePath()) %
                                                     widthInches %
                                                     heightInches);

   return savePlotAsFile(deviceCreationCode);

#else
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
#endif
}


bool PlotManager::hasOutput() const   
{
   return hasPlot();
}
    
bool PlotManager::hasChanges() const
{
   return displayHasChanges_ ;
}

bool PlotManager::isActiveDevice() const
{
   return graphicsDevice_.isActive();
}

boost::posix_time::ptime PlotManager::lastChange() const
{
   return lastChange_;
}
   
void PlotManager::render(boost::function<void(DisplayState)> outputFunction)
{
   // make sure the graphics path exists (may have been blown away
   // by call to dev.off or other call to removeAllPlots)
   Error error = graphicsPath_.ensureDirectory();
   if (error)
   {
      Error graphicsError(errc::PlotFileError, error, ERROR_LOCATION);
      logAndReportError(graphicsError, ERROR_LOCATION);
      return;
   }
   
   // clear changes flag
   setDisplayHasChanges(false);
   
   // optional manipulator structure
   json::Value plotManipulatorJson;

   if (hasPlot()) // write image for active plot
   {
      // copy current contents of the display to the active plot files
      Error error = activePlot().renderFromDisplay();
      if (error)
      {
         // no such file error expected in the case of an invalid graphics
         // context (because generation of the PNG would have failed)
         bool pngNotFound = error.cause() && isPathNotFoundError(error.cause());

         // only log if this wasn't png not found
         if (!pngNotFound)
         {
            // for r code execution errors we just report them
            if (r::isCodeExecutionError(error))
               reportError(error);
            else
               logAndReportError(error, ERROR_LOCATION);
         }

         return;
      }

      // get manipulator
      activePlot().manipulatorAsJson(&plotManipulatorJson);
   }
   else  // write "empty" image 
   {
      // create an empty file
      FilePath emptyImageFilePath = graphicsPath_.complete(emptyImageFilename());
      error = writeStringToFile(emptyImageFilePath, std::string());
      if (error)
      {
         Error graphicsError(errc::PlotRenderingError, error, ERROR_LOCATION);
         logAndReportError(graphicsError, ERROR_LOCATION);
         return;
      }
   }
   
   // call output function
   DisplayState currentState(imageFilename(),
                             plotManipulatorJson,
                             r::session::graphics::device::getWidth(),
                             r::session::graphics::device::getHeight(),
                             activePlotIndex(), 
                             plotCount());
   outputFunction(currentState);
}
   
std::string PlotManager::imageFilename() const 
{
   if (hasPlot())
   {
      return plots_[activePlot_]->imageFilename();   
   }
   else
   {
      return emptyImageFilename();
   }
}
   
void PlotManager::refresh()
{
   invalidateActivePlot();
}
   
FilePath PlotManager::imagePath(const std::string& imageFilename) const
{
   return graphicsPath_.complete(imageFilename);
}

void PlotManager::clear()
{
   graphicsDevice_.close();
}



boost::signal<void ()>& PlotManager::onShowManipulator()
{
   return plotManipulatorManager().onShowManipulator();
}



void PlotManager::setPlotManipulatorValues(const json::Object& values)
{
   return plotManipulatorManager().setPlotManipulatorValues(values);
}

void PlotManager::manipulatorPlotClicked(int x, int y)
{
   plotManipulatorManager().manipulatorPlotClicked(x, y);
}


void PlotManager::onBeforeExecute()
{
   graphicsDevice_.onBeforeExecute();
}

Error PlotManager::savePlotsState()
{
   // exit if we don't have a graphics path
   if (!graphicsPath_.exists())
      return Success() ;

   // list to write
   std::vector<std::string> plots ;
   
    // write the storage id of the active plot
   if (hasPlot())
      plots.push_back(activePlot().storageUuid());

   // build sequence of plot info (id:width,height)
   for (boost::circular_buffer<PtrPlot>::const_iterator it = plots_.begin();
        it != plots_.end();
        ++it)
   {
      const Plot& plot = *(it->get());
      
      boost::format fmt("%1%:%2%,%3%");
      std::string plotInfo = boost::str(fmt % plot.storageUuid() %
                                              plot.renderedSize().width %
                                              plot.renderedSize().height);
      plots.push_back(plotInfo);
   }
   
   // suppres all device events after suspend
   suppressDeviceEvents_ = true ;
   
   // write plot list
   return writeStringVectorToFile(plotsStateFile_, plots);
}
   
Error PlotManager::restorePlotsState()
{
   // exit if we don't have a plot list
   if (!plotsStateFile_.exists())
      return Success() ;
   
   // read plot list from file
   std::vector<std::string> plots;
   Error error = readStringVectorFromFile(plotsStateFile_, &plots);
   if (error)
      return error;

   // if it is empty then return succes
   if (plots.empty())
      return Success();

   // read the storage id of the active plot them remove it from the list
   std::string activePlotStorageId ;
   if (!plots.empty())
   {
      activePlotStorageId = plots[0];
      plots.erase(plots.begin());
   }
   
   // initialize plot list
   std::string plotInfo;
   for (int i=0; i<(int)plots.size(); ++i)
   {
      std::string plotStorageId ;
      DisplaySize renderedSize(0,0);
      
      // extract the id, width, and height
      plotInfo = plots[i];
      boost::cmatch matches ;
      if (regex_utils::match(plotInfo.c_str(), matches, plotInfoRegex_) &&
          (matches.size() > 3) )
      {
         plotStorageId = matches[1];
         renderedSize.width = boost::lexical_cast<int>(matches[2]);
         renderedSize.height = boost::lexical_cast<int>(matches[3]);
      }
      
      // create next plot
      PtrPlot ptrPlot(new Plot(graphicsDevice_,
                               graphicsPath_,
                               plotStorageId,
                               renderedSize));

      // ensure it actually exists on disk before we add it
      if (ptrPlot->hasValidStorage())
      {
         plots_.push_back(ptrPlot);

         // set it as active if necessary
         if (plotStorageId == activePlotStorageId)
            activePlot_ = i;
      }
   }
   
   // if we didn't find the active plot or if it exceeds the size
   // of the circular buffer (would happen when migrating from a
   // suspended session that allowed more plots)
   if ((activePlot_ == -1) ||
       (activePlot_ > (static_cast<int>(plots_.size()) - 1)))
   {
      activePlot_ = plots_.size() - 1;
   }
   
   // restore snapshot for the active plot
   if (hasPlot())
      renderActivePlotToDisplay();

   return Success();
}

namespace {

Error copyDirectory(const FilePath& srcDir, const FilePath& targetDir)
{
   Error error = targetDir.removeIfExists();
   if (error)
      return error;

   error = targetDir.ensureDirectory();
   if (error)
      return error;

   std::vector<FilePath> srcFiles;
   error = srcDir.children(&srcFiles);
   if (error)
      return error;
   BOOST_FOREACH(const FilePath& srcFile, srcFiles)
   {
      FilePath targetFile = targetDir.complete(srcFile.filename());
      Error error = srcFile.copy(targetFile);
      if (error)
         return error;
   }

   return Success();
}

} // anonymous namespace

Error PlotManager::serialize(const FilePath& saveToPath)
{
   // save plots state
   Error error = savePlotsState();
   if (error)
      return error;

   // copy the plots dir to the save to path
   return copyDirectory(graphicsPath_, saveToPath);
}

Error PlotManager::deserialize(const FilePath& restoreFromPath)
{
   // copy the restoreFromPath to the graphics path
   Error error = copyDirectory(restoreFromPath, graphicsPath_);
   if (error)
      return error;

   // restore plots state
   return restorePlotsState();
}

   
void PlotManager::onDeviceNewPage(SEXP previousPageSnapshot)
{
   if (suppressDeviceEvents_)
      return;
   
   // if we have a plot with unrendered changes then save the previous snapshot
   if (hasPlot() && hasChanges())
   {
      if (previousPageSnapshot != R_NilValue)
      {
         r::sexp::Protect protectSnapshot(previousPageSnapshot);
         Error error = activePlot().renderFromDisplaySnapshot(
                                                         previousPageSnapshot);
         if (error)
            logAndReportError(error, ERROR_LOCATION);
      }
      else
      {
         LOG_WARNING_MESSAGE(
                     "onDeviceNewPage was not passed a previousPageSnapshot");
      }
   }

   // if we are replaying a manipulator then this plot replaces the
   // existing plot
   if (hasPlot() &&
       plotManipulatorManager().replayingManipulator())
   {
      // create plot using the active plot's manipulator
      PtrPlot ptrPlot(new Plot(graphicsDevice_,
                               graphicsPath_,
                               activePlot().manipulatorSEXP()));

      // replace active plot
      plots_[activePlotIndex()] = ptrPlot;
   }
   else
   {
      // if there is already a plot active then release its
      // in-memory resources
      if (hasPlot())
         activePlot().purgeInMemoryResources();

      // create new plot (use pending manipulator, if any)
      PtrPlot ptrPlot(new Plot(graphicsDevice_,
                               graphicsPath_,
                               plotManipulatorManager().pendingManipulatorSEXP()));

      // if we're full then remove the first plot's files before adding a new one
      if (plots_.full())
      {
         Error error = plots_.front()->removeFiles();
         if (error)
            LOG_ERROR(error);
      }

      // add the plot
      plots_.push_back(ptrPlot);
      activePlot_ = plots_.size() - 1  ;
   }

   // once we render the new plot we always reset pending manipulator state
   plotManipulatorManager().clearPendingManipulatorState();
   
   // ensure updates
   invalidateActivePlot();
}

void PlotManager::onDeviceDrawing()
{
   if (suppressDeviceEvents_)
      return;
   
   invalidateActivePlot();
}

void PlotManager::onDeviceResized()
{
   if (suppressDeviceEvents_)
      return;
   
   invalidateActivePlot();
}

void PlotManager::onDeviceClosed()
{
   if (suppressDeviceEvents_)
      return ;
   
   // clear plots
   activePlot_ = -1;
   plots_.clear();
   
   // trip changes flag to ensure repaint
   setDisplayHasChanges(true);
   
   // remove all files
   Error error = plotsStateFile_.removeIfExists();
   if (error)
      LOG_ERROR(error);

   error = graphicsPath_.removeIfExists();
   if (error)
      LOG_ERROR(error);
}   
      
Plot& PlotManager::activePlot() const
{
   return *(plots_[activePlot_]);
}
   
bool PlotManager::isValidPlotIndex(int index) const
{
   return (index >= 0) && (index < (int)plots_.size());
}
   
bool PlotManager::hasPlot() const
{
   return activePlot_ >= 0;
}

void PlotManager::setDisplayHasChanges(bool hasChanges)
{
   displayHasChanges_ = hasChanges;

   if (hasChanges)
      lastChange_ = boost::posix_time::microsec_clock::universal_time();
   else
      lastChange_ = boost::posix_time::not_a_date_time;
}

   
void PlotManager::invalidateActivePlot()
{
   setDisplayHasChanges(true);
   
   if (hasPlot())
      activePlot().invalidate();
}
   
// render active plot to display (used in setActivePlot and onSessionResume)
void PlotManager::renderActivePlotToDisplay()
{   
   suppressDeviceEvents_ = true;
   
   // attempt to render the active plot -- notify end user if there is an error
   Error error = activePlot().renderToDisplay();
   if (error)
      reportError(error);
   
   suppressDeviceEvents_ = false;
   
}
   
      
Error PlotManager::plotIndexError(int index, const ErrorLocation& location)
                                                                        const
{
   Error error(errc::InvalidPlotIndex, location);
   error.addProperty("index", index);
   return error;
}

   
std::string PlotManager::emptyImageFilename() const
{
   return "empty." + graphicsDevice_.imageFileExtension();
}

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio



