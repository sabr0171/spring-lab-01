package kz.iitu.spring_lab_01.security;

import kz.iitu.spring_lab_01.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Individual variant 6: a Content-Security-Policy on every response
@SpringBootTest
@AutoConfigureMockMvc
class ContentSecurityPolicyTest extends PostgresIntegrationTest {

    private static final String POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    // <script> without src, on*="..." handlers, style="..." attributes — what the policy refuses
    private static final Pattern INLINE_SCRIPT = Pattern.compile("<script(?![^>]*\\ssrc=)[^>]*>");
    private static final Pattern INLINE_HANDLER = Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_STYLE = Pattern.compile("\\sstyle\\s*=", Pattern.CASE_INSENSITIVE);

    @Autowired
    MockMvc mvc;

    @Test
    void everyPageCarriesThePolicy() throws Exception {
        mvc.perform(get("/")).andExpect(header().string("Content-Security-Policy", POLICY));
        mvc.perform(get("/login")).andExpect(header().string("Content-Security-Policy", POLICY));
        mvc.perform(get("/books")).andExpect(status().isFound())              // even the redirect
                .andExpect(header().string("Content-Security-Policy", POLICY));
        mvc.perform(get("/css/app.css")).andExpect(header().string("Content-Security-Policy", POLICY));
    }

    @Test
    @WithUserDetails("user")
    void theForbiddenPageCarriesThePolicyToo() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Security-Policy", POLICY));
    }

    @Test
    void theStartPageHasTheInlineScriptThePolicyMustBlock() throws Exception {
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();

        assertThat(INLINE_SCRIPT.matcher(html).results().count()).isOne();   // the deliberate one
        assertThat(html).contains("<script src=\"/js/csp-check.js\"></script>");
    }

    @Test
    void theAllowedScriptIsServedToEveryone() throws Exception {
        mvc.perform(get("/js/csp-check.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("securitypolicyviolation")));
    }

    @Test
    @WithUserDetails("admin")
    void theApplicationsOwnPagesNeedNothingThePolicyForbids() throws Exception {
        for (String url : new String[] {"/books", "/admin"}) {
            String html = mvc.perform(get(url)).andReturn().getResponse().getContentAsString();
            assertThat(INLINE_SCRIPT.matcher(html).find()).as("inline script on " + url).isFalse();
            assertThat(INLINE_HANDLER.matcher(html).find()).as("inline handler on " + url).isFalse();
            assertThat(INLINE_STYLE.matcher(html).find()).as("style attribute on " + url).isFalse();
        }
    }

    @Test
    void theLoginPageNeedsNothingThePolicyForbids() throws Exception {
        String html = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();

        assertThat(INLINE_SCRIPT.matcher(html).find()).isFalse();
        assertThat(INLINE_HANDLER.matcher(html).find()).isFalse();
        assertThat(INLINE_STYLE.matcher(html).find()).isFalse();
    }
}
