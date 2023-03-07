-- ----------------------------
-- Table structure for ibs_application
-- ----------------------------
DROP TABLE IF EXISTS `ibs_application`;
CREATE TABLE `ibs_application` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-0',
  `type`               tinyint unsigned  NOT NULL DEFAULT '0'    COMMENT '类型: 0.常规应用, 1.限定应用(与用户类型匹配)',
  `index`              int unsigned      NOT NULL                COMMENT '序号',
  `name`               varchar(64)       NOT NULL                COMMENT '应用名称',
  `alias`              varchar(64)       NOT NULL                COMMENT '应用简称',
  `icon`               varchar(128)               DEFAULT NULL   COMMENT '应用图标',
  `domain`             varchar(128)               DEFAULT NULL   COMMENT '应用域名',
  `permit_life`        int unsigned      NOT NULL DEFAULT '0'    COMMENT '授权码生命周期(毫秒)',
  `token_life`         int unsigned      NOT NULL                COMMENT '令牌生命周期(毫秒)',
  `verify_source`      bit               NOT NULL DEFAULT b'0'   COMMENT '是否验证来源: 0.不验证, 1.验证',
  `signin_one`         bit               NOT NULL DEFAULT b'0'   COMMENT '是否单点登录: 0.允许多点, 1.单点登录',
  `auto_refresh`       bit               NOT NULL DEFAULT b'0'   COMMENT '是否自动刷新: 0.手动刷新, 1.自动刷新',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建用户ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_application_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_application_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='应用表';

-- ----------------------------
-- Table structure for ibs_navigator
-- ----------------------------
DROP TABLE IF EXISTS `ibs_navigator`;
CREATE TABLE `ibs_navigator` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-1',
  `parent_id`          bigint unsigned            DEFAULT NULL   COMMENT '父级导航ID',
  `app_id`             bigint unsigned   NOT NULL                COMMENT '应用ID',
  `type`               tinyint unsigned  NOT NULL                COMMENT '导航级别',
  `index`              int unsigned      NOT NULL                COMMENT '序号',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `module_info`        json                       DEFAULT NULL   COMMENT '模块信息',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建用户ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_navigator_app_id` (`app_id`) USING BTREE,
  KEY `idx_navigator_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_navigator_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_navigator_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='导航表';

-- ----------------------------
-- Table structure for ibs_function
-- ----------------------------
DROP TABLE IF EXISTS `ibs_function`;
CREATE TABLE `ibs_function` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-2',
  `nav_id`             bigint unsigned   NOT NULL                COMMENT '导航(末级模块)ID',
  `type`               tinyint unsigned  NOT NULL                COMMENT '功能类型: 0.全局功能, 1.数据项功能, 2.其他功能',
  `index`              int unsigned      NOT NULL                COMMENT '序号',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `auth_codes`         varchar(256)               DEFAULT NULL   COMMENT '接口授权码,多个授权码使用英文逗号分隔',
  `func_info`          json                       DEFAULT NULL   COMMENT '图标信息',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建用户ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_function_nav_id` (`nav_id`) USING BTREE,
  KEY `idx_function_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_function_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='功能表';


-- ----------------------------
-- Table structure for ibu_user
-- ----------------------------
DROP TABLE IF EXISTS `ibu_user`;
CREATE TABLE `ibu_user` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-3',
  `type`               tinyint unsigned  NOT NULL DEFAULT 1      COMMENT '用户类型: 0.平台用户, 1.外部用户',
  `code`               varchar(16)                DEFAULT NULL   COMMENT '用户编码',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `account`            varchar(64)       NOT NULL                COMMENT '登录账号',
  `mobile`             varchar(32)                DEFAULT NULL   COMMENT '手机号',
  `email`              varchar(64)                DEFAULT NULL   COMMENT '电子邮箱',
  `union_id`           varchar(128)               DEFAULT NULL   COMMENT '微信UnionID',
  `open_id`            json                       DEFAULT NULL   COMMENT '微信OpenID',
  `password`           varchar(256)      NOT NULL                COMMENT '密码(RSA加密)',
  `pay_password`       char(32)                   DEFAULT NULL   COMMENT '支付密码(MD5)',
  `head_img`           varchar(256)               DEFAULT NULL   COMMENT '用户头像',
  `remark`             varchar(256)               DEFAULT NULL   COMMENT '备注',
  `builtin`            bit               NOT NULL DEFAULT b'0'   COMMENT '是否内置: 0.非内置, 1.内置',
  `invalid`            bit               NOT NULL DEFAULT b'0'   COMMENT '是否失效: 0.有效, 1.失效',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建人ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_type` (`type`) USING BTREE,
  KEY `idx_user_code` (`code`) USING BTREE,
  UNIQUE KEY `idx_user_account` (`account`) USING BTREE,
  UNIQUE KEY `idx_user_mobile` (`mobile`) USING BTREE,
  UNIQUE KEY `idx_user_email` (`email`) USING BTREE,
  UNIQUE KEY `idx_user_union_id` (`union_id`) USING BTREE,
  KEY `idx_user_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_user_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='用户表';


