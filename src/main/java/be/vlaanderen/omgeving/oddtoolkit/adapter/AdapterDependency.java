package be.vlaanderen.omgeving.oddtoolkit.adapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AdapterDependency {
  Class<? extends AbstractAdapter<?>>[] value();
}
