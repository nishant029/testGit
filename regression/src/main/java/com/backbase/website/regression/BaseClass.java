package com.backbase.website.regression;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.UnexpectedTagNameException;

public class BaseClass {
	// we can have different webdrivertype, like ie, firefox, htmlunit.
	// also we can get these types from a property file, for now I am just using it from here
	public static WebDriver driver = null;
	String webdriverType = "chrome";
	private FieldType fieldType;

	public enum FieldType {
		/** the field allows input (may be updated) */
		ATTR_VALUE,

		/** the field is read-only */
		TEXT,

		/** the field has a drop-list of allowed values */
		SELECT,

		/** the field is an on/off checkbox */
		CHECKBOX;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	@BeforeClass
	public static void setWebdriver() {
		// set the system property for finding the chrome driver executable
		System.out.println(System.getProperty("user.home"));
		System.setProperty(ChromeDriverService.CHROME_DRIVER_EXE_PROPERTY, System.getProperty("user.home")+"/Downloads/chromedriver_win32/chromedriver37.exe");

		driver = new ChromeDriver();
		driver.manage().window().maximize();
	}

	public WebDriver getWebDriver() {
		return driver;
	}

	public final void click(WebDriver driver, WebElement uiElement) {

		boolean disabled = isDisabled(uiElement);

		if (disabled)
			throw new IllegalStateException("Cannot click element, it is disabled");

		uiElement.click();
	}

	public final void type(WebDriver driver, WebElement uiElement, String typedInput) {
		fieldType = getFieldType();

		while (true) {

			try {
				switch (fieldType) {
				case ATTR_VALUE:
				case TEXT:
					clearAndType(driver, uiElement, typedInput);
					break;
				case SELECT:
					typeSelectChoice(driver, uiElement, typedInput);
					break;
				case CHECKBOX:
					boolean checkValue = Boolean.valueOf(typedInput);
					checkCheckbox(driver, uiElement, checkValue);
					break;
				default:
					throw new IllegalStateException();
				}

			} catch (WebDriverException ex) {
				throw ex;
			}
		}
	}

	public String get(WebDriver driver, WebElement uiElement, int rowIndex) {
		fieldType = getFieldType();

		while (true) {
			String value = null;

			try {

				if (fieldType == null)
					value = getClickableLable(driver, uiElement);

				else {

					switch (fieldType) {
					case ATTR_VALUE:
						value = getValueAttribute(driver, uiElement);
						break;
					case TEXT:
						// this gets the text more reliably than getText() (returns an updated value)
						value = getValueAttribute(driver, uiElement);
						if (value == null)
							value = getText(driver, uiElement);
						break;
					case SELECT:
						value = getCurrentSelectOption(driver, uiElement);
						break;
					case CHECKBOX:
						value = Boolean.valueOf(isChecked(driver, uiElement)).toString();
						break;
					default:
						throw new IllegalStateException();
					}
				}

				value = (value == null) ? "" : value.trim();
				return value;

			} catch (WebDriverException ex) {
				throw ex;
			}
		}
	}

	private String getClickableLable(WebDriver driver, WebElement uiElement) {
		String value = getText(driver, uiElement);

		if (StringUtils.isEmpty(value)) {
			WebElement anchorElement = uiElement;

			if (uiElement.getTagName().equals("span")) {
				anchorElement = uiElement.findElement(By.xpath(".."));
				value = getText(driver, anchorElement);
			}

			if (StringUtils.isEmpty(value)) {
				value = anchorElement.getAttribute("title");
			}
		}

		return value;
	}

	private boolean isChecked(WebDriver driver, WebElement uiElement) {
		boolean value = uiElement.isSelected();
		return value;
	}

	protected String getCurrentSelectOption(WebDriver driver, WebElement selectElement) {
		try {
			WebElement selectedOption = newSelect(driver, selectElement).getFirstSelectedOption();
			String value = getText(driver, selectedOption);
			return value;
		} catch (UnexpectedTagNameException treateLikeBlank) {
			return "";
		} catch (NoSuchElementException treateLikeBlank) {
			return "";
		}
	}

