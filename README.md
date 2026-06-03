# CampusHub（青隅）— 校园互助服务平台

南京大学智能软件与工程学校课程软工二大作业，面向高校大学生的校园互助社交平台，提供树洞倾诉、找搭子、恋爱互助三大核心服务。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + Spring Security + JPA + JWT |
| 前端 | Vue 3 + Vite + Pinia + Vue Router |
| 数据库 | H2 (开发) / MySQL 8.0 (生产) |

## 快速启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:3000`

### 演示账号

| 角色 | 手机号 | 密码 |
|------|--------|------|
| 普通用户1 | 13800138001 | Abc12345 |
| 普通用户2 | 13800138002 | Abc12345 |
| 管理员 | 13800138000 | Admin123 |

## 项目文档

| 阶段 | 内容 | 路径 |
|------|------|------|
| P0 | 项目启动、团队章程、AI工具选型 | `docs/P0/` |
| P1 | 需求分析、用户故事、用例描述 | `docs/P1/` |
| P2 | 架构设计、ADR、模块划分 | `docs/P2/` |
| P3 | 类图设计、API规范、ER图与建表SQL | `docs/P3/` |
| P4 | 编码开发、测试、CI/CD、实验报告 | `docs/P4/` |

详细操作指南请参见 [操作说明指南.md](操作说明指南.md)
