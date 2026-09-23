package com.olarize.platform.internal.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class ApiExceptionHandlerTests {
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new ApiExceptionHandler()).addFilters(new RequestIdFilter()).build();
    }
    @Test void validationHasFieldCodesWithoutRejectedValues() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}")
                .header("X-Request-ID", "test-validation"))
            .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("HTTP_400"))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.requestId").value("test-validation"));
    }
    @Test void unexpectedExceptionDoesNotLeakDetails() throws Exception {
        mvc.perform(get("/test/fail")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(content().string(not(containsString("private-salary"))));
    }
    @Test void malformedJsonAndParameterErrorsAreBadRequests() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{bad-json"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("HTTP_400"));
        mvc.perform(get("/test/number").param("value", "private-salary"))
            .andExpect(status().isBadRequest()).andExpect(content().string(not(containsString("private-salary"))));
    }
    @RestController static class TestController {
        @PostMapping("/test") void create(@Valid @RequestBody Payload payload) { }
        @GetMapping("/test/fail") void fail() { throw new IllegalStateException("private-salary"); }
        @GetMapping("/test/number") int number(@RequestParam int value) { return value; }
    }
    record Payload(@NotBlank String name) { }
}
