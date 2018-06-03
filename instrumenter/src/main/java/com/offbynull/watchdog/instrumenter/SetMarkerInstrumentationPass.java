/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
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

import static com.offbynull.watchdog.instrumenter.InternalFields.INSTRUMENTED_MARKER_FIELD_ACCESS;
import static com.offbynull.watchdog.instrumenter.InternalFields.INSTRUMENTED_MARKER_FIELD_NAME;
import static com.offbynull.watchdog.instrumenter.InternalFields.INSTRUMENTED_MARKER_FIELD_TYPE;
import static com.offbynull.watchdog.instrumenter.InternalFields.INSTRUMENTED_MARKER_FIELD_VALUE;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

final class SetMarkerInstrumentationPass implements InstrumentationPass {

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        FieldNode fieldNode = new FieldNode(
                INSTRUMENTED_MARKER_FIELD_ACCESS,
                INSTRUMENTED_MARKER_FIELD_NAME,
                INSTRUMENTED_MARKER_FIELD_TYPE.getDescriptor(),
                null,
                INSTRUMENTED_MARKER_FIELD_VALUE);

        classNode.fields.add(fieldNode);
    }
}
