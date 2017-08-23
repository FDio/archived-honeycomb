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

package io.fd.honeycomb.translate.util.read.cache;

import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DumpCacheManagerTest {

    @Mock
    private EntityDumpExecutor<IpDetailsReplyDump, Void> executor;
    private InstanceIdentifier<DataObj> identifier;
    private DumpCacheManager<IpDetailsReplyDump, Void> managerPositive;
    private DumpCacheManager<IpDetailsReplyDump, Void> managerPositiveWithPostProcessing;
    private DumpCacheManager<IpDetailsReplyDump, Void> managerNegative;
    private ModificationCache cache;
    private CacheKeyFactory cacheKeyFactory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        managerPositive =
                new DumpCacheManager.DumpCacheManagerBuilder<IpDetailsReplyDump, Void>()
                        .withExecutor(executor)
                        .acceptOnly(IpDetailsReplyDump.class)
                        .build();

        managerPositiveWithPostProcessing =
                new DumpCacheManager.DumpCacheManagerBuilder<IpDetailsReplyDump, Void>()
                        .withExecutor(executor)
                        .acceptOnly(IpDetailsReplyDump.class)
                        .withPostProcessingFunction(createPostProcessor())
                        .build();

        managerNegative =
                new DumpCacheManager.DumpCacheManagerBuilder<IpDetailsReplyDump, Void>()
                        .withExecutor(executor)
                        .acceptOnly(IpDetailsReplyDump.class)
                        .build();

        cache = new ModificationCache();
        identifier = InstanceIdentifier.create(DataObj.class);
        //manager uses this implementation by default, so it can be used to test behaviour
        cacheKeyFactory = new TypeAwareIdentifierCacheKeyFactory(IpDetailsReplyDump.class);

    }

    /**
     * This test verify full dump-caching cycle
     */
    @Test
    public void testCaching() throws ReadFailedException {
        final IpDetailsReplyDump stage1Data = new IpDetailsReplyDump();
        final String key = cacheKeyFactory.createKey(identifier, NO_PARAMS);


        // executor cant return null data
        when(executor.executeDump(identifier, NO_PARAMS)).thenReturn(new IpDetailsReplyDump());

        final Optional<IpDetailsReplyDump> stage1Optional = managerNegative.getDump(identifier, cache);

        // this is first call so instance should be from executor
        // and it should be cached after calling executor
        assertEquals(true, stage1Optional.isPresent());
        assertEquals(stage1Data, stage1Optional.get());
        assertEquals(true, cache.containsKey(key));
        assertEquals(stage1Data, cache.get(key));

        //rebind executor with other data
        IpDetailsReplyDump stage2LoadedDump = new IpDetailsReplyDump();
        when(executor.executeDump(identifier, NO_PARAMS)).thenReturn(stage2LoadedDump);

        final Optional<IpDetailsReplyDump> stage2Optional = managerPositive.getDump(identifier, cache);

        assertEquals(true, stage2Optional.isPresent());
        assertEquals(stage2LoadedDump, stage2Optional.get());

        //rebind executor with other data
        IpDetailsReplyDump stage3LoadedDump = new IpDetailsReplyDump();
        when(executor.executeDump(identifier, NO_PARAMS)).thenReturn(stage3LoadedDump);

        final Optional<IpDetailsReplyDump> stage3Optional = managerPositive.getDump(identifier, cache);
        assertEquals(true, stage3Optional.isPresent());
        //check if it returns instance cached from previous stage
        assertEquals(stage2LoadedDump, stage3Optional.get());
    }

    @Test
    public void testPostprocessing() throws ReadFailedException {
        IpDetailsReplyDump dump = new IpDetailsReplyDump();
        IpDetails details = new IpDetails();
        details.swIfIndex = 2;
        dump.ipDetails.add(details);

        when(executor.executeDump(identifier, null)).thenReturn(dump);

        Optional<IpDetailsReplyDump> optionalDump =
                managerPositiveWithPostProcessing.getDump(identifier, cache);

        assertEquals(true, optionalDump.isPresent());
        assertEquals(1, optionalDump.get().ipDetails.size());
        assertEquals(7, optionalDump.get().ipDetails.get(0).swIfIndex);
    }

    @Test
    public void testSameKeyDifferentTypes() throws ReadFailedException {
        final DumpCacheManager<String, Void> stringManager =
            new DumpCacheManager.DumpCacheManagerBuilder<String, Void>()
                .withExecutor((InstanceIdentifier, Void) -> "value")
                .acceptOnly(String.class)
                .build();

        final DumpCacheManager<Integer, Void> intManager = new DumpCacheManager.DumpCacheManagerBuilder<Integer, Void>()
            .acceptOnly(Integer.class)
            .withExecutor((InstanceIdentifier, Void) -> 3).build();

        final Optional<String> stringDump = stringManager.getDump(identifier, cache);
        final Optional<Integer> integerDump = intManager.getDump(identifier, cache);

        assertTrue(stringDump.isPresent());
        assertTrue(integerDump.isPresent());
        assertEquals("value", stringDump.get());
        assertEquals(3, integerDump.get().intValue());

    }

    @Test
    public void testCachingWithDifferentParams() throws ReadFailedException {
        final DumpCacheManager<Integer, Integer> manager =
            new DumpCacheManager.DumpCacheManagerBuilder<Integer, Integer>()
                .withExecutor((iid, param) -> param)
                .acceptOnly(Integer.class)
                .build();

        final Optional<Integer> dump1 = manager.getDump(identifier, cache, 1);
        final Optional<Integer> dump2 = manager.getDump(identifier, cache, 2);

        assertEquals(1, dump1.get().intValue());
        assertEquals(2, dump2.get().intValue());
    }

    private EntityDumpPostProcessingFunction<IpDetailsReplyDump> createPostProcessor() {
        return ipDetailsReplyDump -> {
            IpDetailsReplyDump modified = new IpDetailsReplyDump();

            for (IpDetails detail : ipDetailsReplyDump.ipDetails) {
                IpDetails modifiedDetail = new IpDetails();
                modifiedDetail.swIfIndex = detail.swIfIndex + 5;

                modified.ipDetails.add(modifiedDetail);
            }

            return modified;
        };
    }

    private interface DataObj extends DataObject {
    }

    private static final class IpDetailsReplyDump {
        List<IpDetails> ipDetails = new ArrayList<>();

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final IpDetailsReplyDump that = (IpDetailsReplyDump) o;
            return Objects.equals(ipDetails, that.ipDetails);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ipDetails);
        }
    }

    private static final class IpDetails {
        int swIfIndex;
    }
}