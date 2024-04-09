package com.kcs.zolang.security.info;

import com.kcs.zolang.security.info.factory.Oauth2UserInfo;

import java.util.Map;

public class GithubOauth2UserInfo extends Oauth2UserInfo {
    public GithubOauth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Map<String, String> response = (Map<String, String>)attributes.get("response");
        return response.get("id");
    }
}
