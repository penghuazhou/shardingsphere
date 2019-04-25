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

package org.apache.shardingsphere.core.parse.antlr.extractor.impl.common.expression;

import com.google.common.base.Optional;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.shardingsphere.core.parse.antlr.extractor.api.OptionalSQLSegmentExtractor;
import org.apache.shardingsphere.core.parse.antlr.extractor.impl.dml.select.SubqueryExtractor;
import org.apache.shardingsphere.core.parse.antlr.extractor.util.ExtractorUtils;
import org.apache.shardingsphere.core.parse.antlr.extractor.util.RuleName;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.CommonExpressionSegment;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.LiteralExpressionSegment;
import org.apache.shardingsphere.core.parse.antlr.sql.segment.dml.expr.ParameterMarkerExpressionSegment;

import java.util.Map;

/**
 * Expression extractor.
 *
 * @author duhongjun
 */
public final class ExpressionExtractor implements OptionalSQLSegmentExtractor {
    
    private final ParameterMarkerExpressionExtractor parameterMarkerExpressionExtractor = new ParameterMarkerExpressionExtractor();
    
    private final LiteralExpressionExtractor literalExpressionExtractor = new LiteralExpressionExtractor();
    
    @Override
    public Optional<? extends ExpressionSegment> extract(final ParserRuleContext expressionNode, final Map<ParserRuleContext, Integer> parameterMarkerIndexes) {
        Optional<ParserRuleContext> subqueryNode = ExtractorUtils.findFirstChildNode(expressionNode, RuleName.SUBQUERY);
        if (subqueryNode.isPresent()) {
            return new SubqueryExtractor().extract(subqueryNode.get(), parameterMarkerIndexes);
        }
        Optional<ParameterMarkerExpressionSegment> parameterMarkerExpressionSegment = parameterMarkerExpressionExtractor.extract(expressionNode, parameterMarkerIndexes);
        if (parameterMarkerExpressionSegment.isPresent()) {
            return parameterMarkerExpressionSegment;
        }
        Optional<LiteralExpressionSegment> literalExpressionSegment = literalExpressionExtractor.extract(expressionNode, parameterMarkerIndexes);
        if (literalExpressionSegment.isPresent()) {
            return literalExpressionSegment;
        }
        // TODO extract column name and value from expression
        return Optional.of(new CommonExpressionSegment(expressionNode.getStart().getStartIndex(), expressionNode.getStop().getStopIndex()));
    }
}
