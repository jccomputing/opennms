/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.smoketest;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.opennms.test.mock.MockLogAppender;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.thoughtworks.selenium.SeleneseTestBase;
import com.thoughtworks.selenium.SeleniumException;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;

public class OpenNMSSeleniumTestCase extends SeleneseTestBase {
    protected static final String LOAD_TIMEOUT = "60000";
    protected static final String BASE_URL = "http://localhost:8980/";

    @Before
    public void setUp() throws Exception {
        final String logLevel = System.getProperty("org.opennms.smoketest.logLevel", "DEBUG");
        MockLogAppender.setupLogging(true, logLevel);

        WebDriver driver = null;

        // Google Chrome if chrome driver property is set
        final String chromeDriverLocation = System.getProperty("webdriver.chrome.driver");
        if (chromeDriverLocation != null) {
            final File chromeDriverFile = new File(chromeDriverLocation);
            if (chromeDriverFile.exists() && chromeDriverFile.canExecute()) {
                System.err.println("using chrome driver");
                driver = new ChromeDriver();
            }
        }
        
        // otherwise, Firefox
        if (driver == null) {
            final File phantomJS = new File("/usr/local/bin/phantomjs");
            if (phantomJS.exists()) {
                final DesiredCapabilities caps = new DesiredCapabilities();
                caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "/usr/local/bin/phantomjs");
                driver = new PhantomJSDriver(caps);
            } else {
                driver = new FirefoxDriver();
            }
        }

        selenium = new WebDriverBackedSelenium(driver, BASE_URL);
        selenium.open("/opennms/login.jsp");
        selenium.type("name=j_username", "admin");
        selenium.type("name=j_password", "admin");
        selenium.click("name=Login");
        waitForPageToLoad();
    }

    @After
    public void tearDown() throws Exception {
        if (selenium != null) {
            try {
                if (selenium.isElementPresent("link=Log out")) selenium.click("link=Log out");
            } catch (final SeleniumException e) {
                // don't worry about it, this is just for logging out
            }
            selenium.stop();
        }
    }

    protected void waitForPageToLoad() {
        selenium.waitForPageToLoad(LOAD_TIMEOUT);
    }

}
