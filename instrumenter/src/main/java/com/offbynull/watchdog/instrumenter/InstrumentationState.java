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

import com.offbynull.watchdog.instrumenter.asm.ClassInformationRepository;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.tree.MethodNode;

final class InstrumentationState {
    private final InstrumentationSettings instrumentationSettings;
    private final ClassInformationRepository classInformationRepository;
    
    private final Map<String, byte[]> extraFiles;

    private final Map<MethodNode, MethodProperties> identifiedMethods;
    
    private ControlFlag controlFlag;

    InstrumentationState(InstrumentationSettings instrumentationSettings, ClassInformationRepository classInformationRepository) {
        Validate.notNull(instrumentationSettings);
        Validate.notNull(classInformationRepository);
        this.instrumentationSettings = instrumentationSettings;
        this.classInformationRepository = classInformationRepository;
        
        this.extraFiles = new HashMap<>();
        
        this.controlFlag = ControlFlag.CONTINUE_INSTRUMENT;
        
        this.identifiedMethods = new HashMap<>();
    }

    InstrumentationSettings instrumentationSettings() {
        return instrumentationSettings;
    }

    ClassInformationRepository classInformationRepository() {
        return classInformationRepository;
    }

    Map<String, byte[]> extraFiles() {
        return extraFiles;
    }

    public Map<MethodNode, MethodProperties> identifiedMethods() {
        return identifiedMethods;
    }

    void control(ControlFlag control) {
        Validate.notNull(control);
        this.controlFlag = control;
    }

    ControlFlag control() {
        return controlFlag;
    }



    enum ControlFlag {
        CONTINUE_INSTRUMENT, //Continue passing forward to further instrumenters.
        NO_INSTRUMENT //Stop passing forward to further instrumenters -- it has been determined that this class must not be instrumented.
    }
}
