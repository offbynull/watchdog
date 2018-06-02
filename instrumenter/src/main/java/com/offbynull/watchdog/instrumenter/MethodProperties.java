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

import com.offbynull.watchdog.instrumenter.asm.VariableTable;
import com.offbynull.watchdog.instrumenter.asm.VariableTable.Variable;
import org.apache.commons.lang3.Validate;

final class MethodProperties {
    private final VariableTable variableTable;
    private final Variable watchdogVariable;
    private final boolean argMode;

    MethodProperties(VariableTable variableTable, Variable watchdogVariable, boolean argMode) {
        Validate.notNull(variableTable);
        Validate.notNull(watchdogVariable);
        this.variableTable = variableTable;
        this.watchdogVariable = watchdogVariable;
        this.argMode = argMode;
    }

    public VariableTable variableTable() {
        return variableTable;
    }

    public Variable watchdogVariable() {
        return watchdogVariable;
    }

    public boolean argMode() {
        return argMode;
    }
    
}
