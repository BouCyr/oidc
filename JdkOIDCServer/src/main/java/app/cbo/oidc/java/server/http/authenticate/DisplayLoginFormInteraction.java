package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DisplayLoginFormInteraction implements Interaction {

    private final String payload;

    public DisplayLoginFormInteraction(String ongoingAuthId) {


        this.payload = """
                <!DOCTYPE html>
                                <html>
                                <head>
                                    <title>PLease don't lie, I did not invest much in security</title>
                                    <link rel="icon" type="image/x-icon" href="/sc/fav.svg">
                                    <link href="/sc/clean.css" rel="stylesheet">
                                </head>
                                <body class="">
                
                
                                <div class="FORM ">
                                    <h1>WELCOME</h1>
                                    <form action="/login" class="container" method="POST">
                                        <input name="ongoing" type="hidden" value="¤¤TOKEN¤¤"/>
                                        <input id="totp" name="totp" type="hidden">
                                        <label autocomplete="off" for="login">Username</label>
                                        <input id="login" name="login"/>
                                        <label for="pwd">Password</label>
                                        <input id="pwd" name="pwd" type="password"/>
                                        <label for="totp1">TOTP</label>
                
                                        <input id="totp1" max="9" maxlength="1" min="0" name="totp1" oninput="totpinput(this);" step="1" totpIdx="1"
                                               type="number"/>
                                        <input id="totp2" max="9" maxlength="1" min="0" name="totp2" oninput="totpinput(this);" step="1" totpIdx="2"
                       type="number"/>
                <input id="totp3" max="9" maxlength="1" min="0" name="totp3" oninput="totpinput(this);" step="1" totpIdx="3"
                       type="number"/>
                <input id="totp4" max="9" maxlength="1" min="0" name="totp4" oninput="totpinput(this);" step="1" totpIdx="4"
                       type="number"/>
                <input id="totp5" max="9" maxlength="1" min="0" name="totp5" oninput="totpinput(this);" step="1" totpIdx="5"
                       type="number"/>
                <input id="totp6" max="9" maxlength="1" min="0" name="totp6" oninput="totpinput(this);" step="1" totpIdx="6"
                       type="number"/>
                
                                        <input class="submit" type="submit" value="Go on"/>
                </form>
                <div style="margin-top: 20px; text-align: center;">
                    <button id="webauthn-login-button" type="button" class="submit" style="background-color: #4CAF50;">Login with Biometrics</button>
                    <!-- Placeholder for registration button, if needed later -->
                    <!-- <button id="webauthn-register-button" type="button" class="submit" style="background-color: #007bff;">Register Biometrics</button> -->
                </div>
                </div>
                
                        <script>
                
                function totpinput(e){
                    if (e.value && e.value.length > e.maxLength)
                        e.value = e.value.slice(0, e.maxLength);
                    if(e.value && e.value.length >0 && totp(e) < 6){
                    nextNumber(totp(e)+1);
                    } else if((!e.value || e.value.length == 0) && totp(e) > 1) {
                        nextNumber(totp(e)-1);
                    }
                    compute();
                }
                
                function compute(){
                    const totpfield = document.getElementById("totp");
                    totpfield.value =
                    document.getElementById("totp1").value
                        + document.getElementById("totp2").value
                        + document.getElementById("totp3").value
                        + document.getElementById("totp4").value
                        + document.getElementById("totp5").value
                        + document.getElementById("totp6").value;
                }
                
                
                function totp(e) {
                    return Number.parseInt(e.getAttribute("totpIdx"));
                }
                function nextNumber(eltToFocus){
                    const id = "totp"+eltToFocus;
                    document.getElementById(id).focus();
                }
                        </script>
                        <script>
// Helper functions for base64url conversion (essential for WebAuthn)
function base64urlToArrayBuffer(base64url) {
    const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    const binaryString = window.atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

function arrayBufferToBase64url(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function startWebAuthnLogin() {
    const ongoingAuthIdElement = document.querySelector('input[name="ongoing"]');
    if (!ongoingAuthIdElement) {
        console.error('Ongoing auth ID input field not found');
        alert('An error occurred. Please try again.');
        return;
    }
    const ongoingAuthId = ongoingAuthIdElement.value;

    try {
        // 1. Get challenge from server
        const response = await fetch(`/login/webauthn/login/start?ongoing=${ongoingAuthId}`);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Failed to get challenge: ${response.status} ${errorText}`);
        }
        const options = await response.json();

        // Convert base64url strings to ArrayBuffers
        options.challenge = base64urlToArrayBuffer(options.challenge);
        if (options.allowCredentials) {
            for (let cred of options.allowCredentials) {
                cred.id = base64urlToArrayBuffer(cred.id);
            }
        }

        // 2. Call WebAuthn API
        const assertion = await navigator.credentials.get({ publicKey: options });

        // Convert ArrayBuffers back to base64url for server
        const assertionResponse = {
            id: assertion.id, // This is already base64url if using FIDO2, might need conversion for U2F
            rawId: arrayBufferToBase64url(assertion.rawId),
            response: {
                authenticatorData: arrayBufferToBase64url(assertion.response.authenticatorData),
                clientDataJSON: arrayBufferToBase64url(assertion.response.clientDataJSON),
                signature: arrayBufferToBase64url(assertion.response.signature),
                userHandle: assertion.response.userHandle ? arrayBufferToBase64url(assertion.response.userHandle) : null,
            },
            type: assertion.type
        };

        // 3. Send assertion to server
        const verifyResponse = await fetch(`/login/webauthn/login/finish?ongoing=${ongoingAuthId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(assertionResponse)
        });

        if (verifyResponse.ok) {
            if (verifyResponse.redirected) {
                window.location.href = verifyResponse.url;
            } else {
                const result = await verifyResponse.json().catch(() => ({})); // Handle empty or non-JSON response
                if (result.redirectTo) {
                     window.location.href = result.redirectTo;
                } else {
                    alert("WebAuthn login successful (client message). Please click 'Go on' if needed or await redirection.");
                }
            }
        } else {
            const errorResult = await verifyResponse.json().catch(() => ({ message: 'Unknown error during verification.' }));
            alert(`WebAuthn login failed: ${errorResult.message || 'Unknown error'}`);
        }

    } catch (error) {
        console.error('WebAuthn login error:', error);
        alert('WebAuthn login failed: ' + error.message);
    }
}

// Placeholder for registration JS
async function startWebAuthnRegistration() {
    alert("WebAuthn registration not implemented yet.");
    // Conceptual:
    // const ongoingAuthId = document.querySelector('input[name="ongoing"]').value;
    // const username = document.getElementById('login').value; // Assuming username is needed
    // if (!username) {
    //     alert("Please enter a username before registering biometrics.");
    //     return;
    // }
    // try {
    //     const response = await fetch(`/login/webauthn/register/start?username=${username}&ongoing=${ongoingAuthId}`);
    //     const options = await response.json();
    //     options.challenge = base64urlToArrayBuffer(options.challenge);
    //     options.user.id = base64urlToArrayBuffer(options.user.id);
    //     if (options.excludeCredentials) {
    //         for (let cred of options.excludeCredentials) {
    //             cred.id = base64urlToArrayBuffer(cred.id);
    //         }
    //     }
    //     const credential = await navigator.credentials.create({ publicKey: options });
    //     // Convert to base64url and send to server endpoint /login/webauthn/register/finish
    // } catch (error) {
    //    console.error('WebAuthn registration error:', error);
    //    alert('WebAuthn registration failed: ' + error.message);
    // }
}

document.addEventListener('DOMContentLoaded', () => {
    const webAuthnButton = document.getElementById('webauthn-login-button');
    if (webAuthnButton) {
        webAuthnButton.addEventListener('click', startWebAuthnLogin);
    }
    // Placeholder for a registration button, if one were added:
    // const webAuthnRegisterButton = document.getElementById('webauthn-register-button');
    // if (webAuthnRegisterButton) {
    //     webAuthnRegisterButton.addEventListener('click', startWebAuthnRegistration);
    // }
});
                        </script>
                    </body>
                </html>
                """.replaceAll("¤¤TOKEN¤¤", ongoingAuthId != null ? ongoingAuthId : UUID.randomUUID().toString());


    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        exchange.sendResponseHeaders(HttpCode.OK.code(), payload.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
}
