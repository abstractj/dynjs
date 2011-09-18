package org.dynjs.runtime.linker;

import org.dynalang.dynalink.linker.CallSiteDescriptor;
import org.dynalang.dynalink.linker.GuardedInvocation;
import org.dynalang.dynalink.linker.GuardingDynamicLinker;
import org.dynalang.dynalink.linker.GuardingTypeConverterFactory;
import org.dynalang.dynalink.linker.LinkRequest;
import org.dynalang.dynalink.linker.LinkerServices;
import org.dynjs.api.Scope;
import org.dynjs.runtime.Converters;
import org.dynjs.runtime.DynAtom;
import org.dynjs.runtime.DynNumber;
import org.dynjs.runtime.DynString;
import org.dynjs.runtime.RT;
import org.dynjs.runtime.primitives.DynPrimitiveNumber;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

public class DynJSLinker implements GuardingDynamicLinker, GuardingTypeConverterFactory {

    @Override
    public GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices) throws Exception {
        CallSiteDescriptor callSiteDescriptor = linkRequest.getCallSiteDescriptor();
        MethodType methodType = callSiteDescriptor.getMethodType();
        if ("print".equals(callSiteDescriptor.getName())) {
            MethodHandle print = lookup().findStatic(RT.class, "print", methodType);
            return new GuardedInvocation(print, null);
        } else if (callSiteDescriptor.getName().startsWith("dyn:getProp")) {
            MethodType targetType = methodType(DynAtom.class, String.class);
            MethodHandle getProperty = lookup().findVirtual(Scope.class, "resolve", targetType);
            return new GuardedInvocation(getProperty, null);
        } else if (callSiteDescriptor.getName().startsWith("dynjs:bop")) {
            if (linkRequest.getArguments().length == 2) {
                MethodType targetType = methodType(DynNumber.class, DynNumber.class);
                String op = linkRequest.getCallSiteDescriptor().getNameToken(2);
                MethodHandle opMH = lookup().findVirtual(DynNumber.class, op, targetType);
                MethodHandle targetHandle = linkerServices.asType(opMH, callSiteDescriptor.getMethodType());
                return new GuardedInvocation(targetHandle, null);
            }
        }
        return null;
    }

    @Override
    public GuardedInvocation convertToType(Class<?> sourceType, Class<?> targetType) {
        if (DynString.class.isAssignableFrom(sourceType) && String.class == targetType) {
            return Converters.Guarded_DynString2String;
        } else if (DynPrimitiveNumber.class.isAssignableFrom(sourceType) && DynNumber.class == targetType) {
            return Converters.Guarded_DynPrimitiveNumber2DynNumber;
        }
        return null;
    }
}
