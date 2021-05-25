import static io.quarkus.test.utils.AwaitilityUtils.untilIsTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.bootstrap.Service;
import io.quarkus.test.utils.AwaitilityUtils;

public class AwaitilityUtilsTest {
    @Test
    public void ss() {
        Service service = new RestService();
        service.register("aaa");
        untilIsTrue(() -> false, AwaitilityUtils.AwaitilitySettings
                .using(Duration.ofSeconds(1), Duration.ofSeconds(3))
                .withService(service)
                .timeoutMessage("Service didn't start in %s minutes", 3));
    }
}
