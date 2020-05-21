/*
 * ColorUtils.hpp
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

#ifndef CORE_COLOR_UTILS_HPP
#define CORE_COLOR_UTILS_HPP

#include <string>

namespace rstudio {
namespace core {
namespace color_utils {

typedef struct 
{
   double r; // percent
   double g; // percent
   double b; // percent
} rgb;

typedef struct 
{
   double h; // angle in degrees
   double s; // percent
   double v; // percent
} hsv;

rgb hsvToRGB(const hsv& in);
std::string rgbToHTML(const rgb& in);

} // namespace color_utils
} // namespace core 
} // namespace rstudio

#endif // CORE_COLOR_UTILS_HPP

