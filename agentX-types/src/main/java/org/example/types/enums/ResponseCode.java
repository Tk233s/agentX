package org.example.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ResponseCode {

    SUCCESS("0000", "成功"),
    UN_ERROR("0001", "未知失败"),
    ILLEGAL_PARAMETER("0002", "非法参数"),
    UNAUTHORIZED("401", "未登录或登录已过期"),
    FORBIDDEN("403", "无权访问该资源"),
    ;

    private String code;
    private String info;

}
