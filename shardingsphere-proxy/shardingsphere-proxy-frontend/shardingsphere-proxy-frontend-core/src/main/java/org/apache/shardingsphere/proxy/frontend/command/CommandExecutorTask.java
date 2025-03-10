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

package org.apache.shardingsphere.proxy.frontend.command;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.db.protocol.CommonConstants;
import org.apache.shardingsphere.db.protocol.packet.CommandPacket;
import org.apache.shardingsphere.db.protocol.packet.CommandPacketType;
import org.apache.shardingsphere.db.protocol.packet.DatabasePacket;
import org.apache.shardingsphere.db.protocol.payload.PacketPayload;
import org.apache.shardingsphere.proxy.backend.communication.SQLStatementSchemaHolder;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.ConnectionStatus;
import org.apache.shardingsphere.proxy.frontend.command.executor.CommandExecutor;
import org.apache.shardingsphere.proxy.frontend.command.executor.QueryCommandExecutor;
import org.apache.shardingsphere.proxy.frontend.exception.ExpectedExceptions;
import org.apache.shardingsphere.proxy.frontend.spi.DatabaseProtocolFrontendEngine;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Command executor task.
 */
@RequiredArgsConstructor
@Slf4j
public final class CommandExecutorTask implements Runnable {
    
    private final DatabaseProtocolFrontendEngine databaseProtocolFrontendEngine;
    
    private final BackendConnection backendConnection;
    
    private final ChannelHandlerContext context;
    
    private final Object message;
    
    /**
     * To make sure SkyWalking will be available at the next release of ShardingSphere,
     * a new plugin should be provided to SkyWalking project if this API changed.
     *
     * @see <a href="https://github.com/apache/skywalking/blob/master/docs/en/guides/Java-Plugin-Development-Guide.md#user-content-plugin-development-guide">Plugin Development Guide</a>
     */
    @Override
    public void run() {
        boolean isNeedFlush = false;
        try (PacketPayload payload = databaseProtocolFrontendEngine.getCodecEngine().createPacketPayload((ByteBuf) message, context.channel().attr(CommonConstants.CHARSET_ATTRIBUTE_KEY).get())) {
            ConnectionStatus connectionStatus = backendConnection.getConnectionStatus();
            if (!backendConnection.getTransactionStatus().isInConnectionHeldTransaction()) {
                connectionStatus.waitUntilConnectionRelease();
                connectionStatus.switchToUsing();
            }
            isNeedFlush = executeCommand(context, payload, backendConnection);
            // CHECKSTYLE:OFF
        } catch (final Exception ex) {
            // CHECKSTYLE:ON
            processException(ex);
        } finally {
            // TODO optimize SQLStatementSchemaHolder
            SQLStatementSchemaHolder.remove();
            Collection<SQLException> exceptions = closeExecutionResources();
            if (!backendConnection.getTransactionStatus().isInConnectionHeldTransaction()) {
                exceptions.addAll(backendConnection.closeDatabaseCommunicationEngines(true));
                exceptions.addAll(backendConnection.closeConnections(false));
                backendConnection.getConnectionStatus().switchToReleased();
            }
            if (isNeedFlush) {
                context.flush();
            }
            processClosedExceptions(exceptions);
        }
    }
    
    private boolean executeCommand(final ChannelHandlerContext context, final PacketPayload payload, final BackendConnection backendConnection) throws SQLException {
        CommandExecuteEngine commandExecuteEngine = databaseProtocolFrontendEngine.getCommandExecuteEngine();
        CommandPacketType type = commandExecuteEngine.getCommandPacketType(payload);
        CommandPacket commandPacket = commandExecuteEngine.getCommandPacket(payload, type, backendConnection);
        CommandExecutor commandExecutor = commandExecuteEngine.getCommandExecutor(type, commandPacket, backendConnection);
        try {
            Collection<DatabasePacket<?>> responsePackets = commandExecutor.execute();
            if (responsePackets.isEmpty()) {
                return false;
            }
            responsePackets.forEach(context::write);
            if (commandExecutor instanceof QueryCommandExecutor) {
                return commandExecuteEngine.writeQueryData(context, backendConnection, (QueryCommandExecutor) commandExecutor, responsePackets.size());
            }
        } finally {
            commandExecutor.close();
        }
        return databaseProtocolFrontendEngine.getFrontendContext().isFlushForPerCommandPacket();
    }
    
    private void processException(final Exception cause) {
        if (!ExpectedExceptions.isExpected(cause.getClass())) {
            log.error("Exception occur: ", cause);
        }
        context.write(databaseProtocolFrontendEngine.getCommandExecuteEngine().getErrorPacket(cause, backendConnection));
        Optional<DatabasePacket<?>> databasePacket = databaseProtocolFrontendEngine.getCommandExecuteEngine().getOtherPacket(backendConnection);
        databasePacket.ifPresent(context::write);
        context.flush();
    }
    
    private Collection<SQLException> closeExecutionResources() {
        Collection<SQLException> result = new LinkedList<>();
        result.addAll(backendConnection.closeDatabaseCommunicationEngines(false));
        result.addAll(backendConnection.closeFederationExecutor());
        return result;
    }
    
    private void processClosedExceptions(final Collection<SQLException> exceptions) {
        if (exceptions.isEmpty()) {
            return;
        }
        SQLException ex = new SQLException("");
        for (SQLException each : exceptions) {
            ex.setNextException(each);
        }
        processException(ex);
    }
}
