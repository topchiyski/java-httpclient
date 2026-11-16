package tests;

import base.BaseApiTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Protects a test from a server that sends its headers and then stalls while sending the body.
 */
public class HardDeadlineTest extends BaseApiTest {

    /**
     * Fails the test if the whole exchange, body included, takes longer than {@link #TEST_TIMEOUT_MS}.
     * The request timeout alone would stop guarding the test once the status line and headers arrive.
     *
     * @throws Exception if sending the HTTP request fails
     */
    @Test(timeOut = TEST_TIMEOUT_MS)
    public void testEndpointWithHardDeadline() throws Exception {
        HttpRequest request = requestTo("/get").GET().build();
        HttpResponse<String> response = sendAndLog(request);
        Assert.assertEquals(response.statusCode(), HTTP_OK);
    }
}
