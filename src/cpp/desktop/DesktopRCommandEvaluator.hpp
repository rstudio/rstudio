/*
 * DesktopRCommandEvaluator.hpp
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

#ifndef DESKTOP_R_COMMAND_EVALUATOR_HPP
#define DESKTOP_R_COMMAND_EVALUATOR_HPP

#include "DesktopMainWindow.hpp"

namespace rstudio {
namespace desktop {

class RCommandEvaluator
{
    public:
        static void setMainWindow(MainWindow* window);
        static void evaluate(std::string rCmd);

    private:
        static MainWindow* window_;
};

} // namespace desktop
} // namespace rstudio

#endif
