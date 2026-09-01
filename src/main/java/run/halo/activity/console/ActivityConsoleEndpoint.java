package run.halo.activity.console;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
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
import run.halo.app.extension.Extension;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.core.extension.endpoint.CustomEndpoint;

/**
 * 控制台 API：活动 CRUD 与报名记录列表。
 *
 * @author halo-plugin-activity
 */
@Component
@AllArgsConstructor
public class ActivityConsoleEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = "console.api.activity.halo.run/v1alpha1/Activity";
        return SpringdocRouteBuilder.route()
            .GET("activities", this::listActivities, builder -> builder
                .operationId("ConsoleListActivities")
                .description("List activities for console.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("page")
                    .in(ParameterIn.QUERY)
                    .implementation(Integer.class)
                    .required(false))
                .parameter(parameterBuilder()
                    .name("size")
                    .in(ParameterIn.QUERY)
                    .implementation(Integer.class)
                    .required(false))
                .parameter(parameterBuilder()
                    .name("keyword")
                    .in(ParameterIn.QUERY)
                    .implementation(String.class)
                    .required(false))
                .response(responseBuilder()
                    .implementation(ListResult.generateGenericClass(Activity.class)))
            )
            .GET("activities/{name}", this::getActivity, builder -> builder
                .operationId("ConsoleGetActivity")
                .description("Get an activity by name for console.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true)
                    .implementation(String.class))
                .response(responseBuilder()
                    .implementation(Activity.class))
            )
            .POST("activities", this::createActivity, builder -> builder
                .operationId("ConsoleCreateActivity")
                .description("Create an activity.")
                .tag(tag)
                .requestBody(requestBodyBuilder()
                    .required(true)
                    .content(contentBuilder()
                        .mediaType(MediaType.APPLICATION_JSON_VALUE)
                        .schema(schemaBuilder()
                            .implementation(Activity.class))))
                .response(responseBuilder()
                    .implementation(Activity.class))
            )
            .PUT("activities/{name}", this::updateActivity, builder -> builder
                .operationId("ConsoleUpdateActivity")
                .description("Update an activity.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true)
                    .implementation(String.class))
                .requestBody(requestBodyBuilder()
                    .required(true)
                    .content(contentBuilder()
                        .mediaType(MediaType.APPLICATION_JSON_VALUE)
                        .schema(schemaBuilder()
                            .implementation(Activity.class))))
                .response(responseBuilder()
                    .implementation(Activity.class))
            )
            .DELETE("activities/{name}", this::deleteActivity, builder -> builder
                .operationId("ConsoleDeleteActivity")
                .description("Delete an activity.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true)
                    .implementation(String.class))
            )
            .GET("activities/{name}/registrations", this::listRegistrations, builder -> builder
                .operationId("ConsoleListRegistrations")
                .description("List registrations of an activity.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true)
                    .implementation(String.class))
                .parameter(parameterBuilder()
                    .name("page")
                    .in(ParameterIn.QUERY)
                    .implementation(Integer.class)
                    .required(false))
                .parameter(parameterBuilder()
                    .name("size")
                    .in(ParameterIn.QUERY)
                    .implementation(Integer.class)
                    .required(false))
                .response(responseBuilder()
                    .implementation(ListResult.generateGenericClass(ActivityRegistration.class)))
            )
            .POST("registrations/{name}/checkin", this::consoleCheckin, builder -> builder
                .operationId("ConsoleCheckinRegistration")
                .description("Mark a registration as checked in.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true)
                    .implementation(String.class))
                .response(responseBuilder()
                    .implementation(ActivityRegistration.class))
            )
            .POST("registrations/{name}/uncheckin", this::consoleUncheckin, builder -> builder
                .operationId("ConsoleUncheckinRegistration")
                .description("Undo check-in of a registration.")
                .tag(tag)
                .parameter(parameterBuilder()
                    .name("name")
                    .in(ParameterIn.PATH)
                    .required(true)
                    .implementation(String.class))
                .response(responseBuilder()
                    .implementation(ActivityRegistration.class))
            )
            .build();
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.activity.halo.run/v1alpha1");
    }

    private Mono<ServerResponse> listActivities(ServerRequest request) {
        int page = parsePositiveInt(request.queryParam("page").orElse("1"), 1);
        int size = parsePositiveInt(request.queryParam("size").orElse("20"), 20);
        String keyword = request.queryParam("keyword").orElse(null);
        Predicate<Activity> filter = activity -> {
            if (keyword == null || keyword.isBlank()) {
                return true;
            }
            String title = activity.getSpec() == null ? null : activity.getSpec().getTitle();
            return title != null && title.contains(keyword);
        };
        return listByFilter(Activity.class, filter,
            Comparator.comparing(a -> a.getMetadata().getCreationTimestamp() == null
                ? Instant.EPOCH : a.getMetadata().getCreationTimestamp(),
                Comparator.reverseOrder()),
            page, size)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> getActivity(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(Activity.class, name)
            .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
            .flatMap(activity -> ServerResponse.ok().bodyValue(activity))
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> createActivity(ServerRequest request) {
        return request.bodyToMono(Activity.class)
            .switchIfEmpty(Mono.error(new ActivityException("请求体不能为空")))
            .flatMap(activity -> {
                if (activity.getSpec() == null || activity.getSpec().getTitle() == null
                    || activity.getSpec().getTitle().isBlank()) {
                    return Mono.error(new ActivityException("活动标题不能为空"));
                }
                if (activity.getMetadata() == null) {
                    activity.setMetadata(new Metadata());
                }
                if (activity.getMetadata().getName() == null
                    || activity.getMetadata().getName().isBlank()) {
                    activity.getMetadata().setName("activity-" + UUID.randomUUID());
                }
                return client.create(activity);
            })
            .flatMap(created -> ServerResponse.ok().bodyValue(created))
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> updateActivity(ServerRequest request) {
        String name = request.pathVariable("name");
        return request.bodyToMono(Activity.class)
            .switchIfEmpty(Mono.error(new ActivityException("请求体不能为空")))
            .flatMap(activity -> {
                if (activity.getSpec() == null || activity.getSpec().getTitle() == null
                    || activity.getSpec().getTitle().isBlank()) {
                    return Mono.error(new ActivityException("活动标题不能为空"));
                }
                return client.fetch(Activity.class, name)
                    .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
                    .flatMap(oldActivity -> {
                        var oldSpec = oldActivity.getSpec();
                        var newSpec = activity.getSpec();
                        if (oldSpec != null && newSpec != null) {
                            // 名额被调大时，若状态为 FULL 则恢复为 PUBLISHED
                            int oldQuota = oldSpec.getQuota() == null ? 0 : oldSpec.getQuota();
                            int newQuota = newSpec.getQuota() == null ? 0 : newSpec.getQuota();
                            if ("FULL".equals(oldSpec.getStatus()) && newQuota > oldQuota) {
                                newSpec.setStatus("PUBLISHED");
                            }
                        }
                        activity.getMetadata().setName(name);
                        // 关键：保持旧数据的版本号，否则 Halo 的 update 会退化为 create 导致主键冲突 500
                        var oldVersion = oldActivity.getMetadata().getVersion();
                        if (oldVersion != null) {
                            activity.getMetadata().setVersion(oldVersion);
                        }
                        return client.update(activity);
                    });
            })
            .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> deleteActivity(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(Activity.class, name)
            .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
            .flatMap(client::delete)
            .then(ServerResponse.ok().build())
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> listRegistrations(ServerRequest request) {
        String activityName = request.pathVariable("name");
        int page = parsePositiveInt(request.queryParam("page").orElse("1"), 1);
        int size = parsePositiveInt(request.queryParam("size").orElse("20"), 20);
        return client.fetch(Activity.class, activityName)
            .switchIfEmpty(Mono.error(new ActivityException("活动不存在")))
            .flatMap(activity -> listByFilter(ActivityRegistration.class,
                r -> activityName.equals(r.getSpec().getActivityName()),
                Comparator.comparing(r -> r.getSpec().getRegistrationTime() == null
                    ? Instant.EPOCH : r.getSpec().getRegistrationTime(),
                    Comparator.reverseOrder()),
                page, size))
            .flatMap(result -> ServerResponse.ok().bodyValue(result))
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> consoleCheckin(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(ActivityRegistration.class, name)
            .switchIfEmpty(Mono.error(new ActivityException("报名记录不存在")))
            .flatMap(registration -> {
                var spec = registration.getSpec();
                if (Boolean.TRUE.equals(spec.getCheckedIn())) {
                    return Mono.error(new ActivityException("该报名已签到"));
                }
                spec.setCheckedIn(true);
                spec.setCheckedInAt(Instant.now());
                return client.update(registration);
            })
            .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
    }

    private Mono<ServerResponse> consoleUncheckin(ServerRequest request) {
        String name = request.pathVariable("name");
        return client.fetch(ActivityRegistration.class, name)
            .switchIfEmpty(Mono.error(new ActivityException("报名记录不存在")))
            .flatMap(registration -> {
                var spec = registration.getSpec();
                if (!Boolean.TRUE.equals(spec.getCheckedIn())) {
                    return Mono.error(new ActivityException("该报名尚未签到"));
                }
                spec.setCheckedIn(false);
                spec.setCheckedInAt(null);
                return client.update(registration);
            })
            .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
            .onErrorResume(ActivityException.class, e -> badRequest(e.getMessage()));
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
}
