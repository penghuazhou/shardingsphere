+++
title = "读写分离"
weight = 2
+++

## 配置入口

类名称：ReadwriteSplittingRuleConfiguration

可配置属性：

| *名称*             | *数据类型*                                                   | *说明*            |
| ----------------- | ----------------------------------------------------------- | ----------------- |
| dataSources (+)   | Collection\<ReadwriteSplittingDataSourceRuleConfiguration\> | 读写数据源配置      |
| loadBalancers (*) | Map\<String, ShardingSphereAlgorithmConfiguration\>         | 从库负载均衡算法配置 |

## 主从数据源配置

类名称：ReadwriteSplittingDataSourceRuleConfiguration

可配置属性：

| *名称*                     | *数据类型*             | *说明*              | *默认值*       |
| -------------------------- | -------------------- | ------------------- | ------------- |
| name                       | String               | 读写分离数据源名称    | -             |
| writeDataSourceName        | String               | 写库数据源名称        | -             |
| readDataSourceNames (+)    | Collection\<String\> | 读库数据源名称列表    | -             |
| loadBalancerName (?)       | String               | 读库负载均衡算法名称  | 轮询负载均衡算法 |

算法类型的详情，请参见[内置负载均衡算法列表](/cn/user-manual/shardingsphere-jdbc/builtin-algorithm/load-balance)。
查询一致性路由的详情，请参见[使用规范](/cn/features/readwrite-splitting/use-norms)。
