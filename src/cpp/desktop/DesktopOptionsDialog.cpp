/*
 * DesktopOptionsDialog.cpp
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

#include "DesktopOptionsDialog.hpp"
#include "ui_DesktopOptionsDialog.h"

#include <QVBoxLayout>
#include <QMessageBox>

#include "DesktopCRANMirrorDialog.hpp"
#include "DesktopChooseRHome.hpp"
#include "DesktopOptions.hpp"
#include "DesktopDetectRHome.hpp"

using namespace desktop;

OptionsDialog::OptionsDialog(MainWindow* parent) :
      QDialog(parent),
      ui(new Ui::OptionsDialog()),
      pOwnerWindow_(parent)
{
   ui->setupUi(this);
#ifdef Q_OS_MAC
   setWindowFlags(windowFlags() | Qt::Sheet);
#endif
   this->setSizeGripEnabled(false);
   ui->buttonBox->button(QDialogButtonBox::Close)->setDefault(true);

#ifndef Q_OS_WIN32
   ui->btnRHome->setVisible(false);
   ui->lblRHome->setVisible(false);
   ui->lblRHomeValue->setVisible(false);
   this->layout()->removeItem(ui->vboxRHome);
#endif

   ui->lblRHomeRestart->setVisible(false);
   QFont smallerFont = ui->lblRHome->font();
   smallerFont.setPointSizeF(smallerFont.pointSizeF() * 0.9);
   ui->lblRHomeRestart->setFont(smallerFont);

   updateRHome();
   ui->lblCRANValue->setText(options().defaultCRANmirrorName());

   ui->comboSaveWorkspace->setCurrentIndex(
         options().saveWorkspaceOnExit());

   connect(ui->btnCRAN, SIGNAL(clicked()),
           this, SLOT(chooseCRANmirror()));
   connect(ui->btnRHome, SIGNAL(clicked()),
           this, SLOT(chooseRHome()));
   connect(ui->comboSaveWorkspace, SIGNAL(currentIndexChanged(int)),
           this, SLOT(changeSaveWorkspace(int)));
}

OptionsDialog::~OptionsDialog()
{
   delete ui;
}

void OptionsDialog::chooseCRANmirror()
{
   CRANMirrorDialog dialog(this->parentWidget());
   if (dialog.exec() == QDialog::Accepted)
   {
      ui->lblCRANValue->setText(dialog.selectedName());
      options().setDefaultCRANmirror(dialog.selectedName(),
                                               dialog.selectedURL());
   }
}

void OptionsDialog::chooseRHome()
{
#ifdef _WIN32
   RVersion rVersion = desktop::detectRVersion(true, this);

   if (rVersion.isValid())
   {
      updateRHome();
      ui->lblRHomeRestart->setVisible(true);
   }

#endif
}

void OptionsDialog::updateRHome()
{
#ifdef _WIN32
   bool defaulted = options().rBinDir().isEmpty();
   QString rDesc = defaulted
                   ? QString("[Default] ") + autoDetect().description()
                   : RVersion(options().rBinDir()).description();
   ui->lblRHomeValue->setText(rDesc);
#endif
}

void OptionsDialog::changeSaveWorkspace(int index)
{
   options().setSaveWorkspaceOnExit(index);
   pOwnerWindow_->setSaveWorkspace(index);
}
