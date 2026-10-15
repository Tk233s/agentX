package org.example.infrastructure.tool;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个可被 Agent 绑定的工具 Bean。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface AgentTool {

    /**
     * Agent 绑定该工具时使用的稳定 ID。
     */
    String value();

    /**
     * 面向用户的工具名称。
     */
    String name() default "";

    /**
     * 工具的整体说明；为空时由方法说明汇总生成。
     */
    String description() default "";

    /**
     * 该工具是否会产生副作用（写文件、修改外部状态等）。
     * 只读会话不会挂载这类工具，因此工具自己声明副作用，权限侧才能可靠地收窄。
     */
    boolean mutating() default false;
}
