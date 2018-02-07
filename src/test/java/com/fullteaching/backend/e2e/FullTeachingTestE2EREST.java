/*
 * (C) Copyright 2017 OpenVidu (http://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.fullteaching.backend.e2e;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;

import io.github.bonigarcia.SeleniumExtension;
import io.github.bonigarcia.wdm.ChromeDriverManager;
import io.github.bonigarcia.wdm.FirefoxDriverManager;

/**
 * E2E tests for FullTeaching REST operations.
 *
 * @author Pablo Fuente (pablo.fuente@urjc.es)
 */
@Tag("e2e")
@DisplayName("E2E tests for FullTeaching REST operations")
@ExtendWith(SeleniumExtension.class)
@RunWith(JUnitPlatform.class)
public class FullTeachingTestE2EREST {

	final String TEST_COURSE = "TEST_COURSE";
	final String TEST_COURSE_INFO = "TEST_COURSE_INFO";
	final String EDITED = " EDITED";

	final String TEACHER_MAIL = "teacher@gmail.com";
	final String TEACHER_PASS = "pass";
	final String TEACHER_NAME = "Teacher Cheater";

	String COURSE_NAME = TEST_COURSE;

	static String APP_URL = "https://localhost:5000/";
	static Exception ex = null;

	final static Logger log = getLogger(lookup().lookupClass());

	BrowserUser user;

	/*** ClassRule methods ***/

	@BeforeAll()
	static void setupAll() {

		if (System.getenv("ET_EUS_API") == null) {
			// Outside ElasTest
			ChromeDriverManager.getInstance().setup();
			FirefoxDriverManager.getInstance().setup();
		}

		if (System.getenv("ET_SUT_HOST") != null) {
			APP_URL = "https://" + System.getenv("ET_SUT_HOST") + ":5000/";
		}

		log.info("Using URL {} to connect to openvidu-testapp", APP_URL);
	}

	BrowserUser setupBrowser(String browser) {

		BrowserUser u;

		switch (browser) {
		case "chrome":
			u = new ChromeUser("TestUser", 20);
			break;
		case "firefox":
			u = new FirefoxUser("TestUser", 20);
			break;
		default:
			u = new ChromeUser("TestUser", 20);
		}

		u.getDriver().get(APP_URL);

		final String GLOBAL_JS_FUNCTION = "var s = window.document.createElement('script');"
				+ "s.innerText = 'window.MY_FUNC = function(containerQuerySelector) {"
				+ "var elem = document.createElement(\"div\");" + "elem.id = \"video-playing-div\";"
				+ "elem.innerText = \"VIDEO PLAYING\";" + "document.body.appendChild(elem);"
				+ "console.error(\"ERRRRORRRR!!!!\")}';" + "window.document.head.appendChild(s);";

		u.runJavascript(GLOBAL_JS_FUNCTION);

		return u;
	}

	@BeforeEach
	void setup() {
		loginTeacher(); // Teacher login
		addCourse(COURSE_NAME); // Add test course

	}

	@AfterEach
	void dispose() {
		this.deleteCourseIfExist();
		this.logout(user);
		user.dispose();
	}

	/*** Test methods ***/

