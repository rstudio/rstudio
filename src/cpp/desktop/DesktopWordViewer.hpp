/*
 * DesktopWordViewer.hpp
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
#ifndef DESKTOPWORDVIEWER_HPP
#define DESKTOPWORDVIEWER_HPP

#include "DesktopOfficeViewer.hpp"

namespace rstudio {
namespace desktop {

class WordViewer : public OfficeViewer
{
public:
   WordViewer();

   core::Error savePosition(IDispatch* source) override;
   core::Error restorePosition(IDispatch* target) const override;
   void resetPosition() override;
   bool hasPosition() const override;

private:
   int docScrollX_;
   int docScrollY_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOPWORDVIEWER_HPP
