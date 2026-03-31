package system.alpha.api.utils.render.fonts;

import lombok.experimental.UtilityClass;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Fonts {
    public final String sf = "sf_pro";
    public final String ps = "product_sans";


    public final Font SF_REGULAR = get(sf + "/sf_regular");

    public final Font PS_BOLD = get(ps + "/productsans_bold");
    public final Font PS_MEDIUM = get(ps + "/productsans_medium");
    public final Font PS_REGULAR = get(ps + "/productsans_regular");


    public final Font ICONS = get("other/icons");


    private Font get(String input) {
        return Font.builder().find(input).load();
    }
}