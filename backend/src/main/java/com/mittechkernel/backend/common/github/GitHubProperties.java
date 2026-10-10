package com.mittechkernel.backend.common.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.github")
public class GitHubProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:8080/api/v1/github/oauth/callback";
    private String scope = "read:user";
    private String oauthAuthorizeUrl = "https://github.com/login/oauth/authorize";
    private String oauthTokenUrl = "https://github.com/login/oauth/access_token";
    private String apiUrl = "https://api.github.com";
    private String tokenEncryptionKey = "";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getOauthAuthorizeUrl() {
        return oauthAuthorizeUrl;
    }

    public void setOauthAuthorizeUrl(String oauthAuthorizeUrl) {
        this.oauthAuthorizeUrl = oauthAuthorizeUrl;
    }

    public String getOauthTokenUrl() {
        return oauthTokenUrl;
    }

    public void setOauthTokenUrl(String oauthTokenUrl) {
        this.oauthTokenUrl = oauthTokenUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getTokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public void setTokenEncryptionKey(String tokenEncryptionKey) {
        this.tokenEncryptionKey = tokenEncryptionKey;
    }
}
