/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2018 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.takes.facets.hamcrest;

import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.takes.Body;
import org.takes.Request;
import org.takes.rq.RqFake;

/**
 * Test case for {@link HmBody}.
 *
 * @author Tolegen Izbassar (t.izbassar@gmail.com)
 * @version $Id$
 * @since 2.0
 */
public final class HmBodyTest {

    /**
     * HmRqBody can test if values of bodies are same.
     */
    @Test
    public void testsBodyValuesAreSame() {
        final String body = "Same";
        MatcherAssert.assertThat(
            new RqFake(
                Collections.<String>emptyList(),
                body
            ),
            new HmBody<>(body)
        );
    }

    /**
     * HmRqBody can test if values of bodies are different.
     */
    @Test
    public void testsBodyValuesAreDifferent() {
        MatcherAssert.assertThat(
            new RqFake(
                Collections.<String>emptyList(),
                "this"
            ),
            Matchers.not(new HmBody<>("that"))
        );
    }

    /**
     * HmRqBody can describe mismatch in readable way.
     */
    @Test
    public void describesMismatchInReadableWay() {
        final Request request = new RqFake(
            Collections.<String>emptyList(),
            "other"
        );
        final HmBody<Body> matcher = new HmBody<>("some");
        matcher.matchesSafely(request);
        final StringDescription description = new StringDescription();
        matcher.describeMismatchSafely(request, description);
        MatcherAssert.assertThat(
            description.toString(),
            Matchers.equalTo(
                "body was: [111, 116, 104, 101, 114]"
            )
        );
    }

    /**
     * HmBody can describe in readable way.
     */
    @Test
    public void describeToInReadableWay() {
        final Request request = new RqFake(
            Collections.<String>emptyList(),
            "one"
        );
        final HmBody<Body> matcher = new HmBody<>("two");
        matcher.matchesSafely(request);
        final StringDescription description = new StringDescription();
        matcher.describeTo(description);
        MatcherAssert.assertThat(
            description.toString(),
            new IsEqual<>(
                "body: [116, 119, 111]"
            )
        );
    }
}
