/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;

/*
Not all fields defined by this type are initialized when it is created.
Some are initialized only when needed.

Accessors have been provided for some public fields so all TypeBindings have the same API...
but access public fields directly whenever possible.
Non-public fields have accessors which should be used everywhere you expect the field to be initialized.

null is NOT a valid value for a non-public field... it just means the field is not initialized.
*/

public class BinaryTypeBinding extends ReferenceBinding {

	// all of these fields are ONLY guaranteed to be initialized if accessed using their public accessor method
	protected ReferenceBinding superclass;
	protected ReferenceBinding enclosingType;
	protected ReferenceBinding[] superInterfaces;
	protected FieldBinding[] fields;
	protected MethodBinding[] methods;
	protected ReferenceBinding[] memberTypes;
	protected TypeVariableBinding[] typeVariables;

	// For the link with the principle structure
	protected LookupEnvironment environment;

	protected SimpleLookupTable storedAnnotations = null; // keys are this ReferenceBinding & its fields and methods, value is an AnnotationHolder

static Object convertMemberValue(Object binaryValue, LookupEnvironment env, char[][][] missingTypeNames) {
	if (binaryValue == null) return null;
	if (binaryValue instanceof Constant)
		return binaryValue;
	if (binaryValue instanceof ClassSignature) {
		TypeBinding typeFromSignature = env.getTypeFromSignature(((ClassSignature) binaryValue).getTypeName(), 0, -1, false, null, missingTypeNames);
		if (typeFromSignature.isBaseType()) {
			return typeFromSignature;
		}
		return resolveType((ReferenceBinding) typeFromSignature, env, false);
	}
	if (binaryValue instanceof IBinaryAnnotation)
		return createAnnotation((IBinaryAnnotation) binaryValue, env, missingTypeNames);
	if (binaryValue instanceof EnumConstantSignature) {
		EnumConstantSignature ref = (EnumConstantSignature) binaryValue;
		ReferenceBinding enumType = (ReferenceBinding) env.getTypeFromSignature(ref.getTypeName(), 0, -1, false, null, missingTypeNames);
		enumType = resolveType(enumType, env, false);
		return enumType.getField(ref.getEnumConstantName(), false);
	}
	if (binaryValue instanceof Object[]) {
		Object[] objects = (Object[]) binaryValue;
		int length = objects.length;
		if (length == 0) return objects;
		Object[] values = new Object[length];
		for (int i = 0; i < length; i++)
			values[i] = convertMemberValue(objects[i], env, missingTypeNames);
		return values;
	}

	// should never reach here.
	throw new IllegalStateException();
}
static AnnotationBinding createAnnotation(IBinaryAnnotation annotationInfo, LookupEnvironment env, char[][][] missingTypeNames) {
	IBinaryElementValuePair[] binaryPairs = annotationInfo.getElementValuePairs();
	int length = binaryPairs == null ? 0 : binaryPairs.length;
	ElementValuePair[] pairs = length == 0 ? Binding.NO_ELEMENT_VALUE_PAIRS : new ElementValuePair[length];
	for (int i = 0; i < length; i++)
		pairs[i] = new ElementValuePair(binaryPairs[i].getName(), convertMemberValue(binaryPairs[i].getValue(), env, missingTypeNames), null);

	char[] typeName = annotationInfo.getTypeName();
	ReferenceBinding annotationType = env.getTypeFromConstantPoolName(typeName, 1, typeName.length - 1, false, missingTypeNames);
	return new UnresolvedAnnotationBinding(annotationType, pairs, env);
}
public static AnnotationBinding[] createAnnotations(IBinaryAnnotation[] annotationInfos, LookupEnvironment env, char[][][] missingTypeNames) {
	int length = annotationInfos == null ? 0 : annotationInfos.length;
	AnnotationBinding[] result = length == 0 ? Binding.NO_ANNOTATIONS : new AnnotationBinding[length];
	for (int i = 0; i < length; i++)
		result[i] = createAnnotation(annotationInfos[i], env, missingTypeNames);
	return result;
}
public static ReferenceBinding resolveType(ReferenceBinding type, LookupEnvironment environment, boolean convertGenericToRawType) {
	if (type instanceof UnresolvedReferenceBinding)
		return ((UnresolvedReferenceBinding) type).resolve(environment, convertGenericToRawType);
	switch (type.kind()) {
		case Binding.PARAMETERIZED_TYPE :
			return ((ParameterizedTypeBinding) type).resolve();
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
			return ((WildcardBinding) type).resolve();
	}
	if (convertGenericToRawType) // raw reference to generic ?
		return (ReferenceBinding) environment.convertUnresolvedBinaryToRawType(type);
	return type;
}
public static TypeBinding resolveType(TypeBinding type, LookupEnvironment environment, ParameterizedTypeBinding parameterizedType, int rank) {
	switch (type.kind()) {
		
		case Binding.PARAMETERIZED_TYPE :
			return ((ParameterizedTypeBinding) type).resolve();
			
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
			return ((WildcardBinding) type).resolve();
			
		case Binding.ARRAY_TYPE :
			resolveType(((ArrayBinding) type).leafComponentType, environment, parameterizedType, rank);
			break;
			
		case Binding.TYPE_PARAMETER :
			((TypeVariableBinding) type).resolve(environment);
			break;
						
		case Binding.GENERIC_TYPE :
			if (parameterizedType == null) // raw reference to generic ?
				return environment.convertUnresolvedBinaryToRawType(type);
			break;
			
		default:			
			if (type instanceof UnresolvedReferenceBinding)
				return ((UnresolvedReferenceBinding) type).resolve(environment, parameterizedType == null);
	}
	return type;
}

/**
 * Default empty constructor for subclasses only.
 */
protected BinaryTypeBinding() {
	// only for subclasses
}

/**
 * Standard constructor for creating binary type bindings from binary models (classfiles)
 * @param packageBinding
 * @param binaryType
 * @param environment
 */
public BinaryTypeBinding(PackageBinding packageBinding, IBinaryType binaryType, LookupEnvironment environment) {
	this.compoundName = CharOperation.splitOn('/', binaryType.getName());
	computeId();

	this.tagBits |= TagBits.IsBinaryBinding;
	this.environment = environment;
	this.fPackage = packageBinding;
	this.fileName = binaryType.getFileName();

	char[] typeSignature = environment.globalOptions.sourceLevel >= ClassFileConstants.JDK1_5 ? binaryType.getGenericSignature() : null;
	this.typeVariables = typeSignature != null && typeSignature.length > 0 && typeSignature[0] == '<'
		? null // is initialized in cachePartsFrom (called from LookupEnvironment.createBinaryTypeFrom())... must set to null so isGenericType() answers true
		: Binding.NO_TYPE_VARIABLES;

	this.sourceName = binaryType.getSourceName();
	this.modifiers = binaryType.getModifiers();

	if ((binaryType.getTagBits() & TagBits.HierarchyHasProblems) != 0)
		this.tagBits |= TagBits.HierarchyHasProblems;
		
	if (binaryType.isAnonymous()) {
		this.tagBits |= TagBits.AnonymousTypeMask;
	} else if (binaryType.isLocal()) {
		this.tagBits |= TagBits.LocalTypeMask;
	} else if (binaryType.isMember()) {
		this.tagBits |= TagBits.MemberTypeMask;
	}
	// need enclosing type to access type variables
	char[] enclosingTypeName = binaryType.getEnclosingTypeName();
	if (enclosingTypeName != null) {
		// attempt to find the enclosing type if it exists in the cache (otherwise - resolve it when requested)
		this.enclosingType = environment.getTypeFromConstantPoolName(enclosingTypeName, 0, -1, true, null /* could not be missing */); // pretend parameterized to avoid raw
		this.tagBits |= TagBits.MemberTypeMask;   // must be a member type not a top-level or local type
		this.tagBits |= 	TagBits.HasUnresolvedEnclosingType;
		if (this.enclosingType().isStrictfp())
			this.modifiers |= ClassFileConstants.AccStrictfp;
		if (this.enclosingType().isDeprecated())
			this.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
	}	
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#availableMethods()
 */
public FieldBinding[] availableFields() {
	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return fields;

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	FieldBinding[] availableFields = new FieldBinding[fields.length];
	int count = 0;
	for (int i = 0; i < fields.length; i++) {
		try {
			availableFields[count] = resolveTypeFor(fields[i]);
			count++;
		} catch (AbortCompilation a){
			// silent abort
		}
	}
	if (count < availableFields.length)
		System.arraycopy(availableFields, 0, availableFields = new FieldBinding[count], 0, count);
	return availableFields;
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding#availableMethods()
 */
public MethodBinding[] availableMethods() {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return methods;

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	MethodBinding[] availableMethods = new MethodBinding[methods.length];
	int count = 0;
	for (int i = 0; i < methods.length; i++) {
		try {
			availableMethods[count] = resolveTypesFor(methods[i]);
			count++;
		} catch (AbortCompilation a){
			// silent abort
		}
	}
	if (count < availableMethods.length)
		System.arraycopy(availableMethods, 0, availableMethods = new MethodBinding[count], 0, count);
	return availableMethods;
}
void cachePartsFrom(IBinaryType binaryType, boolean needFieldsAndMethods) {
	// default initialization for super-interfaces early, in case some aborting compilation error occurs,
	// and still want to use binaries passed that point (e.g. type hierarchy resolver, see bug 63748).
	this.typeVariables = Binding.NO_TYPE_VARIABLES;
	this.superInterfaces = Binding.NO_SUPERINTERFACES;

	// must retrieve member types in case superclass/interfaces need them
	this.memberTypes = Binding.NO_MEMBER_TYPES;
	IBinaryNestedType[] memberTypeStructures = binaryType.getMemberTypes();
	if (memberTypeStructures != null) {
		int size = memberTypeStructures.length;
		if (size > 0) {
			this.memberTypes = new ReferenceBinding[size];
			for (int i = 0; i < size; i++)
				// attempt to find each member type if it exists in the cache (otherwise - resolve it when requested)
				this.memberTypes[i] = environment.getTypeFromConstantPoolName(memberTypeStructures[i].getName(), 0, -1, false, null /* could not be missing */);
			this.tagBits |= 	TagBits.HasUnresolvedMemberTypes;
		}
	}

	
	long sourceLevel = environment.globalOptions.sourceLevel;
	char[] typeSignature = null;
	if (sourceLevel >= ClassFileConstants.JDK1_5) {
		typeSignature = binaryType.getGenericSignature();
		this.tagBits |= binaryType.getTagBits();
	}
	char[][][] missingTypeNames = binaryType.getMissingTypeNames();	
	if (typeSignature == null) {
		char[] superclassName = binaryType.getSuperclassName();
		if (superclassName != null) {
			// attempt to find the superclass if it exists in the cache (otherwise - resolve it when requested)
			this.superclass = environment.getTypeFromConstantPoolName(superclassName, 0, -1, false, missingTypeNames);
			this.tagBits |= TagBits.HasUnresolvedSuperclass;
		}

		this.superInterfaces = Binding.NO_SUPERINTERFACES;
		char[][] interfaceNames = binaryType.getInterfaceNames();
		if (interfaceNames != null) {
			int size = interfaceNames.length;
			if (size > 0) {
				this.superInterfaces = new ReferenceBinding[size];
				for (int i = 0; i < size; i++)
					// attempt to find each superinterface if it exists in the cache (otherwise - resolve it when requested)
					this.superInterfaces[i] = environment.getTypeFromConstantPoolName(interfaceNames[i], 0, -1, false, missingTypeNames);
				this.tagBits |= TagBits.HasUnresolvedSuperinterfaces;
			}
		}
	} else {
		// ClassSignature = ParameterPart(optional) super_TypeSignature interface_signature
		SignatureWrapper wrapper = new SignatureWrapper(typeSignature);
		if (wrapper.signature[wrapper.start] == '<') {
			// ParameterPart = '<' ParameterSignature(s) '>'
			wrapper.start++; // skip '<'
			this.typeVariables = createTypeVariables(wrapper, true, missingTypeNames);
			wrapper.start++; // skip '>'
			this.tagBits |=  TagBits.HasUnresolvedTypeVariables;
			this.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
		}

		// attempt to find the superclass if it exists in the cache (otherwise - resolve it when requested)
		this.superclass = (ReferenceBinding) environment.getTypeFromTypeSignature(wrapper, Binding.NO_TYPE_VARIABLES, this, missingTypeNames);
		this.tagBits |= TagBits.HasUnresolvedSuperclass;

		this.superInterfaces = Binding.NO_SUPERINTERFACES;
		if (!wrapper.atEnd()) {
			// attempt to find each superinterface if it exists in the cache (otherwise - resolve it when requested)
			java.util.ArrayList types = new java.util.ArrayList(2);
			do {
				types.add(environment.getTypeFromTypeSignature(wrapper, Binding.NO_TYPE_VARIABLES, this, missingTypeNames));
			} while (!wrapper.atEnd());
			this.superInterfaces = new ReferenceBinding[types.size()];
			types.toArray(this.superInterfaces);
			this.tagBits |= TagBits.HasUnresolvedSuperinterfaces;
		}
	}

	if (needFieldsAndMethods) {
		createFields(binaryType.getFields(), sourceLevel, missingTypeNames);
		createMethods(binaryType.getMethods(), sourceLevel, missingTypeNames);
	} else { // protect against incorrect use of the needFieldsAndMethods flag, see 48459
		this.fields = Binding.NO_FIELDS;
		this.methods = Binding.NO_METHODS;
	}
	if (this.environment.globalOptions.storeAnnotations)
		setAnnotations(createAnnotations(binaryType.getAnnotations(), this.environment, missingTypeNames));	
}
private void createFields(IBinaryField[] iFields, long sourceLevel, char[][][] missingTypeNames) {
	this.fields = Binding.NO_FIELDS;
	if (iFields != null) {
		int size = iFields.length;
		if (size > 0) {
			this.fields = new FieldBinding[size];
			boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
			boolean isViewedAsDeprecated = isViewedAsDeprecated();
			boolean hasRestrictedAccess = hasRestrictedAccess();
			int firstAnnotatedFieldIndex = -1;
			for (int i = 0; i < size; i++) {
				IBinaryField binaryField = iFields[i];
				char[] fieldSignature = use15specifics ? binaryField.getGenericSignature() : null;
				TypeBinding type = fieldSignature == null 
					? environment.getTypeFromSignature(binaryField.getTypeName(), 0, -1, false, this, missingTypeNames) 
					: environment.getTypeFromTypeSignature(new SignatureWrapper(fieldSignature), Binding.NO_TYPE_VARIABLES, this, missingTypeNames);
				FieldBinding field = 
					new FieldBinding(
						binaryField.getName(), 
						type, 
						binaryField.getModifiers() | ExtraCompilerModifiers.AccUnresolved, 
						this, 
						binaryField.getConstant());
				if (firstAnnotatedFieldIndex < 0
						&& this.environment.globalOptions.storeAnnotations 
						&& binaryField.getAnnotations() != null) {
					firstAnnotatedFieldIndex = i;
				}
				field.id = i; // ordinal
				if (use15specifics)
					field.tagBits |= binaryField.getTagBits();
				if (isViewedAsDeprecated && !field.isDeprecated())
					field.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
				if (hasRestrictedAccess)
					field.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
				if (fieldSignature != null)
					field.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
				this.fields[i] = field;
			}
			// second pass for reifying annotations, since may refer to fields being constructed (147875)
			if (firstAnnotatedFieldIndex >= 0) {
				for (int i = firstAnnotatedFieldIndex; i <size; i++) {
					IBinaryField binaryField = iFields[i];
					this.fields[i].setAnnotations(createAnnotations(binaryField.getAnnotations(), this.environment, missingTypeNames));
				}
			}
		}
	}
}
private MethodBinding createMethod(IBinaryMethod method, long sourceLevel, char[][][] missingTypeNames) {
	int methodModifiers = method.getModifiers() | ExtraCompilerModifiers.AccUnresolved;
	if (sourceLevel < ClassFileConstants.JDK1_5)
		methodModifiers &= ~ClassFileConstants.AccVarargs; // vararg methods are not recognized until 1.5
	ReferenceBinding[] exceptions = Binding.NO_EXCEPTIONS;
	TypeBinding[] parameters = Binding.NO_PARAMETERS;
	TypeVariableBinding[] typeVars = Binding.NO_TYPE_VARIABLES;
	AnnotationBinding[][] paramAnnotations = null; 
	TypeBinding returnType = null;

	final boolean use15specifics = sourceLevel >= ClassFileConstants.JDK1_5;
	char[] methodSignature = use15specifics ? method.getGenericSignature() : null;
	if (methodSignature == null) { // no generics
		char[] methodDescriptor = method.getMethodDescriptor();   // of the form (I[Ljava/jang/String;)V
		int numOfParams = 0;
		char nextChar;
		int index = 0;   // first character is always '(' so skip it
		while ((nextChar = methodDescriptor[++index]) != ')') {
			if (nextChar != '[') {
				numOfParams++;
				if (nextChar == 'L')
					while ((nextChar = methodDescriptor[++index]) != ';'){/*empty*/}
			}
		}

		// Ignore synthetic argument for member types.
		int startIndex = (method.isConstructor() && isMemberType() && !isStatic()) ? 1 : 0;
		int size = numOfParams - startIndex;
		if (size > 0) {
			parameters = new TypeBinding[size];
			if (this.environment.globalOptions.storeAnnotations)
				paramAnnotations = new AnnotationBinding[size][];
			index = 1;
			int end = 0;   // first character is always '(' so skip it
			for (int i = 0; i < numOfParams; i++) {
				while ((nextChar = methodDescriptor[++end]) == '['){/*empty*/}
				if (nextChar == 'L')
					while ((nextChar = methodDescriptor[++end]) != ';'){/*empty*/}

				if (i >= startIndex) {   // skip the synthetic arg if necessary
					parameters[i - startIndex] = environment.getTypeFromSignature(methodDescriptor, index, end, false, this, missingTypeNames);
					// 'paramAnnotations' line up with 'parameters'
					// int parameter to method.getParameterAnnotations() include the synthetic arg
					if (paramAnnotations != null)
						paramAnnotations[i - startIndex] = createAnnotations(method.getParameterAnnotations(i), this.environment, missingTypeNames);
				}
				index = end + 1;
			}
		}

		char[][] exceptionTypes = method.getExceptionTypeNames();
		if (exceptionTypes != null) {
			size = exceptionTypes.length;
			if (size > 0) {
				exceptions = new ReferenceBinding[size];
				for (int i = 0; i < size; i++)
					exceptions[i] = environment.getTypeFromConstantPoolName(exceptionTypes[i], 0, -1, false, missingTypeNames);
			}
		}

		if (!method.isConstructor())
			returnType = environment.getTypeFromSignature(methodDescriptor, index + 1, -1, false, this, missingTypeNames);   // index is currently pointing at the ')'
	} else {
		methodModifiers |= ExtraCompilerModifiers.AccGenericSignature;
		// MethodTypeSignature = ParameterPart(optional) '(' TypeSignatures ')' return_typeSignature ['^' TypeSignature (optional)]
		SignatureWrapper wrapper = new SignatureWrapper(methodSignature);
		if (wrapper.signature[wrapper.start] == '<') {
			// <A::Ljava/lang/annotation/Annotation;>(Ljava/lang/Class<TA;>;)TA;
			// ParameterPart = '<' ParameterSignature(s) '>'
			wrapper.start++; // skip '<'
			typeVars = createTypeVariables(wrapper, false, missingTypeNames);
			wrapper.start++; // skip '>'
		}

		if (wrapper.signature[wrapper.start] == '(') {
			wrapper.start++; // skip '('
			if (wrapper.signature[wrapper.start] == ')') {
				wrapper.start++; // skip ')'
			} else {
				java.util.ArrayList types = new java.util.ArrayList(2);
				while (wrapper.signature[wrapper.start] != ')')
					types.add(environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames));
				wrapper.start++; // skip ')'
				int numParam = types.size();
				parameters = new TypeBinding[numParam];
				types.toArray(parameters);
				if (this.environment.globalOptions.storeAnnotations) {
					paramAnnotations = new AnnotationBinding[numParam][];
					for (int i = 0; i < numParam; i++)
						paramAnnotations[i] = createAnnotations(method.getParameterAnnotations(i), this.environment, missingTypeNames);
				}
			}
		}

		// always retrieve return type (for constructors, its V for void - will be ignored)
		returnType = environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames);

		if (!wrapper.atEnd() && wrapper.signature[wrapper.start] == '^') {
			// attempt to find each exception if it exists in the cache (otherwise - resolve it when requested)
			java.util.ArrayList types = new java.util.ArrayList(2);
			do {
				wrapper.start++; // skip '^'
				types.add(environment.getTypeFromTypeSignature(wrapper, typeVars, this, missingTypeNames));
			} while (!wrapper.atEnd() && wrapper.signature[wrapper.start] == '^');
			exceptions = new ReferenceBinding[types.size()];
			types.toArray(exceptions);
		} else { // get the exceptions the old way
			char[][] exceptionTypes = method.getExceptionTypeNames();
			if (exceptionTypes != null) {
				int size = exceptionTypes.length;
				if (size > 0) {
					exceptions = new ReferenceBinding[size];
					for (int i = 0; i < size; i++)
						exceptions[i] = environment.getTypeFromConstantPoolName(exceptionTypes[i], 0, -1, false, missingTypeNames);
				}
			}
		}
	}

	MethodBinding result = method.isConstructor()
		? new MethodBinding(methodModifiers, parameters, exceptions, this)
		: new MethodBinding(methodModifiers, method.getSelector(), returnType, parameters, exceptions, this);
	if (this.environment.globalOptions.storeAnnotations)
		result.setAnnotations(
			createAnnotations(method.getAnnotations(), this.environment, missingTypeNames),
			paramAnnotations,
			isAnnotationType() ? convertMemberValue(method.getDefaultValue(), this.environment, missingTypeNames) : null);

	if (use15specifics)
		result.tagBits |= method.getTagBits();
	result.typeVariables = typeVars;
	// fixup the declaring element of the type variable
	for (int i = 0, length = typeVars.length; i < length; i++)
		typeVars[i].declaringElement = result;
	return result;
}
/**
 * Create method bindings for binary type, filtering out <clinit> and synthetics
 */
private void createMethods(IBinaryMethod[] iMethods, long sourceLevel, char[][][] missingTypeNames) {
	int total = 0, initialTotal = 0, iClinit = -1;
	int[] toSkip = null;
	if (iMethods != null) {
		total = initialTotal = iMethods.length;
		boolean keepBridgeMethods = sourceLevel < ClassFileConstants.JDK1_5
			&& this.environment.globalOptions.complianceLevel >= ClassFileConstants.JDK1_5;
		for (int i = total; --i >= 0;) {
			IBinaryMethod method = iMethods[i];
			if ((method.getModifiers() & ClassFileConstants.AccSynthetic) != 0) {
				if (keepBridgeMethods && (method.getModifiers() & ClassFileConstants.AccBridge) != 0)
					continue; // want to see bridge methods as real methods
				// discard synthetics methods
				if (toSkip == null) toSkip = new int[iMethods.length];
				toSkip[i] = -1;
				total--;
			} else if (iClinit == -1) {
				char[] methodName = method.getSelector();
				if (methodName.length == 8 && methodName[0] == '<') {
					// discard <clinit>
					iClinit = i;
					total--;
				}
			}
		}
	}
	if (total == 0) {
		this.methods = Binding.NO_METHODS;
		return;
	}

	boolean isViewedAsDeprecated = isViewedAsDeprecated();
	boolean hasRestrictedAccess = hasRestrictedAccess();
	this.methods = new MethodBinding[total];
	if (total == initialTotal) {
		for (int i = 0; i < initialTotal; i++) {
			MethodBinding method = createMethod(iMethods[i], sourceLevel, missingTypeNames);
			if (isViewedAsDeprecated && !method.isDeprecated())
				method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
			if (hasRestrictedAccess)
				method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
			this.methods[i] = method;
		}
	} else {
		for (int i = 0, index = 0; i < initialTotal; i++) {
			if (iClinit != i && (toSkip == null || toSkip[i] != -1)) {
				MethodBinding method = createMethod(iMethods[i], sourceLevel, missingTypeNames);
				if (isViewedAsDeprecated && !method.isDeprecated())
					method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
				if (hasRestrictedAccess)
					method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
				this.methods[index++] = method;
			}
		}
	}
}
private TypeVariableBinding[] createTypeVariables(SignatureWrapper wrapper, boolean assignVariables, char[][][] missingTypeNames) {
	// detect all type variables first
	char[] typeSignature = wrapper.signature;
	int depth = 0, length = typeSignature.length;
	int rank = 0;
	ArrayList variables = new ArrayList(1);
	depth = 0;
	boolean pendingVariable = true;
	createVariables: {
		for (int i = 1; i < length; i++) {
			switch(typeSignature[i]) {
				case '<' : 
					depth++;
					break;
				case '>' : 
					if (--depth < 0)
						break createVariables;
					break;
				case ';' :
					if ((depth == 0) && (i +1 < length) && (typeSignature[i+1] != ':'))
						pendingVariable = true;
					break;
				default:
					if (pendingVariable) {
						pendingVariable = false;
						int colon = CharOperation.indexOf(':', typeSignature, i);
						char[] variableName = CharOperation.subarray(typeSignature, i, colon);
						variables.add(new TypeVariableBinding(variableName, this, rank++));
					}
			}
		}
	}
	// initialize type variable bounds - may refer to forward variables
	TypeVariableBinding[] result;
	variables.toArray(result = new TypeVariableBinding[rank]);
	// when creating the type variables for a type, the type must remember them before initializing each variable
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=163680
	if (assignVariables)
		this.typeVariables = result;
	for (int i = 0; i < rank; i++) {
		initializeTypeVariable(result[i], result, wrapper, missingTypeNames);
	}
	return result;
}
/* Answer the receiver's enclosing type... null if the receiver is a top level type.
*
* NOTE: enclosingType of a binary type is resolved when needed
*/
public ReferenceBinding enclosingType() {
	if ((this.tagBits & TagBits.HasUnresolvedEnclosingType) == 0)
		return this.enclosingType;

	// finish resolving the type
	this.enclosingType = resolveType(this.enclosingType, this.environment, false);
	this.tagBits &= ~TagBits.HasUnresolvedEnclosingType;
	return this.enclosingType;
}
// NOTE: the type of each field of a binary type is resolved when needed
public FieldBinding[] fields() {
	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return fields;

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	for (int i = fields.length; --i >= 0;)
		resolveTypeFor(fields[i]);
	this.tagBits |= TagBits.AreFieldsComplete;
	return fields;
}
/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#genericTypeSignature()
 */
public char[] genericTypeSignature() {
	return computeGenericTypeSignature(this.typeVariables);
}
//NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	int argCount = argumentTypes.length;
	long range;
	if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
		nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {	
			MethodBinding method = methods[imethod];
			if (method.parameters.length == argCount) {
				resolveTypesFor(method);
				TypeBinding[] toMatch = method.parameters;
				for (int iarg = 0; iarg < argCount; iarg++)
					if (toMatch[iarg] != argumentTypes[iarg])
						continue nextMethod;
				return method;
			}
		}	
	}
	return null;
}

//NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
//searches up the hierarchy as long as no potential (but not exact) match was found.
public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
	// sender from refScope calls recordTypeReference(this)
	
	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}

	int argCount = argumentTypes.length;
	boolean foundNothing = true;

	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {	
			MethodBinding method = methods[imethod];
			foundNothing = false; // inner type lookups must know that a method with this name exists
			if (method.parameters.length == argCount) {
				resolveTypesFor(method);
				TypeBinding[] toMatch = method.parameters;
				for (int iarg = 0; iarg < argCount; iarg++)
					if (toMatch[iarg] != argumentTypes[iarg])
						continue nextMethod;
				return method;
			}
		}
	}
	if (foundNothing) {
		if (isInterface()) {
			 if (superInterfaces().length == 1) { // ensure superinterfaces are resolved before checking
				if (refScope != null)
					refScope.recordTypeReference(superInterfaces[0]);
				return superInterfaces[0].getExactMethod(selector, argumentTypes, refScope);
			 }
		} else if (superclass() != null) { // ensure superclass is resolved before checking
			if (refScope != null)
				refScope.recordTypeReference(superclass);
			return superclass.getExactMethod(selector, argumentTypes, refScope);
		}
	}
	return null;
}
//NOTE: the type of a field of a binary type is resolved when needed
public FieldBinding getField(char[] fieldName, boolean needResolve) {
	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	FieldBinding field = ReferenceBinding.binarySearch(fieldName, this.fields);
	return needResolve && field != null ? resolveTypeFor(field) : field;
}
/**
 *  Rewrite of default getMemberType to avoid resolving eagerly all member types when one is requested
 */
