package com.claimsys;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private static final String CLAIM = """
        {"type":"Accident","description":"Rear-ended at a signal","amount":120000,"incidentDate":"%s"}
        """.formatted(java.time.LocalDate.now().minusDays(2));

    @Test
    void fullClaimLifecycle() throws Exception {
        // register a customer
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"password\":\"Passw0rd!\",\"policyNumber\":\"POL2001\"}"))
           .andExpect(status().isCreated());

        // customer submits claim
        String body = mvc.perform(post("/api/claims").with(httpBasic("asha@example.com", "Passw0rd!"))
                .contentType(MediaType.APPLICATION_JSON).content(CLAIM))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.status").value("SUBMITTED"))
           .andExpect(jsonPath("$.priority").value("MEDIUM"))
           .andReturn().getResponse().getContentAsString();
        JsonNode claim = json.readTree(body);
        long id = claim.get("id").asLong();
        assertTrue(claim.get("claimNumber").asText().startsWith("CL"));

        // upload a document, and reject a disallowed type
        mvc.perform(multipart("/api/claims/" + id + "/documents")
                .file(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3}))
                .with(httpBasic("asha@example.com", "Passw0rd!")))
           .andExpect(status().isCreated());
        mvc.perform(multipart("/api/claims/" + id + "/documents")
                .file(new MockMultipartFile("file", "evil.exe", "application/octet-stream", new byte[]{1}))
                .with(httpBasic("asha@example.com", "Passw0rd!")))
           .andExpect(status().isBadRequest());

        // customer cannot use employee endpoints
        mvc.perform(get("/api/employee/claims").with(httpBasic("asha@example.com", "Passw0rd!")))
           .andExpect(status().isForbidden());

        // another customer cannot see this claim
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ben\",\"email\":\"ben@example.com\",\"password\":\"Passw0rd!\",\"policyNumber\":\"POL2002\"}"))
           .andExpect(status().isCreated());
        mvc.perform(get("/api/claims/" + id).with(httpBasic("ben@example.com", "Passw0rd!")))
           .andExpect(status().isNotFound());

        // employee: invalid jump SUBMITTED -> APPROVED is blocked, valid path works
        var emp = httpBasic("employee@claims.com", "Employee@123");
        mvc.perform(patch("/api/employee/claims/" + id + "/status").with(emp)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}"))
           .andExpect(status().isConflict());
        mvc.perform(patch("/api/employee/claims/" + id + "/status").with(emp)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}"))
           .andExpect(status().isOk());
        mvc.perform(patch("/api/employee/claims/" + id + "/status").with(emp)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\",\"note\":\"Docs verified\"}"))
           .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        // search + stats
        mvc.perform(get("/api/employee/claims?status=APPROVED&q=asha").with(emp))
           .andExpect(status().isOk()).andExpect(jsonPath("$[0].claimNumber").exists());
        mvc.perform(get("/api/employee/claims/stats").with(emp))
           .andExpect(status().isOk()).andExpect(jsonPath("$.APPROVED").value(1));
    }

    @Test
    void unauthenticatedAndInvalidRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/claims")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/claims").with(httpBasic("rahul@example.com", "Customer@123"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"\",\"amount\":-5}"))
           .andExpect(status().isBadRequest());
    }
}
