# 快速部署（一页纸）

这是最短版。

适合：

- 交给运维
- 交给测试
- 交给只想启动系统的人

---

## 1. 先装环境

必须安装：

1. `JDK 8`
2. `MySQL`
3. `MongoDB`
4. `Windows`

如果要重新打包源码，再安装：

5. `Maven 3.9+`

如果要接真实采集卡，还需要：

6. 采集卡驱动和 `VK70xNMC_DAQ2.dll`

---

## 2. 默认地址

- 页面：`http://127.0.0.1:9730/edge/`
- 健康检查：`http://127.0.0.1:9730/healthz`

---

## 3. 默认数据库

### MySQL

- 地址：`127.0.0.1:3306`
- 库：`DB_SingalAys`
- 用户：`root`
- 密码：`1234`

### MongoDB

- 默认库：`DB_SingalAys`

---

## 4. 运行步骤

进入运行目录：

`omron-signal-analysis-server-open-runtime`

### 第一步：启动数据库

先启动：

1. `MySQL`
2. `MongoDB`

---

### 第二步：初始化

如果是第一次运行：

```powershell
.\bin\init-mongo.ps1
.\bin\init-es.ps1
```

---

### 第三步：启动服务

```powershell
.\bin\start-demo.ps1
```

查看状态：

```powershell
.\bin\status-demo.ps1
```

停止服务：

```powershell
.\bin\stop-demo.ps1
```

---

## 5. 成功标准

浏览器打开：

- `http://127.0.0.1:9730/healthz`

如果返回：

```json
{"status":"UP"}
```

说明服务已启动。

然后打开：

- `http://127.0.0.1:9730/edge/`

---

## 6. 出问题先查

### 服务起不来

先看：

- `log`

### 页面打不开

先看：

- `9730` 端口是否起来
- `http://127.0.0.1:9730/healthz`

### 没有实时数据

先查：

1. 采集卡是否连接
2. DLL 是否安装
3. RC2 是否还在占端口
4. MySQL / Mongo 是否正常

硬件排查脚本：

```powershell
.\bin\diagnose-hardware.ps1
```

---

## 7. 如果要从源码重新打包

在源码根目录执行：

```powershell
mvn package
```

打包后运行目录在：

`platform-runtime/target/omron-signal-analysis-server-open-runtime`
