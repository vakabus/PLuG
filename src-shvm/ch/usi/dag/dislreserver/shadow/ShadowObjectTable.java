package ch.usi.dag.dislreserver.shadow;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dag.dislreserver.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.util.Logging;
import ch.usi.dag.util.logging.Logger;


public final class ShadowObjectTable {

    private static final Logger __log = Logging.getPackageInstance ();

    //

    private static final int INITIAL_TABLE_SIZE = 10_000_000;

    private static final ConcurrentHashMap <Long, ShadowObject>
        shadowObjects = new ConcurrentHashMap <> (INITIAL_TABLE_SIZE);

    //

    public static void register (final ShadowObject object) {
        if (object == null) {
            __log.warn ("attempting to register a null shadow object");
            return;
        }

        //

        final long objectId = object.getId ();
        final ShadowObject existing = shadowObjects.putIfAbsent (objectId, object);
        if (existing != null) {
            if (__log.traceIsLoggable ()) {
                __log.trace ("updating shadow object 0x%x", objectId);
            }

            existing.updateFrom (object);
        }
    }


    private static boolean isAssignableFromThread (ShadowClass klass) {
        while (!"java.lang.Object".equals (klass.getName ())) {
            if ("java.lang.Thread".equals (klass.getName ())) {
                return true;
            }

            klass = klass.getSuperclass ();
        }

        return false;
    }


    public static ShadowObject get (final long netReference) {
        final long objectId = NetReferenceHelper.getObjectId (netReference);
        if (objectId == 0) {
            // reserved for null
            return null;
        }

        final ShadowObject retVal = shadowObjects.get (objectId);
        if (retVal != null) {
            return retVal;
        }

        //
        // The corresponding shadow object was not found, so we create it.
        // Only "normal" shadow objects will be generated here, not those
        // representing instances of the Class class.
        //
        if (!NetReferenceHelper.isClassInstance (netReference)) {
            return shadowObjects.computeIfAbsent (
                objectId, key -> __createShadowObject (netReference)
            );
        } else {
            throw new DiSLREServerFatalException ("Unknown class instance");
        }
    }


    private static ShadowObject __createShadowObject (
        final long netReference
    ) {
        final ShadowClass shadowClass = ShadowClassTable.get (
            NetReferenceHelper.getClassId (netReference)
        );

        if ("java.lang.String".equals (shadowClass.getName ())) {
            if (__log.traceIsLoggable ()) {
                final long objectId = NetReferenceHelper.getObjectId (netReference);
                __log.trace ("creating uninitialized ShadowString for 0x%x", objectId);
            }

            return new ShadowString (netReference, shadowClass);

        } else if (isAssignableFromThread (shadowClass)) {
            if (__log.traceIsLoggable ()) {
                final long objectId = NetReferenceHelper.getObjectId (netReference);
                __log.trace ("creating uninitialized ShadowThread for 0x%x", objectId);
            }

            return new ShadowThread (netReference, shadowClass);

        } else {
            if (__log.traceIsLoggable ()) {
                final long objectId = NetReferenceHelper.getObjectId (netReference);
                __log.trace ("creating ShadowObject for 0x%x", objectId);
            }

            return new ShadowObject (netReference, shadowClass);
        }
    }


    public static void freeShadowObject (final ShadowObject obj) {
        shadowObjects.remove (obj.getId ());
        ShadowClassTable.freeShadowObject (obj);
    }


    // TODO: find a more elegant way to allow users to traverse the shadow
    // object table
    public static Iterator <Entry <Long, ShadowObject>> getIterator () {
        return shadowObjects.entrySet ().iterator ();
    }


    public static Iterable <ShadowObject> objects () {
        return new Iterable <ShadowObject> () {
            @Override
            public Iterator <ShadowObject> iterator () {
                return shadowObjects.values ().iterator ();
            }
        };
    }


    // TODO LB: Make this interface per-shadow-world instead of static.

    public static void registerShadowThread (
        final long netReference, final String name, final boolean isDaemon
    ) {
        final int shadowClassId = NetReferenceHelper.getClassId (netReference);
        final ShadowClass shadowClass = ShadowClassTable.get (shadowClassId);
        final ShadowThread shadowThread = new ShadowThread (
            netReference, shadowClass, name, isDaemon
        );

        register (shadowThread);
    }


    public static void registerShadowString (
        final long netReference, final String value
    ) {
        final int shadowClassId = NetReferenceHelper.getClassId (netReference);
        final ShadowClass shadowClass = ShadowClassTable.get (shadowClassId);
        final ShadowString shadowString = new ShadowString (
            netReference, shadowClass, value
        );

        register (shadowString);
    }

}
