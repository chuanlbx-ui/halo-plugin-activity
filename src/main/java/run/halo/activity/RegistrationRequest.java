package run.halo.activity;

/**
 * 前台报名请求体。
 *
 * @param name   报名人姓名
 * @param phone  报名人手机号
 * @param remark 报名备注
 * @author halo-plugin-activity
 */
public record RegistrationRequest(String name, String phone, String remark) {
}
