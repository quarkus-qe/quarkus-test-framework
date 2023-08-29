package io.quarkus.test.scenarios.execution.condition;

import org.junit.jupiter.api.extension.ExecutionCondition;

/**
 * Services that provides required {@link ExecutionCondition} applied on every test class
 * annotated with the QuarkusScenario annotation.
 */
public interface QuarkusScenarioExecutionCondition extends ExecutionCondition {

}