public ReferenceBinding getMemberType(char[] typeName) {
	for (int i = this.memberTypes.length; --i >= 0;) {
	    ReferenceBinding memberType = this.memberTypes[i];
	    if (memberType instanceof UnresolvedReferenceBinding) {
			char[] name = memberType.sourceName; // source name is qualified with enclosing type name
			int prefixLength = this.compoundName[this.compoundName.length - 1].length + 1; // enclosing$
			if (name.length == (prefixLength + typeName.length)) // enclosing $ typeName
				if (CharOperation.fragmentEquals(typeName, name, prefixLength, true)) // only check trailing portion
					return this.memberTypes[i] = resolveType(memberType, this.environment, false); // no raw conversion for now
	    } else if (CharOperation.equals(typeName, memberType.sourceName)) {
	        return memberType;
	    }
	}
	return null;
}
// NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
public MethodBinding[] getMethods(char[] selector) {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			int length = end - start + 1;
			if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
				// simply clone method subset
				MethodBinding[] result;				
				System.arraycopy(this.methods, start, result = new MethodBinding[length], 0, length);
				return result;
			}
		}
		return Binding.NO_METHODS;
	}
	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		int start = (int) range, end = (int) (range >> 32);
		int length = end - start + 1;
		MethodBinding[] result = new MethodBinding[length];
		// iterate methods to resolve them
		for (int i = start, index = 0; i <= end; i++, index++)
			result[index] = resolveTypesFor(methods[i]);
		return result;
	}
	return Binding.NO_METHODS;
}
public boolean hasMemberTypes() {
    return this.memberTypes.length > 0;
}
// NOTE: member types of binary types are resolved when needed
public TypeVariableBinding getTypeVariable(char[] variableName) {
	TypeVariableBinding variable = super.getTypeVariable(variableName);
	variable.resolve(this.environment);
	return variable;
}
private void initializeTypeVariable(TypeVariableBinding variable, TypeVariableBinding[] existingVariables, SignatureWrapper wrapper, char[][][] missingTypeNames) {
	// ParameterSignature = Identifier ':' TypeSignature
	//   or Identifier ':' TypeSignature(optional) InterfaceBound(s)
	// InterfaceBound = ':' TypeSignature
	int colon = CharOperation.indexOf(':', wrapper.signature, wrapper.start);
	wrapper.start = colon + 1; // skip name + ':'
	ReferenceBinding type, firstBound = null;
	if (wrapper.signature[wrapper.start] == ':') {
		type = environment.getResolvedType(TypeConstants.JAVA_LANG_OBJECT, null);
	} else {
		type = (ReferenceBinding) environment.getTypeFromTypeSignature(wrapper, existingVariables, this, missingTypeNames);
		firstBound = type;
	}

	// variable is visible to its bounds
	variable.modifiers |= ExtraCompilerModifiers.AccUnresolved;
	variable.superclass = type;

	ReferenceBinding[] bounds = null;
	if (wrapper.signature[wrapper.start] == ':') {
		java.util.ArrayList types = new java.util.ArrayList(2);
		do {
			wrapper.start++; // skip ':'
			types.add(environment.getTypeFromTypeSignature(wrapper, existingVariables, this, missingTypeNames));
		} while (wrapper.signature[wrapper.start] == ':');
		bounds = new ReferenceBinding[types.size()];
		types.toArray(bounds);
	}

	variable.superInterfaces = bounds == null ? Binding.NO_SUPERINTERFACES : bounds;
	if (firstBound == null) {
		firstBound = variable.superInterfaces.length == 0 ? null : variable.superInterfaces[0];
	}
	variable.firstBound = firstBound;
}
/**
 * Returns true if a type is identical to another one,
 * or for generic types, true if compared to its raw type.
 */
