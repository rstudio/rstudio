/*
 * DesktopRCommandEvaluator.cpp
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

#include <core/StringUtils.hpp>
#include "DesktopRCommandEvaluator.hpp"

namespace rstudio {
namespace desktop {

MainWindow* RCommandEvaluator::window_;

void RCommandEvaluator::setMainWindow(MainWindow* window)
{
    window_ = window;
}

// evaluate R command
void RCommandEvaluator::evaluate(std::string rCmd)
{
    if (window_ == nullptr) return;
   rCmd = core::string_utils::jsLiteralEscape(rCmd);
   std::string js = "window.desktopHooks.evaluateRCmd(\"" + rCmd + "\")";
   window_->webPage()->runJavaScript(QString::fromStdString(js));
}

} // namespace desktop
} // namespace rstudio
