+++
pre = "<b>6.4. </b>"
title = "SQL Parser"
weight = 4
chapter = true
+++

## DatabaseTypedSQLParserFacade

| *SPI Name*                   | *Description*                          |
| ---------------------------- | -------------------------------------- |
| DatabaseTypedSQLParserFacade | SQL parser facade for lexer and parser |

| *Implementation Class* | *Description*                          |
| ---------------------- | -------------------------------------- |
| MySQLParserFacade      | SQL parser facade for MySQL            |
| PostgreSQLParserFacade | SQL parser facade for PostgreSQL       |
| SQLServerParserFacade  | SQL parser facade for SQLServer        |
| OracleParserFacade     | SQL parser facade for Oracle           |
| SQL92ParserFacade      | SQL parser facade for SQL92            |

## SQLVisitorFacade

| *SPI Name*                          | *Description*                                            |
| ----------------------------------- | -------------------------------------------------------- |
| SQLVisitorFacade                    | SQL AST visitor facade                                   |

| *Implementation Class*              | *Description*                                            |
| ----------------------------------- | -------------------------------------------------------- |
| MySQLStatementSQLVisitorFacade      | SQL visitor of statement extracted facade for MySQL      |
| PostgreSQLStatementSQLVisitorFacade | SQL visitor of statement extracted facade for PostgreSQL |
| SQLServerStatementSQLVisitorFacade  | SQL visitor of statement extracted facade for SQLServer  |
| OracleStatementSQLVisitorFacade     | SQL visitor of statement extracted facade for Oracle     |
| SQL92StatementSQLVisitorFacade      | SQL visitor of statement extracted facade for SQL92      |
