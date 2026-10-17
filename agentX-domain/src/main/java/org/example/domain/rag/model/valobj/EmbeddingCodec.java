package org.example.domain.rag.model.valobj;

import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

/**
 * 向量文本编解码。
 *
 * 落库形态是 JSON 数组文本（形如 [0.1,-0.2,...]），
 * 这里不用 JSON 库而是手写，一是 domain 模块没有 JSON 依赖，二是 Float.toString 能保证往返精度。
 */
public final class EmbeddingCodec {

    private EmbeddingCodec() {
    }

    public static String encode(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量不能为空");
        }
        StringBuilder builder = new StringBuilder(vector.length * 12);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(vector[i]));
        }
        builder.append(']');
        return builder.toString();
    }

    public static float[] decode(String text) {
        if (text == null || text.isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量文本不能为空");
        }
        String body = text.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量文本格式不正确");
        }
        String[] parts = body.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量文本包含非法数值");
            }
        }
        return vector;
    }
}
