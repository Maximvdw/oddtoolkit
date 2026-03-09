package be.vlaanderen.omgeving.oddtoolkit.config;

import be.vlaanderen.omgeving.oddtoolkit.generator.BaseGenerator;
import java.util.List;
import java.util.Optional;

/**
 * Registry for dynamically managing generators.
 * Allows for flexible registration and discovery of generators at runtime.
 */
public interface GeneratorRegistry {

  /**
   * Register a generator with the given name.
   *
   * @param name unique identifier for the generator
   * @param generator the generator instance
   */
  void register(String name, BaseGenerator generator);

  /**
   * Retrieve a generator by name.
   *
   * @param name the generator name
   * @return the generator instance wrapped in Optional
   */
  Optional<BaseGenerator> get(String name);

  /**
   * Get all registered generator names.
   *
   * @return list of available generator names
   */
  List<String> getAvailableGenerators();

  /**
   * Check if a generator is registered.
   *
   * @param name the generator name
   * @return true if generator exists
   */
  boolean has(String name);
}

