package enigma;

import ucb.junit.textui;

/** The suite of all JUnit tests for the enigma package.
 *  @author
 */
public class UnitTest {

    //put all your tests here

    /** Run the JUnit tests in this package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(PermutationTest.class, MovingRotorTest.class);
    }

}

