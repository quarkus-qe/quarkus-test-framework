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
import org.junit.jupiter.api.Assertions;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.services.DevModeQuarkusApplication;

public class DevModeQuarkusService extends RestService {
    public static final String DEV_UI_PATH = "/q/dev";

    private static final int JAVASCRIPT_WAIT_TIMEOUT_MILLIS = 10000;
    private static final String XPATH_BTN_CLASS = "contains(@class, 'btn')";
    private static final String XPATH_BTN_ON_OFF_CLASS = "contains(@class, 'btnPowerOnOffButton')";
    private static final String CONTINUOUS_TESTING_BTN = "//a[" + XPATH_BTN_CLASS + " and " + XPATH_BTN_ON_OFF_CLASS + "]";
    private static final String CONTINUOUS_TESTING_LABEL_DISABLED = "Tests not running";

    public DevModeQuarkusService enableContinuousTesting() {
        HtmlPage webDevUi = webDevUiPage();

        // If the enable continuous testing btn is not found, we assume it's already enabled it.
        if (isContinuousTestingBtnDisabled(webDevUi)) {
            clickOnElement(getContinuousTestingBtn(webDevUi));
        }

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

    public HtmlElement getContinuousTestingBtn(HtmlPage page) {
        List<HtmlElement> btn = getElementsByXPath(page, CONTINUOUS_TESTING_BTN);
        assertEquals(1, btn.size(), "Should be only one button to enable continuous testing");
        return btn.get(0);
    }

    public boolean isContinuousTestingBtnDisabled(HtmlPage page) {
        HtmlElement btn = getContinuousTestingBtn(page);
        return btn.getTextContent().trim().equals(CONTINUOUS_TESTING_LABEL_DISABLED);
    }

    public HtmlPage clickOnElement(HtmlElement elem) {
        try {
            return elem.click();
        } catch (IOException e) {
            Assertions.fail("Can't click on element. Caused by: " + e.getMessage());
        }

        return null;
    }

    public List<HtmlElement> getElementsByXPath(HtmlPage htmlPage, String path) {
        return htmlPage.getByXPath(path).stream()
                .filter(elem -> elem instanceof HtmlElement)
                .map(elem -> (HtmlElement) elem)
                .collect(toList());
    }

    public HtmlPage webDevUiPage() {
        try {
            HtmlPage page = (HtmlPage) webPage(DEV_UI_PATH).refresh();
            waitUntilLoaded(page);
            return page;
        } catch (IOException e) {
            return null;
        }
    }

    private void waitUntilLoaded(HtmlPage page) {
        page.getEnclosingWindow().getJobManager().waitForJobs(JAVASCRIPT_WAIT_TIMEOUT_MILLIS);
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
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().clearCookies();
        webClient.getCache().clear();
        webClient.getCache().setMaxSize(0);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        // re-synchronize asynchronous XHR.
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setDownloadImages(false);
        webClient.getOptions().setGeolocationEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setCssEnabled(false);
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
