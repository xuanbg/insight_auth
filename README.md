# Insight Auth 服务使用手册

## 目录

- [概述](#概述)
- [Token接口](#Token接口)
  - [获取Code](#获取Code)
  - [获取Token](#获取Token)
  - [微信授权码获取Token](#微信授权码获取Token)
  - [微信UnionId获取Token](#微信UnionId获取Token)
  - [验证Token](#验证Token)
  - [刷新Token](#刷新Token)
  - [注销Token](#注销Token)
- [应用权限接口](#应用权限接口)
  - [获取模块导航](#获取模块导航)
  - [获取模块功能](#获取模块功能)
- [配置管理接口](#配置管理接口)
  - [获取接口配置列表](#获取接口配置列表)
  - [获取接口配置详情](#获取接口配置详情)
  - [新增接口配置](#新增接口配置)
  - [编辑接口配置](#编辑接口配置)
  - [删除接口配置](#删除接口配置)
  - [获取日志列表](#获取日志列表)
  - [获取日志详情](#获取日志详情)
  - [加载接口配置表](#加载接口配置表)
- [DTO类型说明](#DTO类型说明)

## 概述

Auth 服务是一个依赖于用户数据的、基于Token的用户身份认证服务。每一个Token都必须绑定一个AppId，该Id需要在获取Token时作为参数传入。根据应用的相应设置，系统支持两种Token发放模式：

1. 通用模式，可对不同的设备发放多个相同AppId的的Token，用户可同时在不同的设备登录同一个应用；
2. 专用模式，对不同设备发放相同AppId的时候，之前发放的Token自动失效，用户只能在一个设备上登录同一个应用。

### 主要功能

1. Token的发放(账号/密码|手机号/验证码|微信授权)、验证、刷新、注销；
2. 获取应用的功能模块导航信息，以及指定模块的功能和授权信息；
3. 接口配置数据的管理；
4. 接口配置数据变更日志查询。

### 通讯方式

**Insight** 所有的服务都支持 **HTTP/HTTPS** 协议的访问请求，以 **URL Params** 、 **Path Variable** 或 **BODY** 传递参数。如使用 **BODY** 传参，则需使用 **JSON** 格式的请求参数。接口 **URL** 区分大小写，请求以及返回都使用 **UTF-8** 字符集进行编码，接口返回的数据封装为统一的 **JSON** 格式，详见：[**Reply**](#Reply) 数据类型。除获取Token等公开接口外，都需要在请求头的 **Authorization** 字段承载 **AccessToken** 数据。HTTP请求头参数设置如下：

|参数名|参数值|
|----|----|
|Accept|application/json|
|Authorization|AccessToken(Base64编码的字符串)|
|Content-Type|application/json|

## Token接口

### 获取Code

用户可通过此接口获取一个有效时间30秒的32位随机字符串用于生成用户签名。此接口被调用时如用户数据未缓存，则缓存用户数据。同时会在缓存中保存两条过期时间为30秒的String记录。此接口的限流策略为：同一设备的调用间隔需3秒以上，每天调用上限200次。

1. Key:Sign,Value:Code。Sign的算法为 **MD5(MD5(account + password) + Code)**，调用获取Token接口时使用签名验证用户的账号/密码，或手机号/验证码是否正确。如使用手机号/验证码方式登录，则Sign的算法为 **MD5(MD5(mobile + MD5(smsCode)) + Code)**。
2. Key: Code, Value: UserId。调用获取Token接口时可凭签名得到Code，再通过Code得到用户ID。

>注：**password** 为明文密码的 **MD5** 值，数据库中以 **RSA** 算法加密该 **MD5** 值后存储。

请求方法：**GET**

接口URL：**/base/auth/v1.0/tokens/codes**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|account|是|登录账号/手机号/邮箱|
|Integer|type|否|登录类型:0.密码登录;1.验证码登录,默认为0|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|-|Code,30秒内使用有效|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/tokens/codes?account=admin" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Content-Type: application/json'
```

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

### 获取Token

用户可调用此接口获取访问令牌、刷新令牌、令牌过期时间、令牌失效时间和用户信息。signature的计算方法是 **MD5(MD5(account\|mobile\|email + MD5(password\|smsCode)) + Code)** 。此接口的限流策略为：同一设备的调用间隔需3秒以上，每天调用上限200次。

请求方法：**POST**

接口URL：**/base/auth/v1.0/tokens**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|appId|是|应用ID|
|String|tenantId|否|租户ID|
|String|deptId|否|登录部门ID|
|String|account|是|登录账号/手机号/邮箱|
|String|signature|是|签名:MD5(MD5(account\|mobile + MD5(password\|smsCode)) + Code)|
|String|deviceId|否|用户设备ID|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|accessToken|访问用令牌|
|String|refreshToken|刷新用令牌|
|Integer|expire|令牌过期时间(毫秒)|
|Integer|failure|令牌失效时间(毫秒)|
|[UserInfo](#UserInfo)|userInfo|用户信息|

请求参数示例：

```json
{
  "account": "admin",
  "signature": "9c110b1fcd11a35e495aabbd7215eb3c",
  "appId": "9dd99dd9e6df467a8207d05ea5581125",
  "tenantId": "2564cd559cd340f0b81409723fd8632a"
}
```

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
      "deptId": null,
      "code": null,
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

### 微信授权码获取Token

此接口支持通过微信授权获取访问令牌、刷新令牌、令牌过期时间、令牌失效时间和用户信息。如该微信号未绑定用户，则缓存该微信号的 [微信用户信息](#WeChatUser) 30分钟并返回该数据。前端应用可使用微信用户信息中的UnionId调用 [微信UnionId获取Token](#微信UnionId获取Token) 接口，将该UnionId绑定到指定手机号的用户，并获取Token。

请求方法：**POST**

接口URL：**/base/auth/v1.0/tokens/withWechatCode**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|appId|是|应用ID|
|String|tenantId|否|租户ID|
|String|deptId|否|登录部门ID|
|String|weChatAppId|是|微信appId|
|String|code|是|微信授权码|
|String|deviceId|否|用户设备ID|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|accessToken|访问用令牌|
|String|refreshToken|刷新用令牌|
|Integer|expire|令牌过期时间(毫秒)|
|Integer|failure|令牌失效时间(毫秒)|
|[UserInfo](#UserInfo)|userInfo|用户信息|

请求参数示例：

```json
{
  "code": "6IjQwNGEyNTdiYzM1YTQ1NDBhZWQw",
  "weChatAppId": "6821a080527d775101b624d632899fee",
  "appId": "9dd99dd9e6df467a8207d05ea5581125"
}
```

正常返回结果示例：

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
      "deptId": null,
      "code": null,
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

用户不存在时返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": {
    "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL",
    "openid": "o6_bmjrPTlm6_2sgVt7hMZOPfL2M",
    "nickname": "达明",
    "sex": 1,
    "country": "中国",
    "province": "上海",
    "city": "上海",
    "headimgurl": "http://thirdwx.qlogo.cn/mmopen/xbIQx1GRqdvyqkMMhEaGOX802l1CyqMJNgUzK/0",
    "language": "zh_CN"
  },
  "option": false
}
```

[回目录](#目录)

### 微信UnionId获取Token

此接口支持通过微信UnionId获取访问令牌、刷新令牌、令牌过期时间、令牌失效时间和用户信息。同时将用户提供的UnionId绑定到指定手机号的用户。绑定过程如下：

1. 根据 UnionId 和 weChatAppId 在缓存中找到微信用户信息，如未找到缓存数据则返回参数错误；
2. 根据手机号查询用户，如用户不存在，则根据微信用户信息创建用户并返回Token；
3. 如允许更新绑定微信号则绑定新的 UnionId 后返回Token；
4. 如不允许更新绑定微信号则返回用户已绑定其他微信号的错误；

请求方法：**POST**

接口URL：**/base/auth/v1.0/tokens/withWechatUnionId**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|appId|是|应用ID|
|String|tenantId|否|租户ID|
|String|deptId|否|登录部门ID|
|String|weChatAppId|是|微信appId|
|String|unionId|是|微信用户唯一ID|
|String|isReplace|否|是否更新微信号|
|String|deviceId|否|用户设备ID|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|accessToken|访问用令牌|
|String|refreshToken|刷新用令牌|
|Integer|expire|令牌过期时间(毫秒)|
|Integer|failure|令牌失效时间(毫秒)|
|[UserInfo](#UserInfo)|userInfo|用户信息|

请求参数示例：

```json
{
  "unionId": "6IjQwNGEyNTdiYzM1YTQ1NDBhZWQw",
  "weChatAppId": "6821a080527d775101b624d632899fee",
  "appId": "9dd99dd9e6df467a8207d05ea5581125"
}
```

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
      "deptId": null,
      "code": null,
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

### 验证Token

该接口可验证Token是否合法，无需传任何参数。

请求方法：**GET**

接口URL：**/base/auth/v1.0/tokens/status**

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/tokens/status" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization:
 eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

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

### 刷新Token

用户可在访问令牌过期后通过该接口刷新访问令牌的使用期限，访问令牌最多可刷新12次，延长过期时间24小时。用户调用接口成功后将获取一个新的访问令牌，原访问令牌失效，刷新令牌在失效前可一直用于刷新。此接口的限流策略为：同一设备的调用间隔需3秒以上，每天调用上限60次。

请求方法：**PUT**

接口URL：**/base/auth/v1.0/tokens**

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|accessToken|访问用令牌|
|String|refreshToken|刷新用令牌|
|Integer|expire|令牌过期时间(毫秒)|
|Integer|failure|令牌失效时间(毫秒)|
|[UserInfo](#UserInfo)|userInfo|用户信息|

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
      "deptId": null,
      "code": null,
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

### 注销Token

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

### 获取模块导航

获取指定应用的全部模块导航数据。

请求方法：**GET**

接口URL：**/base/auth/v1.0/navigators**

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|id|导航ID|
|String|parentId|父级导航ID|
|Integer|type|导航级别|
|Integer|index|索引,排序用|
|String|name|导航名称|
|[ModuleInfo](#ModuleInfo)|moduleInfo|模块信息|
|List\<[FuncDTO](#FuncDTO)>|functions|功能集合|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/navigators" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'

```

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

### 获取模块功能

根据模块ID获取该模块的全部权限(功能)信息,客户端程序可通过permit字段判断当前用户是否拥有功能的授权.

请求方法：**method**

接口URL：**/base/auth/v1.0/navigators/{id}/functions**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|id|是|模块ID(二级导航ID)|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|id|功能ID|
|String|navId|导航ID|
|Integer|type|节点类型|
|Integer|index|索引,排序用|
|String|name|功能名称|
|String|authCode|授权码|
|[FuncInfo](#FuncInfo)|funcInfo|功能图标信息|
|Boolean|permit|是否授权(true:已授权,false:已拒绝,null:未授权)|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/navigators/711aad8daf654bcdb3a126d70191c15c/functions" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

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

## 配置管理接口

### 获取接口配置列表

通过关键词查询接口配置。查询关键词作用于接口名称、接口URL已及授权码。该接口支持分页，如不传分页参数，则返回最近添加的20条数据。

请求方法：**GET**

接口URL：**/base/auth/v1.0/configs**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|keyword|否|查询关键词|
|Integer|page|否|分页页码|
|Integer|size|否|每页记录数|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/configs?keyword=users" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|id|接口配置ID|
|String|name|接口名称|
|String|method|请求方法(GET|POST|PUT|DELETE)|
|String|url|接口URL|
|String|authCode|授权码,仅授权接口需要具有授权码|
|Boolean|verify|是|是否需要验证,如配置了authCode,则需进行鉴权|
|Boolean|limit|是|是否限流,如配置为限流,则需配置对应限流参数|

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": [
    {
      "id": "d7c405a9ce1d49dbab17f3d7a3e0fabf",
      "name": "获取用户列表",
      "method": "GET",
      "url": "/base/user/v1.0/users",
      "authCode": null,
      "verify": true,
      "limit": true
    }
  ],
  "option": 1
}
```

[回目录](#目录)

### 获取接口配置详情

获取指定ID的接口配置详情。

请求方法：**GET**

接口URL：**/base/auth/v1.0/configs/{id}**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|id|是|接口配置ID|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|id|接口配置ID|
|String|name|接口名称|
|String|method|请求方法(GET|POST|PUT|DELETE)|
|String|url|接口URL|
|String|authCode|授权码|
|Integer|limitGap|访问最小时间间隔(秒)|
|Integer|limitCycle|限流周期(秒)|
|Integer|limitMax|最多调用次数/限流周期|
|String|message|限流时返回的错误消息|
|String|remark|备注|
|Boolean|verify|是|是否需要验证,如配置了authCode,则需进行鉴权|
|Boolean|limit|是|是否限流,如配置为限流,则需配置对应限流参数|
|Date|createdTime|创建时间|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/configs/c0592bb8dc3a11e9bc200242ac110004" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": {
    "id": "c0592bb8dc3a11e9bc200242ac110004",
    "name": "验证短信验证码",
    "method": "GET",
    "url": "/base/message/sms/v1.0/messages/codes/{key}/status",
    "authCode": null,
    "limitGap": 1,
    "limitCycle": null,
    "limitMax": null,
    "message": null,
    "remark": null,
    "createdTime": "2019-09-21 14:40:50",
    "verify": false,
    "limit": true
  },
  "option": null
}
```

[回目录](#目录)

### 新增接口配置

新增一个接口配置。

请求方法：**POST**

接口URL：**/base/auth/v1.0/configs**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|name|是|接口名称|
|String|method|是|请求方法(GET|POST|PUT|DELETE)|
|String|url|是|接口URL|
|String|authCode|否|授权码,仅授权接口需要具有授权码|
|Integer|limitGap|否|访问最小时间间隔(秒)|
|Integer|limitCycle|否|限流周期(秒)|
|Integer|limitMax|否|最多调用次数/限流周期|
|String|remark|否|备注|
|Boolean|verify|是|是否需要验证,如配置了authCode,则需进行鉴权|
|Boolean|limit|是|是否限流,如配置为限流,则需配置对应限流参数|

请求参数示例：

```json
{
  "name": "获取用户列表",
  "method": "GET",
  "url": "/base/user/v1.0/users",
  "authCode": null,
  "limitGap": 1,
  "limitCycle": null,
  "limitMax": null,
  "message": null,
  "remark": null,
  "verify": true,
  "limit": true
}
```

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": "5697c57cf0954631a5f347d7c001ecee",
  "option": null
}
```

[回目录](#目录)

### 编辑接口配置

修改指定ID的接口配置信息。

请求方法：**PUT**

接口URL：**/base/auth/v1.0/configs**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|id|是|接口配置ID|
|String|name|是|接口名称|
|String|method|是|请求方法(GET|POST|PUT|DELETE)|
|String|url|是|接口URL|
|String|authCode|否|授权码,仅授权接口需要具有授权码|
|Integer|limitGap|否|访问最小时间间隔(秒)|
|Integer|limitCycle|否|限流周期(秒)|
|Integer|limitMax|否|最多调用次数/限流周期|
|String|remark|否|备注|
|Boolean|verify|是|是否需要验证,如配置了authCode,则需进行鉴权|
|Boolean|limit|是|是否限流,如配置为限流,则需配置对应限流参数|

请求参数示例：

```json
{
  "id": "d7c405a9ce1d49dbab17f3d7a3e0fabf",
  "name": "获取用户列表",
  "method": "GET",
  "url": "/base/user/v1.0/users",
  "authCode": "getUser",
  "limitGap": 1,
  "limitCycle": null,
  "limitMax": null,
  "message": null,
  "remark": null,
  "verify": true,
  "limit": true
}
```

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

### 删除接口配置

删除指定ID的接口配置数据。

请求方法：**DELETE**

接口URL：**/base/auth/v1.0/configs**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|-|是|接口配置ID|

请求参数示例：

```json
"7179f5e4c7f84879bdfb70de0999b067"
```

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

### 获取日志列表

通过关键词查询接口配置数据变更记录。查询关键词作用于操作类型、业务名称、业务ID、部门ID、操作人ID和操作人姓名。该接口支持分页，如不传分页参数，则返回最近添加的20条数据。

请求方法：**GET**

接口URL：**/base/auth/v1.0/configs/logs**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|keyword|否|查询关键词|
|Integer|page|否|分页页码|
|Integer|size|否|每页记录数|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/configs/logs?keyword=UPDATE" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|id|日志ID|
|String|type|操作类型|
|String|business|业务名称|
|String|businessId|业务ID|
|String|deptId|创建人登录部门ID|
|String|creator|创建人|
|String|creatorId|创建人ID|
|Date|createdTime|创建时间|

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": [
    {
      "id": "fbd221108249433c9e850285263804d7",
      "type": "UPDATE",
      "business": "接口配置管理",
      "businessId": "c097f07552ca47c190f76803f9e89fb1",
      "content": null,
      "deptId": null,
      "creator": "系统管理员",
      "creatorId": "00000000000000000000000000000000",
      "createdTime": "2019-09-13 17:11:36"
    },
    {
      "id": "e7c4643e8c4942a08dc4a9a72b4a9ff5",
      "type": "UPDATE",
      "business": "接口配置管理",
      "businessId": "c097f07552ca47c190f76803f9e89fb1",
      "content": null,
      "deptId": null,
      "creator": "系统管理员",
      "creatorId": "00000000000000000000000000000000",
      "createdTime": "2019-09-13 17:12:59"
    }
  ],
  "option": 2
}
```

[回目录](#目录)

### 获取日志详情

获取指定ID的日志详情。

请求方法：**GET**

接口URL：**/base/auth/v1.0/configs/logs/{id}**

请求参数如下：

|类型|字段|是否必需|字段说明|
|----|----|----|----|
|String|id|是|日志ID|

接口返回数据类型：

|类型|字段|字段说明|
|----|----|----|
|String|id|日志ID|
|String|type|操作类型|
|String|business|业务名称|
|String|businessId|业务ID|
|Object|content|日志内容|
|String|deptId|创建人登录部门ID|
|String|creator|创建人|
|String|creatorId|创建人ID|
|Date|createdTime|创建时间|

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/configs/logs/bc3e1a2256af4445a64420b92776411c" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

返回结果示例：

```json
{
  "success": true,
  "code": 200,
  "message": "请求成功",
  "data": {
    "id": "aa8c5ee4b421440bae6e0099b6550992",
    "type": "INSERT",
    "business": "接口配置管理",
    "businessId": "c097f07552ca47c190f76803f9e89fb1",
    "content": {
      "id": "c097f07552ca47c190f76803f9e89fb1",
      "url": "/base/user/v1.0/user",
      "name": "获取用户列表",
      "limit": true,
      "method": "GET",
      "remark": null,
      "verify": true,
      "message": "获取Code接口每24小时调用次数为360次,请合理使用",
      "authCode": null,
      "limitGap": 1,
      "limitMax": 360,
      "limitCycle": 86400,
      "createdTime": null
    },
    "deptId": null,
    "creator": "系统管理员",
    "creatorId": "00000000000000000000000000000000",
    "createdTime": "2019-09-13 17:10:38"
  },
  "option": null
}
```

[回目录](#目录)

### 加载接口配置到缓存

从数据库读取全部接口配置数据，并加载到Redis。

请求方法：**GET**

接口URL：**/base/auth/v1.0/configs/load**

请求示例：

```bash
curl "http://192.168.16.1:6200/base/auth/v1.0/configs/load" \
 -H 'Accept: application/json' \
 -H 'Accept-Encoding: gzip, identity' \
 -H 'Authorization: eyJpZCI6IjUyZmFlYWI5OWUxMTQwNzBhOTliZDk2YTI0MmM3YWE2IiwidXNlcklkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAiLCJ1c2VyTmFtZSI6bnVsbCwic2VjcmV0IjoiMWQyNWY3MDEwYzVhNDFhNGJiMGE2OTE0ZDA4OWZlNzQifQ==' \
 -H 'Content-Type: application/json'
```

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

## DTO类型说明

文档中所列举的类型皆为 **Java** 语言的数据类型，其它编程语言的的数据类型请自行对应。

### Reply

|类型|字段|字段说明|
|----|----|----|
|Boolean|success|接口调用是否成功，成功：true；失败：false|
|Integer|code|错误代码，2xx代表成功，4xx或5xx代表失败|
|String|message|错误消息，描述了接口调用失败原因|
|Object|data|接口返回数据|
|Object|option|附加数据，如分页数据的总条数|

[回目录](#目录)

### UserInfo

|类型|字段|字段说明|
|----|----|----|
|String|id|用户ID|
|String|tenantId|用户当前登录租户ID|
|String|deptId|用户当前登录部门ID|
|String|code|用户编码|
|String|name|用户姓名|
|String|account|用户登录账号|
|String|mobile|用户绑定手机号|
|String|email|用户绑定邮箱|
|String|headImg|用户头像|
|Boolean|builtin|是否内置用户|
|String|createdTime|用户创建时间|

[回目录](#目录)

### WeChatUser

|类型|字段|字段说明|
|----|----|----|
|String|unionid|微信用户唯一ID|
|String|openid|微信公众号OpenId|
|String|nickname|微信昵称|
|String|sex|性别|
|String|country|所在国家|
|String|province|所在省/直辖市|
|String|city|所在地市|
|String|headimgurl|微信头像URL|
|List\<String>|privilege|微信用户特权|
|String|language|用户语言|

[回目录](#目录)

### ModuleInfo

|类型|字段|字段说明|
|----|----|----|
|String|module|模块名称|
|String|iconUrl|图标路径|
|String|file|模块文件|
|Boolean|isAutoLoad|是否自动启动模块|

[回目录](#目录)

### FuncDTO

|类型|字段|字段说明|
|----|----|----|
|String|id|功能ID|
|String|navId|导航ID|
|Integer|type|节点类型|
|Integer|index|索引,排序用|
|String|name|功能名称|
|String|authCode|授权码|
|[FuncInfo](#FuncInfo)|funcInfo|功能图标信息|
|Boolean|permit|是否授权(true:已授权,false:已拒绝,null:未授权)|

[回目录](#目录)

### FuncInfo

|类型|字段|字段说明|
|----|----|----|
|String|method|方法名称|
|String|iconUrl|图标路径|
|Boolean|isBeginGroup|是否开始分组|
|Boolean|isHideText|是否隐藏文字|

[回目录](#目录)
