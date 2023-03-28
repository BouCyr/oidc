package app.cbo.oidc.java.server.otp;

import org.apache.commons.codec.binary.Hex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

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

        var asb32 = "UUKKMBQE64DSSAVH";

        int cpt = 100;
        while(cpt > 0){

            System.out.println(TOTP.get(asb32));
            Thread.sleep(10_000L);
            cpt--;
        }
    }

    @Test
    @Disabled
    void realTimeTest() throws InterruptedException {

        // check with FreeOTP, GoogleAuthenticator or  https://totp.danhersam.com/

        var secret = SecretGenerator.getInstance().generateSecret(10);

        var asb32 = Base32.encode(secret);
        var asHex = Hex.encodeHexString(secret);
        System.out.println(asb32);
        System.out.println(new org.apache.commons.codec.binary.Base32().encodeAsString(secret));


        int cpt = 100;
        while(cpt > 0){

            var time = (Instant.now().getEpochSecond()/30);


//            long T = (testTime[i] - T0)/X;
            String steps = Long.toHexString(time).toUpperCase();
            while (steps.length() < 16) steps = "0" + steps;




            var totp = TOTP.generateTOTP(asHex, Long.toHexString(time).toUpperCase(), "6");
            var totp2 = TOTP.generateTOTP(asHex, steps, "6");

            System.out.println(totp);
            System.out.println(totp2);

            System.out.println();
            Thread.sleep(10_000L);
            cpt--;
        }
    }



}