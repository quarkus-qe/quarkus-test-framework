package io.quarkus.test.bootstrap;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.services.DevModeQuarkusApplication;
import io.quarkus.test.utils.AwaitilityUtils;

public class DevModeQuarkusService extends RestService {
    public static final String DEV_UI_PATH = "/q/dev";

    private static final int ENABLE_CONTINUOUS_TESTING_WAIT_TIME_MS = 2000;
    private static final String XPATH_BTN_CLASS = "contains(@class, 'btn')";
    private static final String XPATH_BTN_ON_OFF_CLASS = "contains(@class, 'btnPowerOnOffButton')";
    private static final String CONTINUOUS_TESTING_BTN = "//a[" + XPATH_BTN_CLASS + " and " + XPATH_BTN_ON_OFF_CLASS + "]";
    private static final String CONTINUOUS_TESTING_LABEL_DISABLED = "Tests not running";
    private static final String DEV_UI_READY_XPATH = "//a[@class='testsFooterButton btnDisplayTestHelp btn']";

    public DevModeQuarkusService enableContinuousTesting() {
        waitForDevUiReady();
        // If the enable continuous testing btn is not found, we assume it's already enabled it.
        if (isContinuousTestingBtnDisabled()) {
            clickOnElement(getContinuousTestingBtn());
        }

        // Wait a couple of seconds to ensure that continuous testing is enabled
        sleep(ENABLE_CONTINUOUS_TESTING_WAIT_TIME_MS);

        return this;
    }

    public void modifyFile(String file, Function<String, String> modifier) {
        try {
            File targetFile = getServiceFolder().resolve(file).toFile();
            String original = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
            String updated = modifier.apply(original);

            FileUtils.writeStringToFile(targetFile, updated, StandardCharsets.UTF_8, false);
        } catch (IOException e) {
            Assertions.fail("Error modifying file. Caused by " + e.getMessage());
        }
    }

    public void copyFile(String file, String target) {
        try {
            Path sourcePath = Path.of(file);
            File targetPath = getServiceFolder().resolve(target).toFile();
            FileUtils.deleteQuietly(targetPath);

            FileUtils.copyFile(sourcePath.toFile(), targetPath);
            targetPath.setLastModified(System.currentTimeMillis());
        } catch (IOException e) {
            Assertions.fail("Error copying file. Caused by " + e.getMessage());
        }
    }

    @Override
    public void validate(Field field) {
        if (!field.isAnnotationPresent(DevModeQuarkusApplication.class)) {
            Assertions.fail("DevModeQuarkusService service is not annotated with DevModeQuarkusApplication");
        }
    }

    public HtmlElement getContinuousTestingBtn() {
        List<HtmlElement> btn = getElementsByXPath(webDevUiPage(), CONTINUOUS_TESTING_BTN);
        assertEquals(1, btn.size(), "Should be only one button to enable continuous testing");
        return btn.get(0);
    }

    public boolean isContinuousTestingBtnDisabled() {
        HtmlElement btn = getContinuousTestingBtn();
        return btn.getTextContent().trim().equals(CONTINUOUS_TESTING_LABEL_DISABLED);
    }

    public void waitForDevUiReady() {
        AwaitilityUtils.until(
                () -> getElementsByXPath(webDevUiPage(), DEV_UI_READY_XPATH),
                Matchers.not(Matchers.empty()));
    }

    public void clickOnElement(HtmlElement elem) {
        try {
            elem.click();
        } catch (IOException e) {
            Assertions.fail("Can't click on element. Caused by: " + e.getMessage());
        }
    }

    public List<HtmlElement> getElementsByXPath(HtmlPage htmlPage, String path) {
        return htmlPage.getByXPath(path).stream()
                .filter(elem -> elem instanceof HtmlElement)
                .map(elem -> (HtmlElement) elem)
                .collect(toList());
    }

    public HtmlPage webDevUiPage() {
        try {
            return (HtmlPage) webPage(DEV_UI_PATH).refresh();
        } catch (IOException e) {
            return null;
        }
    }

    public HtmlPage webPage(String path) {
        try {
            return webClient().getPage(getHost() + ":" + getPort() + path);
        } catch (IOException e) {
            Assertions.fail("Page with path " + path + " does not exist");
        }

        return null;
    }

    public WebClient webClient() {
        WebClient webClient = new WebClient();
        webClient.getCookieManager().clearCookies();
        webClient.getCache().clear();
        webClient.getCache().setMaxSize(0);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        return webClient;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {

        }
    }
}
