# Cloud Storage printer compatibility fork

- 基于 water2004/litematica-printer v1.1.5（80b2），保留 AGPL-3.0 与上游署名。
- 当前只改 `versions/1.21.1`；通过 Cloud-Storage 的 pixi Java 21 环境执行 Gradle。
- 修改前检查 Git 状态。禁止改只读参考克隆、正式服务器或旧客户端实例。
- 云潜影盒为服务器目录，不是真实库存。只请求材料并使用正常容器点击；不得直接创造客户端物品、自动丢弃物品或跳过服务端确认。
- 按需目录仅对版本 3 且 UUID 匹配的 CloudStorage 标记启用；普通潜影盒路径保留。
- 满背包只回填没有自定义组件的普通建筑方块，保护装备、工具、自定义物品及所需材料。
- 发布 JAR 不包含 smokeTest；修改后编译并用隔离 Paper + Fabric 实例验证取料和回填。
- 不提交或推送，除非用户明确要求。
