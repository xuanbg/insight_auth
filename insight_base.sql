
-- ----------------------------
-- Table structure for ibl_operate_log
-- ----------------------------
DROP TABLE IF EXISTS `ibl_operate_log`;
CREATE TABLE `ibl_operate_log` (
  `id` char(32) NOT NULL COMMENT 'UUID主键',
  `type` varchar(16) NOT NULL COMMENT '类型',
  `business_id` char(32) DEFAULT NULL COMMENT '业务ID',
  `business` varchar(16) DEFAULT NULL COMMENT '业务名称',
  `content` json DEFAULT NULL COMMENT '日志内容',
  `dept_id` char(32) DEFAULT NULL COMMENT '创建人部门ID',
  `creator` varchar(32) NOT NULL COMMENT '创建人,系统自动为系统',
  `creator_id` char(32) NOT NULL COMMENT '创建人ID,系统自动为32个0',
  `created_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_operate_log_type` (`type`) USING BTREE,
  KEY `idx_operate_log_business_id` (`business_id`) USING BTREE,
  KEY `idx_operate_log_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_operate_log_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_operate_log_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='操作日志记录表';

-- ----------------------------
-- Table structure for ibi_interface
-- ----------------------------
DROP TABLE IF EXISTS `ibi_interface`;
CREATE TABLE `ibi_interface` (
  `id` char(32) NOT NULL COMMENT 'UUID主键',
  `name` varchar(64) NOT NULL COMMENT '接口名称',
  `method` varchar(8) NOT NULL COMMENT 'HTTP请求方法',
  `url` varchar(128) NOT NULL COMMENT '接口URL',
  `auth_code` varchar(32) DEFAULT NULL COMMENT '授权码,如接口需要鉴权,则必须设置授权码',
  `limit_gap` int(10) unsigned DEFAULT NULL DEFAULT 0 COMMENT '最小间隔(秒),0表示无调用时间间隔',
  `limit_cycle` int(10) unsigned DEFAULT NULL COMMENT '限流周期(秒),null表示不进行周期性限流',
  `limit_max` int(10) unsigned DEFAULT NULL COMMENT '限制次数/限流周期,null表示不进行周期性限流',
  `message` varchar(32) DEFAULT NULL COMMENT '限流消息',
  `remark` varchar(1024) DEFAULT NULL COMMENT '描述',
  `is_verify` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否验证Token:0.公开接口,不需要验证Token;1.私有接口,需要验证Token',
  `is_limit` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否限流:0.不限流;1.限流',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `idx_interface_hash` (`method`,`url`) USING BTREE,
  KEY `idx_interface_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='接口配置表';


-- ----------------------------
-- Table structure for ibt_tenant
-- ----------------------------
DROP TABLE IF EXISTS `ibt_tenant`;
CREATE TABLE `ibt_tenant` (
  `id` char(32) NOT NULL COMMENT 'UUID主键',
  `code` char(6) NOT NULL COMMENT '租户编号',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `alias` varchar(8) DEFAULT NULL COMMENT '别名',
  `company_info` json DEFAULT NULL COMMENT '企业信息',
  `remark` varchar(1024) DEFAULT NULL COMMENT '描述',
  `expire_date` date DEFAULT NULL COMMENT '过期日期',
  `status` tinyint(3) unsigned NOT NULL DEFAULT '0' COMMENT '租户状态:0.待审核;1.已通过;2.未通过',
  `is_invalid` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否失效:0.正常;1.失效',
  `auditor` varchar(64) DEFAULT NULL COMMENT '审核人',
  `auditor_id` char(32) DEFAULT NULL COMMENT '审核人ID',
  `audited_time` timestamp NULL DEFAULT NULL COMMENT '审核时间',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建人ID',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_code` (`code`) USING BTREE,
  KEY `idx_tenant_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_tenant_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='租户表';

-- ----------------------------
-- Table structure for ibt_tenant_app
-- ----------------------------
DROP TABLE IF EXISTS `ibt_tenant_app`;
CREATE TABLE `ibt_tenant_app` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `tenant_id` char(32) NOT NULL COMMENT '租户ID',
  `app_id` char(32) NOT NULL COMMENT '应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_app_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_tenant_app_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='租户-应用关系表';

-- ----------------------------
-- Table structure for ibt_tenant_user
-- ----------------------------
DROP TABLE IF EXISTS `ibt_tenant_user`;
CREATE TABLE `ibt_tenant_user` (
  `id` char(32) NOT NULL COMMENT 'UUID主键',
  `tenant_id` char(32) NOT NULL COMMENT '租户ID',
  `user_id` char(32) NOT NULL COMMENT '应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_tenant_user_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_tenant_user_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='租户-用户关系表';


-- ----------------------------
-- Table structure for ibs_application
-- ----------------------------
DROP TABLE IF EXISTS `ibs_application`;
CREATE TABLE `ibs_application` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `index` int(11) unsigned NOT NULL COMMENT '序号',
  `name` varchar(64) NOT NULL COMMENT '应用名称',
  `alias` varchar(64) NOT NULL COMMENT '应用简称',
  `icon` varchar(128) DEFAULT NULL COMMENT '应用图标',
  `domain` varchar(128) DEFAULT NULL COMMENT '应用域名',
  `token_life` int(10) unsigned NOT NULL DEFAULT '24' COMMENT '令牌生命周期(毫秒)',
  `is_signin_one` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否单点登录:0.允许多点;1.单点登录',
  `is_auto_refresh` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否自动刷新:0.手动刷新;1.自动刷新',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建用户ID',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_application_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_application_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='应用表';

-- ----------------------------
-- Table structure for ibs_navigator
-- ----------------------------
DROP TABLE IF EXISTS `ibs_navigator`;
CREATE TABLE `ibs_navigator` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `parent_id` char(32) DEFAULT NULL COMMENT '父级导航ID',
  `app_id` char(32) NOT NULL COMMENT '应用ID',
  `type` tinyint(3) unsigned NOT NULL COMMENT '导航级别',
  `index` int(11) unsigned NOT NULL COMMENT '序号',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `module_info` json DEFAULT NULL COMMENT '模块信息',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建用户ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_navigator_app_id` (`app_id`) USING BTREE,
  KEY `idx_navigator_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_navigator_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_navigator_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导航表';

-- ----------------------------
-- Table structure for ibs_function
-- ----------------------------
DROP TABLE IF EXISTS `ibs_function`;
CREATE TABLE `ibs_function` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `nav_id` char(32) NOT NULL COMMENT '导航(末级模块)ID',
  `type` tinyint(3) unsigned NOT NULL COMMENT '功能类型 0:全局功能;1:数据项功能;2:其他功能',
  `index` int(11) unsigned NOT NULL COMMENT '序号',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `auth_code` varchar(32) DEFAULT NULL COMMENT '接口授权码,多个授权码使用英文逗号分隔',
  `icon_info` json DEFAULT NULL COMMENT '图标信息',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建用户ID',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_function_nav_id` (`nav_id`) USING BTREE,
  KEY `idx_function_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_function_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能表';


-- ----------------------------
-- Table structure for ibo_organize
-- ----------------------------
DROP TABLE IF EXISTS `ibo_organize`;
CREATE TABLE `ibo_organize` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `tenant_id` char(32) NOT NULL COMMENT '租户ID',
  `parent_id` char(32) DEFAULT NULL COMMENT '父级ID',
  `type` tinyint(3) unsigned DEFAULT NULL COMMENT '节点类型:0.机构;1.部门;2.职位',
  `index` tinyint(3) unsigned NOT NULL COMMENT '序号',
  `code` varchar(8) DEFAULT NULL COMMENT '编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `alias` varchar(64) DEFAULT NULL COMMENT '简称',
  `full_name` varchar(128) DEFAULT NULL COMMENT '全称',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `is_invalid` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否失效:0.有效;1.失效',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建人ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_organize_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_organize_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_organize_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_organize_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='组织机构表';

-- ----------------------------
-- Table structure for ibo_organize_member
-- ----------------------------
DROP TABLE IF EXISTS `ibo_organize_member`;
CREATE TABLE `ibo_organize_member` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `post_id` char(32) NOT NULL COMMENT '职位ID(组织机构表ID)',
  `user_id` char(32) NOT NULL COMMENT '用户ID(用户表ID)',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_organize_member_post_id` (`post_id`) USING BTREE,
  KEY `idx_organize_member_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='职位成员表';


-- ----------------------------
-- Table structure for ibu_user
-- ----------------------------
DROP TABLE IF EXISTS `ibu_user`;
CREATE TABLE `ibu_user` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `code` varchar(8) DEFAULT NULL COMMENT '用户编码',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `account` varchar(64) NOT NULL COMMENT '登录账号',
  `mobile` varchar(32) DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) DEFAULT NULL COMMENT '电子邮箱',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信UnionID',
  `open_id` json DEFAULT NULL COMMENT '微信OpenID',
  `password` varchar(256) NOT NULL DEFAULT 'e10adc3949ba59abbe56e057f20f883e' COMMENT '密码(RSA加密)',
  `paypw` char(32) DEFAULT NULL COMMENT '支付密码(MD5)',
  `head_img` varchar(256) DEFAULT NULL COMMENT '用户头像',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `is_builtin` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置:0.非内置;1.内置',
  `is_invalid` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否失效:0.有效;1.失效',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建人ID',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_code` (`code`) USING BTREE,
  UNIQUE KEY `idx_user_account` (`account`) USING BTREE,
  UNIQUE KEY `idx_user_mobile` (`mobile`) USING BTREE,
  UNIQUE KEY `idx_user_email` (`email`) USING BTREE,
  UNIQUE KEY `idx_user_union_id` (`union_id`) USING BTREE,
  KEY `idx_user_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_user_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='用户表';


-- ----------------------------
-- Table structure for ibu_group
-- ----------------------------
DROP TABLE IF EXISTS `ibu_group`;
CREATE TABLE `ibu_group` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `tenant_id` char(32) NOT NULL COMMENT '租户ID',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `is_builtin` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置:0.非内置;1.内置',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建人ID',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_group_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_group_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='用户组表';

-- ----------------------------
-- Table structure for ibu_group_member
-- ----------------------------
DROP TABLE IF EXISTS `ibu_group_member`;
CREATE TABLE `ibu_group_member` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `group_id` char(32) NOT NULL COMMENT '用户组ID',
  `user_id` char(32) NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_group_member_group_id` (`group_id`) USING BTREE,
  KEY `idx_group_member_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='用户组成员表';


-- ----------------------------
-- Table structure for ibr_config
-- ----------------------------
DROP TABLE IF EXISTS `ibr_config`;
CREATE TABLE `ibr_config` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `data_type` int(3) unsigned NOT NULL COMMENT '类型:0.无归属;1.仅本人;2.仅本部门;3.部门所有;4.机构所有',
  `name` varchar(32) NOT NULL COMMENT '名称',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_config_data_type` (`data_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='数据配置表';

-- ----------------------------
-- Table structure for ibr_role
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role`;
CREATE TABLE `ibr_role` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `tenant_id` char(32) DEFAULT NULL COMMENT '租户ID,如为空则为角色模板',
  `app_id` char(32) DEFAULT NULL COMMENT '应用ID,如不为空则该角色为应用专有',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `is_builtin` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置:0.非内置;1.内置',
  `creator` varchar(64) NOT NULL COMMENT '创建人',
  `creator_id` char(32) NOT NULL COMMENT '创建人ID',
  `created_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_tenant_id` (`tenant_id`) USING BTREE,
  KEY `idx_role_app_id` (`app_id`) USING BTREE,
  KEY `idx_role_creator_id` (`creator_id`) USING BTREE,
  KEY `idx_role_created_time` (`created_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色表';

-- ----------------------------
-- Table structure for ibr_role_data_permit
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role_data_permit`;
CREATE TABLE `ibr_role_data_permit` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `role_id` char(32) NOT NULL COMMENT '角色ID',
  `module_id` char(32) NOT NULL COMMENT '业务模块ID',
  `mode` int(3) unsigned NOT NULL COMMENT '授权模式:0.相对模式;1.用户模式;2.部门模式',
  `owner_id` char(32) NOT NULL COMMENT '数据所有者ID,相对模式下为模式ID',
  `permit` bit(1) NOT NULL DEFAULT b'0' COMMENT '授权类型:0.只读;1.读写',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_data_permit_role_id` (`role_id`) USING BTREE,
  KEY `idx_role_data_permit_module_id` (`module_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色数据权限表';

-- ----------------------------
-- Table structure for ibr_role_func_permit
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role_func_permit`;
CREATE TABLE `ibr_role_func_permit` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `role_id` char(32) NOT NULL COMMENT '角色ID',
  `function_id` char(32) NOT NULL COMMENT '功能ID',
  `permit` bit(1) NOT NULL DEFAULT b'0' COMMENT '授权类型:0.拒绝;1.允许',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_func_permit_role_id` (`role_id`) USING BTREE,
  KEY `idx_role_func_permit_function_id` (`function_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色功能权限表';

-- ----------------------------
-- Table structure for ibr_role_member
-- ----------------------------
DROP TABLE IF EXISTS `ibr_role_member`;
CREATE TABLE `ibr_role_member` (
  `id` char(32) NOT NULL COMMENT '主键(UUID)',
  `type` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '成员类型:0.未定义;1.用户;2.用户组;3.职位',
  `role_id` char(32) NOT NULL COMMENT '角色ID',
  `member_id` char(32) NOT NULL COMMENT '成员ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_role_member_role_id` (`role_id`) USING BTREE,
  KEY `idx_role_member_member_id` (`member_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色成员表';


-- ----------------------------
-- View structure for ibv_user_roles
-- ----------------------------
DROP VIEW
IF
	EXISTS ibv_user_roles;
CREATE VIEW ibv_user_roles AS SELECT
r.tenant_id,
m.role_id,
m.member_id AS user_id,
NULL AS dept_id 
FROM
	ibr_role r
	JOIN ibr_role_member m ON m.role_id = r.id 
WHERE
	type = 1 UNION
SELECT
	r.tenant_id,
	m.role_id,
	g.user_id,
	NULL AS dept_id 
FROM
	ibr_role r
	JOIN ibr_role_member m ON m.role_id = r.id
	JOIN ibu_group_member g ON g.group_id = m.member_id 
	AND m.type = 2 UNION
SELECT
	r.tenant_id,
	m.role_id,
	p.user_id,
	o.parent_id AS dept_id 
FROM
	ibr_role r
	JOIN ibr_role_member m ON m.role_id = r.id
	JOIN ibo_organize_member p ON p.post_id = m.member_id
	JOIN ibo_organize o ON o.id = p.post_id 
	AND m.type = 3;

-- ----------------------------
-- View structure for ibv_user_permit
-- ----------------------------
DROP VIEW
IF
	EXISTS ibv_user_permit;
CREATE VIEW ibv_user_permit AS SELECT
	f.id,
	f.nav_id,
	n.app_id,
	r.tenant_id,
	r.user_id,
	f.auth_code,
	min(p.permit) AS permit 
FROM
	ibs_function f
	JOIN ibs_navigator n ON n.id = f.nav_id
	JOIN ibr_role_func_permit p ON p.function_id = f.id
	JOIN ibv_user_roles r ON r.role_id = p.role_id 
GROUP BY
	f.id,
	f.nav_id,
	n.app_id,
	r.tenant_id,
	r.user_id,
	f.auth_code;

-- ----------------------------
-- 初始化基础数据:数据权限定义
-- ----------------------------
INSERT `ibr_config` VALUES 
('a2e67d9878b011e8bad87cd30aeb75e4', 0, '无归属'),
('a2e67dd478b011e8bad87cd30aeb75e4', 1, '本人'),
('a2e67deb78b011e8bad87cd30aeb75e4', 2, '本部门'),
('a2e67dfb78b011e8bad87cd30aeb75e4', 3, '部门所有'),
('a2e67e0778b011e8bad87cd30aeb75e4', 4, '机构所有');

-- ----------------------------
-- 初始化用户:系统管理员
-- ----------------------------
INSERT ibu_user (`id`, `name`, `account`, `is_builtin`, `creator`, `creator_id`) VALUES
('00000000000000000000000000000000', '系统管理员', 'admin', 1, '系统管理员', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化租户:因赛特软件
-- ----------------------------
INSERT ibt_tenant (`id`, `code`, `name`, `expire_date`, `status`, `auditor`, `auditor_id`, `audited_time`, `creator`, `creator_id`) VALUES 
('2564cd559cd340f0b81409723fd8632a', 'TI-001', '因赛特软件', '2800-01-01', 1, '系统管理员', '00000000000000000000000000000000', now(), '系统管理员', '00000000000000000000000000000000');
INSERT ibt_tenant_user (`id`, `tenant_id`, `user_id`) VALUES 
(replace(uuid(), '-', ''), '2564cd559cd340f0b81409723fd8632a', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化组织机构:因赛特软件
-- ----------------------------
INSERT ibo_organize (`id`, `tenant_id`, `type`, `index`, `code`, `name`, `alias`, `full_name`, `creator`, `creator_id`) VALUES 
('2564cd559cd340f0b81409723fd8632a', '2564cd559cd340f0b81409723fd8632a', 1, 0, 'TI-001', '因赛特软件有限公司', '因赛特', '因赛特软件', '系统管理员', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化应用:平台管理客户端
-- ----------------------------
INSERT ibs_application (`id`, `index`, `name`, `alias`, `token_life`, `creator`, `creator_id`) VALUES
('9dd99dd9e6df467a8207d05ea5581125', 1, '因赛特多租户平台', 'MTP', 7200, '系统管理员', '00000000000000000000000000000000'),
('e46c0d4f85f24f759ad4d86b9505b1d4', 2, '因赛特用户管理系统', 'RMS', 7200, '系统管理员', '00000000000000000000000000000000');
INSERT ibt_tenant_app (`id`, `tenant_id`, `app_id`) VALUES
(replace(uuid(), '-', ''), '2564cd559cd340f0b81409723fd8632a', '9dd99dd9e6df467a8207d05ea5581125'),
(replace(uuid(), '-', ''), '2564cd559cd340f0b81409723fd8632a', 'e46c0d4f85f24f759ad4d86b9505b1d4');

-- ----------------------------
-- 初始化应用:功能导航
-- ----------------------------
INSERT ibs_navigator(`id`, `parent_id`, `app_id`, `type`, `index`, `name`, `module_info`, `creator`, `creator_id`) VALUES
('8c95d6e097f340d6a8d93a3b5631ba39', null, '9dd99dd9e6df467a8207d05ea5581125', 1, 1, '运营中心', json_object("icon", null), '系统管理员', '00000000000000000000000000000000'),
('711aad8daf654bcdb3a126d70191c15c', '8c95d6e097f340d6a8d93a3b5631ba39', '9dd99dd9e6df467a8207d05ea5581125', 2, 1, '租户管理', json_object("module", 'Tenants', "file", 'Base.dll', "default", true, "icon", null), '系统管理员', '00000000000000000000000000000000'),
('a65a562582bb489ea729bb0838bbeff8', '8c95d6e097f340d6a8d93a3b5631ba39', '9dd99dd9e6df467a8207d05ea5581125', 2, 2, '应用管理', json_object("module", 'Apps', "file", 'Base.dll', "default", false, "icon", null), '系统管理员', '00000000000000000000000000000000'),
('5e4a994ccd2611e9bbd40242ac110008', null, '9dd99dd9e6df467a8207d05ea5581125', 1, 2, '系统设置', json_object("icon", null), '系统管理员', '00000000000000000000000000000000'),
('d6254874cd2611e9bbd40242ac110008', '5e4a994ccd2611e9bbd40242ac110008', '9dd99dd9e6df467a8207d05ea5581125', 2, 1, '接口管理', json_object("module", 'Tenants', "file", 'Base.dll', "default", true, "icon", null), '系统管理员', '00000000000000000000000000000000'),
('4b3ac9336dd8496597e603fc7e8f5140', null, 'e46c0d4f85f24f759ad4d86b9505b1d4', 1, 2, '系统设置', json_object("icon", null), '系统管理员', '00000000000000000000000000000000'),
('100ff6e2748f493586ea4e4cd3f7a4b1', '4b3ac9336dd8496597e603fc7e8f5140', 'e46c0d4f85f24f759ad4d86b9505b1d4', 2, 1, '组织机构', json_object("module", 'Organizes', "file", 'Base.dll', "default", false, "icon", null), '系统管理员', '00000000000000000000000000000000'),
('cdf0ffb178b741b287d1f155d0165112', '4b3ac9336dd8496597e603fc7e8f5140', 'e46c0d4f85f24f759ad4d86b9505b1d4', 2, 2, '用户', json_object("module", 'Users', "file", 'Base.dll', "default", false, "icon", null), '系统管理员', '00000000000000000000000000000000'),
('b13a3593c4ec4d2fb9432045846f7ff9', '4b3ac9336dd8496597e603fc7e8f5140', 'e46c0d4f85f24f759ad4d86b9505b1d4', 2, 3, '用户组', json_object("module", 'Groups', "file", 'Base.dll', "default", false, "icon", null), '系统管理员', '00000000000000000000000000000000'),
('0e74cbb3f9d44bddbd3be3cc702d2a82', '4b3ac9336dd8496597e603fc7e8f5140', 'e46c0d4f85f24f759ad4d86b9505b1d4', 2, 4, '角色权限', json_object("module", 'Roles', "file", 'Base.dll', "default", false, "icon", null), '系统管理员', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化应用:系统功能
-- ----------------------------
INSERT ibs_function(`id`, `nav_id`, `type`, `index`, `name`, `auth_code`, `icon_info`, `creator`, `creator_id`) VALUES
(replace(uuid(), '-', ''), '711aad8daf654bcdb3a126d70191c15c', 0, 1, '刷新', 'getTenant', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '711aad8daf654bcdb3a126d70191c15c', 0, 2, '新增租户', 'newTenant', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '711aad8daf654bcdb3a126d70191c15c', 1, 3, '编辑', 'editTenant', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '711aad8daf654bcdb3a126d70191c15c', 1, 4, '删除', 'deleteTenant', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '711aad8daf654bcdb3a126d70191c15c', 1, 5, '绑定应用', 'bindApp', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '711aad8daf654bcdb3a126d70191c15c', 1, 6, '续租', 'extend', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),

(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 0, 1, '刷新', 'getApp', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 0, 2, '新增应用', 'newApp', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 3, '编辑', 'editApp', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 4, '删除', 'deleteApp', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 5, '新增导航', 'newNav', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 6, '编辑', 'editNav', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 7, '删除', 'deleteNav', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 8, '新增功能', 'newFun', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 9, '编辑', 'editFun', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'a65a562582bb489ea729bb0838bbeff8', 1, 10, '删除', 'deleteFun', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),

(replace(uuid(), '-', ''), 'd6254874cd2611e9bbd40242ac110008', 0, 1, '刷新', 'getConfig', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'd6254874cd2611e9bbd40242ac110008', 0, 2, '新增', 'newConfig', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'd6254874cd2611e9bbd40242ac110008', 1, 3, '编辑', 'editConfig', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'd6254874cd2611e9bbd40242ac110008', 1, 4, '删除', 'deleteConfig', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'd6254874cd2611e9bbd40242ac110008', 1, 5, '查询日志', 'getLog', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'd6254874cd2611e9bbd40242ac110008', 1, 6, '加载配置', 'loadConfigs', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),

(replace(uuid(), '-', ''), '100ff6e2748f493586ea4e4cd3f7a4b1', 0, 1, '刷新', 'getOrganize', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '100ff6e2748f493586ea4e4cd3f7a4b1', 0, 2, '新增', 'newOrganize', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '100ff6e2748f493586ea4e4cd3f7a4b1', 1, 3, '编辑', 'editOrganize', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '100ff6e2748f493586ea4e4cd3f7a4b1', 1, 4, '删除', 'deleteOrganize', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '100ff6e2748f493586ea4e4cd3f7a4b1', 1, 5, '添加成员', 'addOrganizeMember', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '100ff6e2748f493586ea4e4cd3f7a4b1', 1, 6, '移除成员', 'removeOrganizeMember', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),

(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 0, 1, '刷新', 'getUser', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 0, 2, '新增', 'newUser', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 1, 3, '编辑', 'editUser', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 1, 4, '删除', 'deleteUser', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 1, 5, '封禁', 'bannedUser', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 1, 6, '解封', 'releaseUser', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 1, 7, '重置密码', 'resetPassword', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'cdf0ffb178b741b287d1f155d0165112', 1, 7, '邀请用户', 'inviteUser', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),

(replace(uuid(), '-', ''), 'b13a3593c4ec4d2fb9432045846f7ff9', 0, 1, '刷新', 'getGroup', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'b13a3593c4ec4d2fb9432045846f7ff9', 0, 2, '新增用户组', 'newGroup', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'b13a3593c4ec4d2fb9432045846f7ff9', 1, 3, '编辑', 'editGroup', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'b13a3593c4ec4d2fb9432045846f7ff9', 1, 4, '删除', 'deleteGroup', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'b13a3593c4ec4d2fb9432045846f7ff9', 1, 5, '添加成员', 'addGroupMember', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), 'b13a3593c4ec4d2fb9432045846f7ff9', 1, 6, '移除成员', 'removeGroupMember', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),

(replace(uuid(), '-', ''), '0e74cbb3f9d44bddbd3be3cc702d2a82', 0, 1, '刷新', 'getRole', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", true), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '0e74cbb3f9d44bddbd3be3cc702d2a82', 0, 2, '新增', 'newRole,getApp', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '0e74cbb3f9d44bddbd3be3cc702d2a82', 1, 3, '编辑', 'editRole,getApp', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '0e74cbb3f9d44bddbd3be3cc702d2a82', 1, 4, '删除', 'deleteRole', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '0e74cbb3f9d44bddbd3be3cc702d2a82', 1, 5, '添加成员', 'addRoleMember', json_object("icon", null, "iconUrl", null, "beginGroup", true, "hideText", false), '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '0e74cbb3f9d44bddbd3be3cc702d2a82', 1, 6, '移除成员', 'removeRoleMember', json_object("icon", null, "iconUrl", null, "beginGroup", false, "hideText", false), '系统管理员', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化角色:系统管理员
-- ----------------------------
insert ibr_role (id, tenant_id, app_id, name, remark, is_builtin, creator, creator_id) VALUES
(replace(uuid(), '-', ''), '2564cd559cd340f0b81409723fd8632a', 'e46c0d4f85f24f759ad4d86b9505b1d4', '系统管理员', '内置角色，角色成员为系统管理员组成员', 1, '系统管理员', '00000000000000000000000000000000'),
(replace(uuid(), '-', ''), '2564cd559cd340f0b81409723fd8632a', '9dd99dd9e6df467a8207d05ea5581125', '平台管理员', '内置角色，角色成员为系统管理员组成员', 1, '系统管理员', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化用户组:系统管理员
-- ----------------------------
INSERT `ibu_group`(`id`, `tenant_id`, `name`, `remark`, `is_builtin`, `creator`, `creator_id`) VALUES 
(replace(uuid(), '-', ''), '2564cd559cd340f0b81409723fd8632a', '系统管理员', '内置用户组,系统管理员组', b'1', '系统管理员', '00000000000000000000000000000000');

-- ----------------------------
-- 初始化用户组成员:系统管理员
-- ----------------------------
INSERT `ibu_group_member`(`id`, `group_id`, `user_id`) 
select replace(uuid(), '-', ''), g.id, u.id
from ibu_user u, ibu_group g;

-- ----------------------------
-- 初始化角色成员
-- ----------------------------
ALTER TABLE `ibr_role_member` 
MODIFY COLUMN `id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键(UUID)' FIRST;
insert ibr_role_member(id, `type`, role_id, member_id)
select uuid(), 2, r.id, g.id
from ibr_role r, ibu_group g;
update ibr_role_member set id = replace(id, '-', '');
ALTER TABLE `ibr_role_member` 
MODIFY COLUMN `id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键(UUID)' FIRST;

-- ----------------------------
-- 初始化功能权限
-- ---------------------------- 
ALTER TABLE `ibr_role_func_permit` 
MODIFY COLUMN `id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键(UUID)' FIRST;
INSERT `ibr_role_func_permit`(`id`, `role_id`, `function_id`, `permit`) 
select uuid(), r.id, f.id, 1
from ibr_role r
join ibs_navigator n on n.app_id = r.app_id
join ibs_function f on f.nav_id = n.id;
update ibr_role_func_permit set id = replace(id, '-', '');
ALTER TABLE `ibr_role_func_permit` 
MODIFY COLUMN `id` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键(UUID)' FIRST;

-- ----------------------------
-- 初始化接口配置
-- ---------------------------- 
INSERT `ibi_interface`(`id`, `name`, `method`, `url`, `auth_code`, `limit_gap`, `limit_cycle`, `limit_max`, `message`, `is_verify`, `is_limit`) VALUES 
(replace(uuid(), '-', ''), '获取Code', 'GET', '/base/auth/v1.0/tokens/codes', NULL, 1, 86400, 360, '获取Code接口每24小时调用次数为360次,请合理使用', 0, 1),
(replace(uuid(), '-', ''), '获取Token', 'POST', '/base/auth/v1.0/tokens', NULL, 1, 86400, 360, '获取Token接口每24小时调用次数为360次,请合理使用', 0, 1),
(replace(uuid(), '-', ''), '通过微信授权码获取Token', 'POST', '/base/auth/v1.0/tokens/withWechatCode', NULL, 1, 86400, 360, '获取Token接口每24小时调用次数为360次,请合理使用', 0, 1),
(replace(uuid(), '-', ''), '通过微信UnionId获取Token', 'POST', '/base/auth/v1.0/tokens/withWechatUnionId', NULL, 1, 86400, 360, '获取Token接口每24小时调用次数为360次,请合理使用', 0, 1),
(replace(uuid(), '-', ''), '验证Token', 'GET', '/base/auth/v1.0/tokens/status', NULL, NULL, NULL, NULL, NULL, 1, 0),
(replace(uuid(), '-', ''), '刷新Token', 'PUT', '/base/auth/v1.0/tokens', NULL, 10, 3600, 10, '刷新Token接口每小时调用次数为10次,请合理使用', 0, 1),
(replace(uuid(), '-', ''), '用户账号离线', 'DELETE', '/base/auth/v1.0/tokens', NULL, 10, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '获取用户导航栏', 'GET', '/base/auth/v1.0/navigators', NULL, 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '获取模块功能', 'GET', '/base/auth/v1.0/navigators/{id}/functions', NULL, 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '获取接口配置列表', 'GET', '/base/auth/manage/v1.0/configs', 'getConfig', 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '获取接口配置详情', 'GET', '/base/auth/manage/v1.0/configs/{id}', 'getConfig', 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '新增接口配置', 'POST', '/base/auth/manage/v1.0/configs', 'newConfig', 10, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '编辑接口配置', 'PUT', '/base/auth/manage/v1.0/configs', 'editConfig', 10, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '删除接口配置', 'DELETE', '/base/auth/manage/v1.0/configs', 'deleteConfig', 10, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '获取日志列表', 'GET', '/base/auth/manage/v1.0/configs/logs', 'getLog', 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '获取日志详情', 'GET', '/base/auth/manage/v1.0/configs/logs/{id}', 'getLog', 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '加载接口配置表', 'GET', '/base/auth/manage/v1.0/configs/load', 'loadConfigs', 1, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '发送短信', 'POST', '/base/message/sms/v1.0/messages', 'sendMessage', 10, NULL, NULL, NULL, 1, 1),
(replace(uuid(), '-', ''), '发送短信验证码', 'POST', '/base/message/sms/v1.0/messages/codes', NULL, 10, 86400, 30, '今日验证码次数已达上限,请合理使用短信验证码', 0, 1),
(replace(uuid(), '-', ''), '验证短信验证码', 'GET', '/base/message/sms/v1.0/messages/codes/{key}/status', NULL, 1, NULL, NULL, NULL, 0, 1);
