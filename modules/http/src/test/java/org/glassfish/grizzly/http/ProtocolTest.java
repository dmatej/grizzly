/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.http;

import static org.glassfish.grizzly.http.Protocol.HTTP_0_9;
import static org.glassfish.grizzly.http.Protocol.HTTP_1_0;
import static org.glassfish.grizzly.http.Protocol.HTTP_1_1;
import static org.glassfish.grizzly.http.Protocol.HTTP_2_0;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test {@link Protocol}.
 */
public class ProtocolTest {

    @Test
    public void testIsAtLeastReflexive() {
        for (Protocol protocol : Protocol.values()) {
            assertTrue(protocol + " should be at least itself", protocol.isAtLeast(protocol));
        }
    }

    @Test
    public void testIsAtLeastHigherThanArgument() {
        assertTrue(HTTP_1_0.isAtLeast(HTTP_0_9));
        assertTrue(HTTP_1_1.isAtLeast(HTTP_0_9));
        assertTrue(HTTP_1_1.isAtLeast(HTTP_1_0));
        assertTrue(HTTP_2_0.isAtLeast(HTTP_0_9));
        assertTrue(HTTP_2_0.isAtLeast(HTTP_1_0));
        assertTrue(HTTP_2_0.isAtLeast(HTTP_1_1));
    }

    @Test
    public void testIsAtLeastLowerThanArgument() {
        assertFalse(HTTP_0_9.isAtLeast(HTTP_1_0));
        assertFalse(HTTP_0_9.isAtLeast(HTTP_1_1));
        assertFalse(HTTP_0_9.isAtLeast(HTTP_2_0));
        assertFalse(HTTP_1_0.isAtLeast(HTTP_1_1));
        assertFalse(HTTP_1_0.isAtLeast(HTTP_2_0));
        assertFalse(HTTP_1_1.isAtLeast(HTTP_2_0));
    }

    /**
     * {@link Protocol#isAtLeast(Protocol)} relies on the enum constants being declared in ascending version order.
     * This test guards against accidental reordering.
     */
    @Test
    public void testEnumDeclarationOrder() {
        Protocol[] values = Protocol.values();
        for (int i = 1; i < values.length; i++) {
            Protocol previous = values[i - 1];
            Protocol current = values[i];
            boolean higher = current.getMajorVersion() > previous.getMajorVersion()
                    || current.getMajorVersion() == previous.getMajorVersion()
                            && current.getMinorVersion() > previous.getMinorVersion();
            assertTrue(current + " must be declared after " + previous + " in ascending version order", higher);
        }
    }
}
