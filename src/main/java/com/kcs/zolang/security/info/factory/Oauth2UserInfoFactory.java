package com.kcs.zolang.security.info.factory;

import com.kcs.zolang.dto.type.EProvider;
import com.kcs.zolang.security.info.GithubOauth2UserInfo;

import java.util.Map;

public class Oauth2UserInfoFactory {
    public static Oauth2UserInfo getOauth2UserInfo(EProvider provider, Map<String, Object> attributes){
        switch (provider) {
            case GITHUB:
                return new GithubOauth2UserInfo(attributes);
            default:
                throw new IllegalAccessError("잘못된 제공자 입니다.");
        }
    }
}
