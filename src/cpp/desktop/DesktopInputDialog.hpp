/*
 * DesktopInputDialog.hpp
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

#ifndef DESKTOPINPUTDIALOG_HPP
#define DESKTOPINPUTDIALOG_HPP

#include <QDialog>
#include <QLineEdit>

namespace Ui {
    class InputDialog;
}

class InputDialog : public QDialog
{
    Q_OBJECT

public:
    explicit InputDialog(QWidget *parent = nullptr);
    ~InputDialog() override;

    QString caption();
    void setCaption(const QString& caption);
    QString textValue();
    void setTextValue(const QString& value);
    void setSelection(int offset, int length);
    void setOkButtonLabel(const QString& label);
    void setEchoMode(QLineEdit::EchoMode mode);
    void setNumbersOnly(bool numbersOnly);
    void setExtraOptionPrompt(const QString& prompt);
    void setRequired(bool required);

    void setExtraOption(bool extraOption);
    bool extraOption();

    void done(int r) override;

private:
    Ui::InputDialog *ui;
    QPushButton* pOK_;
    bool required_;
};

#endif // DESKTOPINPUTDIALOG_HPP
