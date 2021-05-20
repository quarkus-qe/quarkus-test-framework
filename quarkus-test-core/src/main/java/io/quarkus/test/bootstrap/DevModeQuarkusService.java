package io.quarkus.test.bootstrap;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class DevModeQuarkusService extends BaseService<DevModeQuarkusService> {

    private static final String DEV_UI_PATH = "/q/dev";
    private static final String ENABLE_CONTINUOUS_TESTING_BTN = "//a[@class='btn btnPowerOnOffButton text-warning']";

    public RequestSpecification given() {
        return RestAssured.given().baseUri(getHost()).basePath("/").port(getPort());
    }

    public DevModeQuarkusService enableContinuousTesting() {
        AwaitilityUtils.until(
                () -> getElementsByXPath(webDevUiPage(), ENABLE_CONTINUOUS_TESTING_BTN),
                Matchers.is(Matchers.not(Matchers.empty()))).forEach(this::clickOnElement);

        return this;
    }

    public void modifyFile(String file, Function<String, String> modifier) {
        try {
            File targetFile = servicePath().resolve(file).toFile();
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
            Path targetPath = servicePath().resolve(target);
            FileUtils.deleteQuietly(targetPath.toFile());

            FileUtils.copyFile(sourcePath.toFile(), targetPath.toFile());
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

    private Path servicePath() {
        return Paths.get("target/" + getName());
    }

    private void clickOnElement(HtmlElement elem) {
        try {
            elem.click();
        } catch (IOException e) {
            Assertions.fail("Can't click on element. Caused by: " + e.getMessage());
        }
    }

    private List<HtmlElement> getElementsByXPath(HtmlPage htmlPage, String path) {
        return htmlPage.getByXPath(path).stream()
                .filter(elem -> elem instanceof HtmlElement)
                .map(elem -> (HtmlElement) elem)
                .collect(toList());
    }

    private HtmlPage webDevUiPage() {
        return webPage(DEV_UI_PATH);
    }

    private HtmlPage webPage(String path) {
        try {
            return webClient().getPage(getHost() + ":" + getPort() + path);
        } catch (IOException e) {
            Assertions.fail("Page with path " + path + " does not exist");
        }

        return null;
    }

    private WebClient webClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        return webClient;
    }
}
