package ch.usi.dag.dislreserver.shadow;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.objectweb.asm.Type;


final class PrimitiveShadowClass extends ShadowClass {

    PrimitiveShadowClass (
        final long netReference, final Type type,
        final ShadowObject classLoader
    ) {
        super (netReference, type, classLoader);
    }

    //

    @Override
    public int getModifiers () {
        //
        // Primitive type classes are ABSTRACT, FINAL, and PUBLIC.
        //
        return Modifier.ABSTRACT | Modifier.FINAL | Modifier.PUBLIC;
    }

    //

    /**
     * @see Class#isInstance(Object)
     */
    @Override
    public boolean isInstance (final ShadowObject object) {
        return false;
    }


    /**
     * @see Class#isAssignableFrom(Class)
     */
    @Override
    public boolean isAssignableFrom (final ShadowClass other) {
        return this.equals (other);
    }

    //

    /**
     * @see Class#getName()
     */
    @Override
    public String getName () {
        // Avoid Type.getInternalName() -- returns null for primitive types.
        return getCanonicalName ();
    }

    //

    /**
     * @see Class#getSuperclass()
     */
    @Override
    public ShadowClass getSuperclass () {
        return null;
    }


    @Override
    public ShadowClass [] getInterfaces () {
        return new ShadowClass [0];
    }


    @Override
    public String [] getInterfaceDescriptors () {
        return new String [0];
    }

    //

    @Override
    protected Stream <FieldInfo> _declaredFields () {
        // Primitive types have no declared fields.
        return Stream.empty ();
    }


    @Override
    protected Stream <MethodInfo> _declaredMethods () {
        // Primitive types have no declared methods.
        return Stream.empty ();
    }

}
