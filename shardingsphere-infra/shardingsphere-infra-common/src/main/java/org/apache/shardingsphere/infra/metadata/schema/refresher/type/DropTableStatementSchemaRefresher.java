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

package org.apache.shardingsphere.infra.metadata.schema.refresher.type;

import org.apache.shardingsphere.infra.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.schema.refresher.SchemaRefresher;
import org.apache.shardingsphere.infra.rule.identifier.type.MutableDataNodeRule;
import org.apache.shardingsphere.sql.parser.sql.common.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.common.statement.ddl.DropTableStatement;

import java.util.Collection;

/**
 * Schema refresher for drop table statement.
 */
public final class DropTableStatementSchemaRefresher implements SchemaRefresher<DropTableStatement> {
    
    @Override
    public void refresh(final ShardingSphereMetaData schemaMetaData, final Collection<String> logicDataSourceNames, final DropTableStatement sqlStatement, final ConfigurationProperties props) {
        sqlStatement.getTables().forEach(each -> schemaMetaData.getSchema().remove(each.getTableName().getIdentifier().getValue()));
        Collection<MutableDataNodeRule> rules = schemaMetaData.getRuleMetaData().findRules(MutableDataNodeRule.class);
        for (SimpleTableSegment each : sqlStatement.getTables()) {
            rules.forEach(rule -> rule.remove(each.getTableName().getIdentifier().getValue()));
        }
    }
}
