package app.cbo.oidc.java.server.credentials;

import org.apache.commons.codec.binary.Hex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class OneTimePasswordAlgorithmTest {

    @Test
    void bytesToHex() {

        var bytes = "QWERTY".getBytes();
        var mine = TOTP.bytesToHex(bytes);
        var theirs = Hex.encodeHexString(bytes);

        Assertions.assertThat(mine).isEqualTo(theirs);
    }

    @Test
    public void test_rfc(){

        /*The test token shared secret uses the ASCII string value
        "12345678901234567890".  With Time Step X = 30, and the Unix epoch as
        the initial value to count time steps, where T0 = 0, the TOTP
        algorithm will display the following values for specified modes and
        timestamps.

                +-------------+--------------+------------------+----------+--------+
                |  Time (sec) |   UTC Time   | Value of T (hex) |   TOTP   |  Mode  |
                +-------------+--------------+------------------+----------+--------+
                |      59     |  1970-01-01  | 0000000000000001 | 94287082 |  SHA1
                  */

        var base = "12345678901234567890".getBytes();
        var ascii = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        var utf8 = "12345678901234567890".getBytes(StandardCharsets.UTF_8);

        var hexEncoded = Hex.encodeHexString(ascii);
        String totp = TOTP.generateTOTP(hexEncoded, "1", "8" );

        Assertions.assertThat(totp)
                .isEqualTo("94287082");




    }

    @Test
    @Disabled
    void realTimeTestFixedKey() throws InterruptedException {

        // check with FreeOTP, GoogleAuthenticator or  https://totp.danhersam.com/

        var asb32 = "ALBACORE";

        int cpt = 100;
        while(cpt > 0){

            System.out.println(String.join(" -> ",TOTP.get(asb32,1,1)));
            Thread.sleep(10_000L);
            cpt--;
        }
    }




}