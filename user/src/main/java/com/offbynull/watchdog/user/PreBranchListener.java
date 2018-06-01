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
package com.offbynull.watchdog.user;

/**
 * Listens for branch instructions. This includes all jump and switch instructions, but not try-catch blocks.
 * @author Kasra Faghihi
 */
public interface PreBranchListener {
    /**
     * Code is about to walk over a branch instruction. <b>Implementations must complete quickly and must not block</b>.
     */
    void preBranchInstruction();
}
