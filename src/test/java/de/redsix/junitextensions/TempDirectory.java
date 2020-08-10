package de.redsix.junitextensions;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TempDirectory {

    String parentPath() default "";
}