	@Test
	@DisplayName("Course REST operations")
	void courseRestOperations() throws Exception {

		log.info("Course REST operations");

		// Edit course

		COURSE_NAME = COURSE_NAME + EDITED;

		List<WebElement> l = user.getDriver().findElements(By.className("course-put-icon"));
		openDialog(l.get(l.size() - 1));

		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("input-put-course-name"))));
		user.getDriver().findElement(By.id("input-put-course-name")).clear();
		user.getDriver().findElement(By.id("input-put-course-name")).sendKeys(COURSE_NAME);
		user.getDriver().findElement(By.id("submit-put-course-btn")).click();

		waitForDialogClosed("course-modal");

		user.getWaiter().until(ExpectedConditions.textToBe(
				By.cssSelector("#course-list .course-list-item:last-child div.course-title span"), COURSE_NAME));

	}

	@Test
	@DisplayName("Course info REST operations")
	void courseInfonRestOperations() throws Exception {

		log.info("Course Info REST operations");

		// Empty course info
		enterCourseAndNavigateTab(COURSE_NAME, "info-tab-icon");
		user.getWaiter().until(ExpectedConditions.presenceOfNestedElementLocatedBy(
				By.cssSelector(".md-tab-body.md-tab-active"), By.cssSelector(".card-panel.warning")));

		// Edit course info
		user.getDriver().findElement(By.id("edit-course-info")).click();
		user.getDriver().findElement(By.className("ql-editor")).sendKeys(TEST_COURSE_INFO);
		user.getDriver().findElement(By.id("send-info-btn")).click();
		waitForAnimations();

		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector(".ql-editor p"), TEST_COURSE_INFO));

	}

	@Test
	@DisplayName("Session REST operations")
	void sessionRestOperations() throws Exception {

		log.info("Session REST operations");

		// Add new session

		enterCourseAndNavigateTab(COURSE_NAME, "sessions-tab-icon");
		openDialog("#add-session-icon");

		// Find form elements
		WebElement titleField = user.getDriver().findElement(By.id("input-post-title"));
		WebElement commentField = user.getDriver().findElement(By.id("input-post-comment"));
		WebElement dateField = user.getDriver().findElement(By.id("input-post-date"));
		WebElement timeField = user.getDriver().findElement(By.id("input-post-time"));

		String title = "TEST LESSON NAME";
		String comment = "TEST LESSON COMMENT";

		// Fill input fields
		titleField.sendKeys(title);
		commentField.sendKeys(comment);
		dateField.sendKeys("03-01-2018");
		timeField.sendKeys("03:10");
		timeField.sendKeys("PM");

		user.getDriver().findElement(By.id("post-modal-btn")).click();

		waitForDialogClosed("course-details-modal");

		// Check fields of added session

		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector("li.session-data .session-title"), title));
		user.getWaiter()
				.until(ExpectedConditions.textToBe(By.cssSelector("li.session-data .session-description"), comment));
		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector("li.session-data .session-datetime"),
				"Mar 1, 2018 - 15:10"));

		// Edit session
		openDialog(".edit-session-icon");

		// Find form elements
		titleField = user.getDriver().findElement(By.id("input-put-title"));
		commentField = user.getDriver().findElement(By.id("input-put-comment"));
		dateField = user.getDriver().findElement(By.id("input-put-date"));
		timeField = user.getDriver().findElement(By.id("input-put-time"));

		// Clear elements
		titleField.clear();
		commentField.clear();

		// Fill edited input fields
		titleField.sendKeys(title + EDITED);
		commentField.sendKeys(comment + EDITED);
		dateField.sendKeys("04-02-2019");
		timeField.sendKeys("05:10");
		timeField.sendKeys("AM");

		user.getDriver().findElement(By.id("put-modal-btn")).click();

		waitForDialogClosed("put-delete-modal");

		System.out.println("EDITED!!!!");

		// Check fields of edited session
		user.getWaiter()
				.until(ExpectedConditions.textToBe(By.cssSelector("li.session-data .session-title"), title + EDITED));
		user.getWaiter().until(
				ExpectedConditions.textToBe(By.cssSelector("li.session-data .session-description"), comment + EDITED));
		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector("li.session-data .session-datetime"),
				"Apr 2, 2019 - 05:10"));

		// Delete session
		openDialog(".edit-session-icon");

		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("label-delete-checkbox"))));
		user.getDriver().findElement(By.id("label-delete-checkbox")).click();
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("delete-session-btn"))));
		user.getDriver().findElement(By.id("delete-session-btn")).click();

		waitForDialogClosed("put-delete-modal");

		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("li.session-data"), 0));

	}

	@Test
	@DisplayName("Forum REST operations")
	void forumRestOperations() throws Exception {

		log.info("Forum REST operations");

		// Add new entry to the forum

		enterCourseAndNavigateTab(COURSE_NAME, "forum-tab-icon");
		openDialog("#add-entry-icon");

		// Find form elements
		WebElement titleField = user.getDriver().findElement(By.id("input-post-title"));
		WebElement commentField = user.getDriver().findElement(By.id("input-post-comment"));

		String title = "TEST FORUM ENTRY";
		String comment = "TEST FORUM COMMENT";
		String entryDate = "a few seconds ago";

		// Fill input fields
		titleField.sendKeys(title);
		commentField.sendKeys(comment);

		user.getDriver().findElement(By.id("post-modal-btn")).click();

		waitForDialogClosed("course-details-modal");

		// Check fields of new entry
		WebElement entryEl = user.getDriver().findElement(By.cssSelector("li.entry-title"));

		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector("li.entry-title .forum-entry-title"), title));
		user.getWaiter()
				.until(ExpectedConditions.textToBe(By.cssSelector("li.entry-title .forum-entry-author"), TEACHER_NAME));
		user.getWaiter()
				.until(ExpectedConditions.textToBe(By.cssSelector("li.entry-title .forum-entry-date"), entryDate));

		entryEl.click();

		user.getWaiter()
				.until(ExpectedConditions.textToBe(
						By.cssSelector(".comment-block > app-comment:first-child > div.comment-div .forum-comment-msg"),
						comment));
		user.getWaiter()
				.until(ExpectedConditions.textToBe(
						By.cssSelector(
								".comment-block > app-comment:first-child > div.comment-div .forum-comment-author"),
						TEACHER_NAME));

		// Comment reply

		String reply = "TEST FORUM REPLY";
		openDialog(".replay-icon");
		commentField = user.getDriver().findElement(By.id("input-post-comment"));
		commentField.sendKeys(reply);

		user.getDriver().findElement(By.id("post-modal-btn")).click();

		waitForDialogClosed("course-details-modal");

		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector(
				".comment-block > app-comment:first-child > div.comment-div div.comment-div .forum-comment-msg"),
				reply));
		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector(
				".comment-block > app-comment:first-child > div.comment-div div.comment-div .forum-comment-author"),
				TEACHER_NAME));

		// Forum deactivation

		user.getDriver().findElement(By.id("entries-sml-btn")).click();
		openDialog("#edit-forum-icon");

		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("label-forum-checkbox"))));
		user.getDriver().findElement(By.id("label-forum-checkbox")).click();
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("put-modal-btn"))));
		user.getDriver().findElement(By.id("put-modal-btn")).click();

		waitForDialogClosed("put-delete-modal");

		user.getWaiter().until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("app-error-message .card-panel.warning")));

	}

	@Test
	@DisplayName("Files REST operations")
	void filesRestOperations() throws Exception {

		log.info("Files REST operations");

		enterCourseAndNavigateTab(COURSE_NAME, "files-tab-icon");

		user.getWaiter().until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("app-error-message .card-panel.warning")));
		openDialog("#add-files-icon");

		String fileGroup = "TEST FILE GROUP";

		// Find form elements
		WebElement titleField = user.getDriver().findElement(By.id("input-post-title"));
		titleField.sendKeys(fileGroup);

		user.getDriver().findElement(By.id("post-modal-btn")).click();

		waitForDialogClosed("course-details-modal");

		// Check fields of new file group
		user.getWaiter().until(ExpectedConditions.textToBe(By.cssSelector(".file-group-title h5"), fileGroup));

		// Edit file group
		openDialog("#edit-filegroup-icon");

		// Find form elements
		titleField = user.getDriver().findElement(By.id("input-file-title"));
		titleField.clear();
		titleField.sendKeys(fileGroup + EDITED);

		user.getDriver().findElement(By.id("put-modal-btn")).click();

		waitForDialogClosed("put-delete-modal");

		// Check fields of new file group
		user.getWaiter().until(
				ExpectedConditions.textToBe(By.cssSelector("app-file-group .file-group-title h5"), fileGroup + EDITED));

		// Add file subgroup
		String fileSubGroup = "TEST FILE SUBGROUP";
		openDialog(".add-subgroup-btn");
		titleField = user.getDriver().findElement(By.id("input-post-title"));
		titleField.sendKeys(fileSubGroup);

		user.getDriver().findElement(By.id("post-modal-btn")).click();

		waitForDialogClosed("course-details-modal");

		// Check fields of new file subgroup
		user.getWaiter().until(ExpectedConditions
				.textToBe(By.cssSelector("app-file-group app-file-group .file-group-title h5"), fileSubGroup));

		openDialog("app-file-group app-file-group .add-file-btn");

		WebElement fileUploader = user.getDriver()
				.findElement(By.xpath("//input[contains(@class, 'input-file-uploader')]"));
		File resourcesDirectory = new File("src/test/resources");

		String fileName = "testFile.txt";

		System.out.println(resourcesDirectory.getAbsolutePath() + "/" + fileName);

		fileUploader.sendKeys(resourcesDirectory.getAbsolutePath() + "/" + fileName);

		user.getDriver().findElement(By.id("upload-all-btn")).click();

		// Wait for upload
		user.getWaiter().until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class, 'determinate') and contains(@style, 'width: 100')]")));

		// Close dialog
		user.getDriver().findElement(By.id("close-upload-modal-btn")).click();
		waitForDialogClosed("course-details-modal");

		// Check new uploaded file
		user.getWaiter().until(ExpectedConditions
				.textToBe(By.cssSelector("app-file-group app-file-group .chip .file-name-div"), fileName));

		// Edit file
		openDialog("app-file-group app-file-group .edit-file-name-icon");
		titleField = user.getDriver().findElement(By.id("input-file-title"));
		titleField.clear();

		String editedFileName = "testFileEDITED.txt";

		titleField.sendKeys(editedFileName);
		user.getDriver().findElement(By.id("put-modal-btn")).click();
		waitForDialogClosed("put-delete-modal");

		// Check edited file name
		user.getWaiter().until(ExpectedConditions
				.textToBe(By.cssSelector("app-file-group app-file-group .chip .file-name-div"), editedFileName));

		// Delete file group
		user.getDriver().findElement(By.cssSelector("app-file-group .delete-filegroup-icon")).click();
		user.getWaiter().until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("app-error-message .card-panel.warning")));

	}

	@Test
	@DisplayName("Attenders REST operations")
	void attendersRestOperations() throws Exception {

		log.info("Attenders REST operations");

		enterCourseAndNavigateTab(COURSE_NAME, "attenders-tab-icon");

		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.className("attender-row-div"), 1));

		user.getWaiter()
				.until(ExpectedConditions.textToBe(By.cssSelector(".attender-row-div .attender-name-p"), TEACHER_NAME));

		// Add attender fail

		openDialog("#add-attenders-icon");

		String attenderName = "studentFail@gmail.com";

		WebElement titleField = user.getDriver().findElement(By.id("input-attender-simple"));
		titleField.sendKeys(attenderName);

		user.getDriver().findElement(By.id("put-modal-btn")).click();
		waitForDialogClosed("put-delete-modal");

		user.getWaiter()
				.until(ExpectedConditions.elementToBeClickable(By.cssSelector("app-error-message .card-panel.fail")));

		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.className("attender-row-div"), 1));

		user.getDriver().findElement(By.cssSelector("app-error-message .card-panel.fail .material-icons")).click();

		// Add attender success

		openDialog("#add-attenders-icon");

		attenderName = "student1@gmail.com";

		titleField = user.getDriver().findElement(By.id("input-attender-simple"));
		titleField.sendKeys(attenderName);

		user.getDriver().findElement(By.id("put-modal-btn")).click();
		waitForDialogClosed("put-delete-modal");

		user.getWaiter().until(
				ExpectedConditions.elementToBeClickable(By.cssSelector("app-error-message .card-panel.correct")));

		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.className("attender-row-div"), 2));

		user.getDriver().findElement(By.cssSelector("app-error-message .card-panel.correct .material-icons")).click();

		// Remove attender

		user.getDriver().findElement(By.id("edit-attenders-icon")).click();
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.cssSelector(".del-attender-icon")));
		user.getDriver().findElement(By.cssSelector(".del-attender-icon")).click();
		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.className("attender-row-div"), 1));

	}

	/*** Auxiliary methods ***/

	private void loginTeacher() {
		this.user = setupBrowser("chrome");
		this.login(user, TEACHER_MAIL, TEACHER_PASS);
		waitForAnimations();
	}

	private void login(BrowserUser user, String userEmail, String userPass) {
		openDialog("#download-button");

		// Find form elements (login modal is already opened)
		WebElement userNameField = user.getDriver().findElement(By.id("email"));
		WebElement userPassField = user.getDriver().findElement(By.id("password"));

		// Fill input fields
		userNameField.sendKeys(userEmail);
		userPassField.sendKeys(userPass);

		// Ensure fields contain what has been entered
		Assert.assertEquals(userNameField.getAttribute("value"), userEmail);
		Assert.assertEquals(userPassField.getAttribute("value"), userPass);

		user.getDriver().findElement(By.id("log-in-btn")).click();
	}

	private void logout(BrowserUser user) {
		if (user.getDriver().findElements(By.cssSelector("#fixed-icon")).size() > 0) {
			// Get out of video session page
			user.getDriver().findElement(By.cssSelector("#fixed-icon")).click();
			waitForAnimations();
			user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.cssSelector("#exit-icon")));
			user.getDriver().findElement(By.cssSelector("#exit-icon")).click();
		}

		try {
			// Up bar menu
			user.getWaiter().withTimeout(1000, TimeUnit.MILLISECONDS)
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#arrow-drop-down")));
			user.getDriver().findElement(By.cssSelector("#arrow-drop-down")).click();
			waitForAnimations();
			user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.cssSelector("#logout-button")));
			user.getDriver().findElement(By.cssSelector("#logout-button")).click();
		} catch (TimeoutException e) {
			// Shrunk menu
			user.getWaiter().withTimeout(1000, TimeUnit.MILLISECONDS)
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("a.button-collapse")));
			user.getDriver().findElement(By.cssSelector("a.button-collapse")).click();
			waitForAnimations();
			user.getWaiter().until(
					ExpectedConditions.elementToBeClickable(By.xpath("//ul[@id='nav-mobile']//a[text() = 'Logout']")));
			user.getDriver().findElement(By.xpath("//ul[@id='nav-mobile']//a[text() = 'Logout']")).click();
		}
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id("download-button")));
	}

	private void addCourse(String courseName) {
		int numberOfCourses = user.getDriver().findElements(By.className("course-list-item")).size();

		openDialog("#add-course-icon");

		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("input-post-course-name"))));
		user.getDriver().findElement(By.id("input-post-course-name")).sendKeys(courseName);
		user.getDriver().findElement(By.id("submit-post-course-btn")).click();

		waitForDialogClosed("course-modal");

		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#course-list .course-list-item"),
				numberOfCourses + 1));
		user.getWaiter().until(ExpectedConditions.textToBe(
				By.cssSelector("#course-list .course-list-item:last-child div.course-title span"), courseName));
	}

	private void deleteCourse(String courseName) {

		List<WebElement> allCourses = user.getDriver().findElements(By.className("course-list-item"));
		int numberOfCourses = allCourses.size();
		WebElement course = null;
		for (WebElement c : allCourses) {
			WebElement innerTitleSpan = c.findElement(By.cssSelector("div.course-title span"));
			if (innerTitleSpan.getText().equals(courseName)) {
				course = c;
				break;
			}
		}

		WebElement editIcon = course.findElement(By.className("course-put-icon"));
		openDialog(editIcon);

		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("label-delete-checkbox"))));
		user.getDriver().findElement(By.id("label-delete-checkbox")).click();
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.id(("delete-course-btn"))));
		user.getDriver().findElement(By.id("delete-course-btn")).click();

		waitForDialogClosed("put-delete-course-modal");

		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#course-list .course-list-item"),
				numberOfCourses - 1));
		user.getWaiter()
				.until(ExpectedConditions.not(ExpectedConditions.textToBe(
						By.cssSelector("#course-list .course-list-item:last-child div.course-title span"),
						courseName)));
	}

	private void enterCourseAndNavigateTab(String courseName, String tabId) {

		List<WebElement> allCourses = user.getDriver()
				.findElements(By.cssSelector("#course-list .course-list-item div.course-title span"));
		WebElement courseSpan = null;
		for (WebElement c : allCourses) {
			if (c.getText().equals(courseName)) {
				courseSpan = c;
				break;
			}
		}

		courseSpan.click();

		user.getWaiter().until(ExpectedConditions.textToBe(By.id("main-course-title"), courseName));
		user.getDriver().findElement(By.id(tabId)).click();

		waitForAnimations();
	}

	private void deleteCourseIfExist() {
		user.getDriver().get(APP_URL);
		user.getWaiter().until(ExpectedConditions.presenceOfElementLocated(By.id(("course-list"))));

		List<WebElement> allCourses = user.getDriver().findElements(By.className("course-list-item"));
		WebElement course = null;
		for (WebElement c : allCourses) {
			WebElement innerTitleSpan = c.findElement(By.cssSelector("div.course-title span"));
			if (innerTitleSpan.getText().equals(COURSE_NAME)) {
				course = c;
				break;
			}
		}

		if (course != null) {
			this.deleteCourse(COURSE_NAME);
		}
	}

	private void openDialog(String cssSelector) {
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
		user.getDriver().findElement(By.cssSelector(cssSelector)).click();
		waitForAnimations();
	}

	private void openDialog(WebElement el) {
		user.getWaiter().until(ExpectedConditions.elementToBeClickable(el));
		el.click();
		user.getWaiter().until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class, 'modal-overlay') and contains(@style, 'opacity: 0.5')]")));
	}

	private void waitForDialogClosed(String dialogId) {
		user.getWaiter().until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='" + dialogId
				+ "' and contains(@class, 'my-modal-class') and contains(@style, 'opacity: 0') and contains(@style, 'display: none')]")));
		user.getWaiter()
				.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".modal.my-modal-class.open")));
		user.getWaiter().until(ExpectedConditions.numberOfElementsToBe(By.cssSelector(".modal-overlay"), 0));
	}

	private void waitForAnimations() {
		try {
			Thread.sleep(750);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
