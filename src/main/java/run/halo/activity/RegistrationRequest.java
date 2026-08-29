package run.halo.activity;

import java.util.Map;

/**
 * 前台报名请求体。
 *
 * @param name         报名人姓名
 * @param phone        报名人手机号
 * @param remark       报名备注
 * @param customFields 自定义字段值（键 = 字段 name，值 = 填写内容）
 * @author halo-plugin-activity
 */
public record RegistrationRequest(String name, String phone, String remark,
                                  Map<String, String> customFields) {
}
