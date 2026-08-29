package run.halo.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * 活动自定义模型。
 *
 * @author halo-plugin-activity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "activity.halo.run", version = "v1alpha1", kind = "Activity",
    plural = "activities", singular = "activity")
public class Activity extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ActivitySpec spec;

    @Data
    public static class ActivitySpec {

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;

        /**
         * 封面图 URL。
         */
        private String cover;

        /**
         * 活动开始时间。
         */
        private Instant startTime;

        /**
         * 活动结束时间。
         */
        private Instant endTime;

        /**
         * 活动地点。
         */
        private String location;

        /**
         * 报名名额，小于等于 0 表示不限名额。
         */
        private Integer quota;

        /**
         * 报名截止时间。
         */
        private Instant registrationDeadline;

        /**
         * 活动状态：DRAFT / PUBLISHED / ENDED / FULL。
         */
        private String status;

        /**
         * 富文本活动内容（HTML）。
         */
        private String content;

        /**
         * 报名表单自定义字段配置（可选）。
         * 默认内置字段：姓名、手机号；额外字段通过此配置动态添加。
         */
        private List<FormField> formFields;

        @Data
        public static class FormField {

            /**
             * 字段唯一标识（英文，如 company）。
             */
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            private String name;

            /**
             * 字段显示标签（如 公司名称）。
             */
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            private String label;

            /**
             * 字段类型：text / textarea / select / number。
             */
            private String type;

            /**
             * 是否必填。
             */
            private Boolean required;

            /**
             * select 类型的选项（逗号分隔）。
             */
            private String options;

            /**
             * 占位提示。
             */
            private String placeholder;
        }
    }
}
