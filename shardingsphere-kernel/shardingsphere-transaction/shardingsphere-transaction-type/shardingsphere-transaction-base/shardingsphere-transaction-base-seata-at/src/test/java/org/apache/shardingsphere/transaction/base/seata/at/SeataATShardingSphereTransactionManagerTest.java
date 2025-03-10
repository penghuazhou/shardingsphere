/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.transaction.base.seata.at;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.shardingsphere.infra.database.type.DatabaseTypeRegistry;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutorDataMap;
import org.apache.shardingsphere.transaction.base.seata.at.fixture.MockSeataServer;
import org.apache.shardingsphere.transaction.config.TransactionRuleConfiguration;
import org.apache.shardingsphere.transaction.core.ResourceDataSource;
import org.apache.shardingsphere.transaction.core.TransactionType;
import org.apache.shardingsphere.transaction.rule.TransactionRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.seata.core.context.RootContext;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.rpc.netty.RmNettyRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.SneakyThrows;

@RunWith(MockitoJUnitRunner.class)
public final class SeataATShardingSphereTransactionManagerTest {
    
    private static final MockSeataServer MOCK_SEATA_SERVER = new MockSeataServer();
    
    private final DataSource dataSource = getDataSource();
    
    private final SeataATShardingSphereTransactionManager seataTransactionManager = new SeataATShardingSphereTransactionManager();
    
    private final Queue<Object> requestQueue = MOCK_SEATA_SERVER.getMessageHandler().getRequestQueue();
    
    private final Queue<Object> responseQueue = MOCK_SEATA_SERVER.getMessageHandler().getResponseQueue();
    
    @BeforeClass
    public static void before() {
        Executors.newSingleThreadExecutor().submit(MOCK_SEATA_SERVER::start);
        while (true) {
            if (MOCK_SEATA_SERVER.getInitialized().get()) {
                return;
            }
        }
    }

    @AfterClass
    public static void after() {
        MOCK_SEATA_SERVER.shutdown();
    }
    
    @Before
    public void setUp() {
        seataTransactionManager.init(DatabaseTypeRegistry.getActualDatabaseType("MySQL"), getResourceDataSources(), new TransactionRule(new TransactionRuleConfiguration("BASE", "Seata")));
    }
    
    @After
    public void tearDown() {
        ExecutorDataMap.getValue().clear();
        RootContext.unbind();
        SeataTransactionHolder.clear();
        seataTransactionManager.close();
        releaseRpcClient();
        requestQueue.clear();
        responseQueue.clear();
    }
    
    private DataSource getDataSource() {
        BasicDataSource result = new BasicDataSource();
        result.setUrl("jdbc:h2:mem:demo_ds;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;MODE=MYSQL");
        result.setUsername("sa");
        result.setPassword("");
        return result;
    }
    
    private Collection<ResourceDataSource> getResourceDataSources() {
        return Collections.singletonList(new ResourceDataSource("demo_ds", dataSource));
    }
    
    @Test
    public void assertInit() {
        Map<String, DataSource> actual = getDataSourceMap();
        assertThat(actual.size(), is(1));
        assertThat(actual.get("demo_ds"), instanceOf(DataSourceProxy.class));
        assertThat(seataTransactionManager.getTransactionType(), is(TransactionType.BASE));
    }
    
    @Test
    public void assertGetConnection() throws SQLException {
        Connection actual = seataTransactionManager.getConnection("demo_ds");
        assertThat(actual, instanceOf(ConnectionProxy.class));
    }
    
    @Test
    public void assertBegin() {
        seataTransactionManager.begin();
        assertTrue(seataTransactionManager.isInTransaction());
        assertResult();
    }
    
    @Test
    public void assertCommit() {
        SeataTransactionHolder.set(GlobalTransactionContext.getCurrentOrCreate());
        setXID("testXID");
        seataTransactionManager.commit();
        assertResult();
    }
    
    @Test(expected = IllegalStateException.class)
    public void assertCommitWithoutBegin() {
        SeataTransactionHolder.set(GlobalTransactionContext.getCurrentOrCreate());
        seataTransactionManager.commit();
    }
    
    @Test
    public void assertRollback() {
        SeataTransactionHolder.set(GlobalTransactionContext.getCurrentOrCreate());
        setXID("testXID");
        seataTransactionManager.rollback();
        assertResult();
    }
    
    @Test(expected = IllegalStateException.class)
    public void assertRollbackWithoutBegin() {
        SeataTransactionHolder.set(GlobalTransactionContext.getCurrentOrCreate());
        seataTransactionManager.rollback();
    }
    
    private void assertResult() {
        int requestQueueSize = requestQueue.size();
        if (requestQueueSize == 3) {
            assertThat(requestQueue.poll(), instanceOf(RegisterRMRequest.class));
            if (requestQueueSize == 4) {
                assertThat(requestQueue.poll(), instanceOf(RegisterRMRequest.class));
            }
            assertThat(requestQueue.poll(), instanceOf(RegisterTMRequest.class));
            assertThat(requestQueue.poll(), instanceOf(MergedWarpMessage.class));
        }
        int responseQueueSize = responseQueue.size();
        if (responseQueueSize == 3) {
            assertThat(responseQueue.poll(), instanceOf(RegisterRMResponse.class));
            if (responseQueueSize == 4) {
                assertThat(responseQueue.poll(), instanceOf(RegisterRMResponse.class));
            }
            assertThat(responseQueue.poll(), instanceOf(RegisterTMResponse.class));
            assertThat(responseQueue.poll(), instanceOf(MergeResultMessage.class));
        }
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    @SuppressWarnings("unchecked")
    private Map<String, DataSource> getDataSourceMap() {
        Field field = seataTransactionManager.getClass().getDeclaredField("dataSourceMap");
        field.setAccessible(true);
        return (Map<String, DataSource>) field.get(seataTransactionManager);
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    private void setXID(final String xid) {
        Field field = SeataTransactionHolder.get().getClass().getDeclaredField("xid");
        field.setAccessible(true);
        field.set(SeataTransactionHolder.get(), xid);
        RootContext.bind(xid);
    }
    
    @SneakyThrows(ReflectiveOperationException.class)
    private void releaseRpcClient() {
        Field field = TmNettyRemotingClient.getInstance().getClass().getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(TmNettyRemotingClient.getInstance(), new AtomicBoolean(false));
        field = TmNettyRemotingClient.getInstance().getClass().getDeclaredField("instance");
        field.setAccessible(true);
        field.set(TmNettyRemotingClient.getInstance(), null);
        field = RmNettyRemotingClient.getInstance().getClass().getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(RmNettyRemotingClient.getInstance(), new AtomicBoolean(false));
        field = RmNettyRemotingClient.getInstance().getClass().getDeclaredField("instance");
        field.setAccessible(true);
        field.set(RmNettyRemotingClient.getInstance(), null);
    }
}
