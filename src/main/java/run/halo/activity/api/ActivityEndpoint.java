package run.halo.activity.api;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.activity.Activity;
import run.halo.activity.ActivityException;
import run.halo.activity.ActivityRegistration;
import run.halo.activity.CheckinRequest;
import run.halo.activity.RegistrationRequest;
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.core.extension.endpoint.CustomEndpoint;

/**
 * 前台 API：活动列表、活动详情、活动报名。
 *
 * @author halo-plugin-activity
 */
@Component
@AllArgsConstructor
public class ActivityEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "api.activity.halo.run/v1alpha1/Activity";
        return SpringdocRouteBuilder.route()
            .GET("activities", this::listActivities, builder -> builder
                .operationId("ListActivities")
                .description("List activities.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("page")
                    .in(ParameterIn.QUERY)
                    .description("Page number, starts from 1.")
                    .implementation(Integer.class)
                    .required(false))
                .parameter(parameterBuilder()
                    .name("size")
                    .in(ParameterIn.QUERY)
                    .description("Page size.")
                    .implementation(Integer.class)
                    .required(false))
                .parameter(parameterBuilder()
                    .name("status")
                    .in(ParameterIn.QUERY)
                    .description("Filter by activity status, e.g. PUBLISHED.")
                    .implementation(String.class)
                    .required(false))
                .response(responseBuilder()
                    .implementation(ListResult.generateGenericClass(Activity.class)))
            )
            .GET("activities/{name}", this::getActivity, builder -> builder
                .operationId("GetActivity")
                .description("Get an activity by name.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .description("Activity name.")
                    .required(true)
                    .implementation(String.class))
                .response(responseBuilder()
                    .implementation(Activity.class))
            )
            .POST("activities/{name}/registration", this::createRegistration, builder -> builder
                .operationId("CreateRegistration")
                .description("Register for an activity.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .description("Activity name.")
                    .required(true)
                    .implementation(String.class))
                .requestBody(requestBodyBuilder()
                    .required(true)
                    .content(contentBuilder()
                        .mediaType(MediaType.APPLICATION_JSON_VALUE)
                        .schema(schemaBuilder()
                            .implementation(RegistrationRequest.class))))
                .response(responseBuilder()
                    .implementation(ActivityRegistration.class))
            )
            .POST("activities/{name}/checkin", this::checkin, builder -> builder
                .operationId("CheckinActivity")
                .description("Check in with phone number.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .description("Activity name.")
                    .required(true)
                    .implementation(String.class))
                .requestBody(requestBodyBuilder()
                    .required(true)
                    .content(contentBuilder()
                        .mediaType(MediaType.APPLICATION_JSON_VALUE)
                        .schema(schemaBuilder()
                            .implementation(CheckinRequest.class))))
                .response(responseBuilder()
                    .implementation(ActivityRegistration.class))
            )
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.activity.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> listActivities(ServerRequest request) {
        int page = parsePositiveInt(request.queryParam("page").orElse("1"), 1);
        int size = parsePositiveInt(request.queryParam("size").orElse("10"), 10);
        String status = request.queryParam("status").orElse(null);
        Predicate<Activity> filter = activity -> status == null || status.isBlank()
            || status.equals(activity.getSpec() == null ? null : activity.getSpec().getStatus());
        return listByFilter(Activity.class, filter,
            Comparator.comparing(a -> a.getSpec() == null || a.getSpec().getStartTime() == null
                ? Instant.EPOCH : a.getSpec().getStartTime(), Comparator.reverseOrder()),
            page, size)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getActivity(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(Activity.class, name)
            .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
            .flatMap(activity -> ServerResponse.ok().bodyValue(activity))
            .onErrorResume(ActivityException.class,
                e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> createRegistration(ServerRequest request) {
        String activityName = request.pathVariable("name");
        return request.bodyToMono(RegistrationRequest.class)
            .switchIfEmpty(Mono.error(new ActivityException("请求体不能为空")))
            .flatMap(req -> register(activityName, req))
            .flatMap(registration -> ServerResponse.ok().bodyValue(registration))
            .onErrorResume(ActivityException.class,
                e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> checkin(ServerRequest request) {
        String activityName = request.pathVariable("name");
        return request.bodyToMono(CheckinRequest.class)
            .switchIfEmpty(Mono.error(new ActivityException("请求体不能为空")))
            .flatMap(req -> doCheckin(activityName, req))
            .flatMap(registration -> ServerResponse.ok().bodyValue(registration))
            .onErrorResume(ActivityException.class,
                e -> badRequest(e.getMessage()));
    }

    private Mono<ActivityRegistration> doCheckin(String activityName, CheckinRequest req) {
        if (req.phone() == null || req.phone().isBlank()) {
            return Mono.error(new ActivityException("手机号不能为空"));
        }
        return client.fetch(Activity.class, activityName)
            .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
            .flatMap(activity -> client.listAll(ActivityRegistration.class,
                    ListOptions.builder().build(),
                    org.springframework.data.domain.Sort.unsorted())
                .filter(r -> activityName.equals(r.getSpec().getActivityName()))
                .filter(r -> req.phone().equals(r.getSpec().getPhone()))
                .next()
                .switchIfEmpty(Mono.error(new ActivityException("未找到该手机号的报名记录，请先报名")))
                .flatMap(registration -> {
                    var spec = registration.getSpec();
                    if (Boolean.TRUE.equals(spec.getCheckedIn())) {
                        return Mono.error(new ActivityException("该手机号已签到，请勿重复签到"));
                    }
                    spec.setCheckedIn(true);
                    spec.setCheckedInAt(Instant.now());
                    return client.update(registration);
                }));
    }

    private Mono<ActivityRegistration> register(String activityName, RegistrationRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            return Mono.error(new ActivityException("姓名不能为空"));
        }
        if (req.phone() == null || req.phone().isBlank()) {
            return Mono.error(new ActivityException("手机号不能为空"));
        }
        return client.fetch(Activity.class, activityName)
            .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
            .flatMap(activity -> {
                var spec = activity.getSpec();
                if (spec == null) {
                    return Mono.error(new ActivityException("活动配置不完整"));
                }
                String status = spec.getStatus();
                if ("ENDED".equals(status)) {
                    return Mono.error(new ActivityException("活动已结束，无法报名"));
                }
                if (spec.getRegistrationDeadline() != null
                    && Instant.now().isAfter(spec.getRegistrationDeadline())) {
                    return Mono.error(new ActivityException("报名已截止"));
                }
                return listRegistrationsOf(activityName)
                    .flatMap(registrations -> {
                        boolean duplicated = registrations.stream()
                            .anyMatch(r -> req.phone().equals(r.getSpec().getPhone()));
                        if (duplicated) {
                            return Mono.error(new ActivityException("该手机号已报名，请勿重复提交"));
                        }
                        int quota = spec.getQuota() == null ? 0 : spec.getQuota();
                        if (quota > 0 && registrations.size() >= quota) {
                            if (!"FULL".equals(status)) {
                                spec.setStatus("FULL");
                                return client.update(activity)
                                    .then(Mono.error(
                                        new ActivityException("活动名额已满，无法报名")));
                            }
                            return Mono.error(new ActivityException("活动名额已满，无法报名"));
                        }
                        return createRegistration(activityName, req);
                    });
            });
    }

    private Mono<ActivityRegistration> createRegistration(String activityName,
        RegistrationRequest req) {
        ActivityRegistration registration = new ActivityRegistration();
        Metadata metadata = new Metadata();
        metadata.setName(sanitizeName(activityName) + "-" + sanitizeName(req.phone()));
        registration.setMetadata(metadata);
        ActivityRegistration.ActivityRegistrationSpec spec =
            new ActivityRegistration.ActivityRegistrationSpec();
        spec.setActivityName(activityName);
        spec.setName(req.name());
        spec.setPhone(req.phone());
        spec.setRemark(req.remark());
        spec.setRegistrationTime(Instant.now());
        spec.setStatus("APPROVED");
        registration.setSpec(spec);
        return client.create(registration);
    }

    private Mono<List<ActivityRegistration>> listRegistrationsOf(String activityName) {
        return client.listAll(ActivityRegistration.class, ListOptions.builder().build(),
                org.springframework.data.domain.Sort.unsorted())
            .filter(r -> activityName.equals(r.getSpec().getActivityName()))
            .collectList();
    }

    private <E extends Extension> Mono<ListResult<E>> listByFilter(Class<E> clazz,
        Predicate<E> filter, Comparator<E> comparator, int page, int size) {
        return client.listAll(clazz, ListOptions.builder().build(),
                org.springframework.data.domain.Sort.unsorted())
            .filter(filter)
            .collectSortedList(comparator)
            .map(list -> {
                int total = list.size();
                int from = Math.min((page - 1) * size, total);
                int to = Math.min(page * size, total);
                return new ListResult<>(page, size, total, ListResult.subList(list, from, to));
            });
    }

    private Mono<ServerResponse> badRequest(String message) {
        return ServerResponse.status(HttpStatus.BAD_REQUEST)
            .bodyValue(Map.of("message", message));
    }

    private static int parsePositiveInt(String value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String sanitizeName(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9-]", "-");
    }
}
