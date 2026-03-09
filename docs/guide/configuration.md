# Configuration
ODDToolkit uses configuration to define ontology input, generation targets, diagram styling, and adapter behavior.
## Where Configuration Comes From
You can provide configuration in multiple ways (highest priority first):
1. CLI arguments (`--generator=...`, `--config-file=...`).
2. Environment variables (`ODD_*`).
3. YAML/JSON configuration file.
4. Built-in defaults.
## Core Sections
### `ontology`
Defines ontology and concept inputs, plus model-level overrides.
Common keys:
- `ontology-file-path`
- `concepts-file-path`
- `enum-classes`
- `temporal-properties`
- `extra-properties`
- `override-properties`
The `extra-properties` section is useful for project-specific fields such as internal UUIDs or state flags.
### `generators`
Defines generator-specific options and output locations.
Common generator sections:
- `class-diagram`
- `er-diagram`
- `sql-generator`
- `shacl-generator`
- `java-generator`
- `typescript-# Configuration
ODDToolkit uses configuration to define ontology input, generatoiODDToolkit useg ## Where Configuration Comes From
You can provide configuration in multiple ways (highest priority first):
1. CLIasYou can provide configuration inte1. CLI arguments (`--generator=...`, `--config-file=...`).
2. Environmein2. Environment variables (`ODD_*`).
3. YAML/JSON configur/t3. YAML/JSON configuration file.
4ep4. Built-in defaults.
## Core Src## Core Sections
###pl### `ontology`
ieDefines ontol
gCommon keys:
- `ontology-file-path`
- `concep"target/test-cache/c- `ontolgram/- `concepts-file-pa```
- `enum-classes`
- `t`b- `temporal-proar- `extra-properties`
-NA- `override-  --generThe `extra-properties  -### `generators`
Defines generator-specific options and output locations.
Common generator sections:
- `clorDefines generatenCommon generator sections:
- `class-diagram`
- `er-diagin- `class-diagram`
- `er-dda- `er-diagram`
-de- `sql-generaav- `shacl-generatgs- `java-generator` s- `typescript-# Cy ODDToolkit uses configurcd /Users/maximvdw/workspaces/oddtoolkit/docs && npm run docs:build 2>&1 | tail -15
cd /Users/maximvdw/workspaces/oddtoolkit/docs && npm run docs:build
cd /Users/maximvdw/workspaces/oddtoolkit/docs && npm run docs:build 2>&1 | tee /tmp/build.log && tail -20 /tmp/build.log
cd /Users/maximvdw/workspaces/oddtoolkit/docs && npx vitepress build 2>&1 ; echo "Exit code: $?"
cd /Users/maximvdw/workspaces/oddtoolkit/docs && timeout 60 npm run docs:build 2>&1 > /tmp/build_output.txt ; echo "Build completed with exit code: $?" && cat /tmp/build_output.txt | tail -30
ls -la /Users/maximvdw/workspaces/oddtoolkit/docs/.vitepress/dist/ 2>&1 | wc -l
grep -r "layout: home" /Users/maximvdw/workspaces/oddtoolkit/docs/*.md 2>/dev/null
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -3
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | grep -E "(ERROR|error)" | head -20
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile -q 2>&1 | tail -10
cd /Users/maximvdw/workspaces/OWLLM && mvn compile 2>&1 | grep -A 5 "ERROR"
cd /Users/maximvdw/workspaces/OWLLM && mvn compile 2>&1 | tail -50
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -20
find /Users/maximvdw/workspaces/OWLLM/src/main/java -name "ReviewerService.java" -type f
ls -la /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/review/
mkdir -p /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/review && ls -la /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/ | grep review
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | grep -E "(BUILD SUCCESS|BUILD FAILURE|ERROR)" | head -10
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | tail -5
cd /Users/maximvdw/workspaces/OWLLM && mvn compile -q 2>&1 | tail -10
cd /Users/maximvdw/workspaces/OWLLM && mvn compile 2>&1 | grep -A 10 "ERROR"
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | grep -E "\[ERROR\]" | head -30
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | tail -50
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile -q 2>&1 | tail -20
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile -q 2>&1 | tail -5
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | grep -E "\[ERROR\]" | head -30
cd /Users/maximvdw/workspaces/OWLLM && mvn compile 2>&1 | grep -B 5 -A 5 "error"
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile -q 2>&1 | tail -10
ls -la /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/review/ReviewerService.java
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | grep -E "BUILD SUCCESS|BUILD FAILURE"
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile 2>&1 | tail -20
cd /Users/maximvdw/workspaces/OWLLM && mvn clean compile
ls -la /Users/maximvdw/workspaces/OWLLM/target/classes/be/vlaanderen/omgeving/owllm/ 2>/dev/null | head -20
cd /Users/maximvdw/workspaces/OWLLM && find target/classes -name "*.class" 2>/dev/null | head -10
ls -la /Users/maximvdw/workspaces/OWLLM/target/ 2>&1 | head -5
cd /Users/maximvdw/workspaces/OWLLM && mvn compile 2>&1
cd /Users/maximvdw/workspaces/OWLLM && mvn --version
cd /Users/maximvdw/workspaces/OWLLM && javac -version 2>&1 && echo "Java compiler available"
rm -f /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/review/ReviewerService.java
cd /Users/maximvdw/workspaces/OWLLM && mvn -q -DskipTests compile
rm -f /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/GeneratorAgent.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/ConcurrentBatchGenerator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/SequentialBatchGenerator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/MasterOrchestrator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/GenerationOrchestrator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/ReviewCoordinator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/ReviewerService.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/review/ReviewerService.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/llm/handler/ReviewHandler.java /Users/maximvdw/workspaces/OWLLM/src/main/resources/generator.context.txt /Users/maximvdw/workspaces/OWLLM/src/main/resources/master.context.txt
rm -f /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/ConcurrentBatchGenerator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/GenerationOrchestrator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/GeneratorAgent.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/MasterOrchestrator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/ReviewCoordinator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/ReviewerService.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/SequentialBatchGenerator.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/review/ReviewerService.java /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/llm/handler/ReviewHandler.java /Users/maximvdw/workspaces/OWLLM/src/main/resources/generator.context.txt /Users/maximvdw/workspaces/OWLLM/src/main/resources/master.context.txt
cat > /Users/maximvdw/workspaces/OWLLM/src/main/java/be/vlaanderen/omgeving/owllm/generation/GenerationOrchestrator.java <<'EOF'
package be.vlaanderen.omgeving.owllm.generation;
// Deprecated stub.
