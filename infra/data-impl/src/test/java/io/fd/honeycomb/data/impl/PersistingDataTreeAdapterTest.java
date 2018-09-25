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

package io.fd.honeycomb.data.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class PersistingDataTreeAdapterTest {

    @Mock
    private DataTree delegatingDataTree;
    @Mock
    private DOMSchemaService schemaService;
    @Mock
    private DataTreeSnapshot snapshot;
    @Mock
    private PersistingDataTreeAdapter.JsonPersister persister;

    private Path tmpPersistFile;

    private PersistingDataTreeAdapter persistingDataTreeAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(snapshot).when(delegatingDataTree).takeSnapshot();
        doNothing().when(persister).persistCurrentData(any(Optional.class));
        tmpPersistFile = Files.createTempFile("testing-hc-persistence", "json");
        persistingDataTreeAdapter = new PersistingDataTreeAdapter(delegatingDataTree, schemaService, tmpPersistFile);
    }

    @Test
    public void testNoPersistOnFailure() throws Exception {
        doThrow(new IllegalStateException("testing errors")).when(delegatingDataTree).commit(any(DataTreeCandidate.class));

        try {
            persistingDataTreeAdapter.commit(mock(DataTreeCandidate.class));
            fail("Exception expected");
        } catch (IllegalStateException e) {
            verify(delegatingDataTree, times(0)).takeSnapshot();
            verify(delegatingDataTree).commit(any(DataTreeCandidate.class));
        }
    }

    @Test
    public void testPersist() throws Exception {
        persistingDataTreeAdapter = new PersistingDataTreeAdapter(delegatingDataTree, persister);
        final DataTreeSnapshot snapshot = mock(DataTreeSnapshot.class);
        when(snapshot.readNode(any())).thenReturn(Optional.empty());
        when(delegatingDataTree.takeSnapshot()).thenReturn(snapshot);
        persistingDataTreeAdapter.commit(mock(DataTreeCandidate.class));
        verify(delegatingDataTree).takeSnapshot();
        verify(persister).persistCurrentData(any(Optional.class));
    }

    @Test
    public void testTakeSnapshot() throws Exception {
        persistingDataTreeAdapter.takeSnapshot();
        verify(delegatingDataTree).takeSnapshot();
    }

    @Test
    public void testSetSchema() throws Exception {
        persistingDataTreeAdapter.setSchemaContext(null);
        verify(delegatingDataTree).setSchemaContext(null);
    }

    @Test
    public void testValidate() throws Exception {
        persistingDataTreeAdapter.validate(null);
        verify(delegatingDataTree).validate(null);
    }

    @Test
    public void testPrepare() throws Exception {
        persistingDataTreeAdapter.prepare(null);
        verify(delegatingDataTree).prepare(null);
    }

    @Test
    public void testGetRootPath() throws Exception {
        persistingDataTreeAdapter.getRootPath();
        verify(delegatingDataTree).getRootPath();
    }

    @Test(expected = IllegalStateException.class)
    public void testPersistFailure() throws Exception {
        doThrow(IOException.class).when(schemaService).getGlobalContext();
        final PersistingDataTreeAdapter.JsonPersister jsonPersister =
                new PersistingDataTreeAdapter.JsonPersister(tmpPersistFile, schemaService);
        // Nothing
        jsonPersister.persistCurrentData(Optional.empty());
        // Exception
        jsonPersister.persistCurrentData(Optional.of(ImmutableNodes.leafNode(QName.create("namespace", "leaf"), "value")));
    }

    @Test
    public void testPersisterCreateFile() throws Exception {
        // Delete to test file creation
        Files.delete(tmpPersistFile);
        new PersistingDataTreeAdapter.JsonPersister(tmpPersistFile, schemaService);
        assertTrue(Files.exists(tmpPersistFile));
   }
}