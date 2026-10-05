package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.util.List;

/**
 * 直接查询数据库验证 agent 表的 tools_json 是否有数据。
 * 运行此类的 main 方法即可看到结果。
 */
public class ToolQueryTest {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/agentx?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // 1. 先看表结构：是否有 tools_json 列
            System.out.println("===== 检查表结构 =====");
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "agent", "tools_json");
            if (columns.next()) {
                System.out.println("找到 tools_json 列，类型：" + columns.getString("TYPE_NAME"));
            } else {
                System.out.println("❌ 没有找到 tools_json 列！需要先执行 ALTER TABLE");
                return;
            }

            // 2. 查询所有 Agent 的 tools_json
            System.out.println("\n===== 查询 Agent 的 tools_json =====");
            Gson gson = new Gson();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, tools_json FROM agent")) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String toolsJson = rs.getString("tools_json");

                    System.out.println("\nAgent: " + name + " (id=" + id + ")");
                    System.out.println("  tools_json 原始值: " + toolsJson);

                    if (toolsJson != null && !toolsJson.isEmpty()) {
                        List<String> tools = gson.fromJson(toolsJson, new TypeToken<List<String>>() {}.getType());
                        System.out.println("  解析后工具列表: " + tools);
                    } else {
                        System.out.println("  ❌ tools_json 为空！");
                    }
                }
            }
        }
    }
}
