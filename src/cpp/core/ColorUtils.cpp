/*
 * ColorUtils.cpp
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

#include <core/ColorUtils.hpp>

#include <boost/format.hpp>

namespace rstudio {
namespace core {
namespace color_utils {

// HSV/RGB conversion code originally via
// http://stackoverflow.com/a/6930407/347549; modified to remove C-style casts,
// fix formatting, etc.

rgb hsvToRGB(const hsv& in)
{
   double hh, p, q, t, ff;
   long   i;
   rgb    out;

   if (in.s <= 0.0) 
   {
      out.r = in.v;
      out.g = in.v;
      out.b = in.v;
      return out;
   }

   hh = in.h;
   if (hh >= 360.0) 
      hh = 0.0;
   hh /= 60.0;
   i = static_cast<long>(hh);
   ff = hh - i;
   p = in.v * (1.0 - in.s);
   q = in.v * (1.0 - (in.s * ff));
   t = in.v * (1.0 - (in.s * (1.0 - ff)));

   switch (i) 
   {
   case 0:
      out.r = in.v;
      out.g = t;
      out.b = p;
      break;
   case 1:
      out.r = q;
      out.g = in.v;
      out.b = p;
      break;
   case 2:
      out.r = p;
      out.g = in.v;
      out.b = t;
      break;
   case 3:
      out.r = p;
      out.g = q;
      out.b = in.v;
      break;
   case 4:
      out.r = t;
      out.g = p;
      out.b = in.v;
      break;
   case 5:
   default:
      out.r = in.v;
      out.g = p;
      out.b = q;
      break;
   }
   return out;
}

// given RGB values, convert to an HTML color string
std::string rgbToHTML(const rgb& in)
{
   return (boost::format("#%02X%02X%02X") %
      static_cast<int>(in.r * 255.0) %
      static_cast<int>(in.g * 255.0) %
      static_cast<int>(in.b * 255.0)).str();
}

} // namespace color_utils
} // namespace core 
} // namespace rstudio



