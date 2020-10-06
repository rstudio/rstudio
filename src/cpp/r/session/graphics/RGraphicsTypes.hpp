/*
 * RGraphicsTypes.hpp
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

#ifndef R_SESSION_GRAPHICS_TYPES_HPP
#define R_SESSION_GRAPHICS_TYPES_HPP

#include <boost/utility.hpp>
#include <boost/format.hpp>
#include <boost/function.hpp>

typedef struct SEXPREC *SEXP;

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

struct DisplaySize
{
   DisplaySize(int width, int height) : width(width), height(height) {}
   DisplaySize() : width(0), height(0) {}
   int width;
   int height;
   
   bool operator==(const DisplaySize& other) const
   {
      return width == other.width &&
             height == other.height;
   }
   
   bool operator!=(const DisplaySize& other) const
   {
      return !(*this == other);
   }
};

typedef boost::function<void(double*,double*)> UnitConversionFunction;

struct UnitConversionFunctions
{
   UnitConversionFunction deviceToUser;
   UnitConversionFunction deviceToNDC;
};

struct GraphicsDeviceFunctions
{
   boost::function<bool()> isActive;
   boost::function<DisplaySize()> displaySize;
   UnitConversionFunctions convert;
   boost::function<core::Error(const core::FilePath&,
                               const core::FilePath&)> saveSnapshot;
   boost::function<core::Error(const core::FilePath&)> restoreSnapshot;
   boost::function<void()> copyToActiveDevice;
   boost::function<std::string()> imageFileExtension;
   boost::function<void()> close;
   boost::function<void()> onBeforeExecute;
};


} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_SESSION_GRAPHICS_TYPES_HPP 

