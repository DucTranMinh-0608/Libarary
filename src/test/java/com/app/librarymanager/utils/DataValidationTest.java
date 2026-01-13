package com.app.librarymanager.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DataValidationTest {

  @Test
  void validISBN() {
    assertAll(() -> assertTrue(DataValidation.validISBN("9780596520687")),
        () -> assertTrue(DataValidation.validISBN("9783161484100")),
        () -> assertTrue(DataValidation.validISBN("0596520689")),
        () -> assertFalse(DataValidation.validISBN("0596520688")),
        () -> assertTrue(DataValidation.validISBN("978-0-596-52068-7")),
        () -> assertTrue(DataValidation.validISBN("978 0 596 52068 7")),
        () -> assertFalse(DataValidation.validISBN("0-306-40615-X")));
  }


  @Test
  void validEmail() {
    assertAll(() -> assertFalse(DataValidation.validEmail("a@b.c")),
        () -> assertTrue(DataValidation.validEmail("admin@admin.com")),
        () -> assertTrue(DataValidation.validEmail("--@gmail.com")),
        () -> assertFalse(DataValidation.validEmail("abc@gmail..com")));
  }

  @Test
  void checkInt() {
    assertAll(() -> assertEquals("", DataValidation.checkInt("value", "1")),
        () -> assertEquals("", DataValidation.checkInt("value", "0")),
        () -> assertEquals("", DataValidation.checkInt("value", "2147483647")),
        () -> assertTrue(DataValidation.checkInt("value", "-1").contains("Fail")),
        () -> assertTrue(DataValidation.checkInt("value", "2147483648").contains("Fail")),
        () -> assertTrue(DataValidation.checkInt("value", "2.34").contains("Fail")),
        () -> assertTrue(DataValidation.checkInt("value", "-2.34.567").contains("Fail")),
        () -> assertTrue(DataValidation.checkInt("value", "sus").contains("Fail")));
  }

  @Test
  void checkDouble() {
    assertAll(() -> assertEquals("", DataValidation.checkDouble("value", "1")),
        () -> assertEquals("", DataValidation.checkDouble("value", "0")),
        () -> assertEquals("", DataValidation.checkDouble("value", "2147483647")),
        () -> assertTrue(DataValidation.checkDouble("value", "-1").contains("Fail")),
        () -> assertEquals("", DataValidation.checkDouble("value", "2147483648")),
        () -> assertEquals("", DataValidation.checkDouble("value", "2.34")),
        () -> assertTrue(DataValidation.checkDouble("value", "-2.34.567").contains("Fail")),
        () -> assertTrue(DataValidation.checkDouble("value", "-2").contains("Fail")),
        () -> assertTrue(DataValidation.checkDouble("value", "sus").contains("Fail")));
  }
}