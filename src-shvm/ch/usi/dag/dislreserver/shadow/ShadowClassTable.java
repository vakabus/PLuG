package ch.usi.dag.dislreserver.shadow;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.objectweb.asm.Type;

import ch.usi.dag.dislreserver.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.util.Logging;
import ch.usi.dag.util.asm.ClassNodeHelper;
import ch.usi.dag.util.logging.Logger;


public final class ShadowClassTable {

    private static final Logger __log = Logging.getPackageInstance ();

    //

    private static final int __INITIAL_SIZE__ = 10000;

    //

    static final ShadowObject BOOTSTRAP_CLASSLOADER;

    static final AtomicReference <ShadowClass> JAVA_LANG_CLASS;

    private static final Type __JAVA_LANG_CLASS_TYPE__;

    //

    private static final ConcurrentHashMap <Integer, ShadowClass> shadowClasses;

    private static final ConcurrentHashMap <ShadowObject, ConcurrentHashMap <String, byte []>> classLoaderMap;

    //

    static {
        JAVA_LANG_CLASS = new AtomicReference <> ();
        __JAVA_LANG_CLASS_TYPE__ = Type.getType (Class.class);

        shadowClasses = new ConcurrentHashMap <> (__INITIAL_SIZE__);

        classLoaderMap = new ConcurrentHashMap <> (__INITIAL_SIZE__);

        BOOTSTRAP_CLASSLOADER = new ShadowObject (0, null);
        classLoaderMap.put (BOOTSTRAP_CLASSLOADER, new ConcurrentHashMap <> ());
    }

    //

    public static void load (
        ShadowObject loader, final String className,
        final byte [] classCode, final boolean debug
    ) {
        ConcurrentHashMap <String, byte []> classNameMap;
        if (loader == null) {
            // bootstrap loader
            loader = BOOTSTRAP_CLASSLOADER;
        }

        classNameMap = classLoaderMap.get (loader);
        if (classNameMap == null) {
            final ConcurrentHashMap <String, byte []> tmp = new ConcurrentHashMap <> ();
            if ((classNameMap = classLoaderMap.putIfAbsent (loader, tmp)) == null) {
                classNameMap = tmp;
            }
        }

        if (classNameMap.putIfAbsent (className.replace ('/', '.'), classCode) != null) {
            if (debug) {
                System.out.println ("DiSL-RE: Reloading/Redefining class "+ className);
            }
        }
    }

    //

    static ShadowClass newInstance (
        final long classNetReference, final Type type,
        final String classGenericStr, final ShadowObject classLoader,
        final ShadowClass superClass
    ) {
        if (!NetReferenceHelper.isClassInstance (classNetReference)) {
            throw new DiSLREServerFatalException (String.format (
                "Not a Class<> instance: 0x%x", classNetReference
            ));
        }

        //

        final int classId = NetReferenceHelper.getClassId (classNetReference);

        ShadowClass result = shadowClasses.get (classId);
        if (result == null) {
            result = __createShadowClass (
                classNetReference, type, superClass,
                classLoader
            );

            final ShadowClass previous = shadowClasses.putIfAbsent (classId, result);
            if (previous == null) {
                ShadowObjectTable.register (result);

            } else if (previous.equals (result)) {
                result = previous;

            } else {
                // Someone else just created a class with the same class id,
                // but for some reason the classes are not equal.
                throw new DiSLREServerFatalException (String.format (
                    "different shadow classes (%s) for id (0x%x)",
                    type, classId
                ));
            }
        }

        //

        if (JAVA_LANG_CLASS.get () == null && __JAVA_LANG_CLASS_TYPE__.equals (type)) {
            JAVA_LANG_CLASS.compareAndSet (null, result);
            __log.trace ("initialized JAVA_LANG_CLASS");
        }

        return result;
    }


    private static ShadowClass __createShadowClass (
        final long netReference, final Type type,
        final ShadowClass superClass, ShadowObject classLoader
    ) {
        //
        // Assumes that the sort of primitive types is lexicographically
        // before the sort of arrays and subsequently objects.
        //
        if (type.getSort () < Type.ARRAY) {
            // Primitive type should return null as classloader.
            return new PrimitiveShadowClass (netReference, type, classLoader);

        } else if (type.getSort () == Type.ARRAY) {
            // TODO unknown array component type
            // Array types have the same class loader as their component type.
            return new ArrayShadowClass (netReference, type, classLoader, superClass, null);

        } else if (type.getSort () == Type.OBJECT) {
            if (classLoader == null) {
                // bootstrap loader
                classLoader = BOOTSTRAP_CLASSLOADER;
            }

            final ConcurrentHashMap <String, byte []> classNameMap = classLoaderMap.get (classLoader);
            if (classNameMap == null) {
                throw new DiSLREServerFatalException ("Unknown class loader");
            }

            final byte [] classCode = classNameMap.get (type.getClassName ());
            if (classCode == null || classCode.length == 0) {
                throw new DiSLREServerFatalException (
                    "No bytecode provided for class "+ type.getClassName ()
                );
            }

            return new ObjectShadowClass (
                netReference, type, classLoader, superClass,
                ClassNodeHelper.OUTLINE.unmarshal (classCode)
            );

        } else {
            throw new DiSLREServerFatalException ("unpextected sort of type: "+ type.getSort ());
        }
    }


    static ShadowClass get (final int classId) {
        if (classId == 0) {
            // reserved for java.lang.Class
            return null;
        }

        final ShadowClass result = shadowClasses.get (classId);
        if (result != null) {
            return result;
        }

        throw new DiSLREServerFatalException (String.format (
            "Unknown Class<> instance: 0x%x", classId
        ));
    }


    static void freeShadowObject (final ShadowObject object) {
        final long netReference = object.getNetRef ();
        if (NetReferenceHelper.isClassInstance (netReference)) {
            final int classId = NetReferenceHelper.getClassId (netReference);
            shadowClasses.remove (classId);

        } else if (classLoaderMap.containsKey (object)) {
            classLoaderMap.remove (object);
        }
    }


    public static void registerClass (
        final long netReference, final String typeDescriptor,
        final String classGenericStr, final long classLoaderNetReference,
        final long superclassNetReference
    ) {
        final Type type = Type.getType (typeDescriptor);
        final ShadowObject classLoader = ShadowObjectTable.get (classLoaderNetReference);
        final ShadowClass superClass = (ShadowClass) ShadowObjectTable.get (superclassNetReference);
        newInstance (netReference, type, classGenericStr, classLoader, superClass);
    }

}
