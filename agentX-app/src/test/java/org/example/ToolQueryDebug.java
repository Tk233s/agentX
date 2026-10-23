package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.util.List;

/**
 * 手动排查脚本：直接查数据库确认 agent 表的 tools_json 是否有数据。
 *
 * <p>它不是自动化测试——没有 {@code @Test} 方法，只提供 {@code main}，
 * 所以刻意不带 Test 后缀，避免被 surefire 当成测试类扫进去。
 * 需要排查时用 IDE 直接运行本类的 main 方法。</p>
 *
 * <p>数据库口令从环境变量 {@code MYSQL_PASSWORD} 读取，不写死在代码里。</p>
 */
public class ToolQueryDebug {

    private static final String URL = "jdbc:mysql://127.0.0.1:3306/agentx"
            + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = System.getenv().getOrDefault("MYSQL_PASSWORD", "");

    public static void main(String[] args) throws Exception {
        if (PASSWORD.isEmpty()) {
            System.out.println("请先设置环境变量 MYSQL_PASSWORD 再运行");
            return;
        }
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("===== 检查表结构 =====");
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "agent", "tools_json");
            if (columns.next()) {
                System.out.println("找到 tools_json 列，类型：" + columns.getString("TYPE_NAME"));
            } else {
                System.out.println("没有找到 tools_json 列！需要先执行 ALTER TABLE");
                return;
            }

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
                        List<String> tools = gson.fromJson(toolsJson, new TypeToken<List<String>>() {
                        }.getType());
                        System.out.println("  解析后工具列表: " + tools);
                    } else {
                        System.out.println("  tools_json 为空！");
                    }
                }
            }
        }
    }
}
