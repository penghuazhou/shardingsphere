+++
pre = "<b>6.4. </b>"
title = "SQL 解析"
weight = 4
chapter = true
+++

## DatabaseTypedSQLParserFacade

| *SPI 名称*                    | *详细说明*                            |
| ---------------------------- | ----------------------------------- |
| DatabaseTypedSQLParserFacade | 配置用于SQL解析的词法分析器和语法分析器入口 |

| *Implementation Class* | *Description*                         |
| ---------------------- | ------------------------------------- |
| MySQLParserFacade      | 基于 MySQL 的 SQL 解析器入口             |
| PostgreSQLParserFacade | 基于 PostgreSQL 的SQL 解析器入口         |
| SQLServerParserFacade  | 基于 SQLServer 的SQL 解析器入口          |
| OracleParserFacade     | 基于 Oracle 的SQL 解析器入口             |
| SQL92ParserFacade      | 基于 SQL92 的SQL 解析器入口              |

## SQLVisitorFacade

| *SPI 名称*                           | *详细说明*                                  |
| ----------------------------------- | ------------------------------------------ |
| SQLVisitorFacade                    | SQL 语法树访问器入口                          |

| *Implementation Class*              | *Description*                             |
| ----------------------------------- | ----------------------------------------- |
| MySQLStatementSQLVisitorFacade      | 基于 MySQL 的提取 SQL 语句的语法树访问器       |
| PostgreSQLStatementSQLVisitorFacade | 基于 PostgreSQL 的提取 SQL 语句的语法树访问器  |
| SQLServerStatementSQLVisitorFacade  | 基于 SQLServer 的提取 SQL 语句的语法树访问器   |
| OracleStatementSQLVisitorFacade     | 基于 Oracle 的提取 SQL 语句的语法树访问器      |
| SQL92StatementSQLVisitorFacade      | 基于 SQL92 的提取 SQL 语句的语法树访问器       |
