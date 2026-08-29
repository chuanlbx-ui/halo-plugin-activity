package run.halo.activity;

import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * 活动管理插件主类。
 *
 * @author halo-plugin-activity
 */
@Component
public class ActivityPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public ActivityPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(Activity.class);
        schemeManager.register(ActivityRegistration.class);
        System.out.println("[activity-plugin] 活动插件启动成功，已注册 Activity 与 ActivityRegistration Scheme");
    }

    @Override
    public void stop() {
        schemeManager.unregister(schemeManager.get(Activity.class));
        schemeManager.unregister(schemeManager.get(ActivityRegistration.class));
        System.out.println("[activity-plugin] 活动插件已停止，已注销 Activity 与 ActivityRegistration Scheme");
    }
}
