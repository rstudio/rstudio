/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

#include "AllowDialog.h"
#include "Debug.h"
#include "resource.h"

HINSTANCE AllowDialog::hInstance;

static BOOL CALLBACK allowDialogProc(HWND hwndDlg, UINT message, WPARAM wParam, LPARAM lParam) {
  if (message != WM_COMMAND) {
    return false;
  }
  bool allowed;
  switch (LOWORD(wParam)) {
    case IDCANCEL:
      allowed = false;
      break;
    case IDC_ALLOW_BUTTON:
      allowed = true;
      break;
    default:
      // ignore anything but buttons which close the dialog
      return false;
  }
  bool remember = IsDlgButtonChecked(hwndDlg, IDC_REMEMBER_CHECKBOX) == BST_CHECKED;
  int returnVal = (allowed ? 1 : 0) + (remember ? 2 : 0);
  EndDialog(hwndDlg, (INT_PTR) returnVal);
  return true;
}

void AllowDialog::setHInstance(HINSTANCE hInstance) {
  AllowDialog::hInstance = hInstance;
}

bool AllowDialog::askUserToAllow(bool* remember) {
  int result = (int) DialogBox(hInstance, MAKEINTRESOURCE(IDD_ALLOW_DIALOG),
      NULL, (DLGPROC) allowDialogProc);
  *remember = (result & 2) != 0;
  return (result & 1) != 0;
}
