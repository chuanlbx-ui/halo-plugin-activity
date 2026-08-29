package run.halo.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * 活动报名记录自定义模型。
 *
 * @author halo-plugin-activity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "activity.halo.run", version = "v1alpha1", kind = "ActivityRegistration",
    plural = "registrations", singular = "registration")
public class ActivityRegistration extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ActivityRegistrationSpec spec;

    @Data
    public static class ActivityRegistrationSpec {

        /**
         * 关联的活动名称（Activity 的 metadata.name）。
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String activityName;

        /**
         * 报名人姓名。
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        /**
         * 报名人手机号（同一活动内唯一）。
         */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String phone;

        /**
         * 报名备注。
         */
        private String remark;

        /**
         * 报名时间。
         */
        private Instant registrationTime;

        /**
         * 报名状态：PENDING / APPROVED / REJECTED / CANCELLED。
         */
        private String status;

        /**
         * 是否已签到。
         */
        private Boolean checkedIn;

        /**
         * 签到时间。
         */
        private Instant checkedInAt;
    }
}
