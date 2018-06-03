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

import static com.offbynull.watchdog.instrumenter.InstrumentationState.ControlFlag.NO_INSTRUMENT;
import static com.offbynull.watchdog.instrumenter.InternalFields.INSTRUMENTED_MARKER_FIELD_NAME;
import static com.offbynull.watchdog.instrumenter.InternalFields.INSTRUMENTED_MARKER_FIELD_VALUE;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

final class CheckMarkerInstrumentationPass implements InstrumentationPass {

    @Override
    public void pass(ClassNode classNode, InstrumentationState state) {
        FieldNode fieldNode = classNode.fields.stream()
                .filter(f -> f.name.equals(INSTRUMENTED_MARKER_FIELD_NAME))
                .findAny().orElse(null);
        
        if (fieldNode == null) {
            return;
        }
        
        Validate.isTrue(
                ((Long) fieldNode.value) == INSTRUMENTED_MARKER_FIELD_VALUE,
                "Class %s instrumented with different version of instrumented: %d", classNode.name, fieldNode.value);

        state.control(NO_INSTRUMENT);
    }
}
