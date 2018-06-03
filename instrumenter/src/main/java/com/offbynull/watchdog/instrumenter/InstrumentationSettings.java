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

import com.offbynull.watchdog.instrumenter.generators.DebugGenerators.MarkerType;
import org.apache.commons.lang3.Validate;

/**
 * Instrumentation settings.
 * @author Kasra Faghihi
 */
public final class InstrumentationSettings {
    private final MarkerType markerType;

    /**
     * Constructs a {@link InstrumentationSettings} object.
     * @param markerType marker type
     * @throws NullPointerException if any argument is {@code null}
     */
    public InstrumentationSettings(MarkerType markerType) {
        Validate.notNull(markerType);
        this.markerType = markerType;
    }

    /**
     * Get marker type. Depending on the marker type used, markers will be added to the instrumented code that explains what each portion of
     * the instrumented code is doing. This is useful for debugging the instrumentation logic (if the instrumented code is bugged).
     * @return marker type
     */
    public MarkerType getMarkerType() {
        return markerType;
    }

}
