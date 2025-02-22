/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.tools.redirect;

public class PSNginxRedirectConverter extends PSBaseRedirectConverter{

    public static final String NGINX_RULE="rewrite {0} {1}";
    public static final String NGINX_FLAGS = "last";
    public static final String NGINX_RULE_END=";";



    @Override
    public String convertVanityRedirect(PSPercussionRedirectEntry e) {

        StringBuilder sb = new StringBuilder(NGINX_RULE.replace("{0}",
                "^"+ e.getCondition()).replace("{1}",e.getRedirectTo()));
        sb.append(" ").append(NGINX_FLAGS);
        sb.append(NGINX_RULE_END);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public String convertRegexRedirect(PSPercussionRedirectEntry e) {
        StringBuilder sb = new StringBuilder(NGINX_RULE.replace("{0}",
                e.getCondition()).replace("{1}",e.getRedirectTo()));
        sb.append(" ").append(NGINX_FLAGS);
        sb.append(NGINX_RULE_END);
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    @Override
    public String getFilename() {
        return "nginx_rules.conf";
    }
}
