package run.halo.activity;

/**
 * 活动业务异常，用于报名等场景的校验失败提示。
 *
 * @author halo-plugin-activity
 */
public class ActivityException extends RuntimeException {

    public ActivityException(String message) {
        super(message);
    }
}