	protected String getValueAttribute(WebDriver driver, WebElement uiElement) {
		String value = uiElement.getAttribute("value");
		return value;
	}

	protected static String getText(WebDriver driver, WebElement uiElement) {
		// note: javadocs on getText() are not correct, the value is not (always) trimmed
		String value = uiElement.getText();
		return value.trim();
	}

	protected void clearAndType(WebDriver driver, WebElement uiElement, String typedInput) {

		if (typedInput != null && !typedInput.isEmpty()) {
			//geckodriver doesn't work if the chord is in the same sendKeys call as the input
			//(it types the whole input with the control key down) so we break it into two calls
			uiElement.sendKeys(Keys.chord(Keys.CONTROL,"a"));
			uiElement.sendKeys(typedInput);

		} else {
			// nothing happens if typing an empty string, so use clear() in that case
			uiElement.clear();
		}
	}

	protected void typeSelectChoice(WebDriver driver, WebElement selectElement, String typedInput) {
		Select select = newSelect(driver, selectElement);

		try {
			select.selectByVisibleText(typedInput);

		} catch (NoSuchElementException ex) {

			/*
			 * The provided value (typedInput) may not have been found because it was a value returned by getSelectChoices().
			 * That function trims values before returning them, but a value in a select list may be untrimmed, and
			 * selectByVisibleText() requires an exact match. So, see if the provided value matches an existing trimmed
			 * value. If it does, retry with the untrimmed match.
			 */
			List<String> untrimmedChoices = getUntrimmedSelectChoices(driver, selectElement);

			for (String untrimmedChoice : untrimmedChoices) {
				String choice = untrimmedChoice.trim();
				if (typedInput.equals(choice)) {
					select.selectByVisibleText(untrimmedChoice);
					return;
				}
			}

			throw ex;
		}
	}

	private void checkCheckbox(WebDriver driver, WebElement uiElement, boolean checkValue) {
		boolean isChecked = uiElement.isSelected();

		if (checkValue != isChecked) {
			uiElement.click();
		}
	}

	private List<String> getUntrimmedSelectChoices(WebDriver driver, WebElement selectElement) {
		Select select = newSelect(driver, selectElement);
		List<WebElement> options = select.getOptions();
		List<String> choices = new ArrayList<String>();

		for (WebElement option : options)
			choices.add(option.getText());

		return choices;
	}

	private static Select newSelect(WebDriver driver, WebElement selectElement) {
		Select select = new Select(selectElement);

		String displayMode = selectElement.getCssValue("display");
		if ("none".equals(displayMode)) {
			// see: http://stackoverflow.com/questions/13047056/how-to-read-text-from-hidden-element-with-selenium-webdriver
			selectElement = (WebElement) ((JavascriptExecutor) driver).executeScript(
					"arguments[0].style['display'] = 'block';" +
							"arguments[0].style['visibility'] = 'visible';" +
							"return arguments[0];", selectElement);
		}

		return select;
	}

	protected boolean isDisabled(WebElement uiElement) {
		boolean disabled = false;

		if (!uiElement.isEnabled())
			disabled = true;

		else {
			String classAttr = uiElement.getAttribute("class");

			if (classAttr.contains("ui-state-disabled"))
				disabled = true;

			else if (uiElement.getTagName().equals("span")) {
				classAttr = uiElement.findElement(By.xpath("..")).getAttribute("class");
				disabled = classAttr.contains("ui-state-disabled");
			}
		}

		return disabled;
	}

	@After
	public void baseTearDown() {
		for (String logType : driver.manage().logs().getAvailableLogTypes()) {
			BufferedWriter writer = null;

			try {
				File logfile = new File(System.getProperty("user.home")+"/Downloads/logging/", logType + ".log");
				OutputStream output = FileUtils.openOutputStream(logfile, true);
				writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));

				for (LogEntry e : driver.manage().logs().get(logType)) {
					writer.write(e.toString());
					writer.newLine();
				}

			} catch (IOException ex) {
				throw new RuntimeException(ex);
			} finally {
				IOUtils.closeQuietly(writer);
			}
		}

		if (driver != null) {
			driver.quit();
			driver = null;
		}
	}
}
