/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Modified by Google
package org.eclipse.swt.internal;

public class Library {

	/* SWT Version - Mmmm (M=major, mmm=minor) */
	
	/**
	 * SWT Major version number (must be >= 0)
	 */
    static int MAJOR_VERSION = 3;
	
	/**
	 * SWT Minor version number (must be in the range 0..999)
	 */
    static int MINOR_VERSION = 235;
	
	/**
	 * SWT revision number (must be >= 0)
	 */
	static int REVISION = 0;
	
	/**
	 * The JAVA and SWT versions
	 */
	public static final int JAVA_VERSION, SWT_VERSION;

static {
	JAVA_VERSION = parseVersion(System.getProperty("java.version"));
	SWT_VERSION = SWT_VERSION(MAJOR_VERSION, MINOR_VERSION);
}

static int parseVersion(String version) {
	if (version == null) return 0;
	int major = 0, minor = 0, micro = 0;
	int length = version.length(), index = 0, start = 0;
	while (index < length && Character.isDigit(version.charAt(index))) index++;
	try {
		if (start < length) major = Integer.parseInt(version.substring(start, index));
	} catch (NumberFormatException e) {}
	start = ++index;
	while (index < length && Character.isDigit(version.charAt(index))) index++;
	try {
		if (start < length) minor = Integer.parseInt(version.substring(start, index));
	} catch (NumberFormatException e) {}
	start = ++index;
	while (index < length && Character.isDigit(version.charAt(index))) index++;
	try {
		if (start < length) micro = Integer.parseInt(version.substring(start, index));
	} catch (NumberFormatException e) {}
	return JAVA_VERSION(major, minor, micro);
}

/**
 * Returns the Java version number as an integer.
 * 
 * @param major
 * @param minor
 * @param micro
 * @return the version
 */
public static int JAVA_VERSION (int major, int minor, int micro) {
	return (major << 16) + (minor << 8) + micro;
}

/**
 * Returns the SWT version number as an integer.
 * 
 * @param major
 * @param minor
 * @return the version
 */
public static int SWT_VERSION (int major, int minor) {
	return major * 1000 + minor;
}

/**
 * Loads the shared library that matches the version of the
 * Java code which is currently running.  SWT shared libraries
 * follow an encoding scheme where the major, minor and revision
 * numbers are embedded in the library name and this along with
 * <code>name</code> is used to load the library.  If this fails,
 * <code>name</code> is used in another attempt to load the library,
 * this time ignoring the SWT version encoding scheme.
 *
 * @param name the name of the library to load
 */
public static void loadLibrary (String name) {
	/*
     * Include platform name to support different windowing systems
     * on same operating system.
	 */
	String platform = Platform.PLATFORM;
	
	/*
	 * Get version qualifier.
	 */
	String version = System.getProperty ("swt.version"); //$NON-NLS-1$
	if (version == null) {
		version = "" + MAJOR_VERSION; //$NON-NLS-1$
		/* Force 3 digits in minor version number */
		if (MINOR_VERSION < 10) {
			version += "00"; //$NON-NLS-1$
		} else {
			if (MINOR_VERSION < 100) version += "0"; //$NON-NLS-1$
		}
		version += MINOR_VERSION;		
		/* No "r" until first revision */
		if (REVISION > 0) version += "r" + REVISION; //$NON-NLS-1$
	}

	/*
	 * GOOGLE: Since we're bundling our own version of SWT, we need to be
	 * able to tell SWT where its dynamic libraries live.  Otherwise we'd
	 * have to force our users to always specify a -Djava.library.path
	 * on the command line.
	 */
	String swtLibraryPath = System.getProperty ("swt.library.path");
	try {
		String newName = name + "-" + platform + "-" + version; //$NON-NLS-1$ //$NON-NLS-2$
		if (swtLibraryPath != null)
			System.load(swtLibraryPath + System.mapLibraryName(newName));
		else
			System.loadLibrary (newName);
		return;
	} catch (UnsatisfiedLinkError e1) {		
		try {
			String newName = name + "-" + platform; //$NON-NLS-1$
			if (swtLibraryPath != null)
				System.load(swtLibraryPath + System.mapLibraryName(newName));
			else
				System.loadLibrary (newName);
			return;
		} catch (UnsatisfiedLinkError e2) {
			throw e1;
		}
	}
}

}
