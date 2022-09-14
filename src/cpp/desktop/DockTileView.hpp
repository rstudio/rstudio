/*
 * DockTileView.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DESKTOP_DOCK_TILE_VIEW_HPP
#define DESKTOP_DOCK_TILE_VIEW_HPP

#include <QString>

namespace rstudio {
namespace desktop {

class DockTileView
{
public:
   static void setLabel(const QString& label);
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_DOCK_TILE_VIEW_HPP