-- ----------------------------
-- Table structure for ibt_tenant
-- ----------------------------
DROP TABLE IF EXISTS `ibt_tenant`;
CREATE TABLE `ibt_tenant` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-4',
  `code`               char(8)           NOT NULL                COMMENT '租户编号',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `alias`              varchar(8)                 DEFAULT NULL   COMMENT '别名',
  `area_code`          varchar(4)                 DEFAULT NULL   COMMENT '区号',
  `company_info`       json                       DEFAULT NULL   COMMENT '企业信息',
  `remark`             varchar(1024)              DEFAULT NULL   COMMENT '描述',
  `status`             tinyint unsigned  NOT NULL DEFAULT '0'    COMMENT '租户状态: 0.待审核, 1.已通过, 2.未通过',
  `invalid`            bit               NOT NULL DEFAULT b'0'   COMMENT '是否失效: 0.有效, 1.失效',
  `auditor`            varchar(64)                DEFAULT NULL   COMMENT '审核人',
  `auditor_id`         bigint unsigned            DEFAULT NULL   COMMENT '审核人ID',
  `audited_time`       datetime          NULL     DEFAULT NULL   COMMENT '审核时间',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建人ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_code` (`code`) USING BTREE,
  KEY `idx_tenant_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_tenant_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='租户表';

