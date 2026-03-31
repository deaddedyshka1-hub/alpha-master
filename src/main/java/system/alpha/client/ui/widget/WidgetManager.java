package system.alpha.client.ui.widget;

import lombok.Getter;
import system.alpha.api.event.Listener;
import system.alpha.api.event.events.render.Render2DEvent;
import system.alpha.client.features.modules.render.InterfaceModule;
//.client.ui.widget.overlay.*;
import system.alpha.client.ui.widget.overlay.*;
import system.alpha.client.ui.widget.overlay.notify.NotificationWidget;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WidgetManager {
    @Getter private final static WidgetManager instance = new WidgetManager();

    private final List<Widget> widgets = new ArrayList<>();

    public void load() {
        register(
                new WatermarkWidget(),
                new KeybindsWidget(),
                new PotionsWidget(),
                new CooldownsWidget(),
                new HotbarWidget(),
                new ArmorWidget(),
                new TargetInfoWidget(),
                new MusicWidget(),
                new NotificationWidget()
        );

        InterfaceModule.getInstance().init();

        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (InterfaceModule.getInstance().isEnabled()) {
                for (Widget widget : widgets) {
                    if (widget.isEnabled()) widget.render(event);
                }
            }
        }));
    }

    public void register(Widget... widgets) {
        this.widgets.addAll(List.of(widgets));
    }
}
