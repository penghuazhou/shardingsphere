<#--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
spring.shardingsphere.datasource.ds-2.jdbc-url=jdbc:mysql://localhost:3306/demo_ds_2?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=UTF-8
spring.shardingsphere.datasource.ds-2.type=com.zaxxer.hikari.HikariDataSource
spring.shardingsphere.datasource.ds-2.driver-class-name=com.mysql.jdbc.Driver
spring.shardingsphere.datasource.ds-2.username=root
spring.shardingsphere.datasource.ds-2.password=root

spring.shardingsphere.rules.readwrite-splitting.load-balancers.round_robin.type=ROUND_ROBIN
spring.shardingsphere.rules.readwrite-splitting.data-sources.pr_ds.write-data-source-name=ds-0
spring.shardingsphere.rules.readwrite-splitting.data-sources.pr_ds.read-data-source-names=ds-1,ds-2
spring.shardingsphere.rules.readwrite-splitting.data-sources.pr_ds.load-balancer-name=round_robin