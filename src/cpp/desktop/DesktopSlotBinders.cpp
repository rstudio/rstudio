/*
 * DesktopSlotBinders.cpp
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

#include "DesktopSlotBinders.hpp"

#include <QtCore>

namespace rstudio {
namespace desktop {

StringSlotBinder::StringSlotBinder(QString arg, QObject *parent) :
    QObject(parent), arg_(arg)
{
}

void StringSlotBinder::trigger()
{
   triggered(arg_);
}

} // namespace desktop
} // namespace rstudio
