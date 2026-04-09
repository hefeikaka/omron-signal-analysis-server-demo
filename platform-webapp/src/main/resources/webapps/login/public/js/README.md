## 主页

本工程为React框架下 **页面（网页应用）** 基础工程。

http://100.100.16.132/basic-platform/react/omron-react-project

## feature-ojt 分支

- #### 介绍

该分支为针对公共模块开发所编写的基础工程。

工程内包含一个根据UI交互流程编写的完整的 UI 示例，实现了单一数据表的增删改查。

入口文件为`App.jsx`，为了调试时页面整体外观，加入了`BaseLayout.jsx`组件，此部分无需修改。

模块内跳转逻辑由`OjtNameRouter.jsx`内路由实现，在其中扩展出了

1. 查询页面`OjtNameTable.jsx`
2. 新增 & 修改页面`OjtNameForm.jsx`
3. 详情页面`OjtNameDetail.jsx`（施工中）

作为第一阶段公共组件开发时，主要使用上述的 1 & 2 两个页面。

- #### 安装

`npm install`

使用私有源http://100.100.16.132:7070/repository/npm-public/进行依赖安装。

如出现问题，请联系react支援小组。

- #### 快速开始

`npm start`

可以使用webpack-dev-server快速进行本地调试。

通过更改 ./config/webpack.dev.js中的代理设置可以进行与restapi的联调。

```javascript
proxy: {
	"/api": {
//代理所监视的api地址的关键词，此时如果目标api地址中首段为“/api"则进行代理转发
		target: "http://jsonplaceholder.typicode.com/",
//转发目标"协议+IP+port"
		changeOrigin: true,
//代理时会保留主机头的来源，如果将changeOrigin设置为true则覆盖此行为。在使用基于名称的虚拟托管站点时有用
		secure: false,
//如果代理的目标服务服务器https证书无效，则设置为false
		pathRewrite: { "^/api": "" }
//api地址替换规则，此时会将地址中的“/api”用“”替换
	}
},
```



- ### TodoList

1. 文件、变量的重命名

   重命名项目中所有带 OjtName / ojt 关键字的**文件**和**变量**

2. 完善 UI 交互流程

   完善 notification 中关于具体消息的动态提示。

   完善 Modal 组件中关于删除确认的动态提示。

   ……

3. 完善 restapi 接口的调用

   根据页面逻辑添加对增、删、改、查restapi调用。

   根据 UI 组件对数据的要求，完成对 restapi 返回数据的检查（注意异常情况处理），如有必要则进行转换。
   
   ……

- ### 不足之处

1. 删除成功 notification 显示逻辑不对。

   当前为调用删除restapi后直接显示，应该在删除restapi返回成功结果后再显示。

2. 查询restapi调用问题。

   首次加载componentDidMount调用时，条件为空。

   填入搜索条件后，条件为输入字符串。

   清空搜索条件后，条件为空字符串“”。

3. 编程规范

   单引号，双引号使用的规范。

- ### FAQ

- ### Change Log（更新日志）