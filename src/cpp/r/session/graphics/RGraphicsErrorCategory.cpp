/*
 * RGraphicsErrorCategory.cpp
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

#include <r/session/RGraphics.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

class RGraphicsErrorCategory : public boost::system::error_category
{
public:
   virtual const char * name() const BOOST_NOEXCEPT;
   virtual std::string message( int ev ) const;
};

const boost::system::error_category& rGraphicsCategory()
{
   static RGraphicsErrorCategory rGraphicsErrorCategoryConst;
   return rGraphicsErrorCategoryConst;
}

const char * RGraphicsErrorCategory::name() const BOOST_NOEXCEPT
{
   return "r-graphics";
}

std::string RGraphicsErrorCategory::message( int ev ) const
{
   std::string message;
   switch (ev)
   {
      case errc::IncompatibleGraphicsEngine:
         message = "Incompatible graphics engine";
         break;

      case errc::DeviceNotAvailable:
         message = "Device not available";
         break;
         
      case errc::NoActivePlot:
         message = "No active plot";
         break;
         
      case errc::InvalidPlotIndex:
         message = "Invalid plot index";
         break;
         
      case errc::InvalidPlotImageType:
         message = "Invalid plot image type";
         break;
      
      case errc::PlotRenderingError:
         message = "Plot rendering error";
         break;
         
      case errc::PlotFileError:
         message = "Plot file error";
         break;
         
      default:
         message = "Unknown error";
         break;
   }

   return message;
}
   
} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio
