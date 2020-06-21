/*
 * RGraphicsPlotManager.hpp
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

#ifndef R_SESSION_GRAPHICS_PLOT_MANAGER_HPP
#define R_SESSION_GRAPHICS_PLOT_MANAGER_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/regex.hpp>
#include <boost/circular_buffer.hpp>

#include <core/BoostSignals.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <r/session/RGraphics.hpp>

#include "RGraphicsTypes.hpp"
#include "RGraphicsPlot.hpp"

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

// singleton
class PlotManager;
PlotManager& plotManager();

struct GraphicsDeviceEvents
{
   RSTUDIO_BOOST_SIGNAL<void (SEXP)> onNewPage;
   RSTUDIO_BOOST_SIGNAL<void ()> onDrawing;
   RSTUDIO_BOOST_SIGNAL<void ()> onResized;
   RSTUDIO_BOOST_SIGNAL<void ()> onClosed;
};

class PlotManipulatorManager;
 
class PlotManager : boost::noncopyable, public r::session::graphics::Display
{   
private:
   PlotManager();
   friend PlotManager& plotManager();
   
public:
   virtual ~PlotManager() {}
   
   core::Error initialize(const core::FilePath& graphicsPath,
                          const GraphicsDeviceFunctions& graphicsDevice,
                          GraphicsDeviceEvents* pEvents);
   
   // plot list
   virtual int plotCount() const;
   virtual core::Error plotImageFilename(int index, 
                                         std::string* pImageFilename) const;
   virtual int activePlotIndex() const;
   virtual core::Error setActivePlot(int index);
   virtual core::Error removePlot(int index);
   
   // actions on active plot
   virtual core::Error savePlotAsImage(const core::FilePath& filePath,
                                       const std::string& format,
                                       int widthPx,
                                       int heightPx,
                                       bool useDevicePixelRatio = false);

   virtual core::Error savePlotAsImage(const core::FilePath& filePath,
                                       const std::string& format,
                                       int widthPx,
                                       int heightPx,
                                       double devicePixelRatio);

   virtual core::Error savePlotAsPdf(const core::FilePath& filePath,
                                     double widthInches,
                                     double heightInches,
                                     bool useCairoPdf);

   virtual core::Error savePlotAsMetafile(const core::FilePath& filePath,
                                          int widthPx,
                                          int heightPx);

   // display
   virtual bool hasOutput() const;
   virtual bool hasChanges() const;
   virtual bool isActiveDevice() const;
   virtual boost::posix_time::ptime lastChange() const;
   virtual void render(boost::function<void(DisplayState)> outputFunction);
   virtual std::string imageFilename() const;
   virtual void refresh();
   
    // retrieve image path based on filename
   virtual core::FilePath imagePath(const std::string& imageFilename) const;
   
   virtual void clear();

   virtual RSTUDIO_BOOST_SIGNAL<void ()>& onShowManipulator();
   virtual void setPlotManipulatorValues(const core::json::Object& values);
   virtual void manipulatorPlotClicked(int x, int y);

   virtual void onBeforeExecute();

   // manipulate persistent state
   core::Error savePlotsState();
   core::Error restorePlotsState();

   // fully serialize and deserialize to an external directory
   core::Error serialize(const core::FilePath& saveToPath);
   core::Error deserialize(const core::FilePath& restoreFromPath);
      
private:
   
   // make plot manipulator manager a friend
   friend class PlotManipulatorManager;

   // typedefs
   typedef boost::shared_ptr<Plot> PtrPlot;

   // device events
   void onDeviceNewPage(SEXP previousPageSnapshot);
   void onDeviceDrawing();
   void onDeviceResized();
   void onDeviceClosed();
   
   // active plot 
   Plot& activePlot() const;
   bool isValidPlotIndex(int index) const;
   bool hasPlot() const;
   
   // set change flag
   void setDisplayHasChanges(bool hasChanges);

   // invalidate the active plot
   void invalidateActivePlot();

   // render active plot to display (used in setActivePlot and onSessionResume)
   void renderActivePlotToDisplay();
   
   // render active plot file file
   core::Error savePlotAsFile(const boost::function<core::Error()>&
                                                         deviceCreationFunction);
   core::Error savePlotAsFile(const std::string& fileDeviceCreationCode);

   core::Error savePlotAsBitmapFile(const core::FilePath& targetPath,
                                    const std::string& bitmapFileType,
                                    int width,
                                    int height,
                                    double pixelRatio);

   core::Error savePlotAsSvg(const core::FilePath& targetPath,
                             int width,
                             int height);

   core::Error savePlotAsPostscript(const core::FilePath& targetPath,
                                    int width,
                                    int height);

   
   // error helpers
   core::Error plotIndexError(int index, const core::ErrorLocation& location)
                                                                         const;

   std::string emptyImageFilename() const;

private:   
   friend class SuppressDeviceEventsScope;

   // storage paths
   core::FilePath plotsStateFile_;
   core::FilePath graphicsPath_;
  
   // interface to graphics device
   GraphicsDeviceFunctions graphicsDevice_;
   
   // state
   bool displayHasChanges_;
   boost::posix_time::ptime lastChange_;
   bool suppressDeviceEvents_;
   
   int activePlot_;
   boost::circular_buffer<PtrPlot> plots_;
   
   boost::regex plotInfoRegex_;
};

class SuppressDeviceEventsScope
{
public:
   SuppressDeviceEventsScope(PlotManager& plotManager)
      : plotManager_(plotManager)
   {
      plotManager_.suppressDeviceEvents_ = true;
   }

   virtual ~SuppressDeviceEventsScope()
   {
      plotManager_.suppressDeviceEvents_ = false;
   }
private:
   PlotManager& plotManager_;
};


} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_GRAPHICS_PLOT_MANAGER_HPP 