-- ----------------------------
-- Table structure for ibt_tenant_app
-- ----------------------------
DROP TABLE IF EXISTS `ibt_tenant_app`;
CREATE TABLE `ibt_tenant_app` (
  `id`                 bigint unsigned   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`          bigint unsigned   NOT NULL                COMMENT '租户ID',
  `app_id`             bigint unsigned   NOT NULL                COMMENT '应用ID',
  `expire_date`        date                       DEFAULT NULL   COMMENT '过期日期',
  PRIMARY KEY (`id`) USING BTREE,    
  KEY `idx_tenant_app_tenant_id` (`tenant_id`) USING BTREE,    
  KEY `idx_tenant_app_app_id` (`app_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='租户-应用关系表';

-- ----------------------------
-- Table structure for ibt_tenant_user
-- ----------------------------
DROP TABLE IF EXISTS `ibt_tenant_user`;
CREATE TABLE `ibt_tenant_user` (
  `id`                 bigint unsigned   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id`          bigint unsigned   NOT NULL                COMMENT '租户ID',
  `user_id`            bigint unsigned   NOT NULL                COMMENT '应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_user_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_tenant_user_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='租户-用户关系表';


-- ----------------------------
-- Table structure for ibu_group
-- ----------------------------
DROP TABLE IF EXISTS `ibu_group`;
CREATE TABLE `ibu_group` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-5',
  `tenant_id`          bigint unsigned   NOT NULL                COMMENT '租户ID',
  `code`               char(4)           NOT NULL                COMMENT '用户组编码',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `remark`             varchar(256)               DEFAULT NULL   COMMENT '备注',
  `builtin`            bit               NOT NULL DEFAULT b'0'   COMMENT '是否内置: 0.非内置, 1.内置',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建人ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_group_code` (`code`) USING BTREE,
  KEY `idx_group_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_group_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='用户组表';

-- ----------------------------
-- Table structure for ibu_group_member
-- ----------------------------
DROP TABLE IF EXISTS `ibu_group_member`;
CREATE TABLE `ibu_group_member` (
  `id`                 bigint unsigned   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id`           bigint unsigned   NOT NULL                COMMENT '用户组ID',
  `user_id`            bigint unsigned   NOT NULL                COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_member_group_id` (`group_id`) USING BTREE,
  KEY `idx_group_member_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='用户组成员表';


-- ----------------------------
-- Table structure for ibo_organize
-- ----------------------------
DROP TABLE IF EXISTS `ibo_organize`;
CREATE TABLE `ibo_organize` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-6',
  `tenant_id`          bigint unsigned   NOT NULL                COMMENT '租户ID',
  `parent_id`          bigint unsigned            DEFAULT NULL   COMMENT '父级ID',
  `type`               tinyint unsigned           DEFAULT NULL   COMMENT '节点类型: 0.机构, 1.部门, 2.职位',
  `index`              tinyint unsigned  NOT NULL                COMMENT '序号',
  `code`               varchar(8)                 DEFAULT NULL   COMMENT '编码',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `alias`              varchar(64)                DEFAULT NULL   COMMENT '简称',
  `full_name`          varchar(128)               DEFAULT NULL   COMMENT '全称',
  `remark`             varchar(256)               DEFAULT NULL   COMMENT '备注',
  `invalid`            bit               NOT NULL DEFAULT b'0'   COMMENT '是否失效: 0.有效, 1.失效',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建人ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_organize_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_organize_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_organize_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_organize_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='组织机构表';

-- ----------------------------
-- Table structure for ibo_organize_member
-- ----------------------------
DROP TABLE IF EXISTS `ibo_organize_member`;
CREATE TABLE `ibo_organize_member` (
  `id`                 bigint unsigned   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `post_id`            bigint unsigned   NOT NULL                COMMENT '职位ID(组织机构表ID)',
  `user_id`            bigint unsigned   NOT NULL                COMMENT '用户ID(用户表ID)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_organize_member_post_id` (`post_id`) USING BTREE,
  KEY `idx_organize_member_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='职位成员表';


-- ----------------------------
-- Table structure for ibr_role
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role`;
CREATE TABLE `ibr_role` (
  `id`                 bigint unsigned   NOT NULL                COMMENT '主键-7',
  `tenant_id`          bigint unsigned            DEFAULT NULL   COMMENT '租户ID,如为空且非内置则为角色模板',
  `app_id`             bigint unsigned   NOT NULL                COMMENT '应用ID',
  `name`               varchar(64)       NOT NULL                COMMENT '名称',
  `remark`             varchar(256)               DEFAULT NULL   COMMENT '备注',
  `builtin`            bit               NOT NULL DEFAULT b'0'   COMMENT '是否内置: 0.非内置, 1.内置',
  `creator`            varchar(64)       NOT NULL                COMMENT '创建人',
  `creator_id`         bigint unsigned   NOT NULL                COMMENT '创建人ID',
  `created_time`       datetime          NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_role_app_id` (`app_id`) USING BTREE,
  KEY `idx_role_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_role_created_time` (`created_time`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='角色表';

-- ----------------------------
-- Table structure for ibr_role_permit
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role_permit`;
CREATE TABLE `ibr_role_permit` (
  `id`                 bigint unsigned   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`            bigint unsigned   NOT NULL                COMMENT '角色ID',
  `function_id`        bigint unsigned   NOT NULL                COMMENT '功能ID',
  `permit`             bit               NOT NULL DEFAULT b'0'   COMMENT '授权类型: 0.拒绝, 1.允许',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_func_permit_role_id` (`role_id`) USING BTREE,
  KEY `idx_role_func_permit_function_id` (`function_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='角色功能权限表';

-- ----------------------------
-- Table structure for ibr_role_member
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role_member`;
CREATE TABLE `ibr_role_member` (
  `id`                 bigint unsigned   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type`               tinyint unsigned  NOT NULL DEFAULT '0'    COMMENT '成员类型: 0.未定义, 1.用户, 2.用户组, 3.职位',
  `role_id`            bigint unsigned   NOT NULL                COMMENT '角色ID',
  `member_id`          bigint unsigned   NOT NULL                COMMENT '成员ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_member_role_id` (`role_id`) USING BTREE,
  KEY `idx_role_member_member_id` (`member_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT ='角色成员表';


-- ----------------------------
-- View structure for ibv_user_roles
-- ----------------------------
DROP VIEW IF EXISTS ibv_user_roles;
CREATE VIEW ibv_user_roles AS
select r.tenant_id,
       m.role_id,
       m.member_id as user_id
from ibr_role r
     join ibr_role_member m on m.role_id = r.id
where type = 1
union
select r.tenant_id,
       m.role_id,
       g.user_id
from ibr_role r
     join ibr_role_member m on m.role_id = r.id and m.type = 2
     join ibu_group_member g on g.group_id = m.member_id
union
select r.tenant_id,
       m.role_id,
       p.user_id
from ibr_role r
     join ibr_role_member m on m.role_id = r.id and m.type = 3
     join ibo_organize_member p on p.post_id = m.member_id
     join ibo_organize o on o.id = p.post_id;


-- ----------------------------
-- 初始化应用
-- ----------------------------
INSERT ibs_application (`id`, `index`, `name`, `alias`, `permit_life`, `token_life`, `creator`, `creator_id`, `created_time`) VALUES
(134660498556715024, 1, '因赛特多租户平台管理系统', 'MTP', 5, 7200, '系统', 0, now()),
(134661270778413072, 2, '因赛特用户及权限管理系统', 'RMS', 5, 7200, '系统', 0, now());

-- ----------------------------
-- 初始化应用:功能导航
-- ----------------------------
INSERT ibs_navigator(`id`, `parent_id`, `app_id`, `type`, `index`, `name`, `module_info`, `creator`, `creator_id`, `created_time`) VALUES
(134662851666116624, NULL, 134660498556715024, 1, 1, '运营中心', json_object("iconUrl", "icons/operation.png"), '系统', 0, now()),
(134677437442162704, 134662851666116624, 134660498556715024, 2, 1, '租户管理', json_object("module", 'Tenants', "file", 'Platform.dll', "autoLoad", true, "iconUrl", "icons/tenant.png"), '系统', 0, now()),
(134686821870206992, 134662851666116624, 134660498556715024, 2, 2, '平台用户', json_object("module", 'Users', "file", 'Platform.dll', "autoLoad", false, "iconUrl", "icons/user.png"), '系统', 0, now()),
(134687738271105040, 134662851666116624, 134660498556715024, 2, 3, '计划任务', json_object("module", 'Schedules', "file", 'Platform.dll', "autoLoad", false, "iconUrl", "icons/schedul.png"), '系统', 0, now()),
(134693638549536784, 134662851666116624, 134660498556715024, 2, 4, '消息场景', json_object("module", 'Scenes', "file", 'Platform.dll', "autoLoad", false, "iconUrl", "icons/scene.png"), '系统', 0, now()),
(134667870310236176, NULL, 134660498556715024, 1, 2, '基础数据', json_object("iconUrl", "icons/basedata.png"), '系统', 0, now()),
(134693821907730448, 134667870310236176, 134660498556715024, 2, 1, '应用管理', json_object("module", 'Apps', "file", 'Data.dll', "autoLoad", false, "iconUrl", "icons/resource.png"), '系统', 0, now()),
(134694149466095632, 134667870310236176, 134660498556715024, 2, 2, '接口管理', json_object("module", 'Interfaces', "file", 'Data.dll', "autoLoad", false, "iconUrl", "icons/interface.png"), '系统', 0, now()),
(134694421563179024, 134667870310236176, 134660498556715024, 2, 3, '数据字典', json_object("module", 'Dicts', "file", 'Data.dll', "autoLoad", false, "iconUrl", "icons/dict.png"), '系统', 0, now()),
(134668376701141008, NULL, 134660498556715024, 1, 3, '系统设置', json_object("iconUrl", "icons/setting.png"), '系统', 0, now()),
(134801956148346896, 134668376701141008, 134660498556715024, 2, 1, '角色权限', json_object("module", 'Roles', "file", 'Setting.dll', "autoLoad", false, "iconUrl", "icons/role.png"), '系统', 0, now()),
(134669224093155344, NULL, 134661270778413072, 1, 1, '基础数据', json_object("iconUrl", "icons/basedata.png"), '系统', 0, now()),
(134987266836660240, 134669224093155344, 134661270778413072, 2, 1, '数据字典', json_object("module", 'Dicts', "file", 'Data.dll', "autoLoad", false, "iconUrl", "icons/dict.png"), '系统', 0, now()),
(134987597012271120, 134669224093155344, 134661270778413072, 2, 2, '模板设计', json_object("module", 'Templates', "file", 'Report.dll', "autoLoad", false, "iconUrl", "icons/design.png"), '系统', 0, now()),
(134987730965757968, 134669224093155344, 134661270778413072, 2, 3, '消息场景', json_object("module", 'Scenes', "file", 'Data.dll', "autoLoad", false, "iconUrl", "icons/scene.png"), '系统', 0, now()),
(134676087283122192, NULL, 134661270778413072, 1, 2, '系统设置', json_object("iconUrl", "icons/setting.png"), '系统', 0, now()),
(134987895294394384, 134676087283122192, 134661270778413072, 2, 1, '用户', json_object("module", 'Users', "file", 'Base.dll', "autoLoad", true, "iconUrl", "icons/user.png"), '系统', 0, now()),
(135143913647243280, 134676087283122192, 134661270778413072, 2, 2, '用户组', json_object("module", 'Groups', "file", 'Base.dll', "autoLoad", false, "iconUrl", "icons/group.png"), '系统', 0, now()),
(135159931337703440, 134676087283122192, 134661270778413072, 2, 3, '组织机构', json_object("module", 'Organizes', "file", 'Base.dll', "autoLoad", false, "iconUrl", "icons/organize.png"), '系统', 0, now()),
(135172016545202192, 134676087283122192, 134661270778413072, 2, 4, '角色权限', json_object("module", 'Roles', "file", 'Base.dll', "autoLoad", false, "iconUrl", "icons/role.png"), '系统', 0, now());

-- ----------------------------
-- 平台应用
-- ----------------------------
INSERT ibs_function(`id`, `nav_id`, `type`, `index`, `name`, `auth_codes`, `func_info`, `creator`, `creator_id`, `created_time`) VALUES
(135176265446457360, 134677437442162704, 0, 1, '刷新', 'getTenant', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(135184804227317776, 134677437442162704, 0, 2, '新增租户', 'newTenant', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(135195080192426000, 134677437442162704, 1, 3, '编辑', 'editTenant', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135196622463172624, 134677437442162704, 1, 4, '删除', 'deleteTenant', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135199679670386704, 134677437442162704, 1, 5, '审核', 'auditTenant', json_object("method", "audit", "iconUrl", "icons/audit.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(135200393624813584, 134677437442162704, 1, 6, '禁用', 'disableTenant', json_object("method", "disable", "iconUrl", "icons/disable.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135200994538553360, 134677437442162704, 1, 7, '启用', 'enableTenant', json_object("method", "enable", "iconUrl", "icons/enable.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135204327152156688, 134677437442162704, 1, 8, '绑定应用', 'bindApp', json_object("method", "bind", "iconUrl", "icons/bind.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(135212952100798480, 134677437442162704, 1, 9, '解绑应用', 'unbindApp', json_object("method", "unbind", "iconUrl", "icons/unbind.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135375791633465360, 134677437442162704, 1, 10, '续租应用', 'rentTenantApp', json_object("method", "rent", "iconUrl", "icons/rent.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135511518669176848, 134677437442162704, 0, 11, '查看日志', 'getTenantLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(135511637997125648, 134686821870206992, 0, 1, '刷新', 'getUser', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(135529759529500688, 134686821870206992, 0, 2, '新增用户', 'newUser', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(135547663910174736, 134686821870206992, 1, 3, '编辑', 'editUser', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(135547787197546512, 134686821870206992, 1, 4, '删除', 'deleteUser', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136498440094875664, 134686821870206992, 1, 5, '封禁', 'bannedUser', json_object("method", "disable", "iconUrl", "icons/disable.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136498458226851856, 134686821870206992, 1, 6, '解封', 'releaseUser', json_object("method", "enable", "iconUrl", "icons/enable.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136581793712177168, 134686821870206992, 1, 7, '重置密码', 'resetPassword', json_object("method", "reset", "iconUrl", "icons/reset.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136584873807708176, 134686821870206992, 0, 8, '查看日志', 'getUserLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(136585061590892560, 134687738271105040, 0, 1, '刷新', 'getSchedule', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(136585198014824464, 134687738271105040, 1, 2, '立即执行', 'executeSchedule', json_object("method", "execute", "iconUrl", "icons/execute.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136587520971374608, 134687738271105040, 1, 3, '删除', 'deleteSchedule', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136593138591465488, 134687738271105040, 1, 4, '禁用', 'disableSchedule', json_object("method", "disable", "iconUrl", "icons/disable.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136600640645234704, 134687738271105040, 1, 5, '启用', 'enableSchedule', json_object("method", "enable", "iconUrl", "icons/enable.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136605478531629072, 134687738271105040, 0, 6, '查看日志', 'getScheduleLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(136613642945691664, 134693638549536784, 0, 1, '刷新', 'getScene', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(136617732052353040, 134693638549536784, 0, 2, '新增场景', 'newScene', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136620088739495952, 134693638549536784, 1, 3, '编辑', 'editScene', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136621704779661328, 134693638549536784, 1, 4, '删除', 'deleteScene', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136622740151992336, 134693638549536784, 1, 5, '新增配置', 'newSceneConfig,getApp', json_object("method", "newConfig", "iconUrl", "icons/newValue.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136629768526233616, 134693638549536784, 1, 6, '编辑', 'editSceneConfig,getApp', json_object("method", "editConfig", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136630274938109968, 134693638549536784, 1, 7, '删除', 'deleteSceneConfig', json_object("method", "deleteConfig", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136825929446981648, 134693638549536784, 0, 8, '查看日志', 'getSceneLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

-- ----------------------------
-- 平台基础数据
-- ----------------------------
(136827783442595856, 134693821907730448, 0, 1, '刷新', 'getApp', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(136828861861068816, 134693821907730448, 0, 2, '新增应用', 'newApp', json_object("method", "newApp", "iconUrl", "icons/newapp.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136832381523853328, 134693821907730448, 1, 3, '编辑', 'editApp', json_object("method", "editApp", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136836261057921040, 134693821907730448, 1, 4, '删除', 'deleteApp', json_object("method", "deleteApp", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136837283843145744, 134693821907730448, 1, 5, '新增导航', 'newNav', json_object("method", "newNav", "iconUrl", "icons/newnav.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136840014125334544, 134693821907730448, 1, 6, '编辑', 'editNav', json_object("method", "editNav", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136841287390199824, 134693821907730448, 1, 7, '删除', 'deleteNav', json_object("method", "deleteNav", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136841568651837456, 134693821907730448, 1, 8, '新增功能', 'newFunc', json_object("method", "newFunc", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136844176019947536, 134693821907730448, 1, 9, '编辑', 'editFunc', json_object("method", "editFunc", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136846534900711440, 134693821907730448, 1, 10, '删除', 'deleteFunc', json_object("method", "deleteFunc", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136847562358390800, 134693821907730448, 0, 11, '查看日志', 'getAppLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(136848324241129488, 134694149466095632, 0, 1, '刷新', 'getConfig', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(136849380882776080, 134694149466095632, 0, 2, '新增接口', 'newConfig', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136850670266351632, 134694149466095632, 1, 3, '编辑', 'editConfig', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136853914136870928, 134694149466095632, 1, 4, '删除', 'deleteConfig', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136854528891813904, 134694149466095632, 0, 5, '加载配置', 'loadConfig', json_object("method", "load", "iconUrl", "icons/sync.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136858631420248080, 134694149466095632, 0, 6, '查看日志', 'getConfigLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(136859174897188880, 134694421563179024, 0, 1, '刷新', 'getDict', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(136860340674625552, 134694421563179024, 0, 2, '新增字典', 'newDict,getApp', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136861908820033552, 134694421563179024, 1, 3, '编辑字典', 'editDict,getApp', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136862060397985808, 134694421563179024, 1, 4, '删除字典', 'deleteDict', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136864019771293712, 134694421563179024, 0, 5, '新增值', 'newValue', json_object("method", "newValue", "iconUrl", "icons/newValue.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(136864061580115984, 134694421563179024, 1, 6, '编辑值', 'editValue', json_object("method", "editValue", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(136959397728354320, 134694421563179024, 1, 7, '删除值', 'deleteValue', json_object("method", "deleteValue", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137183443061571600, 134694421563179024, 0, 8, '查看日志', 'getDictLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

-- ----------------------------
-- 平台用户权限
-- ----------------------------
(137183923015778320, 134801956148346896, 0, 1, '刷新', 'getRole', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137184093442932752, 134801956148346896, 0, 2, '新增角色', 'newRole,setRoleFunc', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137184572088516624, 134801956148346896, 1, 3, '编辑', 'editRole,setRoleFunc', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137184594788089872, 134801956148346896, 1, 4, '删除', 'deleteRole', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137184672034586640, 134801956148346896, 1, 5, '添加成员', 'addRoleMember', json_object("method", "addMember", "iconUrl", "icons/add.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137184792885067792, 134801956148346896, 1, 6, '移除成员', 'removeRoleMember', json_object("method", "removeMember", "iconUrl", "icons/remove.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137184897545535504, 134801956148346896, 0, 9, '查看日志', 'getRoleLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),


-- ----------------------------
-- 租户基础数据
-- ----------------------------
(137186238468718608, 134987266836660240, 0, 1, '刷新', 'getDict', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137186726463406096, 134987266836660240, 0, 2, '新增值', 'newValue', json_object("method", "newValue", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137186726492766224, 134987266836660240, 1, 3, '编辑值', 'editValue', json_object("method", "editValue", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137187777031700496, 134987266836660240, 1, 4, '删除值', 'deleteValue', json_object("method", "deleteValue", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137191912959377424, 134987266836660240, 0, 5, '查看日志', 'getDictLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(137193338099990544, 134987597012271120, 0, 1, '刷新', 'getTemplate', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137193878095659024, 134987597012271120, 1, 2, '导入', 'importTemplate', json_object("method", "import", "iconUrl", "icons/import.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137194373312938000, 134987597012271120, 1, 3, '编辑', 'editTemplate', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137194683313946640, 134987597012271120, 1, 4, '删除', 'deleteTemplate', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137198764392710160, 134987597012271120, 1, 5, '复制', 'copyTemplate', json_object("method", "copy", "iconUrl", "icons/copy.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137199396247830544, 134987597012271120, 1, 6, '设计', 'designTemplate', json_object("method", "design", "iconUrl", "icons/design16.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137201646647115792, 134987597012271120, 1, 7, '禁用', 'disableTemplate', json_object("method", "disable", "iconUrl", "icons/disable.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137202444223381520, 134987597012271120, 1, 8, '启用', 'enableTemplate', json_object("method", "enable", "iconUrl", "icons/enable.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137204461440335888, 134987597012271120, 0, 9, '查看日志', 'getTemplateLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(137209097014476816, 134987730965757968, 0, 1, '刷新', 'getScene', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137302943958302736, 134987730965757968, 1, 2, '新增配置', 'newSceneConfig,getApp', json_object("method", "newConfig", "iconUrl", "icons/newValue.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137302943966691344, 134987730965757968, 1, 3, '编辑', 'editSceneConfig,getApp', json_object("method", "editConfig", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137303827987562512, 134987730965757968, 1, 4, '删除', 'deleteSceneConfig', json_object("method", "deleteConfig", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),

-- ----------------------------
-- 租户系统设置
-- ----------------------------
(137304987322548240, 134987895294394384, 0, 1, '刷新', 'getUser', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137305254373883920, 134987895294394384, 0, 2, '新增用户', 'newUser', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137305469814308880, 134987895294394384, 1, 3, '编辑', 'editUser', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137325962659364880, 134987895294394384, 1, 4, '重置密码', 'resetPassword', json_object("method", "reset", "iconUrl", "icons/reset.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137326290138038288, 134987895294394384, 0, 5, '邀请用户', 'inviteUser', json_object("method", "inviteUser", "iconUrl", "icons/add.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137326523928543248, 134987895294394384, 1, 6, '清退用户', 'removeUser', json_object("method", "removeUser", "iconUrl", "icons/remove.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137326685291806736, 134987895294394384, 0, 7, '查看日志', 'getUserLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(137326835884097552, 135143913647243280, 0, 1, '刷新', 'getGroup', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137327116730499088, 135143913647243280, 0, 2, '新增用户组', 'newGroup', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137327470650064912, 135143913647243280, 1, 3, '编辑', 'editGroup', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137327637189099536, 135143913647243280, 1, 4, '删除', 'deleteGroup', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137327832006131728, 135143913647243280, 1, 5, '添加成员', 'addGroupMember', json_object("method", "addMember", "iconUrl", "icons/add.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137328046037270544, 135143913647243280, 1, 6, '移除成员', 'removeGroupMember', json_object("method", "removeMember", "iconUrl", "icons/remove.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137328230725058576, 135143913647243280, 0, 7, '查看日志', 'getGroupLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now()),

(137328438158557200, 135172016545202192, 0, 1, '刷新', 'getRole', json_object("method", "refresh", "iconUrl", "icons/refresh.png", "beginGroup", true, "hideText", true), '系统', 0, now()),
(137328594828394512, 135172016545202192, 0, 2, '新增角色', 'newRole,setRoleFunc', json_object("method", "newItem", "iconUrl", "icons/new.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137328741670977552, 135172016545202192, 1, 3, '编辑', 'editRole,setRoleFunc', json_object("method", "editItem", "iconUrl", "icons/edit.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137329023565955088, 135172016545202192, 1, 4, '删除', 'deleteRole', json_object("method", "deleteItem", "iconUrl", "icons/delete.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137541903179579408, 135172016545202192, 1, 5, '添加成员', 'addRoleMember', json_object("method", "addMember", "iconUrl", "icons/add.png", "beginGroup", true, "hideText", false), '系统', 0, now()),
(137543136208486416, 135172016545202192, 1, 6, '移除成员', 'removeRoleMember', json_object("method", "removeMember", "iconUrl", "icons/remove.png", "beginGroup", false, "hideText", false), '系统', 0, now()),
(137543503541436432, 135172016545202192, 0, 9, '查看日志', 'getRoleLog', json_object("method", "log", "iconUrl", "icons/log.png", "beginGroup", true, "hideText", false), '系统', 0, now());


-- ----------------------------
-- 初始化用户:系统管理员
-- ----------------------------
INSERT ibu_user (`id`, `type`, `name`, `account`, `password`, `builtin`, `creator`, `creator_id`, `created_time`) VALUES
(0, 0, '系统管理员', 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, '系统', 0, now());

-- ----------------------------
-- 初始化角色:管理员
-- ----------------------------
insert ibr_role (`id`, `tenant_id`, `app_id`, `name`, `remark`, `builtin`, `creator`, `creator_id`, `created_time`) values
(137703403710054416, NULL, 134660498556715024, '平台管理员', '内置平台管理员角色', 0, '系统', 0, now()),
(137703825636065296, NULL, 134661270778413072, '系统管理员', '租户系统管理员角色模板', 1, '系统', 0, now());
insert ibr_role_member(`type`, `role_id`, `member_id`) values
(1, 137703403710054416, 0);

-- ----------------------------
-- 初始化功能权限
-- ---------------------------- 
INSERT `ibr_role_permit`(`role_id`, `function_id`, `permit`) 
select r.id, f.id, 1
from ibr_role r
join ibs_navigator n on n.app_id = r.app_id
join ibs_function f on f.nav_id = n.id
where r.app_id in (134660498556715024,134661270778413072);
