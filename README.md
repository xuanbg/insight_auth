# Insight Auth 服务使用手册

## 目录

- [Insight Auth 服务使用手册](#insight-auth-服务使用手册)
  - [目录](#目录)
  - [概述](#概述)
    - [主要功能](#主要功能)
    - [通讯方式](#通讯方式)
  - [Token接口](#token接口)
    - [1.获取Code](#1获取code)
    - [2.扫码授权](#2扫码授权)
    - [3.获取Token](#3获取token)
    - [4.刷新Token](#4刷新token)
    - [5.注销Token](#5注销token)
  - [应用权限接口](#应用权限接口)
    - [1.获取模块导航](#1获取模块导航)
    - [2.获取模块功能](#2获取模块功能)

## 概述

Auth 服务是一个依赖于用户数据的、基于Token的用户身份认证服务。每一个Token都必须绑定一个AppId，该Id需要在获取Token时作为参数传入。根据应用的相应设置，系统支持两种Token发放模式：

1. 通用模式，可对不同的设备发放多个相同AppId的的Token，用户可同时在不同的设备登录同一个应用；
2. 专用模式，对不同设备发放相同AppId的时候，之前发放的Token自动失效，用户只能在一个设备上登录同一个应用。

### 主要功能

1. Token的发放(账号/密码|手机号/验证码|微信授权)、验证、刷新、注销；
2. 获取应用的功能模块导航信息，以及指定模块的功能和授权信息。

### 通讯方式

服务支持 **HTTP/HTTPS** 协议的访问请求，使用 **URL Params** 、 **Path Variable** 或 **BODY** 三种方式传递参数。如使用 **BODY**传参，则需使用 **JSON** 格式的请求参数。接口 **URL** 区分大小写，请求参数以及返回结果都使用 **UTF-8**字符集进行编码。

调用接口时，需要的请求头参数设置如下：

| 参数名           | 参数值                              |
|---------------|----------------------------------|
| Accept        | 固定为：application/json             |
| Authorization | 用户登录获取的AccessToken(Base64编码的字符串) |
| Content-Type  | 固定为：application/json             |

接口返回的数据结构如下：

| 字段      |   类型    | 字段说明                      |
|---------|:-------:|---------------------------|
| success | Boolean | 接口调用是否成功，成功：true；失败：false |
| code    | Integer | 错误代码，2xx代表成功，4xx或5xx代表失败  |
| message | String  | 错误消息，描述了接口调用失败原因          |
| data    | Object  | 接口返回数据，对象或对象的集合           |
| option  | Object  | 附加数据，一般为分页数据的总数量          |

## Token接口

### 1.获取Code

获取Code接口是一个多用途接口，具体作用由type参数决定。接口的限流策略为360次/日。

1. 获取一个有效时间30秒的32位随机字符串用于生成用户签名，然后使用账号和该签名获取Token。Sign的算法为 **MD5(MD5(account + MD5(password)) + Code)**。
2. 获取一个有效时间30秒的32位随机字符串用于生成用户签名，同时将动态密码发送到指定手机号，然后使用手机号作为账号和该签名获取Token。Sign的算法为 **MD5(MD5(mobile + MD5(smsCode)) + Code)**。
3. 获取一个 **URL(含授权识别码)** 用于扫码授权。

请求方法：**POST**

接口URL：**/base/auth/v1.0/codes**

请求参数：**BODY**

| 字段      |   类型    | 必需  | 字段说明                                 |
|---------|:-------:|:---:|--------------------------------------|
| type    | Integer |  ○  | 类型: 0.密码登录, 1.验证码登录, 2.获取授权URL, 默认为0 |
| account | String  |  ○  | 登录账号/手机号/邮箱，获取授权URL时该字段为空            |

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": "404a257bc35a4540aed079dc4b48d957",
  "option": null
}
```

[回目录](#目录)

### 2.扫码授权

扫码授权接口的作用是通过一个有效的Token为指定的授权识别码进行授权，该授权识别码被授权后，即可通过授权识别码获取Token。接口的限流策略为360次/日。

请求方法：**PUT**

接口URL：**/base/auth/v1.0/codes/{code}**

请求参数：**Path Variable**

| 字段   |   类型   | 必需  | 字段说明  |
|------|:------:|:---:|-------|
| code | String |  ●  | 授权识别码 |

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": null,
  "option": null
}
```

[回目录](#目录)

### 3.获取Token

获取Token接口支持多种方式获取用户的访问令牌。限流策略为间隔1秒以上。

1. 使用account和signature获取Token。
2. 使用微信授权码获取Token。如用户未绑定微信unionID，则缓存微信用户信息，重新调用该接口传入手机号进行微信和用户的绑定。
3. 使用微信unionID获取Token。
4. 使用授权识别码获取Token(需先调用扫码授权接口对识别码进行授权)。
5. 使用有效的Token获取指定应用的Token。

请求方法：**POST**

接口URL：**/base/auth/v1.0/tokens**

请求参数：**BODY**

| 字段          |   类型    | 必需  | 字段说明                |
|-------------|:-------:|:---:|---------------------|
| appId       | String  |  ●  | 应用ID                |
| tenantId    | String  |  ○  | 租户ID                |
| account     | String  |  ○  | 登录账号/手机号/邮箱         |
| signature   | String  |  ○  | 签名:MD5(MD5(account\ |mobile + MD5(password\|smsCode)) + Code)|
| weChatAppId | String  |  ○  | 微信appId             |
| code        | String  |  ○  | 微信授权码/授权识别码         |
| unionId     | String  |  ○  | 微信用户唯一ID            |
| replace     | Boolean |  ○  | 是否替换用户的UnionId      |
| deviceId    | String  |  ○  | 用户设备ID              |
| deviceModel | String  |  ○  | 用户设备型号              |

接口返回数据类型：

| 字段           |    类型    | 字段说明       |
|--------------|:--------:|------------|
| accessToken  |  String  | 访问用令牌      |
| refreshToken |  String  | 刷新用令牌      |
| expire       | Integer  | 令牌过期时间(毫秒) |
| failure      | Integer  | 令牌失效时间(毫秒) |
| userInfo     | UserInfo | 用户信息       |

UserInfo 数据类型：

| 字段          |   类型    | 字段说明       |
|-------------|:-------:|------------|
| id          | String  | 用户ID       |
| tenantId    | String  | 用户当前登录租户ID |
| name        | String  | 用户姓名       |
| account     | String  | 用户登录账号     |
| mobile      | String  | 用户绑定手机号    |
| email       | String  | 用户绑定邮箱     |
| headImg     | String  | 用户头像       |
| builtin     | Boolean | 是否内置用户     |
| createdTime | String  | 用户创建时间     |

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": {
    "accessToken": "eyJpZCI6IjQwNGEyNTdiYzM1YTQ1NDBhZWQwNzlkYzRiNDhkOTU3IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiNzE5ODQ2MjU0NTY3NGNmY2I4MzRjZTkxYThjZTI0NGYifQ==",
    "refreshToken": "eyJpZCI6IjQwNGEyNTdiYzM1YTQ1NDBhZWQwNzlkYzRiNDhkOTU3IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiYjRhOWNhNDE2ODFlNGUyNjg4ZTU3NjI4ODdmZDE4MjEifQ==",
    "expire": 7200000,
    "failure": 86400000,
    "userInfo": {
      "id": "00000000000000000000000000000000",
      "tenantId": null,
      "name": "系统管理员",
      "account": "admin",
      "mobile": null,
      "email": null,
      "headImg": "http://image.insight.com/cloudfile/headimgs/default.png",
      "builtin": true,
      "createdTime": "2019-05-17 05:28:33"
    }
  },
  "option": null
}
```

[回目录](#目录)

### 4.刷新Token

用户可在访问令牌过期后通过该接口刷新访问令牌的使用期限，访问令牌最多可刷新12次，延长过期时间24小时。用户调用接口成功后将获取一个新的访问令牌，原访问令牌失效，刷新令牌在失效前可一直用于刷新。此接口的限流策略为10次/小时。

请求方法：**PUT**

接口URL：**/base/auth/v1.0/tokens**

接口返回数据类型：

| 字段           |    类型    | 字段说明       |
|--------------|:--------:|------------|
| accessToken  |  String  | 访问用令牌      |
| refreshToken |  String  | 刷新用令牌      |
| expire       | Integer  | 令牌过期时间(毫秒) |
| failure      | Integer  | 令牌失效时间(毫秒) |
| userInfo     | UserInfo | 用户信息       |

UserInfo 数据类型：

| 字段          |   类型    | 字段说明       |
|-------------|:-------:|------------|
| id          | String  | 用户ID       |
| tenantId    | String  | 用户当前登录租户ID |
| name        | String  | 用户姓名       |
| account     | String  | 用户登录账号     |
| mobile      | String  | 用户绑定手机号    |
| email       | String  | 用户绑定邮箱     |
| headImg     | String  | 用户头像       |
| builtin     | Boolean | 是否内置用户     |
| createdTime | String  | 用户创建时间     |

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": {
    "accessToken": "eyJpZCI6IjQwNGEyNTdiYzM1YTQ1NDBhZWQwNzlkYzRiNDhkOTU3IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiNzE5ODQ2MjU0NTY3NGNmY2I4MzRjZTkxYThjZTI0NGYifQ==",
    "refreshToken": "eyJpZCI6IjQwNGEyNTdiYzM1YTQ1NDBhZWQwNzlkYzRiNDhkOTU3IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiYjRhOWNhNDE2ODFlNGUyNjg4ZTU3NjI4ODdmZDE4MjEifQ==",
    "expire": 7200000,
    "failure": 86400000,
    "userInfo": {
      "id": "00000000000000000000000000000000",
      "tenantId": null,
      "name": "系统管理员",
      "account": "admin",
      "mobile": null,
      "email": null,
      "headImg": "http://image.insight.com/cloudfile/headimgs/default.png",
      "builtin": true,
      "createdTime": "2019-05-17 05:28:33"
    }
  },
  "option": null
}
```

[回目录](#目录)

### 5.注销Token

用户调用该接口成功后，请求头所承载的访问令牌和与其对应的刷新令牌将被注销。

请求方法：**DELETE**

接口URL：**/base/auth/v1.0/tokens**

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": null,
  "option": null
}
```

[回目录](#目录)

## 应用权限接口

### 1.获取模块导航

获取指定应用的全部模块导航数据。

请求方法：**GET**

接口URL：**/base/auth/v1.0/navigators**

接口返回数据类型：

| 字段         |     类型     | 字段说明   |
|------------|:----------:|--------|
| id         |   String   | 导航ID   |
| parentId   |   String   | 父级导航ID |
| type       |  Integer   | 导航级别   |
| index      |  Integer   | 索引,排序用 |
| name       |   String   | 导航名称   |
| ModuleInfo | moduleInfo | 模块信息   |

ModuleInfo 数据类型：

| 字段         |   类型    | 字段说明     |
|------------|:-------:|----------|
| module     | String  | 模块名称     |
| iconUrl    | String  | 图标路径     |
| file       | String  | 模块文件     |
| isAutoLoad | Boolean | 是否自动启动模块 |

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": [
    {
      "id": "5e4a994ccd2611e9bbd40242ac110008",
      "parentId": null,
      "type": 1,
      "index": 2,
      "name": "系统设置",
      "moduleInfo": {
        "iconUrl": "icons/system.png",
        "module": null,
        "file": null,
        "autoLoad": null
      },
      "functions": null
    },
    {
      "id": "717895ca14de11ea9ae00242ac110005",
      "parentId": "5e4a994ccd2611e9bbd40242ac110008",
      "type": 2,
      "index": 1,
      "name": "角色权限",
      "moduleInfo": {
        "iconUrl": "icons/role.png",
        "module": "Roles",
        "file": "System.dll",
        "autoLoad": false
      },
      "functions": null
    }
  ],
  "option": null
}
```

[回目录](#目录)

### 2.获取模块功能

根据模块ID获取该模块的全部权限(功能)信息,客户端程序可通过permit字段判断当前用户是否拥有功能的授权.

请求方法：**method**

接口URL：**/base/auth/v1.0/navigators/{id}/functions**

请求参数：**Path Variable**

| 字段  |  类型  | 必需  | 字段说明         |
|-----|:----:|:---:|--------------|
| id  | Long |  ●  | 模块ID(二级导航ID) |

接口返回数据类型：

| 字段       |    类型    | 字段说明                              |
|----------|:--------:|-----------------------------------|
| id       |  String  | 功能ID                              |
| navId    |  String  | 导航ID                              |
| type     | Integer  | 节点类型                              |
| index    | Integer  | 索引,排序用                            |
| name     |  String  | 功能名称                              |
| authCode |  String  | 授权码                               |
| funcInfo | FuncInfo | 功能图标信息                            |
| permit   | Boolean  | 是否授权(true:已授权,false:已拒绝,null:未授权) |

FuncInfo 数据类型：

| 字段           |   类型    | 字段说明   |
|--------------|:-------:|--------|
| method       | String  | 方法名称   |
| iconUrl      | String  | 图标路径   |
| isBeginGroup | Boolean | 是否开始分组 |
| isHideText   | Boolean | 是否隐藏文字 |

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": [
    {
      "id": "8ce53af6306111eaa03a0242ac110004",
      "navId": "711aad8daf654bcdb3a126d70191c15c",
      "type": 0,
      "index": 1,
      "name": "刷新",
      "authCodes": "getTenant",
      "funcInfo": {
        "method": "refresh",
        "iconUrl": "icons/refresh.png",
        "beginGroup": true,
        "hideText": true
      },
      "permit": true
    },
    {
      "id": "8ce53d39306111eaa03a0242ac110004",
      "navId": "711aad8daf654bcdb3a126d70191c15c",
      "type": 0,
      "index": 2,
      "name": "新增租户",
      "authCodes": "newTenant",
      "funcInfo": {
        "method": "new",
        "iconUrl": "icons/new.png",
        "beginGroup": true,
        "hideText": false
      },
      "permit": true
    }
  ],
  "option": null
}
```

[回目录](#目录)
