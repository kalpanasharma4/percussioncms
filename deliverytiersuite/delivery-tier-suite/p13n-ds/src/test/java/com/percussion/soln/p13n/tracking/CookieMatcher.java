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
package com.percussion.soln.p13n.tracking;

import javax.servlet.http.Cookie;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CookieMatcher extends TypeSafeMatcher<Cookie> {
	private String name;

	private String value;

	public CookieMatcher(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public void describeTo(Description description) {
		description.appendText("a Cookie named ").appendValue(name)
				.appendText(" with a value ").appendValue(value);
	}

	@Override
	public boolean matchesSafely(Cookie c) {
		return c.getName().equals(name) && c.getValue().equals(value);
	}

	@Factory
	public static <T> Matcher<Cookie> aCookie(String name, String value) {
		return new CookieMatcher(name, value);
	}
}