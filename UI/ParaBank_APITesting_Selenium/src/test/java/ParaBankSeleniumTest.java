import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.j2objc.annotations.ReflectionSupport.Level;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

public class ParaBankSeleniumTest {
    private static int screenshotCounter = 1;

    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "C:/selenium webdriver/chromedriver-win64/chromedriver-win64/chromedriver.exe");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20)); // Increased timeout
        
        try {
            String baseUrl = "https://parabank.parasoft.com/parabank/index.htm";
            
            // Enable browser logging
            LoggingPreferences logs = new LoggingPreferences();
            options.setCapability(CapabilityType.LOGGING_PREFS, logs);
            
            testLoginPage(driver, wait, baseUrl);
            testRegistrationPage(driver, wait, baseUrl);
            testTransferFunds(driver, wait, baseUrl);
            testOpenNewAccount(driver, wait, baseUrl);
            
            System.out.println("\nAll UI tests completed successfully!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            takeScreenshot(driver, "final_error_state");
        } finally {
            driver.quit();
        }
    }

    // ========== TEST METHODS WITH DEBUGGING ========== //

    private static void testLoginPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        System.out.println("\n=== Testing Login Page Constraints ===");
        
        try {
            driver.get(baseUrl);
            takeScreenshot(driver, "login_page_loaded");
            
            // Test 1: Valid login
            login(driver, wait, baseUrl, "john", "demo");
            takeScreenshot(driver, "after_valid_login");
            logout(driver, wait);
            
            // Test 2: Invalid username format (symbols)
            testLoginAttempt(driver, wait, "john!", "demo", "Invalid username");
            
            // Test 3: Short password
            testLoginAttempt(driver, wait, "john", "short", "Password must be at least 8 characters");
            
            // Test 4: Non-alphanumeric username
            testLoginAttempt(driver, wait, "john#", "demo1234", "Invalid username");
            
        } catch (Exception e) {
            takeScreenshot(driver, "login_test_failure");
            throw e;
        }
    }

    private static void testRegistrationPage(WebDriver driver, WebDriverWait wait, String baseUrl) {
        System.out.println("\n=== Testing Registration Page Constraints ===");
        
        try {
            driver.get(baseUrl + "register.htm");
            takeScreenshot(driver, "registration_page_loaded");
            
            // Test 1: Valid registration
            fillAndSubmitRegistration(driver, wait, 
                "John", "Doe", "123 Main St", "Anytown", "CA", "12345", 
                "5551234567", "123456789", "johndoe123", "Test1234", "Test1234");
            assertSuccessMessage(driver, wait, "Welcome");
            takeScreenshot(driver, "after_valid_registration");
            logout(driver, wait);
            
            // Test 2: Password mismatch
            fillAndSubmitRegistration(driver, wait, 
                "John", "Doe", "123 Main St", "Anytown", "CA", "12345", 
                "5551234567", "123456789", "johndoe124", "Test1234", "Different123");
            assertErrorMessage(driver, wait, "Passwords did not match");
            
            // Test 3: First name too long
            fillAndSubmitRegistration(driver, wait, 
                "J".repeat(51), "Doe", "123 Main St", "Anytown", "CA", "12345", 
                "5551234567", "123456789", "user123", "ValidPass123", "ValidPass123");
            assertErrorMessage(driver, wait, "First name cannot exceed 50 characters");
            
            // Test 4: Invalid SSN format
            fillAndSubmitRegistration(driver, wait, 
                "John", "Doe", "123 Main St", "Anytown", "CA", "12345", 
                "5551234567", "123abc789", "user123", "ValidPass123", "ValidPass123");
            assertErrorMessage(driver, wait, "Social Security Number must be numeric");
            
        } catch (Exception e) {
            takeScreenshot(driver, "registration_test_failure");
            throw e;
        }
    }

    private static void testTransferFunds(WebDriver driver, WebDriverWait wait, String baseUrl) {
        System.out.println("\n=== Testing Transfer Funds Constraints ===");
        
        try {
            login(driver, wait, baseUrl, "john", "demo");
            
            // Test 1: Valid transfer
            transferFunds(driver, wait, "100", "12345", "67890");
            assertSuccessMessage(driver, wait, "Transfer Complete");
            takeScreenshot(driver, "after_valid_transfer");
            
            // Test 2: Same account transfer
            transferFunds(driver, wait, "100", "12345", "12345");
            assertErrorMessage(driver, wait, "Cannot transfer to the same account");
            
            // Test 3: Zero amount
            transferFunds(driver, wait, "0", "12345", "67890");
            assertErrorMessage(driver, wait, "Amount must be greater than 0");
            
            // Test 4: Non-numeric amount
            transferFunds(driver, wait, "abc", "12345", "67890");
            assertErrorMessage(driver, wait, "Amount must be numeric");
            
            logout(driver, wait);
            
        } catch (Exception e) {
            takeScreenshot(driver, "transfer_test_failure");
            throw e;
        }
    }

    private static void testOpenNewAccount(WebDriver driver, WebDriverWait wait, String baseUrl) {
        System.out.println("\n=== Testing Open New Account Constraints ===");
        
        try {
            login(driver, wait, baseUrl, "john", "demo");
            
            // Test 1: Open Checking account
            openAccount(driver, wait, "CHECKING", "12345");
            assertSuccessMessage(driver, wait, "Account Opened");
            takeScreenshot(driver, "after_checking_account_opened");
            
            // Test 2: Open Savings account
            openAccount(driver, wait, "SAVINGS", "12345");
            assertSuccessMessage(driver, wait, "Account Opened");
            takeScreenshot(driver, "after_savings_account_opened");
            
            // Test 3: No account type selected
            driver.findElement(By.linkText("Open New Account")).click();
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[value='Open New Account']"))).click();
            assertErrorMessage(driver, wait, "Account type is required");
            
            logout(driver, wait);
            
        } catch (Exception e) {
            takeScreenshot(driver, "open_account_test_failure");
            throw e;
        }
    }

    // ========== ENHANCED HELPER METHODS ========== //

    private static void login(WebDriver driver, WebDriverWait wait, String baseUrl, String username, String password) {
        try {
            driver.get(baseUrl);
            wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys(username);
            wait.until(ExpectedConditions.elementToBeClickable(By.name("password"))).sendKeys(password);
            takeScreenshot(driver, "before_login_click");
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[value='Log In']"))).click();
            wait.until(ExpectedConditions.titleContains("Accounts Overview"));
        } catch (Exception e) {
            takeScreenshot(driver, "login_failure");
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    private static void logout(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Log Out"))).click();
            wait.until(ExpectedConditions.titleContains("ParaBank"));
        } catch (Exception e) {
            takeScreenshot(driver, "logout_failure");
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }

    private static void testLoginAttempt(WebDriver driver, WebDriverWait wait, 
                                      String username, String password, String expectedError) {
        try {
            driver.navigate().refresh();
            wait.until(ExpectedConditions.elementToBeClickable(By.name("username"))).sendKeys(username);
            wait.until(ExpectedConditions.elementToBeClickable(By.name("password"))).sendKeys(password);
            takeScreenshot(driver, "before_invalid_login_attempt");
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[value='Log In']"))).click();
            assertErrorMessage(driver, wait, expectedError);
        } catch (Exception e) {
            takeScreenshot(driver, "login_attempt_failure");
            throw new RuntimeException("Login attempt failed: " + e.getMessage(), e);
        }
    }

    private static void fillAndSubmitRegistration(WebDriver driver, WebDriverWait wait,
        String firstName, String lastName, String address, String city, String state,
        String zipCode, String phone, String ssn, String username, String password, String confirm) {
        
        try {
            driver.get("https://parabank.parasoft.com/parabank/register.htm");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.firstName"))).sendKeys(firstName);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.lastName"))).sendKeys(lastName);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.address.street"))).sendKeys(address);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.address.city"))).sendKeys(city);
            if(!state.isEmpty()) {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.address.state"))).sendKeys(state);
            }
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.address.zipCode"))).sendKeys(zipCode);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.phoneNumber"))).sendKeys(phone);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.ssn"))).sendKeys(ssn);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.username"))).sendKeys(username);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("customer.password"))).sendKeys(password);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("repeatedPassword"))).sendKeys(confirm);
            takeScreenshot(driver, "before_registration_submit");
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[value='Register']"))).click();
        } catch (Exception e) {
            takeScreenshot(driver, "registration_fill_failure");
            throw new RuntimeException("Registration form fill failed: " + e.getMessage(), e);
        }
    }

    private static void transferFunds(WebDriver driver, WebDriverWait wait,
                                    String amount, String fromAccount, String toAccount) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Transfer Funds"))).click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("amount"))).clear();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("amount"))).sendKeys(amount);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fromAccountId"))).sendKeys(fromAccount);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("toAccountId"))).sendKeys(toAccount);
            takeScreenshot(driver, "before_transfer_submit");
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[value='Transfer']"))).click();
        } catch (Exception e) {
            takeScreenshot(driver, "transfer_funds_failure");
            throw new RuntimeException("Transfer funds failed: " + e.getMessage(), e);
        }
    }

    private static void openAccount(WebDriver driver, WebDriverWait wait,
                                  String accountType, String fromAccountId) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Open New Account"))).click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("type"))).sendKeys(accountType);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fromAccountId"))).sendKeys(fromAccountId);
            takeScreenshot(driver, "before_open_account_submit");
            wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("input[value='Open New Account']"))).click();
        } catch (Exception e) {
            takeScreenshot(driver, "open_account_failure");
            throw new RuntimeException("Open account failed: " + e.getMessage(), e);
        }
    }

    private static void assertErrorMessage(WebDriver driver, WebDriverWait wait, String expectedError) {
        try {
            // Try multiple locator strategies
            WebElement error = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), " +
                         "translate('" + expectedError.toLowerCase() + "', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'))]")));
            
            System.out.println("Verified error: " + error.getText());
            takeScreenshot(driver, "error_message_shown_" + expectedError.replaceAll("[^a-zA-Z0-9]", "_"));
        } catch (Exception e) {
            takeScreenshot(driver, "error_message_not_found");
            throw new RuntimeException("Expected error message not found: '" + expectedError + "'", e);
        }
    }

    private static void assertSuccessMessage(WebDriver driver, WebDriverWait wait, String expectedText) {
        try {
            // Try multiple locator strategies
            WebElement success = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), " +
                         "translate('" + expectedText.toLowerCase() + "', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'))]")));
            
            System.out.println("Verified success: " + success.getText());
            takeScreenshot(driver, "success_message_shown_" + expectedText.replaceAll("[^a-zA-Z0-9]", "_"));
        } catch (Exception e) {
            takeScreenshot(driver, "success_message_not_found");
            throw new RuntimeException("Expected success message not found: '" + expectedText + "'", e);
        }
    }

    private static void takeScreenshot(WebDriver driver, String description) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String filename = String.format("screenshot_%02d_%s_%d.png", 
                                          screenshotCounter++, 
                                          description,
                                          System.currentTimeMillis());
            Files.copy(screenshot.toPath(), new File(filename).toPath());
            System.out.println("Screenshot saved: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }
}