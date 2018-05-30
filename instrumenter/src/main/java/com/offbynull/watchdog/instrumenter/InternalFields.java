/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.watchdog.instrumenter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class InternalFields {

    private InternalFields() {
        // does nothing
    }

    // The following consts are used to determine if the class being instrumented is already instrumented + to make sure that if it is
    // instrumented that it's instrumented with this version of the instrumenter 
    static final int INSTRUMENTED_MARKER_FIELD_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC;
    static final Type INSTRUMENTED_MARKER_FIELD_TYPE = Type.LONG_TYPE;
    static final String INSTRUMENTED_MARKER_FIELD_NAME = "__WATCHDOG_INSTRUMENTATION_VERSION";
    static final Long INSTRUMENTED_MARKER_FIELD_VALUE = 0L;
}
