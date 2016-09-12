/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.translate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ModificationCacheTest {

    private ModificationCache cache;

    @Before
    public void setUp() throws Exception {
        cache = new ModificationCache();
    }

    @Test
    public void get() throws Exception {
        final Object o = new Object();
        assertNull(cache.get(o));
        assertFalse(cache.containsKey(o));

        assertNull(cache.put(o, o));

        assertTrue(cache.containsKey(o));
        assertEquals(o, cache.get(o));
        assertEquals(o, cache.put(o, o));

        cache.close();
        assertFalse(cache.containsKey(o));
    }

}