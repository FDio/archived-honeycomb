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

package io.fd.honeycomb.translate.impl.read;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import io.fd.honeycomb.translate.util.read.ReflexiveListReaderCustomizer;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite implementation of {@link ListReader} able to place the read result into parent builder object intended
 * for list node type.
 * <p/>
 * This reader checks if the IDs are wildcarded in which case it performs read of all list entries. In case the ID has a
 * key, it reads only the specified value.
 */
@Beta
@ThreadSafe
public class GenericListReader<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
        extends GenericReader<C, B> implements ListReader<C, K, B> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericListReader.class);

    /**
     * Create new {@link GenericListReader}
     *
     * @param managedDataObjectType Class object for managed data type. Must come from a list node type.
     * @param customizer            Customizer instance to customize this generic reader
     */
    public GenericListReader(@Nonnull final InstanceIdentifier<C> managedDataObjectType,
                             @Nonnull final ListReaderCustomizer<C, K, B> customizer) {
        super(managedDataObjectType, customizer);
    }

    @Override
    @Nonnull
    public List<C> readList(@Nonnull final InstanceIdentifier<C> id,
                            @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.trace("{}: Reading all list entries", this);
        final List<K> allIds = getAllIds(id, ctx);
        LOG.debug("{}: Reading list entries for: {}", this, allIds);

        final ArrayList<C> allEntries = new ArrayList<>(allIds.size());
        for (K key : allIds) {
            final InstanceIdentifier.IdentifiableItem<C, K> currentBdItem = RWUtils.getCurrentIdItem(id, key);
            final InstanceIdentifier<C> keyedId = RWUtils.replaceLastInId(id, currentBdItem);
            final Optional<C> read = readCurrent(keyedId, ctx);
            if (read.isPresent()) {
                final DataObject singleItem = read.get();
                checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(singleItem.getClass()));
                allEntries.add(getManagedDataObjectType().getTargetType().cast(singleItem));
            }
        }
        return allEntries;
    }

    @Override
    public List<K> getAllIds(@Nonnull final InstanceIdentifier<C> id, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.trace("{}: Getting all list ids", this);
        final List<K> allIds = ((ListReaderCustomizer<C, K, B>) customizer).getAllIds(id, ctx);
        LOG.debug("{}: All list ids: {}", this, allIds);
        return allIds;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<C> readData) {
        ((ListReaderCustomizer<C, K, B>) customizer).merge(builder, readData);
    }

    public static <C extends DataObject & Identifiable<K>,K extends Identifier<C>, B extends Builder<C>> Reader<C, B> createReflexive(
            final InstanceIdentifier<C> id, Class<B> builderClass,
            final List<K> staticKeys) {
        return new GenericListReader<>(id, new ReflexiveListReaderCustomizer<>(id.getTargetType(), builderClass, staticKeys));
    }
}
