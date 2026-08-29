package run.halo.activity;

/**
 * 前台签到请求体。
 *
 * @param phone 签到手机号
 * @author halo-plugin-activity
 */
public record CheckinRequest(String phone) {
}
