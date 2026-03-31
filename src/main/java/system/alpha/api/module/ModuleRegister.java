package system.alpha.api.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    Category category();
    String description() default ""; // Добавляем поле описания
    int bind() default -999;
}