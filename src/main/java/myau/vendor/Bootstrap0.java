package myau.vendor;

import java.lang.invoke.MethodHandle;

/**
 * Restored from the 2,192-byte embedded classfile in {@code x64-windows.dll}
 * ({@code .rdata} {@code 0x1806E9050}):
 * {@code antiLeak-1a2786b1-7d43-4a96-b4bc-752eb89b1600/bootstrap/Bootstrap0}.
 *
 * <p>Defined at runtime via JNI {@code DefineClass} alongside
 * {@link Loader#registerNativesForClass(int, Class)} so native-obfuscated
 * methods can reverse-invoke resolved {@link MethodHandle} targets across JNI
 * without exposing target descriptors at call sites. Each bridge method in the
 * raw classfile carries {@code ACC_PUBLIC | ACC_STATIC | ACC_BRIDGE | ACC_SYNTHETIC}
 * ({@code 0x1049}) and {@code @LambdaForm.Hidden} / {@code @Hidden} annotations.
 *
 * <p>Restored for structural inspection only; never referenced by active mod code.
 */
public class Bootstrap0 {
    public static int invokereverse0(int a, long b, MethodHandle handle) throws Throwable {
        return (int) handle.invoke(a, b);
    }

    public static Object invokereverse1(MethodHandle handle) throws Throwable {
        return handle.invoke();
    }

    public static Object invokereverse2(int a, long b, MethodHandle handle) throws Throwable {
        return handle.invoke(a, b);
    }

    public static long invokereverse3(int a, long b, MethodHandle handle) throws Throwable {
        return (long) handle.invoke(a, b);
    }

    public static Object invokereverse4(Object arg, MethodHandle handle) throws Throwable {
        return handle.invoke(arg);
    }
}
