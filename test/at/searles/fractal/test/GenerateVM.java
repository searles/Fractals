package at.searles.fractal.test;

import at.searles.fractal.FractviewInstructionSet;
import at.searles.meelan.ops.InstructionSet;
import org.junit.Test;

/**
 * This is in test because it is not needed in the final jar.
 */
public class GenerateVM {
    private InstructionSet instructionSet;

    @Test
    public void simpleTest() {
        initSystemInstructionSet();

        System.out.println(instructionSet.createVM());
    }

    private void initSystemInstructionSet() {
        instructionSet = FractviewInstructionSet.get();
    }
}
