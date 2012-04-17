/*
 * ClassGenerator.java
 *
 * Copyright (c) 1997-2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * See the file "LICENSE.txt" for information on usage and redistribution
 * of this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package pnuts.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.pnuts.lang.ObjectDescFactory;
import org.pnuts.lang.PropertyHandler;
import org.pnuts.lang.ReflectionUtil;
import org.pnuts.lang.Signature;
import pnuts.lang.AbstractData;
import pnuts.lang.Context;
import pnuts.lang.PnutsParserTreeConstants;
import pnuts.lang.SimpleNode;
import pnuts.lang.Runtime;

public class ClassGenerator {
        public final static int THIS_BIT = 0x0001;
        public final static int SUPER_BIT = 0x0002;
        final static String SUPER_PROXY_NAME = "pnuts.compiler.ClassGenerator$SuperCallProxy";

        public static ClassFile createClassFile(String className,
                Class superClass, Class[] interfaceTypes, Set superMethodNames) {
                ClassSpec[] interfaces = new ClassSpec[interfaceTypes.length];
                for (int i = 0; i < interfaces.length; i++){
                        interfaces[i] = ClassSpec.create(interfaceTypes[i]);       
                }
              return createClassFile(className, ClassSpec.create(superClass), interfaces, superMethodNames);  
        }       
        
        public static ClassFile createClassFile(String className,
                ClassSpec superclassSpec, ClassSpec[] interfaces, Set superMethodNames) {
                Class superClass = superclassSpec.compileTimeClass;
                if (superClass == null) {
                        superClass = Object.class;
                }
                ClassFile cf = new ClassFile(className, superclassSpec.className, null,
                        Constants.ACC_PUBLIC);
                if (interfaces != null) {
                        for (int i = 0; i < interfaces.length; i++) {
                                cf.addInterface(interfaces[i].className);
                        }
                }
                cf.addField("_context", "Lpnuts/lang/Context;",
                        (short) (Constants.ACC_PRIVATE | Constants.ACC_STATIC));
                
                superCall(cf, superClass, superMethodNames);
                
                return cf;
                
        }
        
        public static void constructor(ClassFile cf,
                ClassSpec superclassSpec,
                Compiler compiler,
                CompileContext cc,
                List/*<Signature>*/ signatures)
        {
                String superclassName = superclassSpec.className;
                Class superClass = superclassSpec.compileTimeClass;
                Set/*<Constructor>*/ protected_constructors = new HashSet();
                if (superClass == null){
                        superClass = Object.class;
                }
                        
                getProtectedConstructors(superClass, new HashSet(), protected_constructors);
                Constructor[] public_constructors = superClass.getConstructors();
                
                Set/*<Signature>*/ generated_constructors = new HashSet();
                int num_constructors = 0;;
                
                for (int i = 0, n = signatures.size(); i < n; i++){
                        Signature sig = (Signature)signatures.get(i);
//		    SimpleNode fnode = (SimpleNode)sig.nodeInfo;
                        
                        int modifiers = sig.getModifiers();
                        Class[] parameterTypes;
                        Class[] exceptionTypes;
                        
                        List constructors = new ArrayList();
                        if (sig.resolveAsConstructor(superClass, constructors)){
                                for (int j = 0, n2 = constructors.size(); j < n2; j++){
                                        Constructor cons = (Constructor)constructors.get(j);
                                        parameterTypes = cons.getParameterTypes();
                                        exceptionTypes = cons.getExceptionTypes();
                                        constructor(cf, cc, compiler, superclassSpec, parameterTypes,
                                                exceptionTypes, Constants.ACC_PUBLIC);
                                        generated_constructors.add(new Signature(null, void.class,
                                                parameterTypes,
                                                exceptionTypes));
                                        num_constructors++;
                                }
                        } else {
                                parameterTypes = sig.getParameterTypes();
                                exceptionTypes = sig.getExceptionTypes();
                                constructor(cf, cc, compiler, superclassSpec, parameterTypes,
                                        exceptionTypes, Constants.ACC_PUBLIC);
                                generated_constructors.add(new Signature(null, void.class,
                                        parameterTypes,
                                        exceptionTypes));
                                num_constructors++;
                                
                        }
                }
                for (int i = 0; i < public_constructors.length; i++){
                        Constructor cons = public_constructors[i];
                        Class[] parameterTypes = cons.getParameterTypes();
                        Class[] exceptionTypes = cons.getExceptionTypes();
                        Signature sig =
                                new Signature(null, void.class, parameterTypes, exceptionTypes);
                        if (!generated_constructors.contains(sig)){
                                derivedConstructors(cf, cc, compiler, superClass, superclassName, parameterTypes,
                                        exceptionTypes, Constants.ACC_PUBLIC);
                                num_constructors++;
                        }
                }
                if (num_constructors == 0) {
                        cf.openMethod("<init>", "()V", Constants.ACC_PUBLIC);
                        cf.add(Opcode.ALOAD_0);
                        cf.add(Opcode.INVOKESPECIAL, superclassName, "<init>", "()",
                                "V");
			cf.add(Opcode.ALOAD_0);
			cf.add(Opcode.INVOKEVIRTUAL, cf.getClassName(), "__initialize", "()", "V");
                        
                        cf.add(Opcode.RETURN);
                        cf.closeMethod();
                        generated_constructors.add(new Signature(null, void.class,
                                new Class[0],
                                new Class[0]));
                }
                for (Iterator it = protected_constructors.iterator(); it.hasNext();){
                        Constructor cons = (Constructor)it.next();
                        Class[] parameterTypes = cons.getParameterTypes();
                        Class[] exceptionTypes = cons.getExceptionTypes();
                        Signature sig =
                                new Signature(null, void.class, parameterTypes, exceptionTypes);
                        if (!generated_constructors.contains(sig)){
                                derivedConstructors(cf, cc, compiler, superClass, superclassName, parameterTypes,
                                        exceptionTypes, Constants.ACC_PROTECTED);
                        }
                }
        }
        
        /*
         * derived constructors that just calls super(...)
         */
        static void derivedConstructors(ClassFile cf,
                CompileContext cc,
                Compiler compiler,
                Class superClass,
                String superclassName,
                Class[] parameterTypes,
                Class[] exceptionTypes,
                short modifier)
        {
                String sig = ClassFile.signature(parameterTypes);
                
                String[] exceptionTypeInfo = null;
                if (exceptionTypes != null && exceptionTypes.length > 0) {
                        exceptionTypeInfo = new String[exceptionTypes.length];
                        for (int j = 0; j < exceptionTypes.length; j++) {
                                exceptionTypeInfo[j] = exceptionTypes[j].getName();
                        }
                }
                cf.openMethod("<init>", sig + "V", modifier, exceptionTypeInfo);
                cf.add(Opcode.ALOAD_0);
                int pos = 1;
                for (int i = 0; i < parameterTypes.length; i++){
                        Class type = parameterTypes[i];
                        if (type.isPrimitive()){
                                if (type == long.class){
                                        cf.lloadLocal(pos);
                                        pos += 2;
                                } else if (type == double.class){
                                        cf.dloadLocal(pos);
                                        pos += 2;
                                } else if (type == float.class){
                                        cf.floadLocal(pos++);
                                } else if (type == int.class ||
                                        type == byte.class ||
                                        type == short.class ||
                                        type == char.class ||
                                        type == boolean.class) {
                                        cf.iloadLocal(pos++);
                                }
                        } else {
                                cf.loadLocal(pos++);
                        }
                }
                cf.add(Opcode.INVOKESPECIAL, superclassName, "<init>", sig, "V");
                
                /*
                 * __initialize(this);
                 */
                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.INVOKEVIRTUAL, cf.getClassName(), "__initialize", "()", "V");
                
                cf.add(Opcode.RETURN);
                cf.closeMethod();
        }
        
        /*
         * user defined constructors
         */
        static void constructor(ClassFile cf,
                Context cc,
                Compiler compiler,
                ClassSpec superclassSpec,
                Class[] parameterTypes,
                Class[] exceptionTypes,
                short modifier) {
                String className = cf.getClassName();
                for (int i = 0; i < parameterTypes.length; i++){
                        if (parameterTypes[i] == null){
                                parameterTypes[i] = Object.class;
                        }
                }
                String sig = ClassFile.signature(parameterTypes);
                String methodID = Signature.toJavaIdentifier(sig);
                
                String[] exceptionTypeInfo = null;
                if (exceptionTypes != null && exceptionTypes.length > 0) {
                        exceptionTypeInfo = new String[exceptionTypes.length];
                        for (int j = 0; j < exceptionTypes.length; j++) {
                                exceptionTypeInfo[j] = exceptionTypes[j].getName();
                        }
                }
                
                cf.openMethod("<init>", sig + "V", modifier, exceptionTypeInfo);
                
                
                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.INVOKESPECIAL, superclassSpec.className, "<init>", "()", "V");

                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.INVOKEVIRTUAL, cf.getClassName(), "__initialize", "()", "V");
                
                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.GETFIELD, className, methodID, "Lpnuts/lang/PnutsFunction;");
                int f = cf.getLocal();
                
                cf.storeLocal(f);
                cf.loadLocal(f);
                Label nonnull = cf.getLabel();
                cf.add(Opcode.IFNONNULL, nonnull);
                cf.add(Opcode.RETURN);
                
                nonnull.fix();
                cf.loadLocal(f);
                cf.add(Opcode.CHECKCAST, "pnuts.lang.PnutsFunction");
                
                int nargs = parameterTypes.length;
                if (nargs <= -0x8000 || nargs >= 0x8000) {
                        throw new RuntimeException("too many parameters");
                }
                
                Label catchStart = cf.getLabel(true);
                
                int k = 0;
                
                cf.pushInteger(nargs);
                cf.add(Opcode.ANEWARRAY, "java.lang.Object");
                
                for (int j = 0; j < nargs; j++) {
                        cf.add(Opcode.DUP);
                        cf.pushInteger(j + k);
                        Class paramType = parameterTypes[j];
                        if (paramType != null && paramType.isPrimitive()) {
                                loadPrimitive(cf, paramType, j + 1);
                                if (paramType.equals(long.class) || paramType.equals(double.class)){
                                        j++;
                                }
                        } else {
                                cf.loadLocal(j + 1);
                        }
                        cf.add(Opcode.AASTORE);
                }
                k = 0;
                
                cf.add(Opcode.GETSTATIC, className, "_context", "Lpnuts/lang/Context;");
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsFunction", "call",
                        "([Ljava/lang/Object;Lpnuts/lang/Context;)",
                        "Ljava/lang/Object;");
                
                cf.add(Opcode.POP);
                cf.add(Opcode.RETURN);
                
                
                Label catchEnd = cf.getLabel(true);
                Label catchTarget = cf.getLabel(true);
                cf.reserveStack(1);
                int pex = cf.getLocal();
                cf.storeLocal(pex);
                
                cf.loadLocal(pex);
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsException",
                        "getThrowable", "()", "Ljava/lang/Throwable;");
                int ex = cf.getLocal();
                cf.storeLocal(ex);
                Label next;
                if (exceptionTypeInfo != null) {
                        for (int i = 0; i < exceptionTypeInfo.length; i++) {
                                next = cf.getLabel();
                                cf.loadLocal(ex);
                                cf.add(Opcode.INSTANCEOF, exceptionTypeInfo[i]);
                                cf.add(Opcode.IFEQ, next);
                                cf.loadLocal(ex);
                                cf.add(Opcode.ATHROW);
                                next.fix();
                        }
                }
                
                cf.loadLocal(pex);
                cf.add(Opcode.ATHROW);
                cf.addExceptionHandler(catchStart, catchEnd, catchTarget,
                        "pnuts.lang.PnutsException");
                cf.closeMethod();
        }
        
        
        /*
         * this._superCallProxy = new SuperCallProxy(this);
         */
        static void assignSuperCallProxy(ClassFile cf, String className) {
                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.NEW, SUPER_PROXY_NAME);
                cf.add(Opcode.DUP);
                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.INVOKESPECIAL, SUPER_PROXY_NAME, "<init>",
                        "(Ljava/lang/Object;)", "V");
                cf.add(Opcode.PUTFIELD, className, "_superCallProxy",
                        "Lpnuts/lang/AbstractData;");
        }
        
        /*
         * private AbstractData _superCallProxy;
         *
         * public <Type> $super$<MethodName>(...){ super. <MethodName>(...); } ...
         */
        private static void superCall(ClassFile cf, Class superClass, Set superMethodNames) {
                Method[] methods = ReflectionUtil.getInheritableMethods(superClass);
                for (int i = 0; i < methods.length; i++) {
                        Method m = methods[i];
                        
                        int modifiers = m.getModifiers();
                        if (!Modifier.isStatic(modifiers)
                        && !Modifier.isFinal(modifiers)
                        && (Modifier.isPublic(modifiers) || Modifier
                                .isProtected(modifiers))) {
                                String methodName = m.getName();
                                if (superMethodNames != null && !superMethodNames.contains(methodName)){
                                        continue;
                                }
                                Class[] parameterTypes = m.getParameterTypes();
                                Class returnType = m.getReturnType();
                                
                                cf.openMethod("$super$" + methodName,
                                        ClassFile.signature(parameterTypes) +
                                        ClassFile.signature(returnType),
                                        Constants.ACC_PUBLIC);
                                cf.add(Opcode.ALOAD_0);
                                int nparams = parameterTypes.length;
                                for (int j = 0, k = 0; j < nparams; j++) {
                                        Class type = parameterTypes[j];
                                        if (type.isPrimitive()) {
                                                if (type == int.class || type == byte.class
                                                        || type == short.class || type == boolean.class
                                                        || type == char.class) {
                                                        cf.iloadLocal(j + k + 1);
                                                } else if (type == long.class) {
                                                        cf.lloadLocal(j + k + 1);
                                                        k++;
                                                } else if (type == float.class) {
                                                        cf.floadLocal(j + k + 1);
                                                } else if (type == double.class) {
                                                        cf.dloadLocal(j + k + 1);
                                                        k++;
                                                }
                                        } else {
                                                cf.loadLocal(j + k + 1);
                                        }
                                }
                                
                                cf.add(Opcode.INVOKESPECIAL, m.getDeclaringClass().getName(),
                                        methodName, ClassFile.signature(parameterTypes),
                                        ClassFile.signature(returnType));
                                if (returnType.isPrimitive()) {
                                        if (returnType == int.class || returnType == byte.class
                                                || returnType == short.class
                                                || returnType == boolean.class
                                                || returnType == char.class) {
                                                cf.add(Opcode.IRETURN);
                                        } else if (returnType == long.class) {
                                                cf.add(Opcode.LRETURN);
                                        } else if (returnType == float.class) {
                                                cf.add(Opcode.FRETURN);
                                        } else if (returnType == double.class) {
                                                cf.add(Opcode.DRETURN);
                                        } else if (returnType == Void.TYPE) {
                                                cf.add(Opcode.RETURN);
                                        }
                                } else {
                                        cf.add(Opcode.ARETURN);
                                }
                                cf.closeMethod();
                        }
                }
        }
        
        private static void getImplicitInterfaces(Class cls, List list){
                Class[] interfaces = cls.getInterfaces();
                for (int i = 0; i < interfaces.length; i++){
                        list.add(interfaces[i]);
                }
        }
        
        static Constructor[] getProtectedConstructors(Class cls){
                Set/*<Signature>*/ s = new HashSet(); // signatures
                Set/*<Constructor>*/ cons = new HashSet(); // constructors
                Class c = cls;
                while (c != null){
                        getProtectedConstructors(c, s, cons);
                        c = c.getSuperclass();
                }
                return (Constructor[])cons.toArray(new Constructor[cons.size()]);
        }
        
        static void getProtectedConstructors(Class cls, Set signatures, Set constructors){
                Constructor _constructors[] = cls.getDeclaredConstructors();
                for (int j = 0; j < _constructors.length; j++) {
                        Constructor c = _constructors[j];
                        int modifiers = c.getModifiers();
                        if (!Modifier.isProtected(modifiers) || Modifier.isFinal(modifiers)) {
                                continue;
                        }
                        String sig = ClassFile.signature(c.getParameterTypes());
                        if (signatures.add(sig)){
                                constructors.add(c);
                        }
                }
        }
     
        public static void defineMethod(ClassFile cf, Class[] parameterTypes,
                Class returnType, Class[] exceptionTypes, int modifiers,
                String methodName, String sig, int mode) {
                String className = cf.getClassName();
                String[] exceptionTypeInfo = null;
                if (exceptionTypes != null && exceptionTypes.length > 0) {
                        exceptionTypeInfo = new String[exceptionTypes.length];
                        for (int i = 0; i < exceptionTypes.length; i++) {
                                exceptionTypeInfo[i] = exceptionTypes[i].getName();
                        }
                }
                cf.openMethod(
                        methodName,
                        ClassFile.signature(parameterTypes) + ClassFile.signature(returnType),
                        (short) (modifiers & (Constants.ACC_PUBLIC | Constants.ACC_PROTECTED)),
                        exceptionTypeInfo);
                
                cf.add(Opcode.GETSTATIC, className, "_package", "Lpnuts/lang/Package;");
                cf.add(Opcode.LDC, cf.addConstant(sig));
                cf.add(Opcode.INVOKEVIRTUAL,"java.lang.String", "intern", "()", "Ljava/lang/String;");
                cf.add(Opcode.GETSTATIC, className, "_context", "Lpnuts/lang/Context;");
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.Package", "get",
                        "(Ljava/lang/String;Lpnuts/lang/Context;)",
                        "Ljava/lang/Object;");
