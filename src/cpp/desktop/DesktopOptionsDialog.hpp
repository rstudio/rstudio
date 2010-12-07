/*
 * DesktopOptionsDialog.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DESKTOPOPTIONSDIALOG_HPP
#define DESKTOPOPTIONSDIALOG_HPP

#include <QDialog>

#include "DesktopMainWindow.hpp"

using namespace desktop;

namespace Ui {
    class OptionsDialog;
}

class OptionsDialog : public QDialog
{
    Q_OBJECT

public:
   explicit OptionsDialog(MainWindow* parent);
   ~OptionsDialog();

protected slots:
   void chooseCRANmirror();
   void chooseRHome();
   void changeSaveWorkspace(int index);
   void updateRHome();

private:
    Ui::OptionsDialog *ui;
    MainWindow* pOwnerWindow_;
};

#endif // DESKTOPOPTIONSDIALOG_HPP
