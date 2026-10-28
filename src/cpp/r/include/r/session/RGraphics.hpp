/*
 * RGraphics.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
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

#include <boost/system/error_code.hpp>
#include <boost/date_time/posix_time/ptime.hpp>

#include <core/BoostSignals.hpp>

#include "RGraphicsConstants.h"

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
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

} // namespace errc
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio

namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {
template <>
struct is_error_code_enum<rstudio::r::session::graphics::errc::errc_t>
 { static const bool value = true; };
} // namespace system
} // namespace boost


#include <string>

#include <boost/function.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/BoostSignals.hpp>

namespace rstudio {
namespace core {
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

std::string getDefaultBackend();
std::string getDefaultAntialiasing();

namespace device {

enum DeviceType
{
   DeviceTypeAGG,
   DeviceTypeCairo,
   DeviceTypeQuartz,
   DeviceTypeWindows,
   DeviceTypeUnknown,
};

DeviceType activeDeviceType();
int getWidth();
int getHeight();
double devicePixelRatio();

// Draw at the given size (in pixels at 96 DPI) regardless of the size of the
// Plots pane; a non-positive width or height follows the pane again.
void setFixedSize(int width, int height);
bool hasFixedSize();
}
   
struct DisplayState
{
   DisplayState(const std::string& imageFilename, 
                const core::json::Value& manipulatorJson,
                int width,
                int height,
                int activePlotIndex,
                int plotCount,
                bool fixedSize,
                const std::string& altText,
                bool activePlotChanged)
      : imageFilename(imageFilename), 
        manipulatorJson(manipulatorJson),
        width(width),
        height(height),
        activePlotIndex(activePlotIndex),
        plotCount(plotCount),
        fixedSize(fixedSize),
        altText(altText),
        activePlotChanged(activePlotChanged)
   {
   }
   
   std::string imageFilename;
   core::json::Value manipulatorJson;
   int width;
   int height;
   int activePlotIndex;
   int plotCount;
   bool fixedSize;

   // alt text the plot's author supplied, or empty
   std::string altText;

   // whether a different plot is shown than at the previous render, as
   // opposed to the same plot redrawn (e.g. after a resize)
   bool activePlotChanged;
};

extern const char * const kPngFormat;
extern const char * const kJpegFormat;
extern const char * const kTiffFormat;
extern const char * const kBmpFormat;
extern const char * const kMetafileFormat;
extern const char * const kSvgFormat;
extern const char * const kPostscriptFormat;

// How saved bitmaps are encoded; the defaults match the Save Plot as Image
// dialog's.
struct BitmapOptions
{
   // a compression as named by grDevices::tiff()'s compression argument
   // (which ragg's agg_tiff() shares); "none" leaves the TIFF uncompressed
   std::string tiffCompression = "lzw";

   // the quality of a JPEG, from 1 to 100
   int jpegQuality = 100;
};

// The compressions offered when a plot is saved as a TIFF, as named by
// grDevices::tiff()'s compression argument.
std::vector<std::string> tiffCompressionTypes();

class Display
{
public:
   virtual ~Display() {}
   
   // plot list
   virtual int plotCount() const = 0;
   virtual core::Error plotImageFilename(int index, 
                                         std::string* pImageFilename) const = 0;
   virtual int activePlotIndex() const = 0;
   virtual core::Error setActivePlot(int index) = 0;
   virtual core::Error removePlot(int index) = 0;

   // actions on active plot; recordResolution makes sure a bitmap records
   // the resolution it was drawn at, which only matters for files that leave
   // RStudio (and costs a rewrite on macOS), and options say how a TIFF or
   // JPEG is encoded
   virtual core::Error savePlotAsImage(const core::FilePath& filePath,
                                       const std::string& format,
                                       int widthPx,
                                       int heightPx,
                                       bool useDevicePixelRatio,
                                       bool recordResolution = false,
                                       const BitmapOptions& options = BitmapOptions()) = 0;

   virtual core::Error savePlotAsImage(const core::FilePath& filePath,
                                       const std::string& format,
                                       int widthPx,
                                       int heightPx,
                                       double devicePixelRatio,
                                       bool recordResolution = false,
                                       const BitmapOptions& options = BitmapOptions()) = 0;

   virtual core::Error savePlotAsPdf(const core::FilePath& filePath,
                                     double widthInches,
                                     double heightInches,
                                     bool useCairoPdf) = 0;

   virtual core::Error savePlotAsMetafile(const core::FilePath& filePath,
                                          int widthPx,
                                          int heightPx) = 0;
      
   // display
   virtual bool hasOutput() const = 0;
   virtual bool hasChanges() const = 0;
   virtual bool isActiveDevice() const = 0;
   virtual boost::posix_time::ptime lastChange() const = 0;
   virtual void render(boost::function<void(DisplayState)> outputFunction)=0;
   virtual std::string imageFilename() const = 0;
   virtual void refresh() = 0;

   // retrieve image path based on filename
   virtual core::FilePath imagePath(const std::string& imageFilename) const = 0;
   
   // clear the display (closes the device)
   virtual void clear() = 0;

   // subscribe to showManipulator event
   virtual RSTUDIO_BOOST_SIGNAL<void ()>& onShowManipulator() = 0;

   // set manipulator values
   virtual void setPlotManipulatorValues(const core::json::Object& values) = 0;
   virtual void manipulatorPlotClicked(int x, int y) = 0;

   // notify that we are about to execute code
   virtual void onBeforeExecute() = 0;
};
   
// singleton
Display& display();

const boost::system::error_category& rGraphicsCategory();

std::string extraBitmapParams();

namespace errc {
   
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
} // namespace rstudio

#endif // R_SESSION_GRAPHICS_HPP 