//                int f = cf.getLocal();
                cf.add(Opcode.CHECKCAST, "pnuts.lang.PnutsFunction");
                
                int nargs = parameterTypes.length;
                if (nargs <= -0x8000 || nargs >= 0x8000) {
                        throw new RuntimeException("too many parameters");
                }
                boolean hasThis = ((mode & THIS_BIT) == THIS_BIT);
                boolean hasSuper = ((mode & SUPER_BIT) == SUPER_BIT);
                int i = 0;
                
                Label catchStart = cf.getLabel(true);
                
                int nargs2 = nargs;
                if (hasThis) {
                        nargs2++;
                        i++;
                }
                if (hasSuper) {
                        nargs2++;
                        i++;
                }
                
                cf.pushInteger(nargs2);
                cf.add(Opcode.ANEWARRAY, "java.lang.Object");
                
                int k = 0;
                for (int j = 0; k < nargs; j++, k++) {
                        cf.add(Opcode.DUP);
                        cf.pushInteger(k/* + i*/);
                        Class paramType = parameterTypes[k];
                        if (paramType.isPrimitive()) {
                                loadPrimitive(cf, paramType, j + 1);
                                if (paramType.equals(long.class) || paramType.equals(double.class)){
                                        j++;
                                }
                        } else {
                                cf.loadLocal(j + 1);
                        }
                        cf.add(Opcode.AASTORE);
                }
                i = 0;
                if (hasThis) {
                        cf.add(Opcode.DUP);
                        cf.pushInteger(i++);
                        cf.loadLocal(0);
                        cf.add(Opcode.AASTORE);
                }
                if (hasSuper) {
                        cf.add(Opcode.DUP);
                        cf.pushInteger(i++);
                        cf.loadLocal(0);
                        cf.add(Opcode.GETFIELD, className, "_superCallProxy",
                                "Lpnuts/lang/AbstractData;");
                        cf.add(Opcode.AASTORE);
                }
                
                cf.add(Opcode.GETSTATIC, className, "_context", "Lpnuts/lang/Context;");
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsFunction", "call",
                        "([Ljava/lang/Object;Lpnuts/lang/Context;)",
                        "Ljava/lang/Object;");
                
                if (returnType == void.class) {
                        cf.add(Opcode.POP);
                        cf.add(Opcode.RETURN);
                } else if (returnType.isPrimitive()) {
                        returnPrimitive(cf, returnType);
                } else {
                        if (returnType != Object.class) {
                                // TODO: transform array <-> list?
                                cf.add(Opcode.CHECKCAST, returnType.getName());
                        }
                        cf.add(Opcode.ARETURN);
                }
                
                Label catchEnd = cf.getLabel(true);
                Label catchTarget = cf.getLabel(true);
                cf.reserveStack(1);
                int pex = cf.getLocal();
                cf.storeLocal(pex);
                
                cf.loadLocal(pex);
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsException",
                        "getThrowable", "()", "Ljava/lang/Throwable;");
                int ex = cf.getLocal();
                cf.storeLocal(ex);
                Label next;
                if (exceptionTypeInfo != null) {
                        for (i = 0; i < exceptionTypeInfo.length; i++) {
                                next = cf.getLabel();
                                cf.loadLocal(ex);
                                cf.add(Opcode.INSTANCEOF, exceptionTypeInfo[i]);
                                cf.add(Opcode.IFEQ, next);
                                cf.loadLocal(ex);
                                cf.add(Opcode.ATHROW);
                                next.fix();
                        }
                }
                
                
                cf.loadLocal(pex);
                cf.add(Opcode.ATHROW);
                cf.addExceptionHandler(catchStart, catchEnd, catchTarget,
                        "pnuts.lang.PnutsException");
                
                cf.closeMethod();
        }
        
        public static void defineMethod(ClassFile cf, Class[] parameterTypes,
                Class returnType, Class[] exceptionTypes, int modifiers,
                String methodName, String sig, String functionFieldName) {
                String className = cf.getClassName();
                String[] exceptionTypeInfo = null;
                if (exceptionTypes != null && exceptionTypes.length > 0) {
                        exceptionTypeInfo = new String[exceptionTypes.length];
                        for (int i = 0; i < exceptionTypes.length; i++) {
                                exceptionTypeInfo[i] = exceptionTypes[i].getName();
                        }
                }
                cf.openMethod(
                        methodName,
                        ClassFile.signature(parameterTypes) + ClassFile.signature(returnType),
                        (short) (modifiers & (Constants.ACC_PUBLIC | Constants.ACC_PROTECTED)),
                        exceptionTypeInfo);
                cf.add(Opcode.ALOAD_0);
                cf.add(Opcode.GETFIELD, className, functionFieldName, "Lpnuts/lang/PnutsFunction;");
                
                int nargs = parameterTypes.length;
                if (nargs <= -0x8000 || nargs >= 0x8000) {
                        throw new RuntimeException("too many parameters");
                }
                
                Label catchStart = cf.getLabel(true);
                
                cf.pushInteger(nargs);
                cf.add(Opcode.ANEWARRAY, "java.lang.Object");
                
                int k = 0;
                for (int j = 0; k < nargs; j++, k++) {
                        cf.add(Opcode.DUP);
                        cf.pushInteger(k/* + i*/);
                        Class paramType = parameterTypes[k];
                        if (paramType.isPrimitive()) {
                                loadPrimitive(cf, paramType, j + 1);
                                if (paramType.equals(long.class) || paramType.equals(double.class)){
                                        j++;
                                }
                        } else {
                                cf.loadLocal(j + 1);
                        }
                        cf.add(Opcode.AASTORE);
                }
                
                
                cf.add(Opcode.GETSTATIC, className, "_context", "Lpnuts/lang/Context;");
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsFunction", "call",
                        "([Ljava/lang/Object;Lpnuts/lang/Context;)",
                        "Ljava/lang/Object;");
                
                if (returnType == void.class) {
                        cf.add(Opcode.POP);
                        cf.add(Opcode.RETURN);
                } else if (returnType.isPrimitive()) {
                        returnPrimitive(cf, returnType);
                } else {
                        if (returnType != Object.class) {
                                // TODO: transform array <-> list?
                                cf.add(Opcode.CHECKCAST, returnType.getName());
                        }
                        cf.add(Opcode.ARETURN);
                }
                
                Label catchEnd = cf.getLabel(true);
                Label catchTarget = cf.getLabel(true);
                cf.reserveStack(1);
                int pex = cf.getLocal();
                cf.storeLocal(pex);
                
                cf.loadLocal(pex);
                cf.add(Opcode.INVOKEVIRTUAL, "pnuts.lang.PnutsException",
                        "getThrowable", "()", "Ljava/lang/Throwable;");
                int ex = cf.getLocal();
                cf.storeLocal(ex);
                Label next;
                if (exceptionTypeInfo != null) {
                        for (int i = 0; i < exceptionTypeInfo.length; i++) {
                                next = cf.getLabel();
                                cf.loadLocal(ex);
                                cf.add(Opcode.INSTANCEOF, exceptionTypeInfo[i]);
                                cf.add(Opcode.IFEQ, next);
                                cf.loadLocal(ex);
                                cf.add(Opcode.ATHROW);
                                next.fix();
                        }
                }
                
                
                cf.loadLocal(pex);
                cf.add(Opcode.ATHROW);
                cf.addExceptionHandler(catchStart, catchEnd, catchTarget,
                        "pnuts.lang.PnutsException");
                
                cf.closeMethod();
        }
        
        
        private static void loadPrimitive(ClassFile cf, Class primitive, int index) {
                if (primitive == int.class) {
                        cf.add(Opcode.NEW, "java.lang.Integer");
                        cf.add(Opcode.DUP);
                        cf.iloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Integer", "<init>", "(I)",
                                "V");
                } else if (primitive == byte.class) {
                        cf.add(Opcode.NEW, "java.lang.Byte");
                        cf.add(Opcode.DUP);
                        cf.iloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Byte", "<init>",
                                "(B)", "V");
                } else if (primitive == short.class) {
                        cf.add(Opcode.NEW, "java.lang.Short");
                        cf.add(Opcode.DUP);
                        cf.iloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Short", "<init>", "(S)",
                                "V");
                } else if (primitive == char.class) {
                        cf.add(Opcode.NEW, "java.lang.Character");
                        cf.add(Opcode.DUP);
                        cf.iloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Character", "<init>",
                                "(C)", "V");
                } else if (primitive == long.class) {
                        cf.add(Opcode.NEW, "java.lang.Long");
                        cf.add(Opcode.DUP);
                        cf.lloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Long", "<init>",
                                "(J)", "V");
                } else if (primitive == float.class) {
                        cf.add(Opcode.NEW, "java.lang.Float");
                        cf.add(Opcode.DUP);
                        cf.floadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Float", "<init>", "(F)",
                                "V");
                } else if (primitive == double.class) {
                        cf.add(Opcode.NEW, "java.lang.Double");
                        cf.add(Opcode.DUP);
                        cf.dloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Double", "<init>", "(D)",
                                "V");
                } else if (primitive == boolean.class) {
                        cf.add(Opcode.NEW, "java.lang.Boolean");
                        cf.add(Opcode.DUP);
                        cf.iloadLocal(index);
                        cf.add(Opcode.INVOKESPECIAL, "java.lang.Boolean", "<init>", "(Z)",
                                "V");
                }
        }
        
        private static void returnPrimitive(ClassFile cf, Class type) {
                if (type == int.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Integer");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Integer", "intValue", "()",
                                "I");
                        cf.add(Opcode.IRETURN);
                } else if (type == byte.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Byte");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Byte", "byteValue", "()",
                                "B");
                        cf.add(Opcode.IRETURN);
                } else if (type == short.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Short");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Short", "shortValue", "()",
                                "S");
                        cf.add(Opcode.IRETURN);
                } else if (type == char.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Character");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Character", "charValue",
                                "()", "C");
                        cf.add(Opcode.IRETURN);
                } else if (type == long.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Long");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Long", "longValue", "()",
                                "L");
                        cf.add(Opcode.LRETURN);
                } else if (type == float.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Float");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Float", "floatValue", "()",
                                "F");
                        cf.add(Opcode.FRETURN);
                } else if (type == double.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Double");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Double", "doubleValue",
                                "()", "D");
                        cf.add(Opcode.DRETURN);
                } else if (type == boolean.class) {
                        cf.add(Opcode.CHECKCAST, "java.lang.Boolean");
                        cf.add(Opcode.INVOKEVIRTUAL, "java.lang.Boolean", "booleanValue",
                                "()", "Z");
                        cf.add(Opcode.IRETURN);
                } else {
                        throw new InternalError();
                }
        }
        
        
        static class MethodArity {
                String name;
                int nargs;
                
                MethodArity(String name, int nargs){
                        this.name = name;
                        this.nargs = nargs;
                }
                
                public int hashCode(){
                        return name.hashCode() * nargs;
                }
                
                public boolean equals(Object obj){
                        if (obj instanceof MethodArity){
                                MethodArity a = (MethodArity)obj;
                                return a.nargs == nargs && a.name.equals(name);
                        }
                        return false;
                }
        }
        
        private static class ThisTransformer extends ScopeAnalyzer {
                
                private Set methods;
                private Set fields;
                private Set nodesNeedTransformation;
                
                ThisTransformer(Set methods, Set fields, Set nodesNeedTransformation){
                        this.methods = methods;
                        this.fields = fields;
                        this.nodesNeedTransformation = nodesNeedTransformation;
                }
                
                protected void handleFreeVariable(SimpleNode node, Context context){
                        SimpleNode parent = node.jjtGetParent();
                        if (parent != null && parent.id == PnutsParserTreeConstants.JJTAPPLICATIONNODE){
                                String symbol = node.str;
                                SimpleNode argNode = parent.jjtGetChild(1);
                                int n = argNode.jjtGetNumChildren();
                                
                                MethodArity arity = new MethodArity(symbol, n);
                                if (methods.contains(arity)){
                                        nodesNeedTransformation.add(parent);
                                }
                        } else if (parent != null && parent.id != PnutsParserTreeConstants.JJTMEMBERNODE){
                                if (fields.contains(node.str)){
                                        nodesNeedTransformation.add(node);
                                }
                        }
                }
                
                protected void declared(SimpleNode node, Context context, String symbol){
                        if (fields.contains(node.str)){
                                nodesNeedTransformation.add(node);
                        }
                }
        }
        
        private static void populateMembers(SimpleNode classDefBody, Set methods, Set fields){
                for (int i = 0, n = classDefBody.jjtGetNumChildren(); i < n; i++){
                        SimpleNode c = classDefBody.jjtGetChild(i);
                        String name;
                        int arity;
                        if (c.id == PnutsParserTreeConstants.JJTMETHODDEF){
                                name = c.str;
                                int num = c.jjtGetNumChildren();
                                if (num == 2){
                                        SimpleNode typedParamList = c.jjtGetChild(0);
                                        arity = typedParamList.jjtGetNumChildren();
                                } else if (num == 3){
                                        SimpleNode typedParamList = c.jjtGetChild(1);
                                        arity = typedParamList.jjtGetNumChildren();
                                } else {
                                        throw new InternalError();
                                }
                                methods.add(new MethodArity(name, arity));
                        } else if (c.id == PnutsParserTreeConstants.JJTFIELDDEF){
                                fields.add(c.str);
                        }
                }
        }
        
        private static void populateMethodArities(Class cls, Set methods){
                Method m[] = cls.getMethods();
                for (int i = 0; i < m.length; i++){
                        MethodArity a = new MethodArity(m[i].getName(), m[i].getParameterTypes().length);
                        methods.add(a);
                }
        }
        
        private static void populateFieldNames(Class cls, final Set fields){
                ObjectDescFactory.getDefault().create(cls).handleProperties(new PropertyHandler(){
                        public void handle(String propertyName, Class type, Method readMethod, Method writeMethod){
                                fields.add(propertyName);
                        }
                });
        }
        
        private static void transformApplicationNode(SimpleNode node){
                node.id = PnutsParserTreeConstants.JJTMETHODNODE;
                SimpleNode n0 = node.jjtGetChild(0);
                node.str = n0.str;
                n0.str = Compiler.THIS;
        }
        
        private static void transformApplicationNode(SimpleNode node, Set methods, Set fields){
                Set nodes = new HashSet();
                new ThisTransformer(methods, fields, nodes).analyze(node);
                for (Iterator it = nodes.iterator(); it.hasNext();){
                        SimpleNode n = (SimpleNode)it.next();
                        if (n.id == PnutsParserTreeConstants.JJTAPPLICATIONNODE){
                                transformApplicationNode(n);
                        }
                }
        }
        
        public static void transformClassDefBody(SimpleNode classDefBody, Class superclass){
                Set methods = new HashSet();
                Set fields = new HashSet();
                populateMethodArities(superclass, methods);
                populateFieldNames(superclass, fields);
                populateMembers(classDefBody, methods, fields);
                transformApplicationNode(classDefBody, methods, fields);
        }
        
        public static class SuperCallProxy implements AbstractData {
                private Object target;
                
                public SuperCallProxy(Object target) {
                        this.target = target;
                }
                
                public Object get(String name, Context context) {
                        throw new UnsupportedOperationException();
                }
                
                public void set(String name, Object value, Context context) {
                        throw new UnsupportedOperationException();
                }
                
                public Object invoke(String name, Object[] args, Context context) {
                        return Runtime.callMethod(context, target.getClass(), "$super$"
                                + name, args, null, target);
                }
        }
}
