package io.quarkus.test.bootstrap;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.yaml.snakeyaml.Yaml;

import io.quarkus.test.configuration.PropertyLookup;
import io.quarkus.test.logging.FileLoggingHandler;
import io.quarkus.test.logging.Log;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.ProcessBuilderProvider;

public class QuarkusHelmClient {

    public static final String COMMAND_LOG_FILE = "quarkus-helm-command.out";
    static final PropertyLookup COMMAND = new PropertyLookup("ts.quarkus.helm.cmd", "helm");
    private static final Path TARGET = Paths.get("target");
    private static final int NAME_POS = 0;
    private static final int NAMESPACE_POS = 1;
    private static final int REVISION_POS = 2;
    private static final int UPDATED_POS = 3;
    private static final int STATUS_POS = 4;
    private static final int CHART_VERSION_POS = 5;
    private static final int EXPECTED_CHART_FIELDS_AMOUNT = 5;

    private final ScenarioContext context;
    private Yaml yaml = new Yaml();

    public QuarkusHelmClient(ScenarioContext ctx) {
        this.context = ctx;
    }

    public Result run(String... args) {
        return runCliAndWait(args);
    }

    public File getWorkingDirectory() {
        return TARGET.toFile();
    }

    public NewChartResult createEmptyChart(String chartName) {
        Result chartResultCmd = runCliAndWait("create", chartName);
        return new NewChartResult(chartResultCmd, chartName, getWorkingDirectory().getAbsolutePath());
    }

    public Result installChart(String chartName, String chartFolderPath) {
        return runCliAndWait("install", chartName, chartFolderPath);
    }

    public Result updateChart(String chartName, String chartFolderPath) {
        return runCliAndWait("update", chartName, chartFolderPath);
    }

    public Result uninstallChart(String chartName) {
        return runCliAndWait("uninstall", chartName);
    }

    public List<ChartListResult> getCharts() {
        Result result = runCliAndWait("list");
        String[] charts = result.getOutput().split(System.lineSeparator());
        List<ChartListResult> chartList = new ArrayList<>();
        if (charts.length > 0) {
            // skip helm.list header -> pos(0)
            for (int i = 1; i < charts.length; i++) {
                String[] chartContent = charts[i].split("\t");
                if (chartContent.length >= EXPECTED_CHART_FIELDS_AMOUNT) {
                    ChartListResult chartItem = new ChartListResult(result);
                    chartItem.setName(chartContent[NAME_POS]);
                    chartItem.setNamespace(chartContent[NAMESPACE_POS]);
                    chartItem.setRevision(chartContent[REVISION_POS]);
                    chartItem.setUpdated(chartContent[UPDATED_POS]);
                    chartItem.setStatus(chartContent[STATUS_POS]);
                    chartItem.setChartVersion(chartContent[CHART_VERSION_POS]);
                    chartList.add(chartItem);
                } else {
                    Log.warn("Unexpected 'helm list' response format");
                }
            }
        }
        return chartList;
    }

    public List<String> getChartsNames(String chartName) {
        List<QuarkusHelmClient.ChartListResult> charts = getCharts();
        return charts.stream()
                .map(QuarkusHelmClient.ChartListResult::getName)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public Result chartDependencyUpdate(String chartFullPath) {
        return runCliAndWait("dependency", "update", chartFullPath);
    }

    public Result chartDependencyBuild(String chartFullPath) {
        return runCliAndWait("dependency", "build", chartFullPath);
    }

    public Map<String, Object> getChartValues(String chartFolderPath) throws FileNotFoundException {
        return getRawYaml("values.yaml", chartFolderPath);
    }

    public Map<String, Object> getRawYaml(String yamlNameExtension, String chartFolderPath) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(chartFolderPath + "/" + yamlNameExtension);
        return yaml.load(inputStream);
    }

    public void waitToReadiness(String fullReadinessPath, Duration atMost) {
        await().ignoreExceptions().atMost(atMost)
                .untilAsserted(() -> {
                    URL url = new URL(fullReadinessPath);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    try {
                        con.setRequestMethod("GET");
                        con.connect();
                        Assertions.assertTrue(con.getResponseCode() == HttpURLConnection.HTTP_OK);
                    } finally {
                        con.disconnect();
                    }
                });
    }

    private Result runCliAndWait(String... args) {
        return runCliAndWait(TARGET, args);
    }

    private Result runCliAndWait(Path workingDirectory, String... args) {
        Result result = new Result();
        File output = workingDirectory.resolve(COMMAND_LOG_FILE).toFile();

        try (FileLoggingHandler loggingHandler = new FileLoggingHandler(output)) {
            loggingHandler.startWatching();
            List<String> cmd = buildCmd(args);
            result.commandExecuted = cmd.stream().collect(Collectors.joining(" "));
            Log.info(result.commandExecuted);
            Process process = runCli(workingDirectory, output, cmd);
            result.exitCode = process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            Log.warn("Failed to run Quarkus Helm command. Caused by: " + e.getMessage());
            result.exitCode = 1;
        }

        result.output = FileUtils.loadFile(output).trim();
        FileUtils.deleteFileContent(output);
        return result;
    }

    private List<String> buildCmd(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(COMMAND.get().split(" ")));
        cmd.addAll(Arrays.asList(args));
        return cmd;
    }

    private Process runCli(Path workingDirectory, File logOutput, List<String> cmd) {
        try {
            return ProcessBuilderProvider.command(cmd)
                    .redirectErrorStream(true)
                    .redirectOutput(logOutput)
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public class Result {

        private static final int EXIT_SUCCESS = 0;

        protected int exitCode;
        protected String output;
        protected String commandExecuted;

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public boolean isSuccessful() {
            return EXIT_SUCCESS == exitCode;
        }

        public String getCommandExecuted() {
            return commandExecuted;
        }
    }

    public class NewChartResult extends Result {

        private String chartFolderPath;
        private String chartName;

        public NewChartResult(Result result, String chartPath, String chartName) {
            this.exitCode = result.exitCode;
            this.output = result.output;
            this.commandExecuted = result.commandExecuted;
            this.chartName = chartName;
            this.chartFolderPath = chartPath;
        }

        public String getChartFolderPath() {
            return chartFolderPath;
        }

        public String getChartName() {
            return chartName;
        }
    }

    public class ChartListResult extends Result {

        private String name;
        private String namespace;
        private String revision;
        private String updated;
        private String status;
        private String chartVersion;

        public ChartListResult(Result result) {
            this.exitCode = result.exitCode;
            this.output = result.output;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public void setUpdated(String updated) {
            this.updated = updated;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setChartVersion(String chartVersion) {
            this.chartVersion = chartVersion;
        }

        public String getName() {
            return name;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getRevision() {
            return revision;
        }

        public String getUpdated() {
            return updated;
        }

        public String getStatus() {
            return status;
        }

        public String getChartVersion() {
            return chartVersion;
        }
    }
}
