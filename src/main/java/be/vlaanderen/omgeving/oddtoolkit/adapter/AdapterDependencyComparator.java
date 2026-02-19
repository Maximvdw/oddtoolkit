package be.vlaanderen.omgeving.oddtoolkit.adapter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AdapterDependencyComparator implements
    Comparator<AbstractAdapter<?>> {

  // cache computed depths for classes to keep ordering stable and avoid recomputation
  private final Map<Class<?>, Integer> depthCache = new HashMap<>();

  @Override
  public int compare(AbstractAdapter<?> a1,
      AbstractAdapter<?> a2) {
    // quick identity check
    if (a1 == a2) {
      return 0;
    }

    Class<?> c1 = a1.getClass();
    Class<?> c2 = a2.getClass();

    // If c1 transitively depends on c2, c1 should come after c2
    if (dependsOn(c1, c2, new HashSet<>())) {
      return 1;
    }

    // If c2 transitively depends on c1, c2 should come after c1
    if (dependsOn(c2, c1, new HashSet<>())) {
      return -1;
    }

    // No direct transitive relation. Compare by declared dependency "depth" so adapters
    // that are deeper in the dependency graph come later. This helps when multiple
    // adapters share common dependencies but also have other inter-dependencies.
    int d1 = computeDepth(c1, new HashSet<>());
    int d2 = computeDepth(c2, new HashSet<>());
    if (d1 != d2) {
      return Integer.compare(d1, d2);
    }

    // Final deterministic fallback: compare by full class name
    return c1.getName().compareTo(c2.getName());
  }

  /**
   * Returns true if sourceClass transitively declares a dependency on targetClass.
   * We treat a declared dependency dep as matching any subtype/implementation of dep
   * (using isAssignableFrom).
   */
  private boolean dependsOn(Class<?> sourceClass, Class<?> targetClass, Set<Class<?>> visited) {
    if (sourceClass == null || targetClass == null) {
      return false;
    }
    if (sourceClass.equals(targetClass)) {
      // a class is not considered to depend on itself for ordering purposes
      return false;
    }

    // Prevent cycles / repeated work
    if (!visited.add(sourceClass)) {
      return false;
    }

    AdapterDependency depAnn = sourceClass.getAnnotation(AdapterDependency.class);
    if (depAnn == null) {
      return false;
    }

    for (Class<? extends AbstractAdapter<?>> dep : depAnn.value()) {
      // If the declared dependency type matches the target (or is a supertype of the target)
      if (dep.isAssignableFrom(targetClass)) {
        return true;
      }
      // Recurse into the dependency's own declared dependencies
      if (dependsOn(dep, targetClass, visited)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Compute a "depth" for the class: the maximum number of dependency hops
   * following AdapterDependency annotations. Classes with no AdapterDependency have depth 0.
   * Uses visited set to avoid cycles and a cache for performance.
   */
  private int computeDepth(Class<?> cls, Set<Class<?>> visited) {
    if (cls == null) {
      return 0;
    }
    Integer cached = depthCache.get(cls);
    if (cached != null) {
      return cached;
    }
    if (!visited.add(cls)) {
      // cycle encountered -> break it by returning 0 for this path
      return 0;
    }

    AdapterDependency ann = cls.getAnnotation(AdapterDependency.class);
    int depth = 0;
    if (ann != null && ann.value().length > 0) {
      int maxChild = 0;
      for (Class<? extends AbstractAdapter<?>> dep : ann.value()) {
        int childDepth = 1 + computeDepth(dep, visited);
        if (childDepth > maxChild) {
          maxChild = childDepth;
        }
      }
      depth = maxChild;
    }

    depthCache.put(cls, depth);
    return depth;
  }

}
