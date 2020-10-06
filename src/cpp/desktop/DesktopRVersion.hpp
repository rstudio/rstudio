/*
 * DesktopRVersion.hpp
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
#ifndef DESKTOPRVERSION_HPP
#define DESKTOPRVERSION_HPP

#include <iostream>
#include <fstream>

#include <QtCore>
#include <QWidget>

#include "desktop-config.h"

namespace rstudio {
namespace desktop {

enum Architecture
{
   ArchNone = 1,
   ArchX86 = 2,
   ArchX64 = 4,
   ArchUnknown = 0x100
};
typedef int Architectures;

enum ValidateResult
{
   ValidateSuccess = 1,
   ValidateNotFound = 2,
   ValidateBadArchitecture = 4,
   ValidateVersionTooOld = 8
};

class RVersion
{
public:
   RVersion(QString binDir=QString());

   QString binDir() const;
   QString homeDir() const;
   QString description() const;
   bool isEmpty() const;
   bool isValid() const;
   ValidateResult validate() const;
   quint32 version() const;
   Architecture architecture() const;
   int compareTo(const RVersion& other) const;
   bool operator<(const RVersion& other) const;
   bool operator==(const RVersion& other) const;

private:
   void stat();

   QString binDir_;
   QString homeDir_;
   bool loaded_;
   quint32 version_;
   Architecture arch_;
};

QString binDirToHomeDir(QString binDir);

// Detect versions that might be implied by a user-specified dir.
// The versions that are returned might not be valid.
QList<RVersion> detectVersionsInDir(QString dir);

// Enumerates all valid versions of R that are detected on the system.
// The versions parameter can be used to explicitly provide one or more
// R versions that may not be detected.
QList<RVersion> allRVersions(QList<RVersion> versions=QList<RVersion>());

RVersion autoDetect(Architecture architecture, bool preferredOnly=false);
RVersion autoDetect();

RVersion detectRVersion(bool forceUi,
                        QWidget* parent = nullptr);

} // namespace desktop
} // namespace rstudio

#endif // DESKTOPRVERSION_HPP
