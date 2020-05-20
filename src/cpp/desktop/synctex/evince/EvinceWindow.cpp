/*
 * EvinceWindow.cpp
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

#include "EvinceWindow.hpp"

namespace rstudio {
namespace desktop {
namespace synctex {

EvinceWindow::EvinceWindow(const SynctexViewerInfo& viewerInfo,
                           const QString &service,
                           QObject *parent)
    : QDBusAbstractInterface(service,
                             QString::fromUtf8("/org/gnome/evince/Window/0"),
                             staticInterfaceName(),
                             QDBusConnection::sessionBus(),
                             parent),
      viewerInfo_(viewerInfo)
{
}

EvinceWindow::~EvinceWindow()
{
}

} // namespace synctex
} // namespace desktop
} // namespace rstudio

