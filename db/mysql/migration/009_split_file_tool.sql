use `agentx`;

-- 文件工具拆分为 file-read（列目录、读文件）与 file-write（创建、追加）两个工具 ID。
-- 旧的 "file" 同时具备读写能力，这里等价展开成两个新 ID，避免升级后能力静默丢失。
UPDATE agent
SET tools_json = REPLACE(tools_json, '"file"', '"file-read", "file-write"')
WHERE tools_json LIKE '%"file"%'
  AND tools_json NOT LIKE '%"file-read"%'
  AND tools_json NOT LIKE '%"file-write"%';

-- 能力模式自带的默认工具由代码维护（ToolRegistry.defaultToolIds），此处无需改数据：
-- CODE 默认挂 file-read + file-write，CODE_READONLY 默认只挂 file-read。
