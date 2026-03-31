package system.alpha.api.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    RENDER("Render"),
    PLAYER("Player"),
    OTHER("Other");

    private final String label;
}
