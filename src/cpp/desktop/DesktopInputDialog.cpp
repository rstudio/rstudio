/*
 * DesktopInputDialog.cpp
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

#include "DesktopInputDialog.hpp"
#include "ui_DesktopInputDialog.h"

#include <QPushButton>

InputDialog::InputDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::InputDialog())
{
   ui->setupUi(this);
   setWindowFlags(Qt::Dialog | Qt::MSWindowsFixedSizeDialogHint);
}

InputDialog::~InputDialog()
{
   delete ui;
}

QString InputDialog::caption()
{
   return ui->label->text();
}

void InputDialog::setCaption(QString caption)
{
   ui->label->setText(caption);
}

QString InputDialog::textValue()
{
   return ui->lineEdit->text();
}

void InputDialog::setTextValue(QString value)
{
   ui->lineEdit->setText(value);
}

void InputDialog::setSelection(int offset, int length)
{
   offset = std::min(offset, textValue().size());
   length = std::min(length,
                     textValue().size() - offset);

   ui->lineEdit->setSelection(offset, length);
}

void InputDialog::setOkButtonLabel(QString label)
{
   QPushButton* pBtn = ui->buttonBox->button(QDialogButtonBox::Ok);
   pBtn->setText(label);
}
