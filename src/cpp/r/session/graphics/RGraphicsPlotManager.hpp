/*
 * RGraphicsPlotManager.hpp
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

#ifndef R_SESSION_GRAPHICS_PLOT_MANAGER_HPP
#define R_SESSION_GRAPHICS_PLOT_MANAGER_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/signal.hpp>
#include <boost/regex.hpp>
#include <boost/circular_buffer.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <r/session/RGraphics.hpp>

#include "RGraphicsTypes.hpp"
#include "RGraphicsPlot.hpp"

namespace r {
namespace session {
namespace graphics {

// singleton
class PlotManager;
PlotManager& plotManager();   

struct GraphicsDeviceEvents
{
   boost::signal<void (SEXP)> onNewPage; 
   boost::signal<void ()> onDrawing;
   boost::signal<void ()> onResized;
   boost::signal<void ()> onClosed;
};

class PlotManipulatorManager;
 
class PlotManager : boost::noncopyable, public r::session::graphics::Display
{   
private:
   PlotManager();
   friend PlotManager& plotManager();
   
public:
   virtual ~PlotManager() {}
   
   rscore::Error initialize(const rscore::FilePath& graphicsPath,
                          const GraphicsDeviceFunctions& graphicsDevice,
                          GraphicsDeviceEvents* pEvents);
   
   // plot list
   virtual int plotCount() const;
   virtual rscore::Error plotImageFilename(int index,
                                         std::string* pImageFilename) const;
   virtual int activePlotIndex() const;
   virtual rscore::Error setActivePlot(int index) ;
   virtual rscore::Error removePlot(int index);
   
   // actions on active plot
   virtual rscore::Error savePlotAsImage(const rscore::FilePath& filePath,
                                       const std::string& format,
                                       int widthPx,
                                       int heightPx);

   virtual rscore::Error savePlotAsPdf(const rscore::FilePath& filePath,
                                     double widthInches,
                                     double heightInches,
                                     bool useCairoPdf);

   virtual rscore::Error savePlotAsMetafile(const rscore::FilePath& filePath,
                                          int widthPx,
                                          int heightPx);

   // display
   virtual bool hasOutput() const;
   virtual bool hasChanges() const;
   virtual bool isActiveDevice() const;
   virtual boost::posix_time::ptime lastChange() const;
   virtual void render(boost::function<void(DisplayState)> outputFunction); 
   virtual std::string imageFilename() const ;
   virtual void refresh() ;
   
    // retrieve image path based on filename
   virtual rscore::FilePath imagePath(const std::string& imageFilename) const;
   
   virtual void clear();

   virtual boost::signal<void ()>& onShowManipulator() ;
   virtual void setPlotManipulatorValues(const rscore::json::Object& values);
   virtual void manipulatorPlotClicked(int x, int y);

   virtual void onBeforeExecute();

   // manipulate persistent state
   rscore::Error savePlotsState();
   rscore::Error restorePlotsState();

   // fully serialize and deserialize to an external directory
   rscore::Error serialize(const rscore::FilePath& saveToPath);
   rscore::Error deserialize(const rscore::FilePath& restoreFromPath);
      
private:
   
   // make plot manipulator manager a friend
   friend class PlotManipulatorManager;

   // typedefs
   typedef boost::shared_ptr<Plot> PtrPlot ;

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
   rscore::Error savePlotAsFile(const boost::function<rscore::Error()>&
                                                         deviceCreationFunction);
   rscore::Error savePlotAsFile(const std::string& fileDeviceCreationCode);

   rscore::Error savePlotAsBitmapFile(const rscore::FilePath& targetPath,
                                    const std::string& bitmapFileType,
                                    int width,
                                    int height);

   rscore::Error savePlotAsSvg(const rscore::FilePath& targetPath,
                             int width,
                             int height);

   rscore::Error savePlotAsPostscript(const rscore::FilePath& targetPath,
                                    int width,
                                    int height);

   
   // error helpers
   rscore::Error plotIndexError(int index, const rscore::ErrorLocation& location)
                                                                         const;

   std::string emptyImageFilename() const ;

private:   
   friend class SuppressDeviceEventsScope;

   // storage paths
   rscore::FilePath plotsStateFile_;
   rscore::FilePath graphicsPath_;
  
   // interface to graphics device
   GraphicsDeviceFunctions graphicsDevice_ ;
   
   // state
   bool displayHasChanges_;
   boost::posix_time::ptime lastChange_;
   bool suppressDeviceEvents_;
   
   int activePlot_;
   boost::circular_buffer<PtrPlot> plots_ ;
   
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

#endif // R_SESSION_GRAPHICS_PLOT_MANAGER_HPP 