public boolean isEquivalentTo(TypeBinding otherType) {
	if (this == otherType) return true;
	if (otherType == null) return false;
	switch(otherType.kind()) {
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
			return ((WildcardBinding) otherType).boundCheck(this);
		case Binding.RAW_TYPE :
			return otherType.erasure() == this;
	}
	return false;
}
public boolean isGenericType() {
    return this.typeVariables != Binding.NO_TYPE_VARIABLES;
}
public int kind() {
	if (this.typeVariables != Binding.NO_TYPE_VARIABLES)
		return Binding.GENERIC_TYPE;
	return Binding.TYPE;
}	
// NOTE: member types of binary types are resolved when needed
public ReferenceBinding[] memberTypes() {
 	if ((this.tagBits & TagBits.HasUnresolvedMemberTypes) == 0)
		return this.memberTypes;

	for (int i = this.memberTypes.length; --i >= 0;)
		this.memberTypes[i] = resolveType(this.memberTypes[i], this.environment, false); // no raw conversion for now
	this.tagBits &= ~TagBits.HasUnresolvedMemberTypes;
	return this.memberTypes;
}
// NOTE: the return type, arg & exception types of each method of a binary type are resolved when needed
public MethodBinding[] methods() {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return methods;

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	for (int i = methods.length; --i >= 0;)
		resolveTypesFor(methods[i]);
	this.tagBits |= TagBits.AreMethodsComplete;
	return methods;
}
private FieldBinding resolveTypeFor(FieldBinding field) {
	if ((field.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return field;

	TypeBinding resolvedType = resolveType(field.type, this.environment, null, 0);
	field.type = resolvedType;
	if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
		field.tagBits |= TagBits.HasMissingType;
	}		
	field.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return field;
}
MethodBinding resolveTypesFor(MethodBinding method) {
	if ((method.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return method;

	if (!method.isConstructor()) {
		TypeBinding resolvedType = resolveType(method.returnType, this.environment, null, 0);
		method.returnType = resolvedType;
		if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |= TagBits.HasMissingType;
		}		
	}
	for (int i = method.parameters.length; --i >= 0;) {
		TypeBinding resolvedType = resolveType(method.parameters[i], this.environment, null, 0);
		method.parameters[i] = resolvedType;
		if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |= TagBits.HasMissingType;
		}		
	}
	for (int i = method.thrownExceptions.length; --i >= 0;) {
		ReferenceBinding resolvedType = resolveType(method.thrownExceptions[i], this.environment, true);
		method.thrownExceptions[i] = resolvedType;
		if ((resolvedType.tagBits & TagBits.HasMissingType) != 0) {
			method.tagBits |= TagBits.HasMissingType;
		}
	}
	for (int i = method.typeVariables.length; --i >= 0;) {
		method.typeVariables[i].resolve(this.environment);
	}
	method.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return method;
}
AnnotationBinding[] retrieveAnnotations(Binding binding) {
	return AnnotationBinding.addStandardAnnotations(super.retrieveAnnotations(binding), binding.getAnnotationTagBits(), this.environment);
}
SimpleLookupTable storedAnnotations(boolean forceInitialize) {
	if (forceInitialize && this.storedAnnotations == null) {
		if (!this.environment.globalOptions.storeAnnotations)
			return null; // not supported during this compile
		this.storedAnnotations = new SimpleLookupTable(3);
	}
	return this.storedAnnotations;
}
/* Answer the receiver's superclass... null if the receiver is Object or an interface.
*
* NOTE: superclass of a binary type is resolved when needed
*/
public ReferenceBinding superclass() {
	if ((this.tagBits & TagBits.HasUnresolvedSuperclass) == 0)
		return this.superclass;

	// finish resolving the type
	this.superclass = resolveType(this.superclass, this.environment, true);
	this.tagBits &= ~TagBits.HasUnresolvedSuperclass;
	if (this.superclass.problemId() == ProblemReasons.NotFound)
		this.tagBits |= TagBits.HierarchyHasProblems; // propagate type inconsistency
	return this.superclass;
}
// NOTE: superInterfaces of binary types are resolved when needed
public ReferenceBinding[] superInterfaces() {
	if ((this.tagBits & TagBits.HasUnresolvedSuperinterfaces) == 0)
		return this.superInterfaces;

	for (int i = this.superInterfaces.length; --i >= 0;) {
		this.superInterfaces[i] = resolveType(this.superInterfaces[i], this.environment, true);
		if (this.superInterfaces[i].problemId() == ProblemReasons.NotFound)
			this.tagBits |= TagBits.HierarchyHasProblems; // propagate type inconsistency
	}
	this.tagBits &= ~TagBits.HasUnresolvedSuperinterfaces;
	return this.superInterfaces;
}
public TypeVariableBinding[] typeVariables() {
 	if ((this.tagBits & TagBits.HasUnresolvedTypeVariables) == 0)
		return this.typeVariables;

 	for (int i = this.typeVariables.length; --i >= 0;)
		this.typeVariables[i].resolve(this.environment);
	this.tagBits &= ~TagBits.HasUnresolvedTypeVariables;
	return this.typeVariables;
}
public String toString() {
	StringBuffer buffer = new StringBuffer();

	if (isDeprecated()) buffer.append("deprecated "); //$NON-NLS-1$
	if (isPublic()) buffer.append("public "); //$NON-NLS-1$
	if (isProtected()) buffer.append("protected "); //$NON-NLS-1$
	if (isPrivate()) buffer.append("private "); //$NON-NLS-1$
	if (isAbstract() && isClass()) buffer.append("abstract "); //$NON-NLS-1$
	if (isStatic() && isNestedType()) buffer.append("static "); //$NON-NLS-1$
	if (isFinal()) buffer.append("final "); //$NON-NLS-1$

	if (isEnum()) buffer.append("enum "); //$NON-NLS-1$
	else if (isAnnotationType()) buffer.append("@interface "); //$NON-NLS-1$
	else if (isClass()) buffer.append("class "); //$NON-NLS-1$
	else buffer.append("interface "); //$NON-NLS-1$	
	buffer.append((compoundName != null) ? CharOperation.toString(compoundName) : "UNNAMED TYPE"); //$NON-NLS-1$

	if (this.typeVariables == null) {
		buffer.append("<NULL TYPE VARIABLES>"); //$NON-NLS-1$
	} else if (this.typeVariables != Binding.NO_TYPE_VARIABLES) {
		buffer.append("<"); //$NON-NLS-1$
		for (int i = 0, length = this.typeVariables.length; i < length; i++) {
			if (i  > 0) buffer.append(", "); //$NON-NLS-1$
			if (this.typeVariables[i] == null) {
				buffer.append("NULL TYPE VARIABLE"); //$NON-NLS-1$
				continue;
			}
			char[] varChars = this.typeVariables[i].toString().toCharArray();
			buffer.append(varChars, 1, varChars.length - 2);
		}
		buffer.append(">"); //$NON-NLS-1$
	}
	buffer.append("\n\textends "); //$NON-NLS-1$
	buffer.append((superclass != null) ? superclass.debugName() : "NULL TYPE"); //$NON-NLS-1$

	if (superInterfaces != null) {
		if (superInterfaces != Binding.NO_SUPERINTERFACES) {
			buffer.append("\n\timplements : "); //$NON-NLS-1$
			for (int i = 0, length = superInterfaces.length; i < length; i++) {
				if (i  > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append((superInterfaces[i] != null) ? superInterfaces[i].debugName() : "NULL TYPE"); //$NON-NLS-1$
			}
		}
	} else {
		buffer.append("NULL SUPERINTERFACES"); //$NON-NLS-1$
	}

	if (enclosingType != null) {
		buffer.append("\n\tenclosing type : "); //$NON-NLS-1$
		buffer.append(enclosingType.debugName());
	}

	if (fields != null) {
		if (fields != Binding.NO_FIELDS) {
			buffer.append("\n/*   fields   */"); //$NON-NLS-1$
			for (int i = 0, length = fields.length; i < length; i++)
				buffer.append((fields[i] != null) ? "\n" + fields[i].toString() : "\nNULL FIELD"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL FIELDS"); //$NON-NLS-1$
	}

	if (methods != null) {
		if (methods != Binding.NO_METHODS) {
			buffer.append("\n/*   methods   */"); //$NON-NLS-1$
			for (int i = 0, length = methods.length; i < length; i++)
				buffer.append((methods[i] != null) ? "\n" + methods[i].toString() : "\nNULL METHOD"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL METHODS"); //$NON-NLS-1$
	}

	if (memberTypes != null) {
		if (memberTypes != Binding.NO_MEMBER_TYPES) {
			buffer.append("\n/*   members   */"); //$NON-NLS-1$
			for (int i = 0, length = memberTypes.length; i < length; i++)
				buffer.append((memberTypes[i] != null) ? "\n" + memberTypes[i].toString() : "\nNULL TYPE"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	} else {
		buffer.append("NULL MEMBER TYPES"); //$NON-NLS-1$
	}

	buffer.append("\n\n\n"); //$NON-NLS-1$
	return buffer.toString();
}
MethodBinding[] unResolvedMethods() { // for the MethodVerifier so it doesn't resolve types
	return methods;
}
}
