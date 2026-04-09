# 部署文档（傻瓜式）

这份文档是给“接手项目的人”用的。

目标是：

- 不看源码，也能把系统启动起来
- 按步骤准备环境
- 出问题知道先看哪里

---

## 1. 你会拿到什么

建议交付这两个目录：

1. 源码工程目录  
   `omron-signal-analysis-server-open`

2. 已打包运行目录  
   `platform-runtime/target/omron-signal-analysis-server-open-runtime`

如果对方只是运行系统，不改代码，那么只需要第二个目录。

---

## 2. 别人电脑需要安装什么

### 必装

1. `JDK 8`
2. `MySQL 8.x`
3. `MongoDB`
4. `Windows`

### 按需安装

1. `Maven 3.9+`
   只有在“需要重新打包源码”时才需要
2. `Elasticsearch`
   当前主要页面功能不是强依赖，但如果要完整兼容索引初始化，建议安装
3. 采集卡驱动和 DLL
   如果要直连真实采集卡，必须安装

---

## 3. 当前默认配置

### Web

- 访问地址：`http://127.0.0.1:9730/edge/`
- 健康检查：`http://127.0.0.1:9730/healthz`

### MySQL

- 地址：`127.0.0.1:3306`
- 数据库：`DB_SingalAys`
- 用户：`root`
- 密码：`1234`

配置文件：

- `etc/mysql-feature-store.properties`

### MongoDB

- 默认数据库：`DB_SingalAys`

配置文件：

- `etc/com.omron.gc.cm.mongodb-signal.cfg`

### 直采

- 采集端口：`8234`
- 设备号：`M001-1`
- 默认采集频率显示：`5000`
- 默认采集时长：`1000 ms`
- 默认采集间隔：`10000 ms`

配置文件：

- `etc/direct-acquisition.properties`

---

## 4. 如果对方只想运行，不想编译

使用运行目录：

- `omron-signal-analysis-server-open-runtime`

### 第一步：启动数据库

先确保：

1. MySQL 已启动
2. MongoDB 已启动

如果需要初始化数据层，可以运行：

```powershell
.\bin\init-mongo.ps1
.\bin\init-es.ps1
```

说明：

- `init-mongo.ps1` 用于初始化 Mongo 集合
- `init-es.ps1` 用于初始化 ES 映射

---

### 第二步：检查配置

进入运行目录后，重点看 `etc` 文件夹。

最常改的是：

1. `etc/mysql-feature-store.properties`
   改 MySQL 地址、账号、密码
2. `etc/com.omron.gc.cm.mongodb-signal.cfg`
   改 Mongo 地址和库名
3. `etc/direct-acquisition.properties`
   改采集参数

---

### 第三步：启动系统

在运行目录下执行：

```powershell
.\bin\start-demo.ps1
```

查看状态：

```powershell
.\bin\status-demo.ps1
```

停止系统：

```powershell
.\bin\stop-demo.ps1
```

---

### 第四步：打开页面

浏览器访问：

- `http://127.0.0.1:9730/edge/`

健康检查：

- `http://127.0.0.1:9730/healthz`

如果健康检查返回：

```json
{"status":"UP"}
```

说明服务已经启动成功。

---

## 5. 如果对方要从源码重新打包

需要先安装：

1. `JDK 8`
2. `Maven 3.9+`

在源码根目录执行：

```powershell
mvn package
```

打包成功后，运行目录会出现在：

`platform-runtime/target/omron-signal-analysis-server-open-runtime`

然后再按“只运行”的步骤启动即可。

---

## 6. 运行目录里每个文件夹干什么

### `bin`

启动、停止、初始化、测试脚本。

### `etc`

配置文件。

### `lib`

运行需要的 jar 包。

### `webapps`

前端页面资源。

### `data`

初始化数据和运行辅助数据。

### `log`

日志目录。

### `storage`

本地存储目录。

### `deploy`

部署资源目录。

### `system`

兼容 RC2 目录形态保留的系统级目录。

---

## 7. 出问题先看哪里

### 1. 服务起不来

先看：

- `log`
- `bin\start-open.ps1`

### 2. 页面打不开

先检查：

- `http://127.0.0.1:9730/healthz`
- `9730` 端口是否被占用

### 3. 页面打开但没数据

先检查：

1. 采集卡是否连接
2. 是否已经安装采集卡驱动和 DLL
3. 是否误开了 RC2，导致端口被占
4. MySQL 和 Mongo 是否正常

### 4. 硬件问题

可以运行：

```powershell
.\bin\diagnose-hardware.ps1
```

---

## 8. 建议交付方式

最稳的方式是一起交付：

1. 源码目录
2. 运行目录
3. 这份部署文档

这样：

- 开发人员可以改代码
- 运维人员可以直接启动
- 测试人员可以按文档操作

---

## 9. 最短启动流程

如果只记最短版，就记这 5 步：

1. 安装 `JDK 8`
2. 启动 `MySQL`
3. 启动 `MongoDB`
4. 进入运行目录执行 `.\bin\start-open.ps1`
5. 打开 `http://127.0.0.1:9730/edge/`
