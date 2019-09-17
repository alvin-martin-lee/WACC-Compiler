import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class   WACCFrontEndTest {

  private final String[] validTests = WACCTestLists.validTests;
  private final String validTestPrefix = "test/valid/";
  private final String[] invalidSyntaxTests = WACCTestLists.invalidSyntaxTests;
  private final String invalidSyntaxTestPrefix = "test/invalid/syntaxErr/";
  private final String[] invalidSemanticTests = WACCTestLists.invalidSemanticTests;
  private final String invalidSemanticTestPrefix = "test/invalid/semanticErr/";
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();

  @Test
  void runValidTests() {
    System.out.println("==================");
    System.out.println("| Valid Programs |");
    System.out.println("==================");

    List<String> passedTests = new ArrayList<>();
    List<String> failedTests = new ArrayList<>();


    for (String test : validTests) {
      try {
        ProcessBuilder builder = new ProcessBuilder("./compile", validTestPrefix + test, "-OC");
        final Process process = builder.start();
        int result = process.waitFor();
        if (result == 0) {
          passedTests.add(test);
        } else {
          failedTests.add(test);
        }
      } catch (Exception e) {
        failedTests.add(test);
      }
    }

    // Check that all tests passed
    assertEquals(passedTests.size() + failedTests.size(), validTests.length);

    System.out.println();
    System.out.println(passedTests.size() + "/" + validTests.length + " tests passed");
    System.out.println();
    System.out.println("Failed tests:");
    for (String test : failedTests) {
      System.out.println(test);
    }
    System.out.println("----------");
    System.out.println();

    assertEquals(0, failedTests.size());
  }

  @Test
  void runInvalidSyntaxTests() {
    System.out.println("===========================");
    System.out.println("| Invalid Syntax Programs |");
    System.out.println("===========================");

    List<String> passedTests = new ArrayList<>();
    List<String> failedTests = new ArrayList<>();


    for (String test : invalidSyntaxTests) {
      try {
        ProcessBuilder builder = new ProcessBuilder("./compile", invalidSyntaxTestPrefix + test, "-OC");
        final Process process = builder.start();
        int result = process.waitFor();
        if (result == 100) {
          passedTests.add(test);
        } else {
          failedTests.add(test);
        }
      } catch (Exception e) {
        failedTests.add(test);
      }
    }

    // Check that all tests passed
    assertEquals(passedTests.size() + failedTests.size(), invalidSyntaxTests.length);

    System.out.println();
    System.out.println(passedTests.size() + "/" + invalidSyntaxTests.length + " tests passed");
    System.out.println();
    System.out.println("Failed tests:");
    for (String test : failedTests) {
      System.out.println(test);
    }
    System.out.println("----------");
    System.out.println();

    assertEquals(0, failedTests.size());
  }

  @Test
  void runInvalidSemanticTests() {
    System.out.println("=============================");
    System.out.println("| Invalid Semantic Programs |");
    System.out.println("=============================");

    List<String> passedTests = new ArrayList<>();
    List<String> failedTests = new ArrayList<>();


    for (String test : invalidSemanticTests) {
      try {
        ProcessBuilder builder = new ProcessBuilder("./compile", invalidSemanticTestPrefix + test, "-OC");
        final Process process = builder.start();
        int result = process.waitFor();
        if (result == 200) {
          passedTests.add(test);
        } else {
          failedTests.add(test);
        }
      } catch (Exception e) {
        failedTests.add(test);
      }
    }

    // Check that all tests passed
    assertEquals(passedTests.size() + failedTests.size(), invalidSemanticTests.length);

    System.out.println();
    System.out.println(passedTests.size() + "/" + invalidSemanticTests.length + " tests passed");
    System.out.println();
    System.out.println("Failed tests:");
    for (String test : failedTests) {
      System.out.println(test);
    }
    System.out.println("----------");
    System.out.println();

    assertEquals(0, failedTests.size());
  }
}