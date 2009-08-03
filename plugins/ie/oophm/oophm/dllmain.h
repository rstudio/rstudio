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

// dllmain.h : Declaration of module class.

class CoophmModule : public CAtlDllModuleT< CoophmModule >
{
public :
	DECLARE_LIBID(LIBID_oophmLib)
	DECLARE_REGISTRY_APPID_RESOURCEID(IDR_OOPHM, "{F9365E53-5A14-47F3-BF1D-10CAAB815309}")
};

extern class CoophmModule _AtlModule;
