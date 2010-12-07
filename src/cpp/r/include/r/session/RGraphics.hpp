/*
 * RGraphics.hpp
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

#ifndef R_SESSION_GRAPHICS_HPP
#define R_SESSION_GRAPHICS_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>

#include <core/Error.hpp>

namespace core {
   class FilePath;
}

namespace r {
namespace session {
namespace graphics {
   
struct DisplayState
{
   DisplayState(const std::string& imageFilename, 
                int width,
                int height,
                int activePlotIndex,
                int plotCount)
      : imageFilename(imageFilename), 
        width(width),
        height(height),
        activePlotIndex(activePlotIndex),
        plotCount(plotCount)
   {
   }
   
   std::string imageFilename;
   int width;
   int height;
   int activePlotIndex;
   int plotCount;
};

class Display
{
public:
   virtual ~Display() {}
   
   // plot list
   virtual int plotCount() const = 0 ;
   virtual core::Error plotImageFilename(int index, 
                                         std::string* pImageFilename) const = 0;
   virtual int activePlotIndex() const = 0;
   virtual core::Error setActivePlot(int index) = 0;
   virtual core::Error removePlot(int index) = 0;

   // capabilities
   virtual bool supportsSvg() = 0;
   
   // actions on active plot   
   virtual core::Error savePlotAsPng(const core::FilePath& filePath,
                                     int widthPx,
                                     int heightPx) = 0;

   virtual core::Error savePlotAsSvg(const core::FilePath& filePath,
                                     int widthPx,
                                     int heightPx) = 0;
   
   virtual core::Error savePlotAsPdf(const core::FilePath& filePath,
                                     double widthInches,
                                     double heightInches) = 0;
      
   // display
   virtual bool hasOutput() const = 0 ;
   virtual bool hasChanges() const = 0 ;
   virtual void render(boost::function<void(DisplayState)> outputFunction)=0; 
   virtual std::string imageFilename() const = 0 ;
   virtual void refresh() = 0;

   // retrieve image path based on filename
   virtual core::FilePath imagePath(const std::string& imageFilename) const = 0;
   
   // clear the display (closes the device)
   virtual void clear() = 0;
};
   
// singleton
Display& display();

const boost::system::error_category& rGraphicsCategory() ;

namespace errc {
   
enum errc_t 
{
   Success = 0,
   IncompatibleGraphicsEngine,
   DeviceNotAvailable,
   NoActivePlot,
   InvalidPlotIndex,
   InvalidPlotImageType,
   PlotRenderingError,
   PlotFileError
};

inline boost::system::error_code make_error_code( errc_t e )
{
   return boost::system::error_code( e, rGraphicsCategory() ); }

inline boost::system::error_condition make_error_condition( errc_t e )
{
   return boost::system::error_condition( e, rGraphicsCategory() );
}
   
} // namespace errc
   
} // namespace graphics
} // namespace session
} // namespace r

namespace boost {
namespace system {
template <>
struct is_error_code_enum<r::session::graphics::errc::errc_t>
 { static const bool value = true; };
} // namespace system
} // namespace boost

#endif // R_SESSION_GRAPHICS_HPP 

