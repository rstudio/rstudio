/*
 * Copyright 2008 Google Inc.
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

// dllmain.cpp : Implementation of DllMain.
#include "stdafx.h"
#include "resource.h"
#include "oophm_i.h"
#include "dllmain.h"
#include "dlldatax.h"
#include "AllowDialog.h"

CoophmModule _AtlModule;

// DLL Entry Point
extern "C" BOOL WINAPI DllMain(HINSTANCE hInstance, DWORD dwReason, LPVOID lpReserved)
{
#ifdef _MERGE_PROXYSTUB
        if (!PrxDllMain(hInstance, dwReason, lpReserved))
                return FALSE;
#endif
        DisableThreadLibraryCalls(hInstance);

        AllowDialog::setHInstance(hInstance);

        return _AtlModule.DllMain(dwReason, lpReserved); 
}
