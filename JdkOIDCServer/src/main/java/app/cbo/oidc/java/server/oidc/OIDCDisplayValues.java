package app.cbo.oidc.java.server.oidc;

import app.cbo.oidc.java.server.utils.EnumValuesHelper;

/**
 * display
 * OPTIONAL. ASCII string value that specifies how the Authorization Server displays the authentication and consent userId interface pages to the End-User. The defined values are:
 * -page : The Authorization Server SHOULD display the authentication and consent UI consistent with a full User Agent page view. If the display parameter is not specified, this is the default display mode.
 * -popup : The Authorization Server SHOULD display the authentication and consent UI consistent with a popup User Agent window. The popup User Agent window should be of an appropriate size for a login-focused dialog and should not obscure the entire window that it is popping up over.
 * -touch : The Authorization Server SHOULD display the authentication and consent UI consistent with a device that leverages a touch interface.
 * -wap : The Authorization Server SHOULD display the authentication and consent UI consistent with a "feature phone" type display.
 * <p>
 * The Authorization Server MAY also attempt to detect the capabilities of the User Agent and present an appropriate display.
 */
public enum OIDCDisplayValues implements EnumValuesHelper.ParamEnum {
    PAGE("page"),
    POPUP("popup"),
    TOUCH("touch"),
    WAP("wap");

    private final String paramValue;

    OIDCDisplayValues(String paramValue) {
        this.paramValue = paramValue;
    }

    public String paramValue(){
        return this.paramValue;
    }


}
