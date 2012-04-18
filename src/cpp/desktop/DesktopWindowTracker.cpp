/*
 * DesktopWindowTracker.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopWindowTracker.hpp"

#include "DesktopSlotBinders.hpp"

namespace desktop {

WindowTracker::WindowTracker(QObject *parent) :
    QObject(parent)
{
}

BrowserWindow* WindowTracker::getWindow(QString key)
{
   return map_.value(key, NULL);
}

void WindowTracker::addWindow(QString key, BrowserWindow* window)
{
   map_.insert(key, window);

   // Freed by signal
   StringSlotBinder* stringSlotBinder = new StringSlotBinder(key, this);
   connect(stringSlotBinder, SIGNAL(triggered(QString)),
           stringSlotBinder, SLOT(deleteLater()));

   connect(window, SIGNAL(destroyed()),
           stringSlotBinder, SLOT(trigger()));

   connect(stringSlotBinder, SIGNAL(triggered(QString)),
           this, SLOT(onWindowDestroyed(QString)));
}

void WindowTracker::onWindowDestroyed(QString key)
{
   map_.remove(key);
}

} // namespace desktop
